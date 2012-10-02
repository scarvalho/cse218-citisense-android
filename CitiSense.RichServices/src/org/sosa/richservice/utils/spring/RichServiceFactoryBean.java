package org.sosa.richservice.utils.spring;

import java.util.ArrayList;
import java.util.Collection;

import org.sosa.richservice.Policy;
import org.sosa.richservice.RichService;
import org.sosa.richservice.ServiceDataConnector;
import org.sosa.richservice.ServiceDataConnectorForRichService;
import org.sosa.richservice.ServiceDescriptor;
import org.sosa.richservice.utils.richservice.RichServiceBuilder;
import org.springframework.beans.factory.FactoryBean;

/**
 * 
 * @author celal.ziftci
 * 
 */
public class RichServiceFactoryBean implements FactoryBean<RichService> {

	ServiceDataConnectorForRichService<ServiceDescriptor> connector;
	Collection<ServiceDataConnector<ServiceDescriptor>> serviceDataConnectors;

	public Collection<ServiceDataConnector<ServiceDescriptor>> getServiceDataConnectors() {
		return serviceDataConnectors;
	}

	public void setServiceDataConnectors(
			Collection<ServiceDataConnector<ServiceDescriptor>> serviceDataConnectors) {
		this.serviceDataConnectors = serviceDataConnectors;
	}

	Collection<ServiceDescriptor> descriptors;
	Collection<Policy> policies;

	public Collection<Policy> getPolicies() {
		return policies;
	}

	public void setPolicies(Collection<Policy> policies) {
		this.policies = policies;
	}

	public void setServiceDataConnectorForRichService(
			ServiceDataConnectorForRichService<ServiceDescriptor> connector) {
		this.connector = connector;
	}

	public void setServiceDescriptors(Collection<ServiceDescriptor> descriptors) {
		this.descriptors = descriptors;
	}

	@Override
	public RichService getObject() throws Exception {
		RichService richService = null;
		Collection<ServiceDescriptor> allDescriptors = new ArrayList<ServiceDescriptor>();
		allDescriptors.addAll(descriptors);
		if (serviceDataConnectors != null) {
			for (ServiceDataConnector aConnector : this.serviceDataConnectors) {
				allDescriptors.add(aConnector.getService());
			}
		}

		if (connector == null) {
			richService = RichServiceBuilder.buildRichService(allDescriptors
					.toArray(new ServiceDescriptor[allDescriptors.size()]));
		} else {
			richService = RichServiceBuilder.buildRichService(connector,
					allDescriptors.toArray(new ServiceDescriptor[allDescriptors
							.size()]));
		}
		if (serviceDataConnectors != null) {
			for (ServiceDataConnector aConnector : this.serviceDataConnectors) {
				richService.addServiceDataConnector(aConnector.getService()
						.getServiceName(), aConnector);
			}
		}

		if (policies != null) {
			RichServiceBuilder.addPoliciesTo(richService, policies
					.toArray(new Policy[0]));
		}

		return richService;
	}

	@Override
	public Class<?> getObjectType() {
		return RichService.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
