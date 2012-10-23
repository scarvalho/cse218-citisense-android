package org.citisense.android.bluetooth;

import static org.citisense.datastructure.BluetoothConstants.CONNECTION_ESTABLISHED;
import static org.citisense.datastructure.BluetoothConstants.CONNECTION_FAILED;
import static org.citisense.datastructure.BluetoothConstants.CONNECTION_LOST;
import static org.citisense.datastructure.BluetoothConstants.MESSAGE_DEVICE_NAME;
import static org.citisense.datastructure.BluetoothConstants.MESSAGE_READ;
import static org.citisense.datastructure.BluetoothConstants.MESSAGE_STATE_CHANGE;
import static org.citisense.datastructure.BluetoothConstants.MESSAGE_WRITE;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import org.citisense.android.service.impl.AppLogger;
import org.citisense.android.service.impl.ApplicationSettings;
import org.citisense.android.service.impl.BluetoothServiceImpl;
import org.citisense.android.service.impl.SensorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;

public class MockBluetoothChatService extends BluetoothChatService {
	
	protected MockConnectThread mMockConnectThread;
	protected MockConnectedThread mMockConnectedThread;
	
	public MockBluetoothChatService(Handler handler){
		mState = STATE_CONNECTED;
		mHandler = handler;
	}
//	public synchronized void setState(int state){
//		
//	}
//	public synchronized int getState(){
//		return mState;
//	}
	public synchronized void start() {
		if (D)
			if (AppLogger.isDebugEnabled(logger))
				logger.debug("start");

		// Cancel any thread attempting to make a connection
		if (mMockConnectThread != null) {
			mMockConnectThread.cancel();
			mMockConnectThread = null;
		}

		// Cancel any thread currently running a connection
		if (mMockConnectedThread != null) {
			mMockConnectedThread.cancel();
			mMockConnectedThread = null;
		}

		// Start the thread to listen on a BluetoothServerSocket
//		if (mAcceptThread == null) {
//			mAcceptThread = new AcceptThread();
//			// No need. We don't listen for incoming connections.
//			//mAcceptThread.start();
//		}
		setState(STATE_LISTEN);
	}
	public synchronized void connect(BluetoothDevice device, int delay){
		if (D)
			if (AppLogger.isDebugEnabled(logger)) logger.debug("connect to: " + device);

		// Cancel any thread attempting to make a connection
//		if (mState == STATE_CONNECTING) {
			if (mMockConnectThread != null) {
				mMockConnectThread.cancel();
				mMockConnectThread = null;
			}
//		}

		// Cancel any thread currently running a connection
		if (mMockConnectedThread != null) {
			mMockConnectedThread.cancel();
			mMockConnectedThread = null;
		}
		
		if(mConnectTimer != null) {
			mConnectTimer.cancel();
		}

		// Start the thread to connect with the given device
		mDevice = device;
		mConnectTimer = new Timer();
		if(delay > MIN_CONNECT_DELAY)
			mConnectTimer.schedule(new DelayedConnect(), delay);
		else
			mConnectTimer.schedule(new DelayedConnect(), MIN_CONNECT_DELAY);
		setState(STATE_CONNECTING);
	}
	
	public synchronized void connected(){
		if (D)
			if (AppLogger.isDebugEnabled(logger))
				logger.debug("connected");

		// Cancel the thread that completed the connection
		if (mMockConnectThread != null) {
			mMockConnectThread.cancel();
			mMockConnectThread = null;
		}

		// Cancel any thread currently running a connection
		if (mMockConnectedThread != null) {
			mMockConnectedThread.cancel();
			mMockConnectedThread = null;
		}
		mMockConnectedThread = new MockConnectedThread();
		mMockConnectedThread.start();
		
		// TESTING: Try holding onto a WakeLock while connected
		if(!wakeLock.isHeld()) {
			wakeLock.acquire();
		}

		// Send the name of the connected device back to the UI Activity
		Message msg = mHandler.obtainMessage(MESSAGE_DEVICE_NAME);
		Bundle bundle = new Bundle();
		bundle.putString(BluetoothServiceImpl.DEVICE_NAME, "MOCK BLUETOOTH DEVICE");
		msg.setData(bundle);
		mHandler.sendMessage(msg);
		msg = mHandler.obtainMessage(CONNECTION_ESTABLISHED);
		mHandler.sendMessage(msg);

		setState(STATE_CONNECTED);
	}
	public synchronized void stop() {
		if (D)
			if (AppLogger.isDebugEnabled(logger))
				logger.debug("stop");
		if (mMockConnectThread != null) {
			mMockConnectThread.cancel();
			mMockConnectThread = null;
		}
		if (mMockConnectedThread != null) {
			mMockConnectedThread.cancel();
			mMockConnectedThread = null;
		}
//		if (mMockAcceptThread != null) {
//			mMockAcceptThread.cancel();
//			mMockAcceptThread = null;
//		}
		
		// TESTING: Release any WakeLock that may be held
		if(wakeLock.isHeld()) {
			wakeLock.release();
		}
		
		setState(STATE_NONE);
	}

