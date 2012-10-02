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
	
	private static final double CO_SPIKE_THRESHOLD = 15.0;
	private static final double NO2_SPIKE_THRESHOLD = 1.0;
	private static final double O3_SPIKE_THRESHOLD = 0.3;

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
	private SensorReading[] lastReadings = new SensorReading[SensorType.values().length];
	private SensorReading[] lastLastReadings = new SensorReading[SensorType.values().length];
	
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

	// public BluetoothChat(Context context, Handler parentHandler) {
	public BluetoothServiceImpl() {
		this.parentHandler = new SensorServiceBluetooth();

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// If still null, not supported
		if (mBluetoothAdapter == null) {
			// Toast.makeText(context, "Bluetooth is not available",
			// Toast.LENGTH_LONG).show();
			return;
		}

		// Hack: If the application crashed earlier, it may have left the bluetooth connection in a bad state
		// Bad solution: Always disable and then re-enable the BT connection at start up
		if (mBluetoothAdapter.isEnabled()) {
			mBluetoothAdapter.disable();
			while (mBluetoothAdapter.getState() != BluetoothAdapter.STATE_OFF)
				try {Thread.sleep(1000);} catch(InterruptedException e){};
//			try {
//				Thread.sleep(2500);
//			} catch (InterruptedException e) {
//				
//			}
		}
		
		if (!mBluetoothAdapter.isEnabled()) {
			mBluetoothAdapter.enable();
			while (mBluetoothAdapter.getState() != BluetoothAdapter.STATE_ON)
				try {Thread.sleep(1000);} catch(InterruptedException e){};;
			// Intent enableIntent = new Intent(
			// BluetoothAdapter.ACTION_REQUEST_ENABLE);
			// context.startActivity(enableIntent);
		}
		
		Log.d("BluetoothServiceImpl", "Device state: " + mBluetoothAdapter.getState());

		// Initialize the BluetoothChatService to perform bluetooth connections
		mChatService = new BluetoothChatService(mHandler);
		// Initialize the buffer for outgoing messages
		mOutStringBuffer = new StringBuffer("");
		
		mChatService.start();
		
		// When first starting, try to connect to your last sensor board
		String address = settings.preferences().getString("lastSensorAddress", null);
		if(address != null) {
			connectSensor(address, 0);
		}
		
		context = settings.context();
	}

	public void connectSensor(String address) {
		connectSensor(address, 0);
	}
	
	public void connectSensor(String address, int delay) {
		if(address == null) {
			if(AppLogger.isErrorEnabled(logger))
				logger.error("Address to connect to was NULL!");
			return;
		}
		
		// The sensor is already connected, disconnect first
		if(sensorState != BluetoothChatService.STATE_NONE) {
			disconnectSensor();
			// Sleep a second to let the BT hardware catch up
//			try {
//				Thread.sleep(1000);
//			} catch (Exception e) {
//				
//			}
			
		}
			
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
		} else {
			if(AppLogger.isErrorEnabled(logger))
				logger.error("Invalid Bluetooth device address passed to connect()");
		}
	}

	public void disconnectSensor() {
		if (mChatService != null) {
			disconnectedOnPurpose = true;
			mChatService.stop();
		}
	}

	public int isSensorConnected() {
		return sensorState;
	}

	public void setObservationRepository(
			ObservationRepository observationRepository) {
		this.observationRepository = observationRepository;
	}

	public void setLocalRepository(LocalRepository localRepository) {
		this.localRepository = localRepository;
	}

	public void setLocationService(LocationService locationService) {
		this.locationService = locationService;
	}

	// public void ensureDiscoverable() {
	// if (D)
	// if (AppLogger.isDebugEnabled(logger))
	// logger.debug("ensure discoverable");
	// if (mBluetoothAdapter.getScanMode() !=
	// BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
	// Intent discoverableIntent = new Intent(
	// BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
	// discoverableIntent.putExtra(
	// BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
	// context.startActivity(discoverableIntent);
	// }
	// }

	/**
	 * Sends a message.
	 * 
	 * @param message
	 *            A string of text to send.
	 */
	public void sendMessage(String message) {
		// Check that we're actually connected before trying anything
		if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
			// Toast.makeText(context, R.string.not_connected,
			// Toast.LENGTH_SHORT)
			// .show();
			return;
		}

		// Check that there's actually something to send
		if (message.length() > 0) {
			// Get the message bytes and tell the BluetoothChatService to write
			byte[] send = message.getBytes();
			mChatService.write(send);

			// Reset out string buffer to zero and clear the edit text field
			mOutStringBuffer.setLength(0);
		}
	}

	// private static String getHexString(byte[] b) {
	// String result = "";
	// for (int i = 0; i < b.length; i++) {
	// result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
	// }
	// return result;
	// }

	// Returns the name of the device currently connected to.
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
					
					// Third byte is sensor identifier
					byte sensorByte = buf[2];
					// Fourth and fifth bytes are sensor value
					byte[] valBytes = { buf[3], buf[4] };
	
					int val = (int) valBytes[0] & 0xff;
					int val2 = (int) valBytes[1] & 0xff;
					val = val << 8;
					val = val | val2;
					
					float value = (float)val;
	
					int sensor = (int) sensorByte & 0xff;
					int j = 0;
					while (sensor != 0) {
						sensor = sensor >> 1;
						j++;
					}
					
					// Check if the sensor type is NONE (j == 8)
					// If so, discard the message
					if(j == 8) {
						break;
					}
					
					// Figure out units for measurement
					SensorType sensorType = SensorType.getSensorTypeFor(j);
					String sensorUnits = sensorType.getUnits();
	
					if (sensorType == SensorType.NO2) {
						value /= 1000;
					} else if(sensorType == SensorType.O3) {
						value /= 1000;
					} else if (sensorType == SensorType.CO) {
						value /= 10;
					} else if (sensorType == SensorType.TEMP) {
						value /= 10;
					} else if (sensorType == SensorType.PRES) {
						value /= 10;
					} else if (sensorType == SensorType.HUMD) {
	
					} else {
						sensorUnits = "UNKNOWN";
					}
	
					// Get a current time stamp and location for the data
					SensorReading sensorReading = new SensorReadingImpl(sensorType,
							value, sensorUnits, System.currentTimeMillis(),
							locationService.getLastKnownLocation());
	
					parentHandler.obtainMessage(DATA_FROM_SENSOR, -1, -1,
							sensorReading).sendToTarget();
	
					break;
				case MESSAGE_DEVICE_NAME:
					// save the connected device's name
					mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
