package org.citisense.android;

public interface IAqiDataSource {

	public int GetAqi() throws Exception;
	public IReading[] GetReadings() throws Exception;
	
}
