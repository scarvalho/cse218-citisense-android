package org.sosa.richservice.base;

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
public class ServiceDescriptorBase implements ServiceDescriptor {

	private final Collection<Class> ifaces;
	private String serviceName;
	private final Collection<String> requiredServices = new ArrayList<String>();
	private Iterator<Class> ifaceIter;
	
	public ServiceDescriptorBase(String serviceName, Collection<Class> iface,
			String... requiredServices) {
		this.ifaces = iface;
		this.serviceName = serviceName;
		if (requiredServices != null && requiredServices.length > 0) {
			this.requiredServices.addAll(Arrays.asList(requiredServices));
		}
	}

	@SuppressWarnings("unchecked")
	public ServiceDescriptorBase(String serviceName, Class iface,
			String... requiredServices) {
		this(serviceName, Arrays.asList(iface), requiredServices);
	}
	
	public Class getServiceClass(String operation, Class[] parameterTypes) {
		Class result = null;
		for (Class cls : ifaces) {
			try {
				Method method = cls.getMethod(operation, parameterTypes);
				result = cls;
			} catch (NoSuchMethodException e) {
				//Try next Method
			}
		}
		return result;		
	}
	
	public void initInterfaceIterator()
	{
		ifaceIter = ifaces.iterator();		
	}
	public Class getNextInterface()
	{
		if (ifaceIter.hasNext())
		{
			return ifaceIter.next();
		}
		else
		{
			return null;
		}
	}
	
	public Method getServiceMethod(String operation, Class[] parameterTypes)
	{
		// Find the method signature from the service interface
		
		Method methodToCall = null;
		for (Class<Object> cls : ifaces) 
		{
			try {
				methodToCall = cls.getMethod(operation, parameterTypes);
				break; // stop immediately when found
			} catch (NoSuchMethodException e) {
				// not this one, maybe next one
			};
		}
		return methodToCall;
	
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
