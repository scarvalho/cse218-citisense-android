package org.sosa.richservice.base;

import org.sosa.richservice.ServiceRequest;

/**
 * 
 * @author celal.ziftci
 * 
 */
@SuppressWarnings("unchecked")
public class ServiceRequestBase implements ServiceRequest {

	private final String serviceName;
	private final String operation;
	private final Class[] parameterTypes;
	private final Object[] parameterValues;

	public ServiceRequestBase(String serviceName, String operation,
			Class[] parameterTypes, Object[] parameterValues) {
		this.operation = operation;
		this.parameterTypes = parameterTypes;
		this.parameterValues = parameterValues;
		this.serviceName = serviceName;
	}

	@Override
	public String getServiceName() {
		return serviceName;
	}

	@Override
	public String getOperation() {
		return operation;
	}

	@Override
	public Class[] getParameterTypes() {
		return parameterTypes;
	}

	@Override
	public Object[] getParameterValues() {
		return parameterValues;
	}
}
