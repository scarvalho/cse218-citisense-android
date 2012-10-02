package org.sosa.richservice.base;

import org.sosa.richservice.MessageNotification;

public class MessageNotificationBase implements MessageNotification {

	private final String source, messageId, contents, topic;
	private static final long serialVersionUID = -8790521760307265663L;

	public MessageNotificationBase(String source, String messageId,
			String topic, String contents) {
		this.source = source;
		this.messageId = messageId;
		this.topic = topic;
		this.contents = contents;
	}

	@Override
	public String getContents() {
		return contents;
	}

	@Override
	public String getTopic() {
		return topic;
	}

	@Override
	public String getMessageId() {
		return messageId;
	}

	@Override
	public String getSource() {
		return source;
	}

}
