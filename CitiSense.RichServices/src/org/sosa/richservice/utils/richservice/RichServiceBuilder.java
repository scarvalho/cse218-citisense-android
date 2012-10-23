package org.sosa.richservice.utils.richservice;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.sosa.richservice.Policy;
import org.sosa.richservice.RichService;
import org.sosa.richservice.ServiceDataConnector;
import org.sosa.richservice.ServiceDataConnectorForRichService;
import org.sosa.richservice.ServiceDescriptor;
import org.sosa.richservice.ServiceDescriptorLocal;
import org.sosa.richservice.base.RichServiceBase;
import org.sosa.richservice.base.servicedataconnector.ServiceDataConnectorJavaLocal;

/**
 * 
 * @author celal.ziftci
 * 
 */
public class RichServiceBuilder {

	public static RichService buildRichService(ServiceDescriptor... services)
			throws Exception {
		// Build a map of types to service-descriptors
		Map<String, ServiceDescriptor> descriptors = getServiceRegistryFrom(services);
		RichService richService = new RichServiceBase();

		// FIXME: Check for duplicate named services etc...
		for (ServiceDescriptor serviceDescriptor : services) {
			if (serviceDescriptor instanceof ServiceDescriptorLocal) {
				ServiceDataConnector connector = localServiceDataConnectorFor(
						(ServiceDescriptorLocal) serviceDescriptor, descriptors);
				richService.addServiceDataConnector(serviceDescriptor
						.getServiceName(), connector);
			}
			// TODO: We don't know how to create connectoes for other services
			// yet...
		}

		return richService;
	}

	public static RichService buildRichService(
			ServiceDataConnectorForRichService externalConnectorDescriptor,
			ServiceDescriptor... services) throws Exception {
		// Build a map of types to service-descriptors
		Map<String, ServiceDescriptor> descriptors = getServiceRegistryFrom(services);
		descriptors.put(externalConnectorDescriptor.getService()
				.getServiceName(), externalConnectorDescriptor.getService());

		RichService richService = new RichServiceBase();

		// FIXME: Check for duplicate named services etc...
		for (ServiceDescriptor serviceDescriptor : services) {
			if (serviceDescriptor instanceof ServiceDescriptorLocal) {
				ServiceDataConnector connector = localServiceDataConnectorFor(
						(ServiceDescriptorLocal) serviceDescriptor, descriptors);
				richService.addServiceDataConnector(serviceDescriptor
						.getServiceName(), connector);
			}
			// TODO: We don't know how to create connectors for other services
			// yet...
		}

		richService.setServiceDataConnectorForRichService(
				externalConnectorDescriptor.getService().getServiceName(),
				externalConnectorDescriptor);

		return richService;
	}

	public static Map<String, ServiceDescriptor> getServiceRegistryFrom(
			ServiceDescriptor... descriptors) {
		Map<String, ServiceDescriptor> map = new HashMap<String, ServiceDescriptor>();
		for (ServiceDescriptor serviceDescriptor : descriptors) {
			map.put(serviceDescriptor.getServiceName(), serviceDescriptor);
		}

		return map;
	}

	public static RichService addPoliciesTo(RichService richService,
			Policy... policies) {
		for (Policy policy : policies) {
			richService.addPolicy(policy.getName(), policy);
		}
		return richService;
	}

	public static ServiceDataConnector localServiceDataConnectorFor(
			ServiceDescriptorLocal serviceDescriptor) throws Exception {
		if (serviceDescriptor.getRequiredServices().size() != 0) {
			throw new Exception(
					"Cannot add this service without a registry, because it has external dependencies");
		}
		return localServiceDataConnectorFor(serviceDescriptor, Collections
				.<String, ServiceDescriptor> emptyMap());
	}

	@SuppressWarnings("unchecked")
	public static ServiceDataConnector localServiceDataConnectorFor(
			ServiceDescriptorLocal serviceDescriptor,
			Map<String, ServiceDescriptor> services) throws Exception {

		/**
		 * For each service: <br/>
		 * 1) Instantiate a connector <br/>
		 * 2) instantiate stubbed implementations of the dependencies of the
		 * service that will merely serialize the call and send to the service
		 * data connector <br/>
		 * 3) Set the dependency instance on the service implementation class
		 */

		ServiceDataConnector connector = new ServiceDataConnectorJavaLocal();

		Collection<String> requiredServices = serviceDescriptor
				.getRequiredServices();
		for (String requiredServiceName : requiredServices) {
			ServiceDescriptorLocal requiredService = (ServiceDescriptorLocal)services
					.get(requiredServiceName);
			
			serviceDescriptor.instantiateStubs(requiredService, connector, requiredServiceName);
		}

		connector.setService(serviceDescriptor);
		return connector;
	}

	private static Map<Class<?>, ServiceDescriptor> serviceDescriptorMapFor(
			ServiceDescriptor... services) {
		Map<Class<?>, ServiceDescriptor> descriptors = new HashMap<Class<?>, ServiceDescriptor>();
		for (ServiceDescriptor serviceDescriptor : services) {
			serviceDescriptor.initInterfaceIterator();
			Class iface;
			while ((iface = serviceDescriptor.getNextInterface()) != null)
			{
				descriptors.put(iface, serviceDescriptor);
			}
		}

		return descriptors;
	}

	public static void setServiceDataConnectorForRichService(RichService rs,
			ServiceDataConnectorForRichService sd) {
		rs.setServiceDataConnectorForRichService(sd.getService()
				.getServiceName(), sd);
	}

	public static void addServiceDataConnector(RichService rs,
			ServiceDataConnector... connectors) {
		for (ServiceDataConnector connector : connectors) {
			rs.addServiceDataConnector(connector.getService().getServiceName(),
					connector);
		}
	}
}
