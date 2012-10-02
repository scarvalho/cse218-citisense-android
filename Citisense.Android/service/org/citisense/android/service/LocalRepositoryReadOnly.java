package org.citisense.android.service;

import java.util.Collection;

import org.citisense.datastructure.SensorReading;
import org.citisense.datastructure.SensorType;

/**
 * Only exposes read-only API. The reason for this is the fact that the UI does
 * not need to store anything, and hence should only be given access to
 * read-only apis.
 * 
 * @author celal.ziftci
 * 
 */
public interface LocalRepositoryReadOnly {
	/**
	 * Returns the latest recorded {@link SensorReading} for the given
	 * {@link SensorType}.
	 * 
	 * @param sensorType
	 * @returnthe latest recorded {@link SensorReading} for the given
	 *            {@link SensorType}.
	 */
	public SensorReading getLastReading(SensorType sensorType);

	/**
	 * Returns the latest recorded {@link SensorReading}s for all known
	 * {@link SensorType}s.
	 * 
	 * @return the latest recorded {@link SensorReading}s for all known
	 *         {@link SensorType}s.
	 */
	public Collection<SensorReading> getLastReadingsForAllSensorTypes();
	
	public SensorReading getOldestReading(SensorType sensorType);
	
	public Collection<SensorReading> getOldestReadingForAllSensors();
	
	public SensorReading getMaxReadingForSensorDuring(SensorType sensorType,
			long startTime, long endTime);

	public Collection<SensorReading> getMaxReadingsDuring(long startTime,
			long endTime);
	
	public Collection<SensorReading> getReadingsByTypeDuring(SensorType sensorType, long startTime,
			long endTime);
	
	public Collection<Collection<SensorReading>> getReadingsDuring(
			long startTime, long endTime);
	
}