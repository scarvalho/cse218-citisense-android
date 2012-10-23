/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for incoming
 * connections, a thread for connecting with a device, and a thread for
 * performing data transmissions when connected.
 */
public class BluetoothChatService {
	protected final Logger logger = LoggerFactory
			.getLogger(BluetoothChatService.class);

	// Debugging
	protected static final boolean D = true;

	// Name for the SDP record when creating server socket
	protected static final String NAME = "BluetoothChat";

	// Unique UUID for this application
	protected static final UUID MY_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");

	// Member fields
	protected BluetoothAdapter mAdapter;
	protected Handler mHandler;
//	private AcceptThread mAcceptThread;
	protected Timer mConnectTimer;
	protected ConnectThread mConnectThread;
	protected ConnectedThread mConnectedThread;
	protected BluetoothDevice mDevice;
	protected int mState;
	
	protected PowerManager powerManager = ApplicationSettings
			.instance().powerManager();
	protected PowerManager.WakeLock wakeLock;
	
	// Minimum delay before attempting a connection
	protected static final int MIN_CONNECT_DELAY = 500;

	// Constants that indicate the current connection state
	public static final int STATE_NONE = 0; // we're doing nothing
	public static final int STATE_LISTEN = 1; // now listening for incoming
	// connections
	public static final int STATE_CONNECTING = 2; // now initiating an outgoing
	// connection
	public static final int STATE_CONNECTED = 3; // now connected to a remote

	// device

	/**
	 * Constructor. Prepares a new BluetoothChat session.
	 * 
	 * @param handler
	 *            A Handler to send messages back to the UI Activity
	 */
	public BluetoothChatService(Handler handler) {
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		mState = STATE_NONE;
		mHandler = handler;
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BluetoothChatService");
	}

	/** Barebone constructor, for mock
	 * 
	 */
	public BluetoothChatService(){
		mState = STATE_NONE;
		mHandler = null;
		mAdapter = null;
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MockBluetoothChatService");
	}
	
