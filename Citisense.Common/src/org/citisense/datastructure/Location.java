package org.citisense.datastructure;

import java.io.Serializable;

public interface Location<T> extends Serializable {

	public static final Location<Object> UNKNOWN = null;
	public static final double UNDEFINED_LATITUDE = Double.NaN;
	public static final double UNDEFINED_LONGITUDE = Double.NaN;
	public static final double UNDEFINED_ALTITUDE = Double.NaN;
	public static final long UNDEFINED_TIME = Long.MIN_VALUE;
	public static final String UNDEFINED_SOURCE = null;
	public static final float UNDEFINED_ACCURACY = Float.NaN;

	public double getLatitude();

	public double getLongitude();

	public double getAltitude();

	public long getTime();
	
	public T getExtraInformation();
	
	public String getProvider();
	
	public float getAccuracy();
}
