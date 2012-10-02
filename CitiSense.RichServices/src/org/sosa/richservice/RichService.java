package org.sosa.richservice;

import java.util.Collection;

/**
 * 
 * @author celal.ziftci
 * 
 */
public interface RichService {

	/**
	 * Returns the message bus used in this rich service
	 * 
	 * @return the message bus used in this rich service
	 */
	public MessageBus getMessageBus();

	/**
	 * Returns the message interceptor used in this rich service
	 * 
	 * @return the message interceptor used in this rich service
	 */
	public MessageInterceptor getMessageInterceptor();

	public Collection<ServiceDataConnector> getServiceDataConnectors();

	/**
	 * Returns the {@link ServiceDataConnector} for the service with the given
	 * name.
	 * 
	 * @param serviceName
	 * @return
	 */
	public ServiceDataConnector getServiceDataConnector(String serviceName);

	/**
	 * Adds a new service data connector to this rich service. Note that each
	 * service data connector is associated with a service, hence this is in a
	 * sense adding a new service to this rich service.
	 * 
	 * @param name
	 *            the name that the newly added service will be called with
	 * @param service
	 *            the new service in this rich service
	 */
	public void addServiceDataConnector(String name,
			ServiceDataConnector service);

	/**
	 * Removes an existing service data connector in this rich service.
	 * 
	 * @param name
	 */
	public ServiceDataConnector removeServiceDataConnector(String name);

	/**
	 * This is the gateway for the whole Rich Service. All requests from within
	 * this Rich Service to the upper layer go through this connector, and all
	 * outside requests come through this connector as well.
	 * 
	 * @param name
	 * @param service
	 */
	public void setServiceDataConnectorForRichService(String name,
			ServiceDataConnector service);

	/**
	 * This is the gateway for the whole Rich Service. All requests from within
	 * this Rich Service to the upper layer go through this connector, and all
	 * outside requests come through this connector as well.
	 * 
	 * @param name
	 * @param service
	 */
	public ServiceDataConnector getServiceDataConnectorForRichService();

	/**
	 * Adds a policy to be applied to all messages put onto the
	 * {@link MessageBus}.
	 * 
	 * @param policyName
	 * @param policy
	 */
	public void addPolicy(String policyName, Policy policy);

	/**
	 * Removes an existing policy.
	 * 
	 * @param policyName
	 * @return the policy with the given name if it exists, {@code null}
	 *         otherwise.
	 */
	public Policy removePolicy(String policyName);

	// public Map<String, Object> getServiceImplementations();
}