	/**
	 * Set the current state of the chat connection
	 * 
	 * @param state
	 *            An integer defining the current connection state
	 */
	protected synchronized void setState(int state) {
		if (D)
			if (AppLogger.isDebugEnabled(logger)) logger.debug("setState() " + mState + " -> " + state);
		mState = state;

		// Give the new state to the Handler so the UI Activity can update
		mHandler.obtainMessage(MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
	}

	/**
	 * Return the current connection state.
	 */
	public synchronized int getState() {
		return mState;
	}

	/**
	 * Start the chat service. Specifically start AcceptThread to begin a
	 * session in listening (server) mode. Called by the Activity onResume()
	 */
	public synchronized void start() {
		if (D)
			if (AppLogger.isDebugEnabled(logger))
				logger.debug("start");

		// Cancel any thread attempting to make a connection
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		// Cancel any thread currently running a connection
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		// Start the thread to listen on a BluetoothServerSocket
//		if (mAcceptThread == null) {
//			mAcceptThread = new AcceptThread();
//			// No need. We don't listen for incoming connections.
//			//mAcceptThread.start();
//		}
		setState(STATE_LISTEN);
	}

	/**
	 * Start the ConnectThread to initiate a connection to a remote device.
	 * 
	 * @param device
	 *            The BluetoothDevice to connect
	 */
	public synchronized void connect(BluetoothDevice device, int delay) {
		if (D)
			if (AppLogger.isDebugEnabled(logger)) logger.debug("connect to: " + device);

		// Cancel any thread attempting to make a connection
//		if (mState == STATE_CONNECTING) {
			if (mConnectThread != null) {
				mConnectThread.cancel();
				mConnectThread = null;
			}
//		}

		// Cancel any thread currently running a connection
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
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

	/**
	 * Start the ConnectedThread to begin managing a Bluetooth connection
	 * 
	 * @param socket
	 *            The BluetoothSocket on which the connection was made
	 * @param device
	 *            The BluetoothDevice that has been connected
	 */
	public synchronized void connected(BluetoothSocket socket,
			BluetoothDevice device) {
		if (D)
			if (AppLogger.isDebugEnabled(logger))
				logger.debug("connected");

		// Cancel the thread that completed the connection
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		// Cancel any thread currently running a connection
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		// Cancel the accept thread because we only want to connect to one
		// device
//		if (mAcceptThread != null) {
//			mAcceptThread.cancel();
//			mAcceptThread = null;
//		}

		// Start the thread to manage the connection and perform transmissions
		mConnectedThread = new ConnectedThread(socket);
		mConnectedThread.start();
		
		// TESTING: Try holding onto a WakeLock while connected
		if(!wakeLock.isHeld()) {
			wakeLock.acquire();
		}

		// Send the name of the connected device back to the UI Activity
		Message msg = mHandler.obtainMessage(MESSAGE_DEVICE_NAME);
		Bundle bundle = new Bundle();
		bundle.putString(BluetoothServiceImpl.DEVICE_NAME, device.getName());
		msg.setData(bundle);
		mHandler.sendMessage(msg);
		msg = mHandler.obtainMessage(CONNECTION_ESTABLISHED);
		mHandler.sendMessage(msg);

		setState(STATE_CONNECTED);
	}

	/**
	 * Stop all threads
	 */
	public synchronized void stop() {
		if (D)
			if (AppLogger.isDebugEnabled(logger))
				logger.debug("stop");
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}
//		if (mAcceptThread != null) {
//			mAcceptThread.cancel();
//			mAcceptThread = null;
//		}
		
		// TESTING: Release any WakeLock that may be held
		if(wakeLock.isHeld()) {
			wakeLock.release();
		}
		
		setState(STATE_NONE);
	}

	/**
	 * Write to the ConnectedThread in an unsynchronized manner
	 * 
	 * @param out
	 *            The bytes to write
	 * @see ConnectedThread#write(byte[])
	 */
	public void write(byte[] out) {
		// Create temporary object
		ConnectedThread r;
		// Synchronize a copy of the ConnectedThread
		synchronized (this) {
			if (mState != STATE_CONNECTED)
				return;
			r = mConnectedThread;
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

	/**
	 * This thread runs while listening for incoming connections. It behaves
	 * like a server-side client. It runs until a connection is accepted (or
	 * until cancelled).
	 */
	private class AcceptThread extends Thread {
		// The local server socket
		private final BluetoothServerSocket mmServerSocket;

		public AcceptThread() {
			BluetoothServerSocket tmp = null;

			// Create a new listening server socket
			try {
				tmp = mAdapter
						.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
			} catch (IOException e) {
				// This could happen if for example Bluetooth not available, 
				// or insufficient permissions, or channel in use
				if(AppLogger.isErrorEnabled(logger))
					logger.error("listen() failed", e);
			}
			mmServerSocket = tmp;
		}

		public void run() {
			if (D)
				if (AppLogger.isTraceEnabled(logger)) logger.trace("BEGIN mAcceptThread" + this);
			setName("AcceptThread");
			BluetoothSocket socket = null;

			// Listen to the server socket if we're not connected
			while (mState != STATE_CONNECTED) {
				try {
					// This is a blocking call and will only return on a
					// successful connection or an exception
					socket = mmServerSocket.accept();
				} catch (IOException e) {
					if(AppLogger.isErrorEnabled(logger))
						logger.error("accept() failed", e);
					break;
				} catch (NullPointerException e) {
					if(AppLogger.isErrorEnabled(logger))
						logger.error("mmServerSocket was null, probably due to failing listen() call");
					break;
				}

				// If a connection was accepted
				if (socket != null) {
					synchronized (BluetoothChatService.this) {
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
							try {
								socket.close();
							} catch (IOException e) {
								if(AppLogger.isErrorEnabled(logger))
									logger.error("Could not close unwanted socket",
										e);
							}
							break;
						}
					}
				}
			}
			if (D)
				logger.trace("END mAcceptThread");
		}

		public void cancel() {
			if (D)
				if (AppLogger.isDebugEnabled(logger)) logger.debug("cancel " + this);
			try {
				mmServerSocket.close();
			} catch (IOException e) {
				if(AppLogger.isErrorEnabled(logger))
					logger.error("close() of server failed", e);
			}
		}
	}
	
	private class DelayedConnect extends TimerTask {
		public void run() {
			mConnectThread = new ConnectThread(mDevice);
			if (AppLogger.isDebugEnabled(logger))
				logger.debug("Should be starting ConnectThread");
			mConnectThread.start();
		}
	}

	/**
	 * This thread runs while attempting to make an outgoing connection with a
	 * device. It runs straight through; the connection either succeeds or
	 * fails.
	 */
	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;

		public ConnectThread(BluetoothDevice device) {
			mmDevice = device;
			BluetoothSocket tmp = null;

			// Get a BluetoothSocket for a connection with the
			// given BluetoothDevice
			try {
				if (AppLogger.isDebugEnabled(logger))
					logger.debug("Trying to connect...");
				//tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
				// Hack to make it work on faulty devices
				BluetoothDevice hxm = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(device.getAddress());
				Method m;
				m = hxm.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
				tmp = (BluetoothSocket)m.invoke(hxm, Integer.valueOf(1));
			} catch (Exception e) {
				if(AppLogger.isErrorEnabled(logger))
					logger.error("create() failed", e);
			}
			mmSocket = tmp;
		}

		public void run() {
			logger.trace("BEGIN mConnectThread");
			setName("ConnectThread");
			
			// Always cancel discovery because it will slow down a connection
			if(mAdapter.isDiscovering())
				mAdapter.cancelDiscovery();

			// Make a connection to the BluetoothSocket
			try {
				// This is a blocking call and will only return on a
				// successful connection or an exception
				mmSocket.connect();
			} catch (IOException e) {
				connectionFailed();
				// Close the socket
				try {
					sleep(1000);
					mmSocket.close();
					if(AppLogger.isErrorEnabled(logger))
						logger.error("Closed socket. Exception: " + e);
				} catch (IOException e2) {
					if(AppLogger.isErrorEnabled(logger))
						logger.error("unable to close() socket during connection failure",
									e2);
				} catch (InterruptedException e3) {
					
				}
				// Start the service over to restart listening mode
				BluetoothChatService.this.start();
				return;
			}

			// Reset the ConnectThread because we're done
			synchronized (BluetoothChatService.this) {
				mConnectThread = null;
			}

			// Start the connected thread
			connected(mmSocket, mmDevice);
		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				if(AppLogger.isErrorEnabled(logger))
					logger.error("close() of connect socket failed", e);
			}
		}
	}

