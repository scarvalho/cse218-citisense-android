package org.citisense.android.service.location;

import android.location.Location;

public class LocationHelper {
	// Maximum possible speed a person could travel
	// Sometimes we see a completely wrong location, this can help us catch it
	// Ex: All readings in SD, suddenly one in SF, then back to SD
	// For now: max speed due to error from reading + 300 m/s (just under jumbo jet speed)
	private static float MAX_VALID_SPEED = 300;
	
	public static boolean isAcceptableSpeed(Location oldLocation, Location newLocation) {
		boolean result = true;
		
		// Get maximum amount of error from the two readings
		float maxError = (newLocation.getAccuracy() > oldLocation.getAccuracy()) ? 
				newLocation.getAccuracy() : oldLocation.getAccuracy();
		// Subtract distance that may be due to error in reading
		float adjustedDistance = oldLocation.distanceTo(newLocation) - maxError;
		// Note: This speed might be negative, which is fine
		float measuredSpeed = adjustedDistance / (newLocation.getTime() - oldLocation.getTime());
		
		// If speed seems too high, return false
		if(measuredSpeed >  MAX_VALID_SPEED) {
			result = false;
		}
		
		return result;
	}
}
