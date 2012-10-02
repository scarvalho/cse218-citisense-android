package org.sosa.richservice;

import java.util.Collection;

/**
 * Marker interface for a Message Bus explained in RichServices vision (see
 * {@link https://sosa.ucsd.edu/ResearchCentral/download.jsp?id=159}).
 * 
 * @author celal.ziftci
 * 
 */
public interface MessageBus {

	public void deliverMessage(Message msg);

	public void addServiceDataConnector(String serviceName,
			ServiceDataConnector connector);

	public ServiceDataConnector getServiceDataConnector(String serviceName);

	public ServiceDataConnector removeServiceDataConnector(String serviceName);

	public Collection<ServiceDataConnector> getServiceDataConnectors();
}
