package org.citisense.android;

import static org.citisense.datastructure.BluetoothConstants.REQUEST_CONNECT_DEVICE;

import java.util.Collections;

import org.citisense.android.androidservice.AndroidBackgroundServiceStarter;
import org.citisense.android.androidservice.LocalBinder;
import org.citisense.android.bluetooth.BluetoothChatService;
import org.citisense.android.service.impl.AppLogger;
import org.citisense.android.service.impl.ApplicationSettings;
import org.citisense.android.service.impl.CitiSenseExposedServices;
import org.citisense.android.service.impl.ConnectivityNotificationService;
import org.citisense.datastructure.SensorReading;
import org.citisense.datastructure.SensorType;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 
 * @author celal.ziftci
 * 
 */
public class Main extends Activity {
	//private final Logger logger = LoggerFactory.getLogger(Main.class);

	private Button toggleLoggingButton;
	private Button showCacheButton;
	private Button sensorReadingProviderButton;
	private Button backendServerHeartbeatButton;
	private TextView infoBox;
	private TextView statusBox;
	
	private CitiSenseExposedServices exposedServices;

	private final ServiceConnection serviceBindCallback = new ServiceConnection() {
		@SuppressWarnings("unchecked")
		public void onServiceConnected(ComponentName className, IBinder service) {
			exposedServices = ((LocalBinder<CitiSenseExposedServices>) service)
					.getService();
		}

		public void onServiceDisconnected(ComponentName className) {
			// helloService = null;
			// As our service is in the same process, this should never be
			// called
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// bind to our service
		AndroidBackgroundServiceStarter.start(this);
		AndroidBackgroundServiceStarter.bind(this, serviceBindCallback);

		// This should probably be done in the background service, the first
		// time it is created...
		// ApplicationSettings.instance().setupApplicationSettings(this);
		setupGUI();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// Unbind on destroy so that we don't leak service connections
		if (serviceBindCallback != null) {
			this.unbindService(serviceBindCallback);
		}
	}

	/**
	 * TODO: Perform cleanup like killing threads etc!!
	 */
	@Override
	public void finish() {
		super.finish();
	}

	public String getCachedData() {
		StringBuffer buffer = new StringBuffer(50);
		int totalCounter = 0;

		for (SensorType sensorType : SensorType.values()) {
			// sensorType
			String sensorID = ApplicationSettings.instance()
					.sensorIDForSensorType(sensorType.getPinNumber());
			SensorReading[] observations = exposedServices.getObservation(
					ApplicationSettings.instance().studyID(),
					ApplicationSettings.instance().subjectID(), sensorID,
					Collections.<String, String> emptyMap());
			if (observations != null && observations.length > 0) {
				// buffer.append(applicationSettings.headerForSensorID(sensorID));
				buffer.append(sensorType).append(": ").append(
						observations.length).append("\n");
				totalCounter += observations.length;
				// for (int i = 0; i < observations.length; i++) {
				// buffer.append(observations[i]).append("\n");
				// }
			}
			// buffer.append("\n");
		}

		// for (String sensorID : ids) {
		// SensorReading[] observations = exposedServices.getObservation(
		// ApplicationSettings.instance().studyID(),
		// ApplicationSettings.instance().subjectID(), sensorID,
		// Collections.<String, String> emptyMap());
		// if (observations != null && observations.length > 0) {
		// // buffer.append(applicationSettings.headerForSensorID(sensorID));
		// buffer.append(" (").append(observations.length).append(
		// " total)\n");
		// totalCounter += observations.length;
		// for (int i = 0; i < observations.length; i++) {
		// buffer.append(observations[i]).append("\n");
		// }
		// }
		// buffer.append("\n");
		// }

		buffer.insert(0, "# READINGS IN THE PHONE NOW: " + totalCounter
				+ "\n\n");
		return buffer.toString();
	}
	
	
	private class CheckBackendServerHeartbeat
			extends AsyncTask<Void, Void, Boolean>{
		@Override
		protected Boolean doInBackground(Void... arg0) {
			return ConnectivityNotificationService.canConnect();
		}
		@Override
		protected void onPostExecute(Boolean result) {
			if(result)
				backendServerHeartbeatButton
					.setText("Backend server: ALIVE");
			else
				backendServerHeartbeatButton
				.setText("Backend server: DEAD");
		}
	}

	private void setupGUI() {
		toggleLoggingButton = new Button(this);
		toggleLoggingButton.setText("Logging level: " + AppLogger.getLogLevelString());
		showCacheButton = new Button(this);
		showCacheButton
				.setText("Cached sensor-readings:\nsensor-board -> phone");
		sensorReadingProviderButton = new Button(this);
		sensorReadingProviderButton
				.setText("Pollution data provided by: (click to refresh!)");
		backendServerHeartbeatButton = new Button(this);
		backendServerHeartbeatButton
				.setText("Backend server: (click to refresh!)");
		// backendServerHeartbeatButton.setClickable(false);
		// if (isBackendServerAlive()) {
		// backendServerHeartbeatButton
		// .setText("Backend server: ALIVE");
		// } else {
		// backendServerHeartbeatButton
		// .setText("Backend server: DEAD");
		// }

		infoBox = new TextView(this);
		statusBox = new TextView(this);

		LinearLayout sensingLayout = new LinearLayout(this);
		sensingLayout.setOrientation(LinearLayout.VERTICAL);
		sensingLayout.addView(showCacheButton);
		sensingLayout.addView(toggleLoggingButton);
		sensingLayout.addView(sensorReadingProviderButton);
		sensingLayout.addView(backendServerHeartbeatButton);

		LinearLayout cacheLayout = new LinearLayout(this);
		cacheLayout.setOrientation(LinearLayout.HORIZONTAL);

		LinearLayout verticalLayout = new LinearLayout(this);
		verticalLayout.setOrientation(LinearLayout.VERTICAL);
		verticalLayout.addView(infoBox);
		verticalLayout.addView(sensingLayout);
		verticalLayout.addView(cacheLayout);
		verticalLayout.addView(statusBox);

		sensorReadingProviderButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (exposedServices.isSensorConnected() == BluetoothChatService.STATE_CONNECTED) {
					sensorReadingProviderButton
							.setText("Pollution data provided by: sensor-board");
				} else if(exposedServices.isSensorConnected() == BluetoothChatService.STATE_CONNECTING){
					sensorReadingProviderButton
						.setText("Connecting to sensor-board...");
				} else {
					sensorReadingProviderButton
							.setText("Pollution data provided by: San Diego Web Report");
				}
			}
		});

		backendServerHeartbeatButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				backendServerHeartbeatButton
				.setText("Backend server: Checking...");
				
				// Check connectivity in other thread
				new CheckBackendServerHeartbeat().execute();
				
