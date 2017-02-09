################################################################
#
# An "episodic" thread class. It must be subclassed to turn 
# "device specific" things ON and OFF periodically. This 
# enables things like blinking LEDs or beeping sound devices.
#
# An "episodic" thread supports multiple consequtive "episodes".
# An episode can be started and lasts until explicitly stopped.
# An episode typically runs for many cycles. Each cycle has an
# "on" phase and an "off" phase. By defalut, there is a 50% 
# duty cycle. 
#
# The the "episodic" thread allows new episodes to start/end
# until the thread is terminated by the parent thread.
#
################################################################

import threading
import time # only needed for unit test

DEBUG = False

################################################################
class Episodic_Thread(threading.Thread):
	
	################################################################
	#
	# Constructor for an "episodic" thread class
	#
	# Creates the various threading constructs required for 
	# synchronization between this thread and the parent thread.
	#
	# Subclasses will require parameter(s) specific to what happens 
	# during the ON and OFF phases of an episode.
	#
	# The "on" and "off" method paramaters get called during the
	# ON and OFF phases respectively.
	#
	################################################################
	def __init__(self, half_cycle_time, on_method, off_method):
		if (DEBUG): print("Episodic_Thread initializing")
		threading.Thread.__init__(self)

		# persist the default half cycle time
		self.half_cycle_time = half_cycle_time
		
		# persist the ON and OFF methods
		self.on_method = on_method
		self.off_method = off_method
		

		# create a condition for assisting proper termination of 
		# a running episode
		self.condition = threading.Condition()
		
		# set up an event that will let the thread know to die
		self.dieEvent = threading.Event()
		# set up an event and lock that will let the thread know to suspend
		self.suspendEvent = threading.Event()
		self.suspendLock = threading.Lock()
		# define a status variable
		self.suspended = True
		
	################################################################
	#
	# Run method for "episodic" thread.
	#
	# Behavior is controlled by events, locks, condtions. A suspend 
	# event and lock start and stop an episode. A condition 
	# (plus its internal lock) allow an episode to be 
	# terminated by the parent anywhere within either phase of a
    # cycle within an episode.
	# 
	# A die event indicates the thread is to die. This also requires
	# any existing episode to terminate first.
	#
	################################################################
	def run(self):
		if (DEBUG): print("Episodic_Thread starting to run")
		while not self.dieEvent.is_set() :
			# suspend until want to start an episode
			## NOTE:
			## lock must be acquired after thread initialized and before
			## thread started
			## 
			## also susupend here after end an episode because
			## the suspend event is set
			if (DEBUG): print("Episodic thread about to suspend episode")
			self.suspendLock.acquire()
			## should block at the acquire when parent acquires lock
			
			## get here when acquire lock (parent releases)
			if (DEBUG): print("Episodic thread back, new episode starts")
			# release lock to allow future suspension by parent
			self.suspendLock.release()
			
			# check to see if really want to die rather than 
			# start a new episode
			if self.dieEvent.is_set():
				break
				
			# new episode while not suspended
			cyclePhaseON = False
			while not self.suspendEvent.is_set():
				
				# switch states (switch here so end loop with device state known)
				if (cyclePhaseON):
					cyclePhaseON = False
				else:
					cyclePhaseON = True
				
				if (cyclePhaseON): # effect the ON phase of cycle					
					## device specific ON stuff here
					if (DEBUG) :
						print("Episodic_Thread: Cycle phase ON")
					
					# do device specific ON activity
					self.__onPhase()
					
				else: # effect the OFF phase of cycle
					## device specific OFF stuff here
					if (DEBUG): 
						print("Episodic_Thread: Cycle phase OFF")
					
					# do device specific OFF activity
					self.__offPhase()

				# acquire the condition lock and wait for 1/2 beep period
				## this wait can be terminated by parent at any time
				if (DEBUG): print("Episodic_Thread about to acquire condition and wait")
				self.condition.acquire()
				self.condition.wait(self.half_cycle_time)
				
			## END: of episode/phase loop
            
            # make sure end up with device OFF
			if (DEBUG): print("Episodic_Thread: device state at end of episode: ", cyclePhaseON)
			if (cyclePhaseON == True):
                ## device specific OFF stuff here
				if (DEBUG): 
					print("Episodic_Thread: Cycle phase OFF")
				# do device specific OFF activity
				self.__offPhase()
			
		## END: of die loop
			
		if (DEBUG): print("Episodic_Thread dying")
		
		return 

	################################################################
	# 
	# Device specific ON activity
	#
	# This method must be overriden to accomplish anything
	#
	################################################################
	def __onPhase(self):
		if (DEBUG): print("Device ON activity")
		if (self.on_method == None):
			print("Episodic_Thread: NO on_method")
		else:
			 self.on_method()

	################################################################
	# 
	# Device specific OFF activity
	#
	# This method simply calls the on_method (parameter)
	#
	################################################################
	def __offPhase(self):
		if (DEBUG): print("Device OFF activity")
		
		if (self.off_method == None):
			print("Episodic_Thread: NO off_method")
		else:
			 self.off_method()
		

	################################################################
	# 
	# Interrupts the "wait" in an episode cycle so that the episode
	# can cleanly terminate. This allows the episode to terminate
	# anywhere within a phase of an episode cycle.
	#
	################################################################
	def __interruptEpisode(self):
		self.condition.acquire()
		self.condition.notify()
		self.condition.release()
	
	################################################################
	# 
	# Suspends an episode on a natural boundary, i.e., after a
	# 1/2 of a full blink cycle. 
	#
	# This is because blink loop uses the suspend event to determine
	# where to continue to blink or stop, waiting on the event to
	# clear.
	#
	################################################################
	def __suspend(self):
		self.suspendLock.acquire()
		self.suspendEvent.set()
		self.suspended = True

	################################################################
	# 
	# Starts an episode on a natural boundary, i.e., at the 
	# beginning of the 'on' portion of the cycle.
	#
	# This is because blink loop starts once the suspend event
	# is cleared.
	#
	################################################################
	def __unsuspend(self):
		self.suspendEvent.clear()
		self.suspendLock.release()
		self.suspended = False

	################################################################
	# 
	# Stops an episode no matter where in blink cycle.
	#
	################################################################
	def stopEpisode(self):
		self.__suspend()
		self.__interruptEpisode()

	################################################################
	# 
	# Starts an episode with display on for 1/2 cycle. Episode
	# continues till stopped.
	#
	################################################################
	def startEpisode(self):
		self.__unsuspend()

 	################################################################
	# 
	# Starts the thread "cleanly". This means starts up ready to 
    # run an episode from the beginning (starts with an "on" phase).
	#
	################################################################
	def startClean(self):
		# make sure thread does not start an episode upon starting
		self.stopEpisode()

		# start the thread
		self.start()

 	################################################################
	# 
	# Set the time for a half cycle
	#
	################################################################
	def setHalfCycleTime(self, time):
		self.half_cycle_time = time

	################################################################
	# 
	# Terminates the episodic thread.
	#
	################################################################
	def terminate(self):
		if (DEBUG): print("Episodic_Thread will try to die")
		self.dieEvent.set()
		# unsuspend so can see the die event
		if self.suspended:
			if (DEBUG): print("Episodic_Thread: Releasing blink lock")
			#self.suspendEvent.clear()
			#self.suspendLock.release()
			self.__unsuspend()
		else:
			if (DEBUG): print("Episodic_Thread: Not suspended, setting suspend event")
			# send suspend event to break loop so can see die event
			self.suspendEvent.set()	

		# join with that caller
		self.join()
		if (DEBUG): print("Episodic_Thread: Joined, so can now die")

#####################################################################
#####################################################################
# main for unit test
#####################################################################
#####################################################################

if __name__ == '__main__':
    print("Episodic_Thread unit test")
    
	# initialize the beeper thread
    et = Episodic_Thread(0.5, None, None)
    
    et.startClean()
    
    time.sleep(1)
    
    et.startEpisode()
    
    time.sleep(5)
    
    et.stopEpisode()
    
    time.sleep(1)
    
    et.terminate()
 
