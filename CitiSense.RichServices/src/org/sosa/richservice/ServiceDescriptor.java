package org.sosa.richservice;

import java.util.Collection;

/**
 * Marker interface for a Service explained in RichServices vision (see
 * {@link https://sosa.ucsd.edu/ResearchCentral/download.jsp?id=159}).
 * 
 * @author celal.ziftci
 * 
 */
public interface ServiceDescriptor {

	public Collection<Class> getExposedInterface();

	public Collection<String> getRequiredServices();

	public void setServiceName(String serviceName);

	public String getServiceName();
}
