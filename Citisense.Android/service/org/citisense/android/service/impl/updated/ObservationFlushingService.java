package org.citisense.android.service.impl.updated;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.citisense.android.service.impl.AppLogger;
import org.citisense.android.service.impl.ApplicationSettings;
import org.citisense.android.service.impl.ConnectivityNotificationService;
import org.citisense.datastructure.SensorReading;
import org.citisense.service.ObservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sosa.richservice.Message;
import org.sosa.richservice.MessageNotification;
import org.sosa.richservice.MessageRequest;
import org.sosa.richservice.ServiceDescriptorLocal;
import org.sosa.richservice.base.MessageRequestBase;
import org.sosa.richservice.base.ServiceDescriptorLocalBase;
import org.sosa.richservice.base.servicedataconnector.ServiceDataConnectorJavaLocal;
import org.sosa.richservice.utils.richservice.RichServiceUtils;

/**
 * This is a probabilistic flushing service. It flushes the cache when
 * connectivity has been restored deterministically. It also flushes the cache
 * nondeterministically with a probability of 10%.
 * <p/>
 * Nondeterminism is added to cater to the synchronization problems on what
 * happens when connectivity comes back.
 * 
 * @author celal.ziftci
 * 
 */
public class ObservationFlushingService {
	private final Logger logger = LoggerFactory
			.getLogger(ObservationFlushingService.class);

	private final ServiceDataConnectorJavaLocal connector;
	private final ServiceDescriptorLocal descriptor;
	private final Method dataReadMethod, dataSaveMethod, dataDeleteMethod;
	private boolean connectivityOn = false;
	private final ApplicationSettings applicationSettings = ApplicationSettings
			.instance();

	public ObservationFlushingService() {
		try {
			dataReadMethod = ObservationRepository.class.getMethod(
					"getObservation", String.class, String.class, String.class,
					Map.class);

			dataSaveMethod = ObservationRepository.class.getMethod(
					"newObservation", String.class, String.class, String.class,
					SensorReading[].class);

			dataDeleteMethod = ObservationRepository.class.getMethod(
					"deleteObservation", String.class, String.class,
					String.class, Map.class);
		} catch (Exception e) {
			throw new RuntimeException("Cannot instantiate class '"
					+ ObservationFlushingService.class.getName() + "'", e);
		}

		connector = new ServiceDataConnectorJavaLocal() {
			@Override
			public void receiveMessage(Message msg) {

				if (msg instanceof MessageNotification) {

					MessageNotification notification = (MessageNotification) msg;
					String topic = notification.getTopic();
					String contents = notification.getContents();
					if ("connectivity".equals(topic) && "on".equals(contents)) {
						connectivityOn = true;
						if (AppLogger.isDebugEnabled(logger))
							logger.debug("Connectivity: UP");
					} else if ("connectivity".equals(topic)
							&& "off".equals(contents)) {
						connectivityOn = false;
						if (AppLogger.isDebugEnabled(logger))
							logger.debug("Connectivity: DOWN");
					}

					if ("flushing-trigger".equals(topic)) {
						//if (connectivityOn) {
						if(ConnectivityNotificationService.canConnect()) {
							if (AppLogger.isDebugEnabled(logger))
								logger.debug("Time to flush cached sensor readings");
							// FLUSH
							Collection<String> sensorIDs = applicationSettings
									.getAllSensorIDs();
							// For each type of sensor data
							for (String sensorID : sensorIDs) {
								// Get cached data
								SensorReading[] cachedObservations = readObservationsFor(sensorID);

								while(cachedObservations != null) {
									if(cachedObservations.length == 0) {
										// If no data returned for the window, you still need to
										// 'clear' that window and move on to the next
										deleteLocalDataFor(sensorID);
										break;
									}
									
									// Flush it
									try {
										logger.info("Started flushing sensorId " + sensorID);
										flushDataFor(sensorID,
												cachedObservations);
										logger.info("Finished flushing " + sensorID);
										// Delete it locally
										deleteLocalDataFor(sensorID);
									} catch (Throwable e) {
										connectivityOn = false;
										logger.info("Failed flushing " + sensorID);
										logger
												.error(
														"Exception on flushing. Ignoring this, and marking connectivity to be down until next notification. Flushing will be tried again in the future.",
														e);
										break;
									}
									// Get next set of data
									cachedObservations = readObservationsFor(sensorID);
								}
							}
							
							try {
								finishedFlushing();
							} catch (Throwable e) {
								logger.error(
										"Exception during get to update-region.",
										e);
							}
						} else {
							// no-op
							if (AppLogger.isDebugEnabled(logger))
								logger.debug("Received flushing-trigger but connectivity is down. Not doing anything.");
						}
					}
				} else {
					super.receiveMessage(msg);
				}
			}

			private SensorReading[] readObservationsFor(String sensorID) {
				if (AppLogger.isDebugEnabled(logger))
					logger.debug("Reading cached data for sensor '{}'", sensorID);
				MessageRequest request = new MessageRequestBase(descriptor
						.getServiceName(), "ObservationRepository",
						RichServiceUtils.generateMessageId(), dataReadMethod
								.getName(), dataReadMethod.getParameterTypes(),
						new Object[] { applicationSettings.studyID(),
								applicationSettings.subjectID(), sensorID,
								Collections.EMPTY_MAP });
				SensorReading[] cachedObservations = (SensorReading[]) super
						.sendMessage(request, 0);
				return cachedObservations;
			}

			private void flushDataFor(String sensorID,
					SensorReading[] cachedObservations) {
				if (AppLogger.isDebugEnabled(logger))
					logger.debug(
						"Flushing cached data onto the cloud for sensor '{}'",
						sensorID);
				MessageRequest request = new MessageRequestBase(descriptor
						.getServiceName(), "CloudService", RichServiceUtils
						.generateMessageId(), dataSaveMethod.getName(),
						dataSaveMethod.getParameterTypes(), new Object[] {
								applicationSettings.studyID(),
								applicationSettings.subjectID(), sensorID,
								cachedObservations });
				super.sendMessage(request, 0);
			}
			
			private void finishedFlushing() {
				if (AppLogger.isDebugEnabled(logger))
					logger.debug(
						"Finished flushing. Telling server to update regions.");
				MessageRequest request = new MessageRequestBase(descriptor
						.getServiceName(), "CloudService", RichServiceUtils
						.generateMessageId(), "endObservation",
						new Class[] { }, new Object[] { });
				super.sendMessage(request, 0);
			}

			private void deleteLocalDataFor(String sensorID) {
				if (AppLogger.isDebugEnabled(logger))
					logger.debug("Deleting locally cached data for sensor '{}'",
						sensorID);
				MessageRequest request = new MessageRequestBase(descriptor
						.getServiceName(), "ObservationRepository",
						RichServiceUtils.generateMessageId(), dataDeleteMethod
								.getName(), dataDeleteMethod
								.getParameterTypes(), new Object[] {
								applicationSettings.studyID(),
								applicationSettings.subjectID(), sensorID,
								Collections.EMPTY_MAP });
				super.sendMessage(request, 0);
			}
		};

		descriptor = new ServiceDescriptorLocalBase(
				"ObservationFlushingService", (Class<?>) null, null,
				"CloudService", "ObservationRepository");
		connector.setService(descriptor);
	}

	public ServiceDataConnectorJavaLocal getServiceDataConnector() {
		return connector;
	}

	// private boolean nondeterministicAllowance() {
	// return (Math.random() < 0.1);
	// }
}
