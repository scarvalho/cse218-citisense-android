package org.citisense.android.service.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;

import org.citisense.android.AqiCalculator;
import org.citisense.android.UICallbackTypes;
import org.citisense.android.service.LocalRepository;
import org.citisense.datastructure.Location;
import org.citisense.datastructure.SensorReading;
import org.citisense.datastructure.SensorType;
import org.citisense.datastructure.impl.LocationImpl;
import org.citisense.datastructure.impl.SensorReadingImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.Handler;

public class AqiTracker {
	private final Logger logger = LoggerFactory.getLogger(AqiTracker.class);
	private static SensorReading todaysMaxAqi = null;
	private static SensorReading currentHoursMaxAqi = null;
	private static Calendar whenTrackingStarted = null;
	private static Collection<SensorReading> todaysHourlyMaxAqis = null;
	
	public static final Collection<SensorReading> getTodaysHourlyMaxAqis(){ 
		return todaysHourlyMaxAqis;
	}
	
	public static SensorReading getTodaysMaxAqi() {
		return todaysMaxAqi;
	}
	
	public static SensorReading getCurrentHoursMaxAqi() {
		return currentHoursMaxAqi;
	}
	
	public void updateAqi( LocalRepository localRepository, ApplicationSettings settings){
		Calendar current = Calendar.getInstance();

		SensorReading currentAqi = calculateAqi(localRepository.getLastReadingsForAllSensorTypes());
		localRepository.storeSensorReading(currentAqi);
		
		// Send the newly calculated AQI value to the UI to be shown
		if(settings.isUiHandlerActive()) {
			Handler uiHandler = settings.uiHandler();
			if(uiHandler != null) {
				uiHandler.obtainMessage(UICallbackTypes.AQI_UI_NEW_AQI, -1, -1, currentAqi).sendToTarget();
			}
		}
		
		if(whenTrackingStarted == null){
			bootstrap(localRepository, current, currentAqi);
		}
		
		if(isNewHour(current)){
			if(AppLogger.isInfoEnabled(logger)) logger.info("Time to update hourly MAX_AQI...");
			saveMaxAqi(localRepository, currentHoursMaxAqi);			
			currentHoursMaxAqi = convertAqiToMax(currentAqi);
		}else{
			currentHoursMaxAqi = takeMax(currentHoursMaxAqi, currentAqi);
		}
		
		if(isNewDay(current)){
			if(AppLogger.isInfoEnabled(logger)) logger.info("Time to update daily MAX AQI");
			todaysHourlyMaxAqis.clear();
			todaysMaxAqi = convertAqiToMax(currentAqi);			
		}else{
			todaysMaxAqi  = takeMax(todaysMaxAqi , currentHoursMaxAqi);
		}
		
		if(AppLogger.isDebugEnabled(logger))
			logger.debug("Done tracking Current AQI = {} (hour max = {}) (day max = {})",
				new Object[]{currentAqi.getSensorData(), currentHoursMaxAqi.getSensorData(), todaysMaxAqi.getSensorData()});
	}
	
	private void bootstrap(LocalRepository localRepository, Calendar current, SensorReading currentAqi){
		whenTrackingStarted = current;
		if(AppLogger.isInfoEnabled(logger)) logger.info(
				"Started tracking AQI at " + 
				SimpleDateFormat.getInstance().format(whenTrackingStarted.getTime()) + 
				" Bootstrapping AQI Tracker...");

		Calendar startOfHour = rewindHours(current, 0);
		Calendar startOfDay = rewindDays(current, 0);
		todaysHourlyMaxAqis = localRepository.getReadingsByTypeDuring(SensorType.MAX_AQI, startOfDay.getTimeInMillis(), current.getTimeInMillis());
		SensorReading maxAqiSoFarCurrentHour = 
				localRepository.getMaxReadingForSensorDuring(SensorType.AQI, startOfHour.getTimeInMillis(), current.getTimeInMillis());
		SensorReading maxAqiSoFarToday =
				localRepository.getMaxReadingForSensorDuring(SensorType.MAX_AQI, startOfDay.getTimeInMillis(), current.getTimeInMillis());
		
		if(maxAqiSoFarCurrentHour == null)
			currentHoursMaxAqi = convertAqiToMax(currentAqi);
		else
			currentHoursMaxAqi = currentAqi.getSensorData() > maxAqiSoFarCurrentHour.getSensorData() 
								? convertAqiToMax(currentAqi)
								: convertAqiToMax(maxAqiSoFarCurrentHour);
		
		if(AppLogger.isDebugEnabled(logger)) logger.debug(String.format("Retrieving MAX_AQI readings from the DB between %1$tm %1$te,%1$tY %1tT and now", startOfDay));
		
		if(maxAqiSoFarToday == null)
			todaysMaxAqi = currentHoursMaxAqi;
		else
			todaysMaxAqi = currentHoursMaxAqi.getSensorData() > maxAqiSoFarToday.getSensorData() ?
					currentHoursMaxAqi : maxAqiSoFarToday;
		
		if(AppLogger.isInfoEnabled(logger)) logger.info(
				"...done Bootstrapping AQI Tracker current MAX_AQI: " + currentHoursMaxAqi.getSensorData() + 
				" today's MAX_AQI: " + todaysMaxAqi.getSensorData() + 
				" hourly MAX_AQI readings count: " + todaysHourlyMaxAqis.size());
	}

