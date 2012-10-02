package org.citisense.android;

public enum Aqi_ui_state {
	Initializing,
	ConnectingToService,
	WaitingForInitialData,
	GotDataWaitingToUpdate,
	UpdatingData,
	ErrorGettingData,
	LaunchingDetails
}
