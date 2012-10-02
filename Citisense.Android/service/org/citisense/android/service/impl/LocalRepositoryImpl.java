package org.citisense.android.service.impl;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.citisense.android.service.LocalRepository;
import org.citisense.datastructure.Location;
import org.citisense.datastructure.SensorReading;
import org.citisense.datastructure.SensorType;
import org.citisense.datastructure.impl.LocationImpl;
import org.citisense.datastructure.impl.SensorReadingImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class LocalRepositoryImpl implements LocalRepository {
	private static final Logger logger = LoggerFactory
			.getLogger(LocalRepositoryImpl.class);
	private static final String DATABASE_NAME = "sensorData.db";
	private static final int DATABASE_VERSION = 19;
	private static final String DATABASE_PATH = "/sdcard/" + DATABASE_NAME;

	private SQLiteDatabase db;

	private Map<SensorType, SensorReading> latestReadingsCache = new HashMap<SensorType, SensorReading>();
	
	public LocalRepositoryImpl() {
		openDatabase();
	}
	
	@Override
	public void openDatabase() {
		// if already open, return
		if(this.db != null && isDatabaseOpen()) {
			return;
		}
		// Open (and create if necessary) a database
		// on the sdcard
		this.db = SQLiteDatabase
				.openDatabase(
						DATABASE_PATH,
						null,
						(SQLiteDatabase.CREATE_IF_NECESSARY | SQLiteDatabase.OPEN_READWRITE));

		// If database is already of the right version...
		if (db.getVersion() == DATABASE_VERSION) {
			// Check to see what is the oldest reading remaining to be uploaded
			return;
		}

		// Else create all required tables
		for (SensorType sensorType : SensorType.values()) {
			// Temporarily drop the database tables and recreate...
//					try {
//						db.execSQL("DROP TABLE " + sensorType.name());
//					} catch (Exception e) {
//					if(AppLogger.isWarnEnabled(logger))
//						logger.warn("Failed drop table! - " + e);
//					}
			try {
				String sensorName = sensorType.name();
				db.execSQL("DROP TABLE IF EXISTS " + sensorName);
				db.execSQL("CREATE TABLE " + sensorName
						+ "(id INTEGER PRIMARY KEY, " + "value REAL, "
						+ "unit TEXT, " + "timestamp INTEGER UNIQUE, "
						+ "location TEXT);");
				db.execSQL("CREATE INDEX " + sensorName + "_timestamp_idx ON " + sensorName
						+ "(timestamp asc);");
			} catch (SQLException e) {
				if(AppLogger.isWarnEnabled(logger)) logger.warn("Failed create! - " + e);
			}
		}
		db.setVersion(DATABASE_VERSION);
	}
	
	@Override
	public void closeDatabase() {
		if(db != null && isDatabaseOpen()) {
			db.close();
		}
	}
	
	@Override
	public boolean isDatabaseOpen() {
		if(db != null) {
			return db.isOpen();
		}
		return false;
	}

	/**
	 * If there is no data, returns null.
	 */
	@Override
	public SensorReading getLastReading(SensorType sensorType) {
		
		try {
			if(latestReadingsCache.containsKey(sensorType)){
				return latestReadingsCache.get(sensorType);
			}
		} catch (Exception cachEx) {
			if(AppLogger.isWarnEnabled(logger))logger.warn("Exception during cache hit", cachEx);
		}
		
		SensorReading ret;
		double data;
		String units;
		Location<Object> location;

		// Check to ensure database is already open
		if(this.db == null || !isDatabaseOpen()) {
			openDatabase();
		}
		
		// SELECT id, value, unit, timestamp FROM sensorType ORDER BY id desc
		// LIMIT1
		Cursor cursor = db.query(sensorType.name(), new String[] { "id",
				"value", "unit", "timestamp", "location" }, null, null, null,
				null, "id desc", "1");
		try {

			if (cursor.moveToFirst()) {
				data = cursor.getDouble(1);
				units = cursor.getString(2);
				long timeEpoch = cursor.getLong(3);
				// timeDate = cursor.getString(3);

				try {
					location = LocationImpl.fromString(cursor.getString(4));
				} catch (ParseException e) {
					logger
							.error("Failed to parse location from database string - "
									+ e);
					location = Location.UNKNOWN;
				}
				if (AppLogger.isDebugEnabled(logger)) logger.debug("Fetched from table " + sensorType + ": " + data
						+ "," + units + "," + timeEpoch);

				ret = new SensorReadingImpl(sensorType, data, units, timeEpoch,
						location);

			} else {
				ret = null;
			}
			return ret;
		} finally {
			try {
				cursor.close();
			} catch (Exception e) {
				// Ignore...
			}
		}
	}

	@Override
	public void storeSensorReading(SensorReading sensorReading) {
		SensorType sensor = sensorReading.getSensorType();
		double value = sensorReading.getSensorData();
		String units = sensorReading.getSensorUnits();
		// String timestamp = sensorReading.getTimeDateAsString();
		long timestamp = sensorReading.getTimeMilliseconds();
		String location = sensorReading.getLocation().toString();
		
		//TODO: if this is the max of the hour, store this in an arraylist?

		if (AppLogger.isDebugEnabled(logger)) logger.debug("Storing into table " + sensor.name() + ": " + value + ","
				+ units + "," + timestamp + "," + location);

		String query = "INSERT INTO " + sensor.name()
				+ " (value, unit, timestamp, location) VALUES (" + value
				+ ", \"" + units + "\", \"" + timestamp + "\", \"" + location
				+ "\");";
		// Check to ensure database is already open
		if(this.db == null || !isDatabaseOpen()) {
			openDatabase();
		}
		
		try {
			db.execSQL(query);
		} catch (SQLException e) {
			if(AppLogger.isErrorEnabled(logger))
				logger.error("Failed insert! - " + e);
		}
		
		try {
			// assumes that each sensor reading coming in is newer than the previous one
			latestReadingsCache.put(sensorReading.getSensorType(), sensorReading);
		} catch (Exception cacheEx) {
			if(AppLogger.isWarnEnabled(logger))logger.warn("Exception during cache hit", cacheEx);
		}
	}

	/**
	 * If there is no data, returns an empty collection.
	 */
	@Override
	// TODO: Should consider adding a time bound so we can detect when the last readings are old
	public Collection<SensorReading> getLastReadingsForAllSensorTypes() {
		Collection<SensorReading> ret = new ArrayList<SensorReading>();
		for (SensorType sensorType : SensorType.values()) {
			SensorReading reading = getLastReading(sensorType);
			if (reading != null) {
				ret.add(reading);
			}
		}

		return ret;
	}
	
	public SensorReading getMaxReadingForSensorDuring(SensorType sensorType, long startTime, long endTime) {
		SensorReading ret = null;
		
		String units;
		double data;
		Location<Object> location;
		
		// - 1 and +1 so it includes the bounds
		long startT = startTime - 1;
		long endT = endTime + 1;

//		String query = "SELECT id,MAX(value),unit,timestamp,location FROM "
//			+ sensorType.name() + " WHERE timestamp BETWEEN " + startT + " AND " + endT;

		String query = "SELECT id,MAX(value),unit,timestamp,location FROM "
			+ "(SELECT id,value,unit,timestamp,location FROM " + sensorType.name()
			+ " WHERE timestamp BETWEEN " + startT + " AND " + endT + ")";
		
//		String query = "SELECT id,MAX(value),unit,timestamp,location FROM "
//			+ sensorType.name();
		
		// Check to ensure database is already open
		if(this.db == null || !isDatabaseOpen()) {
			openDatabase();
		}
		
		Cursor cursor = db.rawQuery(query, null);
		
		try {
			if(cursor.moveToFirst()) {
				data = cursor.getDouble(1);				
				units = cursor.getString(2);
				
				// If no such units exists in the table, it seems to return null
				if(units == null)
					return null;
				
				long timeEpoch = cursor.getLong(3);
				
				try {
					location = LocationImpl.fromString(cursor.getString(4));
				} catch (ParseException e) {
					logger
							.error("Failed to parse location from database string - "
									+ e);
					location = Location.UNKNOWN;
				}
				ret = new SensorReadingImpl(sensorType, data, units, timeEpoch, location);
			}
		} finally {
			try {
				if(cursor != null && !cursor.isClosed()) {
					cursor.close();
				}
			} catch (Exception e) {
				// ignore
			}
		}
		
		return ret;
	}
	
	public Collection<SensorReading> getOldestReadingForAllSensors() {
		Collection<SensorReading> ret = new ArrayList<SensorReading>();
		
		for(SensorType sensorType : SensorType.values()) {
			SensorReading s = getOldestReading(sensorType);
			if(s != null)
				ret.add(s);
		}
		
		return ret;
	}
	
	public SensorReading getOldestReading(SensorType sensorType) {
		SensorReading ret;
		double data;
		String units;
		Location<Object> location;
		
		// Check to ensure database is already open
		if(this.db == null || !isDatabaseOpen()) {
			openDatabase();
		}

		// SELECT id, value, unit, timestamp FROM sensorType ORDER BY id desc
		// LIMIT1
		Cursor cursor = db.query(sensorType.name(), new String[] { "id",
				"value", "unit", "timestamp", "location" }, null, null, null,
				null, "id asc", "1");
		try {

			if (cursor.moveToFirst()) {
				data = cursor.getDouble(1);
				units = cursor.getString(2);
				long timeEpoch = cursor.getLong(3);
				// timeDate = cursor.getString(3);

				try {
					location = LocationImpl.fromString(cursor.getString(4));
				} catch (ParseException e) {
					logger
							.error("Failed to parse location from database string - "
									+ e);
					location = Location.UNKNOWN;
				}
				if (AppLogger.isDebugEnabled(logger)) logger.debug("Fetched from table " + sensorType + ": " + data
						+ "," + units + "," + timeEpoch);

				ret = new SensorReadingImpl(sensorType, data, units, timeEpoch,
						location);

			} else {
				ret = null;
			}
			return ret;
		} finally {
			try {
				cursor.close();
			} catch (Exception e) {
				// Ignore...
			}
		}
	}

	public Collection<SensorReading> getMaxReadingsDuring(long startTime,
			long endTime) {
		Collection<SensorReading> ret = new ArrayList<SensorReading>();

		for (SensorType sensorType : SensorType.values()) {
			ret.add(getMaxReadingForSensorDuring(sensorType, startTime, endTime));
		}

		return ret;
	}

	public Collection<SensorReading> getReadingsByTypeDuring(SensorType sensorType, long startTime,
			long endTime) {

		// get all the readings of sensorType from the database during the time range
		Collection<SensorReading> ret = getRangeReadingsForSensorSorted(
				sensorType, startTime, endTime, "timestamp asc");
		
		if(ret == null)
			ret = new ArrayList<SensorReading>();
		
		return ret;
	}
	
	public Collection<Collection<SensorReading>> getReadingsDuring(
			long startTime, long endTime) {
		Collection<Collection<SensorReading>> ret =
							new ArrayList<Collection<SensorReading>>();

		// you can tell the sensor type because type is stored in every
		// SensorReading object
		for (SensorType sensorType : SensorType.values()) {
			Collection<SensorReading> rangeReadings = getRangeReadingsForSensorSorted(
					sensorType, startTime, endTime, "timestamp asc");
			if (rangeReadings == null || rangeReadings.size() == 0) {
				continue;
			}	
			ret.add(rangeReadings);
		}

		return ret;
	}

	// allows existing code to still act the same while allowing getReadingsDuring
	// to use a different sort
	public Collection<SensorReading> getRangeReadingsForSensor(
			SensorType sensorType, long startTime, long endTime) {
		// default
		return getRangeReadingsForSensorSorted(sensorType, startTime, endTime,
				"id asc");
	}
	
	public Collection<SensorReading> getRangeReadingsForSensorSorted(
			SensorType sensorType, long startTime, long endTime, String sort) {

		Collection<SensorReading> ret = new ArrayList<SensorReading>();
		double data;
		String units;
		Location<Object> location;
		// String startT = SensorReadingImpl.formatCalendarString(startTime);
		// String endT = SensorReadingImpl.formatCalendarString(endTime);
		// - 1 and +1 so it includes the bounds
		// TODO: check if will break Aqi_ui
		long startT = startTime - 1;
		long endT = endTime + 1;

		String args = "timestamp < \"" + endT + "\" AND timestamp > \""
				+ startT + "\"";
		
		// Check to ensure database is already open
		if(this.db == null || !isDatabaseOpen()) {
			openDatabase();
		}
		
		Cursor cursor = db.query(sensorType.name(), new String[] { "id",
				"value", "unit", "timestamp", "location" }, args, null, null,
				null, sort);

		try {
			if (cursor.moveToFirst()) {

				do {
					data = cursor.getDouble(1);
					units = cursor.getString(2);
					long timeEpoch = cursor.getLong(3);
					// timeDate = cursor.getString(3);
					
					try {
						location = LocationImpl.fromString(cursor.getString(4));
					} catch (ParseException e) {
						logger
								.error("Failed to parse location from database string - "
										+ e);
						location = Location.UNKNOWN;
					}
					// TODO: can add to index 0 to reverse the order
					ret.add(new SensorReadingImpl(sensorType, data, units,
							timeEpoch, location));
				} while (cursor.moveToNext());
			} else {
				ret = null;
			}
		} finally {
			try {
				if (cursor != null && !cursor.isClosed()) {
					cursor.close();
				}
			} catch (Exception e) {
				// Ignore...
			}
		}
		return ret;
	}
	
	// Drop all readings between the given range of times for a given SensorType
	public void dropReadingsFromRange(SensorType sensorType, long startTime, long endTime) {
		
		long startT = startTime - 1;
		long endT = endTime + 1;

		String args = "timestamp < \"" + endT + "\" AND timestamp > \""
				+ startT + "\"";
		
		String query = "DELETE FROM " + sensorType.name()
				+ " WHERE " + args;
		
		// Check to ensure database is already open
		if(this.db == null || !isDatabaseOpen()) {
			openDatabase();
		}
		
		try {
			db.execSQL(query);
		} catch (SQLException e) {
			if(AppLogger.isErrorEnabled(logger))
			 	logger.error("Failed to drop old readings! - " + e);
		}
	}

	 // Drops the count oldest readings from desired table
	 public void dropOldReadings(SensorType sensorType, int count) {
		 String query = "DELETE FROM " + sensorType.name()
		 	+ " WHERE `id` IN (SELECT `id` FROM " + sensorType.name()
		 	+ " ORDER BY `id` ASC LIMIT " + count + ")";
		 
		// Check to ensure database is already open
		if(this.db == null || !isDatabaseOpen()) {
			openDatabase();
		}
			
		 try {
			 db.execSQL(query);
		 } catch (SQLException e) {
			 if(AppLogger.isErrorEnabled(logger))
			 	logger.error("Failed to drop old readings! - " + e);
		 }
	 }
	
	 // Drops the count oldest readings from all sensor tables
	 public void dropOldReadingsForAllSensorTypes(int count) {
		 for (SensorType sensorType : SensorType.values()) {
			 dropOldReadings(sensorType, count);
		 }
	 }
	 
	 // Drop all readings for the given sensor type
	 public void dropAllReadings(SensorType sensorType) {
		 String query = "DELETE FROM " + sensorType.name();
		 
		// Check to ensure database is already open
		if(this.db == null || !isDatabaseOpen()) {
			openDatabase();
		}
		
		 try {
			 db.execSQL(query);
		 } catch (SQLException e) {
			 if(AppLogger.isErrorEnabled(logger))
			 	logger.error("Failed to drop old readings! - " + e);
		 }
	 }
}