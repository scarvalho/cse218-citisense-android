package org.sosa.richservice.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.citisense.utils.thread.DaemonThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sosa.richservice.InterceptionResult;
import org.sosa.richservice.Message;
import org.sosa.richservice.MessageAddressed;
import org.sosa.richservice.MessageInterceptor;
import org.sosa.richservice.Policy;

/**
 * This implementation:
 * <ul>
 * <li>has a specific daemon thread that watches the queue {@code
 * toBeInterceptedQ} and calls {@link #interceptMessage(MessageAddressed)} on
 * the ones that arrive.</li>
 * <li>traverses the policies (in {@link #interceptMessage(MessageAddressed)})
 * in separate threads.</li>
 * </ul>
 * 
 * @author celal.ziftci
 * 
 */
public class MessageInterceptorBase implements MessageInterceptor {

	private final Map<String, Policy> policies = new ConcurrentHashMap<String, Policy>();
	private BlockingQueue<Message> toBeInterceptedQ, interceptedQ;
	private final ExecutorService policyExecutor = Executors
			.newCachedThreadPool(new DaemonThreadFactory(
					"MessageInterceptor.PolicyExecutor"));

	private final ExecutorService qWatcher = Executors
			.newSingleThreadExecutor(new DaemonThreadFactory(
					"MessageInterceptor.QueueWatcher"));

	private final Logger logger = LoggerFactory
			.getLogger(MessageInterceptorBase.class);

	public MessageInterceptorBase(BlockingQueue<Message> toBeInterceptedQ,
			BlockingQueue<Message> interceptedQ) {
		this.toBeInterceptedQ = toBeInterceptedQ;
		this.interceptedQ = interceptedQ;

		final Runnable messageInterceptorThread = new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						interceptMessage(MessageInterceptorBase.this.toBeInterceptedQ
								.take());
					} catch (InterruptedException e) {
						if (logger.isWarnEnabled()) {
							logger.warn("The message consumer thread of message interceptor has died due to interruption. No more messages will be intercepted and delivered to the bus.",
											e);
						}
						return;
					}
				}
			}
		};
		qWatcher.submit(messageInterceptorThread);

	}

	@Override
	public void interceptMessage(final Message msg) {
		if (logger.isTraceEnabled()) {
			logger.trace("Message to be intercepted: '{}'", msg);
		}

		Future<InterceptionResult> futureMessage = policyExecutor
				.submit(new Callable<InterceptionResult>() {

					@Override
					public InterceptionResult call() throws Exception {
						Message currentMessage = msg;
						Collection<Message> extras = new ArrayList<Message>();

						Collection<Policy> all = policies.values();
						for (Policy policy : all) {
							if (logger.isTraceEnabled()) {
								logger.trace("Running policy '{}' now...",
										policy.getName());
							}
							InterceptionResult interceptionResult = policy
									.interceptMessage(currentMessage);
							if (logger.isTraceEnabled()) {
								logger
										.trace(
												"The result of running the '{}' policy:\nIN:\n{}\nOUT{}",
												new Object[] {
														policy.getName(),
														currentMessage,
														interceptionResult
																.interceptedMessage() });
							}
							currentMessage = interceptionResult
									.interceptedMessage();
							extras.addAll(interceptionResult.extraMessages());
						}

						return new InterceptionResultBase(currentMessage,
								extras.toArray(new Message[extras.size()]));
					}
				});

		InterceptionResult interceptionResult = null;
		try {
			interceptionResult = futureMessage.get();
		} catch (InterruptedException e) {
			if (logger.isWarnEnabled()) {
				logger.warn("The thread running the message through interceptors has been interrupted. Will skip the interception, and continue with message delivery as if the message is finished interception",
								e);
			}
		} catch (ExecutionException e) {
			if (logger.isWarnEnabled()) {
				logger.warn("Exception thrown during message interception. "
										+ "Will skip interception and deliver the original message.",
							e);
			}
		}

		if (interceptionResult != null) {
			try {
				interceptedQ.put(interceptionResult.interceptedMessage());
				interceptedQ.addAll(interceptionResult.extraMessages());
			} catch (InterruptedException e) {
				// FIXME: Put an error message for the sender of this message
				// (caller of this method...)
			}
		} else {
			try {
				interceptedQ.put(msg);
			} catch (InterruptedException e) {
				// FIXME: Put an error message for the sender of this message
				// (caller of this method...)
			}
		}
	}

	@Override
	public void addPolicy(String policyName, Policy policy) {
		policies.put(policyName, policy);
	}

	@Override
	public Policy removePolicy(String policyName) {
		return policies.remove(policyName);
	}

}
