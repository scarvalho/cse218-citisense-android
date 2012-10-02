package org.citisense.android;

public interface IAqiDataListener {
	public void AqiUpdated(int aqi, IReading[] readings);
}
