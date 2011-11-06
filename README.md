KBProxyServer
=============

This is a small Java server that operates as one-to-one proxy for TCP clients. Clients connect through a KeyedBits socket and provide an ID, and get paired up with another client that has the same ID. The two clients can then send data to one another. This could, for example be used for file transfers between clients on different subnets.

Work in Progress
----------------

Currently, this project is completely untested, and is still in the process of being developed. If you plan to use such a proxy, it is suggested that you wait until this message is removed.
