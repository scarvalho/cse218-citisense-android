package org.citisense.android;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;

import org.citisense.android.service.impl.AppLogger;
import org.citisense.android.service.impl.ApplicationSettings;
import org.citisense.datastructure.SensorReading;
import org.citisense.datastructure.SensorType;
import org.citisense.datastructure.impl.LocationImpl;
import org.citisense.datastructure.impl.SensorReadingImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.location.Location;
import android.location.LocationManager;

public class AirDataReader {

	private static final Logger logger = LoggerFactory
			.getLogger(AirDataReader.class);

	private Location currentLocation = new Location(
			LocationManager.GPS_PROVIDER);
	private String closestLocationName = "DOWNTOWN";
	private Location closestLocation = new Location(
			LocationManager.GPS_PROVIDER);

	private enum AirDataParserStates {
		LookingForType, LookingForLocation,
	}

	private static final Hashtable<String, Double[]> availLocations = new Hashtable<String, Double[]>();
	static {
		// Create the hashtable of recognized locations
		availLocations.put("ALPINE", new Double[] { 32.835052, -116.766411 });
		availLocations.put("CHULA_VI", new Double[] { 32.640054, -117.084195 });
		availLocations.put("DEL_MAR", new Double[] { 32.959489, -117.265315 });
		availLocations.put("DOWNTOWN", new Double[] { 32.715329, -117.157255 });
		availLocations.put("EL_CAJON", new Double[] { 32.794773, -116.962527 });
		availLocations
				.put("ESCONDIDO", new Double[] { 33.119207, -117.086421 });
		availLocations.put("OTAY_MES", new Double[] { 32.563353, -116.979355 });
		availLocations.put("OVERLAND", new Double[] { 33.674111, -117.809837 });
		availLocations.put("PENDLETON", new Double[] { 33.36434, -117.408649 });
		availLocations.put("SAN_MARCOS",
				new Double[] { 33.143372, -117.166145 });
	}
	private static final Map<String, String> VarMap = new HashMap<String, String>();
	static {
		VarMap.put("01_OZONE", SensorType.O3.toString());
		VarMap.put("06_CO", SensorType.CO.toString());
		VarMap.put("04_NO", SensorType.NO2.toString());
		VarMap.put("16_EXTMP", SensorType.TEMP.toString());
		VarMap.put("17_HUMI", SensorType.HUMD.toString());
	}

	private static final HashMap<SensorType, String> UnitsMap = new HashMap<SensorType, String>();
	static {
		UnitsMap.put(SensorType.O3, "PPM");
		UnitsMap.put(SensorType.CO, "PPM");
		UnitsMap.put(SensorType.NO2, "PPM");
		UnitsMap.put(SensorType.TEMP, "F");
		UnitsMap.put(SensorType.HUMD, "PERC");
	}

	private static final String[] UNITS = { "PPM", "DEGC", "DEGF", "UG/M3",
			"PERC" };

	public Collection<SensorReading> getReading() throws MalformedURLException,
			IOException {
		Calendar date = Calendar.getInstance();
		return getReading(date, this.closestLocationName);
	}

