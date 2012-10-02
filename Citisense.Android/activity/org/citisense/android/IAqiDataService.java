package org.citisense.android;

import android.content.Context;
import android.location.Location;

public interface IAqiDataService {
	public void Start(Context context);
	public void RequestUpdate();
	public void Register(IAqiDataListener listener);
	public void UpdateLocation(Location location);
	public void Destroy();
	public IReading[] getLastReadings( );
}
