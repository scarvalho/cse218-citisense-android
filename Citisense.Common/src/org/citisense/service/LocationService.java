package org.citisense.service;

import org.citisense.datastructure.Location;

public interface LocationService {

	public Location<Object> getLastKnownLocation();
}
