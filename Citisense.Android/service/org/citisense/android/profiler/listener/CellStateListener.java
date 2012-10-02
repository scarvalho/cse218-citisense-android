package org.citisense.android.profiler.listener;


import java.io.IOException;

import org.citisense.android.profiler.log.LogWriter;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TextView;

public class CellStateListener extends PhoneStateListener {

	public static String getCellDataActivityName(int dataActivity) {

		switch (dataActivity) {
		case TelephonyManager.DATA_ACTIVITY_DORMANT:
			return "DATA_ACTIVITY_DORMANT";
		case TelephonyManager.DATA_ACTIVITY_IN:
			return "DATA_ACTIVITY_IN";
		case TelephonyManager.DATA_ACTIVITY_INOUT:
			return "DATA_ACTIVITY_INOUT";
		case TelephonyManager.DATA_ACTIVITY_NONE:
			return "DATA_ACTIVITY_NONE";
		case TelephonyManager.DATA_ACTIVITY_OUT:
			return "DATA_ACTIVITY_OUT";
		default:
			Log.w(TAG, "DataActivity: " + dataActivity);
			return "system_error";
		}

	}

	public static String getCellNetworkTypeName(int networkType) {

		switch (networkType) {
		case (TelephonyManager.NETWORK_TYPE_1xRTT):
			return "1xRTT";
		case (TelephonyManager.NETWORK_TYPE_CDMA):
			return "CDMA";
		case (TelephonyManager.NETWORK_TYPE_EDGE):
			return "EDGE";
		case (TelephonyManager.NETWORK_TYPE_GPRS):
			return "GPRS";
		case TelephonyManager.NETWORK_TYPE_HSDPA:
			return "HSDPA";
		case TelephonyManager.NETWORK_TYPE_HSPA:
			return "HSPA";
		case TelephonyManager.NETWORK_TYPE_HSUPA:
			return "HSUPA";
		case (TelephonyManager.NETWORK_TYPE_UMTS):
			return "UMTS";
		case (TelephonyManager.NETWORK_TYPE_UNKNOWN):
			return "TYPE_UNKNOWN";
		default:
			Log.w(TAG, "NetworkType: " + networkType);
			return "system_error";
		}

	}

	public static String getCellStateName(int state) {
		switch (state) {
		case ServiceState.STATE_EMERGENCY_ONLY:
			return "STATE_EMERGENCY_ONLY";
		case ServiceState.STATE_IN_SERVICE:
			return "IN_SERVICE";
		case ServiceState.STATE_OUT_OF_SERVICE:
			return "OUT_OF_SERVICE";
		case ServiceState.STATE_POWER_OFF:
			return "POWER_OFF";
		default:
			Log.w(TAG, "state: " + state);
			return "system_error";
		}
	}

	private TelephonyManager phoneManager;

	private TextView dataText;

	private TextView connText;

	private TextView signalText;

	private static final String TAG = "CellStateListener";

	public static String getDataConnectionStateName(int s) {

		switch (s) {
		case TelephonyManager.DATA_CONNECTED:
			return "DATA_CONNECTED";
		case TelephonyManager.DATA_CONNECTING:
			return "DATA_CONNECTING";
		case TelephonyManager.DATA_DISCONNECTED:
			return "DATA_DISCONNECTED";
		case TelephonyManager.DATA_SUSPENDED:
			return "DATA_SUSPENDED";
		default:
			Log.w(TAG, "DataConnectionState: " + s);
			return "system_error";
		}
	}

	private LogWriter dlw = LogWriter.getInstance();

	private String getDataActivityDirectionName(int s) {
		switch (s) {
		case TelephonyManager.DATA_ACTIVITY_DORMANT:
			return "DATA_ACTIVITY_DORMANT";
		case TelephonyManager.DATA_ACTIVITY_IN:
			return "DATA_ACTIVITY_IN";
		case TelephonyManager.DATA_ACTIVITY_INOUT:
			return "DATA_ACTIVITY_INOUT";
		case TelephonyManager.DATA_ACTIVITY_NONE:
			return "DATA_ACTIVITY_NONE";
		case TelephonyManager.DATA_ACTIVITY_OUT:
			return "DATA_ACTIVITY_OUT";
		default:
			Log.w(TAG, "Data activity: " + s);
			return "system_error";
		}
	}

