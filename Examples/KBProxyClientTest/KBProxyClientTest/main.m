//
//  main.m
//  KBProxyClientTest
//
//  Created by Alex Nichol on 11/7/11.
//  Copyright (c) 2011 __MyCompanyName__. All rights reserved.
//

#import <Foundation/Foundation.h>

#import "KBEncodeObjC.h"
#import "KBDecodeObjC.h"

#include <fcntl.h>
#include <sys/types.h>
#include <errno.h>
#include <sys/socket.h>
#include <netdb.h>
#include <netinet/in.h>

int handleKBFileDescriptor (int fd);
int handleKBSender (int fd);
int handleKBReciever (int fd);
NSString * readLineFile (FILE * aFile);

int main (int argc, const char * argv[]) {
	@autoreleasepool {
		const char * hostName = "127.0.0.1";
		const int portNum = 9000;
		
		struct sockaddr_in serv_addr;
		struct hostent * server = NULL;
		int fileDescriptor = socket(AF_INET, SOCK_STREAM, 0);
		if (fileDescriptor < 0) {
			fprintf(stderr, "socket_connection_create_network: could not create socket()\n");
			return NULL;
		}
		
		server = gethostbyname(hostName);
		if (!server) {
			fprintf(stderr, "could not gethostbyname()\n");
			exit(-1);
		}
		
		bzero(&serv_addr, sizeof(struct sockaddr_in));
		serv_addr.sin_family = AF_INET;
		memcpy(&serv_addr.sin_addr.s_addr, server->h_addr, server->h_length);
		serv_addr.sin_port = htons(portNum);
		
		if (connect(fileDescriptor, (const struct sockaddr *)&serv_addr, sizeof(struct sockaddr_in)) < 0) {
			fprintf(stderr, "failed to connect()\n");
			exit(-1);
		}
		
		if (handleKBFileDescriptor(fileDescriptor) != 0) {
			fprintf(stderr, "Encountered error.\n");
			exit(-1);
		}
		
		close(fileDescriptor);
	}
	return 0;
}

int handleKBFileDescriptor (int fd) {
	printf("Enter alias: ");
	NSString * tokenStr = readLineFile(stdin);
	NSData * token = [tokenStr dataUsingEncoding:NSUTF8StringEncoding];
	NSDictionary * authDict = [NSDictionary dictionaryWithObjectsAndKeys:@"auth", @"type",
							   token, @"token", nil];
	if (!kb_encode_full_fd(authDict, fd)) {
		return 1;
	}
	// wait for connected
	NSDictionary * connInfo = nil;
	while ((connInfo = (NSDictionary *)kb_decode_full_fd(fd))) {
		if ([[connInfo objectForKey:@"type"] isEqual:@"conn"]) {
			if ([[connInfo objectForKey:@"action"] isEqual:@"connected"]) {
				if ([[connInfo objectForKey:@"started"] boolValue]) {
					return handleKBReciever(fd);
				} else {
					return handleKBSender(fd);
				}
			}
		}
	}
	return -1;
}

int handleKBSender (int fd) {
	printf("Sending messages to other client...\n");
	while (true) {
		printf("Enter a message: ");
		NSString * message = readLineFile(stdin);
		NSDictionary * dictionary = [NSDictionary dictionaryWithObjectsAndKeys:@"data", @"type",
									 [message dataUsingEncoding:NSUTF8StringEncoding], @"data", nil];
		if (!kb_encode_full_fd(dictionary, fd)) {
			return -1;
		}
	}
	return 0;
}

int handleKBReciever (int fd) {
	printf("Receiving messages...\n");
	NSDictionary * input = nil;
	while ((input = (NSDictionary *)kb_decode_full_fd(fd))) {
		if ([[input objectForKey:@"type"] isEqual:@"data"]) {
			NSString * original = [[NSString alloc] initWithData:[input objectForKey:@"data"]
														encoding:NSUTF8StringEncoding];
			printf("%s\n", [original UTF8String]);
		} else if ([[input objectForKey:@"type"] isEqual:@"conn"]) {
			if ([[input objectForKey:@"action"] isEqual:@"disconnected"]) {
				printf("Remote host disconnected");
				return 0;
			}
		}
	}
	return -1;
}

NSString * readLineFile (FILE * aFile) {
	NSMutableString * string = [NSMutableString string];
	int c;
	while ((c = fgetc(aFile)) != EOF) {
		if (c == '\n') break;
		if (c != '\r')
			[string appendFormat:@"%c", (char)c];
	}
	return string;
}

