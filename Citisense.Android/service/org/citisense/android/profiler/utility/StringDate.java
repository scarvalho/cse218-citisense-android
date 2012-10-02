package org.citisense.android.profiler.utility;

import java.text.SimpleDateFormat;
import java.util.Date;

public class StringDate {
	private static SimpleDateFormat sdf = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss.SSS");

	public static String getCurrentDate() {
		return sdf.format(new Date());
	}
	
	public static String formatDate(Date date) {
		return sdf.format(date);
	}
}
