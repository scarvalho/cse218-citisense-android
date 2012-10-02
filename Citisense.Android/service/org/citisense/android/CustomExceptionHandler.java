package org.citisense.android;

import java.lang.Thread.UncaughtExceptionHandler;

import org.citisense.android.service.impl.AppLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomExceptionHandler implements UncaughtExceptionHandler {
	
	private final Logger logger = LoggerFactory.getLogger(CustomExceptionHandler.class);
	
	
	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		if(AppLogger.isErrorEnabled(logger)) {
			logger.error("Uncaught exception in thread id: " + thread.getId()
					+ ", name: " + thread.getName() + " " + ex.fillInStackTrace() + " " + ex.getCause());
			Runtime rt = Runtime.getRuntime();
			logger.error("Allocated memory: " + rt.totalMemory() + ", Free memory: " + rt.freeMemory());
		}
	}

}
