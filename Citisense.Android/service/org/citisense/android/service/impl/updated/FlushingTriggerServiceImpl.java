package org.citisense.android.service.impl.updated;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.citisense.android.CustomExceptionHandler;
import org.citisense.android.service.FlushingTriggerService;
import org.citisense.android.service.impl.AppLogger;
import org.citisense.android.service.impl.ApplicationSettings;
import org.citisense.utils.thread.DaemonThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sosa.richservice.ServiceDescriptorLocal;
import org.sosa.richservice.base.MessageNotificationBase;
import org.sosa.richservice.base.ServiceDescriptorLocalBase;
import org.sosa.richservice.base.servicedataconnector.ServiceDataConnectorJavaLocal;
import org.sosa.richservice.utils.richservice.RichServiceUtils;

public class FlushingTriggerServiceImpl implements FlushingTriggerService {
	private Logger logger = LoggerFactory
			.getLogger(FlushingTriggerServiceImpl.class);

	private final ServiceDataConnectorJavaLocal connector;
	private final ServiceDescriptorLocal descriptor;
	private final ExecutorService executor;
	private final ApplicationSettings applicationSettings = ApplicationSettings
			.instance();

	public FlushingTriggerServiceImpl() {
		connector = new ServiceDataConnectorJavaLocal();
		descriptor = new ServiceDescriptorLocalBase("FlushingTriggerServiceConnector",
				(Class<?>) null, null);
		connector.setService(descriptor);

		executor = Executors.newSingleThreadExecutor(new DaemonThreadFactory(
				FlushingTriggerServiceImpl.class.getSimpleName() + "-Thread"));
		this.start();
	}

	public ServiceDataConnectorJavaLocal getServiceDataConnector() {
		return connector;
	}

	public void start() {
		// TODO: Check to make sure you don't run it again on subsequent calls
		executor.execute(new Runnable() {

			@Override
			public void run() {
				// Try to catch an unhandled exception in the main service thread
				Thread.currentThread().setUncaughtExceptionHandler(new CustomExceptionHandler());
				while (!Thread.currentThread().isInterrupted()) {
					try {
						Thread.sleep(applicationSettings
								.flushingTriggerFrequency());
					} catch (InterruptedException e) {
						// TODO: log
						return;
					}
					if (AppLogger.isDebugEnabled(logger))
						logger.debug("Flushing trigger fired!");
					flushAllData();
				}
			}

		});
	}
	
	public void flushAllData() {
		connector.sendMessage(new MessageNotificationBase(
			descriptor.getServiceName(), RichServiceUtils
					.generateMessageId(), "flushing-trigger",
			""));
	}

	public void stop() {
		this.executor.shutdownNow();
	}
}
