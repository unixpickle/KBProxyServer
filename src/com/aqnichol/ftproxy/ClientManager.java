package com.aqnichol.ftproxy;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;

import com.aqnichol.ftproxy.Client.ConnectionState;

public class ClientManager implements Client.ClientCallback {

	private ArrayList<Client> clients;
	
	public ClientManager () {
		clients = new ArrayList<Client>();
	}
	
	public void handleConnection (Socket socket) throws IOException {
		Client theClient = new Client(socket, this);
		synchronized (clients) {
			clients.add(theClient);
		}
		theClient.detatchClientThread();
	}
	
	public void clientUnhandledException (Client c, Exception e) {
		try {
			synchronized (clients) {
				if (!clients.contains(c)) {
					return;
				}
				clients.remove(c);
			}
			c.disconnect();
			broadcastClientDisconnect(c);
		} catch (IOException disconnException) {
			disconnException.printStackTrace();
		}
	}
	
	/**
	 * Called when a client sends an auth packet.
	 * @param c The client that has sent the auth packet
	 * @param token The auth token that the client wishes to use
	 * @return true when the token is in use by one of fewer other clients,
	 * or false if the token is already in use by two clients.
	 */
	public boolean clientRequestedAuthToken (Client c, byte[] token) {
		// confirm that no other two clients have the same token
		broadcastClientDisconnect(c);
		c.setAuthToken(null);
		c.setConnectionState(ConnectionState.NoIdentification);
		synchronized (clients) {
			for (int i = 0; i < clients.size(); i++) {
				Client aClient = clients.get(i);
				if (Arrays.equals(token, aClient.getAuthToken())) {
					if (aClient.getConnectionState() == ConnectionState.NotPaired) {
						// pair up the two clients
						c.setAuthToken(token);
						
						c.setPairedClient(aClient);
						aClient.setPairedClient(c);
						
						c.setConnectionState(ConnectionState.NotNotified);
						aClient.setConnectionState(ConnectionState.NotNotified);
						
						try {
							aClient.notifyClientState("connected", true);
						} catch (IOException e) {
							this.clientUnhandledException(aClient, e);
						}
						
						aClient.setConnectionState(ConnectionState.Connected);
						
						try {
							c.notifyClientState("connected", false);
						} catch (IOException e) {
							this.clientUnhandledException(c, e);
						}
						
						c.setConnectionState(ConnectionState.Connected);
						
						return true;
					} else {
						return false;
					}
				}
			}
		}
		
		c.setConnectionState(ConnectionState.NotPaired);
		c.setAuthToken(token);
		return true;
	}
	
	private void broadcastClientDisconnect (Client client) {
		Client paired = client.getPairedClient();
		client.setPairedClient(null);
		if (paired != null) {
			paired.setPairedClient(null);
			
			if (paired.getConnectionState() != ConnectionState.Connected) {
				paired.setConnectionState(ConnectionState.NotPaired);
				return;
			}
			
			paired.setConnectionState(ConnectionState.NotPaired);
			
			try {
				paired.notifyClientState("disconnected");
			} catch (IOException e) {
				this.clientUnhandledException(paired, e);
			}
		}
	}
	
}
