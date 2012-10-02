package org.citisense.android.androidservice;

import org.citisense.android.service.impl.AppLogger;
import org.citisense.android.service.impl.CitiSenseExposedServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;

public class AndroidBackgroundServiceStarter {
	private static final Logger logger = LoggerFactory
			.getLogger(AndroidBackgroundServiceStarter.class);

	public static void start(Context context) {
		Intent backgroundServiceIntent = new Intent(context,
				AndroidBackgroundService.class);
		ComponentName result = context.startService(backgroundServiceIntent);
		if (result == null) {
			throw new RuntimeException("Cannot bind to "
					+ AndroidBackgroundService.class.getName());
		} else {
			if (AppLogger.isDebugEnabled(logger)) {
				logger.debug("Created  {} (or it was already there)",
						AndroidBackgroundService.class.getName());
			}
		}
	}

	public static void bind(Context context,
			ServiceConnection serviceBindCallback) {
		Intent backgroundServiceIntent = new Intent(context,
				AndroidBackgroundService.class);
		
		context.startService(backgroundServiceIntent);

		boolean serviceBound = context.bindService(backgroundServiceIntent,
				serviceBindCallback, Context.BIND_AUTO_CREATE);
		if (!serviceBound) {
			throw new RuntimeException("Cannot bind to "
					+ AndroidBackgroundService.class.getName());
		} else {
			if (AppLogger.isDebugEnabled(logger)) logger.debug("Bound to "+ CitiSenseExposedServices.class.getName());
		}
	}
}
