package org.slf4j.impl;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.ILoggerFactory;

/**
 * This class is implemented to fix an issue with Android logging. Android
 * logging expects a logger's tag to be at most 23 characters. But when a
 * front-end like slf4j is used, class-names are used and they can certainly be
 * longer than 23 characters. To get things working, I shorten the tag names if
 * they are longer than 23 characters.
 * <p/>
 * FIXME:<br/>
 * <ul>
 * <li>Currently, if a tag name doesn't fit, I only take the last 23 chars of
 * it. A nicer shortening can be done as shown in the following example: <br>
 * org.sosa.richservice.base.MessageCorrelatorBase -> o.s.r.b.MesCorrelatorBase</li>
 * <li>A better datastructure can be used instead of a HashMap (maybe a
 * ConcurrentHashMap?)</li>
 * </ul>
 * 
 * @author celal.ziftci
 * 
 */
public class AndroidLoggerFactory implements ILoggerFactory {
	private static final int MAX_TAG_LENGTH = 23;

	private final Map<String, AndroidLogger> loggerMap;

	public AndroidLoggerFactory() {
		loggerMap = new HashMap<String, AndroidLogger>();
	}

	/* @see org.slf4j.ILoggerFactory#getLogger(java.lang.String) */
	public AndroidLogger getLogger(String name) {
		name = forceValidTag(name);

		AndroidLogger slogger = null;
		// protect against concurrent access of the loggerMap
		synchronized (this) {
			slogger = loggerMap.get(name);
			if (slogger == null) {
				slogger = new AndroidLogger(name);
				loggerMap.put(name, slogger);
			}
		}
		return slogger;
	}

	protected String forceValidTag(String tag) {
		if (tag.length() > MAX_TAG_LENGTH) {
			// try to do something expected

			// if there's a '.' in the tag, it probably came from a fully
			// qualified class name
			// use the trailing end

			// extra character because we're going to remove the '.'
			String lastPart = tag.substring(tag.length() - MAX_TAG_LENGTH);

			int dot = lastPart.indexOf('.');
			if (dot >= 0 && dot != lastPart.length() - 1) {
				// return as much of the dotted path as will fit
				// return lastPart;
				return lastPart.substring(dot + 1);
			} else {
				// no useful dot location, just take the beginning
				// return tag.substring(0, MAX_TAG_LENGTH);
				return lastPart;
			}
		} else {
			return tag;
		}
	}
}