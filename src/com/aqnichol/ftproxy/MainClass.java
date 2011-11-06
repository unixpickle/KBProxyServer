package com.aqnichol.ftproxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MainClass {

	public static void main (String[] args) throws IOException {
		ClientManager manager = new ClientManager();
		ServerSocket server = new ServerSocket(9000);
		while (true) {
			Socket socket = server.accept();
			manager.handleConnection(socket);
		}
	}

}