	private Collection<SensorReading> getReading(Calendar cal, String location)
			throws MalformedURLException, IOException {

		Collection<SensorReading> readings = new ArrayList<SensorReading>();

		BufferedReader reader = this.getDataReader(cal);

		String line;

		int hour = cal.get(Calendar.HOUR_OF_DAY) - 1;

		if (AppLogger.isInfoEnabled(logger)) logger.info("Looking for" + " location: " + location + " hour: " + hour);

		AirDataParserStates state = AirDataParserStates.LookingForType;
		String type = null;
		int lineNumber = 0;
		while ((line = reader.readLine()) != null) {
			++lineNumber;
			try {
				switch (state) {
				case LookingForType:
					String typeFromLine = line.trim().split(" ")[0];
					if (IsTypeLine(line) && VarMap.containsKey(typeFromLine)) {
						type = VarMap.get(typeFromLine);
						if (AppLogger.isDebugEnabled(logger)) {
							logger.debug("Type: " + type);
						}
						state = AirDataParserStates.LookingForLocation;
					}
					continue;
				case LookingForLocation:
					if (line.length() == 0) {
						if (AppLogger.isDebugEnabled(logger)) {
							logger.debug("Blank line, didn't find entry, NEXT!");
						}
						state = AirDataParserStates.LookingForType;
						continue;
					}
					StringTokenizer st = new StringTokenizer(line);
					String lineLocation = st.nextToken();
					if (lineLocation.equals(location)) {
						// Log.i("Aqi_ui_detailed", "Looking for hour : " +
						// line);
						state = AirDataParserStates.LookingForType;
						String[] hourReadings = new String[24];
						int index = 0;
						while (index < 24) {
							hourReadings[index++] = st.nextToken();
						}
						Double hourValue = Double.NaN;
						for (int hourIndex = hour; hourIndex >= 0; --hourIndex) {
							try {
								hourValue = Double
										.parseDouble(hourReadings[hourIndex]);
							} catch (NumberFormatException nfe) {
								continue;
							}
							if (hourValue != Double.NaN) {
								if (AppLogger.isDebugEnabled(logger)) {
									logger.debug("Found Reading - line #"
											+ lineNumber + ": " + line);
									logger.debug("Found Reading: " + type + " "
											+ +hourValue);
								}
								SensorType sensorType = SensorType
										.valueOf(type);
								String units = UnitsMap.containsKey(sensorType) ? UnitsMap
										.get(sensorType)
										: "?";
								if (AppLogger.isTraceEnabled(logger)) {
									logger.trace("Original location : "
											+ currentLocation.getLatitude()
											+ ","
											+ currentLocation.getLongitude());
								}
								LocationImpl<Object> loc = AndroidLocationImpl
										.convertFromAndroidLocation(
												currentLocation, null);
								if (AppLogger.isTraceEnabled(logger)) {
									logger.trace("Converted location : "
											+ loc.getLatitude() + ","
											+ loc.getLongitude());
								}
								// populate the hour, assume oldest possible -->
								// start of the hour
								cal.set(Calendar.HOUR_OF_DAY, hourIndex);
								cal.set(Calendar.MINUTE, 0);
								cal.set(Calendar.SECOND, 0);
								cal.set(Calendar.MILLISECOND, 0);
								SensorReading reading = new SensorReadingImpl(
										sensorType, hourValue,
										units, cal.getTimeInMillis(), loc);
								readings.add(reading);
								// bypass the break if we want all readings so far today, not just most recent
								// TODO: make sure this doesn't break anything
								break;
							}
						}
					}
					continue;
				}
			} catch (Exception ex) {
				if(AppLogger.isErrorEnabled(logger))
					logger.error(ex.getMessage());
				continue;
			}
		}
 
		if (AppLogger.isInfoEnabled(logger)) {
			logger.info("Found " + readings.size()
					+ " Readings from SD service");
		}

		return readings;
	}

	private boolean IsTypeLine(String line) {
		boolean isType = false;
		for (String unit : UNITS) {
			if (line.contains(unit))
				isType = true;
		}
		return isType;
	}

	// private final static String[] VARIABLES = {
	// "01_OZONE ",
	// "02_NOX ",
	// "03_NO2 ",
	// "04_NO ",
	// "05_S02 ",
	// "06_CO ",
	// "10_INTMP",
	// //"11_PM2_2_5",
	// "11_PM_2_5",
	// //"12_WDR",
	// //"13_SIGMA",
	// //"14_WSR",
	// //"15_WSA",
	// "16_EXTMP",
	// "17_HUMI",
	// "18_BAROM",
	// //"19_SORAD"
	// };

	private BufferedReader getDataReader(Calendar date)
			throws MalformedURLException, IOException, FileNotFoundException {

		URL url = new URL(ApplicationSettings.instance()
				.sanDiegoPollutionWebSite()
		// "http://jtimmer.cts.com/20110308.txt"
		);

		BufferedReader reader = new BufferedReader(new InputStreamReader(url
				.openStream()), ApplicationSettings.instance()
				.AVG_SDAPC_WEB_REPORT_LENGTH());

		return reader;
	}

	public void updateClosestLocation(Location location) {
		if (AppLogger.isDebugEnabled(logger)) logger.debug("Update location " + location.getLatitude() + "," + location.getLongitude());
		if (location != null) {
			// Update latitude and longitude for current location
			currentLocation.setLatitude(location.getLatitude());
			currentLocation.setLongitude(location.getLongitude());
			if (AppLogger.isInfoEnabled(logger)) logger.info("New location: " + location.toString());
			// Iterate through all the locations we have data for
			// and find the closest area to display
			Enumeration<String> e = availLocations.keys();
			float closestDist = Float.MAX_VALUE;
			while (e.hasMoreElements()) {
				String areaName = e.nextElement();
				Double[] coord = availLocations.get(areaName);
				Location loc = new Location(LocationManager.NETWORK_PROVIDER);
				loc.setLatitude(coord[0]);
				loc.setLongitude(coord[1]);

				float distanceBetw = currentLocation.distanceTo(loc);
				if (distanceBetw < closestDist) {
					closestLocationName = areaName;
					closestLocation = loc;
					closestDist = distanceBetw;
				}
			} // end while
		}
	}

}
