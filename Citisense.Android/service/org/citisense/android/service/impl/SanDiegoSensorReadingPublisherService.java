package org.citisense.android.service.impl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.citisense.android.AirDataReader;
import org.citisense.android.CustomExceptionHandler;
import org.citisense.android.service.LocalRepository;
import org.citisense.datastructure.SensorReading;
import org.citisense.datastructure.SensorType;
import org.citisense.service.LocationService;
import org.citisense.service.ObservationRepository;
import org.citisense.utils.thread.DaemonThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.location.Location;
import android.location.LocationManager;

/**
 * This is a service that periodically polls the San Diego air pollution web
 * site ({@link ApplicationSettings#sanDiegoPollutionWebSite()}) and stores the
 * ozone ({@link SensorType#O3}) readings in the local database on the phone.
 * <p/>
 * It contains a thread that wakes up every
 * {@link ApplicationSettings#sanDiegoPollutionWebSitePollFrequency()}
 * milliseconds. It gets the pollution data, parses it and stores the ozone
 * readings into the local repository.
 * 
 * @author celal.ziftci
 * 
 */
public class SanDiegoSensorReadingPublisherService {
	private static final Logger logger = LoggerFactory
			.getLogger(SanDiegoSensorReadingPublisherService.class);

	private LocationService locationService;
	private LocalRepository localRepository;
	private ObservationRepository observationRepository;
	private final ApplicationSettings settings = ApplicationSettings.instance();
	
	private static final String threadName = SanDiegoSensorReadingPublisherService.class
			.getSimpleName();
	private static final SensorType storedPollutantType = SensorType.O3;

	private final ExecutorService executor = Executors.newFixedThreadPool(1,
			new DaemonThreadFactory(threadName));

	public SanDiegoSensorReadingPublisherService() {
		executor.execute(new Runnable() {

			private final SanDiegoPollutantsWebServicePoller parser = new SanDiegoPollutantsWebServicePoller();

			// private final SensorReading fakeReading = new SensorReadingImpl(
			// storedPollutantType, "3.5", storedPollutantType.getUnits(),
			// Calendar.getInstance().getTime(), new LocationImpl<Object>(
			// 0D, 0D, 0D, 0L, "fake", 0F));

			@Override
			public void run() {
				// Try to catch an unhandled exception in the main service thread
				Thread.currentThread().setUncaughtExceptionHandler(new CustomExceptionHandler());
				
				// TODO: There is a race condition here. If this background
				// thread starts before rich services finishes its setup, there
				// can be a NPE. I fixed this by sleeping until the setup is
				// done but what is really needed is lifecycle semantics in
				// rich-services (start/stop).
				while (localRepository == null || locationService == null) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						if (AppLogger.isWarnEnabled(logger)) {
							logger.warn(
									"{} thread is interrupted while waiting for setup (dependent services to be set). Thread will die, and sensor readings from the San Diego pollutant web server will not be stored anymore.",
									threadName);
						}
						return;
					}
				}

				// Run as long as the application runs.
				while (!Thread.currentThread().isInterrupted()) {
					Collection<SensorReading> sensorReadings = parser
							.getSensorReadings();
					// This is for testing only!
					// Collection<SensorReading> sensorReadings = Arrays
					// .asList(fakeReading);
					for (SensorReading sensorReading : sensorReadings) {
						// If necessary, we can store other types of sensor
						// readings too...
						if (sensorReading.getSensorType() == storedPollutantType) {
							if (AppLogger.isDebugEnabled(logger)) {
								logger
										.debug(
												"Storing an {} sensor reading obtained from San Diego pollution web site",
												storedPollutantType);
							}
							localRepository.storeSensorReading(sensorReading);
							AqiTracker aqiTracker = new AqiTracker();
							aqiTracker.updateAqi(localRepository, settings);
						}
					}
					try {
						Thread.sleep(ApplicationSettings.instance()
								.sanDiegoPollutionWebSitePollFrequency());
					} catch (InterruptedException e) {
						if (AppLogger.isWarnEnabled(logger)) {
							logger.warn("{} thread is interrupted. Thread will die, and sensor readings from the San Diego pollutant web server will not be stored anymore.",
									threadName);
						}
						return;
					}
				}
				if (AppLogger.isWarnEnabled(logger)) {
					logger.warn("{} thread is interrupted. Thread will die, and sensor readings from the San Diego pollutant web server will not be stored anymore.",
								threadName);
				}
			}
		});

	}

	public void setLocationService(LocationService locationService) {
		this.locationService = locationService;
	}

	public void setLocalRepository(LocalRepository localRepository) {
		this.localRepository = localRepository;
	}
	
	public void setObservationRepository(ObservationRepository observationRepository) {
		this.observationRepository = observationRepository;
	}

	/**
	 * This is the class that performs the parsing and tagging of the reading
	 * with a location.
	 * 
	 * @author celal.ziftci
	 * 
	 */
	private final class SanDiegoPollutantsWebServicePoller {
		private final Logger logger = LoggerFactory
				.getLogger(SanDiegoPollutantsWebServicePoller.class);

		@SuppressWarnings("unchecked")
		public Collection<SensorReading> getSensorReadings() {
			AirDataReader reader = new AirDataReader();
			android.location.Location androidLoc = new Location(
					LocationManager.GPS_PROVIDER);
			org.citisense.datastructure.Location<Object> location = locationService
					.getLastKnownLocation();
			if (AppLogger.isDebugEnabled(logger)) logger.debug("Location from provider " + location.toString());
			// No location? nothing to do then...
			if (((Double) location.getLatitude()).isNaN()
					|| ((Double) location.getLongitude()).isNaN()) {
				if (AppLogger.isDebugEnabled(logger)) logger.debug("Didn't get a valid location from the provider, returning empty set.");
				return new ArrayList<SensorReading>();
			}
			if (AppLogger.isDebugEnabled(logger)) logger.debug("Got a location from provider! parsing SD Readings... " + location.toString());

			androidLoc.setLatitude(location.getLatitude());
			androidLoc.setLongitude(location.getLongitude());
			reader.updateClosestLocation(androidLoc);
			Collection<SensorReading> readings = new ArrayList<SensorReading>();
			try {
				if (AppLogger.isDebugEnabled(logger))
					logger.debug("Getting readings from SD service...");
				readings = reader.getReading();
				if (AppLogger.isDebugEnabled(logger)) logger.debug("Got " + readings.size() + " Readings from SD service");
			} catch (MalformedURLException e) {
				if (AppLogger.isWarnEnabled(logger)) {
					logger.warn("Exception getting readings from SD service. It will be ignored.", e);
				}
			} catch (IOException e) {
				if (AppLogger.isWarnEnabled(logger)) {
					logger.warn("Exception getting readings from SD service. It will be ignored.", e);
				}
			}
			if (AppLogger.isDebugEnabled(logger)) {
				logger.debug("Returning " + readings.size() + " sensor readings");
			}
			return readings;
		}
	}
}
