package org.sosa.richservice.base.servicedataconnector;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sosa.richservice.Message;
import org.sosa.richservice.MessageAddressed;
import org.sosa.richservice.MessageBus;
import org.sosa.richservice.MessageError;
import org.sosa.richservice.MessageRequest;
import org.sosa.richservice.ServiceDataConnector;
import org.sosa.richservice.ServiceDescriptor;
import org.sosa.richservice.base.MessageCorrelator;
import org.sosa.richservice.base.MessageResponseBase;
import org.sosa.richservice.utils.richservice.RichServiceUtils;

/**
 * 
 * @author celal.ziftci
 * 
 */
public class ServiceDataConnectorJMSBusClient implements
		ServiceDataConnector<ServiceDescriptor> {

	private ServiceDescriptor service;
	private MessageBus bus;
	private final MessageCorrelator<String, MessageAddressed> messageCorrelator = new MessageCorrelator<String, MessageAddressed>();
	private Object jmsHandle;
	private final Logger logger = LoggerFactory
			.getLogger(ServiceDataConnectorJMSBusClient.class);

	@Override
	public void setService(ServiceDescriptor service) {
		this.service = service;
	}

	@Override
	public void setMessageBus(MessageBus bus) {
		this.bus = bus;
	}

	public Object getJmsHandle() {
		return jmsHandle;
	}

	public void setJmsHandle(Object jmsHandle) {
		this.jmsHandle = jmsHandle;
	}

	@Override
	public ServiceDescriptor getService() {
		return service;
	}

	@Override
	public MessageBus getMessageBus() {
		return bus;
	}

	@Override
	public Object sendMessage(Message request, int timeout) {
		// FIXME: Not implemented yet...
		return null;
	}

	@Override
	public void sendMessage(Message request) {
		// FIXME: Not implemented yet...
	}

	@Override
	public void receiveMessage(Message msg) {
		if (msg instanceof MessageRequest) {
			MessageRequest request = (MessageRequest) msg;
			try {
				Method method = RichServiceUtils.findMethodOn(jmsHandle
						.getClass(), request);
				Object result = method.invoke(jmsHandle, request
						.getOperationParameterValues());
				bus.deliverMessage(new MessageResponseBase(service
						.getServiceName(), msg.getSource(), RichServiceUtils
						.generateMessageId(), msg.getMessageId(), result));
			} catch (Throwable e) {
				MessageError error = RichServiceUtils.errorResponseFor(request,
						"Exception on handling request", e);
				bus.deliverMessage(error);
			}
		} else {
			MessageError error = RichServiceUtils.errorResponseFor(msg,
					"Unimplemented message type '" + msg.getClass() + "'",
					new Exception("Unimplemented message type '"
							+ msg.getClass() + "'"));
			bus.deliverMessage(error);
		}
	}
}
