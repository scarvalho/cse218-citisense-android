package org.citisense.android.profiler.task;


import java.io.IOException;
import java.util.TimerTask;

import org.citisense.android.profiler.log.LogUploader;
import org.citisense.android.profiler.log.LogWriter;

import android.content.Context;
import android.util.Log;


public class LogUploaderTask extends TimerTask {

	private static final String TAG = "LogUploaderTask";

	private LogWriter dlw = LogWriter.getInstance();

	private Context context;

	@Override
	public void run() {
		
		if(!TrafficStatsLogger.isDataTrafficActive()){
			Log.e(TAG, "Traffic is not active!");
			return;
		}
		Log.i(TAG, "Traffic is active");
		if (true) {
			//debugFunction();
			return;
		}
		
		try {
			dlw.open();
		} catch (IOException e) {
			Log.e(TAG, "File could not be opened!", e);
			return;
		}
		// dlw.copyToFile("tmp.dat");
		String content = null;
		try {
			content = dlw.getContent();
		} catch (IOException e) {
			Log.e(TAG, "Error getting log data", e);
		}
		LogUploader dls = new LogUploader(context);
		String data = dls.compressData(content.getBytes());
		try {
			dls.postSensorData(data);
			dlw.delete();
		} catch (Exception e) {
			Log.e(TAG, "Error on posting log data", e);
			return;
		}
		dlw.close();

	}

	private void debugFunction() {
		try {
			dlw.open();
		} catch (IOException e) {
			Log.e(TAG, "File could not be opened!", e);
			return;
		}
		// dlw.copyToFile("tmp.dat");
		String content = null;
		try {
			content = dlw.getContent();
		} catch (IOException e) {
			Log.e(TAG, "Error getting log data", e);
		}
		try {

			dlw.delete();
		} catch (Exception e) {
			Log.e(TAG, "Error on posting log data", e);
			return;
		}
		dlw.close();

	}

	public LogUploaderTask(Context context) {
		this.context = context;
	}
}
