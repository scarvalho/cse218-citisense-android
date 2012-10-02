package org.citisense.android.profiler.utility;

import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentStopWatch {
	
	private ConcurrentHashMap<Long,StopWatch> map;
	
	public static ConcurrentStopWatch INSTANCE=new ConcurrentStopWatch();

	private ConcurrentStopWatch(){
		map=new ConcurrentHashMap<Long,StopWatch>();
	}
	
	public void start(){
		StopWatch watch=existsStopWatch();
		watch.start();
	}
	
	private static StopWatch existsStopWatch() {
		if(INSTANCE.map.containsKey(Thread.currentThread().getId())){
			return INSTANCE.map.get(Thread.currentThread().getId());
		}else{
			StopWatch watch=new StopWatch();
			INSTANCE.map.put(Thread.currentThread().getId(),watch);
			return watch;
		}
	}

	public void stop(){
		StopWatch watch=existsStopWatch();
		watch.stop();
	}
	
	public long getElapsedTimeMicro() {
		StopWatch watch=existsStopWatch();
		return watch.getElapsedTimeMicro();
	}

	public long getElapsedTimeMilli() {
		StopWatch watch=existsStopWatch();
		return watch.getElapsedTimeMilli();
	}

	public void reset() {
		StopWatch watch=existsStopWatch();
		watch.reset();
	}
}
