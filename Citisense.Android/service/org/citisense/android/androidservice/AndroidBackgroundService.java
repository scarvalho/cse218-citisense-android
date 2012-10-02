package org.citisense.android.androidservice;

import org.citisense.android.AndroidRichService;
import org.citisense.android.Aqi_ui;
import org.citisense.android.CustomExceptionHandler;
import org.citisense.android.R;
import org.citisense.android.profiler.DeviceStateRecorder;
import org.citisense.android.service.impl.AppLogger;
import org.citisense.android.service.impl.ApplicationSettings;
import org.citisense.android.service.impl.CitiSenseExposedServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class AndroidBackgroundService extends Service {

	private AndroidRichService citisense;
	private LocalBinder<CitiSenseExposedServices> binder;
	private DeviceStateRecorder stateRecorder;
	
	private static final Logger logger = LoggerFactory
			.getLogger(AndroidBackgroundServiceStarter.class);

	// LIFECYCLE METHODS
	/**
	 * Do all your initialisation here.
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		
		// Try to catch an unhandled exception in the main service thread
		Thread.currentThread().setUncaughtExceptionHandler(new CustomExceptionHandler());
		
		ApplicationSettings.instance().setupApplicationSettings(this);

		Notification notification = new Notification(R.drawable.app_icon, 
				"CitiSense", System.currentTimeMillis());
		Intent notificationIntent = new Intent(this, Aqi_ui.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, 
				notificationIntent, 0);
		notification.setLatestEventInfo(this, "CitiSense", 
				"Air Quality Monitoring", pendingIntent);
		startForeground(ApplicationSettings.instance().hashCode(), notification);
		
		try {
			citisense = new AndroidRichService();
		} catch (Exception e) {
			throw new RuntimeException("Cannot instantiate AndroidRichService",
					e);
		}
		// Some sort of helper method to initialise our resource
		// Initialise our Binder that will be passed to onServiceConnected
		binder = new LocalBinder<CitiSenseExposedServices>(citisense
				.getCitiSenseExposedServices());
		stateRecorder = new DeviceStateRecorder(this);
		//stateRecorder.start();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startid) {
		return START_STICKY;
	}

	/**
	 * Called by system when bound to pass a Binder back
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	/**
	 * Called when all Activities are unbound
	 */
	public boolean onUnbind(Intent intent) {
		return true;
	}

	/**
	 * Called by system when the service is destroyed. Perform cleanup here.
	 */
	@Override
	public void onDestroy() {
		// Added for debug purposes
		if(AppLogger.isWarnEnabled(logger))
			logger.warn("The service had it's onDestroy() method called.");
		citisense.close();
		binder = null;
		stateRecorder.stop();
	}
}