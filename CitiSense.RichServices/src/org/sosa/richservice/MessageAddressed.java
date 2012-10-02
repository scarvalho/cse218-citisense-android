package org.sosa.richservice;

import java.io.Serializable;

/**
 * A message that has a specific destination.
 * 
 * @author celal.ziftci
 * 
 */
public interface MessageAddressed extends Serializable, Message {

	public String getDestination();
}
