package org.citisense.android;

public class Range implements Comparable<Range> {

	// Comparison uses Double.compare, due to NaN
	private double lo, hi;
	
	public Range(double a, double b)
	{
		if (Double.compare(a, b) >= 0)
		{
			hi = a;
			lo = b;
		} else {
			hi = b;
			lo = a;
		}
	}
	
	public double Hi(){ return hi; }
	public double Lo(){ return lo; }
	
	public double Delta()
	{
		if (Double.valueOf(lo).isNaN() || Double.valueOf(hi).isNaN())
		{
			return Double.MAX_VALUE;
		}
		else
		{
			return hi - lo;
		}
	}
	
	public boolean Contains(double value)
	{ 
		return (Double.compare(lo, value) <= 0) && (Double.compare(value, hi) < 0);
	}
	
	@Override
	public int compareTo(Range other) {
		if( other != null && Double.compare(other.lo, this.lo) == 0 && Double.compare(other.hi, this.hi) == 0 )
		{
			return 0;
		}
		else
		{
			return -1;
		}			
	}

	@Override
	public boolean equals(Object o) {
		if (! (o instanceof Range))
		{
			return false;
		}
		else
		{
			return compareTo((Range)o) == 0;
		}
	}
	
	@Override
	public int hashCode() {

		// SDH: note that we're implementing hashcode and equals in order to get HashMap to work 
		// properly on Range objects
		
		int result = 17;

		long loBits = Double.doubleToLongBits(lo);
		result = 31 * result + (int) (loBits ^ (loBits >>> 32));

		long hiBits = Double.doubleToLongBits(hi);
		result = 31 * result + (int) (hiBits ^ (hiBits >>> 32));

		return result;
	}

}
