package org.sosa.richservice.base.servicedataconnector;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sosa.richservice.Message;
import org.sosa.richservice.MessageAddressed;
import org.sosa.richservice.MessageBus;
import org.sosa.richservice.MessageError;
import org.sosa.richservice.MessageNotification;
import org.sosa.richservice.MessageRequest;
import org.sosa.richservice.MessageResponse;
import org.sosa.richservice.ServiceDataConnector;
import org.sosa.richservice.ServiceDescriptorLocal;
import org.sosa.richservice.base.MessageCorrelator;
import org.sosa.richservice.base.MessageErrorBase;
import org.sosa.richservice.base.MessageResponseBase;
import org.sosa.richservice.base.ServiceDescriptorLocalBase;
import org.sosa.richservice.utils.richservice.RichServiceUtils;

/**
 * 
 * @author celal.ziftci
 * 
 */
public class ServiceDataConnectorJavaLocal implements
		ServiceDataConnector<ServiceDescriptorLocal> {

	private ServiceDescriptorLocal service;
	private MessageBus bus;
	private final MessageCorrelator<String, Message> messageCorrelator = new MessageCorrelator<String, Message>();

	private final Logger logger = LoggerFactory
			.getLogger(ServiceDataConnectorJavaLocal.class);

	@Override
	public void setService(ServiceDescriptorLocal service) {
		this.service = service;
	}

	@Override
	public void setMessageBus(MessageBus bus) {
		this.bus = bus;
	}

	@Override
	public ServiceDescriptorLocal getService() {
		return service;
	}

	@Override
	public MessageBus getMessageBus() {
		return bus;
	}

	public void sendMessage(Message message) {
		bus.deliverMessage(message);
	}

	@Override
	public Object sendMessage(Message message, int timeout) {
		if (message instanceof MessageAddressed) {
			MessageAddressed addressedMessage = (MessageAddressed) message;
			if (message instanceof MessageRequest) {
				logger.trace("Request from " + addressedMessage.getSource()
						+ " to " + addressedMessage.getDestination());
			} else if (message instanceof MessageResponse) {
				logger.trace("Response from " + addressedMessage.getSource()
						+ " to " + addressedMessage.getDestination());
			}
		}

		if (message instanceof MessageRequest) {
			// TODO: They can send other types of messages, like notifications
			// for
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
			} catch (InterruptedException e) {
				throw new RuntimeException(
						"Interrupted waiting for response to a request", e);
			}

			if (objectReceived instanceof MessageError) {
				Throwable error = (Throwable) ((MessageError) objectReceived)
						.getResponse();
				// This is not a RuntimeException necessarily, it can be other
				// exceptions allowed by method signature... If
				// runtimeexception, let it go.
				// FIXME: I put the if runtimeexception let it go part later on,
				// so it needs some more thinking and testing.
				if (error instanceof RuntimeException) {
					throw (RuntimeException) error;
				} else {
					throw new RuntimeException("Error received ", error);
				}
			} else if (objectReceived instanceof MessageResponse) {
				return ((MessageResponse) objectReceived).getResponse();
			} else {
				throw new RuntimeException(
						"Received a message that is not a response or error upon a request message: "
								+ objectReceived);
			}
		} else {
			// TODO: There can be other message types here
			return null;
		}
	}

	@Override
	public void receiveMessage(Message msg) {
		if (msg instanceof MessageRequest) {
			MessageRequest request = (MessageRequest) msg;
			logger.trace("Request from " + msg.getSource() + " to "
					+ request.getDestination());

			// Find the correct API to call on the service
			try {

				// Find the method signature from the service interface				
				Method methodToCall = service.getServiceMethod(request.getOperation(), request.getOperationParameterTypes());
				
				// Make the request on the actual service implementation
				Object result = ((ServiceDescriptorLocalBase) service).invokeMethodOnImpl(methodToCall, request
						.getOperationParameterValues());

				bus.deliverMessage(new MessageResponseBase(service
						.getServiceName(), msg.getSource(), RichServiceUtils
						.generateMessageId(), msg.getMessageId(), result));

			} catch (InvocationTargetException e) {
				String error = RichServiceUtils.throwableToString(e.getCause());
				bus.deliverMessage(new MessageErrorBase(service
						.getServiceName(), msg.getSource(), RichServiceUtils
						.generateMessageId(), msg.getMessageId(), error, e
						.getCause()));
			} catch (Throwable e) {
				String error = RichServiceUtils.throwableToString(e);
				bus.deliverMessage(new MessageErrorBase(service
						.getServiceName(), msg.getSource(), RichServiceUtils
						.generateMessageId(), msg.getMessageId(), error, e));
			}
		} else if (msg instanceof MessageResponse) {
			MessageResponse response = (MessageResponse) msg;
			logger.trace("Response from " + msg.getSource() + " to "
					+ response.getDestination());

			// messageCorrelator.responseReceived(((MessageResponse) msg)
			// .getCorrelationId(), ((MessageResponse) msg).getResponse());
			messageCorrelator
					.responseReceived(response.getCorrelationId(), msg);
		} else if (msg instanceof MessageNotification) {
			// FIXME: Do something?? Probably just ignore..
		} else {
			throw new IllegalArgumentException("Unknown message type '"
					+ msg.getClass() + "'");
		}
	}
}