	/**
	 * This thread runs during a connection with a remote device. It handles all
	 * incoming and outgoing transmissions.
	 */
	private class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public ConnectedThread(BluetoothSocket socket) {
			if (AppLogger.isDebugEnabled(logger))
				logger.debug("create ConnectedThread");
			this.setName("BluetoothCommThread");
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the BluetoothSocket input and output streams
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				if(AppLogger.isErrorEnabled(logger))
					logger.error("temp sockets not created", e);
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		public void run() {
			if(AppLogger.isInfoEnabled(logger))
				logger.info("BEGIN mConnectedThread");
			byte[] buffer = new byte[1024];
			byte[] readyData = new byte[1024];

			boolean messageStarted = false;
			int bytesToDiscard = 0;

			int bytes;
			int readyDataSize = 0;

			// Keep listening to the InputStream while connected
			while (true) {
				try {
					// Wait 6 seconds (time board sends reading
					Thread.sleep(6000);
					// THE CURRENT FORMAT FOR DATA FROM THE SENSOR BOARD IS AS
					// FOLLOWS:
					// 'xM' + 1 byte sensor# + 2 bytes sensor value + 'x' + LF +
					// CR
					// Total of 2 + 1 + 2 + 1 + 1 + 1 = 8 bytes
					// The implementation below keeps the first 5 bytes, drops
					// the rest

					// Read from the InputStream
					bytes = mmInStream.read(buffer);
					// DEBUG
					//byte[] lastPayloadCopy = new byte[bytes];
					//System.arraycopy(buffer, 0, lastPayloadCopy, 0, bytes);
					//logger.debug("Last message: " + Arrays.toString(lastPayloadCopy));
					// END DEBUG
					// While data remains to be processed
					for (int i = 0; i < bytes; i++) {
						
						// Check to see if any bytes should be discarded
						if (bytesToDiscard > 0) {
							bytesToDiscard--;
							continue;
						}
						if (buffer[i] == (byte) 'x') {
							// If we got a complete message...
							if (messageStarted && readyDataSize == 5) {
								messageStarted = false;
								// Skip the next two bytes
								// bytesToDiscard = 2;
								
								// Send off the data gathered so far
								mHandler.obtainMessage(MESSAGE_READ,
										readyDataSize, -1, readyData)
										.sendToTarget();
								
								// Continue onto the next byte
								continue;
							}
							// else if is beginning of message
							// else some data was lost in the process...
							else {
								// Allocate new buffer for next message
								readyData = new byte[1024];
								readyDataSize = 0;
								// Start tracking from here
								messageStarted = true;
							}
						}
						if (messageStarted) {
							readyData[readyDataSize] = buffer[i];
							readyDataSize++;
						}
					}

				} catch (IOException e) {
					if(AppLogger.isErrorEnabled(logger))
						logger.error("disconnected", e);
					connectionLost();
					break;
				} catch (InterruptedException e) {
					
				}
			}
		}

		/**
		 * Write to the connected OutStream.
		 * 
		 * @param buffer
		 *            The bytes to write
		 */
		public void write(byte[] buffer) {
			try {
				mmOutStream.write(buffer);

				// Share the sent message back to the UI Activity
				mHandler.obtainMessage(MESSAGE_WRITE, -1, -1, buffer)
						.sendToTarget();
			} catch (IOException e) {
				if(AppLogger.isErrorEnabled(logger))
						logger.error("Exception during write", e);
			}
		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				if(AppLogger.isErrorEnabled(logger))
					logger.error("close() of connect socket failed", e);
			}
		}
	}
}
