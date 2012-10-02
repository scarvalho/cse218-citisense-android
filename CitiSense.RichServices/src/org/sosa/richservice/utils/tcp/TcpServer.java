package org.sosa.richservice.utils.tcp;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sosa.richservice.Message;
import org.sosa.richservice.MessageRequest;
import org.sosa.richservice.base.UncaughtExceptionHandlingThreadFactory;

/**
 * This class works in conjunction with TcpClient.java and TcpPayload.java
 * 
 * This server test class opens a socket on localhost and waits for a client to
 * connect. When a client connects, this server serializes an instance of
 * TcpPayload and sends it to the client.
 * 
 * @author celal.ziftci
 */

public class TcpServer {
	public final int port, maxNumberOfConnections;

	private final ServerSocket serverSocket;
	private final Executor executor;
	private final TcpMessageHandler handler;
	private final Logger logger = LoggerFactory.getLogger(TcpServer.class);

	public TcpServer(int port, int maxNumberOfConnections,
			TcpMessageHandler handler) throws Exception {
		this(port, maxNumberOfConnections, handler, null);
	}

	public TcpServer(int port, int maxNumberOfConnections,
			TcpMessageHandler handler, UncaughtExceptionHandler exceptionHandler)
			throws Exception {
		this.port = port;
		// FIXME: Implement the maximum size constraint. Basically, use a
		// semaphore and block on it as new requests come and go.
		this.maxNumberOfConnections = maxNumberOfConnections;
		this.handler = handler;
		if (exceptionHandler != null) {
			executor = Executors
					.newFixedThreadPool(this.maxNumberOfConnections + 1,
							new UncaughtExceptionHandlingThreadFactory(
									exceptionHandler));
		} else {
			executor = Executors
					.newFixedThreadPool(this.maxNumberOfConnections + 1);
		}
		// Bind to the port
		this.serverSocket = new java.net.ServerSocket(port);
		assert this.serverSocket.isBound();
		if (this.serverSocket.isBound()) {
			logger
					.info(
							"SERVER inbound data port {} is ready and waiting for client to connect...",
							this.serverSocket.getLocalPort());
		}

		startListeningThread();
	}

	private void startListeningThread() {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					while (true) {
						// listen for and accept a client connection to
						// serverSocket
						final Socket sock = TcpServer.this.serverSocket
								.accept();

						executor.execute(new Runnable() {

							@Override
							public void run() {
								InputStream inputStream;
								try {
									inputStream = sock.getInputStream();
									ObjectInputStream oistream = new ObjectInputStream(
											inputStream);
									// TODO: Currently, only requests can be
									// sent, but maybe in the future other
									// things as well.
									MessageRequest read = (MessageRequest) oistream
											.readObject();
									Message responseMsg = handler
											.handleMessage(read);

									OutputStream oStream = sock
											.getOutputStream();
									ObjectOutputStream ooStream = new ObjectOutputStream(
											oStream);
									ooStream.writeObject(responseMsg);
									ooStream.close();
								} catch (Throwable e) {
									logger
											.error(
													"Error communicating with client. Sending an exception message to the client.",
													e);
									try {
										OutputStream oStream = sock
												.getOutputStream();
										ObjectOutputStream ooStream = new ObjectOutputStream(
												oStream);
										ooStream.writeObject(e);
										ooStream.close();
									} catch (Throwable e2) {
										logger
												.error(
														"Error sending an exception message to the client.",
														e2);
										try {
											sock.close();
										} catch (Throwable e1) {
											logger.warn("Error closing socket to the client",
															e1);

										}
									}

								}

							}
						});

						Thread.sleep(1000);
					}
				} catch (Throwable se) {
					logger
							.error(
									"Error on the tcp server loop. The server port will be closed now.",
									se);

				} finally {
					try {
						TcpServer.this.serverSocket.close();
					} catch (Throwable t) {
						logger
								.error(
										"Error closing the server socket of tcp server loop. Ignoring this. Note that, no client can connect to this server on this port ("
												+ TcpServer.this.port
												+ ") anymore.", t);
					}
				}
			}
		});
	}
}