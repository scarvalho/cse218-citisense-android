package org.citisense.android.share;

import java.util.Calendar;

import org.citisense.android.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;

/**
 * This pops up when someone hits the share button.
 * It should give them options about how much data to share and with
 * what message.
 * @author Nima
 */

public class ShareDialogue extends Activity {
	//private final Logger logger = LoggerFactory.getLogger(ShareDialogue.class);
	
	public static String EXTRA_HOURS_TO_SHARE = "hours_to_share";
	private Button button_1hour, button_4hour, button_day, button_cancel;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Setup the window
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.share_dialogue);
		
		// setResult CANCELED in case the user backs out
		setResult(Activity.RESULT_CANCELED);
		
		// Return intent containing number of seconds to share
		button_1hour = (Button) findViewById(R.id.share_dialogue_1hour);
		button_1hour.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.putExtra(EXTRA_HOURS_TO_SHARE, (long)3600);
				setResult(Activity.RESULT_OK, intent);
				finish();
			}
		});
		
		button_4hour = (Button) findViewById(R.id.share_dialogue_4hour);
		button_4hour.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.putExtra(EXTRA_HOURS_TO_SHARE, (long)14400);
				setResult(Activity.RESULT_OK, intent);
				finish();
			}
		});
		
		button_day = (Button) findViewById(R.id.share_dialogue_day);
		button_day.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Get a calendar set to today, midnight
				Calendar cal = Calendar.getInstance();
				cal.set(Calendar.MILLISECOND, 0);
				cal.set(Calendar.SECOND, 1);
				cal.set(Calendar.MINUTE, 0);
				cal.set(Calendar.HOUR, 0);
				cal.set(Calendar.AM_PM, 0);
				long secToMidnight = (System.currentTimeMillis() - cal.getTimeInMillis()) / 1000;
				
				Intent intent = new Intent();				
				intent.putExtra(EXTRA_HOURS_TO_SHARE, secToMidnight);
				setResult(Activity.RESULT_OK, intent);
				finish();
			}
		});
		
		button_cancel = (Button) findViewById(R.id.share_dialogue_cancel);
		button_cancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}
}
