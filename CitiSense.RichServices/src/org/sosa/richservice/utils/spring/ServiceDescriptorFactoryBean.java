package org.sosa.richservice.utils.spring;

import java.util.ArrayList;
import java.util.Collection;

import org.sosa.richservice.ServiceDescriptor;
import org.sosa.richservice.base.ServiceDescriptorBase;
import org.sosa.richservice.base.ServiceDescriptorLocalBase;
import org.springframework.beans.factory.FactoryBean;

/**
 * 
 * @author celal.ziftci
 * 
 */
public class ServiceDescriptorFactoryBean implements
		FactoryBean<ServiceDescriptor> {

	private String name;
	private Class serviceInterface;
	private Collection<Class> serviceInterfaces;

	private Class implementation;
	private Object implementationObject;

	private Collection<String> dependencies;

	public Object getImplementationObject() {
		return implementationObject;
	}

	public void setImplementationObject(Object implementationObject) {
		this.implementationObject = implementationObject;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Class getInterface() {
		return serviceInterface;
	}

	public void setInterface(Class serviceInterface) {
		this.serviceInterface = serviceInterface;
	}

	public Collection<Class> getInterfaces() {
		return serviceInterfaces;
	}

	public void setInterfaces(Collection<Class> interfaces) {
		this.serviceInterfaces = interfaces;
	}

	public Class<?> getImplementation() {
		return implementation;
	}

	public void setImplementation(Class implementation) {
		this.implementation = implementation;
	}

	public Collection<String> getDependencies() {
		return dependencies;
	}

	public void setDependencies(Collection<String> dependencies) {
		this.dependencies = dependencies;
	}

	@Override
	public ServiceDescriptor getObject() throws Exception {
		// TODO: Make some sanity checks!!!

		if (implementation == null && implementationObject == null) {
			return new ServiceDescriptorBase(name, serviceInterface);
		} else {
			Collection<Class> interfaces;
			if (this.serviceInterfaces != null
					&& this.serviceInterfaces.size() > 0) {
				interfaces = serviceInterfaces;
			} else if (serviceInterface != null) {
				interfaces = new ArrayList<Class>();
				interfaces.add(this.serviceInterface);
			} else {
				interfaces = new ArrayList<Class>();
			}

			Object object = null;
			if (implementationObject != null) {
				object = implementationObject;
			} else {
				// implementingObject = Class.forName(implementation);
				object = implementation.newInstance();
			}
			// local
			if (dependencies != null) {
				return new ServiceDescriptorLocalBase(name, interfaces, object,
						dependencies.toArray(new String[dependencies.size()]));
			} else {
				return new ServiceDescriptorLocalBase(name, interfaces, object);
			}
		}
	}

	@Override
	public Class getObjectType() {
		return ServiceDescriptor.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
