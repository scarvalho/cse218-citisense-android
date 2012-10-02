package org.citisense.android;

import org.citisense.android.service.impl.ApplicationSettings;
import org.citisense.datastructure.impl.LocationImpl;

import android.location.Location;

public class AndroidLocationImpl extends LocationImpl<Location> {
	public AndroidLocationImpl(Location androidLocation){
		super(androidLocation.getLatitude(), 
				androidLocation.getLongitude(), 
				androidLocation.getLongitude(), 
				androidLocation.getTime(),
				androidLocation.getProvider(),
				androidLocation.getAccuracy(),
				androidLocation);		
	}
	public android.location.Location getAndroidLocation(){
		return this.getExtaInformation();
	}
	public static LocationImpl<Object> convertFromAndroidLocation(android.location.Location androidLocation, Object extra){
		if(androidLocation != null){
			return new LocationImpl<Object>(androidLocation.getLatitude(), 
					androidLocation.getLongitude(), 
					androidLocation.getAltitude(), 
					androidLocation.getTime(),
					androidLocation.getProvider(),
					androidLocation.getAccuracy(),
					extra);
		} else {
			//when the phone can't find a location  we default to showing san diego coordinates. we could do something else here, maybe the user can choose a default location?
			return new LocationImpl<Object>(ApplicationSettings.instance().startingLatitude(),
					ApplicationSettings.instance().startingLongitude(),
					0, System.currentTimeMillis(), "fake", (float)0);
		}
		
	}
}
