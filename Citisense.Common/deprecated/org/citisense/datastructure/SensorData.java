package org.citisense.datastructure;

import java.io.Serializable;

/**
 * A Serializable pojo that contains a header, and multiple rows of data.
 * <p/>
 * Header is used to identify the type of sensor that is sending the data, and
 * the rest is the data itself in separate rows for each separate reading.
 * 
 * @author celal.ziftci
 * 
 */
public interface SensorData extends Serializable {
	public String getHeader();

	public String[] getData();
}
