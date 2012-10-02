package org.citisense.android.profiler.log;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Deflater;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;

public class LogUploader {

	private final String TAG = "LogSender";

	private URI serverRequestUri;
	private int connectTimeout;
	private int operationTimeout;

	public LogUploader(Context context) {
		TelephonyManager cellphone = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		String deviceId = cellphone.getDeviceId();
		try {
			serverRequestUri = new URI("http://citisenseci.ucsd.edu/logs/add-compressed/"+ deviceId + "/");
			//serverRequestUri = new URI("http://citisenseci.ucsd.edu/logs/add/"+ deviceId + "/");
		} catch (URISyntaxException e) {
			Log.e(TAG, "Error creating URI", e);
		}
	}

	// Compress the input string using zlib and return it as base64 string
	public String compressData(byte[] input) {
		Deflater deflater = new Deflater();
		deflater.setInput(input);
		deflater.finish();
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream(
				input.length);
		byte[] buf = new byte[1024];
		while (!deflater.finished()) {
			int compByte = deflater.deflate(buf);
			byteStream.write(buf, 0, compByte);
		}
		try {
			byteStream.close();
		} catch (IOException e) {

			Log.e(TAG,
					"Error closing byte stream during compression for upload",
					e);

		}

		return Base64.encodeToString(byteStream.toByteArray(), Base64.DEFAULT);

	}

	public void postSensorData(String sensorData) throws Exception {
		HttpParams httpParameters = new BasicHttpParams();
		// Set the timeout in milliseconds until a connection is established.
		HttpConnectionParams.setConnectionTimeout(httpParameters,
				this.connectTimeout);
		// Set the default socket timeout (SO_TIMEOUT)
		// in milliseconds which is the timeout for waiting for data.
		HttpConnectionParams
				.setSoTimeout(httpParameters, this.operationTimeout);
		// Create a new HttpClient and Post Header
		DefaultHttpClient httpclient = new DefaultHttpClient(httpParameters);
		HttpPost httppost = new HttpPost(serverRequestUri);
		// Add your data
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		nameValuePairs.add(new BasicNameValuePair("logs", sensorData));
		httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

		Log.i(TAG, "Posting sensor data to the backend: " + sensorData);

		// Execute HTTP Post Request
		Log.d(TAG, "Posting HTTP: " + httppost.getURI());
		HttpResponse response = httpclient.execute(httppost);
		StatusLine statusLine = response.getStatusLine();
		if (200 == statusLine.getStatusCode()) {
			Log.d(TAG,
					"Success posting data with status code '"
							+ statusLine.getStatusCode() + "'");
			return;
		} else {

			Log.e(TAG,
					"Error posting data with status code '"
							+ statusLine.getStatusCode() + "'");

			throw new Exception("Error posting data with status line '"
					+ statusLine + "'");
		}
	}

}
