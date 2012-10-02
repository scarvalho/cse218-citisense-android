package org.sosa.richservice.base.policy;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.sosa.richservice.InterceptionResult;
import org.sosa.richservice.Message;
import org.sosa.richservice.MessageRequest;
import org.sosa.richservice.MessageResponse;
import org.sosa.richservice.Policy;
import org.sosa.richservice.base.InterceptionResultBase;
import org.sosa.richservice.base.MessageRequestBase;
import org.sosa.richservice.base.MessageResponseBase;

/**
 * 
 * @author celal.ziftci
 * 
 */
public class ReroutingPolicy implements Policy {

	private String name = "ReroutingPolicy";
	private boolean enabled = true;

	// This is where we keep the info on how to do the rerouting
	private Map<String, String> reroutingTable = new HashMap<String, String>();
	private Map<String, String> serviceCorrelationMap = new ConcurrentHashMap<String, String>();

	@Override
	public InterceptionResult interceptMessage(Message msg) {
		// intercept all requests to storage service and send them to
		// cloud instead
		if (msg instanceof MessageRequest) {
			MessageRequest request = (MessageRequest) msg;
			String destination = request.getDestination();

			// Is this asked to reroute messages to a specific destination?
			String newDestination = reroutingTable.get(destination);
			if (newDestination != null && isEnabled()) {
				// The message with this msgId was originally sent to this
				// destination...
				serviceCorrelationMap.put(request.getMessageId(), destination);

				return new InterceptionResultBase(new MessageRequestBase(
						request.getSource(), newDestination, request
								.getMessageId(), request.getOperation(),
						request.getOperationParameterTypes(), request
								.getOperationParameterValues()));
			}
		}
		// intercept all responses from cloud storage service and send
		// them to requested instead, acting like regular storage
		// service has been invoked
		else if (msg instanceof MessageResponse) {
			MessageResponse response = (MessageResponse) msg;

			// Check if we had re-routed the request of this response
			// earlier
			String destinationToCorrelate = this.serviceCorrelationMap
					.remove(response.getCorrelationId());
			if (destinationToCorrelate != null) {
				return new InterceptionResultBase(new MessageResponseBase(
						destinationToCorrelate, response.getDestination(),
						response.getMessageId(), response.getCorrelationId(),
						response.getResponse()));
			}

		}

		return new InterceptionResultBase(msg);
	}

	public Map<String, String> getReroutingTable() {
		return reroutingTable;
	}

	public void setReroutingTable(Map<String, String> reroutingTable) {
		this.reroutingTable = reroutingTable;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
