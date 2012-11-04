package org.sosa.richservice.utils.richservice;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.sosa.richservice.MessageRequest;
import org.sosa.richservice.ServiceDataConnector;
import org.sosa.richservice.ServiceDescriptorLocal;
import org.sosa.richservice.base.MessageRequestBase;

/**
 * Creates a proxy for a service type which makes calls to serviceDataConnector.
 * 
 * @author celal.ziftci
 * 
 */
public class ServiceProxy implements InvocationHandler {

	private final ServiceDataConnector<ServiceDescriptorLocal> connector;
	private final String serviceBeingCalled;

	@SuppressWarnings("unchecked")
	public static <T> T proxy(Class<T> cls,
			ServiceDataConnector<ServiceDescriptorLocal> connector,
			String requiredServiceName) {

		return (T) Proxy.newProxyInstance(cls.getClassLoader(),
				new Class[] { cls }, new ServiceProxy(connector,
						requiredServiceName));
	}

	private ServiceProxy(
			ServiceDataConnector<ServiceDescriptorLocal> connector,
			String serviceName) {
		this.connector = connector;
		this.serviceBeingCalled = serviceName;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {

		MessageRequest message = new MessageRequestBase(connector.getService()
				.getServiceName(), serviceBeingCalled, RichServiceUtils
				.generateMessageId(), method.getName(), method
				.getParameterTypes(), args);

		return connector.sendMessage(message, -1);
	}
}