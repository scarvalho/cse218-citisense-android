package org.citisense.android.servicedataconnector;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Deflater;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.citisense.android.service.impl.AppLogger;
import org.citisense.android.service.impl.ApplicationSettings;
import org.citisense.datastructure.SensorReading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sosa.richservice.Message;
import org.sosa.richservice.MessageBus;
import org.sosa.richservice.MessageRequest;
import org.sosa.richservice.MessageResponse;
import org.sosa.richservice.ServiceDataConnectorForRichService;
import org.sosa.richservice.ServiceDescriptor;
import org.sosa.richservice.base.MessageErrorBase;
import org.sosa.richservice.base.MessageResponseBase;
import org.sosa.richservice.utils.richservice.RichServiceUtils;

/**
 * 
 * @author celal.ziftci
 * 
 */
public class ServiceDataConnectorForRichServiceHTTPClient implements
		ServiceDataConnectorForRichService<ServiceDescriptor> {

	private Logger logger = LoggerFactory
			.getLogger(ServiceDataConnectorForRichServiceHTTPClient.class);

	private MessageBus bus;
	private ServiceDescriptor externalServiceDescriptor;
	private final int connectTimeout, operationTimeout;
	private String serverRequestUri;

	public ServiceDataConnectorForRichServiceHTTPClient(
			ServiceDescriptor externalServiceDescriptor, String serverRequestUri) {
		this(externalServiceDescriptor, serverRequestUri, 0, 0);
	}

	public ServiceDataConnectorForRichServiceHTTPClient(
			ServiceDescriptor externalServiceDescriptor,
			String serverRequestUri, int connectTimeout, int operationTimeout) {
		this.externalServiceDescriptor = externalServiceDescriptor;
		this.serverRequestUri = serverRequestUri;
		this.connectTimeout = connectTimeout;
		this.operationTimeout = operationTimeout;
	}

	@Override
	public void sendMessage(Message request) {
		// Should never be called...
	}

	@Override
	public Object sendMessage(Message message, int timeout) {
		// Should never be called...
		return null;
	}
	
	public void getRegionUpdate() throws Exception {
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
		String updateUri = ApplicationSettings.instance().serverUpdateRegionsUri() 
				+ ApplicationSettings.instance().getPhoneID() + "/";
		HttpGet httpget = new HttpGet(updateUri);
		
		// Execute HTTP Get Request
		if (AppLogger.isDebugEnabled(logger)) logger.debug("Getting HTTP: " + updateUri);
		HttpResponse response = httpclient.execute(httpget);
		StatusLine statusLine = response.getStatusLine();
		if (200 == statusLine.getStatusCode()) {
			return;
		} else {
			if (AppLogger.isErrorEnabled(logger)) {
				logger.error("Error getting update-region with status code '"
						+ statusLine.getStatusCode() + "'");
			}
			throw new Exception("Error getting update-region with status line '"
					+ statusLine + "'");
		}
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
		nameValuePairs.add(new BasicNameValuePair("sensor_data", sensorData));
		httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

		if (AppLogger.isInfoEnabled(logger)) {
			logger.info("Posting sensor data to the backend: " + sensorData);
		}

		// Execute HTTP Post Request
		if (AppLogger.isDebugEnabled(logger)) logger.debug("Posting HTTP: " + httppost.getURI());
		HttpResponse response = httpclient.execute(httppost);
		StatusLine statusLine = response.getStatusLine();
		if (200 == statusLine.getStatusCode()) {
			return;
		} else {
			if (AppLogger.isErrorEnabled(logger)) {
				logger.error("Error posting data with status code '"
						+ statusLine.getStatusCode() + "'");
			}
			throw new Exception("Error posting data with status line '"
					+ statusLine + "'");
		}
	}

	@Override
	public void receiveMessage(Message msg) {
		if (msg instanceof MessageRequest) {
			MessageRequest request = (MessageRequest) msg;
			// String operation = request.getOperation();
			// Class[] paramTypes = request.getOperationParameterTypes();
			// Class requestedInterface = findServiceInterface(request);

			try {
				// FIXME: We should check what the request is. Right now, I
				// always assume this is a store request!
				// MessageRequest newRequest = new MessageRequestBase(
				// externalServiceDescriptor.getServiceName(),
				// requestedInterface.getName(), msg.getMessageId(),
				// ((MessageRequest) msg).getOperation(),
				// ((MessageRequest) msg).getOperationParameterTypes(),
				// ((MessageRequest) msg).getOperationParameterValues());
				if (request.getOperation().equals("newObservation")) {
					Object[] values = request.getOperationParameterValues();

					// String sensorID = (String) values[2];
					SensorReading[] observations = (SensorReading[]) values[3];

					StringBuffer buffer = new StringBuffer(ApplicationSettings
							.instance().AVG_SENSOR_READING_PAYLOAD_LENGTH()
							* observations.length);
					for (SensorReading observe : observations) {
						// Only upload readings with non-fake locations
						if(!observe.getLocation().getProvider().equals("fake")) {
							buffer
							.append(
									payloadFromSensorReading(observe))
							.append("||");
						}
					}
					// Remove the extra '||' at the end of the string
					int bufferLength = buffer.length();
					if(bufferLength > 1) {
						buffer.delete(bufferLength - 2, bufferLength);
						bufferLength = bufferLength - 2;
					}
					
					byte[] bufferBytes = buffer.toString().getBytes("UTF-8");
					// Compress the data
					String compressedString = compressData(bufferBytes);
					
					// Send them
					postSensorData(compressedString);
					// If nothing was thrown, it was ok...
					MessageResponse responseFromUp = new MessageResponseBase(
							externalServiceDescriptor.getServiceName(), msg
									.getSource(), RichServiceUtils
									.generateMessageId(), msg.getMessageId(),
							observations.length);

					// Re-write the message so that now it looks like it is
					// being
					// responded by this service
					bus.deliverMessage(new MessageResponseBase(
							externalServiceDescriptor.getServiceName(), msg
									.getSource(),
							responseFromUp.getMessageId(), msg.getMessageId(),
							responseFromUp.getResponse()));
				}
				
				else if (request.getOperation().equals("endObservation")) {
					getRegionUpdate();
					// If nothing was thrown, it was ok...
					MessageResponse responseFromUp = new MessageResponseBase(
							externalServiceDescriptor.getServiceName(), msg
									.getSource(), RichServiceUtils
									.generateMessageId(), msg.getMessageId(),
							null);

					// Re-write the message so that now it looks like it is
					// being
					// responded by this service
					bus.deliverMessage(new MessageResponseBase(
							externalServiceDescriptor.getServiceName(), msg
									.getSource(),
							responseFromUp.getMessageId(), msg.getMessageId(),
							responseFromUp.getResponse()));
				}
				
			} catch (UnknownHostException e) {
				bus.deliverMessage(new MessageErrorBase(
						externalServiceDescriptor.getServiceName(), msg
								.getSource(), RichServiceUtils
								.generateMessageId(), msg.getMessageId(),
						"Cannot connect to the url '" + serverRequestUri
								+ "', it is unknown", e));
			} catch (IOException e) {
				bus.deliverMessage(new MessageErrorBase(
						externalServiceDescriptor.getServiceName(), msg
								.getSource(), RichServiceUtils
								.generateMessageId(), msg.getMessageId(),
						"Connection cannot be opened or it is broken", e));
			} catch (Throwable e) {
				if (AppLogger.isErrorEnabled(logger)) {
					logger
							.error("Error posting sensor data. Return error response for the request.");
				}
				bus.deliverMessage(new MessageErrorBase(
						externalServiceDescriptor.getServiceName(), msg
								.getSource(), RichServiceUtils
								.generateMessageId(), msg.getMessageId(),
						"Unknown exception", e));
			} finally {
			}
		}
		// FIXME: This connector does not expose any interfaces to the upper
		// world yet. If it does one day, we will think about it...
	}
	
	// Compress the input string using zlib and return it as base64 string
	private String compressData(byte[] input) {
		Deflater deflater = new Deflater();
		deflater.setInput(input);
		deflater.finish();
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream(input.length);
		byte[] buf = new byte[1024];
		while(!deflater.finished()) {
			int compByte = deflater.deflate(buf);
			byteStream.write(buf, 0, compByte);
		}
		try {
			byteStream.close();
		} catch (IOException e) {
			if(AppLogger.isErrorEnabled(logger)) {
				logger.error("Error closing byte stream during compression for upload");
			}
		}

		return Base64.encodeBytes(byteStream.toByteArray());
		
	}

	/*
	 * private Class findServiceInterface(MessageRequest request) { Class iface
	 * = null; Collection<Class> interfaces =
	 * this.getService().getExposedInterface();
	 * 
	 * for (Class cls : interfaces) { try { Method method =
	 * cls.getMethod(request.getOperation(), request
	 * .getOperationParameterTypes()); iface = cls; } catch (Exception e) { //
	 * FIXME: Make sure this is smth like nosuchmethodexception or // security
	 * exception. Other ones are legit } } return iface; }
	 */

	@Override
	public ServiceDescriptor getService() {
		return externalServiceDescriptor;
	}

	@Override
	public void setService(ServiceDescriptor service) {
		this.externalServiceDescriptor = service;
	}

	@Override
	public MessageBus getMessageBus() {
		return bus;
	}

	@Override
	public void setMessageBus(MessageBus bus) {
		this.bus = bus;
	}

	/**
	 * Given a reading, creates the payload to be sent to the backend CI. <br/>
	 * The payload looks like this:
	 * 
	 * <pre>
	 * phone-id,sensor-type(CO, HUMD etc.),reading,date-sampled,lat,lon,alt,source(gps,wifi etc.),accuracy
	 * </pre>
	 * 
	 * @param reading
	 * @return
	 */
	private static String payloadFromSensorReading(SensorReading reading) {
		// return reading.getTimeDateAsString() + "," + reading.getSensorType()
		// + "," + reading.getSensorData() + ","
		// + reading.getSensorUnits() + ","
		// + reading.getLocation().getLatitude() + ","
		// + reading.getLocation().getLongitude() + ","
		// + reading.getLocation().getAltitude() + ","
		// + reading.getLocation().getProvider() + ","
		// + reading.getLocation().getAccuracy();

		// phone-id, sensor-type, reading, date-sampled, lat, lon, alt, source,
		// accuracy
		return ApplicationSettings.instance().getPhoneID() + ","
				+ reading.getSensorType() + "," + reading.getSensorData() + ","
				//+ reading.getTimeDateAsString() + ","
				+ reading.getTimeSeconds() + ","
				+ reading.getLocation().getLatitude() + ","
				+ reading.getLocation().getLongitude() + ","
				+ reading.getLocation().getAltitude() + ","
				+ reading.getLocation().getProvider() + ","
				+ reading.getLocation().getAccuracy();
	}
}
