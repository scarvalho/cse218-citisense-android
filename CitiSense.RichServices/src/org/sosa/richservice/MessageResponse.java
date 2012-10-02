package org.sosa.richservice;

/**
 * 
 * @author celal.ziftci
 * 
 */
public interface MessageResponse extends MessageAddressed {
	// public MessageRequest getOriginalRequest();

	public String getCorrelationId();

	public Object getResponse();
}
