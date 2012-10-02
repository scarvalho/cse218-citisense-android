package org.sosa.richservice.base;

import java.io.Serializable;

import org.sosa.richservice.MessageResponse;

/**
 * 
 * @author celal.ziftci
 * 
 */
public class MessageResponseBase extends MessageBase implements
		MessageResponse, Serializable {

	private static final long serialVersionUID = 4444220699078687423L;

	private final String correlationId;
	private final Object response;

	public MessageResponseBase(String source, String destination,
			String messageId, String correlationId, Object response) {
		super(source, destination, messageId);
		this.correlationId = correlationId;
		this.response = response;
	}

	// public MessageResponseBase(MessageRequest originalRequest, String source,
	// String destination,
	// String messageId, String correlationId, Object response) {
	// super(source, destination, messageId);
	// this.correlationId = correlationId;
	// this.response = response;
	// }

	@Override
	public String getCorrelationId() {
		return correlationId;
	}

	@Override
	public Object getResponse() {
		return response;
	}

}
