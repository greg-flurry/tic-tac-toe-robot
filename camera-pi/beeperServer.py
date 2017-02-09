# 
# This is the server for beeper for the game playing arm
#
# This version acts as a network server that listens for commands; it 
# does NOT return status. 
#
# It uses a Beeper thread to do the actual beeping so that can
# get commands in parallel.
#
# Commnads:
#	- start [beeper] 
#	- stop [beeper]
#
# Command format: "mainCmd"
#	where 
#		mainCmd = 'start' starts the beeper
#		mainCmd = 'stop' stops the beeper
#

#from socket import *
#import io
import RPi.GPIO as GPIO
import beeper
import sendingServer

#################### Global Variables #####################

# information for client communication
ip_address = '192.168.1.143' # ip address for socket
ip_port = 9011 # port number for socket
server_name = 'Beeper'

DEBUG = False

#################### Functions ############################

#
# set up a thread to beep
# 
# requires a GPIO pin and a time for a half cycle of beeping
#
def setUpBeeper():
	
	# GPIO pin for buzzer
	buzzer = 17
	
	# half period for beep (in seconds)
	halfPeriod = 0.5

	# set up the GPIO pin
	GPIO.setup(buzzer, GPIO.OUT)
	
	# initialize the beeper thread 
	beep = beeper.Beeper(halfPeriod, GPIO, buzzer)

	# start the blinker thread with beeper stopped
	beep.startClean()

	return beep

#################### Main ############################
#################### Main ############################
#################### Main ############################
#################### Main ############################

# set up the socket server
## in effect, Pi process becomes a network server
server = sendingServer.Sending_Server(ip_address, ip_port, server_name)

# wait for client connection
server.awaitConnect()

# set up GPIO
GPIO.setmode(GPIO.BCM)

# set up blinker
beeper = setUpBeeper()

# set up beeper status
beeperON = False

# loop waiting for commands (start or stop beeping)
while True:

	# get the command
	cmd = server.getCommand()

	facets = cmd.split()
	if (len(facets) != 0) : # if connection not closed
		# find the command and parameters#		facets = cmd.split(';')
		if (DEBUG): print("facets= ", facets)
		mainCmd = facets[0]
		if (mainCmd == "start"): # want to start beeper
			if (DEBUG): print("Want to start beeper")
			if not beeperON:
				# turn on beeper
				beeper.startEpisode()
				beeperON = True
				if (DEBUG): print("Beeper started")
			else :
				if (DEBUG): print("Beeper already started")

		elif (mainCmd == "stop"): # want to stop beeper
			if (DEBUG): print("Want to stop beeper")
			if beeperON:
				# turn off beeper
				beeper.stopEpisode()
				beeperON = False
				if (DEBUG): print("Beeper stopped")
			else :
				if (DEBUG): print("Beeper already stopped")

		else:
			print("Bad command!")
	else:
		# finish the communication 
		if (DEBUG): print("Done with commo!")
		break

	## END: if for valid command
## END: while loop for commands

# kill the beeper thread
if (DEBUG): print("Main: will try to kill thread")
beeper.terminate()
if (DEBUG): print("Thread cleaned up")

# close client connection, etc.
GPIO.cleanup()

server.close()
   
print ("Finished!")
