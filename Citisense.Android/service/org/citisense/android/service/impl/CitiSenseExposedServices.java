package org.citisense.android.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.citisense.android.service.BluetoothService;
import org.citisense.android.service.FlushingTriggerService;
import org.citisense.android.service.LocalRepository;
import org.citisense.android.service.LocalRepositoryReadOnly;
import org.citisense.android.service.impl.updated.FlushingTriggerServiceImpl;
import org.citisense.datastructure.Location;
import org.citisense.datastructure.SensorReading;
import org.citisense.datastructure.SensorType;
import org.citisense.service.LocationService;
import org.citisense.service.ObservationRepository;
import org.citisense.service.SensorServiceTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CitiSenseExposedServices implements LocationService,
		SensorServiceTest, ObservationRepository, BluetoothService,
		LocalRepositoryReadOnly, LocalRepository, FlushingTriggerService {
	private final Logger logger = LoggerFactory.getLogger(CitiSenseExposedServices.class);
	private LocationService locationService;
	private SensorServiceTest sensorServiceTest;
	private ObservationRepository observationRepository;
	private BluetoothService bluetoothService;
	private LocalRepository combinedRepository;
	private FlushingTriggerService flushingTriggerService;
	
	@Override
	public void flushAllData() {
		flushingTriggerService.flushAllData();
	}
	
	@Override
	public void openDatabase() {
		combinedRepository.openDatabase();
	}
	
	@Override
	public void closeDatabase() {
		combinedRepository.closeDatabase();
	}
	
	@Override
	public boolean isDatabaseOpen() {
		return combinedRepository.isDatabaseOpen();
	}

	@Override
	public SensorReading getLastReading(SensorType sensorType){
		return combinedRepository.getLastReading(sensorType);
	}
	
	@Override
	public Collection<SensorReading> getLastReadingsForAllSensorTypes() {
		Collection<SensorReading> readings = combinedRepository.getLastReadingsForAllSensorTypes();
		
		for(SensorReading r : readings){
			if( r.getSensorType() == SensorType.MAX_AQI){
				
				readings.remove(r);
				
				// This can potentially return null and cause an exception
				SensorReading todaysMaxAqi = AqiTracker.getTodaysMaxAqi();
				if(todaysMaxAqi != null)
					readings.add(todaysMaxAqi);
			}
		}
		
		return readings;		
	}
	
	@Override
	public SensorReading getOldestReading(SensorType sensorType) {
		return combinedRepository.getOldestReading(sensorType);
	}

	@Override
	public Collection<SensorReading> getOldestReadingForAllSensors() {
		return combinedRepository.getOldestReadingForAllSensors();
	}
	
	@Override
	public SensorReading getMaxReadingForSensorDuring(SensorType sensorType,
			long startTime, long endTime) {
		return combinedRepository.getMaxReadingForSensorDuring(sensorType, startTime, endTime);
	}
	
	@Override
	public Collection<SensorReading> getMaxReadingsDuring(long startTime, long endTime) {
		return combinedRepository.getMaxReadingsDuring(startTime, endTime);
	}
	
	@Override
	public Collection<Collection<SensorReading>> getReadingsDuring(
			long startTime, long endTime) {
		return combinedRepository.getReadingsDuring(startTime, endTime);
	}
	
	public Collection<SensorReading> getReadingsDuring(
			SensorType sensorType, long startTime, long endTime) {
		return combinedRepository.getReadingsByTypeDuring(sensorType, startTime, endTime);
	}
	
	@Override
	public void storeSensorReading(SensorReading sensorReading) {
		combinedRepository.storeSensorReading(sensorReading);
	}
	
	@Override
	public void dropReadingsFromRange(SensorType sensorType, long startTime, long endTime) {
		combinedRepository.dropReadingsFromRange(sensorType, startTime, endTime);
	}

	@Override
	public void dropOldReadings(SensorType sensorType, int count) {
		combinedRepository.dropOldReadings(sensorType, count);
	}

	@Override
	public void dropOldReadingsForAllSensorTypes(int count) {
		combinedRepository.dropOldReadingsForAllSensorTypes(count);
	}
	
	@Override
	public void dropAllReadings(SensorType sensorType) {
		combinedRepository.dropAllReadings(sensorType);
	}
	
	@Override
	public void connectSensor(String address) {
		bluetoothService.connectSensor(address);
	}
	
	@Override
	public void disconnectSensor() {
		bluetoothService.disconnectSensor();
	}

	@Override
	public int isSensorConnected() {
		return bluetoothService.isSensorConnected();
	}

	//@SuppressWarnings("unchecked")
	@Override
	public Location<Object> getLastKnownLocation() {
		return locationService.getLastKnownLocation();
	}

	@Override
	public void sense(SensorReading sensorReading) {
		sensorServiceTest.sense(sensorReading);
	}

	@Override
	public int deleteObservation(String studyID, String subjectID,
			String sensorId, Map<String, String> attributes) {
		return observationRepository.deleteObservation(studyID, subjectID,
				sensorId, attributes);
	}

	@Override
	public SensorReading[] getObservation(String studyID, String subjectID,
			String sensorId, Map<String, String> attributes) {
		return observationRepository.getObservation(studyID, subjectID,
				sensorId, attributes);
	}

	@Override
	public int newObservation(String studyID, String subjectID,
			String sensorId, SensorReading[] observations) {
		// TODO: replace with local repo...
		return observationRepository.newObservation(studyID, subjectID,
				sensorId, observations);
	}

	// SETTERS
	public void setLocationService(LocationService locationService) {
		this.locationService = locationService;
	}

	public void setSensorServiceTest(SensorServiceTest sensorServiceTest) {
		this.sensorServiceTest = sensorServiceTest;
	}

	public void setObservationRepository(
			ObservationRepository observationRepository) {
		this.observationRepository = observationRepository;
	}

	public void setBluetoothService(BluetoothService bluetoothService) {
		this.bluetoothService = bluetoothService;
	}
	
	public void setFlushingTriggerService(FlushingTriggerService flushingTriggerService) {
		this.flushingTriggerService = flushingTriggerService;
	}

	public void setLocalRepository(LocalRepository localRepository) {
		this.combinedRepository = localRepository;
	}

	@Override
	public Collection<SensorReading> getReadingsByTypeDuring(SensorType sensorType, long startTime,
			long endTime) {
		return this.combinedRepository.getReadingsByTypeDuring(sensorType, startTime, endTime);
	}
	
	public SensorReading getCurrentMaxAqi(){
		return AqiTracker.getCurrentHoursMaxAqi();	
	}
	
	public Collection<SensorReading> getMaxAqisForToday(){
		
		Collection<SensorReading> readings = new ArrayList<SensorReading>();
		Collection<SensorReading> previousMaxReadings = AqiTracker.getTodaysHourlyMaxAqis();
		if(previousMaxReadings != null){
			if(AppLogger.isDebugEnabled(logger)) logger.debug("Got " + previousMaxReadings.size() + " previous hourly MAX_AQI readings from AqiTracker");
			readings.addAll(previousMaxReadings);
		}else{
			if(AppLogger.isDebugEnabled(logger)) logger.debug("Got <null> previous hourly MAX_AQI readings from AqiTracker");
		}
		SensorReading currentHoursMaxAqi = AqiTracker.getCurrentHoursMaxAqi(); 
		if(currentHoursMaxAqi != null){
			if(AppLogger.isDebugEnabled(logger)) logger.debug("Got " + currentHoursMaxAqi.getSensorData() + " current hour MAX_AQI reading from AqiTracker");
			readings.add(currentHoursMaxAqi);
		}else{
			if(AppLogger.isDebugEnabled(logger)) logger.debug("Got <null> current hour MAX_AQI reading from AqiTracker");
		}
		if(AppLogger.isDebugEnabled(logger)) logger.debug("Returning " + readings.size() + " MAX_AQI readings for today");
		return readings;
	}


}
