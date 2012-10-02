package org.citisense.android;

import android.graphics.Color;

/**
 * AQILevel represents an AQI level specified by the EPA. It contains a range
 * of valid AQI values in the level as well as the color representation
 * designated by the EPA. An AQILevel object identifies its level by a key
 * (see static member ints).
 * 
 * @author Erica Klein
 */
public class AQILevel {
	// There are 6 levels of AQI designated by the EPA
	public static int NUM_LEVELS = 6;
	public static int GOOD = 0;
	public static int MODERATE = 1;
	public static int UNHEALTHY_SENSITIVE = 2;
	public static int UNHEALTHY = 3;
	public static int VERY_UNHEALTHY = 4;
	public static int HAZARDOUS = 5;
	private static String[] NAMES = new String[]
	    { "Good", "Moderate", "Unhealthy for sensitive groups",
		  "Unhealty", "Very unhealthy", "Hazardous" };
	
	// Max AQI value specified by EPA
	public static int MAX_AQI = 500;
	
	// Value from 0 to 5 designating this level
	private int key;
	
	// Values associated with a given level, do not change
	private int rangeMin;
	private int rangeMax;
	private int r;
	private int g;
	private int b;
	private int fadeR;
	private int fadeG;
	private int fadeB;
	private int color;
	private int fadeColor;
	
	/**
	 * Creates an AQILevel object with range and color values specific to that
	 * level.
	 * @param key: value from 0 to 5 to specify the level, corresponding to
	 * 			   static ints above.
	 * Note: general constructor use would be:
	 * 			new AQILevel(AQILevel.getLevel(32.1));
	 */
	AQILevel(int key) {
		this.key = key;
		
		switch(key) {
		case 0: // good
			rangeMin = 0;
			rangeMax = 50;
			
			r = 0;
			g = 228;
			b = 0;
			
			fadeR = 140;
			fadeG = 255;
			fadeB = 140;
			break;
		case 1: // moderate
			rangeMin = 51;
			rangeMax = 100;
			
			r = 255;
			g = 255;
			b = 0;
			
			fadeR = r;
			fadeG = g;
			fadeB = 140;
			
			break;
		case 2: // unhealthy for sensitive groups
			rangeMin = 101;
			rangeMax = 150;
			
			r = 255;
			g = 126;
			b = 0;
			
			fadeR = 255;
			fadeG = 211;
			fadeB = 140;
			
			break;
		case 3: // unhealthy
			rangeMin = 151;
			rangeMax = 200;
			
			r = 255;
			g = 0;
			b = 0;
			
			fadeR = 255;
			fadeG = 140;
			fadeB = 140;
			
			break;
		case 4: // very unhealthy
			rangeMin = 201;
			rangeMax = 300;
			
			r = 153;
			g = 0;
			b = 76;
			
			fadeR = 209;
			fadeG = 115;
			fadeB = 181;
			
			break;
		default: // hazardous
			rangeMin = 301;
			rangeMax = 500;
			
			r = 76;
			g = 0;
			b = 38;
			
			fadeR = 189;
			fadeG = 104;
			fadeB = 134;
			
			break;
		}
		color = Color.rgb(r, g, b);
		fadeColor = Color.rgb(fadeR, fadeG, fadeB);
		//fadeColor = Color.argb(128, r, g, b);
	}
	
	/**
	 * Given an AQI value, returns the level key it is in.
	 * @param val: AQI value
	 * @return key for the level val is in
	 */
	public static int getLevel(double val) {
		if (val <= 50) {
			return AQILevel.GOOD;
		} else if (val <= 100) {
			return AQILevel.MODERATE;
		} else if (val <= 150) {
			return AQILevel.UNHEALTHY_SENSITIVE;
		} else if (val <= 200) {
			return AQILevel.UNHEALTHY;
		} else if (val <= 300) {
			return AQILevel.VERY_UNHEALTHY;
		} else {
			return AQILevel.HAZARDOUS;
		}
	}

	/**
	 * @param val: the AQI value in which level we want the name of
	 * @return the level name for a given AQI value.
	 */
	public static String getLevelName(double val) {
		return NAMES[getLevel(val)];
	}
	
	/**
	 * @return the name for this object's level.
	 */
	public String getLevelName() {
		return NAMES[key];
	}
	
	/**
	 * @return the key for this level, value from 0 to 5 to specify the level,
	 * 		   corresponding to static ints above.
	 */
	public int getKey() {
		return key;
	}

	/**
	 * @return the maximum AQI for this level.
	 */
	public int getRangeMin() {
		return rangeMin;
	}

	/**
	 * @return the minimum AQI for this level.
	 */
	public int getRangeMax() {
		return rangeMax;
	}

	/**
	 * @return the red value for this level's color.
	 */
	public int getRedVal() {
		return r;
	}

	/**
	 * @return the green value for this level's color.
	 */
	public int getGreenVal() {
		return g;
	}

	/**
	 * @return the blue value for this level's color.
	 */
	public int getBlueVal() {
		return b;
	}

	/**
	 * @return the integer representation of this level's RGB color.
	 */
	public int getColor() {
		return color;
	}
	

	/**
	 * @return the integer representation of this level's faded RGB color.
	 */
	public int getFadeColor() {
		return fadeColor;
	}
}
