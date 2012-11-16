package org.citisense.android;

import java.util.Arrays;
//blah blah blah git
import org.citisense.android.service.BluetoothService;
import org.citisense.android.service.FlushingTriggerService;
import org.citisense.android.service.LocalRepository;
import org.citisense.android.service.impl.AppLogger;
import org.citisense.android.service.impl.ApplicationSettings;
import org.citisense.android.service.impl.BluetoothServiceImpl;
import org.citisense.android.service.impl.CitiSenseExposedServices;
import org.citisense.android.service.impl.ConnectivityNotificationService;
import org.citisense.android.service.impl.LocalRepositoryImpl;
import org.citisense.android.service.impl.LocationServiceImpl;
import org.citisense.android.service.impl.ObservationRepositoryImpl;
import org.citisense.android.service.impl.SensorServiceTestImpl;
import org.citisense.android.service.impl.updated.FlushingTriggerServiceImpl;
import org.citisense.android.service.impl.updated.ObservationFlushingService;
import org.citisense.android.servicedataconnector.ServiceDataConnectorForRichServiceHTTPClient;
import org.citisense.service.LocationService;
import org.citisense.service.ObservationRepository;
import org.citisense.service.SensorServiceTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sosa.richservice.RichService;
import org.sosa.richservice.ServiceDataConnectorForRichService;
import org.sosa.richservice.ServiceDescriptor;
import org.sosa.richservice.ServiceDescriptorLocal;
import org.sosa.richservice.base.ServiceDescriptorBase;
import org.sosa.richservice.base.ServiceDescriptorLocalBase;
import org.sosa.richservice.base.servicedataconnector.ServiceDataConnectorJavaLocal;
import org.sosa.richservice.utils.richservice.RichServiceBuilder;

public class AndroidRichService {

	/**
	 * This makes the code in this class much safer, because it removes the
	 * possibility of using a wrong String as the name of a service ;)
	 * 
	 * @author celal.ziftci
	 * 
	 */
	private enum Services {
		ObservationRepository, LocalRepository, LocationService, FlushingTriggerService, SanDiegoSensorReadingPublisherService, BluetoothService, SensorServiceTest, CloudService, ExposedServices;
	}

	// private final Logger logger =
	// LoggerFactory.getLogger(AndroidRichService.class);
	private final CitiSenseExposedServices exposedServices;
	private final ApplicationSettings applicationSettings = ApplicationSettings
			.instance();
	private static final Logger logger = LoggerFactory
							.getLogger(AndroidRichService.class);
	
