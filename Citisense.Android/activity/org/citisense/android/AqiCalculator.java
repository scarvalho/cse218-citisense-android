package org.citisense.android;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import org.citisense.android.AqiBreakpointTable.AqiBreakpointEntry;
import org.citisense.datastructure.SensorType;

public class AqiCalculator {

	private AqiCalculator() {

	}

	// public static int calculateAQI(HashMap<String, Double> readings) {
	// int maxAQI = -1;
	// for( String pollutant : readings.keySet() ){
	// int aqi = calculateAQI(readings.get(pollutant), pollutant);
	// maxAQI = Math.max(aqi, maxAQI);
	// }
	// return maxAQI;
	// }

	public static final int UNKNOWN_AQI = -1;

	// FIXME: Use SensorType here, not String (like "CO")
	public static int calculateAQI(double concentration, String pollutant) {
		if (pollutant == "CO") {
			DecimalFormat oneDec = new DecimalFormat("#.#");
			concentration = Double.valueOf(oneDec.format(concentration));
		} else if (pollutant == "NO2") {
			DecimalFormat twoDec = new DecimalFormat("#.##");
			concentration = Double.valueOf(twoDec.format(concentration));
		} else if (pollutant == "O3") {
			DecimalFormat threeDec = new DecimalFormat("#.###");
			concentration = Double.valueOf(threeDec.format(concentration));
		}

		AqiBreakpointTable table = AqiCalculator.aqiTable.get(pollutant);

		if (table == null)
			return UNKNOWN_AQI;

		AqiBreakpointEntry entry = table.GetEntryByConcentration(concentration);

		if (entry == null || entry.aqiValues == null
				|| entry.breakpoints == null)
			return UNKNOWN_AQI;

		return calculateAqiInRange(entry.aqiValues, entry.breakpoints,
				concentration);
	}

	public static int calculateAqiInRange(Range aqiRange, Range breakpoint,
			double concentration) {
		return (int) Math.round((aqiRange.Delta() / breakpoint.Delta())
				* (concentration - breakpoint.Lo()) + aqiRange.Lo());
	}

	private static final Map<String, AqiBreakpointTable> aqiTable = new HashMap<String, AqiBreakpointTable>();
	static {
		AqiBreakpointTable ozone8hourTable = new AqiBreakpointTable(
				SensorType.O3.toString()); // PollutantKeys.Ozone_EightHour);
		ozone8hourTable.AddEntry(0.000, 0.059, 0.0, 50.0);
		ozone8hourTable.AddEntry(0.060, 0.075, 51.0, 100.0);
		ozone8hourTable.AddEntry(0.076, 0.095, 101.0, 150.0);
		ozone8hourTable.AddEntry(0.096, 0.115, 151.0, 200.0);
		ozone8hourTable.AddEntry(0.116, 0.374, 201.0, 300.0);
		ozone8hourTable.AddEntry(0.405, 0.504, 301.0, 400.0);
		ozone8hourTable.AddEntry(0.505, 0.604, 401.0, 500.0);
		aqiTable.put(ozone8hourTable.GetPollutantName(), ozone8hourTable);

		AqiBreakpointTable coTable = new AqiBreakpointTable(
				SensorType.CO.toString());// PollutantKeys.CarbonMonoxide);
		coTable.AddEntry(0.0, 4.4, 0.0, 50.0);
		coTable.AddEntry(4.5, 9.4, 51.0, 100.0);
		coTable.AddEntry(9.5, 12.4, 101.0, 150.0);
		coTable.AddEntry(12.5, 15.4, 151.0, 200.0);
		coTable.AddEntry(15.5, 30.4, 201.0, 300.0);
		coTable.AddEntry(30.5, 40.4, 301.0, 400.0);
		coTable.AddEntry(40.5, 50.4, 401.0, 500.0);
		aqiTable.put(coTable.GetPollutantName(), coTable);

		AqiBreakpointTable no2Table = new AqiBreakpointTable(
				SensorType.NO2.toString()); // PollutantKeys.NitrusOxide);
		no2Table.AddEntry(0.0,  0.64,   0.0,   0.0); // while not documented for AQI, we need to return a value to be consistent
		no2Table.AddEntry(0.65, 1.24, 201.0, 300.0);
		no2Table.AddEntry(1.25, 1.64, 301.0, 400.0);
		no2Table.AddEntry(1.65, 2.04, 401.0, 500.0);
		aqiTable.put(no2Table.GetPollutantName(), no2Table);
	}
}
