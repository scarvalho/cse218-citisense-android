import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Test {
	private static final DecimalFormat latitudeFormat = new DecimalFormat("###.###");
	public static void main(String[] args) {
		DecimalFormat latitudeFormat = new DecimalFormat("###.###");
		double d = Double.parseDouble(latitudeFormat.format(13.34523487));
		System.out.println(d);
		
		SimpleDateFormat dateFormatter = new SimpleDateFormat("M/d/yyyy h:mm:ss a");
		System.out.println(dateFormatter.format(Calendar.getInstance().getTime()));
		System.out.println(Calendar.getInstance().getTimeInMillis());
	}
}
