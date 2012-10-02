package org.sosa.richservice;

/**
 * 
 * @author celal.ziftci
 * 
 */
public interface MessageRequest extends MessageAddressed {
	public String getOperation();

	@SuppressWarnings("unchecked")
	public Class[] getOperationParameterTypes();

	public Object[] getOperationParameterValues();

}
