package org.citisense.utils.thread;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 
 * @author celal.ziftci
 * 
 */
public class DaemonThreadFactory implements ThreadFactory {

	private final AtomicInteger counter = new AtomicInteger(0);
	private final String nameRoot;

	public DaemonThreadFactory(String nameRoot) {
		this.nameRoot = nameRoot;
	}

	@Override
	public Thread newThread(Runnable runnable) {
		Thread thread = new Thread(runnable);
		thread.setDaemon(true);
		thread.setName(nameRoot + "-" + counter.incrementAndGet());

		return thread;
	}

}
