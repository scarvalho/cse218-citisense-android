package org.citisense.android;

import java.util.HashMap;

public class AqiBreakpointTable {
	private String m_pollutantName;
	private HashMap<Range,Range> m_table;	
	
	public String GetPollutantName(){ return m_pollutantName; }
	
	public AqiBreakpointTable(String pollutantName){
		m_pollutantName = pollutantName;
		// SDH: giving a 10 value startup size to ensure allocation
		m_table = new HashMap<Range, Range>(10);
	}
	
	public void AddEntry( Range breakpoints, Range aqiValues ){
		m_table.put(breakpoints, aqiValues);		
	}
	public void AddEntry( double bplo, double bphi, double aqilo, double aqihi){
		Range bp = new Range(bplo, bphi);
		Range aqi = new Range(aqilo, aqihi);
		AddEntry(bp,aqi);
	}
	
	
	public class AqiBreakpointEntry{
		public Range breakpoints;
		public Range aqiValues;
	}
	
	public AqiBreakpointEntry GetEntryByConcentration(double concentration)
	{
		AqiBreakpointEntry entry = new AqiBreakpointEntry();
		for(Range breakpoints : m_table.keySet()){
			if( breakpoints.Contains(concentration) ){
				entry.breakpoints = breakpoints;
				entry.aqiValues = m_table.get(breakpoints);
				break;
			}
		}
		
		return entry;
	}
	
	
}
