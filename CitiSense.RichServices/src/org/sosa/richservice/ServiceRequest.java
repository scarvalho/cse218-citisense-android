package org.sosa.richservice;

/**
 * 
 * @author celal.ziftci
 * 
 */
public interface ServiceRequest {

	public String getServiceName();

	public String getOperation();

	@SuppressWarnings("unchecked")
	public Class[] getParameterTypes();

	public Object[] getParameterValues();
}
