package org.sosa.richservice;

import org.sosa.richservice.ServiceDescriptor;

/**
 * 
 * @author celal.ziftci
 * 
 */
public interface ServiceDescriptorLocal extends ServiceDescriptor {
	public Object getExposedImplementation();
	
	public void instantiateStubs(ServiceDescriptorLocal requiredService, 
			ServiceDataConnector<ServiceDescriptorLocal> connector, 
			String requiredServiceName) throws Exception;
}