	private SensorReading calculateAqi(Collection<SensorReading> readings) {
		// Get last reading of each type
		int aqi = Integer.MIN_VALUE;
		int temp = 0;
		long time = System.currentTimeMillis();
		Location<Object> loc = null, aqiLoc = null;
		String mainPollutant = null;
		SensorReading maxReading = null;
		for (SensorReading reading : readings){
			// Call AQI Calculator to get AQI
			temp = AqiCalculator.calculateAQI(
					reading.getSensorData(), 
					reading.getSensorType().toString());
			if (temp > aqi){
				aqi = temp;
				//time = reading.getTimeMilliseconds();
				loc = reading.getLocation();
				maxReading = reading;
				mainPollutant = reading.getSensorType().name();
			}
		}
		
		// Hack
		// AQI has a different loc form: the offending pollutant is stored in 'source'
		if(loc != null)
			aqiLoc = new LocationImpl<Object>(loc.getLatitude(), loc.getLongitude(), 
					loc.getAltitude(), time, mainPollutant, 0, loc.getExtraInformation());
		
		// Make a new sensor reading of type AQI
		SensorReading aqi_reading = new SensorReadingImpl(
				SensorType.AQI, aqi, maxReading.getSensorType().toString(), time, aqiLoc);
		if(AppLogger.isInfoEnabled(logger))
			logger.info("Adding AQI reading: AQI = {} original = {} ({})",
				new Object[]{
				aqi_reading.getSensorData(), 
				maxReading.getSensorType().toString(), 
				maxReading.getSensorData()});
		return aqi_reading;
	}
	
	private void saveMaxAqi(LocalRepository localRepository, SensorReading maxAqi) {
		if(AppLogger.isInfoEnabled(logger)) logger.info("Storing MAX_AQI value to hourly cache & local storage: " + maxAqi);
		todaysHourlyMaxAqis.add(maxAqi);
		localRepository.storeSensorReading(maxAqi);
		// A max AQI was computed. All old, flushed AQI readings may be now deleted
		localRepository.dropReadingsFromRange(SensorType.AQI, 0, ObservationRepositoryImpl.timeOfLastAqiFlush());		
	}
	
	private boolean isNewDay(Calendar current){
		return current.get(Calendar.DAY_OF_MONTH) != getTodaysMaxAqi().getTimeDate().getDate();
	}
	
	private boolean isNewHour(Calendar current){
		return current.get(Calendar.HOUR_OF_DAY) != getCurrentHoursMaxAqi().getTimeDate().getHours();
	}	

	private Calendar rewindHours(Calendar current, int hours ){
		Calendar rewound = (Calendar)current.clone();
		clearCalendarFields(rewound, Calendar.MINUTE, Calendar.SECOND, Calendar.MILLISECOND, Calendar.AM_PM);
		rewound.add(Calendar.HOUR_OF_DAY, hours);
		return rewound;
	}
	
	private Calendar rewindDays(Calendar current, int days ){
		Calendar rewound = (Calendar)current.clone();
		clearCalendarFields(rewound, Calendar.HOUR,Calendar.MINUTE, Calendar.SECOND, Calendar.MILLISECOND, Calendar.AM_PM);
		rewound.add(Calendar.DAY_OF_MONTH, days);
		return rewound;
	}

	private SensorReading takeMax(SensorReading original, SensorReading candidate) {
		return candidate.getSensorData() > original.getSensorData() 
				? convertAqiToMax(candidate)
				: original;
	}

	private SensorReadingImpl convertAqiToMax(SensorReading aqiReading) {
		return new SensorReadingImpl(SensorType.MAX_AQI, 
					aqiReading.getSensorData(), aqiReading.getSensorUnits(), aqiReading.getTimeMilliseconds(), aqiReading.getLocation() );
	}
	
	private void clearCalendarFields(Calendar calendar,  int... fields){
		for(int field : fields){
			calendar.set(field, 0);
		}
	}
}
	