//				if(ConnectivityNotificationService.canConnect()) {
//					backendServerHeartbeatButton
//							.setText("Backend server: ALIVE");
//				} else {
//					backendServerHeartbeatButton
//							.setText("Backend server: DEAD");
//				}
			}
		});

		toggleLoggingButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				AppLogger.toggleLevel();
				toggleLoggingButton.setText("Logging level: " + AppLogger.getLogLevelString());
			}
		});

		showCacheButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				statusBox.setText(getCachedData());
			}
		});
		setContentView(verticalLayout);
	}

	// Behavior is provided by static function ConnectivityNotificationService.canConnect()
//	private boolean isBackendServerAlive() {
//		HttpParams httpParameters = new BasicHttpParams();
//		HttpConnectionParams.setConnectionTimeout(httpParameters,
//				ApplicationSettings.instance().timeoutConnect());
//		HttpConnectionParams.setSoTimeout(httpParameters, ApplicationSettings
//				.instance().timeoutOperation());
//		DefaultHttpClient httpclient = new DefaultHttpClient(httpParameters);
//		HttpGet heartbeat = new HttpGet(ApplicationSettings.instance()
//				.serverHeartBeatUri());
//		try {
//			HttpResponse response = httpclient.execute(heartbeat);
//			if (response.getStatusLine().getStatusCode() == 200) {
//				return true;
//			}
//		} catch (Exception e2) {
//		}
//		// If we are here, some exception occurred, or HTTP 200 was not sent.
//		return false;
//	}

	// /////////////////
	// NIMA: Code for connection menu
	// /////////////////
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.option_menu, menu);
		return true;
	}

	// @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.scan:
			// Launch the DeviceListActivity to see devices and do scan
			Intent serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
			return true;
			// case R.id.discoverable:
			// // NIMA: Ensure this device is discoverable by others
			// //btChat.ensureDiscoverable();
			// return true;
		case R.id.celalAct:
			serverIntent = new Intent(this, Aqi_ui.class);
			startActivity(serverIntent);
			return true;
		}
		return false;
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
		// case REQUEST_ENABLE_BT:
		// // When the request to enable Bluetooth returns
		// if (resultCode == Activity.RESULT_OK) {
		// // Bluetooth is now enabled, so set up a chat session
		// // setupChat();
		// } else {
		// // User did not enable Bluetooth or an error occured
		// Toast.makeText(this, R.string.bt_not_enabled_leaving,
		// Toast.LENGTH_SHORT).show();
		// finish();
		// }
		}
	}

	// /////////////////
	// END code for connection menu
	// /////////////////
}
