package org.sosa.richservice.base;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ThreadFactory;

/**
 * 
 * @author celal.ziftci
 * 
 */
public class UncaughtExceptionHandlingThreadFactory implements ThreadFactory {
	private final UncaughtExceptionHandler handler;

	public UncaughtExceptionHandlingThreadFactory(
			UncaughtExceptionHandler handler) {
		this.handler = handler;
	}

	@Override
	public Thread newThread(Runnable arg0) {
		Thread t = new Thread();
		t.setUncaughtExceptionHandler(handler);
		return t;
	}

}
