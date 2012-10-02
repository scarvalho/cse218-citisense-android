package org.citisense.utils.logging;

import java.io.InputStream;
import java.util.logging.LogManager;

public class JULFromClasspathLoader {

	public JULFromClasspathLoader() {
		System.out.println("Configuring logging...");
		String propertiesFileName = "log.config";
		InputStream inputStream = getClass().getClassLoader()
				.getResourceAsStream(propertiesFileName);
		try {
			LogManager.getLogManager().readConfiguration(inputStream);
			System.out.println("Logging configured successfully from '"
					+ propertiesFileName + "'");
		} catch (Exception e) {
			System.err.println("Unable to configure logging for "
					+ propertiesFileName);
			e.printStackTrace();
		}
	}
}