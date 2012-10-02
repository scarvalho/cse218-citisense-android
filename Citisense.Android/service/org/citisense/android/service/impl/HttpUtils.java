package org.citisense.android.service.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URLEncoder;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

public class HttpUtils {

	public static String getBitlyRedirectUrlForMap(long startTime, long endTime) throws Exception {
		ApplicationSettings settings = ApplicationSettings.instance();
		
		// Request approval for this public map posting
		String approvalUrl = settings.serverApprovalMapUrlRoot()
				+ settings.getPhoneID() + "/"
				+ startTime + "-" + endTime;
		
		if(getApprovalForMap(approvalUrl)) {
			String mapsUrlForThisPhone = settings.serverRequestMapUrlRoot()
					+ settings.getPhoneID() + "/"
					+ startTime + "-" + endTime;
			return getBitlyRedirectUrlForMap(mapsUrlForThisPhone);
		} else {
			return "Failed to get server approval...";
		}
	}
	
	private static boolean getApprovalForMap(String url) {
		try {
			HttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet();
			request.setURI(new URI(url));
			HttpResponse response = client.execute(request);
			StatusLine statusLine = response.getStatusLine();
			if(200 == statusLine.getStatusCode()) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
	}

	private static String getBitlyRedirectUrlForMap(String mapUrl)
			throws Exception {
		ApplicationSettings settings = ApplicationSettings.instance();
		String bitlyQueryCommon = "http://api.bitly.com/v3/shorten?login="
				+ settings.bitlyUser() + "&apiKey=" + settings.bitlyAPIkey()
				+ "&format=txt&longUrl=";
		String mapsUrlQuery = URLEncoder.encode(mapUrl, settings
				.bitlyQueryEncoding());
		HttpParams httpParameters = new BasicHttpParams();
		// Set the timeout in milliseconds until a connection is established.
		HttpConnectionParams.setConnectionTimeout(httpParameters, 3000);
		// Set the default socket timeout (SO_TIMEOUT)
		// in milliseconds which is the timeout for waiting for data.
		HttpConnectionParams.setSoTimeout(httpParameters, 3000);
		// Create a new HttpClient and Post Header
		DefaultHttpClient httpclient = new DefaultHttpClient(httpParameters);
		String bitlyQuery = bitlyQueryCommon + mapsUrlQuery;
		HttpGet bitlyGet = new HttpGet(bitlyQuery);

		HttpResponse response = httpclient.execute(bitlyGet);

		StatusLine statusLine = response.getStatusLine();
		// System.out.println(statusLine);
		if (200 == statusLine.getStatusCode()) {
			// successful request
			return streamToString(response.getEntity().getContent());
		} else {
			throw new Exception(
					"Unsuccessful request to bit.ly with status code: "
							+ statusLine.getStatusCode());
		}
	}

	private static String streamToString(InputStream stream) throws IOException {
		if (stream != null) {
			Writer writer = new StringWriter();

			char[] buffer = new char[1024];
			try {
				Reader reader = new BufferedReader(new InputStreamReader(
						stream, "UTF-8"));
				int n;
				while ((n = reader.read(buffer)) != -1) {
					writer.write(buffer, 0, n);
				}
			} finally {
				stream.close();
			}
			return writer.toString();
		} else {
			return "";
		}
	}
}
