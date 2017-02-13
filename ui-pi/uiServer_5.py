# 
# This is the server for the user interface for the game playing arm
#
# This version acts as a network server that listens for commands and 
# returns status.
#
# Commnads:
#	- display message; return key status 
#	- display message; no wait
#
# Status:
#	- state of switches (toggled or not)
#
# Command format: "mainCmd[&subCmd];color;message"
#	where 
#		mainCmd = 'show'
#		subCmd = 'wait' [optional] -- if present, wait for button press
#			   = 'blink' [optional] -- if present, display blinks
#		color = 'r' (red), 'g' (green), 'b' (blue), 'y' (yellow), 'w' (white)
#		message = up to 32 char, 16 per line, with '\n' between lines
#

from socket import *
import time

import RPi.GPIO as GPIO
from testLCD import Adafruit_RGBCharLCD as LCD

import blinker
import sendingServer

#################### Global variables ############################

DEBUG = False

# for communication with client
ip_address = '192.168.1.179'
ip_port = 9000
server_name = 'Blinker'

#################### Buttons ############################
# Raspberry Pi B+ configuration (GPIO BCM digital output numbers)
button_L = 16
button_R = 12

#################### Functions ############################

#
# set up LCD display
# 
# Assumes GPIO already initialized to BCM
#
def setUpLCD() :
	# Raspberry Pi B+ configuration (GPIO BCM digital output numbers)
	lcd_rs = 27  
	lcd_en = 22
	lcd_d4 = 25
	lcd_d5 = 24
	lcd_d6 = 23
	lcd_d7 = 18
	lcd_red   = 4
	lcd_green = 17
	lcd_blue  = 7  # Pin 7 is CE1

	# Define LCD column and row size for 16x2 LCD.
	lcd_columns = 16
	lcd_rows    = 2

	# Initialize the LCD using the pins above.
	lcd = LCD(lcd_rs, lcd_en, lcd_d4, lcd_d5, lcd_d6, lcd_d7,
			lcd_columns, lcd_rows, lcd_red, lcd_green, lcd_blue, gpio=GPIO)

	# return the LCD
	return lcd

#
# set up buttons
# 
# Assumes GPIO already initialized to BCM
#
def setUpButtons() :
	
	# set up buttons
	GPIO.setup(button_L, GPIO.IN, pull_up_down=GPIO.PUD_UP)
	GPIO.setup(button_R, GPIO.IN, pull_up_down=GPIO.PUD_UP)
	
	# enable event detection
	GPIO.add_event_detect(button_L, GPIO.RISING, bouncetime=300)
	GPIO.add_event_detect(button_R, GPIO.RISING, bouncetime=300)

#
# reset button event detection 
#   this avoids spurious detection due to switch bounce
#   (the bouncetime is actually ignored)
#
def resetButtons() :
	# clear the event detection
	GPIO.remove_event_detect(button_L)
	GPIO.remove_event_detect(button_R)
		
	# enable event detection
	GPIO.add_event_detect(button_L, GPIO.RISING, bouncetime=300)
	GPIO.add_event_detect(button_R, GPIO.RISING, bouncetime=300)


#################### Main ############################
#################### Main ############################
#################### Main ############################
#################### Main ############################

# set up GPIO
GPIO.setmode(GPIO.BCM)

# set up LCD
lcd = setUpLCD()
# turn off backlight
lcd.set_backlight(0.0)

# set up buttons
setUpButtons()

# set up the socket server
server = sendingServer.Sending_Server(ip_address, ip_port, server_name)
 
# wait for client connection
server.awaitConnect()

# set up blinker
half_blink_cycle = 0.5
blinky = blinker.Blinker(half_blink_cycle, lcd)
blinky.startClean()

# loop waiting for commands (display messages, get button status)
while True:

	# get the command
	cmd = server.getCommand()

	facets = cmd.split()
	if (len(facets) != 0) : # if connection not closed
		# find the command and parameters
		facets = cmd.split(';')
		if (DEBUG): print("facets= ", facets)
		subFacets = facets[0].split('&')
		if (DEBUG): print("sub facets= ", subFacets)
		mainCmd = subFacets[0]
		subCmd = ' '
		if (len(subFacets) >= 2) :
			subCmd = subFacets[1]
		if (mainCmd == "show"):

			# suspend blinking
			if not blinky.suspended :
				if (DEBUG): print("Suspending blink")
				#blinky.suspend()
				
				## knock thread out of middle blinking loop 
				#blinky.interruptEpisode()
				blinky.stopEpisode()
				
			else :
				if (DEBUG): print("blink already suspended")

			# determine color
			color = 'black'
			rC = 0.0
			gC = 0.0
			bC = 0.0
			if (facets[1] == 'r') : 
				color = 'red'
				rC = 1.0
			elif (facets[1] == 'b') : 
				color = 'blue'
				bC = 1.0
			elif (facets[1] == 'g') : 
				color = 'green'
				gC = 1.0
			elif (facets[1] == 'y') : 
				color = 'yellow'
				rC = 1.0
				gC = 1.0
			if (facets[1] == 'w') : 
				color = 'white'
				rC = 1.0
				gC = 1.0
				bC = 1.0
			
			if (DEBUG): print("In the color ", color, " showing ...")
			if (DEBUG): print(facets[2])
			
			# reset the event detect to prevent false stuff
			# do a delay first to eliminate bounce
			resetButtons()		

			# display the message
			if (DEBUG): print("Writing message")
			lcd.clear()
			lcd.set_color(rC, gC, bC) 
			lcd.message(facets[2])
			
			# set up for status return
			status = 0	
			
			# set colors for blinker
			blinky.setColors(rC, gC, bC)

			# check for blink sub-command
			if (subCmd == 'blink') :
				if (DEBUG) : print("Want to start blinking")
				# unsuspend thread to start blinking
				if blinky.suspended :
					if (DEBUG): print("Main: think thread unsuspended")
					if (DEBUG): print(" --- should see blinking")
					#blinky.unsuspend()
					blinky.startEpisode()

			# if want to wait for button press
			if (subCmd == 'wait') :		
				# wait for button press	
				if (DEBUG): print("waiting for button press")
				while True :
					if (GPIO.event_detected(button_L)) :
						if (DEBUG): print("Button L pressed")
						status = status + 1
						break
					if (GPIO.event_detected(button_R)) :
						if (DEBUG): print("Button R pressed")
						status = status + 2
						break
		
				if (DEBUG): print("Button pressed")
				
				# clear the display to prevent further presses
				lcd.clear()
				lcd.set_backlight(0.0)

			# send the return status 
			server.sendStatus(status)

	else:
		# finish the communication 
		if (DEBUG): print("Done with commo!")
		break

## END: while loop

# kill the blinker thread
if (DEBUG): print("Main: will try to kill thread")
blinky.terminate()
if (DEBUG): print("Thread cleaned up")

# close client connection, etc.
lcd.clear()
GPIO.cleanup()

server.close()    
print ("Finished!")
