package org.sosa.richservice.base;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.sosa.richservice.Message;
import org.sosa.richservice.MessageBus;
import org.sosa.richservice.MessageInterceptor;
import org.sosa.richservice.Policy;
import org.sosa.richservice.RichService;
import org.sosa.richservice.ServiceDataConnector;

/**
 * 
 * @author celal.ziftci
 * 
 */
public class RichServiceBase implements RichService {

	private MessageBus messageBus;
	private MessageInterceptor messageInterceptor;
	private String nameOfServiceDataConnectorForRichService;

	public RichServiceBase() {
		BlockingQueue<Message> newMessagesQ = new LinkedBlockingQueue<Message>();
		BlockingQueue<Message> interceptedMessagesQ = new LinkedBlockingQueue<Message>();
		messageBus = new MessageBusBase(newMessagesQ, interceptedMessagesQ);
		messageInterceptor = new MessageInterceptorBase(newMessagesQ,
				interceptedMessagesQ);
	}

	public RichServiceBase(MessageBus bus, MessageInterceptor interceptor) {
		messageBus = bus;
		messageInterceptor = interceptor;
	}

	@Override
	public MessageBus getMessageBus() {
		return messageBus;
	}

	@Override
	public MessageInterceptor getMessageInterceptor() {
		return messageInterceptor;
	}

	@Override
	public void addServiceDataConnector(String serviceName,
			ServiceDataConnector connector) {
		messageBus.addServiceDataConnector(serviceName, connector);
	}

	@Override
	public Collection<ServiceDataConnector> getServiceDataConnectors() {
		return messageBus.getServiceDataConnectors();
	}

	@Override
	public ServiceDataConnector removeServiceDataConnector(String name) {
		return messageBus.removeServiceDataConnector(name);
	}

	@Override
	public void addPolicy(String policyName, Policy policy) {
		messageInterceptor.addPolicy(policyName, policy);
	}

	@Override
	public Policy removePolicy(String policyName) {
		return messageInterceptor.removePolicy(policyName);
	}

	@Override
	public ServiceDataConnector getServiceDataConnectorForRichService() {
		return messageBus
				.getServiceDataConnector(nameOfServiceDataConnectorForRichService);
	}

	@Override
	public void setServiceDataConnectorForRichService(String name,
			ServiceDataConnector service) {
		messageBus.addServiceDataConnector(name, service);
		nameOfServiceDataConnectorForRichService = name;
	}

	@Override
	public ServiceDataConnector getServiceDataConnector(String serviceName) {
		return messageBus.getServiceDataConnector(serviceName);
	}

}
