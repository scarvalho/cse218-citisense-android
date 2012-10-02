package org.sosa.richservice;

/**
 * Marker interface for ServiceDataConnector explained in RichServices vision
 * (see {@link https://sosa.ucsd.edu/ResearchCentral/download.jsp?id=159}).
 * 
 * @author celal.ziftci
 * 
 */
public interface ServiceDataConnector<T extends ServiceDescriptor> {
	/**
	 * Return the service that this connector exposes to the outside world.
	 * 
	 * @return the service that this connector exposes to the outside world.
	 */
	public T getService();

	public void setService(T service);

	/**
	 * Returns the {@link MessageBus} this connector communicates with.<br/>
	 * This connector knows how to create proper messages to talk to the bus
	 * (serialization, deserialization etc.)
	 * 
	 * @return the {@link MessageBus} this connector communicates with.
	 */
	public MessageBus getMessageBus();

	public void setMessageBus(MessageBus bus);

	/**
	 * Receive a message, and act accordingly.<br/>
	 * The message received can be anything defined in {@link MessageType}, and
	 * this connector will reply with either an OK, an exception, or a response,
	 * which will also be message driven.
	 * 
	 * @param msg
	 */
	public void receiveMessage(Message msg);

	/**
	 * Sends a message to be put on the bus, and returns immediately without
	 * waiting for a response.
	 * 
	 * @param msg
	 * @param waitForResponseAmount
	 * @return
	 */
	public void sendMessage(Message msg);

	/**
	 * The service that is being exposed to has made the service request called
	 * {@code request}. Handle the call.<br/>
	 * Note that, this method should never throw an exception, but put an error
	 * message onto the bus instead. This way, the message bus knows whom to
	 * send the error message (hence the exception).
	 * 
	 * @param service
	 * @param operation
	 * @param parameterValues
	 */
	public Object sendMessage(Message msg, int waitForResponseAmount);
}
