package org.citisense.android;

// Simple class used to describe the different states of the AQI UI
public class AqiDisplay {
	public int meaningId;
	public int imageId;
	public AqiDisplay(int meaningId, int imageId){
		this.meaningId = meaningId;
		this.imageId = imageId;
	}
}
