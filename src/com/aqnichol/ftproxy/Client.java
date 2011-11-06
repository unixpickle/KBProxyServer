package com.aqnichol.ftproxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import com.aqnichol.keyedbits.decode.ValueDecoder;
import com.aqnichol.keyedbits.encode.ValueEncoder;
import com.aqnichol.keyedbits.value.UnmatchedTypeException;

public class Client {

	public interface ClientCallback {

		public void ClientEncounteredException (Client c, Exception e);
		public boolean ClientRequestedAuthorization (Client c, byte[] token);

	}

	private InputStream socketInput;
	private OutputStream socketOutput;
	private Socket socket;

	private ClientCallback callback = null;

	private byte[] authToken = null;
	private Object authTokenLock;
	private Client pairedClient = null;
	private Object pairedClientLock;

	public Client (Socket aSocket, ClientCallback callback) throws IOException {
		socket = aSocket;
		socketInput = socket.getInputStream();
		socketOutput = socket.getOutputStream();

		this.callback = callback;
		pairedClientLock = new Object();
		authTokenLock = new Object();
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
					callback.ClientEncounteredException(client, e);
				} catch (UnmatchedTypeException e) {
					callback.ClientEncounteredException(client, e);
				} catch (PacketValidator.InvalidPacketException e) {
					callback.ClientEncounteredException(client, e);
				}
			}
		};
		new Thread(r).start();
	}

	public void disconnect () throws IOException {
		socketInput.close();
		socketOutput.close();
		socket.close();
	}
	
	public void sendMap (Map<String, ?> dictionary) {
		synchronized (socketOutput) {
			try {
				ValueEncoder.encodeRootObjectToStream(dictionary, socketOutput);
			} catch (IOException e) {
				callback.ClientEncounteredException(this, e);
			}
		}
	}

	public void pairedWithClient (Client aClient) {
		synchronized (pairedClientLock) {
			pairedClient = aClient;
		}
		HashMap<String, String> connInfo = new HashMap<String, String>();
		connInfo.put("type", "conn");
		connInfo.put("action", "connected");
		sendMap(connInfo);
	}

	public Client getPairedClient () {
		synchronized (pairedClientLock) {
			return pairedClient;
		}
	}

	public void remoteClientDisconnected () {
		synchronized (pairedClientLock) {
			pairedClient = null;
		}
		HashMap<String, String> connInfo = new HashMap<String, String>();
		connInfo.put("type", "conn");
		connInfo.put("action", "connected");
		sendMap(connInfo);
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

	private void clientSentData (Map<String, ?> theMessage) {
		Client paired = this.getPairedClient();
		if (paired != null) {
			paired.sendMap(theMessage);
		}
	}

	private void clientSentAuthToken (ByteBuffer token) {
		if (!callback.ClientRequestedAuthorization(this, token.array())) {
			// write an error map
			HashMap<String, Object> errorMsg = new HashMap<String, Object>();
			errorMsg.put("type", "error");
			errorMsg.put("msg", "Auth token already in use");
			errorMsg.put("code", new Integer(1));
			sendMap(errorMsg);
		}
	}

}
