package org.sosa.richservice;

public interface MessageNotification extends Message {

	public String getTopic();

	public String getContents();
}
