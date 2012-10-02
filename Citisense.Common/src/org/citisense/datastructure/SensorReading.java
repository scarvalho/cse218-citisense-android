package org.citisense.datastructure;

import java.io.Serializable;
import java.util.Date;

public interface SensorReading extends Serializable {
	public SensorType getSensorType();

	public double getSensorData();

	public String getSensorUnits();

	public Date getTimeDate();
	
	public long getTimeSeconds();
	
	public long getTimeMilliseconds();
	
	public String getTimeDateAsString();
	
	public Location<Object> getLocation();
}
