package org.citisense.android;

import static org.citisense.datastructure.BluetoothConstants.REQUEST_CONNECT_DEVICE;

import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import org.citisense.android.androidservice.AndroidBackgroundServiceStarter;
import org.citisense.android.androidservice.LocalBinder;
import org.citisense.android.bluetooth.BluetoothChatService;
import org.citisense.android.service.impl.AppLogger;
import org.citisense.android.service.impl.ApplicationSettings;
import org.citisense.android.service.impl.CitiSenseExposedServices;
import org.citisense.android.service.impl.HttpUtils;
import org.citisense.android.share.ShareDialogue;
import org.citisense.datastructure.SensorReading;
import org.citisense.datastructure.SensorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.telephony.SmsManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class Aqi_ui extends Activity {	
	private final Logger logger = LoggerFactory.getLogger(Aqi_ui.class);

	private final static int AQI_CONNECTING = -2;
	private final static int AQI_ERROR = -1;
	
	// The maximum value that fits on the AQI value bar
	private final static double MAX_BAR_AQI_VAL = 300.0;
	
	private final static int REQUEST_FACEBOOK_SHARE = 200;
	
	private int currentAqi = AQI_CONNECTING;
	private Date currentReadingTime = null;
	private SensorType currentUnits = null;
//	private Date lastUpdateTime = null;
	private TextView readingLabel;
	private TextView aqiTextView;
	private ImageView cloudImageView;
	private TextView aqiMeaningText;
	private TextView aqiReadingTimeText;
//	private TextView updatedTimeAgoText;
	private Button sensorConnect;
	private View aqiBarLine;
	private ImageView aqiBarArrow;
	private View aqiBar;
	private boolean isScreenLocked;
	
	private CitiSenseExposedServices exposedServices;
	
	private static final int HideCloud = -1;
	private static final Map<Integer, AqiDisplay> aqiDisplayValues; 
	static {
		
		TreeMap<Integer,AqiDisplay> map = new TreeMap<Integer, AqiDisplay>();
		map.put(AQI_CONNECTING,  
				     new AqiDisplay(R.string.aqi_connecting,     R.drawable.cloud_grey));
		map.put(AQI_ERROR,  
					 new AqiDisplay(R.string.aqi_error,          R.drawable.cloud_red));
		//map.put(0,   new AqiDisplay(R.string.aqi_not_available,  R.drawable.cloud_grey));
		map.put(50,  new AqiDisplay(R.string.aqi_good,           R.drawable.cloud_green));
		map.put(100, new AqiDisplay(R.string.aqi_moderate,       R.drawable.cloud_yellow));
		map.put(150, new AqiDisplay(R.string.aqi_sensitive,      R.drawable.cloud_orange));
		map.put(200, new AqiDisplay(R.string.aqi_unhealthy,      R.drawable.cloud_red));
		map.put(300, new AqiDisplay(R.string.aqi_very_unhealthy, R.drawable.cloud_purple));
		map.put(Integer.MAX_VALUE, 
				     new AqiDisplay(R.string.aqi_hazardous,      R.drawable.cloud_maroon));
		aqiDisplayValues = Collections.unmodifiableMap(map);
	}
	
	/////////////////////////////////////////////
	// Android Activity life cycle
	/////////////////////////////////////////////
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if(AppLogger.isInfoEnabled(logger))
			logger.info("Created app");
		
		// Try to catch an unhandled exception in the main service thread
		Thread.currentThread().setUncaughtExceptionHandler(new CustomExceptionHandler());
		
		// Get needed views
		setContentView(R.layout.main);
		readingLabel   	   = (TextView)  findViewById(R.id.readingLabel);
		aqiTextView    	   = (TextView)  findViewById(R.id.AqiTextView);
		cloudImageView 	   = (ImageView) findViewById(R.id.CloudView);
		aqiMeaningText 	   = (TextView)  findViewById(R.id.aqi_meaning);
		aqiReadingTimeText = (TextView)  findViewById(R.id.readingTime);
//		updatedTimeAgoText = (TextView)  findViewById(R.id.updatedTimeAgo);
		sensorConnect  	   = (Button)    findViewById(R.id.bluetoothButton);
		aqiBarLine		   = (View)		 findViewById(R.id.aqiBarView_line);
		aqiBarArrow		   = (ImageView) findViewById(R.id.aqiBarView_arrow);
		aqiBar			   = (View)		 findViewById(R.id.aqiBarView);
		
		isScreenLocked = false;
		
		bindToService();
		new UpdateAqiDisplay().execute();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		if(AppLogger.isInfoEnabled(logger))
			logger.info("Started app");
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if(AppLogger.isInfoEnabled(logger))
			logger.info("Resumed app");
		ApplicationSettings.instance().setUiHandler(aqi_ui_handler);
		ApplicationSettings.instance().setUiHandlerActive(true);
		checkBluetoothConnection();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		if(AppLogger.isInfoEnabled(logger))
			logger.info("Stopped app");
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		if(AppLogger.isInfoEnabled(logger))
			logger.info("Restart app");
	}

	@Override
	protected void onPause() {
		super.onPause();
		ApplicationSettings.instance().setUiHandlerActive(false);
		if(AppLogger.isInfoEnabled(logger))
			logger.info("Paused app");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// Unbind on destroy so that we don't leak service connections
		if (serviceBindCallback != null) {
			this.unbindService(serviceBindCallback);
		}
		if(AppLogger.isInfoEnabled(logger))
			logger.info("Destroyed app");
	}
	
	/////////////////////////////////////////////
	// Handler used for callbacks
	// Allow us to update UI elements without polling
	/////////////////////////////////////////////
	Handler aqi_ui_handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				// Bluetooth connection established
				case UICallbackTypes.AQI_UI_BLUETOOTH_ON:
					sensorConnect.setVisibility(View.INVISIBLE);
					break;
				// Bluetooth connection lost
				case UICallbackTypes.AQI_UI_BLUETOOTH_OFF:
					sensorConnect.setVisibility(View.VISIBLE);
					break;
				case UICallbackTypes.AQI_UI_NEW_AQI:
					SensorReading newAqiReading = (SensorReading) msg.obj;
					if(newAqiReading != null) {
						currentAqi = (int)newAqiReading.getSensorData();
						currentReadingTime = newAqiReading.getTimeDate();
						currentUnits = (newAqiReading.getLocation() != null && newAqiReading.getLocation().getProvider() != null) 
							? SensorType.valueOf(newAqiReading.getLocation().getProvider()) 
							: null;
						new UpdateAqiDisplay().execute();
					} else {
						if(AppLogger.isWarnEnabled(logger))
							logger.warn("The UI was sent a NULL SensorReading! Should not happen!");
					}
					if(this.hasMessages(UICallbackTypes.AQI_UI_NEW_AQI)) {
						if(AppLogger.isWarnEnabled(logger))
							logger.warn("The UI has more than one AQI update pending! Should not happen!");
					}
					break;
				default:
					break;
			}
		}
	};
	
