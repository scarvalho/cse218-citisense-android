package org.citisense.android.profiler.listener;


import java.io.IOException;

import org.citisense.android.profiler.log.LogWriter;

import android.content.Context;
import android.location.GpsStatus;
import android.location.LocationManager;
import android.util.Log;
import android.widget.TextView;

public class GpsStatusListener implements GpsStatus.Listener{
	
	private static final String TAG = "GpsStatusListener";
	private TextView text;
	private LogWriter dlw = LogWriter.getInstance();
	private LocationManager manager;

	public void onGpsStatusChanged(int event) {
		String name=getEventName(event);
		StringBuffer b=new StringBuffer();
		b.append("GPS state: "+name+"\n");
		Log.d(TAG,"GPS state: "+name);
		if (text != null) {
			text.setText(b.toString());
		}
		writeToLog(name);
	}

	private String getEventName(int event) {
		Log.i(TAG, "onGpsStatusChanged: " + event);
		if (event == GpsStatus.GPS_EVENT_FIRST_FIX) {
			return "GPS_EVENT_FIRST_FIX";
		}
		if (event == GpsStatus.GPS_EVENT_STARTED) {
			return "GPS_EVENT_STARTED";
		}
		if (event == GpsStatus.GPS_EVENT_STOPPED) {
			return "GPS_EVENT_STOPPED";
		}
		if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) {
			return "GPS_EVENT_SATELLITE_STATUS";
		}
		return "system_error";
	}

	public void setTextView(TextView gpsText) {
		this.text=gpsText;
	}

	public void listen(Context context) {
		manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		manager.addGpsStatusListener(this);
	}
	
	private void writeToLog(String state) {
		try {
			dlw.open();
		} catch (IOException e) {
			Log.e(TAG, "Error opening file, " + e.getMessage());
			return;
		}
		try {
			dlw.writeEvent("gps", state);
			dlw.close();
		} catch (IOException e) {
			Log.e(TAG, "Error writing to file, " + e.getMessage());
		}

	}

	public void unRegister() {
		manager.removeGpsStatusListener(this);
	}

}
