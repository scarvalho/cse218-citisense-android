package org.sosa.richservice.base;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author celal.ziftci
 * 
 */
public class MessageCorrelator<K, V> {

	private static class ResponseHolder<V> {
		final Semaphore waiter = new Semaphore(0);
		V response;
	}

	private final Logger logger = LoggerFactory
			.getLogger(MessageCorrelator.class);

	private Map<K, ResponseHolder<V>> map = new ConcurrentHashMap<K, ResponseHolder<V>>();

	public void prepareForRequest(K key) {
		ResponseHolder<V> holder = new ResponseHolder<V>();
		map.put(key, holder);
	}

	/**
	 * Blocks the calling thread until a response comes.
	 * 
	 * @param key
	 * @return
	 * @throws Exception
	 */
	public V waitForResponse(K key) throws InterruptedException {
		// ResponseHolder<V> holder = new ResponseHolder<V>();
		// map.put(key, holder);
		ResponseHolder<V> holder = map.get(key);
		if (holder == null) {
			throw new RuntimeException(
					"You should call prepareForRequest first, and then wait for a response.");
		}
		logger.trace(Thread.currentThread().getName()
				+ " waiting on semaphore for key '" + key + "'");

		holder.waiter.acquire();
		logger.trace(Thread.currentThread().getName()
				+ " got the response for the key '" + key + "'");
		map.remove(key);

		return holder.response;
	}

	/**
	 * 
	 * @param key
	 * @param response
	 */
	public void responseReceived(K key, V response) {
		ResponseHolder<V> holder = map.get(key);
		if (holder == null) {
			if (logger.isErrorEnabled())
				logger
						.error(
								"No entry was found for key: {}. This means nobody was waiting for a response on this key on this bus. Ignoring it here, but keep in mind that this can cause blocking in some other part of the system.",
								key);
		} else {
			holder.response = response;
			logger.trace(Thread.currentThread().getName()
					+ " releasing semaphore for '" + key + "'");
			holder.waiter.release();
		}
	}
}
