package org.citisense.android.service.impl;

import org.citisense.datastructure.SensorReading;
import org.citisense.service.ObservationRepository;
import org.citisense.service.SensorServiceTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SensorServiceTestImpl implements SensorServiceTest {
	private final Logger logger = LoggerFactory
			.getLogger(SensorServiceTestImpl.class);

	private final ApplicationSettings settings = ApplicationSettings.instance();
	private ObservationRepository observationRepository;

	public ObservationRepository getObservationRepository() {
		return observationRepository;
	}

	public void setObservationRepository(
			ObservationRepository observationRepository) {
		this.observationRepository = observationRepository;
	}

	public void sense(SensorReading sensorReading) {
		if (AppLogger.isDebugEnabled(logger))
			logger.debug("Sending sensor reading '{}'", sensorReading);
		// TODO: replace
		observationRepository.newObservation(settings.studyID(), settings
				.subjectID(), settings.sensorIDForSensorType(sensorReading
				.getSensorType().getPinNumber()),
				new SensorReading[] { sensorReading });
	}
}