	public void listen(Context context) {
		phoneManager = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		phoneManager.listen(this, PhoneStateListener.LISTEN_CALL_STATE
				| PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
				| PhoneStateListener.LISTEN_DATA_ACTIVITY
				| PhoneStateListener.LISTEN_SERVICE_STATE
				| PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
	}
	
	public void onDataActivity(int direction){
		
		String s=getDataActivityDirectionName(direction);
		Log.d(TAG, "CellDataActivity direction: " + s);
		writeDataActivityToLog(s);
	}

	

	public void onDataConnectionStateChanged(int state, int networktype) {
		String n = getDataConnectionStateName(state);
		StringBuffer b = new StringBuffer();
		Log.d(TAG, "DataConnection state: " + n);
		b.append("DataConnection state: " + n + "\n");
		String type = getCellNetworkTypeName(networktype);
		Log.d(TAG, "DataConnection networktype: " + type);
		b.append("DataConnection networktype: " + type);
		writeDataConnStateToLog(type, n);
		if (dataText != null) {
			dataText.setText(b.toString());
		}
		
	}

	public void onServiceStateChanged(ServiceState serviceState) {
		StringBuffer b = new StringBuffer();
		String state= getCellStateName(serviceState.getState());
		Log.d(TAG, "Cell state: " + state);
		b.append("Cell state: " + state+ "\n");
				
		String type = getCellNetworkTypeName(phoneManager.getNetworkType());

		String activity = getCellDataActivityName(phoneManager
				.getDataActivity());

		Log.d(TAG, "Type: " + type);
		b.append("Type: " + type + "\n");
		Log.d(TAG, "Data activity: " + activity);
		b.append("Data activity: " + activity);

		if (connText != null) {
			connText.setText(b.toString());
		}
		writeServicesStateToLog(state, type, activity);
	}
	
	public void onSignalStrengthsChanged(SignalStrength strength) {
		StringBuffer b = new StringBuffer();
		Log.d(TAG, "CdmaDbm: " + strength.getCdmaDbm());
		b.append("CdmaDbm: " + strength.getCdmaDbm() + "\n");
		Log.d(TAG, "CdmaEcio: " + strength.getCdmaEcio());
		b.append("CdmaEcio: " + strength.getCdmaEcio() + "\n");
		Log.d(TAG, "EvdoDbm: " + strength.getEvdoDbm());
		b.append("EvdoDbm: " + strength.getEvdoDbm() + "\n");
		Log.d(TAG, "EvdoEcio: " + strength.getEvdoEcio());
		b.append("EvdoEcio: " + strength.getEvdoEcio() + "\n");
		Log.d(TAG, "EvdoSnr: " + strength.getEvdoSnr());
		b.append("EvdoSnr: " + strength.getEvdoSnr() + "\n");
		Log.d(TAG, "GsmBitErrorRate: " + strength.getGsmBitErrorRate());
		b.append("GsmBitErrorRate: " + strength.getGsmBitErrorRate() + "\n");
		Log.d(TAG, "GsmSignalStrength: " + strength.getGsmSignalStrength());
		// value of our interest
		b.append("GsmSignalStrength: " + strength.getGsmSignalStrength());
		if (signalText != null) {
			signalText.setText(b.toString());
		}
		writeSignalStrenghtToLog(strength.getGsmSignalStrength());

	}

	public void setConnectionStateTextView(TextView text) {
		this.connText = text;
	}

	public void setDataStateTextView(TextView text) {
		this.dataText = text;
	}

	public void setSignalStrenghtTextView(TextView text) {
		this.signalText = text;
	}

	public void unRegister() {
		phoneManager.listen(this, LISTEN_NONE);
	}

	private void writeDataActivityToLog(String direction) {
		try {
			dlw.open();
		} catch (IOException e) {
			Log.e(TAG, "Error opening file, " + e.getMessage());
			return;
		}

		StringBuffer sb = new StringBuffer();
		sb.append(direction);

		try {
			dlw.writeEvent("cell_data_activity", sb.toString());
			dlw.close();
		} catch (IOException e) {
			Log.e(TAG, "Error writing to file, " + e.getMessage());
		}
	}
	
	private void writeDataConnStateToLog(String type, String state) {
		try {
			dlw.open();
		} catch (IOException e) {
			Log.e(TAG, "Error opening file, " + e.getMessage());
			return;
		}
		StringBuffer sb = new StringBuffer();
		sb.append(type);
		sb.append(",");
		sb.append(state);
		try {
			dlw.writeEvent("cell_data_conn_state", sb.toString());
			dlw.close();
		} catch (IOException e) {
			Log.e(TAG, "Error writing to file, " + e.getMessage());
		}

	}

	private void writeServicesStateToLog(String state, String type, String activity) {
		try {
			dlw.open();
		} catch (IOException e) {
			Log.e(TAG, "Error opening file, " + e.getMessage());
			return;
		}

		StringBuffer sb = new StringBuffer();
		sb.append(type);
		sb.append(",");
		sb.append(state);
		sb.append(",");
		sb.append(activity);

		try {
			dlw.writeEvent("cell_services_state", sb.toString());
			dlw.close();
		} catch (IOException e) {
			Log.e(TAG, "Error writing to file, " + e.getMessage());
		}
	}

	private void writeSignalStrenghtToLog(int signal) {
		try {
			dlw.open();
		} catch (IOException e) {
			Log.e(TAG, "Error opening file, " + e.getMessage());
			return;
		}
		try {
			dlw.writeEvent("cell_signal", signal + "");
			dlw.close();
		} catch (IOException e) {
			Log.e(TAG, "Error writing to file, " + e.getMessage());
		}

	}

}
