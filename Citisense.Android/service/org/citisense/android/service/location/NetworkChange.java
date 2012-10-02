package org.citisense.android.service.location;

import org.citisense.android.service.impl.AppLogger;
import org.citisense.android.service.impl.ApplicationSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.PowerManager;


// Idea behind policy:
// While moving and network provides acceptable error, continue using it
// But if error goes up, attempt to use GPS
// If GPS doesn't do better than the threshold, shut it off and continue to use network for now...
// Periodically try GPS again if network stays lousy

public class NetworkChange {
	
	private Logger logger = LoggerFactory.getLogger(NetworkChange.class);
	
	private final LocationManager locationManager = ApplicationSettings
		.instance().locationManager();
	private PowerManager powerManager = ApplicationSettings
		.instance().powerManager();
	private PowerManager.WakeLock wakeLock;
	
	private int m_errorThreshold;
	private LocationListener m_listenerCallback;
	
	// The number of locations that have to be from the same location to reset isMoving
	private static int SAME_LOCATION_COUNT_THRESHOLD = 10;
	// How old a location reading can be (in milliseconds)
	private static int READING_AGE_THRESHOLD = 1200000;
	// Meters between two readings to be considered 'moving'
	private static float SAME_LOCATION_DISTANCE_THRESHOLD = 5;
	// Number of bad readings needed to elevate to GPS
	private static int NETWORK_ERROR_COUNT_THRESHOLD = 2;
	private static int GPS_ERROR_COUNT_THRESHOLD = 3;
	private static int NUM_NETWORK_SINCE_GPS_THRESHOLD = 3;
	
	private int networkLocationCount = 0;
	private int gpsLocationCount = 0;
	
	private int sameLocationCount = 0;
	private int badErrorNetworkCount = 0;
	private int badErrorGPSCount = 0;
	private int networkCountSinceLastGPS = 0;
	private boolean isMoving = false;
	private boolean isGPSActive = false;
	private boolean isNetworkActive = true;
	private Location lastNetworkLocation;
	private Location lastGPSLocation;
	private Location lastReturnedLocation;
	
	private LocationListener m_networkChangeListener;
	
