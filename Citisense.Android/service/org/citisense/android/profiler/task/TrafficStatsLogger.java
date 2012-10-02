package org.citisense.android.profiler.task;


import java.io.IOException;
import java.util.HashMap;
import java.util.TimerTask;

import org.citisense.android.profiler.DeviceStateRecorder;
import org.citisense.android.profiler.log.LogWriter;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.TrafficStats;
import android.util.Log;

public class TrafficStatsLogger extends TimerTask {

	private static final String TAG = "TrafficStatsLogger";

	private LogWriter dlw = LogWriter.getInstance();

	private Context context;

	private DeviceStateRecorder deviceStateRecoder;

	private static boolean dataTrafficActive = false;

	private static long previousTotalBytes = 0;

	public static boolean isDataTrafficActive() {
		return dataTrafficActive;
	}

	public TrafficStatsLogger(DeviceStateRecorder deviceStateRecoder,
			Context context) {
		this.context = context;
		this.deviceStateRecoder = deviceStateRecoder;
	}

	@Override
	public synchronized void run() {
		long total = TrafficStats.getTotalRxBytes()
				+ TrafficStats.getTotalTxBytes();
		if (previousTotalBytes == total) {
			dataTrafficActive = false;
		} else {
			dataTrafficActive = true;
		}
	
		long mobileTotal = TrafficStats.getMobileRxBytes()
				+ TrafficStats.getMobileTxBytes();
		long wifiTotal = (total - mobileTotal);
		

		PackageManager pm = context.getPackageManager();
		HashMap<Integer, Long> uids = new HashMap<Integer, Long>();
		long totalSum=0;
		for (ApplicationInfo appInfo : pm
				.getInstalledApplications(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)) {
			long bytes = TrafficStats.getUidRxBytes(appInfo.uid);
			bytes += TrafficStats.getUidTxBytes(appInfo.uid);
			if (!uids.containsKey(appInfo.uid)) {
				uids.put(appInfo.uid, bytes);
				// few applications can refer to same uid - sum each uid stats
				// only once
				if (bytes > 0) {
					Log.d(TAG, "package " + appInfo.packageName + ": " + bytes);
					writePackageTrafficToLog(appInfo.packageName, bytes);
					totalSum+=bytes;
				}
			}
		}
		previousTotalBytes = total;
		writeTotalTrafficToLog(total, mobileTotal, wifiTotal);
		deviceStateRecoder.scheduleTrafficStatsLogger();
		
		//I am observing the same issue here
		//http://stackoverflow.com/questions/8478696/android-trafficstats-gettotalrxbytes-is-less-than-expected
		Log.d(TAG, "total=" + total + " mob=" + mobileTotal + " wifi="
				+ wifiTotal+ " app_sum="+totalSum);
	}

	private void writePackageTrafficToLog(String packageName, long bytes) {
		StringBuffer sb = new StringBuffer();
		sb.append(packageName);
		sb.append(",");
		sb.append(bytes);
		try {
			dlw.open();
		} catch (IOException e) {
			Log.e(TAG, "Error opening file! ", e);
			return;
		}
		try {
			dlw.writeEvent("package_traffic", sb.toString());
			dlw.close();
		} catch (IOException e) {
			Log.e(TAG, "Error writing to file ", e);
		}
	}

	private void writeTotalTrafficToLog(long total, long mobileTotal,
			long wifiTotal) {
		StringBuffer sb = new StringBuffer();
		sb.append(total);
		sb.append(",");
		sb.append(mobileTotal);
		sb.append(",");
		sb.append(wifiTotal);
		try {
			dlw.open();
		} catch (IOException e) {
			Log.e(TAG, "Error opening file! ", e);
			return;
		}
		try {
			dlw.writeEvent("traffic_stats", sb.toString());
			dlw.close();
		} catch (IOException e) {
			Log.e(TAG, "Error writing to file ", e);
		}

	}

}