//	private Handler timeUpdateHandler = new Handler();
//	private Runnable updateTimeTask = new Runnable(){
//
//		@Override
//		public void run() {
//			if( lastUpdateTime != null ){
//				Date now = new Date();
//				long delta = now.getTime() - lastUpdateTime.getTime();
//				long mins = (delta % (hourInMillis)) / minutesInMillis;
//				if(mins < 1){
//					updatedTimeAgoText.setText("updated just now");
//				}else if(mins == 1){
//					updatedTimeAgoText.setText("updated 1 minute ago");
//				}else{
//					updatedTimeAgoText.setText("updated " + mins + " minutes ago");
//				}
//			}
//			timeUpdateHandler.postDelayed(this, minutesInMillis/2);
//		}
//		
//	};
	
	/////////////////////////////////////////////
	// Updating the AQI on the UI
	/////////////////////////////////////////////	
	private class UpdateAqiDisplay extends AsyncTask<Void, Void, AqiDisplay> {
		protected AqiDisplay doInBackground(Void... voids) {
			if (AppLogger.isInfoEnabled(logger)) logger.info("Updating AQI, value = " + currentAqi);
			for( int level : aqiDisplayValues.keySet() ) {
				if (AppLogger.isDebugEnabled(logger)) logger.debug("Checking UI level: " + level);
				if( currentAqi <= level ){
					if (AppLogger.isInfoEnabled(logger)) logger.info("Displaying AQI, for level = " + level);
					return aqiDisplayValues.get(level);
				}
			}
			return null;
		}
		
		protected void onPostExecute(AqiDisplay display) {
			if(display != null) {
				aqiMeaningText.setText(display.meaningId);
				if( display.imageId == HideCloud){
					cloudImageView.setVisibility(View.INVISIBLE);
				} else {
					cloudImageView.setVisibility(View.VISIBLE);
					cloudImageView.setImageResource(display.imageId);
				}
			}
			
			if( currentAqi >= 0 ){
				readingLabel.setVisibility(View.VISIBLE);
				aqiBarLine.setVisibility(View.VISIBLE);
				aqiBarArrow.setVisibility(View.VISIBLE);
				
				// Adjust location of line on bar
				double aqiRatio = (1.0 * currentAqi) / MAX_BAR_AQI_VAL;
				if(aqiRatio > 1.0)
					aqiRatio = 1.0;
				
				int bar_width = aqiBar.getWidth();
				int line_left = (int)(bar_width * aqiRatio);
				Animation lineAnimation = new TranslateAnimation(line_left - aqiBarLine.getLeft(), 
						line_left - aqiBarLine.getLeft(), 0, 0);
				lineAnimation.setDuration(500);
				lineAnimation.setFillAfter(true);
				aqiBarLine.startAnimation(lineAnimation);
				aqiBarArrow.startAnimation(lineAnimation);
				
				aqiTextView.setText(Integer.toString(currentAqi));
				String units = (currentUnits != null) ? currentUnits.toString() : "";
				aqiReadingTimeText.setText(units + " at " + DateFormat.getTimeInstance(DateFormat.SHORT).format(currentReadingTime));				
			} else if( currentAqi == AQI_ERROR){
				aqiTextView.setText("!");
				aqiBarLine.setVisibility(View.INVISIBLE);
				aqiBarArrow.setVisibility(View.INVISIBLE);
			} else{
				readingLabel.setVisibility(View.INVISIBLE);
				aqiBarLine.setVisibility(View.INVISIBLE);
				aqiBarArrow.setVisibility(View.INVISIBLE);
				aqiTextView.setText("");
			}
			if (AppLogger.isInfoEnabled(logger)) logger.info("Updated AQI to " + aqiMeaningText.getText().toString());		
		}
	}

	/////////////////////////////////////////////
	// Sharing features
	/////////////////////////////////////////////
	public void share(View view) {
		if(AppLogger.isInfoEnabled(logger))
			logger.info("Begin Share");
		String share_subject = new String(
				"Check out what I'm breathing right now!");
		
		String share_body = getTextContent(3600);

		final Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_SUBJECT, share_subject);
		intent.putExtra(Intent.EXTRA_TEXT, share_body);

		if(AppLogger.isInfoEnabled(logger))
			logger.info("Sharing with other apps");
		startActivity(Intent.createChooser(intent, getString(R.string.share)));
	}
	
	public String getTextContent(long secondsToShare){
		aqiMeaningText.toString();
		
		String mapPortion = "";
		
		try {
			long curTime = (long) System.currentTimeMillis() / 1000;
			mapPortion = " map: " + HttpUtils.getBitlyRedirectUrlForMap(curTime - secondsToShare, curTime);
		} catch (Exception e) {
			if(AppLogger.isWarnEnabled(logger))
				logger.warn("Exception getting bitly map url", e);
			return null;
		}
		
		String textSubject = new String("My current air quality is " 
										+ aqiMeaningText.getText().toString()
										+ " (Air Quality Index = " 
										+ Integer.toString(currentAqi) 
										+") in " 
										+ "San Diego"
										+ "."
										+ mapPortion);

		if(AppLogger.isInfoEnabled(logger))
			logger.info("Sharing text: " + textSubject);
		return textSubject;
	}

	public void sendSMS(boolean facebook, long secondsToShare) {
		SmsManager manager = SmsManager.getDefault();
		
		String phoneNumber = (facebook ? "32665" : "40404");
		String messageContent = getTextContent(secondsToShare);
		if(messageContent != null) {
			manager.sendTextMessage(phoneNumber, null, messageContent, null, null);
			
			// Upload any queued up data while you are at it
			exposedServices.flushAllData();
			
			Toast.makeText(getApplicationContext(), 
					"Posted update to " + (facebook ? "Facebook" : "Twitter") + "." , 
					Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(getApplicationContext(), 
					"Connectivity error when attempting post..." , 
					Toast.LENGTH_LONG).show();
		}
		
	}
	
	public void sendFacebookSMS(View view) {
		if(AppLogger.isInfoEnabled(logger))
			logger.info("facebook button pressed");
		
		// Prompt the user to select how much data to share
		Intent serverIntent = new Intent(this, ShareDialogue.class);
		startActivityForResult(serverIntent, REQUEST_FACEBOOK_SHARE);
	}
	
	public void sendTwitterSMS(View view) {
		if(AppLogger.isInfoEnabled(logger))
			logger.info("twitter button pressed");

		sendSMS(false, 3600);
	}
	
	/////////////////////////////////////////////
	// UI Interactions
	/////////////////////////////////////////////
	
	public void detailedView(View view) {
		if(AppLogger.isInfoEnabled(logger))
			logger.info("User tap for detail screen");
		Intent myIntent = new Intent(view.getContext(), Aqi_ui_detailed.class);

		// TODO: TJ Decide if we should get the data here to send to details, or let it get it...		
		startActivityForResult(myIntent, 0);
	}
	
	public void helpDialog(View view) {
		Dialog dialog = new Dialog(this, android.R.style.Theme_Dialog);
		dialog.setContentView(R.layout.aqi_help_dialog);
		dialog.setTitle("Pollution Information");
		dialog.setCancelable(true);
		dialog.show();
	}

	public void connectToSensor(View view){
		if(AppLogger.isInfoEnabled(logger))
			logger.info("connect to sensor button pressed");
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("You are not currently connected to your sensor board.\n\n" +
				"Please check that your sensor is charged and turned on.\n\n" +
				"After turning your board back on, please wait a moment while it reconnects.")
				.setTitle("No Sensor Board Detected!")
				.setCancelable(false)
				.setNeutralButton("Okay", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			                dialog.cancel();
			           }
			       });
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	/////////////////////////////////////////////
	// Activity options menu
	/////////////////////////////////////////////
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.option_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent serverIntent;
		switch (item.getItemId()) {
		case R.id.scan:
			// Launch the DeviceListActivity to see devices and do scan
			serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
			return true;
		case R.id.celalAct:
			serverIntent = new Intent(this, Main.class);
			startActivity(serverIntent);
			return true;
		case R.id.lockScreen:
			toggleScreenLock();
			return true;
		case R.id.exitApp:
			this.finish();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REQUEST_CONNECT_DEVICE:
				// When DeviceListActivity returns with a device to connect
				if (resultCode == Activity.RESULT_OK) {
					// Get the device MAC address
					String address = data.getExtras().getString(
							DeviceListActivity.EXTRA_DEVICE_ADDRESS);
					exposedServices.connectSensor(address);
				}
				break;
			case REQUEST_FACEBOOK_SHARE:
				if(resultCode == Activity.RESULT_OK) {
					long secToShare = data.getLongExtra(
							ShareDialogue.EXTRA_HOURS_TO_SHARE, 0);
					sendSMS(true, secToShare);
				}
				break;
		}
	}
	
	private synchronized void toggleScreenLock() {
		isScreenLocked = !isScreenLocked;
		findViewById(R.id.CloudView).setClickable(!isScreenLocked);
		findViewById(R.id.aqiBarView_help_button).setClickable(!isScreenLocked);
		findViewById(R.id.bluetoothButton).setClickable(!isScreenLocked);
		findViewById(R.id.facebookButton).setClickable(!isScreenLocked);
		findViewById(R.id.shareAQIButton).setClickable(!isScreenLocked);
		findViewById(R.id.twitterButton).setClickable(!isScreenLocked);
	}
	
	/////////////////////////////////////////////
	// Start up the background service
	/////////////////////////////////////////////
	private void bindToService(){
		AndroidBackgroundServiceStarter.bind(this, serviceBindCallback);
	}	

	private final ServiceConnection serviceBindCallback = new ServiceConnection() {
		@SuppressWarnings("unchecked")
		public void onServiceConnected(ComponentName className, IBinder service) {
			exposedServices = ((LocalBinder<CitiSenseExposedServices>) service)
					.getService();
			
			// Check if bluetooth already connected
			// Further updates are done via callbacks
			checkBluetoothConnection();			
//			timeUpdateHandler.post(updateTimeTask);
			if(exposedServices != null && exposedServices.isSensorConnected() == BluetoothChatService.STATE_CONNECTED) {
				SensorReading lastAqiReading = exposedServices.getLastReading(SensorType.AQI);
				if(lastAqiReading != null) {
					currentAqi = (int)lastAqiReading.getSensorData();
					currentReadingTime = lastAqiReading.getTimeDate();
					currentUnits = (lastAqiReading.getLocation() != null && lastAqiReading.getLocation().getProvider() != null) 
						? SensorType.valueOf(lastAqiReading.getLocation().getProvider()) 
						: null;
					new UpdateAqiDisplay().execute();
				}
			}
		}
		
		public void onServiceDisconnected(ComponentName className) {
			// As our service is in the same process, this should never be
			// called
		}
	};
	
	// Only checked once, on binding to the background service
	// Further updates are handled through callbacks
	private void checkBluetoothConnection() {
		if(exposedServices == null)
			return;
		//check if sensor is connected
		if(//exposedServices.isSensorConnected() == BluetoothChatService.STATE_CONNECTING || 
				exposedServices.isSensorConnected() == BluetoothChatService.STATE_CONNECTED){
			sensorConnect.setVisibility(View.INVISIBLE);
		} else {
			sensorConnect.setVisibility(View.VISIBLE);
		}
	}
}