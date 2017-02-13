################################################################
#
# A blinker based on the episodic thread class
#
################################################################

import episodic_thread
import time

DEBUG = False

class Blinker(episodic_thread.Episodic_Thread):
	
	################################################################
	#
	# Constructor for a blinking thread class
	#
	# Supplies the proper parameters for the parent class. This 
	# includes the methods for turning the display on/off.
	#
	# It also records the information needed to turn on/off the
	# display.
	#
	################################################################
	def __init__(self, half_cycle_time, lcd):
		if (DEBUG): print("Blinker initializing")
		# init the super class
		episodic_thread.Episodic_Thread.__init__(self, half_cycle_time, self.onPhase, self.offPhase)

		# persist the display information
		self.lcd = lcd
		

	def onPhase(self):
		# turn on display with the right colors
		if (DEBUG) :
			print("using colors ", self.r, self.g, self.b)
			print("Blink ON")
		self.lcd.set_color(self.r, self.g, self.b)

	def offPhase(self):
		# turn off display
		if (DEBUG): print("blink OFF")
		self.lcd.set_backlight(0.0)



	################################################################
	# 
	# Set the color to be used for the display (really backlight)
	#
	# The blink thread must know the color because turning off 
	# the display in effect uses no color.
	#
	################################################################
	def setColors(self, r, g, b):
		self.r = r
		self.g = g
		self.b = b

