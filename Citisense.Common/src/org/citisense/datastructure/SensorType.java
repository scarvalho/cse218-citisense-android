package org.citisense.datastructure;

public enum SensorType {

	/**
	 * Remember: Ordinals start from 0. Our pins start from 1.
	 */
	NO2("PPM"), O3("PPM"), CO("PPM"), TEMP("C"), HUMD("%"), PRES("MBAR"), AQI("AQI"), MAX_AQI("AQI");

	private final String units;
	private static final double UNDEFINED = Double.NaN;

	private SensorType(String units) {
		this.units = units;
	}

	public String getUnits() {
		return units;
	}

	public int getPinNumber() {
		return this.ordinal() + 1;
	}

	public double UNDEFINED() {
		return UNDEFINED;
	}

	public static String getNameFor(int pinNumber) {
		return getSensorTypeFor(pinNumber).name();
	}
	
	public static SensorType getSensorTypeFor(int pinNumber) {
		int ordinal = pinNumber - 1;
		// If none or non-existing sensor type requested
		if (ordinal < 0 || ordinal >= SensorType.values().length) {
			throw new ArrayIndexOutOfBoundsException("Invalid pinNumber: "
					+ pinNumber);
		}
		return SensorType.values()[ordinal];
	}

//	public static void main(String[] args) {
//		System.out.println(SensorType.CO.ordinal());
//		System.out.println(SensorType.CO.name());
//		System.out.println(SensorType.CO.getUnits());
//		System.out.println(SensorType.CO.getPinNumber());
//	}

}
