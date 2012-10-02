package org.citisense.android.profiler;


import java.util.Timer;

import org.citisense.android.profiler.listener.CellStateListener;
import org.citisense.android.profiler.listener.GpsStatusListener;
import org.citisense.android.profiler.log.LogWriter;
import org.citisense.android.profiler.receiver.BatteryStateReceiver;
import org.citisense.android.profiler.receiver.BluetoothStateReceiver;
import org.citisense.android.profiler.receiver.ScreenReceiver;
import org.citisense.android.profiler.receiver.WifiStateReceiver;
import org.citisense.android.profiler.task.LogUploaderTask;
import org.citisense.android.profiler.task.TrafficStatsLogger;

import android.content.Context;
import android.util.Log;


public class DeviceStateRecorder {
	
	private static final String TAG = "DeviceStateRecoder";
	
	private BatteryStateReceiver batteryInfoReceiver = new BatteryStateReceiver();

	private BluetoothStateReceiver blueToothReceiver = new BluetoothStateReceiver();

	private GpsStatusListener gpsListener = new GpsStatusListener();

	private WifiStateReceiver networkReceiver = new WifiStateReceiver();

	private CellStateListener phoneListener = new CellStateListener();

	private ScreenReceiver screenReceiver = new ScreenReceiver();

	private Timer trafficTimer;

	private Timer logTimer;

	private Context context;
	
	public DeviceStateRecorder(Context c){
		context=c;
		LogWriter.getInstance().setContext(c);
		Log.i(TAG, "onCreate");
		Log.i(TAG, "thread name: " + Thread.currentThread().getName());
	}

	private void launchTasks() {
		scheduleTrafficStatsLogger();
		LogUploaderTask logUploader=new LogUploaderTask(context);
		logTimer=new Timer("log_uploader");
		logTimer.scheduleAtFixedRate(logUploader, 0, 1000*60);
	}
	
	public void scheduleTrafficStatsLogger() {
		trafficTimer=new Timer("traffic_stats");
		//trafficTimer.scheduleAtFixedRate(tsl, 0, 1000*60);
		TrafficStatsLogger trafficLogger = new TrafficStatsLogger(this,context);
		trafficTimer.schedule(trafficLogger, 1000*60);
	}

	private void registerReceivers() {
		batteryInfoReceiver.register(context);
		networkReceiver.register(context);
		blueToothReceiver.register(context);
		screenReceiver.register(context);
		phoneListener.listen(context);
		gpsListener.listen(context);
	}
	
	public void stop(){
		unRegisterReceivers();
		cancelTimers();
	}
	
	public void start(){
		registerReceivers();
		launchTasks();
	}
	
	private void cancelTimers() {
		trafficTimer.cancel();
		logTimer.cancel();
	}

	private void unRegisterReceivers() {
		batteryInfoReceiver.unRegister();
		networkReceiver.unRegister();
		blueToothReceiver.unRegister();
		screenReceiver.unRegister();
		phoneListener.unRegister();
		gpsListener.unRegister();
	}
}
