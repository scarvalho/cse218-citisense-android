package org.citisense.android;

public interface IReading {
	public Double getValue();
	public String getName();
	public int getAqi();
	public IContext getContext();
}