	@SuppressWarnings("unchecked")
	public AndroidRichService() throws Exception {
		/**************************************************************************************
		 **************************** INFRASTRUCTURE SERVICES *********************************
		 *************************************************************************************/
		if (AppLogger.isDebugEnabled(logger))
			logger.debug("Creating INFRASTRUCTURE SERVICES...");
		ConnectivityNotificationService connectivityNotificationService = new ConnectivityNotificationService();
		ObservationFlushingService observationFlushingService = new ObservationFlushingService();
		FlushingTriggerServiceImpl flushingTriggerService = new FlushingTriggerServiceImpl();
		if (AppLogger.isDebugEnabled(logger))
			logger.debug("...created INFRASTRUCTURE SERVICES!");
		
		/**************************************************************************************
		 ********************************** BUSINESS SERVICES *********************************
		 *************************************************************************************/
		

		// Stores readings in SQLLite database on the SD card
		if (AppLogger.isDebugEnabled(logger))
			logger.debug("Creating localRepositoryDescriptor...");
		ServiceDescriptorLocal localRepositoryDescriptor = new ServiceDescriptorLocalBase(
				Services.LocalRepository.name(), 
				LocalRepository.class,
				new LocalRepositoryImpl());
		if (AppLogger.isDebugEnabled(logger))
			logger.debug("...created localRepositoryDescriptor!");
		
		if (AppLogger.isDebugEnabled(logger))
			logger.debug("Creating flushingTriggerServiceDescriptor...");
		ServiceDescriptorLocal flushingTriggerServiceDescriptor = new ServiceDescriptorLocalBase(
				Services.FlushingTriggerService.name(), 
				FlushingTriggerService.class,
				flushingTriggerService);
		if (AppLogger.isDebugEnabled(logger))
			logger.debug("...created flushingTriggerServiceDescriptor!");
		
		// Stores readings in memory
		if (AppLogger.isDebugEnabled(logger))
			logger.debug("Creating observationRepositoryDescriptor...");
		ServiceDescriptorLocal observationRepositoryDescriptor = new ServiceDescriptorLocalBase(
				Services.ObservationRepository.name(),
				ObservationRepository.class, 
				new ObservationRepositoryImpl(),
				Services.LocalRepository.name());
		if (AppLogger.isDebugEnabled(logger))
			logger.debug("...created observationRepositoryDescriptor!");
		

		
		// Service that piggy-backs on the Android "LocationListener" to get location updates
		//	- Gets android "LocationManager" from the application settings
		if (AppLogger.isDebugEnabled(logger))
			logger.debug("Creating locationServiceDescriptor...");
		ServiceDescriptorLocal locationServiceDescriptor = new ServiceDescriptorLocalBase(
				Services.LocationService.name(), 
				LocationService.class,
				new LocationServiceImpl());
		if (AppLogger.isDebugEnabled(logger))
			logger.debug("...created locationServiceDescriptor!");
		
		// This creates a thread that polls the SD service and uses the Local Repository (SD card)
		//	also depends on the Location Service (android location) to figure out which SD site to use
//		if (AppLogger.isDebugEnabled(logger))
//			logger.debug("Creating sanDiegoSensorReadingPublisherServiceDescriptor...");
//		ServiceDescriptorLocal sanDiegoSensorReadingPublisherServiceDescriptor 
//			= new ServiceDescriptorLocalBase(
//				Services.SanDiegoSensorReadingPublisherService.name(),
//				(Class) null, // Doesn't implement a known interface, other services don't depend on it...
//				new SanDiegoSensorReadingPublisherService(),
//				Services.LocationService.name(),
//				Services.LocalRepository.name(),
//				Services.ObservationRepository.name());
//		if (AppLogger.isDebugEnabled(logger))
//			logger.debug("...created sanDiegoSensorReadingPublisherServiceDescriptor!");

		// Starts the bluetooth chat service (handles low-level BT communication) and then handles
		//	the messages & interprets the packet formats
		if (AppLogger.isDebugEnabled(logger))
			logger.debug("Creating bluetoothServiceDescriptor...");
		BluetoothService blueToothServiceImpl = new BluetoothServiceImpl();
		ServiceDescriptorLocal bluetoothServiceDescriptor = new ServiceDescriptorLocalBase(
				Services.BluetoothService.name(), 
				BluetoothService.class,
				blueToothServiceImpl, 
				Services.LocalRepository.name(),
				Services.ObservationRepository.name(), 
				Services.LocationService.name());
		if (AppLogger.isDebugEnabled(logger))
			logger.debug("...created bluetoothServiceDescriptor!");
		
		// Sends a sensor reading to the observation repository (in-memory storage), only for testing
		// A local service - used to test delivery of a sensor reading.
		// TODO: Should be removed once everything is confirmed to be working
		// with the real sensor.
		if (AppLogger.isDebugEnabled(logger))
			logger.debug("Creating sensorServiceTestDescriptor...");
		ServiceDescriptorLocal sensorServiceTestDescriptor = new ServiceDescriptorLocalBase(
				Services.SensorServiceTest.name(), SensorServiceTest.class,
				new SensorServiceTestImpl(), Services.ObservationRepository
						.name());
		if (AppLogger.isDebugEnabled(logger))
			logger.debug("...created sensorServiceTestDescriptor!");
		
		// ?? Connects to what?....
		// ServiceDataConnector to cloud
		if (AppLogger.isDebugEnabled(logger))
			logger.debug("Creating cloudServiceDescriptor...");
		ServiceDescriptor cloudServiceDescriptor = new ServiceDescriptorBase(
				Services.CloudService.name(), 
				Arrays.<Class> asList(ObservationRepository.class));
		if (AppLogger.isDebugEnabled(logger))
			logger.debug("...created cloudServiceDescriptor!");
		
		// Recieves messages (?? FROM WHO ??) and uses HTTP requests to store the readings on the server
		if (AppLogger.isDebugEnabled(logger))
			logger.debug("Creating richServiceConnector...");
		ServiceDataConnectorForRichService<ServiceDescriptor> richServiceConnector 
			= new ServiceDataConnectorForRichServiceHTTPClient(
				cloudServiceDescriptor, 
				applicationSettings.serverAddCompressedUri(),
				applicationSettings.timeoutConnect(), 
				applicationSettings.timeoutOperation());
		if (AppLogger.isDebugEnabled(logger))
			logger.debug("...created richServiceConnector!");
		
		// ** CONTINUE HERE **
		// Service that accepts the calls from activities
		if (AppLogger.isDebugEnabled(logger))
			logger.debug("Creating exposedServicesDescriptor...");
		ServiceDescriptor exposedServicesDescriptor = new ServiceDescriptorLocalBase(
				Services.ExposedServices.name(), (Class) null,
				new CitiSenseExposedServices(),
				Services.LocationService.name(),
				Services.SensorServiceTest.name(),
				Services.ObservationRepository.name(),
				Services.BluetoothService.name(),
				Services.LocalRepository.name(),
				Services.FlushingTriggerService.name());
		if (AppLogger.isDebugEnabled(logger))
			logger.debug("...created exposedServicesDescriptor!");
		
		/**************************************************************************************
		 ************************************* POLICIES ***************************************
		 *************************************************************************************/
		// POLICY 1 - for exceptions on connectivity
		// Map<String, String> rerouteTable = new HashMap<String, String>();
		// rerouteTable.put("CloudService", "ObservationRepository");
		// // rerouteTable.put("ComputationService", "CloudService");
		// ReroutingOnExceptionPolicy routeOnErrorPolicy = new
		// ReroutingOnExceptionPolicy();
		// routeOnErrorPolicy.setReroutingTable(rerouteTable);

		// POLICY 2 - for connectivity notifications
		// ReroutingOnNotificationPolicy notificationBasedRoutingPolicy = new
		// ReroutingOnNotificationPolicy();
		// notificationBasedRoutingPolicy.addRoutingRule("connectivity", "off",
		// "connectivity", "on", "CloudService", "ObservationRepository");

		// POLICY 3 - for logging
		// Policy loggingPolicy = new LoggingPolicy();

		// Policy sdReroutePolicy = new LocalRepositorySelectionPolicy(
		// blueToothServiceImpl);

		/**************************************************************************************
		 ************************************* BUILD RICHSERVICE ******************************
		 *************************************************************************************/
		if (AppLogger.isDebugEnabled(logger))
			logger.debug("Creating buildRichService...");
		RichService rs = RichServiceBuilder.buildRichService(
				richServiceConnector, observationRepositoryDescriptor, flushingTriggerServiceDescriptor,
				bluetoothServiceDescriptor, sensorServiceTestDescriptor,
				locationServiceDescriptor, localRepositoryDescriptor,// combinedRepositoryDescriptor,
//				sanDiegoSensorReadingPublisherServiceDescriptor,
				exposedServicesDescriptor);
		if (AppLogger.isDebugEnabled(logger))
			logger.debug("...created buildRichService!");
		
		// RichServiceBuilder.addServiceDataConnector(rs,
		// connectivityNotificationService.getServiceDataConnector(),
		// observationFlushingService.getServiceDataConnector());
		if (AppLogger.isDebugEnabled(logger))
			logger.debug("Creating addServiceDataConnector...");
		RichServiceBuilder.addServiceDataConnector(rs,
				connectivityNotificationService.getServiceDataConnector(),
				observationFlushingService.getServiceDataConnector(),
				flushingTriggerService.getServiceDataConnector());
		if (AppLogger.isDebugEnabled(logger))
			logger.debug("...created addServiceDataConnector!");
		
		// RichServiceBuilder.addPoliciesTo(rs, routeOnErrorPolicy,
		// notificationBasedRoutingPolicy);
		// RichServiceBuilder.addPoliciesTo(rs, sdReroutePolicy);

		/**************************************************************************************
		 *************************************************************************************/
		// sensor = (SensorServiceTestImpl) ((ServiceDataConnectorJavaLocal) (rs
		// .getServiceDataConnector("SensorServiceTest"))).getService()
		// .getExposedImplementation();
		// localRepository = (LocalRepository) ((ServiceDataConnectorJavaLocal)
		// (rs
		// .getServiceDataConnector("LocalRepository"))).getService()
		// .getExposedImplementation();
		// observationRepository = (ObservationRepository)
		// ((ServiceDataConnectorJavaLocal) (rs
		// .getServiceDataConnector("ObservationRepository")))
		// .getService().getExposedImplementation();
		if (AppLogger.isDebugEnabled(logger))
			logger.debug("Creating exposedServices...");
		exposedServices = (CitiSenseExposedServices) ((ServiceDataConnectorJavaLocal) (rs
				.getServiceDataConnector(Services.ExposedServices.name())))
				.getService().getExposedImplementation();
		if (AppLogger.isDebugEnabled(logger))
			logger.debug("...created exposedServices!");
	}

	public CitiSenseExposedServices getCitiSenseExposedServices() {
		return exposedServices;
	}

	public void close() {
		// FIXME: Kill any threads etc related to the rich service.
		exposedServices.disconnectSensor();
		exposedServices.closeDatabase();
	}
}
