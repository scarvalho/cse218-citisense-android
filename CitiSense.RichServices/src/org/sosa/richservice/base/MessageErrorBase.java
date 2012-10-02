package org.sosa.richservice.base;

import java.io.Serializable;

import org.sosa.richservice.MessageError;

/**
 * 
 * @author celal.ziftci
 * 
 */
public class MessageErrorBase extends MessageResponseBase implements
		MessageError, Serializable {

	private static final long serialVersionUID = -6907062592810733943L;
	private final String errorMessage;

	public MessageErrorBase(String source, String destination,
			String messageId, String correlationId, String errorMessage,
			Throwable e) {
		super(source, destination, messageId, correlationId, e);
		this.errorMessage = errorMessage;
	}

	@Override
	public String getErrorMessage() {
		return errorMessage;
	}

}
