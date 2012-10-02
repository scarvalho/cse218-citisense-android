package org.citisense.android.service.impl;

import org.citisense.android.AndroidLocationImpl;
import org.citisense.android.service.location.AllNetwork;
import org.citisense.android.service.location.NetworkChange;
import org.citisense.datastructure.Location;
import org.citisense.datastructure.impl.LocationImpl;
import org.citisense.service.LocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class LocationServiceImpl implements LocationService {

	private Logger logger = LoggerFactory.getLogger(LocationServiceImpl.class);

	private final LocationManager locationManager = ApplicationSettings
			.instance().locationManager();

	//private Location<Object> lastKnownLocation = new LocationImpl<Object>(
	//		Location.UNDEFINED_LATITUDE, Location.UNDEFINED_LONGITUDE,
	//		Location.UNDEFINED_ALTITUDE, Location.UNDEFINED_TIME, 
	//		Location.UNDEFINED_SOURCE, Location.UNDEFINED_ACCURACY);
	
	private Location<Object> lastKnownLocation = ApplicationSettings.instance().isNodeStationary() ?
			new LocationImpl<Object>(ApplicationSettings.instance().startingLatitude(),	ApplicationSettings.instance().startingLongitude(),
					0, System.currentTimeMillis(), "stationary", (float)0)
			:
			AndroidLocationImpl.convertFromAndroidLocation(locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER),null);
	
	// List of providers to be used
	private AllNetwork allNetworkProvider;
	private NetworkChange networkChangeProvider;
	
	private int errorThreshold = 75;
		

	public LocationServiceImpl() {
		LocationListener locationListener = new LocationListener() {
			public void onLocationChanged(
					android.location.Location location) {
				lastKnownLocation = new LocationImpl<Object>(location
						.getLatitude(), location.getLongitude(),
						location.getAltitude(), location.getTime(), 
						location.getProvider(), location.getAccuracy());
				if (AppLogger.isDebugEnabled(logger))
					logger.debug("Location changed to {}",
						lastKnownLocation);
			}
			public void onStatusChanged(String provider, int status, Bundle extras) {}

		    public void onProviderEnabled(String provider) {}

		    public void onProviderDisabled(String provider) {}
		};
		
		if(!ApplicationSettings.instance().isNodeStationary()) {
			// Instantiate the possibly used providers
			allNetworkProvider = new AllNetwork(locationListener);
			networkChangeProvider = new NetworkChange(errorThreshold, locationListener);
			
			// For now, only use: AllNetwork
			allNetworkProvider.stop();
			networkChangeProvider.start();
		}
	}

	@Override
	public Location<Object> getLastKnownLocation() {
		if (AppLogger.isDebugEnabled(logger))
			logger.debug("Returning Location {}",
				lastKnownLocation);
		return lastKnownLocation;
	}
}
