package org.sosa.richservice;

/**
 * 
 * @author celal.ziftci
 * 
 */
public interface MessageInterceptor {

	public void interceptMessage(Message msg);

	public void addPolicy(String policyName, Policy policy);

	public Policy removePolicy(String policyName);
}
