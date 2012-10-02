package org.citisense.service;

import java.util.Map;

public interface SensorReadingRepository {

	/**
	 * Stores new sensor readings.<br/>
	 * The passed in {@code sensorReadings} contain comma separated values of
	 * sensor readings. All elements in {@code sensorReadings} are assumed to be
	 * sensor reading values (no headers).
	 * 
	 * @param studyID
	 * @param subjectID
	 * @param sensorID
	 * @param sensorReadings
	 */
	public void newSensorReading(String studyID, String subjectID,
			String sensorID, String[] sensorReadings);

	/**
	 * Stores the new sensor reading.<br/>
	 * The passed in {@code sensorReadings} contain comma separated values of
	 * sensor readings. All elements in {@code sensorReadings} are assumed to be
	 * sensor reading values (no headers).
	 * 
	 * @param studyID
	 * @param subjectID
	 * @param sensorID
	 * @param sensorReading
	 */
	public void newSensorReading(String studyID, String subjectID,
			String sensorID, String sensorReading);

	/**
	 * Returns the sensorReading with the given ID.
	 * 
	 * @param observationId
	 * @return
	 */
	public String getSensorReading(String sensorReadingID);

	/**
	 * Returns the latest reading for the given sensor.
	 * 
	 * @param sensorType
	 * @return
	 */
	public String getLatestSensorReading(String sensorID);

	/**
	 * Returns the latest reading for the given sensor.
	 * 
	 * @param sensorID
	 * @param queryParams
	 *            the parameters that filter the readings
	 * @return
	 */
	public String[] getAllSensorReadings(String sensorID,
			Map<String, String> queryParams);

	/**
	 * Deletes the sensor reading with the given ID.
	 * 
	 * @param observationId
	 * @return
	 */
	public int deleteSensorReading(String sensorReadingId);

	/**
	 * Deletes the sensor readings with the given IDs.
	 * 
	 * @param observationId
	 * @return
	 */
	public int deleteSensorReading(String[] sensorReadingIds);
}
