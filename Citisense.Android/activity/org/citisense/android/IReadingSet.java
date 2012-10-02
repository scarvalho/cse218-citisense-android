package org.citisense.android;

public interface IReadingSet {

	IReading[] getReadings();
	int getAqi();
	IReading getOffender();
}
