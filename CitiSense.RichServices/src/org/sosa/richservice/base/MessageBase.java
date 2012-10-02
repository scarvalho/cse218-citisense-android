package org.sosa.richservice.base;

import java.io.Serializable;

import org.sosa.richservice.MessageAddressed;

/**
 * 
 * @author celal.ziftci
 * 
 */
public abstract class MessageBase implements MessageAddressed, Serializable {

	private static final long serialVersionUID = 7834327627010647224L;

	private final String source, destination, messageId;

	public MessageBase(String source, String destination, String messageId) {
		this.source = source;
		this.destination = destination;
		this.messageId = messageId;
	}

	@Override
	public String getSource() {
		return source;
	}

	@Override
	public String getDestination() {
		return destination;
	}

	@Override
	public String getMessageId() {
		return messageId;
	}

}
