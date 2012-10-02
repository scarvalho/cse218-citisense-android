package org.sosa.richservice;

/**
 * 
 * @author celal.ziftci
 * 
 */
public interface Policy {
	public InterceptionResult interceptMessage(Message msg);

	public String getName();
}
