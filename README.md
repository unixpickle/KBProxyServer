# KBProxyServer

This is a small Java server that operates as one-to-one proxy for TCP clients. Clients connect through a KeyedBits socket and provide an ID, and get paired up with another client that has the same ID. The two clients can then send data to one another. This could, for example be used for file transfers between clients on different subnets.

## Work in Progress

Currently, this project is completely untested, and is still in the process of being developed. If you plan to use such a proxy, it is suggested that you wait until this message is removed.

# Proxy Protocol

KBProxyServer uses KeyedBits to send dictionaries back and forth accross TCP. The protocol that clients can use to communicate with the server works something like this:

	[Client1->Server] connect to server on port 9000
	[Client2->Server] connect to server on port 9000
	[Client1->Server] send ["type":"auth", "token":<DATA>]
	[Client2->Server] send ["type":"auth", "token":<DATA>]
	// both clients have identified themselves, proxy pairs them
	[Server->Client1] send ["type":"conn", "action":"connected", started:1]
	[Server->Client2] send ["type":"conn", "action":"connected", started:0]
	// example of writing data from one client to the other
	[Client1->Server] send ["type":"data", "data":<OBJECT>]
	[Server->Client2] send ["type":"data", "data":<OBJECT>]
	// Client1 disconnects
	[Server->Client2] send ["type":"conn", "action":"disconnected"]

## Error codes

Errors are sent via KeyedBits dictionaries in the following format:

	["type":"error", "msg":<STRING_ERROR_MSG>, "code":<INT_ERROR_CODE>]

Error codes:

- 1: Token in use: the token you supplied to auth is already used by another client pair. Choose another. 
