package org.citisense.android;

import static org.citisense.datastructure.BluetoothConstants.REQUEST_CONNECT_DEVICE;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import org.citisense.android.androidservice.AndroidBackgroundServiceStarter;
import org.citisense.android.androidservice.LocalBinder;
import org.citisense.android.service.impl.AppLogger;
import org.citisense.android.service.impl.CitiSenseExposedServices;
import org.citisense.datastructure.SensorReading;
import org.citisense.datastructure.SensorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class Aqi_ui_detailed extends Activity {
	private final Logger logger = LoggerFactory.getLogger(Aqi_ui_detailed.class);
	private TableLayout tableLayout;
	private AQIGraphView graphView;
	private DrawGraphTask drawGraphTask;
	private boolean isGraphDone;
	private double[] graphValues;
	private ImageView graphViewMask;
	
	private Collection<SensorReading> _maxAqis = null;
	private Collection<SensorReading> getMaxAqis(){
		if( _maxAqis == null && this.exposedServices != null ){
			_maxAqis = this.exposedServices.getMaxAqisForToday();
		}
		return _maxAqis;
	}
	
	private CitiSenseExposedServices exposedServices;
	private final ServiceConnection serviceBindCallback = new ServiceConnection() {
		@SuppressWarnings("unchecked")
		public void onServiceConnected(ComponentName className, IBinder service) {
			exposedServices = ((LocalBinder<CitiSenseExposedServices>) service)
					.getService();
			
			displayDetailedView();
		}
		public void onServiceDisconnected(ComponentName className) {
			// As our service is in the same process, this should never be
			// called
		}
	};
	
	public void detailsClick(View view) {
		if(AppLogger.isInfoEnabled(logger))
			logger.info("User tap for main screen");

		// TODO: find a better method to go back...
		finish();
	}

	
	/** Called when the activity is first created. */
	public void onCreate(Bundle savedInstanceState) {
		//lgr.debug("start: Detail: onCreate");
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.detailed);
		tableLayout = (TableLayout) findViewById(R.id.pollutantTableLayout);
		graphView = (AQIGraphView) findViewById(R.id.graphView);
		graphViewMask = (ImageView) findViewById(R.id.graphViewMask);
		isGraphDone = false;
		graphValues = null;
		
		if(AppLogger.isInfoEnabled(logger))
			logger.info("Created app");
		
		AndroidBackgroundServiceStarter.start(this);
		AndroidBackgroundServiceStarter.bind(this, serviceBindCallback);

	}
	
	private static final int HOURS_EXPECTED_BY_GRAPH = 24;
	private static final double[] EMPTY_GRAPH_DATA = new double[]{-1};
	private double[] getHourlyMaxAQI() {
		
		Collection<SensorReading> readings = this.getMaxAqis();
			
		if(readings == null){
			return EMPTY_GRAPH_DATA;
		}
		
		double[] values = new double[HOURS_EXPECTED_BY_GRAPH];
		Arrays.fill(values, 0, values.length-1, -1.0);
		for(SensorReading reading : readings){
			int hour = reading.getTimeDate().getHours();
			values[hour] = reading.getSensorData();
		} 
		return values;
	}
	
	private class DrawGraphTask extends AsyncTask<Void, Void, double[]> {
		
		@Override
		protected void onPreExecute() {
			graphViewMask.setVisibility(View.VISIBLE);
//			RotateAnimation anim = new RotateAnimation(0, 360, 
//					graphViewMask.getWidth()/2, graphViewMask.getHeight()/2);
//			ScaleAnimation anim = new ScaleAnimation(1.0f, 0.5f, 1.0f, 0.5f);
//			anim.setRepeatCount(Animation.INFINITE);
//			anim.setDuration(2000);
//			graphViewMask.startAnimation(anim);
		}

		@Override
		protected double[] doInBackground(Void... arg0) {		
			if (exposedServices == null) {
				graphValues = EMPTY_GRAPH_DATA;
	    	} else {
	    		graphValues = getHourlyMaxAQI();
	    	}  
			return graphValues;
		}
		
		@Override
		protected void onPostExecute(double[] values) {
			updateGraphValues(values);
	    	graphViewMask.setVisibility(View.INVISIBLE);
	    	isGraphDone = true;
		}
	}
	
	private void updateGraphValues(double[] values) {
		AQIGraphView graphView = (AQIGraphView) findViewById(R.id.graphView);
    	graphView.setValues(values);
    	graphView.invalidate();
	}
    
    private void displayDetailedView() {    	
    	if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
    		//drawGraph();
    		drawGraphTask = new DrawGraphTask();
    		drawGraphTask.execute();
    	}else{
    		displayDetailsList();
    		//drawGraph();
    		drawGraphTask = new DrawGraphTask();
    		drawGraphTask.execute();
    	}
	}

    protected void displayDetailsList()
    {
    	if( this.exposedServices == null) return;
    	
    	Collection<SensorReading> latestReadings = 
    		this.exposedServices.getLastReadingsForAllSensorTypes();
    		
    	SensorType aqiSensorType = null; 
    	SensorReading aqiSensorReading = null;
    	long lastUpdatedMilliseconds = 0;
    	
		for(SensorReading reading : latestReadings){	
			
			if(reading.getSensorType() == SensorType.AQI){
				aqiSensorReading = reading;
				aqiSensorType = SensorType.valueOf(reading.getLocation().getProvider());
			}
			
			TableRow row = getRowBySensorType(reading.getSensorType());
			
			if( row == null) continue; // no UI for this reading type (ignore)
			
			if(reading.getTimeMilliseconds() > lastUpdatedMilliseconds ){
				lastUpdatedMilliseconds = reading.getTimeMilliseconds();
			}
			
			String units = reading.getSensorUnits();
			double pollutantVal = reading.getSensorData();
			if(reading.getSensorType() == SensorType.TEMP && reading.getSensorUnits().equals("C"))
			{
				pollutantVal = (pollutantVal*((double)9/(double)5)) + 32;
				DecimalFormat twoDec = new DecimalFormat("#.#");
				pollutantVal = Double.valueOf(twoDec.format(pollutantVal));
				units = "\u00B0F";
			}
			
			if(reading.getSensorType() == SensorType.MAX_AQI){
				units = "(" + reading.getLocation().getProvider() + ")";
			}
			
			TextView valueAndUnitsTextView = (TextView) row.getChildAt(1);
			valueAndUnitsTextView.setText(Double.toString(pollutantVal) + " " + units);
		}
		
		if(lastUpdatedMilliseconds > 0){
			TextView updatedTextView = (TextView) this.findViewById(R.id.detailsUpdatedText);
			updatedTextView.setText("last updated at " + DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(lastUpdatedMilliseconds)));			
		}
		
		
		if(aqiSensorType != null){
			TableRow rowToHighlight = getRowBySensorType(aqiSensorType);
			
			int aqiColor = new AQILevel(AQILevel.getLevel(aqiSensorReading.getSensorData())).getColor();
			
			rowToHighlight.setBackgroundColor(aqiColor);
			for(int i = 0 ; i < rowToHighlight.getChildCount(); ++i){
				TextView text = (TextView) rowToHighlight.getChildAt(i);
				if( text != null ) text.setTextColor(Color.BLACK);
			}
		}
    }
    
    private TableRow getRowBySensorType(SensorType type){
    	return (TableRow) tableLayout.findViewWithTag(type.name().toString() + "_val");
    }
    
	@Override
	protected void onStop() {
		super.onStop();
		// handler.removeCallbacks(updateAqiTask);
		if(AppLogger.isInfoEnabled(logger))
			logger.info("Stopped app");
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		if(AppLogger.isInfoEnabled(logger))
			logger.info("Restart app");
	}

	@Override
	protected void onResume() {
		super.onResume();
		if(AppLogger.isInfoEnabled(logger))
			logger.info("Resumed app");
		
		if(exposedServices != null)
			displayDetailedView();
	}

	@Override
	protected void onStart() {
		super.onStart();
		if(AppLogger.isInfoEnabled(logger))
			logger.info("Started app");
	}

	@Override
	protected void onPause() {
		super.onPause();
		if(AppLogger.isInfoEnabled(logger))
			logger.info("Paused app");
		if(drawGraphTask != null && drawGraphTask.getStatus() != AsyncTask.Status.FINISHED) {
			drawGraphTask.cancel(true);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// Unbind on destroy so that we don't leak service connections
		if (serviceBindCallback != null) {
			this.unbindService(serviceBindCallback);
		}
		if(AppLogger.isInfoEnabled(logger))
			logger.info("Destroyed app");
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				// Get the device MAC address
				String address = data.getExtras().getString(
						DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				exposedServices.connectSensor(address);
			}
			break;
		}
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		setContentView(R.layout.detailed);
		
		graphView = (AQIGraphView) findViewById(R.id.graphView);
		graphViewMask = (ImageView) findViewById(R.id.graphViewMask);
		
		if(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
			tableLayout = (TableLayout) findViewById(R.id.pollutantTableLayout);
			displayDetailsList();
		}
		// If null, hasnt finished loading data
		// Will get filled in when ready
		if(!isGraphDone) {
			graphViewMask.setVisibility(View.VISIBLE);
		} else {
			updateGraphValues(graphValues);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.option_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent serverIntent;
		switch (item.getItemId()) {
		case R.id.scan:
			// Launch the DeviceListActivity to see devices and do scan
			serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
			return true;
		case R.id.celalAct:
			serverIntent = new Intent(this, Main.class);
			startActivity(serverIntent);
			return true;
		}

		return super.onOptionsItemSelected(item);
	}
}