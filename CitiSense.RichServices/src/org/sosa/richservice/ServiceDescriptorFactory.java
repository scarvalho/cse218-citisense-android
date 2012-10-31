package org.sosa.richservice;

import java.util.Arrays;
import java.util.Collection;

import org.sosa.richservice.base.ServiceDescriptorSingleIF;
import org.sosa.richservice.base.ServiceDescriptorMultiIF;

public class ServiceDescriptorFactory {

	public static ServiceDescriptor createServiceDescriptor(String serviceName,
			Collection<Class> ifaces, Object impl, String... requiredServices)
	{
		return new ServiceDescriptorMultiIF(serviceName, ifaces, impl, requiredServices);
	}

	public static ServiceDescriptor createServiceDescriptor(String serviceName,
			Class iface, Object impl, String... requiredServices )
	{
		return new ServiceDescriptorMultiIF(serviceName, Arrays.asList(iface), impl, requiredServices);
	}	
	
	public static ServiceDescriptor createServiceDescriptor(boolean multiIFImplementation, String serviceName,
			Class iface, Object impl, String... requiredServices )
	{
		if (multiIFImplementation)
			return new ServiceDescriptorMultiIF(serviceName, Arrays.asList(iface), impl, requiredServices);
		else
			return new ServiceDescriptorSingleIF(serviceName, iface, impl, requiredServices);
	}	
		
}