	/**
	 * Write to the MockConnectedThread in an unsynchronized manner
	 * 
	 * @param out
	 *            The bytes to write
	 * @see MockConnectedThread#write(byte[])
	 */
	public void write(byte[] out) {
		// Create temporary object
		MockConnectedThread r;
		// Synchronize a copy of the MockConnectedThread
		synchronized (this) {
			if (mState != STATE_CONNECTED)
				return;
			r = mMockConnectedThread;
		}
		// Perform the write unsynchronized
		r.write(out);
	}

	/**
	 * Indicate that the connection attempt failed and notify the UI Activity.
	 */
	private void connectionFailed() {
		setState(STATE_LISTEN);

		// Send a failure message back to the Activity
		Message msg = mHandler.obtainMessage(CONNECTION_FAILED);
//		Bundle bundle = new Bundle();
//		bundle
//				.putString(BluetoothServiceImpl.TOAST,
//						"Unable to connect device");
//		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}

	/**
	 * Indicate that the connection was lost and notify the UI Activity.
	 */
	private void connectionLost() {
		setState(STATE_LISTEN);

		// Send a failure message back to the Activity
		Message msg = mHandler.obtainMessage(CONNECTION_LOST);
//		Bundle bundle = new Bundle();
//		bundle.putString(BluetoothServiceImpl.TOAST,
//				"Device connection was lost");
//		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}

	private class MockAcceptThread extends Thread {
		public MockAcceptThread() {
			// Do nothing to setup the socket. 
		}
		public void run(){
			if (D)
				if (AppLogger.isTraceEnabled(logger)) logger.trace("BEGIN mMockAcceptThread" + this);
			setName("MockAcceptThread");
			BluetoothSocket socket = null;

			// Listen to the server socket if we're not connected
			while (mState != STATE_CONNECTED) {
				// Assuming that connection is established; removed all the try-to-connect code
				// If a connection was accepted
				if (true) {
					synchronized (MockBluetoothChatService.this) {
						switch (mState) {
						case STATE_LISTEN:
						case STATE_CONNECTING:
							// Situation normal. Start the connected thread.
							connected(socket, socket.getRemoteDevice());
							break;
						case STATE_NONE:
						case STATE_CONNECTED:
							// Either not ready or already connected. Terminate
							// new socket.
//							try {
//								socket.close();
//							} catch (IOException e) {
//								if(AppLogger.isErrorEnabled(logger))
//									logger.error("Could not close unwanted socket",
//										e);
//							}
							break;
						}
					}
				}
			}
			if (D)
				logger.trace("END mMockAcceptThread");
		}
		public void cancel(){
			if (D)
				if (AppLogger.isDebugEnabled(logger)) logger.debug("cancel " + this);
//			try {
//				mmServerSocket.close();
//			} catch (IOException e) {
//				if(AppLogger.isErrorEnabled(logger))
//					logger.error("close() of server failed", e);
//			}
		}
	}
	private class DelayedConnect extends TimerTask {
		public void run(){
			mMockConnectThread = new MockConnectThread();
			if (AppLogger.isDebugEnabled(logger))
				logger.debug("Should be starting MockConnectThread");
			mMockConnectThread.start();
		}
	}
	private class MockConnectThread extends Thread {
		public MockConnectThread() {
//			mmDevice = device;
			BluetoothSocket tmp = null;

			// Get a BluetoothSocket for a connection with the
			// given BluetoothDevice
//			try {
//				if (AppLogger.isDebugEnabled(logger))
//					logger.debug("Trying to connect...");
//				//tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
//				// Hack to make it work on faulty devices
//				BluetoothDevice hxm = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(device.getAddress());
//				Method m;
//				m = hxm.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
//				tmp = (BluetoothSocket)m.invoke(hxm, Integer.valueOf(1));
//			} catch (Exception e) {
//				if(AppLogger.isErrorEnabled(logger))
//					logger.error("create() failed", e);
//			}
//			mmSocket = tmp;
		}
		public void run(){
			logger.trace("BEGIN mMockConnectThread");
			setName("MockConnectThread");
			
//			// Always cancel discovery because it will slow down a connection
//			if(mAdapter.isDiscovering())
//				mAdapter.cancelDiscovery();

			// Make a connection to the BluetoothSocket
//			try {
//				// This is a blocking call and will only return on a
//				// successful connection or an exception
//				mmSocket.connect();
//			} catch (IOException e) {
//				connectionFailed();
//				// Close the socket
//				try {
//					sleep(1000);
//					mmSocket.close();
//					if(AppLogger.isErrorEnabled(logger))
//						logger.error("Closed socket. Exception: " + e);
//				} catch (IOException e2) {
//					if(AppLogger.isErrorEnabled(logger))
//						logger.error("unable to close() socket during connection failure",
//									e2);
//				} catch (InterruptedException e3) {
//					
//				}
//				// Start the service over to restart listening mode
//				MockBluetoothChatService.this.start();
//				return;
//			}

			// Reset the MockConnectThread because we're done
			synchronized (MockBluetoothChatService.this) {
				mMockConnectThread = null;
			}

			// Start the connected thread
			connected();		
		}
		public void cancel(){
//			try {
//				mmSocket.close();
//			} catch (IOException e) {
//				if(AppLogger.isErrorEnabled(logger))
//					logger.error("close() of connect socket failed", e);
//			}		
		}
	}

	private class MockConnectedThread extends Thread {
		
		public MockConnectedThread(){
			if (AppLogger.isDebugEnabled(logger))
				logger.debug("create MockConnectedThread");
			this.setName("BluetoothCommThread");
//			mmSocket = socket;
//			InputStream tmpIn = null;
//			OutputStream tmpOut = null;
//
//			// Get the BluetoothSocket input and output streams
//			try {
//				tmpIn = socket.getInputStream();
//				tmpOut = socket.getOutputStream();
//			} catch (IOException e) {
//				if(AppLogger.isErrorEnabled(logger))
//					logger.error("temp sockets not created", e);
//			}

//			mmInStream = tmpIn;
//			mmOutStream = tmpOut;		
		}
		public void run(){
			if(AppLogger.isInfoEnabled(logger))
				logger.info("BEGIN mMockConnectedThread");
			byte[] readyData = new byte[1024];
			int readyDataSize = 0;

			// Keep listening to the InputStream while connected
			while (true) {
				try{
					// Wait 6 seconds (time board sends reading
					Thread.sleep(6000);
					// THE CURRENT FORMAT FOR DATA FROM THE SENSOR BOARD IS AS
					// FOLLOWS:
					// 'xM' + 1 byte sensor# + 2 bytes sensor value + 'x' + LF +
					// CR
					// Total of 2 + 1 + 2 + 1 + 1 + 1 = 8 bytes
					// The implementation below keeps the first 5 bytes, drops
					// the rest
					
                    readyData = new byte[1024];
                    readyDataSize = 5;
                    int numReadings = 6;
                    int val;
                    for (int i = 0; i < numReadings; i++)
                    {
                    	Thread.sleep(2, 0);
                        val = (int) (90 + (Math.random() * 5));
                    	SensorMessage newMsg = new SensorMessage(i + 1, val );
                    	
                        // Send off the data gathered so far
                        mHandler.obtainMessage(MESSAGE_READ,
                                newMsg.OUTPUT_MSG_SIZE, -1, newMsg.getByteArray())
                                .sendToTarget();                       
                       
                    }
				}catch (InterruptedException e){
					
				}
			}		
		}
		public void write(byte[] buffer){
//				mmOutStream.write(buffer);
				// Share the sent message back to the UI Activity
				mHandler.obtainMessage(MESSAGE_WRITE, -1, -1, buffer)
						.sendToTarget();

		}
		public void cancel(){
	
		}
	}
}