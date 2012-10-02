package org.citisense.android.profiler.receiver;


import java.io.IOException;

import org.citisense.android.profiler.log.LogWriter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;
import android.widget.TextView;

public class BatteryStateReceiver extends BroadcastReceiver {

	private static final String TAG = "BatteryStateReceiver";
	private TextView batteryLevel;
	private Context context;
	private LogWriter dlw = LogWriter.getInstance();

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "thread name: " + Thread.currentThread().getName());
		Log.d(TAG, intent.getAction());
		int h = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
		String health = getHealth(h);
		int s = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
		String status = getStatus(s);
		int rawlevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
		int voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
		double temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10;
		String tech = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);
		int level = -1;
		if (rawlevel >= 0 && scale > 0) {
			level = (rawlevel * 100) / scale;
		}
		StringBuffer b = new StringBuffer();
		b.append("Battery Level Remaining: " + level + "%");
		b.append("\n");
		b.append("Health: " + health);
		b.append("\n");
		b.append("Status: " + status);
		b.append("\n");
		b.append("Voltage: " + voltage + "mV");
		b.append("\n");
		b.append("Technology: " + tech);
		b.append("\n");
		b.append("Temperature: " + temp + "°C");
	
		writeToLog(level,health,status,voltage,tech,temp);
	
		if (batteryLevel != null) {
			batteryLevel.setText(b.toString());
		}
		Log.d(TAG, b.toString());

	}

	private void writeToLog(int level, String health, String status,
			int voltage, String tech, double temp) {
		
		try {
			dlw.open();
		} catch (IOException e) {
			Log.e(TAG, "Error opening file, " + e.getMessage());
			return;
		}
		
		StringBuffer sb = new StringBuffer();
		sb.append(level + "%");
		sb.append(",");
		sb.append(health);
		sb.append(",");
		sb.append(status);
		sb.append(",");
		sb.append(voltage + "mV");
		sb.append(",");
		sb.append(tech);
		sb.append(",");
		sb.append(temp + "°C");
		
		try {
			dlw.writeEvent("battery_info", sb.toString());
			dlw.close();
		} catch (IOException e) {
			Log.e(TAG, "Error writing to file, " + e.getMessage());
		}
		
	}

	public static String getStatus(int s) {
		String status;
		switch (s) {
		case BatteryManager.BATTERY_STATUS_UNKNOWN:
			status = "unknwon";
			break;
		case BatteryManager.BATTERY_STATUS_CHARGING:
			status = "charging";
			break;
		case BatteryManager.BATTERY_STATUS_DISCHARGING:
			status = "discharging";
			break;
		case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
			status = "not_charging";
			break;
		case BatteryManager.BATTERY_STATUS_FULL:
			status = "full";
			break;
		default:
			status = "system_error";
			break;
		}
		return status;
	}

	public static String getHealth(int h) {
		String health;

		switch (h) {
		case BatteryManager.BATTERY_HEALTH_UNKNOWN:
			health = "Unknwon";
			break;
		case BatteryManager.BATTERY_HEALTH_GOOD:
			health = "Good";
			break;
		case BatteryManager.BATTERY_HEALTH_OVERHEAT:
			health = "Overheat";
			break;
		case BatteryManager.BATTERY_HEALTH_DEAD:
			health = "Dead";
			break;
		case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
			health = "Over voltage";
			break;
		case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
			health = "Unspecified failure";
			break;
		default:
			health = "System error";
			break;
		}
		return health;
	}

	public void setTextView(TextView batteryLevel) {
		this.batteryLevel = batteryLevel;
	}

	public void register(Context context) {
		Log.i(TAG, "registering batteryLevelReceiver");
		IntentFilter batteryLevelFilter = new IntentFilter(
				Intent.ACTION_BATTERY_CHANGED);
		context.registerReceiver(this, batteryLevelFilter);
		this.context = context;
	}

	public void unRegister() {
		context.unregisterReceiver(this);
	}

}
