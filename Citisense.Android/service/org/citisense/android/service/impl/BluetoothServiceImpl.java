package org.citisense.android.service.impl;

import static org.citisense.datastructure.BluetoothConstants.CONNECTION_ESTABLISHED;
import static org.citisense.datastructure.BluetoothConstants.CONNECTION_FAILED;
import static org.citisense.datastructure.BluetoothConstants.CONNECTION_LOST;
import static org.citisense.datastructure.BluetoothConstants.DATA_FROM_SENSOR;
import static org.citisense.datastructure.BluetoothConstants.MESSAGE_DEVICE_NAME;
import static org.citisense.datastructure.BluetoothConstants.MESSAGE_READ;
import static org.citisense.datastructure.BluetoothConstants.MESSAGE_STATE_CHANGE;
import static org.citisense.datastructure.BluetoothConstants.MESSAGE_TOAST;
import static org.citisense.datastructure.BluetoothConstants.MESSAGE_WRITE;

import org.citisense.android.UICallbackTypes;
import org.citisense.android.bluetooth.BluetoothChatService;
import org.citisense.android.bluetooth.MockBluetoothChatService;
import org.citisense.android.service.BluetoothService;
import org.citisense.android.service.LocalRepository;
import org.citisense.datastructure.SensorReading;
import org.citisense.datastructure.SensorType;
import org.citisense.datastructure.impl.SensorReadingImpl;
import org.citisense.service.LocationService;
import org.citisense.service.ObservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class BluetoothServiceImpl implements BluetoothService {

	private final Logger logger = LoggerFactory
			.getLogger(BluetoothServiceImpl.class);

	// Debugging
	private static final boolean D = true;

	// Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	// Name of the connected device
	private String mConnectedDeviceName = null;
	// Current state of connection
	private int sensorState = BluetoothChatService.STATE_NONE;
	// String buffer for outgoing messages
	private StringBuffer mOutStringBuffer;
	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter;
	// Member object for the chat services
	private BluetoothChatService mChatService = null;
	// Used for reconnecting timing
	private int quickReconnectCount = 0;
	// Local copies of the last set of readings received
	protected static SensorReadingFilter sFilter = new SensorReadingFilter();
	
	// So we know not to reconnect when a lost connection error occurs
	private boolean disconnectedOnPurpose = false;
	
	private ObservationRepository observationRepository;
	private LocalRepository localRepository;
	private LocationService locationService;
	
	private Context context;
	
	private final ApplicationSettings settings = ApplicationSettings
		.instance();

	// private Context context;
	private Handler parentHandler;

	/**
	 * Constructor enables the Bluetooth adapter, and starts the ChatService to BT.
	 *
	 * @post
	 *   	Bluetooth connection is enabled
	 */
	public BluetoothServiceImpl() {
		this.parentHandler = new SensorServiceBluetooth();

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		String address = null;
		// If still null, not supported
		if (mBluetoothAdapter != null) {

			// Hack: If the application crashed earlier, it may have left the bluetooth connection in a bad state
			// Bad solution: Always disable and then re-enable the BT connection at start up
			if (mBluetoothAdapter.isEnabled()) {
				mBluetoothAdapter.disable();
				while (mBluetoothAdapter.getState() != BluetoothAdapter.STATE_OFF)
					try {Thread.sleep(1000);} catch(InterruptedException e){};
	
			}
			
			if (!mBluetoothAdapter.isEnabled()) {
				mBluetoothAdapter.enable();
				while (mBluetoothAdapter.getState() != BluetoothAdapter.STATE_ON)
					try {Thread.sleep(1000);} catch(InterruptedException e){};;
	
			}
				
			Log.d("BluetoothServiceImpl", "Device state: " + mBluetoothAdapter.getState());
			
			// Initialize the BluetoothChatService to perform bluetooth connections
			mChatService = new BluetoothChatService(mHandler);
			address = settings.preferences().getString("lastSensorAddress", null);
		}
		else {
				// Initialize the BluetoothChatService to perform bluetooth connections
				mChatService = new MockBluetoothChatService(mHandler);
				address = "MOCK BLUETOOTH ADDRESS";
		}
		// Initialize the buffer for outgoing messages
		mOutStringBuffer = new StringBuffer("");
		mChatService.start();
		
		// When first starting, try to connect to your last sensor board
		if(address != null) {
			connectSensor(address, 0);
		}
		
		context = settings.context();
		
		//Assert post-conditions of function.
		assert(mBluetoothAdapter.isEnabled() == true);
	}

	/**
	 * Request to connect immediately to sensor at address provided.
	 *
	 * @param address the address of sensor
	 * @pre   address is non-null
	 * 		
	 */
	public void connectSensor(String address) {
		assert(address != null);
		
		connectSensor(address, 0);
	}

	/**
	 * Request to sensor at address provided, after a delay in milliseconds.
	 * Connect sensor thread is started.
	 *
	 * @param address  the address of sensor
	 * @param delay    the delay in milliseconds
	 * @pre   address is non-null
	 * @pre   delay is non-negative
	 * @pre   Current sensor state is BluetoothChatService.STATE_NONE
	 */
	public void connectSensor(String address, int delay) {
		assert(sensorState == BluetoothChatService.STATE_NONE);
		assert(address != null);
		assert(delay >= 0);
		
		// Save the passed in address for future use
		SharedPreferences.Editor editor = settings.preferences().edit();
		editor.putString("lastSensorAddress", address);
		try{
			editor.commit();
		}
		catch(Exception e) {
			if(AppLogger.isErrorEnabled(logger))
				logger.error("Failed to commit saving address to SharedPreferences: " + e);
		}
		
		if(BluetoothAdapter.checkBluetoothAddress(address)) {
			BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
			// Attempt to connect to the device
			mChatService.connect(device, delay);
		} 
		else {
			BluetoothDevice device = null;
			mChatService.connect(device, delay);
		}
	}

	/**
	 * Request to disconnect sensor. ChatService is stopped.
	 *
	 */	
	public void disconnectSensor() {
		if (mChatService != null) {
			disconnectedOnPurpose = true;
			mChatService.stop();
		}
	}

	/**
	 * Returns sensor state 
	 *
	 * @result sensorState is one of BluetoothChatService.STATE_NONE,
	 * 							    BluetoothChatService.STATE_LISTEN, 
	 * 							    BluetoothChatService.STATE_CONNECTING,
	 * 								BluetoothChatService.STATE_CONNECTED
	 */	
	public int getSensorState() {
		return sensorState;
	}

	/**
	 * Sets Observation Repository for service
	 * 
	 * @param  observationRepository the repository where measurements will be saved
	 * @pre    observationRepository is non-null
	 *
	 */		
	public void setObservationRepository(
			ObservationRepository observationRepository) {
		assert(observationRepository != null);
		this.observationRepository = observationRepository;
	}

	/**
	 * Sets Local Repository for service
	 * 
	 * @param  localRepository the repository where measurements will be saved
	 * @pre    localRepository is non-null
	 *
	 */		
	public void setLocalRepository(LocalRepository localRepository) {
		assert(localRepository != null);
		this.localRepository = localRepository;
	}

	/**
	 * Sets Location Service implementation
	 * 
	 * @param  locationService the class implementing location services
	 * @pre    locationService is non-null
	 *
	 */	
	public void setLocationService(LocationService locationService) {
		assert(locationService != null);
		this.locationService = locationService;
	}

	/**
	 * Sends a message.
	 * 
	 * @param message A string of text to send.
	 * 
	 * @pre   Current state should be STATE_CONNECTED
	 * @pre   message is non-null
	 * @post  if connected, message will be sent
	 */
	public void sendMessage(String message) {
		
		assert(mChatService.getState() == BluetoothChatService.STATE_CONNECTED);
		assert(message != null);

		// Get the message bytes and tell the BluetoothChatService to write
		byte[] send = message.getBytes();
		mChatService.write(send);

		// Reset out string buffer to zero and clear the edit text field
		mOutStringBuffer.setLength(0);
	}

	/**
	 * Returns the name of the device currently connected to.
	 *
	 * @result String is name of current device
	 */
	public String getDeviceName() {
		return mConnectedDeviceName;
	}

	// The Handler that gets information back from the BluetoothChatService
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			// UI handler to callback to
			Handler uiHandler = settings.uiHandler();
			
			switch (msg.what) {
				case CONNECTION_LOST:
					if (AppLogger.isDebugEnabled(logger))
						logger.debug("Connection to sensor LOST!");
					// Tell UI that bluetooth losts
					if(uiHandler != null) {
						uiHandler.obtainMessage(UICallbackTypes.AQI_UI_BLUETOOTH_OFF, -1, -1).sendToTarget();
					}
					
					context = settings.context();
					
					// If it was disconnected on purpose, don't try to reconnect
					if(disconnectedOnPurpose) {
						disconnectedOnPurpose = false;
						if(context != null) {
							Toast.makeText(context, "Disconnected from sensor", Toast.LENGTH_SHORT).show();
						}
						break;
					}
					if(context != null) {
						Toast.makeText(context, "Lost connection to sensor. Attempting to reconnect...", Toast.LENGTH_LONG).show();
					}
					quickReconnectCount = Integer.MAX_VALUE;
					connectSensor(settings.preferences().getString("lastSensorAddress", null), 
							settings.sensorConnectDelay());
					// Ensure proper tear down has occured
					//disconnectSensor();
					break;
				case CONNECTION_FAILED:
					if (AppLogger.isDebugEnabled(logger))
						logger.debug("Connection to sensor FAILED!");
					// Tell UI that bluetooth losts
					if(uiHandler != null) {
						uiHandler.obtainMessage(UICallbackTypes.AQI_UI_BLUETOOTH_OFF, -1, -1).sendToTarget();
					}
					context = settings.context();
					if(context != null)
						Toast.makeText(context, "Connection to sensor failed.\nAttempting to reconnect...", Toast.LENGTH_LONG).show();
					if(quickReconnectCount > 0) {
						connectSensor(settings.preferences().getString("lastSensorAddress", null), 
								settings.sensorConnectDelay());
						--quickReconnectCount;
					} else {
						connectSensor(settings.preferences().getString("lastSensorAddress", null), 
								3 * settings.sensorConnectDelay());
					}
					break;
				case CONNECTION_ESTABLISHED:
					if (AppLogger.isDebugEnabled(logger))
						logger.debug("Connection to sensor ESTABLISHED");
					// Tell UI that bluetooth connected
					if(uiHandler != null) {
						uiHandler.obtainMessage(UICallbackTypes.AQI_UI_BLUETOOTH_ON, -1, -1).sendToTarget();
					}
					context = settings.context();
					if(context != null)
						Toast.makeText(context, "Connection to sensor established.", Toast.LENGTH_LONG).show();
					quickReconnectCount = 0;
					break;
				case MESSAGE_STATE_CHANGE:
					if (D)
						if(AppLogger.isDebugEnabled(logger)) logger.debug("MESSAGE_STATE_CHANGE: " + msg.arg1);
					switch (msg.arg1) {
					case BluetoothChatService.STATE_CONNECTED:
						sensorState = BluetoothChatService.STATE_CONNECTED;
						break;
					case BluetoothChatService.STATE_CONNECTING:
						sensorState = BluetoothChatService.STATE_CONNECTING;
						break;
					case BluetoothChatService.STATE_LISTEN:
					case BluetoothChatService.STATE_NONE:
						sensorState = BluetoothChatService.STATE_NONE;
						break;
					}
					break;
				case MESSAGE_WRITE:
					break;
				case MESSAGE_READ:
					byte[] readBuf = (byte[]) msg.obj;
					byte[] buf = new byte[msg.arg1];
	
					// String strConv = readBuf.toString();
	
					for (int i = 0; i < msg.arg1; i++)
						buf[i] = readBuf[i];
					
					SensorMessage smsg = new SensorMessage(buf);
	
					// Get a current time stamp and location for the data
					SensorReading sensorReading = new SensorReadingImpl(smsg.sensorType,
							smsg.value, smsg.sensorUnits, System.currentTimeMillis(),
							locationService.getLastKnownLocation());
	
					parentHandler.obtainMessage(DATA_FROM_SENSOR, -1, -1,
							sensorReading).sendToTarget();
	
					break;
				case MESSAGE_DEVICE_NAME:
					// save the connected device's name
					mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
					break;
				case MESSAGE_TOAST:
					context = settings.context();
					if(context != null)
						Toast.makeText(context, msg.getData().getString(TOAST),
								Toast.LENGTH_SHORT).show();
				break;
			}
		}
	};

	private class SensorServiceBluetooth extends Handler {

		// private final SimpleDateFormat formatter = new SimpleDateFormat(
		// "M/d/yyyy h:mm:ss a");

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case DATA_FROM_SENSOR:
				// What to do with the incoming data from BT
				SensorReading sensorReading = (SensorReading) msg.obj;
				
				// Filter spike from reading
				SensorReading lastReading = sFilter.filterReading(sensorReading);
				
				if(lastReading != null) {
					// Push to local storage
					localRepository.storeSensorReading(lastReading);

					//trigger to calculate AQI (we do this once per set of readings 
					//which correlates roughly with when we get a new "CO" reading
					if(lastReading.getSensorType() == SensorType.CO) {
						AqiTracker at = new  AqiTracker();
						at.updateAqi(localRepository, settings);
					}
				}
				
			}
		}
	}

}