//					context = settings.context();
//					if(context != null)
//						Toast.makeText(uiContext, "Connected to " +
//							mConnectedDeviceName,
//							Toast.LENGTH_SHORT).show();
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
				//int pinNumber = sensorReading.getSensorType().getPinNumber();
				// The last reading of the same type
				SensorReading lastReading = lastReadings[sensorReading.getSensorType().ordinal()];
				SensorReading lastLastReading= lastLastReadings[sensorReading.getSensorType().ordinal()];
				
				//Log.d("SensorService", "Recieved data from " + sensorReading.getSensorType() + " sensor: " + sensorReading.getSensorData());
				
				// For each gas reading...
				if(lastReading != null &&
						( lastReading.getSensorType() == SensorType.CO || 
						lastReading.getSensorType() == SensorType.NO2 ||
						lastReading.getSensorType() == SensorType.O3) ) {
					double lastData = lastReading.getSensorData();
					double newData = sensorReading.getSensorData();
					
					// Filter out spikes from data
					double diff = Math.abs(lastData - newData);
					// If CO is a drop of 15.0 or more...
					if( lastReading.getSensorType() == SensorType.CO &&  diff > CO_SPIKE_THRESHOLD) {
						// No older data to compare to, so go ahead and filter it out
						if(lastLastReading == null) {
							lastReading = null;
						} else {
							double lastLastData = lastLastReading.getSensorData();
							double lastDiff = Math.abs(lastLastData - lastData);
							if(lastDiff > CO_SPIKE_THRESHOLD) {
								lastReading = null;
							}
						}
					}
					// If NO2 is a drop of 1.0 or more...
					else if( lastReading.getSensorType() == SensorType.NO2 &&  diff > NO2_SPIKE_THRESHOLD) {
						// No older data to compare to, so go ahead and filter it out
						if(lastLastReading == null) {
							lastReading = null;
						} else {
							double lastLastData = lastLastReading.getSensorData();
							double lastDiff = Math.abs(lastLastData - lastData);
							if(lastDiff > NO2_SPIKE_THRESHOLD) {
								lastReading = null;
							}
						}
					}
					// If O3 is a drop of 1.0 or more...
					else if( lastReading.getSensorType() == SensorType.O3 && diff > O3_SPIKE_THRESHOLD) {
						// No older data to compare to, so go ahead and filter it out
						if(lastLastReading == null) {
							lastReading = null;
						} else {
							double lastLastData = lastLastReading.getSensorData();
							double lastDiff = Math.abs(lastLastData - lastData);
							if(lastDiff > O3_SPIKE_THRESHOLD) {
								lastReading = null;
							}
						}
					}
				}
				// Keep track as last reading
				lastLastReadings[sensorReading.getSensorType().ordinal()] = lastReading;
				// Make copy and store
				SensorReading copyReading = new SensorReadingImpl( sensorReading.getSensorType(), 
						sensorReading.getSensorData(),
						sensorReading.getSensorUnits(),
						sensorReading.getTimeMilliseconds(),
						sensorReading.getLocation() );
				lastReadings[sensorReading.getSensorType().ordinal()] = copyReading;

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
