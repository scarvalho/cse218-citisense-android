package org.sosa.richservice.base;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.citisense.utils.thread.DaemonThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sosa.richservice.Message;
import org.sosa.richservice.MessageAddressed;
import org.sosa.richservice.MessageBus;
import org.sosa.richservice.MessageError;
import org.sosa.richservice.MessageNotification;
import org.sosa.richservice.MessageRequest;
import org.sosa.richservice.ServiceDataConnector;
import org.sosa.richservice.utils.richservice.RichServiceUtils;

/**
 * 
 * This implementation:
 * <ul>
 * <li>puts all messages received (via {@link #deliverMessage(MessageAddressed)}
 * ) to the queue {@code newArrivalsQ}.</li>
 * <li>has a separate thread that watches the queue {@code toConsumeQ} and
 * consumes the messages</li>
 * <li>consumes the messages in {@code toConsumeQ} in separate threads.</li>
 * </ul>
 * 
 * <p/>
 * FIXME: Provide cleanup operations that kill the internal thread etc..
 * 
 * @author celal.ziftci
 * 
 */
@SuppressWarnings("unchecked")
public class MessageBusBase implements MessageBus {
	private final Logger logger = LoggerFactory.getLogger(MessageBusBase.class);

	private final BlockingQueue<Message> newArrivalsQ, toConsumeQ;
	private final Map<String, ServiceDataConnector> mappings = new HashMap<String, ServiceDataConnector>();

	private final ExecutorService serviceCallExecutor = Executors
			.newCachedThreadPool(new DaemonThreadFactory(
					"MessageBus.ServiceInvoker"));
	private final ExecutorService qWatcher = Executors
			.newSingleThreadExecutor(new DaemonThreadFactory(
					"MessageBus.QueueWatcher"));

	public MessageBusBase(BlockingQueue<Message> newArrivalsQ,
			BlockingQueue<Message> toConsumeQ) {
		this.newArrivalsQ = newArrivalsQ;
		this.toConsumeQ = toConsumeQ;

		final Runnable messageConsumerThread = new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						consumeMessage(MessageBusBase.this.toConsumeQ.take());
					} catch (InterruptedException e) {
						if (logger.isWarnEnabled()) {
							logger.warn("The message consumer thread of message bus has died due to interruption. No more messages will be delivered.",
											e);
						}
						return;
					}
				}
			}
		};
		qWatcher.submit(messageConsumerThread);
	}

	private void consumeMessage(final Message msg) {
		if (msg instanceof MessageAddressed) {
			MessageAddressed addressedMessage = (MessageAddressed) msg;
			String destination = addressedMessage.getDestination();
			final ServiceDataConnector destinationConnector = mappings
					.get(destination);
			if (destinationConnector == null) {
				logger.error("Destination '{}' does not exist", destination);
				// FIXME: Return an error instead, not an exception
				throw new RuntimeException("Destination " + destination
						+ " does not exist");
			}

			sendToDestinationConnector(destinationConnector, msg);
		} else if (msg instanceof MessageNotification) {
			MessageNotification notification = (MessageNotification) msg;
			Collection<ServiceDataConnector> connectors = mappings.values();
			// FIXME: We need to have a way to do pub/sub based on topic. This
			// is very inefficient, especially because we are using
			// threads. Best way to do this is to let services subscribe to
			// the topics they are interested in...
			for (ServiceDataConnector connector : connectors) {
				if (!connector.getService().getServiceName().equals(
						notification.getSource())) {
					sendToDestinationConnector(connector, notification);
				}
			}
		} else {
			// FIXME: Fix this. Basically, log it and notify authorities :)
		}
	}

	private void sendToDestinationConnector(
			final ServiceDataConnector destinationConnector, final Message msg) {
		// Make sure the thing that receives does not throw! It should
		// send an error instead, and the connector that receives will throw
		// the
		// exception on behalf.
		serviceCallExecutor.submit(new Runnable() {

			@Override
			public void run() {
				try {
					if (logger.isTraceEnabled()) {
						logger.trace("Delivering message: {} to service ''",
								msg, destinationConnector.getService()
										.getServiceName());
					}
					destinationConnector.receiveMessage(msg);
				} catch (Throwable e) {
					if (msg instanceof MessageRequest) {
						deliverErrorIfIsRequest(msg,
								"Service threw exception upon request", e);
					} else {
						// Don't do anything (it was either a response or
						// notification given to the service, so it should
						// really not throw), just log it...
						if (logger.isWarnEnabled()) {
							logger.warn("Service '"
													+ destinationConnector
															.getService()
															.getServiceName()
													+ "' threw exception. "
													+ "This should not have happened, so ignoring the exception.",
											e);
						}
					}
				}

			}
		});
	}

	@Override
	public void deliverMessage(Message msg) {
		try {
			if (logger.isTraceEnabled()) {
				logger.trace("Message delivery requested. The message is: {}",
						msg);
			}
			newArrivalsQ.put(msg);
		} catch (InterruptedException e) {
			if (logger.isErrorEnabled()) {
				logger.error("Delivery of message '" + msg.toString()
						+ "' failed due to exception.", e);
			}
			deliverErrorIfIsRequest(msg, "Cannot queue request on message bus",
					e);
		}
	}

	@Override
	public void addServiceDataConnector(String serviceName,
			ServiceDataConnector connector) {
		// FIXME: Check for null, duplicates etc...
		mappings.put(serviceName, connector);
		connector.setMessageBus(this);
	}

	@Override
	public ServiceDataConnector removeServiceDataConnector(String serviceName) {
		ServiceDataConnector connector = mappings.remove(serviceName);
		if (connector != null) {
			connector.setMessageBus(null);
		}
		return connector;
	}

	@Override
	public Collection<ServiceDataConnector> getServiceDataConnectors() {
		// FIXME: Return a shallow copy of it
		return mappings.values();
	}

	@Override
	public ServiceDataConnector getServiceDataConnector(String serviceName) {
		return mappings.get(serviceName);
	}

	private void deliverErrorIfIsRequest(Message msg, String exceptionMessage,
			Throwable t) {
		if (msg instanceof MessageRequest) {
			MessageError response = RichServiceUtils.errorResponseFor(
					(MessageRequest) msg, exceptionMessage, t);
			if (logger.isTraceEnabled()) {
				logger
						.trace(
								"Error occurred upon request. The message that the error rooted from is: {}, and the error response is: {}",
								msg, response);
				logger.trace("The exception is: ", t);
			}
			MessageBusBase.this.deliverMessage(response);
		}
	}
}
