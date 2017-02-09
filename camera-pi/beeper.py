################################################################
#
# A beeper class based on the episodic thread class. 
#
################################################################

import episodic_thread
import RPi.GPIO as GPIO

import time # only needed for unit test

DEBUG = False

################################################################
class Beeper(episodic_thread.Episodic_Thread):
	
	################################################################
	#
	# Does device specific activity to turn ON the buzzer
	#
	################################################################	
	def onPhase(self):
		if (DEBUG): print("Beeper ON")
		
		self.gpio.output(self.buzzer_pin, GPIO.HIGH)

	################################################################
	#
	# Does device specific activity to turn OFF the buzzer
	#
	################################################################	
	def offPhase(self):
		if (DEBUG): print("Beeper OFF ")

		self.gpio.output(self.buzzer_pin, GPIO.LOW)

	################################################################
	#
	# Constructor
	#
	# This constructor supplies the proper parameters to the super
	# class. This includes the methods for turning the sound
	# device on/off.
	#
	# It also records the information needed for GPIO activity 
	#
	################################################################
	def __init__(self, half_cycle_time, gpio, buzzer_pin):
		if (DEBUG): print("Beeper initializing")
		# init super class
		episodic_thread.Episodic_Thread.__init__(self, half_cycle_time, self.onPhase, self.offPhase)
		
		# persist GPIO info
		self.gpio = gpio
		self.buzzer_pin = buzzer_pin

################################################################
######################## MAIN - unit test ######################
################################################################

if __name__ == '__main__':

	print("Beeper unit test")

	# GPIO pin for buzzer
	buzzer = 17

	GPIO.setmode(GPIO.BCM)

	GPIO.setup(buzzer, GPIO.OUT)
   
	# initialize the beeper thread with gpio info
	beeper = Beeper(0.5, GPIO, buzzer)
    
	beeper.startClean()
    
	time.sleep(1)
    
	beeper.startEpisode()
    
	time.sleep(3)
    
	beeper.stopEpisode()
    
	time.sleep(1)
    
	beeper.terminate()
 
	GPIO.cleanup()
