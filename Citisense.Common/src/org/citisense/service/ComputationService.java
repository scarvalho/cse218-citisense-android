package org.citisense.service;

import java.util.Date;

import org.citisense.datastructure.Location;

/**
 * 
 * @author celal.ziftci
 * 
 */
public interface ComputationService {
	public int getEstimate(Location<Object> location, Date date);
}
