package org.citisense.android.service;

public interface BluetoothService {
	public void connectSensor(String address);
	public void disconnectSensor();
	public int isSensorConnected();
}