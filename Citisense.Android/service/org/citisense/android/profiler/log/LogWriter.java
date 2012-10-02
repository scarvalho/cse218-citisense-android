package org.citisense.android.profiler.log;


import java.io.BufferedWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;

import org.citisense.android.profiler.utility.StringDate;


import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.util.Log;

public class LogWriter {

	private final String TAG = "DataLogWriter";
	private ReentrantLock lock;
	private String fileName = "battery_info.log";
	private BufferedWriter out;
	private String osVersion;
	private String model;
	private String deviceId;

	private static LogWriter dlw = new LogWriter();

	public synchronized void open() throws IOException {

		String state = Environment.getExternalStorageState();
		if (!state.equals("mounted")) {
			Log.e(TAG, "the sd card is not mounted, state:" + state);
			throw new IOException("The sd card is not mounted");
		}

		lock.lock();

		Log.d(TAG, "Trying to open file:" + fileName+" "+Thread.currentThread().getId());
		File root = Environment.getExternalStorageDirectory();
		File file = new File(root, fileName);
		try {
			Log.i(TAG, "logwriter instance:" + this);
			if (!file.exists()) {
				createNewLog(file);
			}
		} catch (IOException e) {
			lock.unlock();
			Log.e(TAG, "Could not create file: "+fileName, e);
			throw e;
		}
		if (!file.canWrite()) {
			lock.unlock();
			throw new IOException("The file can not be written: " + fileName);
		}
		FileWriter fw = new FileWriter(file, true);
		out = new BufferedWriter(fw, 1024 * 8);
		Log.d(TAG, "exiting open file:" + fileName+" "+Thread.currentThread().getId());
	}

	private void createNewLog(File file) throws IOException {
		file.createNewFile();
		FileWriter fw = new FileWriter(file, true);
		Log.d(TAG, "Creating file...");
		if(deviceId==null){
			Log.e(TAG, "The IMEI is null!!!");
		}
		SimpleDateFormat formatter = new SimpleDateFormat("yyMMdd_HHmmss");
		String s = formatter.format(new Date());
		fw.append("date,");
		fw.append(s);
		fw.append("imei,");
		fw.append(deviceId);
		fw.append("\n");
		fw.append("model,");
		fw.append(model);
		fw.append("\n");
		fw.append("os,");
		fw.append(osVersion);
		fw.append("\n");
		fw.append("#");
		fw.append("\n");
		fw.append("#cell_data_conn_state, type, state");
		fw.append("\n");
		fw.append("#wifi_rssi, value");
		fw.append("\n");
		fw.append("#cell_services_state, type, state, activity");
		fw.append("\n");
		fw.append("#cell_signal, value");
		fw.append("\n");
		fw.append("#wifi_state, state");
		fw.append("\n");
		fw.append("#battery_info, level, health, status, voltage, technology, temperature");
		fw.append("\n");
		fw.append("#traffic_stats, total bytes, mobile bytes, wifi bytes");
		fw.append("\n");
		fw.append("#gps, state");
		fw.append("\n");
		fw.append("#bluetooth, state");
		fw.append("\n");
		fw.append("#screen, state");
		fw.append("\n");
		fw.append("#");
		fw.append("\n");
		fw.close();
	}

	private LogWriter() {
		lock = new ReentrantLock();
	}

	public static LogWriter getInstance() {
		return dlw;
	}

	public void close() {
		if (!lock.isHeldByCurrentThread()) {
			Log.e(TAG, "Lock is not held by current thread");
			return;
		}
		if (out == null) {
			Log.e(TAG, "File was not opened: " + fileName);
			return;
		}
		try {
			out.close();
			out = null;
		} catch (IOException e) {
			Log.e(TAG, "Could not close file: " + fileName, e);
		}
		lock.unlock();
	}

	public void writeEvent(String field, String value) throws IOException {

		if (!lock.isHeldByCurrentThread()) {
			Log.e(TAG, "Lock is not held by current thread");
			return;
		}

		if (out == null) {
			Log.e(TAG, "File was not opened: " + fileName);
			return;
		}
		StringBuffer buffer = new StringBuffer();
		buffer.append(StringDate.getCurrentDate());
		buffer.append(",");
		buffer.append(field);
		buffer.append(",");
		buffer.append(value);
		// buffer.append(" ");
		// buffer.append(Thread.currentThread().getId());
		buffer.append("\n");
		try {
			out.append(buffer.toString());
		} catch (IOException e) {
			Log.e(TAG, "Could not write to file: " + fileName, e);
			throw e;
		}
	}

	public void copyToFile(String string) {
		// TODO Auto-generated method stub

	}

	public void setContext(Context context) {
		TelephonyManager cellphone = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		Log.i(TAG, "Setting phone model, id, version....");
		deviceId = cellphone.getDeviceId();
		osVersion = Build.VERSION.RELEASE;
		model = Build.MODEL;
		if(deviceId==null){
			Log.e(TAG, "The imei is null!!!!");
		}
	}

	public void delete() throws IOException {
		String state = Environment.getExternalStorageState();
		if (!state.equals("mounted")) {
			Log.e(TAG, "the sd card is not mounted, state:" + state);
			throw new IOException("The sd card is not mounted");
		}
		
		Log.d(TAG, "Trying to delete file:" + fileName);
		File root = Environment.getExternalStorageDirectory();
		File file = new File(root, fileName);
		if (!file.delete()) {
			Log.e(TAG, "File could not be deleted: " + fileName);
		}
	}

	public String getContent() throws IOException {
		StringBuffer sb = new StringBuffer();
	
		
		String state = Environment.getExternalStorageState();
		if (!state.equals("mounted")) {
			Log.e(TAG, "the sd card is not mounted, state:" + state);
			throw new IOException("The sd card is not mounted");
		}
		
		Log.d(TAG, "Trying to read file:" + fileName);
		File root = Environment.getExternalStorageDirectory();
		File file = new File(root, fileName);

		FileReader fr = new FileReader(file);

		BufferedReader bw = new BufferedReader(fr);
		String line;
		try {
			line = bw.readLine();
			while (line != null) {
				sb.append(line);
				sb.append("\n");
				line = bw.readLine();
			}
		} catch (IOException e) {
			Log.e(TAG, "Error reading file: " + fileName, e);
		}

		return sb.toString();
	}

}
