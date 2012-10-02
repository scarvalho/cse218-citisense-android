package org.sosa.richservice.base;

import java.util.Arrays;
import java.util.Collection;

import org.sosa.richservice.InterceptionResult;
import org.sosa.richservice.Message;

public class InterceptionResultBase implements InterceptionResult {

	private final Message intercepted;
	private final Collection<? extends Message> extra;

	public InterceptionResultBase(Message intercepted, Message... extraMessages) {
		// TODO: Null checks
		this.intercepted = intercepted;
		extra = Arrays.asList(extraMessages);
	}

	@Override
	public Collection<? extends Message> extraMessages() {
		return extra;
	}

	@Override
	public Message interceptedMessage() {
		return intercepted;
	}
}
