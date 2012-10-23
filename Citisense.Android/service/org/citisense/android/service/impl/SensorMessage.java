package org.citisense.android.service.impl;

import org.citisense.datastructure.SensorType;

public class SensorMessage {

	SensorType sensorType;
	float value;
	String sensorUnits;
	public final int OUTPUT_MSG_SIZE = 5;
	public final int INPUT_MSG_SIZE = 8;
	
	public SensorMessage(SensorType type, int val)
	{
		sensorType = type;
		value = val;
		sensorUnits = sensorType.getUnits();
	}
	
	public SensorMessage(int sensorNum, int val)
	{
		sensorType = getSensorType((byte)(1 << (sensorNum - 1)));
		value = (float)val;
		sensorUnits = sensorType.getUnits();
	}
	
	public SensorMessage(byte[] serialMsg)
	{
		// Third byte is sensor identifier
		int val =  serialMsg[3];
		val = val << 8 | serialMsg[4];
		value = (float)val;

		sensorType = getSensorType(serialMsg[2]);
		
		// Check if the sensor type is NONE (j == 8)
		// If so, discard the message
		if (sensorType != null) 
		{		
			// Figure out units for measurement
			
			sensorUnits = sensorType.getUnits();
		
			if (sensorType == SensorType.NO2) {
				value /= 1000;
			} 
			else if(sensorType == SensorType.O3) {
				value /= 1000;
			} 
			else if (sensorType == SensorType.CO) {
				value /= 10;
			} 
			else if (sensorType == SensorType.TEMP) {
				value /= 10;
			} 
			else if (sensorType == SensorType.PRES) {
				value /= 10;
			} 
			else if (sensorType == SensorType.HUMD) {		
			} 
			else {
				sensorUnits = "UNKNOWN";
			}	
		}
	}
	
	public byte[] getByteArray()
	{
		byte[] msg = new byte[32];
		msg[0] = 'x';
		msg[1] = 'M';
		msg[2] = (byte)(sensorType.ordinal() + 1);
		msg[3] = (byte)((((int)value) & 0xFF00) >> 8);
		msg[4] = (byte)(((int)value & 0xFF));
		return msg;
	}
		
	private SensorType getSensorType(byte flag)
	{
		int j = 0;
		int f = flag;
		while (f != 0) 
		{
			f = f >> 1;
			j++;
		}
		if (j != 8)
		{
			sensorType = SensorType.getSensorTypeFor(j);
		}
		else
		{
			sensorType = null;
		}
		return sensorType;
	}
}
