package org.citisense.android.service.impl;

import org.citisense.datastructure.SensorReading;
import org.citisense.datastructure.SensorType;
import org.citisense.datastructure.impl.SensorReadingImpl;

public class SensorReadingFilter {
	private static final double CO_SPIKE_THRESHOLD = 15.0;
	private static final double NO2_SPIKE_THRESHOLD = 1.0;
	private static final double O3_SPIKE_THRESHOLD = 0.3;
	
	private SensorReading[] lastReadings = new SensorReading[SensorType.values().length];
	private SensorReading[] lastLastReadings = new SensorReading[SensorType.values().length];
	
	public SensorReadingFilter(){

	}
	
	public SensorReading filterReading(SensorReading sensorReading){
		SensorReading lastReading = lastReadings[sensorReading.getSensorType().ordinal()];
		SensorReading lastLastReading = lastLastReadings[sensorReading.getSensorType().ordinal()];
		
		if(lastReading != null &&
				(lastReading.getSensorType() == SensorType.CO ||
				lastReading.getSensorType() == SensorType.NO2 ||
				lastReading.getSensorType() == SensorType.O3)){
			double lastData = lastReading.getSensorData();
			double newData = sensorReading.getSensorData();
			
			double diff = Math.abs(lastData - newData);
			// If CO is a drop of 15.0 or more...
			if( lastReading.getSensorType() == SensorType.CO &&  diff > CO_SPIKE_THRESHOLD) {
				// No older data to compare to, so go ahead and filter it out
				if(lastLastReading == null) {
					lastReading = null;
				} else {
					double lastLastData = lastLastReading.getSensorData();
					double lastDiff = Math.abs(lastLastData - lastData);
					if(lastDiff > CO_SPIKE_THRESHOLD) {
						lastReading = null;
					}
				}
			}
			// If NO2 is a drop of 1.0 or more...
			else if( lastReading.getSensorType() == SensorType.NO2 &&  diff > NO2_SPIKE_THRESHOLD) {
				// No older data to compare to, so go ahead and filter it out
				if(lastLastReading == null) {
					lastReading = null;
				} else {
					double lastLastData = lastLastReading.getSensorData();
					double lastDiff = Math.abs(lastLastData - lastData);
					if(lastDiff > NO2_SPIKE_THRESHOLD) {
						lastReading = null;
					}
				}
			}
			// If O3 is a drop of 1.0 or more...
			else if( lastReading.getSensorType() == SensorType.O3 && diff > O3_SPIKE_THRESHOLD) {
				// No older data to compare to, so go ahead and filter it out
				if(lastLastReading == null) {
					lastReading = null;
				} else {
					double lastLastData = lastLastReading.getSensorData();
					double lastDiff = Math.abs(lastLastData - lastData);
					if(lastDiff > O3_SPIKE_THRESHOLD) {
						lastReading = null;
					}
				}
			}
		}
		
		// Keep track as last reading
		lastLastReadings[sensorReading.getSensorType().ordinal()] = lastReading;
		// Make copy and store
		SensorReading copyReading = new SensorReadingImpl( sensorReading.getSensorType(), 
				sensorReading.getSensorData(),
				sensorReading.getSensorUnits(),
				sensorReading.getTimeMilliseconds(),
				sensorReading.getLocation() );
		lastReadings[sensorReading.getSensorType().ordinal()] = copyReading;

		return lastReading;
	}
	
	
}
