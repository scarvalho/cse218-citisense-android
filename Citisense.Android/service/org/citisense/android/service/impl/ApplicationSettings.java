package org.citisense.android.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.citisense.android.R;
import org.citisense.datastructure.SensorType;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Handler;
import android.os.PowerManager;
import android.telephony.TelephonyManager;

import com.google.code.microlog4android.Logger;
import com.google.code.microlog4android.LoggerFactory;
import com.google.code.microlog4android.appender.Appender;
import com.google.code.microlog4android.appender.FileAppender;
import com.google.code.microlog4android.appender.LogCatAppender;
import com.google.code.microlog4android.config.PropertyConfigurator;

/**
 * Date format we use in Java: "M/d/yyyy h:mm:ss a" <br/>
 * Date format we use in Python: "%m/%d/%Y %I:%M:%S %r"
 * 
 * @author celal.ziftci
 * 
 */
public class ApplicationSettings {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(ApplicationSettings.class);

	private Context context;

	private LocationManager locationManager;
	private PowerManager powerManager;
	private String uniquePhoneId;

	private final String PREFERENCES_NAME = "CITISENSE";
	private SharedPreferences preferences;
	
	// Used for call backs to the UI
	private Handler uiHandler;
	private boolean uiHandlerActive = false;

	/**
	 * SINGLETON
	 */
	private static final ApplicationSettings instance = new ApplicationSettings();

	public static final ApplicationSettings instance() {
		return instance;
	}

	private ApplicationSettings() {
	}

	public LocationManager locationManager() {
		return locationManager;
	}
	
	public PowerManager powerManager() {
		return powerManager;
	}

	public SharedPreferences preferences() {
		return preferences;
	}
	
	public Context context() {
		return context;
	}
	
	public Handler uiHandler() {
		return uiHandler;
	}
	
	public void setUiHandler(Handler handler) {
		uiHandler = handler;
	}
	
	public boolean isUiHandlerActive() {
		return uiHandlerActive;
	}
	
	public void setUiHandlerActive(boolean isActive) {
		uiHandlerActive = isActive;
	}

	public void setPreferences(SharedPreferences preferences) {
		this.preferences = preferences;
	}

	public String sanDiegoPollutionWebSite() {
		return getString(R.string.sanDiegoPollutionWebSite);
	}

	public String serverHost() {
		return getString(R.string.serverHost);
	}

	/**
	 * This is the port that listens to Android phones to connect
	 */
	public int serverPort() {
		return getInteger(R.integer.serverPort);
	}

	public String serverAddCompressedUri() {
		return getString(R.string.serverAddCompressedUri);
	}
	
	public String serverUpdateRegionsUri() {
		return getString(R.string.serverUpdateRegionsUri);
	}

	public String serverHeartBeatUri() {
		return getString(R.string.serverHeartBeatUri);
	}
	
	public String serverRequestMapUrlRoot() {
		return getString(R.string.serverRequestMapUrlRoot);
	}
	
	public String serverApprovalMapUrlRoot() {
		return getString(R.string.serverApprovalMapUrlRoot);
	}
	

	/**
	 * Timeout setting for checking how frequently to check if there is
	 * connectivity to the backend server that stores the sensor readings (in
	 * milliseconds).
	 */
	public int connectivityCheckFrequency() {
		return getInteger(R.integer.connectivityCheckFrequency);
	}

	/**
	 * Timeout setting for flushing the locally cached sensor readings to the
	 * backend server that stores the sensor readings (in milliseconds).
	 */
	public int flushingTriggerFrequency() {
		return getInteger(R.integer.flushingTriggerFrequency);
	}

	/**
	 * Timeout setting for the connection to the backend (in milliseconds)
	 */
	public int timeoutConnect() {
		return getInteger(R.integer.timeoutConnect);
	}
	
	/**
	 * Maximum number of seconds worth of readings to flush at a time
	 */
	public int maxSecondsOfDataToFlush() {
		return getInteger(R.integer.maxSecondsOfDataToFlush);
	}

	/**
	 * Timeout setting for an operation call to the backend (in milliseconds).
	 */
	public int timeoutOperation() {
		return getInteger(R.integer.timeoutOperation);
	}

	/**
	 * Delay between sensor connection attempts
	 */
	public int sensorConnectDelay() {
		return getInteger(R.integer.sensorConnectDelay);
	}

	/**
	 * Timeout setting for polling the San Diego pollution web server (in
	 * milliseconds).
	 */
	public int sanDiegoPollutionWebSitePollFrequency() {
		return getInteger(R.integer.sanDiegoPollutionWebSitePollFrequency);
	}
	
	public int maxAqiUpdateFrequency() {
		return getInteger(R.integer.maxAqiUpdateFrequency);
	}
	
	public int uiServicePollingFrequency() {
		return getInteger(R.integer.uiServicePollingFrequency);
	}

	public int AVG_SENSOR_READING_PAYLOAD_LENGTH() {
		return getInteger(R.integer.AVG_SENSOR_READING_PAYLOAD_LENGTH);
	}

	public int AVG_SDAPC_WEB_REPORT_LENGTH() {
		return getInteger(R.integer.AVG_SDAPC_WEB_REPORT_LENGTH);
	}

	public String bitlyUser() {
		return getString(R.string.bitlyUser);
	}
	
	public String bitlyAPIkey() {
		return getString(R.string.bitlyAPIkey);
	}
	
	public String bitlyQueryEncoding() {
		return getString(R.string.bitlyQueryEncoding);
	}
	
	public boolean isNodeStationary() {
		return getString(R.string.isNodeStationary).equals("true");
	}
	
	public float startingLatitude() {
		return Float.parseFloat(getString(R.string.startingLatitude));
	}
	
