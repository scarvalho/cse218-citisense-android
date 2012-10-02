package org.citisense.android.profiler.receiver;


import java.io.IOException;

import org.citisense.android.profiler.log.LogWriter;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.TextView;


public class WifiStateReceiver extends BroadcastReceiver {

	private TextView stateText;
	
	private TextView rssiText;

	private Context context;

	private static final String TAG = "NetworkStateReceiver";
	
	private LogWriter dlw = LogWriter.getInstance();

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "action: " + intent.getAction());

		if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
			onConnectivityEvent(intent);
		}
		if (intent.getAction().equals(WifiManager.RSSI_CHANGED_ACTION)) {
			onRSSIEvent(intent);
		}
	}

	private void onRSSIEvent(Intent intent) {
		WifiManager myWifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		WifiInfo myWifiInfo = myWifiManager.getConnectionInfo();
		Log.d(TAG, "Wifi RSSI: " + myWifiInfo.getRssi());
		if (rssiText != null) {
			rssiText.setText("Wifi RSSI: " + myWifiInfo.getRssi());
		}
		writeRssiToLog(myWifiInfo.getRssi());
		
		

	}
	
	private void writeRssiToLog(int rssi) {
		try {
			dlw.open();
		} catch (IOException e) {
			Log.e(TAG, "Error opening file, " + e.getMessage());
			return;
		}
		try {
			dlw.writeEvent("wifi_rssi", rssi+"");
			dlw.close();
		} catch (IOException e) {
			Log.e(TAG, "Error writing to file, " + e.getMessage());
		}

	}

	private void onConnectivityEvent(Intent intent) {

		boolean noConnectivity = intent.getBooleanExtra(
				ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);

		String reason = intent.getStringExtra(ConnectivityManager.EXTRA_REASON);

		boolean isFailover = intent.getBooleanExtra(
				ConnectivityManager.EXTRA_IS_FAILOVER, false);

		NetworkInfo currentNetworkInfo = (NetworkInfo) intent
				.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);

		String currentName = currentNetworkInfo.getTypeName();
		int currentType = currentNetworkInfo.getType();
		String currentState = currentNetworkInfo.getState().name();
		boolean connecting = currentNetworkInfo.isConnectedOrConnecting();

		NetworkInfo otherNetworkInfo = (NetworkInfo) intent
				.getParcelableExtra(ConnectivityManager.EXTRA_OTHER_NETWORK_INFO);

		String otherName = null;
		int otherType = 0;
		String otherState = null;
		boolean otherConnecting = false;

		if (otherNetworkInfo != null) {
			otherName = otherNetworkInfo.getTypeName();
			otherType = otherNetworkInfo.getType();
			otherState = otherNetworkInfo.getState().name();
			otherConnecting = otherNetworkInfo.isConnectedOrConnecting();
		}

		StringBuffer b = new StringBuffer();
		b.append("No Connectivity: " + noConnectivity);
		b.append("\n");
		b.append("Reason: " + reason);
		b.append("\n");
		b.append("Failover: " + isFailover);
		b.append("\n");
		b.append("Current network name: " + currentName);
		b.append("\n");
		b.append("Current networktype: " + currentType);
		b.append("\n");
		b.append("Current network state: " + currentState);
		b.append("\n");
		b.append("Current network connecting: " + connecting);
		b.append("\n");
		b.append("Other network name: " + otherName);
		b.append("\n");
		b.append("Other networktype: " + otherType);
		b.append("\n");
		b.append("Other network state: " + otherState);
		b.append("\n");
		b.append("Other network connecting: " + otherConnecting);
		Log.d(TAG, b.toString());
		if (stateText != null) {
			stateText.setText(b.toString());
		}
		writeStateToLog(currentState);
	}

	private void writeStateToLog(String currentState) {
		try {
			dlw.open();
		} catch (IOException e) {
			Log.e(TAG, "Error opening file, " + e.getMessage());
			return;
		}
		try {
			dlw.writeEvent("wifi_state", currentState);
			dlw.close();
		} catch (IOException e) {
			Log.e(TAG, "Error writing to file, " + e.getMessage());
		}
		
	}

	public void setStateTextView(TextView wifiLevel2) {
		this.stateText = wifiLevel2;

	}
	
	public void setRssiTextView(TextView wifiLevel2) {
		this.rssiText = wifiLevel2;

	}

	public void register(Context context) {
		Log.d(TAG, "registering WifiStatus");
		IntentFilter wifiFilter = new IntentFilter(
				ConnectivityManager.CONNECTIVITY_ACTION);
		context.registerReceiver(this, new IntentFilter(
				WifiManager.RSSI_CHANGED_ACTION));
		context.registerReceiver(this, wifiFilter);
		this.context = context;

	}

	public void unRegister() {
		context.unregisterReceiver(this);
	}

}
