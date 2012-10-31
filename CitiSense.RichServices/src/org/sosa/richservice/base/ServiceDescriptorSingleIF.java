package org.sosa.richservice.base;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;

import org.sosa.richservice.ServiceDataConnector;
import org.sosa.richservice.ServiceDescriptor;
import org.sosa.richservice.ServiceDescriptorLocal;
import org.sosa.richservice.utils.richservice.ServiceProxy;

/**
 * 
 * @author celal.ziftci
 * 
 */
public class ServiceDescriptorSingleIF extends ServiceDescriptorBase
{


	private final Class iface;
	private boolean isIterReset = true;
	public ServiceDescriptorSingleIF(String serviceName,
			Class iface, Object impl, String... requiredServices) {
		super(serviceName, impl, requiredServices);
		this.iface = iface;
		
	}

	public Class getServiceClass(String operation, Class[] parameterTypes) {
		Class result = null;
		try {
			Method method = iface.getMethod(operation, parameterTypes);
			result = iface;
		} catch (NoSuchMethodException e) {
			result = null;
		}	
		return result;		
	}
	
	public Method getServiceMethod(String operation, Class[] parameterTypes)
	{
		// Find the method signature from the service interface	
		Method methodToCall = null;
		try {
			methodToCall = iface.getMethod(operation, parameterTypes);
			// stop immediately when found
			} catch (NoSuchMethodException e) {
			// not this one, maybe next one
			};
		return methodToCall;
	}	
	
	public void instantiateStubs(ServiceDescriptorLocal requiredService, ServiceDataConnector<ServiceDescriptor> connector, String requiredServiceName) throws Exception
	{
		requiredService.initInterfaceIterator();
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

	@Override
	public void initInterfaceIterator() {
		isIterReset = true;
		
	}

	@Override
	public Class getNextInterface() {		
		if (isIterReset) 
		{
			isIterReset = false;
			return iface;
		}
		else return null;
	}
	

}
