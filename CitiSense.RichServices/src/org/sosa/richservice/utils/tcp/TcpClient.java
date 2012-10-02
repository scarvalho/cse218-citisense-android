package org.sosa.richservice.utils.tcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import org.sosa.richservice.Message;

/**
 * TcpClient.java
 * 
 * This class works in conjunction with TcpServer.java and TcpPayload.java
 * 
 * This client test class connects to server class TcpServer, and in response,
 * it receives a serialized an instance of TcpPayload.
 * 
 * @author celal.ziftci
 */

public class TcpClient {
	// public final String host = "128.54.17.237";
	// public final int port = 5050;
	private final String host;
	private final int port;
	private final int timeout;

	private Socket socket;

	/**
	 * Creates a TcpClient that will connect to the given {@code host} and
	 * {@code port}, and time out after {@code timeout} milliseconds.
	 * 
	 * @throws IOException
	 * @throws UnknownHostException
	 */
	public TcpClient(String host, int port, int timeout)
			throws UnknownHostException, IOException {
		this.host = host;
		this.port = port;
		this.timeout = timeout;

		SocketAddress address = new InetSocketAddress(this.host, this.port);
		socket = new Socket();
		socket.connect(address, this.timeout);
	}

	/**
	 * Creates a TcpClient that will connect to the given {@code host} and
	 * {@code port}, and waits infinitely long to connect.
	 * 
	 * @throws IOException
	 * @throws UnknownHostException
	 */
	public TcpClient(String host, int port) throws UnknownHostException,
			IOException {
		this(host, port, 0);
	}

	public Message sendMessage(Message msg) throws UnknownHostException,
			IOException, ClassNotFoundException {

		OutputStream os = socket.getOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(os);
		oos.writeObject(msg);

		InputStream iStream = this.socket.getInputStream();
		ObjectInputStream oiStream = new ObjectInputStream(iStream);
		Message response = (Message) oiStream.readObject();
		return response;
	}

	public void close() {
		try {
			if (socket != null) {
				socket.close();
			}
		} catch (IOException e) {
			// TODO: ignore and log
		}
	}
}