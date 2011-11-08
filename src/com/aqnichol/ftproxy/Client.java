package com.aqnichol.ftproxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import com.aqnichol.keyedbits.decode.ValueDecoder;
import com.aqnichol.keyedbits.encode.ValueEncoder;
import com.aqnichol.keyedbits.value.UnmatchedTypeException;

public class Client {

	public interface ClientCallback {

		public void clientUnhandledException (Client c, Exception e);
		public boolean clientRequestedAuthToken (Client c, byte[] token);

	}
	
	public enum ConnectionState {
		NoIdentification, NotPaired, NotNotified, Connected
	}

	private InputStream socketInput;
	private OutputStream socketOutput;
	private Socket socket;
	
	private ClientCallback callback = null;

	private byte[] authToken = null;
	private Object authTokenLock;
	private Client pairedClient = null;
	private Object pairedClientLock;
	private ConnectionState connectionState;
	private Object connectionStateLock;
	
	private static String stringFromSockAddr (SocketAddress addr) {
		if (addr instanceof InetSocketAddress) {
			InetSocketAddress inet = (InetSocketAddress)addr;
			return inet.getHostName() + ":" + inet.getPort();
		}
		return addr.toString();
	}

	public Client (Socket aSocket, ClientCallback callback) throws IOException {
		System.out.println("New connection: " + Client.stringFromSockAddr(aSocket.getRemoteSocketAddress()));
		socket = aSocket;
		socketInput = socket.getInputStream();
		socketOutput = socket.getOutputStream();
		this.callback = callback;
		
		pairedClientLock = new Object();
		authTokenLock = new Object();
		connectionStateLock = new Object();
		
		setConnectionState(ConnectionState.NoIdentification);
	}

	public void detatchClientThread () {
		final Client client = this;
		Runnable r = new Runnable () {
			public void run () {
				try {
					while (true) {
						mainClientLoop();
					}
				} catch (IOException e) {
					callback.clientUnhandledException(client, e);
				} catch (UnmatchedTypeException e) {
					callback.clientUnhandledException(client, e);
				} catch (PacketValidator.InvalidPacketException e) {
					callback.clientUnhandledException(client, e);
				}
			}
		};
		new Thread(r).start();
	}

	public void disconnect () throws IOException {
		synchronized (socket) {
			System.out.println("Disconnected: " + Client.stringFromSockAddr(socket.getRemoteSocketAddress()));
			socketInput.close();
			socketOutput.close();
			socket.close();
		}
	}
	
	public void sendMap (Map<String, ?> dictionary) throws IOException {
		synchronized (socketOutput) {
			ValueEncoder.encodeRootObjectToStream(dictionary, socketOutput);
		}
	}
	
	public Client getPairedClient () {
		synchronized (pairedClientLock) {
			return pairedClient;
		}
	}

	public void setPairedClient (Client aClient) {
		synchronized (pairedClientLock) {
			pairedClient = aClient;
		}
	}
	
	public void notifyClientState (String connState) throws IOException {
		HashMap<String, String> connInfo = new HashMap<String, String>();
		connInfo.put("type", "conn");
		connInfo.put("action", connState);
		sendMap(connInfo);
	}
	
	public void notifyClientState (String connState, boolean started) throws IOException {
		HashMap<String, Object> connInfo = new HashMap<String, Object>();
		connInfo.put("type", "conn");
		connInfo.put("action", connState);
		connInfo.put("started", new Integer(started ? 1 : 0));
		sendMap(connInfo);
	}
	
	public ConnectionState getConnectionState () {
		synchronized (connectionStateLock) {
			return connectionState;
		}
	}
	
	public void setConnectionState (ConnectionState aState) {
		synchronized (connectionStateLock) {
			connectionState = aState;
		}
	}
	
	public byte[] getAuthToken () {
		synchronized (authTokenLock) {
			return authToken;
		}
	}

	public void setAuthToken (byte[] authToken) {
		synchronized (authTokenLock) {
			this.authToken = authToken;
		}
	}

	private void mainClientLoop () throws IOException, UnmatchedTypeException, PacketValidator.InvalidPacketException {
		Object decoded = ValueDecoder.decodeRootObjectFromStream(socketInput);
		if (decoded == null) {
			throw new UnmatchedTypeException("Decoded object was null");
		}
		if (decoded instanceof Map) {
			PacketValidator validator = new PacketValidator((Map<?, ?>)decoded);
			validator.validateGeneralPacket();

			@SuppressWarnings("unchecked")
			Map<String, ?> packetInfo = (Map<String, ?>)decoded;

			if (packetInfo.get("type").equals("data")) {
				clientSentData(packetInfo);
			} else if (packetInfo.get("type").equals("auth")) {
				clientSentAuthToken((ByteBuffer)packetInfo.get("token"));
			}
		} else {
			throw new UnmatchedTypeException("Invalid class for client packet: " + decoded.getClass());
		}
	}

	private void clientSentData (Map<String, ?> theMessage) throws IOException {
		Client paired = this.getPairedClient();
		if (paired != null) {
			// wait for the paired client to be connected before we send this object.
			int timeout = 1000/50;
			while (paired.getConnectionState() != ConnectionState.Connected) {
				try {
					Thread.sleep(50);
					timeout -= 1;
				} catch (InterruptedException e) {
				}	
				if (timeout <= 0) throw new IOException("Paired client is no longer connected");
			}
			paired.sendMap(theMessage);
		}
	}

	private void clientSentAuthToken (ByteBuffer token) throws IOException {
		if (!callback.clientRequestedAuthToken(this, token.array())) {
			// write an error map
			HashMap<String, Object> errorMsg = new HashMap<String, Object>();
			errorMsg.put("type", "error");
			errorMsg.put("msg", "Auth token already in use");
			errorMsg.put("code", new Integer(1));
			sendMap(errorMsg);
		}
	}

}
