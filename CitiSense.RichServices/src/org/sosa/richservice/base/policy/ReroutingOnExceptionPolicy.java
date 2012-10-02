package org.sosa.richservice.base.policy;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sosa.richservice.InterceptionResult;
import org.sosa.richservice.Message;
import org.sosa.richservice.MessageRequest;
import org.sosa.richservice.MessageResponse;
import org.sosa.richservice.Policy;
import org.sosa.richservice.base.InterceptionResultBase;
import org.sosa.richservice.base.MessageErrorBase;
import org.sosa.richservice.base.MessageRequestBase;

/**
 * 
 * FIXME: We need to add this an option to say which exceptions cause a
 * rerouting.
 * 
 * @author celal.ziftci
 * 
 */
public class ReroutingOnExceptionPolicy implements Policy {

	private String name = "ReroutingOnExceptionPolicy";
	Logger logger = LoggerFactory.getLogger(ReroutingOnExceptionPolicy.class);

	private boolean isEnabled = true;

	private Map<String, MessageRequest> requestMessages = new HashMap<String, MessageRequest>();
	// This is where we keep the info on how to do the rerouting
	private Map<String, String> reroutingTable = new HashMap<String, String>();

	@Override
	public InterceptionResult interceptMessage(Message msg) {

		if (!isEnabled()) {
			return new InterceptionResultBase(msg);
		}

		// Remember request messages in case we need to get info about them
		// later on
		if (msg instanceof MessageRequest) {
			requestMessages.put(msg.getMessageId(), (MessageRequest) msg);
		} else if (msg instanceof MessageResponse) {
			MessageRequest originalRequest = requestMessages
					.remove(((MessageResponse) msg).getCorrelationId());

			// If an error message is sent back to a request, and if this is due
			// to
			// connectivity issues, then fwd it to a local service.
			// Original request message: src1 -> dst1 & msgID(1)
			// Error message: dst1 -> src1 & msgID(2) & corrID(1)
			// New request message: src1 -> dst2 & msgID(3)
			// New response message: dst2 -> src1 & msgID(4) & corrID(3)
			// Updated original response: dst1 -> src1 & msgID(4) & corrID(1)
			// TODO: Currently, all I do is fwd the message to the new service
			// and expect it to respond. I'm not re-writing any source or
			// destination information on the response.
			// It is not clear to me if I should do this, i.e. if we need to be
			// transparent, so leaving it to later.
			if (msg instanceof MessageErrorBase) {
				MessageErrorBase errorResponse = (MessageErrorBase) msg;
				// TODO: Check if the exception is one to be intercepted!
				String source = errorResponse.getSource();
				String newDestination = reroutingTable.get(source);
				if (newDestination != null) {
					logger
							.trace(
									"Due to an error, rerouting a request message to '{}' instead of '{}'",
									newDestination, originalRequest
											.getDestination());
					MessageRequest newRequest = new MessageRequestBase(
							originalRequest.getSource(), newDestination,
							originalRequest.getMessageId(), originalRequest
									.getOperation(), originalRequest
									.getOperationParameterTypes(),
							originalRequest.getOperationParameterValues());

					return new InterceptionResultBase(newRequest);
				}
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
		return isEnabled;
	}

	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}
}
