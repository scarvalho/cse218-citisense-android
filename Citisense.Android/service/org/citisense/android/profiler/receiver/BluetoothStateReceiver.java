package org.citisense.android.profiler.receiver;


import java.io.IOException;

import org.citisense.android.profiler.log.LogWriter;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.TextView;

public class BluetoothStateReceiver extends BroadcastReceiver {

	private static final String TAG = "BluetoothStateReceiver";
	private Context context;
	private TextView text;
	private LogWriter dlw = LogWriter.getInstance();

	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "blueToothReceiver " + intent.getAction());
		int s = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
		String state = getBlueToothState(s);
		StringBuffer b = new StringBuffer();
		b.append("Bluetooth state: " + state);

		writeToLog(state);
		Log.d(TAG, b.toString());
		if (text != null) {
			text.setText(b.toString());
		}

	}

	private void writeToLog(String state) {
		try {
			dlw.open();
		} catch (IOException e) {
			Log.e(TAG, "Error opening file, " + e.getMessage());
			return;
		}
		try {
			dlw.writeEvent("bluetooth", state);
			dlw.close();
		} catch (IOException e) {
			Log.e(TAG, "Error writing to file, " + e.getMessage());
		}

	}

	private String getBlueToothState(int s) {
		String state;
		switch (s) {
		case BluetoothAdapter.STATE_OFF:
			state = "off";
			break;
		case BluetoothAdapter.STATE_ON:
			state = "on";
			break;
		case BluetoothAdapter.STATE_TURNING_OFF:
			state = "turning_off";
			break;
		case BluetoothAdapter.STATE_TURNING_ON:
			state = "turning_on";
			break;
		default:
			state = "system_error";
			Log.i(TAG, "bluetooth state: " + s);
			break;
		}
		return state;
	}

	public void register(Context context) {
		Log.i(TAG, "registering BluetoothStateReceiver");
		IntentFilter filter = new IntentFilter(
				BluetoothAdapter.ACTION_STATE_CHANGED);
		context.registerReceiver(this, filter);
		this.context = context;
	}

	public void unRegister() {
		context.unregisterReceiver(this);
	}

	public void setTextView(TextView text) {
		this.text = text;
	}

}
