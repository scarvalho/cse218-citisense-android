package org.citisense.datastructure;

public class BluetoothConstants {
	// Message types sent from the BluetoothChatService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	// Message types to send to parent activity
	public static final int DATA_FROM_SENSOR = 10;
	public static final int CONNECTION_ESTABLISHED = 11;
	public static final int CONNECTION_LOST = 12;
	public static final int CONNECTION_FAILED = 13;

	// Intent request codes
	public static final int REQUEST_CONNECT_DEVICE = 1;
	public static final int REQUEST_ENABLE_BT = 2;
	
	// MOCK Service
	public static final boolean USE_MOCK_BLUETOOTH = true;
}
