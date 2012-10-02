package org.citisense.android.service.impl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.citisense.android.CustomExceptionHandler;
import org.citisense.utils.thread.DaemonThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sosa.richservice.ServiceDescriptorLocal;
import org.sosa.richservice.base.MessageNotificationBase;
import org.sosa.richservice.base.ServiceDescriptorLocalBase;
import org.sosa.richservice.base.servicedataconnector.ServiceDataConnectorJavaLocal;
import org.sosa.richservice.utils.richservice.RichServiceUtils;

public class ConnectivityNotificationService {
	private static Logger logger = LoggerFactory
			.getLogger(ConnectivityNotificationService.class);

	private final ServiceDataConnectorJavaLocal connector;
	private final ServiceDescriptorLocal descriptor;
	private final ExecutorService executor;
	private static final ApplicationSettings applicationSettings = ApplicationSettings
			.instance();

	public ConnectivityNotificationService() {
		connector = new ServiceDataConnectorJavaLocal();
		descriptor = new ServiceDescriptorLocalBase(
				"ConnectivityNotificationService", (Class<?>) null, null);
		connector.setService(descriptor);

		executor = Executors.newSingleThreadExecutor(new DaemonThreadFactory(
				ConnectivityNotificationService.class.getSimpleName()
						+ "-Thread"));
		//this.start();
	}

	public ServiceDataConnectorJavaLocal getServiceDataConnector() {
		return connector;
	}

	public void start() {
		// TODO: Check to make sure you don't run it again on subsequent calls
		executor.execute(new Runnable() {

			@Override
			public void run() {
				// Try to catch an unhandled exception in the main service thread
				Thread.currentThread().setUncaughtExceptionHandler(new CustomExceptionHandler());
				while (!Thread.currentThread().isInterrupted()) {
					try {
						Thread.sleep(applicationSettings
								.connectivityCheckFrequency());
					} catch (InterruptedException e) {
						// TODO: log
						return;
					}

					boolean canConnect = canConnect();
					connector.sendMessage(new MessageNotificationBase(
							descriptor.getServiceName(), RichServiceUtils
									.generateMessageId(), "connectivity",
							canConnect ? "on" : "off"));
				}
			}

		});
	}

	// private boolean canConnect() {
	// TcpClient client = null;
	// try {
	// client = new TcpClient(applicationSettings.serverHost(),
	// applicationSettings.serverPort(), applicationSettings
	// .timeoutConnect());
	// return true;
	// } catch (UnknownHostException e) {
	// // Log in trace
	// return false;
	// } catch (IOException e) {
	// // Log in trace
	// return false;
	// } finally {
	// if (client != null) {
	// client.close();
	// }
	// }
	// }

	public static boolean canConnect() {
		HttpParams httpParameters = new BasicHttpParams();
		// Set the timeout in milliseconds until a connection is established.
		HttpConnectionParams.setConnectionTimeout(httpParameters,
				applicationSettings.timeoutConnect());
		// Set the default socket timeout (SO_TIMEOUT)
		// in milliseconds which is the timeout for waiting for data.
		HttpConnectionParams.setSoTimeout(httpParameters, applicationSettings
				.timeoutOperation());
		// Create a new HttpClient and Post Header
		DefaultHttpClient httpclient = new DefaultHttpClient(httpParameters);
		HttpGet heartbeat = new HttpGet(applicationSettings
				.serverHeartBeatUri());
		// Execute HTTP Post Request
		try {
			HttpResponse response = httpclient.execute(heartbeat);
			if (response.getStatusLine().getStatusCode() == 200) {
				if (AppLogger.isDebugEnabled(logger))
					logger.debug("Server heartbeat returned HTTP 200. Connectivity is ON");
				return true;
			}
			// } catch (IOException e) {
			// if (e instanceof ConnectTimeoutException
			// || e instanceof SocketTimeoutException) {
			// logger.debug("{}. Assuming this means connectivity is off", e
			// .getClass().getSimpleName());
			// logger.trace("", e);
			// return false;
			// } else {
			// logger
			// .debug("Unknown IOException. Assuming this means connectivity is off");
			// logger.trace("", e);
			// return false;
			// }
		} catch (Exception e2) {
			if (AppLogger.isDebugEnabled(logger))
				logger.debug("Heartbeat exception. Connectivity is OFF");
			logger.trace("", e2);
		}
		// If we are here, some exception occurred, or HTTP 200 was not sent.
		return false;
	}

	public void stop() {
		this.executor.shutdownNow();
	}
}