	public float startingLongitude() {
		return Float.parseFloat(getString(R.string.startingLongitude));
	}
	
	public String getSensorName(int pinNumber) {
		return SensorType.getNameFor(pinNumber);
	}

	public void setupApplicationSettings(Context context) {
		this.context = context;
		this.locationManager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);
		this.powerManager = (PowerManager) context
				.getSystemService(Context.POWER_SERVICE);
		this.preferences = context.getSharedPreferences(PREFERENCES_NAME, 0);
		this.uniquePhoneId = ((TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();

		// System.setErr(new PrintStream(new AndroidSystemOutputStream(
		// "System.err")));
		// System.setOut(new PrintStream(new AndroidSystemOutputStream(
		// "System.out")));
		PropertyConfigurator.getConfigurator(context).configure();
		prepareM4ALoggers();
		if (AppLogger.isDebugEnabled(logger)) {
			logger.debug("connectivityCheckFrequency: {}", this
					.connectivityCheckFrequency());
			logger.debug("flushingTriggerFrequency: {}", this
					.flushingTriggerFrequency());
			logger.debug("sanDiegoPollutionWebSite: {}", this
					.sanDiegoPollutionWebSite());
			logger.debug("sanDiegoPollutionWebSitePollFrequency: {}", this
					.sanDiegoPollutionWebSitePollFrequency());
			logger.debug("sensorConnectDelay: {}", this.sensorConnectDelay());
			logger.debug("serverHost: {}", this.serverHost());
			logger.debug("serverPort: {}", this.serverPort());
			logger.debug("serverAddCompressedUri: {}", this.serverAddCompressedUri());
			logger.debug("serverHeartBeatUri: {}", this.serverHeartBeatUri());
			logger.debug("timeoutConnect: {}", this.timeoutConnect());
			logger.debug("timeoutOperation: {}", this.timeoutOperation());
			logger.debug("AVG_SENSOR_READING_PAYLOAD_LENGTH: {}", this
					.AVG_SENSOR_READING_PAYLOAD_LENGTH());
			logger.debug("AVG_SDAPC_WEB_REPORT_LENGTH: {}", this
					.AVG_SDAPC_WEB_REPORT_LENGTH());
		}
	}

	/**
	 * FIXME: The file appender is not being properly "opened" after it is
	 * configured here by
	 * {@link ApplicationSettings#setupApplicationSettings(Context)}. This is
	 * because the logger factory is already configured when it is used for
	 * logging before any of the android related stuff starts (like rich
	 * services etc). So it must be opened after the setup manually.
	 * <p/>
	 * Also, the FileAppender is somehow by default not appending, it must be
	 * made to append. Very sad...
	 * <p/>
	 * This is pretty nasty, a better solution is to actually make sure logging
	 * is set up once and configured at the very beginning of the application.
	 * For this, application lifecycle callbacks must be found, which I couldn't
	 * do personally (Celal).
	 */
	private static void prepareM4ALoggers() {
		Logger logger = LoggerFactory.getLogger(ApplicationSettings.class);
		List<LogCatAppender> logcatAppenders = new ArrayList<LogCatAppender>();

		int totalAppenders = logger.getNumberOfAppenders();
		for (int i = 0; i < totalAppenders; i++) {
			try {
				Appender appender = logger.getAppender(i);

				// Make sure we only append to log files, not overwrite them!
				if (appender instanceof FileAppender) {
					((FileAppender) appender).setAppend(true);
				}

				// If there are multiple instances of logcat-appenders, keep the
				// last one.
				if (appender instanceof LogCatAppender) {
					logcatAppenders.add((LogCatAppender) appender);
				}

				// Call open on all appenders: this makes sure FileAppender
				// works properly!
				appender.open();
			} catch (IOException e) {
				throw new RuntimeException(
						"Cannot start application, because cannot configure logging",
						e);
			}
		}

		if (logcatAppenders.size() > 1) {
			for (int i = 1; i < logcatAppenders.size(); i++) {
				logger.removeAppender(logcatAppenders.get(i));
			}
		}
	}

	private int getInteger(int R$id) {
		throwIfContextNull();
		return context.getResources().getInteger(R$id);
	}

	private String getString(int R$id) {
		throwIfContextNull();
		return context.getResources().getString(R$id);
	}

	private void throwIfContextNull() {
		final String error = "Using ApplicationSettings before it is initialized.";
		if (context == null) {
			if(AppLogger.isErrorEnabled(logger)) {
				logger.error(error);
			}
			throw new RuntimeException(error);
		}
	}

	// ***************************************************************************
	// TO BE REMOVED SOON...
	// ***************************************************************************
	/**
	 * These constants are generated when a new study is started and a device is
	 * put into it. They should all be in the sensor, but for now, they are here
	 * because we are generating fake data.<br/>
	 * TODO: Remove these, no need anymore!
	 */
	private String studyID = "4cca2c5417180000000057ea";
	private String subjectID = "4cca5b3517180000000057eb";

	public String studyID() {
		return this.studyID;
	}

	public String subjectID() {
		return subjectID;
	}
	
	public Collection<String> getAllSensorIDs() {
		Collection<String> ids = new ArrayList<String>();
		for (SensorType sensorType : SensorType.values()) {
			ids.add(sensorIDForSensorType(sensorType.getPinNumber()));
		}
		return ids;
	}
	
	public String getPhoneID() {
		return uniquePhoneId;
	}
	
	public String sensorIDForSensorType(int pinNumber) {
		return uniquePhoneId + "-" + pinNumber;
	}
	public SensorType getSensorTypeFromSensorID(String sensorId){
		return SensorType.getSensorTypeFor(Integer.parseInt(sensorId.split("-")[1]));
	}
}