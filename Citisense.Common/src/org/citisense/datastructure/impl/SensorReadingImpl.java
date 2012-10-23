package org.citisense.datastructure.impl;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.citisense.datastructure.Location;
import org.citisense.datastructure.SensorReading;
import org.citisense.datastructure.SensorType;

public class SensorReadingImpl implements SensorReading {
	private static final long serialVersionUID = -559013932783057060L;

	// TODO move this to a better place
	private static final SimpleDateFormat dateFormatter = new SimpleDateFormat(
			"M/d/yyyy h:mm:ss a");

	private SensorType sensorType;
	private double data;
	private String dataString;
	private String units;
	private Date timeDate;
	private long timeLong;
	private Location<Object> location;
	
	private static final DecimalFormat readingFormat = new DecimalFormat("###.##");

	public SensorReadingImpl(SensorType sensorType, double data, String units,
			long timeLong, Location<Object> location) {
		this.sensorType = sensorType;
		this.dataString = readingFormat.format(data).toString();
		this.data = Double.valueOf(dataString);
		this.units = units;
		this.timeLong = timeLong;
		this.timeDate = null;
		this.location = location;
	}

//	public SensorReadingImpl(SensorType sensorType, String data, String units,
//			String timeDate, Location location) {
//		this.sensorType = sensorType;
//		this.data = readingFormat.format(Double.parseDouble(data));
//		this.units = units;
//		try {
//			this.timeLong = dateFormatter.parse(timeDate).getTime();
//		} catch (ParseException e) {
//			throw new RuntimeException(
//					"Cannot parse date string with simple date format '"
//							+ timeDate + "'");
//		}
//		this.location = location;
//	}

	public SensorType getSensorType() {
		return sensorType;
	}

	public String getSensorUnits() {
		return units;
	}

	public double getSensorData() {
		return data;
	}
	
	public String getSensorDataString() {
		return dataString;
	}
	
	public Date getTimeDate() {
		if(this.timeDate == null)
			this.timeDate = new Date(timeLong);
		return this.timeDate;
	}

	public long getTimeMilliseconds() {
		return timeLong;
	}
	
	public long getTimeSeconds() {
		return timeLong / 1000;
	}

	public String getTimeDateAsString() {
		if(this.timeDate == null)
			this.timeDate = new Date(timeLong);
		return dateFormatter.format(this.timeDate);
	}

	public Location<Object> getLocation() {
		return location;
	}

	@Override
	public String toString() {
		return "Sensor type: " + sensorType.name() + ", data: " + data
				+ ", units: " + units + ", timeDate: "
				+ this.getTimeDateAsString() + ", location: " + location;
	}

	public static String formatCalendarString(Date date) {
		return dateFormatter.format(date);
	}
	
	public static void main(String[] args) throws ParseException {
		Date d = dateFormatter.parse("4/21/2011 7:15:43 PM");
		System.out.println(d);
	}
	
	public void setPollutantType(String mainPollutant)
	{
		// Hack
		// AQI has a different loc form: the offending pollutant is stored in 'source'
		Location<Object> aqiLoc = new LocationImpl<Object>(location.getLatitude(), location.getLongitude(), 
				location.getAltitude(), timeLong, mainPollutant, 0, location.getExtraInformation());
		location = aqiLoc;
	}
	
	public SensorType getPollutantType()
	{
		// Hack
		// For AQI, main pollutant is stashed in location.getProvider();
		if (location.getProvider() == "fake")
		{
			return null;
		}
		else
		{
			return SensorType.valueOf(location.getProvider());
		}
		
	}
}