	private void disableGPS() {
		if(wakeLock.isHeld()) {
			if(AppLogger.isDebugEnabled(logger))
				logger.debug("GPS was disabled!");
			isGPSActive = false;
			locationManager.removeUpdates(m_networkChangeListener);
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, m_networkChangeListener);
			isNetworkActive = true;
			networkCountSinceLastGPS = 0;
			badErrorNetworkCount = 0;
			
			// Release previously held cpu lock
			wakeLock.release();
		}
	}
	
	private void enableGPS() {
		if(!wakeLock.isHeld()) {
			if(AppLogger.isDebugEnabled(logger))
				logger.debug("GPS was enabled!");
			isGPSActive = true;
			badErrorGPSCount = 0;
			
			// Get cpu lock since GPS doesnt arrive while phone sleeps
			wakeLock.acquire();
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, m_networkChangeListener);
		}
	}
	
	public NetworkChange(int errorThreshold, LocationListener listenerCallback) {
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NetworkChange");
		m_errorThreshold = errorThreshold;
		m_listenerCallback = listenerCallback;
//		lastNetworkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
//		lastGPSLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//		if(lastNetworkLocation.getTime() > lastGPSLocation.getTime()) {
//			lastReturnedLocation = lastNetworkLocation;
//		} else {
//			lastReturnedLocation = lastGPSLocation;
//		}
		
		m_networkChangeListener = new LocationListener() {
			@Override
			public void onLocationChanged(Location location) {
				boolean shouldReturnNew = false;
				
				if(AppLogger.isDebugEnabled(logger)) {
					logger.debug(location.toString());
					//logger.debug("Is GPS Active: " + isGPSActive + ", Reading Source: " + location.getProvider());	
				}
				
				//////////////////////////////////
				// HANDLE NETWORK LOCATION UPDATES
				//////////////////////////////////
				if(location.getProvider().equals(LocationManager.NETWORK_PROVIDER)) {
					networkLocationCount++;
					
					if(isGPSActive) {
						networkCountSinceLastGPS++;
						if(networkCountSinceLastGPS >= NUM_NETWORK_SINCE_GPS_THRESHOLD) {
							if(AppLogger.isDebugEnabled(logger))
								logger.debug("Disabling GPS: No updates received for a while, disabling.");
							disableGPS();
						}
					}
					
					// Make sure last reading is not too old
					if(lastNetworkLocation != null && location.getTime() - lastNetworkLocation.getTime() < READING_AGE_THRESHOLD) {
						
						// LOGIC FOR MOVEMENT DETECTION
						// NOTE: A more accurate fix may adjust location and thus be counted as movement
						if(location.distanceTo(lastNetworkLocation) > SAME_LOCATION_DISTANCE_THRESHOLD) {
							// Reset count to 0, set moving
							sameLocationCount = 0;
							isMoving = true;
						} else {
							// Same location, increment same location count
							sameLocationCount++;
							if(isMoving && sameLocationCount >= SAME_LOCATION_COUNT_THRESHOLD) {
								isMoving = false;
							}
						}
						
						// IF MOVING...
						if(isMoving) {
							// If error > threshold, enable GPS
							if( (location.getAccuracy() < m_errorThreshold || 
									location.getAccuracy() < lastReturnedLocation.getAccuracy()) &&
									LocationHelper.isAcceptableSpeed(lastReturnedLocation, location) ) {
								shouldReturnNew = true;
								badErrorNetworkCount = 0;
								// If network is doing better than GPS, turn off GPS!
								if(isGPSActive && 
										lastReturnedLocation.getProvider().equals(LocationManager.GPS_PROVIDER)) {
									if(AppLogger.isDebugEnabled(logger))
										logger.debug("Disabling GPS: Network accuracy is acceptable.");
									disableGPS();
								}
							} else {
								badErrorNetworkCount++;
								if(badErrorNetworkCount == NETWORK_ERROR_COUNT_THRESHOLD) {
									if(AppLogger.isDebugEnabled(logger))
										logger.debug("Enabling GPS: Network error too high.");
									enableGPS();
								}
							}
						}
					}
					// Update 'last network location'	
					lastNetworkLocation = location;
				}
				//////////////////////////////
				// HANDLE GPS LOCATION UPDATES
				//////////////////////////////
				else if(location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
					
					networkCountSinceLastGPS = 0;
					
					if(lastGPSLocation != null && location.getTime() - lastGPSLocation.getTime() < READING_AGE_THRESHOLD) {
						
						// LOGIC FOR MOVEMENT DETECTION
						if(location.distanceTo(lastGPSLocation) > SAME_LOCATION_DISTANCE_THRESHOLD
								|| location.hasSpeed() && location.getSpeed() > 0.0f) {
							// Reset count to 0, set moving
							sameLocationCount = 0;
							isMoving = true;
						} else {
							// Same location, increment same location count
							sameLocationCount++;
							if(isMoving && sameLocationCount >= SAME_LOCATION_COUNT_THRESHOLD) {
								isMoving = false;
							}
						}
						
						// If error is bad, stop trying to use GPS
						if( (location.getAccuracy() < m_errorThreshold || 
								location.getAccuracy() < lastReturnedLocation.getAccuracy()) &&
								LocationHelper.isAcceptableSpeed(lastReturnedLocation, location) ) {
							shouldReturnNew = true;
							badErrorGPSCount = 0;
						} else if(isMoving) {
							badErrorGPSCount++;
							if(badErrorGPSCount == GPS_ERROR_COUNT_THRESHOLD) {
								if(AppLogger.isDebugEnabled(logger))
									logger.debug("Disabling GPS: Error too high to be useful.");
								disableGPS();
							}
						}
					}
					
					if(!isMoving) {
						if(AppLogger.isDebugEnabled(logger))
							logger.debug("Disabling GPS: No longer moving.");
						disableGPS();
					}
					gpsLocationCount++;
					// Update 'last GPS location'	
					lastGPSLocation = location;
				}
				
				if(lastReturnedLocation == null 
						|| (!isMoving && location.getAccuracy() < lastReturnedLocation.getAccuracy())) {
					shouldReturnNew = true;
				}
				
				// Return the location
				if(shouldReturnNew) {
					m_listenerCallback.onLocationChanged(location);
					lastReturnedLocation = location;
				} else {
					m_listenerCallback.onLocationChanged(lastReturnedLocation);
				}
				
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
	}
	
	public void start() {
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, m_networkChangeListener);
	}
	
	public void stop() {
		locationManager.removeUpdates(m_networkChangeListener);
	}

}
