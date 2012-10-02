package org.sosa.richservice;

public interface RichServiceManager {
	public void updatePolicy(String policyName, Policy newPolicy);

	public void addPolicy(String policyName, Policy policy);

	public void removePolicy(String policyName);

	public void updateServiceDataConnector(String policyName,
			ServiceDataConnector connector);

	public void addServiceDataConnector(String policyName, Policy policy);

	public void removeServiceDataConnector(String policyName);
}
