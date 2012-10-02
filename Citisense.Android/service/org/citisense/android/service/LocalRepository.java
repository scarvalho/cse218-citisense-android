package org.citisense.android.service;

import org.citisense.datastructure.SensorReading;
import org.citisense.datastructure.SensorType;

public interface LocalRepository extends LocalRepositoryReadOnly {
	public void storeSensorReading(SensorReading sensorReading);
	
	public void dropReadingsFromRange(SensorType sensorType, long startTime, long endTime);

	public void dropOldReadings(SensorType sensorType, int count);
	
	public void dropOldReadingsForAllSensorTypes(int count);
	
	public void dropAllReadings(SensorType sensorType);
	
	public boolean isDatabaseOpen();
	
	public void openDatabase();
	
	public void closeDatabase();
}