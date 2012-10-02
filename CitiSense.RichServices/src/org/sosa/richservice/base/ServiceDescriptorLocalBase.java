package org.sosa.richservice.base;

import java.util.Collection;

import org.sosa.richservice.ServiceDescriptorLocal;

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
}
