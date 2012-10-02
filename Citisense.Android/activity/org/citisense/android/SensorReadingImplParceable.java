//package org.citisense.android;
//
//import org.citisense.datastructure.Location;
//import org.citisense.datastructure.SensorType;
//import org.citisense.datastructure.impl.SensorReadingImpl;
//
//import android.os.Parcel;
//import android.os.Parcelable;
//
//// Implemented based off of
////	http://stackoverflow.com/questions/2139134/how-to-send-an-object-from-one-android-activity-to-another-with-intent-putextra
//public class SensorReadingImplParceable extends SensorReadingImpl implements
//		Parcelable {
//	private static final long serialVersionUID = -3780237547125428981L;
//
//	public SensorReadingImplParceable(SensorType sensorType, String data,
//			String units, String timeDate, Location location) {
//		super(sensorType, data, units, timeDate, location);
//		// TODO Auto-generated constructor stub
//	}
//
//	@Override
//	public int describeContents() {
//		// TODO Supposedly not needed
//		return 0;
//	}
//
//	@Override
//	public void writeToParcel(Parcel dest, int flags) {
//		// TODO Auto-generated method stub
//		dest.writeString(getSensorType().toString());
//		dest.writeString(getSensorData());
//		dest.writeString(getSensorUnits());
//		dest.writeString(getTimeDateAsString());
//		// for now just serialize the location right here, later maybe create a parcable location
//		Location location = getLocation();
//		dest.writeDouble(location.getLatitude());
//		dest.writeDouble(location.getLongitude());
//		dest.writeDouble(location.getAltitude());
//		dest.writeLong(location.getTime());
//		// TODO serialize extra information
//	}
//	
//	public static final Parcelable.Creator<SensorReadingImplParceable> CREATOR = new Parcelable.Creator<SensorReadingImplParceable>() {
//
//		@Override
//		public SensorReadingImplParceable createFromParcel(Parcel source) {
//			return null;
//		}
//
//		@Override
//		public SensorReadingImplParceable[] newArray(int size) {
//			// TODO Auto-generated method stub
//			return null;
//		}
//	};
//
//}
