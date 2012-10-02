package org.citisense.service;

import java.lang.reflect.Method;
import java.util.Map;

import org.citisense.datastructure.SensorReading;

public interface ObservationRepository {

	public int newObservation(String studyID, String subjectID,
			String sensorId, SensorReading[] observations);

	public SensorReading[] getObservation(String studyID, String subjectID,
			String sensorId, Map<String, String> attributes);

	public int deleteObservation(String studyID, String subjectID,
			String sensorId, Map<String, String> attributes);

	static class Main {
		// TODO: Provide an API that atomically removes and returns data
		public static void main(String[] args) throws Exception {
			Method dataSaveMethod = ObservationRepository.class.getMethod(
					"newObservation", String.class, String.class, String.class,
					SensorReading[].class);
			System.out.println(dataSaveMethod);
		}
	}
}
