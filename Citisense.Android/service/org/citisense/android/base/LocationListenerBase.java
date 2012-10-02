package org.citisense.android.base;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

public class LocationListenerBase implements LocationListener {

	@Override
	public void onLocationChanged(Location location) {
	}

	@Override
	public void onProviderDisabled(String s) {
	}

	@Override
	public void onProviderEnabled(String s) {
	}

	@Override
	public void onStatusChanged(String s, int i, Bundle bundle) {
	}
}
