package org.citisense.datastructure.impl;

import org.citisense.datastructure.SensorData;

/**
 * 
 * @author celal.ziftci
 * 
 */
public class SensorDataImpl implements SensorData {

	private static final long serialVersionUID = -559013932783057060L;

	private final String header;
	private final String[] data;

	public SensorDataImpl(String header, String[] data) {
		this.header = header;
		this.data = data;
	}

	@Override
	public String[] getData() {
		return data;
	}

	@Override
	public String getHeader() {
		return header;
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer(header);
		for (String dataRow : data) {
			buffer.append("\n").append(dataRow);
		}
		return buffer.toString();
	}
}
