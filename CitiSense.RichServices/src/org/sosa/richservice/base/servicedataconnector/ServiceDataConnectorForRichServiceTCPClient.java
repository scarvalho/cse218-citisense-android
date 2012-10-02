package org.sosa.richservice.base.servicedataconnector;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.UnknownHostException;
import java.util.Collection;

import org.sosa.richservice.Message;
import org.sosa.richservice.MessageBus;
import org.sosa.richservice.MessageRequest;
import org.sosa.richservice.MessageResponse;
import org.sosa.richservice.ServiceDataConnectorForRichService;
import org.sosa.richservice.ServiceDescriptor;
import org.sosa.richservice.base.MessageErrorBase;
import org.sosa.richservice.base.MessageRequestBase;
import org.sosa.richservice.base.MessageResponseBase;
import org.sosa.richservice.utils.richservice.RichServiceUtils;
import org.sosa.richservice.utils.tcp.TcpClient;

/**
 * 
 * @author celal.ziftci
 * 
 */
public class ServiceDataConnectorForRichServiceTCPClient implements
		ServiceDataConnectorForRichService<ServiceDescriptor> {

	private MessageBus bus;
	private ServiceDescriptor externalServiceDescriptor;
	private final String host;
	private final int port;
	private final int connectTimeout;

	public ServiceDataConnectorForRichServiceTCPClient(
			ServiceDescriptor externalServiceDescriptor, String host, int port) {
		this(externalServiceDescriptor, host, port, 0);
	}

	public ServiceDataConnectorForRichServiceTCPClient(
			ServiceDescriptor externalServiceDescriptor, String host, int port,
			int connectTimeout) {
		this.externalServiceDescriptor = externalServiceDescriptor;
		this.host = host;
		this.port = port;
		this.connectTimeout = connectTimeout;
	}

	@Override
	public void sendMessage(Message request) {
		// Should never be called...
	}

	@Override
	public Object sendMessage(Message message, int timeout) {
		// Should never be called...
		return null;
	}

	@Override
	public void receiveMessage(Message msg) {
		if (msg instanceof MessageRequest) {
			MessageRequest request = (MessageRequest) msg;
			String operation = request.getOperation();
			Class[] paramTypes = request.getOperationParameterTypes();
			Class requestedInterface = findServiceInterface(request);
			TcpClient tcpClient = null;
			try {
				tcpClient = new TcpClient(host, port, connectTimeout);
				// FIXME: are you sure that this will always be a response
				// message??
				// Send the message such that it looks like it is going from
				// this service
				MessageRequest newRequest = new MessageRequestBase(
						externalServiceDescriptor.getServiceName(),
						requestedInterface.getName(), msg.getMessageId(),
						((MessageRequest) msg).getOperation(),
						((MessageRequest) msg).getOperationParameterTypes(),
						((MessageRequest) msg).getOperationParameterValues());

				MessageResponse responseFromUp = (MessageResponse) tcpClient
						.sendMessage(newRequest);

				// Re-write the message so that now it looks like it is being
				// responded by this service
				bus.deliverMessage(new MessageResponseBase(
						externalServiceDescriptor.getServiceName(), msg
								.getSource(), responseFromUp.getMessageId(),
						msg.getMessageId(), responseFromUp.getResponse()));
			} catch (UnknownHostException e) {
				bus.deliverMessage(new MessageErrorBase(
						externalServiceDescriptor.getServiceName(), msg
								.getSource(), RichServiceUtils
								.generateMessageId(), msg.getMessageId(),
						"Cannot connect to the host '" + host
								+ "', it is unknown", e));
			} catch (IOException e) {
				bus.deliverMessage(new MessageErrorBase(
						externalServiceDescriptor.getServiceName(), msg
								.getSource(), RichServiceUtils
								.generateMessageId(), msg.getMessageId(),
						"Connection cannot be opened or it is broken", e));
			} catch (Throwable e) {
				bus.deliverMessage(new MessageErrorBase(
						externalServiceDescriptor.getServiceName(), msg
								.getSource(), RichServiceUtils
								.generateMessageId(), msg.getMessageId(),
						"Unknown exception", e));
			} finally {
				// TODO: This is inefficient, we should use some kind of pooling
				// maybe?
				if (tcpClient != null) {
					tcpClient.close();
				}
			}
		}
		// FIXME: This connector does not expose any interfaces to the upper
		// world yet. If it does one day, we will think about it...
	}

	private Class findServiceInterface(MessageRequest request) {
		Class iface = null;
		Collection<Class> interfaces = this.getService().getExposedInterface();

		for (Class cls : interfaces) {
			try {
				Method method = cls.getMethod(request.getOperation(), request
						.getOperationParameterTypes());
				iface = cls;
			} catch (Exception e) {
				// FIXME: Make sure this is smth like nosuchmethodexception or
				// security exception. Other ones are legit
			}
		}
		return iface;
	}

	@Override
	public ServiceDescriptor getService() {
		return externalServiceDescriptor;
	}

	@Override
	public void setService(ServiceDescriptor service) {
		this.externalServiceDescriptor = service;
	}

	@Override
	public MessageBus getMessageBus() {
		return bus;
	}

	@Override
	public void setMessageBus(MessageBus bus) {
		this.bus = bus;
	}
}
