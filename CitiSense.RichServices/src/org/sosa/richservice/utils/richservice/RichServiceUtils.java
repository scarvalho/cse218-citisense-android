package org.sosa.richservice.utils.richservice;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.UUID;

import org.sosa.richservice.Message;
import org.sosa.richservice.MessageAddressed;
import org.sosa.richservice.MessageError;
import org.sosa.richservice.MessageRequest;
import org.sosa.richservice.base.MessageErrorBase;

/**
 * 
 * @author celal.ziftci
 * 
 */
public class RichServiceUtils {

	public static String generateMessageId() {
		return UUID.randomUUID().toString();
	}

	public static String throwableToString(Throwable t) {
		StringBuffer message = new StringBuffer();
		if (t.getMessage() != null) {
			message.append(t.getMessage());
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		t.printStackTrace(ps);

		message.append("\n").append(baos.toString());
		return message.toString();
	}

	public static Method findMethodOn(Class cls, String methodName,
			Class[] paramTypes) {
		try {
			return cls.getMethod(methodName, paramTypes);
		} catch (SecurityException e) {
			throw new RuntimeException("Security exception getting method '"
					+ methodName + "' on class '" + cls.getName(), e);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("No method '" + methodName
					+ "' on class '" + cls.getName(), e);
		}
	}

	public static Method findMethodOn(Class cls, MessageRequest msg) {
		return findMethodOn(cls, msg.getOperation(), msg
				.getOperationParameterTypes());
	}

	public static MessageError errorResponseFor(Message msg,
			String exceptionMessage, Throwable t) {
		if (msg instanceof MessageAddressed) {
			MessageAddressed addressed = (MessageAddressed) msg;
			return new MessageErrorBase(addressed.getDestination(), addressed
					.getSource(), RichServiceUtils.generateMessageId(),
					addressed.getMessageId(), exceptionMessage, t);
		} else {
			// FIXME: It may not be awesome to have a null message source!
			return new MessageErrorBase(null, msg.getSource(), RichServiceUtils
					.generateMessageId(), msg.getMessageId(), exceptionMessage,
					t);
		}

	}
}
