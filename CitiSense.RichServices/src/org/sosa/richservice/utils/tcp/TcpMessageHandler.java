package org.sosa.richservice.utils.tcp;

import org.sosa.richservice.Message;

/**
 * 
 * @author celal.ziftci
 * 
 */
public interface TcpMessageHandler {

	public Message handleMessage(Message msg);
}
