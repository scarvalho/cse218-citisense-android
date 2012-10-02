package org.sosa.richservice.base.policy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sosa.richservice.InterceptionResult;
import org.sosa.richservice.Message;
import org.sosa.richservice.MessageAddressed;
import org.sosa.richservice.MessageError;
import org.sosa.richservice.MessageNotification;
import org.sosa.richservice.MessageRequest;
import org.sosa.richservice.MessageResponse;
import org.sosa.richservice.Policy;
import org.sosa.richservice.base.InterceptionResultBase;

/**
 * 
 * @author celal.ziftci
 * 
 */
public class LoggingPolicy implements Policy {
	private Logger logger = LoggerFactory.getLogger(LoggingPolicy.class);

	@Override
	public String getName() {
		return "LoggingPolicy";
	}

	@Override
	@SuppressWarnings("unchecked")
	public InterceptionResult interceptMessage(Message msg) {
		StringBuffer buffer = new StringBuffer("Message: ").append(
				msg.getClass().getSimpleName()).append("\n");

		if (msg instanceof MessageAddressed) {
			buffer.append("\t from: ").append(msg.getSource()).append("\n");
			buffer.append("\t to: ").append(
					((MessageAddressed) msg).getDestination()).append("\n");
		}

		if (msg instanceof MessageRequest) {
			MessageRequest request = (MessageRequest) msg;

			buffer.append("\t operation: ").append(request.getOperation())
					.append("(");
			Class[] paramTypes = request.getOperationParameterTypes();
			for (Class class1 : paramTypes) {
				buffer.append(class1.getSimpleName()).append(", ");
			}
			buffer.append(")\n");

			buffer.append("\t values: ");
			Object[] paramValues = request.getOperationParameterValues();
			for (Object obj : paramValues) {
				buffer.append(obj).append(", ");
			}
			buffer.append("\n");
		} else if (msg instanceof MessageError) {
			MessageError error = (MessageError) msg;
			buffer.append("\t error: ").append(error.getErrorMessage()).append(
					"\n");
		} else if (msg instanceof MessageResponse) {
			MessageResponse reponse = (MessageResponse) msg;
			buffer.append("\t response: ").append(reponse.getResponse())
					.append("\n");
		} else if (msg instanceof MessageNotification) {
			MessageNotification not = (MessageNotification) msg;
			buffer.append("\t topic: ").append(not.getTopic()).append("\n");
			buffer.append("\t contents: ").append(not.getContents()).append(
					"\n");
		}
		logger.debug(buffer.toString());
		return new InterceptionResultBase(msg);
	}

}
