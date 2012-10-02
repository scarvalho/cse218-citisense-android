package org.citisense.android.policy.impl;

import org.citisense.android.bluetooth.BluetoothChatService;
import org.citisense.android.service.BluetoothService;
import org.citisense.android.service.impl.AppLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sosa.richservice.InterceptionResult;
import org.sosa.richservice.Message;
import org.sosa.richservice.Policy;
import org.sosa.richservice.base.InterceptionResultBase;
import org.sosa.richservice.base.MessageRequestBase;

public class LocalRepositorySelectionPolicy implements Policy {

	private static final Logger logger = LoggerFactory
			.getLogger(LocalRepositorySelectionPolicy.class);

	private final BluetoothService bluetoothService;

	public LocalRepositorySelectionPolicy(BluetoothService bluetoothService) {
		this.bluetoothService = bluetoothService;
	}

	@Override
	public String getName() {
		return "LocalRepositorySelectionPolicy";
	}

	@Override
	public InterceptionResult interceptMessage(Message msg) {
		if (msg instanceof MessageRequestBase) {
			MessageRequestBase request = (MessageRequestBase) msg;
			if (request.getDestination().equals("LocalRepository")
					&& bluetoothService.isSensorConnected() == BluetoothChatService.STATE_NONE) {
				// re-route it to sd service
				if (AppLogger.isDebugEnabled(logger)) {
					logger.debug("Re-routing message destined to LocalRepository to SanDiegoRepository");
				}
				MessageRequestBase reroutedRequest = new MessageRequestBase(
						request.getSource(), "SanDiegoRepository", request
								.getMessageId(), request.getOperation(),
						request.getOperationParameterTypes(), request
								.getOperationParameterValues());
				return new InterceptionResultBase(reroutedRequest);
				// FIXME: We also need to make sure we track the response and
				// re-write its source.
			}
		}

		return new InterceptionResultBase(msg);
	}
}
