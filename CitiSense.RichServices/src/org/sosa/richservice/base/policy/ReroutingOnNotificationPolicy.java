package org.sosa.richservice.base.policy;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sosa.richservice.InterceptionResult;
import org.sosa.richservice.Message;
import org.sosa.richservice.MessageNotification;
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
public class ReroutingOnNotificationPolicy implements Policy {

	Logger logger = LoggerFactory
			.getLogger(ReroutingOnNotificationPolicy.class);

	private String name = "ReroutingPolicy";
	private boolean enabled = true;

	private static class Pair<T1, T2> {
		T1 one;
		T2 two;

		Pair(T1 one, T2 two) {
			this.one = one;
			this.two = two;
		}
	}

	// This is where we keep the info on whether we are interested in a
	// notification
	// This is where we keep the info on how to do the rerouting
	private Map<String, Pair<String, String>> addToRerouteTable = new HashMap<String, Pair<String, String>>();
	private Map<String, Pair<String, String>> removeFromRerouteTable = new HashMap<String, Pair<String, String>>();
	private Map<String, String> reroutingTable = new HashMap<String, String>();
	private Map<String, String> serviceCorrelationMap = new ConcurrentHashMap<String, String>();

	@Override
	public InterceptionResult interceptMessage(Message msg) {
		if (msg instanceof MessageNotification) {
			// Alter the routing table
			MessageNotification notification = (MessageNotification) msg;
			String key = notification.getTopic() + ":"
					+ notification.getContents();
			Pair<String, String> add = addToRerouteTable.get(key);
			Pair<String, String> remove = removeFromRerouteTable.get(key);
			if (add != null) {
				reroutingTable.put(add.one, add.two);
				logger.trace(
						"Messages will be rerouted to '{}' instead of '{}'.",
						add.two, add.one);
			} else if (remove != null) {
				reroutingTable.remove(remove.one);
				logger
						.trace(
								"Rerouting of messages to '{}' instead of '{}' is stopped.",
								remove.two, remove.one);
			}
		} else if (msg instanceof MessageRequest) {
			MessageRequest request = (MessageRequest) msg;
			String destination = request.getDestination();

			// Is this asked to reroute messages to a specific destination?
			String newDestination = reroutingTable.get(destination);
			if (newDestination != null && isEnabled()) {
				// The message with this msgId was originally sent to this
				// destination...
				logger.trace(
						"Rerouting message destined to '{}' to '{}' instead.",
						destination, newDestination);
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

	public void addRoutingRule(String activatingNotificationTopic,
			String activatingNotificationContent,
			String deactivatingNotificationTopic,
			String deactivatingNotificationContent, String fromService,
			String toService) {
		Pair<String, String> pair = new Pair<String, String>(fromService,
				toService);
		addToRerouteTable.put(activatingNotificationTopic + ":"
				+ activatingNotificationContent, pair);
		removeFromRerouteTable.put(deactivatingNotificationTopic + ":"
				+ deactivatingNotificationContent, pair);
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
