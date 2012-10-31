package org.sosa.richservice.base;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.sosa.richservice.ServiceDataConnector;
import org.sosa.richservice.ServiceDescriptor;
import org.sosa.richservice.ServiceDescriptorLocal;
import org.sosa.richservice.utils.richservice.ServiceProxy;

public class ServiceDescriptorMultiIF extends ServiceDescriptorBase {

	private final Collection<Class> ifaces;
	private Iterator<Class> ifaceIter;
	
	public ServiceDescriptorMultiIF(String serviceName,
			Collection<Class> iface, Object impl, String... requiredServices) {
		super(serviceName, impl, requiredServices);
		this.ifaces = iface;
		// TODO Auto-generated constructor stub
	}

	public ServiceDescriptorMultiIF(String serviceName, Class iface, Object impl,
			String... requiredServices) {
		super(serviceName, impl, requiredServices);
		ifaces = Arrays.asList(iface);
		// TODO Auto-generated constructor stub
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
	
	public void instantiateStubs(ServiceDescriptorLocal requiredService, ServiceDataConnector<ServiceDescriptor> connector, String requiredServiceName) throws Exception
	{
		requiredService.initInterfaceIterator();
		Class iface;
		while ((iface = requiredService.getNextInterface()) != null)
		{
			Object serviceStub = ServiceProxy.proxy(iface, connector,
					requiredServiceName);
			String setterName = "set" + iface.getSimpleName();
			Object impl = super.getExposedImplementation();
			
			try {
				Method serviceSetter = impl.getClass().getMethod(
								setterName, new Class[] { iface });
				serviceSetter.invoke(impl, serviceStub);
			} catch (SecurityException e) {
				throw new Exception("Exception using setter '"
						+ setterName
						+ "' in class '"
						+ impl.getClass() + "'");
			} catch (NoSuchMethodException e) {
				throw new Exception("The setter '"
						+ setterName
						+ "' cannot be found in class '"
						+ impl.getClass() + "'");
			} 
		}
	}	

}
