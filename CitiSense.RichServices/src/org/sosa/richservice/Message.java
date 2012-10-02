package org.sosa.richservice;

/**
 * A generic message with a source (the entity that created this message), and a
 * message id (for easy identification).
 * 
 * @author celal.ziftci
 * 
 */
public interface Message {

	public abstract String getSource();

	public abstract String getMessageId();

}