package org.citisense.android.service.impl;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.citisense.android.service.LocalRepository;
import org.citisense.datastructure.SensorReading;
import org.citisense.datastructure.SensorType;
import org.citisense.service.ObservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObservationRepositoryImpl implements ObservationRepository {
	private boolean firstCheck = true;
	private final Logger logger = LoggerFactory.getLogger(ObservationRepositoryImpl.class);
	private ApplicationSettings applicationSettings = ApplicationSettings.instance();
	private static long timeOfLastAqiFlush = 0;
	private final Map<String, ObservationTimestampRange> timestampMap = new HashMap<String, ObservationTimestampRange>(){{
		Calendar time = Calendar.getInstance();
		Collection<String> sensorIds = applicationSettings.getAllSensorIDs();
		for(String sensorId : sensorIds){
			put(sensorId, new ObservationTimestampRange(time));
		}
	}};
	private LocalRepository localRepository;
	
	public void setLocalRepository(LocalRepository localRepository){
		this.localRepository = localRepository;
	}
	
	private void setStartToOldestReadings() {
		// Check if any old readings remain to be uploaded
		Collection<SensorReading> oldestReadings = this.localRepository.getOldestReadingForAllSensors();
		for(SensorReading reading : oldestReadings) {
			// Adjust start time to be that of oldest reading in phone
			String sensorId = applicationSettings.sensorIDForSensorType(reading.getSensorType().getPinNumber());
			Calendar newStart = Calendar.getInstance();
			newStart.setTimeInMillis(reading.getTimeMilliseconds());
			
			ObservationTimestampRange range = timestampMap.get(sensorId);
			// if the end is not null, then it is in the process of being used
			// do not modify it while in use! could crash system
			if(range.end == null)
				range.moveStart(newStart);
		}
	}
	
	@Override
	public SensorReading[] getObservation(String studyID, String subjectID,
			String sensorId, Map<String, String> attributes) {
		
		if(firstCheck) {
			firstCheck = false;
			setStartToOldestReadings();
		}
		
		ObservationTimestampRange deviceObservationRange = timestampMap.get(sensorId);
		// Get the next window of data
		// If it was empty, continue to next window until you catch up to current time
		SensorReading[] readings = null;
		Calendar oneHourAgo = Calendar.getInstance();
		oneHourAgo.add(Calendar.HOUR, -1);
		
		// Keep requesting data until either:
		// (a) you get more than 0 readings
		// (b) you have advanced the end to a point that is within an hour of the current time
		while(readings == null) {
			if(AppLogger.isDebugEnabled(logger)) logger.debug("OTR: {} before advance: {}-{}", new Object[]{ sensorId, deviceObservationRange.getStart().getTimeInMillis(), deviceObservationRange.getEnd() == null ? 0 : deviceObservationRange.getEnd().getTimeInMillis()});
			// Advance end by maxSecondsOfDataToFlush
			deviceObservationRange.advanceEnd(applicationSettings.maxSecondsOfDataToFlush());
			if(AppLogger.isDebugEnabled(logger)) logger.debug("OTR: {} after  advance: {}-{}", new Object[]{ sensorId, deviceObservationRange.getStart().getTimeInMillis(), deviceObservationRange.getEnd() == null ? 0 : deviceObservationRange.getEnd().getTimeInMillis()});
			// The following never returns null, so this is safe
			SensorReading[] nextReadings = getPendingReadings(sensorId, deviceObservationRange.getStart(), deviceObservationRange.getEnd());
			if(nextReadings.length > 0 || deviceObservationRange.end.after(oneHourAgo)) {
				readings = nextReadings;
			} else {
				// If no data, and we are more than an hour behind,
				// go ahead and try the next window worth of data
				deviceObservationRange.observe();
			}
		}
		
		return readings;
	}

	@Override
	public int deleteObservation(String studyID, String subjectID,
			String sensorId, Map<String, String> attributes) {
		SensorType type = ApplicationSettings.instance().getSensorTypeFromSensorID(sensorId);
		
		ObservationTimestampRange deviceObservationRange = timestampMap.get(sensorId);
		if(AppLogger.isDebugEnabled(logger)) logger.debug("OTR: {} before observe: {}-{}", new Object[]{ sensorId, deviceObservationRange.getStart().getTimeInMillis(), deviceObservationRange.getEnd() == null ? 0 : deviceObservationRange.getEnd().getTimeInMillis()});
		if(type == SensorType.AQI) {
			timeOfLastAqiFlush = deviceObservationRange.getEnd().getTimeInMillis();
		} else {
			deletePendingReadings(sensorId, deviceObservationRange.getStart(), deviceObservationRange.getEnd());
		}
		
		// Note: observe() changes the value of deviceObservationRange
		deviceObservationRange.observe();
		if(AppLogger.isDebugEnabled(logger)) logger.debug("OTR: {} after  observe: {}-{}", new Object[]{ sensorId, deviceObservationRange.getStart().getTimeInMillis(), deviceObservationRange.getEnd() == null ? 0 : deviceObservationRange.getEnd().getTimeInMillis()});
		
		// TODO: determine if the number of readings deleted is being consumed anywhere or if its ok to just return 0, maybe change interface?
		return 0;
	}
	
	public static long timeOfLastAqiFlush() {
		return timeOfLastAqiFlush;
	}

	
	private SensorReading[] getPendingReadings(String sensorId, Calendar lastTimestamp, Calendar nextTimestamp){
		SensorType type = ApplicationSettings.instance().getSensorTypeFromSensorID(sensorId);
		
		// HACK: consider moving up to ObservationFlushingService, but it doesn't know about sensor types
		if( type == SensorType.MAX_AQI)
			return new SensorReading[0];
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm:ss:SS MM/dd/yyyy");
		
		Collection<SensorReading> readings = localRepository.getReadingsByTypeDuring(
												type,
												lastTimestamp.getTimeInMillis(), 
												nextTimestamp.getTimeInMillis());
	
		SensorReading[] ret = readings == null 
								? new SensorReading[0] 
								: readings.toArray(new SensorReading[]{});
		if(AppLogger.isDebugEnabled(logger)) logger.debug("Found {} {} ({}) readings between {}-{}",new Object[]{
				ret.length,
				type, 
				sensorId,
				dateFormat.format(lastTimestamp.getTime()),
				dateFormat.format(nextTimestamp.getTime())});		

		
		return ret;
	}
	
	private void deletePendingReadings(String sensorId, Calendar lastTimestamp, Calendar nextTimestamp) {
		SensorType type = ApplicationSettings.instance().getSensorTypeFromSensorID(sensorId);
		
		//hacky
		if( type == SensorType.MAX_AQI)
			return;
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm:ss:SS MM/dd/yyyy");
		
		localRepository.dropReadingsFromRange(type, lastTimestamp.getTimeInMillis(), nextTimestamp.getTimeInMillis());
		
		if(AppLogger.isDebugEnabled(logger)) logger.debug("Dropped {} ({}) readings between {}-{}",new Object[]{
				type, 
				sensorId,
				dateFormat.format(lastTimestamp.getTime()),
				dateFormat.format(nextTimestamp.getTime())});
	}
	
	@Override
	@Deprecated
	public int newObservation(String studyID, String subjectID,
			String sensorId, SensorReading[] observations) {

		return 0;
	}
		
	// This class handles tracking the time range that is being observed
	private class ObservationTimestampRange{
		public ObservationTimestampRange(Calendar start){
			this.start = start;
			this.end = null;
		}
		private Calendar start;
		public Calendar getStart() {
			return start;
		}		
		private Calendar end;
		public Calendar getEnd() {
			return end;
		}
		
		public void moveStart(Calendar start) {
			this.start = start;
			this.end = null;
		}
		
		public void advanceEnd(int seconds) {
			end = (Calendar)this.start.clone();
			end.add(Calendar.SECOND, seconds);
			Calendar maxEndTime = Calendar.getInstance();
			maxEndTime.add(Calendar.SECOND, -30);
			// Never let end advance past 30 seconds before current time
			if(end.after(maxEndTime))
				end = maxEndTime;
		}
		
		public void observe(){
			this.start = (Calendar)this.end.clone();
			this.end = null;
		}
	}

}
