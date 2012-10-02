package org.sosa.richservice.utils.spring;

import org.sosa.richservice.ServiceDataConnector;
import org.sosa.richservice.utils.richservice.ServiceProxy;
import org.springframework.beans.factory.FactoryBean;

/**
 * 
 * @author celal.ziftci
 * 
 */
@SuppressWarnings("unchecked")
public class ServiceProxyFactoryBean<T> implements FactoryBean<T> {

	private Class<T> serviceInterface;
	private ServiceDataConnector serviceDataConnector;
	private String serviceCalled;

	public Class<T> getServiceInterface() {
		return serviceInterface;
	}

	public void setServiceInterface(Class<T> serviceInterface) {
		this.serviceInterface = serviceInterface;
	}

	public ServiceDataConnector getServiceDataConnector() {
		return serviceDataConnector;
	}

	public void setServiceDataConnector(
			ServiceDataConnector serviceDataConnector) {
		this.serviceDataConnector = serviceDataConnector;
	}

	public String getServiceCalled() {
		return serviceCalled;
	}

	public void setServiceCalled(String serviceCalled) {
		this.serviceCalled = serviceCalled;
	}

	@Override
	public T getObject() throws Exception {
		return (T) ServiceProxy.proxy(serviceInterface, serviceDataConnector,
				serviceCalled);
	}

	@Override
	public Class<?> getObjectType() {
		return Object.class;
	}

	@Override
	public boolean isSingleton() {
		return false;
	}

}
