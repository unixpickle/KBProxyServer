package com.aqnichol.ftproxy;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;

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
	
	public void ClientEncounteredException (Client c, Exception e) {
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
	
	public boolean ClientRequestedAuthorization (Client c, byte[] token) {
		// confirm that no other two clients have the same token
		broadcastClientDisconnect(c);
		c.setAuthToken(null);
		synchronized (clients) {
			for (int i = 0; i < clients.size(); i++) {
				Client aClient = clients.get(i);
				if (Arrays.equals(token, aClient.getAuthToken())) {
					if (aClient.getPairedClient() == null) {
						// pair up the two clients
						c.setAuthToken(token);
						c.pairedWithClient(aClient);
						aClient.pairedWithClient(aClient);
						return true;
					} else {
						return false;
					}
				}
			}
		}
		c.setAuthToken(token);
		return true;
	}
	
	private void broadcastClientDisconnect (Client client) {
		System.out.println("Client disconnected: " + client);
		Client paired = client.getPairedClient();
		if (paired != null) {
			paired.remoteClientDisconnected();
		}
	}
	
}
