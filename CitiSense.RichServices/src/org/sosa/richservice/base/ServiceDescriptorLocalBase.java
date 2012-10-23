package org.sosa.richservice.base;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;

import org.sosa.richservice.ServiceDataConnector;
import org.sosa.richservice.ServiceDescriptorLocal;
import org.sosa.richservice.utils.richservice.ServiceProxy;

/**
 * 
 * @author celal.ziftci
 * 
 */
public class ServiceDescriptorLocalBase extends ServiceDescriptorBase implements
		ServiceDescriptorLocal {

	private final Object impl;

	public ServiceDescriptorLocalBase(String serviceName,
			Collection<Class> iface, Object impl, String... requiredServices) {
		super(serviceName, iface, requiredServices);
		this.impl = impl;
	}

	public ServiceDescriptorLocalBase(String serviceName, Class iface,
			Object impl, String... requiredServices) {
		super(serviceName, iface, requiredServices);
		this.impl = impl;
	}

	@Override
	public Object getExposedImplementation() {
		return impl;
	}
	
	public void instantiateStubs(ServiceDescriptorLocal requiredService, ServiceDataConnector<ServiceDescriptorLocal> connector, String requiredServiceName) throws Exception
	{
		requiredService.initInterfaceIterator();
		Class iface;
		while ((iface = requiredService.getNextInterface()) != null)
		{
			Object serviceStub = ServiceProxy.proxy(iface, connector,
					requiredServiceName);
			String setterName = "set" + iface.getSimpleName();
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
	
	public Object invokeMethodOnImpl(Method methodToCall, Object[] parameterValues) throws InvocationTargetException, IllegalAccessException 
	{
		// Make the request on the actual service implementation
		Object result = methodToCall.invoke(impl, parameterValues);

		return result;
	}
}
