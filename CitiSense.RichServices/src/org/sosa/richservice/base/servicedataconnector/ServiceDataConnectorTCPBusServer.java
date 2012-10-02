package org.sosa.richservice.base.servicedataconnector;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sosa.richservice.Message;
import org.sosa.richservice.MessageAddressed;
import org.sosa.richservice.MessageBus;
import org.sosa.richservice.MessageError;
import org.sosa.richservice.MessageRequest;
import org.sosa.richservice.MessageResponse;
import org.sosa.richservice.ServiceDataConnector;
import org.sosa.richservice.ServiceDescriptor;
import org.sosa.richservice.base.MessageCorrelator;
import org.sosa.richservice.base.MessageRequestBase;
import org.sosa.richservice.base.MessageResponseBase;
import org.sosa.richservice.utils.richservice.RichServiceUtils;
import org.sosa.richservice.utils.tcp.TcpMessageHandler;
import org.sosa.richservice.utils.tcp.TcpServer;

/**
 * 
 * @author celal.ziftci
 * 
 */
public class ServiceDataConnectorTCPBusServer implements
		ServiceDataConnector<ServiceDescriptor> {

	private ServiceDescriptor service;
	private MessageBus bus;
	private final MessageCorrelator<String, MessageAddressed> messageCorrelator = new MessageCorrelator<String, MessageAddressed>();
	private final Map<String, String> serviceNameMappings;

	private final Logger logger = LoggerFactory
			.getLogger(ServiceDataConnectorTCPBusServer.class);

	private class MessageBusUncaughtExceptionHandler implements
			UncaughtExceptionHandler {

		@Override
		public void uncaughtException(Thread t, Throwable e) {
		}

	}

	public ServiceDataConnectorTCPBusServer(int port,
			Map<String, String> serviceNameMappings) throws Exception {
		this.serviceNameMappings = serviceNameMappings;
		new TcpServer(port, 100, new ServiceDataConnectorTcpMessageHandler());
	}

	@Override
	public void setService(ServiceDescriptor service) {
		this.service = service;
	}

	@Override
	public void setMessageBus(MessageBus bus) {
		this.bus = bus;
	}

	@Override
	public ServiceDescriptor getService() {
		return service;
	}

	@Override
	public MessageBus getMessageBus() {
		return bus;
	}

	public void sendMessage(Message request) {
		// FIXME: Implement
	}

	@Override
	public Object sendMessage(Message message, int timeout) {
		logger.trace("ServiceDataConnectorJMSLocal.handleServiceRequest:"
				+ " request from " + service.getServiceName() + " to " + "???");

		// TODO: They can send other types of messages, like notifications for
		// example. Handle that as well...
		// MessageRequest message = new MessageRequestBase(service
		// .getServiceName(), request.getServiceName(), RichServiceUtils
		// .generateMessageId(), request.getOperation(), request
		// .getParameterTypes(), request.getParameterValues());

		// FIXME: Either find a better semantics for this, or make sure you
		// remove the key in case of bus exceptions...
		messageCorrelator.prepareForRequest(message.getMessageId());
		bus.deliverMessage(message);

		Message objectReceived;
		try {
			objectReceived = messageCorrelator.waitForResponse(message
					.getMessageId());
		} catch (Exception e) {
			throw new RuntimeException("Unexpected exception", e);
		}

		if (objectReceived instanceof MessageError) {
			Throwable error = (Throwable) ((MessageError) objectReceived)
					.getResponse();
			// TODO: This is not a RuntimeException necessarily, it can be other
			// exceptions allowed by method signature...
			throw new RuntimeException("Error received ", error);
		} else if (objectReceived instanceof MessageResponse) {
			return ((MessageResponse) objectReceived).getResponse();
		} else {
			throw new RuntimeException(
					"Received a message that is not a response or error upon a request message: "
							+ objectReceived);
		}
	}

	@Override
	public void receiveMessage(Message msg) {
		if (msg instanceof MessageRequest) {
			// TODO: Not implemented as of yet.. In the future, this needs to be
			// implemented so that it makes a call to the remote connector to
			// handle the request
		} else if (msg instanceof MessageResponse) {
			logger.trace("Response from " + msg.getSource() + " to "
					+ ((MessageResponse) msg).getDestination());

			// messageCorrelator.responseReceived(((MessageResponse) msg)
			// .getCorrelationId(), ((MessageResponse) msg).getResponse());
			messageCorrelator.responseReceived(((MessageResponse) msg)
					.getCorrelationId(), ((MessageResponse) msg));
		} else {
			throw new IllegalArgumentException("Unknown message type '"
					+ msg.getClass() + "'");
		}
	}

	private class ServiceDataConnectorTcpMessageHandler implements
			TcpMessageHandler {

		@Override
		public Message handleMessage(Message msg) {
			if (msg instanceof MessageRequest) {
				MessageRequest request = (MessageRequest) msg;

				String destinationRewritten = ServiceDataConnectorTCPBusServer.this.serviceNameMappings
						.get(request.getDestination());
				Object response = ServiceDataConnectorTCPBusServer.this
						.sendMessage(new MessageRequestBase(service
								.getServiceName(), destinationRewritten,
								RichServiceUtils.generateMessageId(), request
										.getOperation(), request
										.getOperationParameterTypes(), request
										.getOperationParameterValues()), -1);
				return new MessageResponseBase(request.getDestination(),
						request.getSource(), RichServiceUtils
								.generateMessageId(), msg.getMessageId(),
						response);
			} else {
				// FIXME: Should not happen for now...
				return null;
			}
		}

	}
}
