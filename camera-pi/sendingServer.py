################################################################
#
# Server class for receiving commands from a client and sending 
# information to the client. The class hides most of the minutia
# of bi-directional communication.
#
# It supports 
# -- getting a command, 
# -- sending a 4 byte status, which could be a data length
# -- sending an arbitrary byte array
# -- sending an io.BytesIO "stream" which sends the length
#        of the data in the stream and then the bytes in 
#        the stream
#
# File contains a simple user test at end.
#
################################################################

import threading
from socket import *
import io
import struct

DEBUG = False

################################################################
class Sending_Server():
	
	################################################################
	# Constructor that sets up the socket server
	#
	# Parameters:
	# -- ip address for device (string)
	# -- ip port on which to listen (int)
	# -- name to use for server reports (string)
	################################################################
	def __init__(self, ip_address, ip_port, name):
		if (DEBUG): print("SendingServer initializing")

		# set up the socket
		## in effect, Pi process becomes a network server
		self.piServer = socket(AF_INET, SOCK_STREAM)
		self.piServer.bind((ip_address, ip_port))

		# let admin know can now get client to try to connect
		print (name, " Server listening on port ", ip_port, " ...")
		self.piServer.listen(1)

	################################################################
	# Waits for the client to connect and make the resulting
	# connection look like file for information returned to client.
	#
	################################################################
	def awaitConnect(self):
		# wait for client to connect
		self.preConn,address = self.piServer.accept()
		if (DEBUG): print ("Received connection from", address)

		# make connection look like a file
		self.connection = self.preConn.makefile('wb')

	################################################################
	# Waits for the client to send a command string. It can be no
	# longer than 128 bytes.
	#
	# Returns a string sent by the client
	#
	################################################################
	def getCommand(self):
		if (DEBUG): print("Waiting for command.")
		command = self.preConn.recv(128)
		if (DEBUG): print ("Command: ", command)
		# make the command a useful string
		cmd = command.decode('utf-8')
		
		return cmd

	################################################################
	# Sends status to the client. 
	#
	# Parameters:
	# -- status (an integer (4 bytes))
	#
	################################################################
	def sendStatus(self, status):
		self.connection.write(struct.pack('<L', status))
		self.connection.flush()
		if (DEBUG): print ("think sent status")

	################################################################
	# Sends data to the client. Data can be an arbitrary length.
	#
	# Parameters:
	# -- data (byte array)
	#
	################################################################
	def sendData(self, data):
		# send data
		self.connection.write(data)
		self.connection.flush()
		if (DEBUG): print ("think sent data")
 
	################################################################
	# Sends a stream (io.BytesIO). It derives that length and 
	# data from the stream  and sends them serially to the client. 
	#
	# Parameters:
	# -- stream (io.BytesIO)
	#
	################################################################
	def sendStream(self, stream):
		# send length of stream
		self.sendStatus(stream.tell())
		# get to start of stream
		stream.seek(0)
		# send stream bytes
		self.sendData(stream.read())
		
	################################################################
	# Closes all the artifacts created for communication. 
	#
	################################################################
	def close(self):
		self.connection.close()
		self.preConn.close()
		self.piServer.close()    

#####################################################################
#####################################################################
# main for unit test
#####################################################################
#####################################################################

if __name__ == '__main__':
	print("Sending_Server unit test")
	
	server = Sending_Server('192.168.1.143', 9011, 'Testing')
    
	server.awaitConnect()

	while True:
		# get the command
		cmd = server.getCommand()
		
		if (cmd == ''): 
			break
		else:
			print("I got the command: ", cmd)
		
	## END: while getting commands
	
	server.close()	

    
