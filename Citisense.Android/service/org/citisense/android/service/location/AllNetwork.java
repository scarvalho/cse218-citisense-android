package org.citisense.android.service.location;

import org.citisense.android.service.impl.ApplicationSettings;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

// A simple policy that simply uses network based location all the time

public class AllNetwork {
	
	private final LocationManager locationManager = ApplicationSettings
		.instance().locationManager();
	
	private Location lastNetworkLocation;
	
	private LocationListener m_listenerCallback;
	private LocationListener m_allNetworkListener = new LocationListener() {
		@Override
		public void onLocationChanged(Location location) {
			// If the speed traveled between these readings makes no sense, don't use it
			if(lastNetworkLocation != null && !LocationHelper.isAcceptableSpeed(lastNetworkLocation, location)) {
				return;
			}
			lastNetworkLocation = location;
			m_listenerCallback.onLocationChanged(location);
		}
		@Override
		public void onProviderDisabled(String provider) {
			
		}
		@Override
		public void onProviderEnabled(String provider) {
			
		}
		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			
		}
	};
	
	public AllNetwork(LocationListener listenerCallback) {
		m_listenerCallback = listenerCallback;
	}
	
	public void start() {
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, m_allNetworkListener);
	}
	
	public void stop() {
		locationManager.removeUpdates(m_allNetworkListener);
	}
	
}
