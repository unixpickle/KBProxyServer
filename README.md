KBProxyServer
=============

This is a small Java server that operates as one-to-one proxy for TCP clients. Clients connect through a KeyedBits socket and provide an ID, and get paired up with another client that has the same ID. The two clients can then send data to one another. This could, for example be used for file transfers between clients on different subnets.

Work in Progress
----------------

Currently, this project is completely untested, and is still in the process of being developed. If you plan to use such a proxy, it is suggested that you wait until this message is removed.

Proxy Protocol
==============

KBProxyServer uses KeyedBits to send dictionaries back and forth accross TCP. The protocol that clients can use to communicate with the server works something like this:

	[Client1->Server] connect to server on port 9000
	[Client2->Server] connect to server on port 9000
	[Client1->Server] send ["type":"auth", "token":<DATA_HERE>]
	[Client2->Server] send ["type":"auth", "token":<DATA_HERE>]
	// both clients have identified themselves, proxy pairs them
	[Server->Client2] send ["type":"conn", "action":"connected"]
	[Server->Client1] send ["type":"conn", "action":"connected"]
	// example of writing data from one client to the other
	[Client1->Server] send ["type":"data", "data":<SOME_DATA>]
	[Server->Client2] send ["type":"data", "data":<SOME_DATA>]
	// Client1 disconnects
	[Server->Client2] send ["type":"conn", "action":"disconnected"]
