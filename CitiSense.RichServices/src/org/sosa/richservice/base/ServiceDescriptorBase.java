package org.sosa.richservice.base;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.sosa.richservice.ServiceDescriptor;
import org.sosa.richservice.utils.richservice.ServiceProxy;

/**
 * 
 * @author celal.ziftci
 * 
 */
public abstract class ServiceDescriptorBase implements ServiceDescriptor {

	
	private String serviceName;
	private final Collection<String> requiredServices = new ArrayList<String>();
	private final Object impl;
	
	public ServiceDescriptorBase(String serviceName, Object impl, String... requiredServices) {

		this.serviceName = serviceName;
		this.impl = impl;
		if (requiredServices != null && requiredServices.length > 0) {
			this.requiredServices.addAll(Arrays.asList(requiredServices));
		}
	}
	
	public Object getExposedImplementation() {
		return impl;
	}
	
	public Object invokeMethodOnImpl(Method methodToCall, Object[] parameterValues) throws InvocationTargetException, IllegalAccessException 
	{
		// Make the request on the actual service implementation
		Object result = methodToCall.invoke(impl, parameterValues);

		return result;
	}	
	

	
	

	/**
	 * Returns a shallow copy of the internal list.
	 */
	@Override
	public List<String> getRequiredServices() {
		return new ArrayList<String>(requiredServices);
	}

	@Override
	public String getServiceName() {
		return serviceName;
	}

	@Override
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

}
