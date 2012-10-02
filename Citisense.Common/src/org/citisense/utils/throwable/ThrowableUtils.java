package org.citisense.utils.throwable;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class ThrowableUtils {

	public static boolean containsThrowableInChain(Throwable caught,
			Class<? extends Throwable> toLookFor) {
		if (caught == null) {
			return false;
		} else {
			Throwable currentException = caught;
			Throwable lastException = null;
			while (currentException != null
					&& currentException != lastException) {
				if (toLookFor.isAssignableFrom(currentException.getClass())) {
					return true;
				}
				lastException = currentException;
				currentException = currentException.getCause();
			}
			return false;
		}
	}

	public static String throwableToString(Throwable t) {
		StringBuffer message = new StringBuffer();
		String exMessage = t.getMessage();
		if (message != null) {
			message.append(exMessage);
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		t.printStackTrace(ps);

		message.append("\n").append(baos.toString());
		return message.toString();
	}

	public static void main(String[] args) {
		System.out
				.println(containsThrowableInChain(new Exception(new Exception(
						new RuntimeException())), RuntimeException.class));
	}
}
