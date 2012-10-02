package org.citisense.datastructure.impl;

import java.text.DecimalFormat;
import java.text.ParseException;

import org.citisense.datastructure.Location;

public class LocationImpl<T> implements Location<T> {

	private static final long serialVersionUID = -6177272453187727268L;

	private final double latitude, longitude, altitude;
	private final long time;
	private final T extaInformation;
	private String provider;
	private float accuracy;

	private static final DecimalFormat locationFormat = new DecimalFormat("###.#######");

	public LocationImpl(double latitude, double longitude, double altitude,
			long time, String provider, float accuracy, T extraInformation) {
		this.latitude = Double.parseDouble(locationFormat.format(latitude));
		this.longitude = Double.parseDouble(locationFormat.format(longitude));
		this.altitude = Double.parseDouble(locationFormat.format(altitude));
		this.time = time;
		this.extaInformation = extraInformation;
		this.provider = provider;
		this.accuracy = Float.parseFloat(locationFormat.format(accuracy));
		if(this.accuracy > 999)
			this.accuracy = 999;
	}

	public LocationImpl(double latitude, double longitude, double altitude,
			long time, String provider, float accuracy) {
		this(latitude, longitude, altitude, time, provider, accuracy, null);
	}

	@Override
	public double getAltitude() {
		return altitude;
	}

	@Override
	public double getLatitude() {
		return latitude;
	}

	@Override
	public double getLongitude() {
		return longitude;
	}

	@Override
	public long getTime() {
		return time;
	}

	@Override
	public T getExtraInformation() {
		return extaInformation;
	}

	public String getProvider() {
		return provider;
	}

	public float getAccuracy() {
		return accuracy;
	}

	@Override
	public String toString() {
		return "lat: " + latitude + ", lon: " + longitude + ", alt: "
				+ altitude + ", at: " + time + ", pro: " + provider + ", acc: "
				+ accuracy;
	}

	/**
	 * Returns a Location represented by the given String {@code
	 * locationAsString}.<br/> {@code locationAsString} should be in the format of
	 * the String returned by {@link #toString()} of this class.
	 * 
	 * @param locationAsString
	 * @return
	 * @throws ParseException
	 */
	public static <T> Location<T> fromString(String locationAsString)
			throws ParseException {
		int numExpectedFields = 6;
		String[] splitted = locationAsString.split(",");
		if (splitted.length != numExpectedFields) {
			throw new ParseException("Cannot parse String '" + locationAsString
					+ "', expected " + numExpectedFields
					+ " comma separated parameters, found " + splitted.length,
					0);
		}

		double lat = Double.parseDouble(splitted[0].substring(5));
		double lon = Double.parseDouble(splitted[1].substring(6));
		double alt = Double.parseDouble(splitted[2].substring(6));
		long time = Long.parseLong(splitted[3].substring(5));
		String prov = splitted[4].substring(6);
		float acc = Float.parseFloat(splitted[5].substring(6));

		return new LocationImpl<T>(lat, lon, alt, time, prov, acc);
	}

	// public static void main(String[] args) throws ParseException {
	// Location<Object> location = new LocationImpl<Object>(123.45D, 11.12D,
	// 3.412D, 123123123123L, "gps", 0.90f, new Object());
	// System.out.println(fromString(location.toString()));
	// }

	public T getExtaInformation() {
		return extaInformation;
	}
}
