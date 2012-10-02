package org.citisense.android;

import org.citisense.android.androidservice.AndroidBackgroundServiceStarter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StartupIntentReciever extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		AndroidBackgroundServiceStarter.start(context);
	}

}
