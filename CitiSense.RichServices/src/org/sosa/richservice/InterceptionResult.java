package org.sosa.richservice;

import java.util.Collection;

public interface InterceptionResult {

	public Message interceptedMessage();

	public Collection<? extends Message> extraMessages();
}
