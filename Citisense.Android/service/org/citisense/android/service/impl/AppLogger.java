package org.citisense.android.service.impl;
import org.slf4j.Logger;

import android.util.Log;

public class AppLogger {

	// Current logging level
	private static final int INIT_LOG_LEVEL = Log.WARN;
	public static int CUR_LOG_LEVEL = INIT_LOG_LEVEL;
	
	public static synchronized int toggleLevel() {
		CUR_LOG_LEVEL = (CUR_LOG_LEVEL == INIT_LOG_LEVEL) ? Log.DEBUG : INIT_LOG_LEVEL;
		return CUR_LOG_LEVEL;
	}
	
	public static String getLogLevelString() {
		if(CUR_LOG_LEVEL == Log.DEBUG)
			return "DEBUG";
		else if(CUR_LOG_LEVEL == Log.ERROR)
			return "ERROR";
		else if(CUR_LOG_LEVEL == Log.INFO)
			return "INFO";
		else if(CUR_LOG_LEVEL == Log.VERBOSE)
			return "VERBOSE";
		else if(CUR_LOG_LEVEL == Log.ASSERT)
			return "ASSERT";
		else if(CUR_LOG_LEVEL == Log.WARN)
			return "WARN";
		else
			return "unknown";
	}
	
	public static boolean isDebugEnabled(Logger logger) {
		return (CUR_LOG_LEVEL <= Log.DEBUG) && logger.isDebugEnabled();
	}
	
	public static boolean isWarnEnabled(Logger logger) {
		return (CUR_LOG_LEVEL <= Log.WARN) && logger.isWarnEnabled();
	}
	
	public static boolean isErrorEnabled(Logger logger) {
		return (CUR_LOG_LEVEL <= Log.ERROR) && logger.isErrorEnabled();
	}
	
	public static boolean isInfoEnabled(Logger logger) {
		return (CUR_LOG_LEVEL <= Log.INFO) && logger.isInfoEnabled();
	}
	
	public static boolean isTraceEnabled(Logger logger) {
		return (CUR_LOG_LEVEL <= Log.VERBOSE) && logger.isTraceEnabled();
	}
}
