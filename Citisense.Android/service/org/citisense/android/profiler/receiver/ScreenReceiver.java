package org.citisense.android.profiler.receiver;


import java.io.IOException;

import org.citisense.android.profiler.log.LogWriter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.TextView;

public class ScreenReceiver extends BroadcastReceiver {

	private static final String TAG = "ScreenReceiver";
	private TextView text;
	private Context context;
	private LogWriter dlw = LogWriter.getInstance();

	public void onReceive(Context context, Intent intent) {

		Log.d(TAG, "thread name: " + Thread.currentThread().getName());
		Thread.currentThread().getId();

		StringBuffer b = new StringBuffer();

		if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
			b.append("Screen: OFF");
			writeToLog("off");
		} else {
			b.append("Screen: ON");

			writeToLog("on");
		}

		if (text != null) {
			text.setText(b.toString());
		}
		Log.d(TAG, intent.getAction());
		b.append("\n");
		Log.d(TAG, b.toString());

	}

	private void writeToLog(String state) {
		try {
			dlw.open();
		} catch (IOException e) {
			Log.e(TAG, "Error opening file, " + e.getMessage());
			return;
		}
		try {
			dlw.writeEvent("screen", state);
			dlw.close();
		} catch (IOException e) {
			Log.e(TAG, "Error writing to file, " + e.getMessage());
		}

	}

	public void setTextView(TextView screenText) {
		text = screenText;
	}

	public void register(Context context) {
		this.context = context;
		Log.i(TAG, "registering ACTION_SCREEN_OFF");
		IntentFilter intentOff = new IntentFilter(Intent.ACTION_SCREEN_OFF);
		context.registerReceiver(this, intentOff);

		Log.i(TAG, "registering ACTION_SCREEN_ON");
		IntentFilter intentOn = new IntentFilter(Intent.ACTION_SCREEN_ON);
		context.registerReceiver(this, intentOn);
		context.getSystemService(Context.TELEPHONY_SERVICE);

	}

	public void unRegister() {
		context.unregisterReceiver(this);
	}

}
