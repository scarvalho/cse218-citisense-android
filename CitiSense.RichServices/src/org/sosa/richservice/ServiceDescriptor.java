package org.sosa.richservice;

import java.lang.reflect.Method;
import java.util.Collection;

/**
 * Marker interface for a Service explained in RichServices vision (see
 * {@link https://sosa.ucsd.edu/ResearchCentral/download.jsp?id=159}).
 * 
 * @author celal.ziftci
 * 
 */
public interface ServiceDescriptor {


	public Collection<String> getRequiredServices();

	public void setServiceName(String serviceName);

	public String getServiceName();
	
	public void initInterfaceIterator();
	
	public Class getNextInterface();
	
	public Class getServiceClass(String operation, Class[] parameterTypes);
	
	public Method getServiceMethod(String operation, Class[] parameterTypes);
}
