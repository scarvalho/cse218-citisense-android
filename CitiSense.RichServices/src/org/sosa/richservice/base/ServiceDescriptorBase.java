package org.sosa.richservice.base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.sosa.richservice.ServiceDescriptor;

/**
 * 
 * @author celal.ziftci
 * 
 */
public class ServiceDescriptorBase implements ServiceDescriptor {

	private final Collection<Class> ifaces;
	private String serviceName;
	private final Collection<String> requiredServices = new ArrayList<String>();

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

	@Override
	public Collection<Class> getExposedInterface() {
		return ifaces;
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
