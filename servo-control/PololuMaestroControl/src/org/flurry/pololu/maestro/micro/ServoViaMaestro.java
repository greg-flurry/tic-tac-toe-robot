/**
 * This represents a servo controlled via a Pololu Micro Maestro. 
 *  
 * There can be many instances of this class. All instances have a singleton
 * static class for the Maestro underneath.
 * 
 * During setup of a servo, the user should measure the PWM pulse width at the minimum
 * and maximum travel points. Best done with the Pololu Maestro Control Center.
 * 
 * @author gregflurry
 *
 */

package org.flurry.pololu.maestro.micro;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;

import org.flurry.servo.Servo;

public class ServoViaMaestro implements Servo {
	
	private static final boolean DEBUG = false;
	
	// the configuration
	private ServoConfig config = null;
	
	// the target position in degrees
	private double target;

	
	/**
	 * Establishes the "configuration" for the servo in the Maestro.
	 * 
	 * It maps the canonical information to device-specific information and writes
	 * the relevant information (speed, acceleration, minimumPWM, maximumPWM) to 
	 * Maestro parameters.
	 * 
	 * The min and max are set so that the controller limits allow the desired
	 * PWM limits to be achieved. This requires some manipulation due to the 
	 * scaling factor (of 16) used by the controller.
	 * 
	 * @param config, a ServoConfig instance
	 * @param goHome, indicates whether to move to home position in config
	 * @throws AlreadyBoundException 
	 */
	public void setConfiguration(ServoConfig config, boolean goHome) throws AlreadyBoundException {
		if (DEBUG) System.out.println("setConfig for " + config.name);

		// set the configuration
		if (this.config == null) {
			// set the configuration
			this.config = config;
		} else { // trying to override the configuration			
			throw new AlreadyBoundException("Servo already configured.");
		}
		
		// for speed and acceleration, the canonical and raw values are the same
		PololuMaestroMicro.setSpeed(config.channel, config.speed);
		PololuMaestroMicro.setAcceleration(config.channel, config.acceleration);
		
		// set the home position to zero; 
		// 	this means the servo should not move without a specific command to do so
		PololuMaestroMicro.setServoHome(config.channel, 0);
			
		// fix minimum and maximum limits
		double realMin = 0.0;
		double realMax = 0.0;
		if (config.minimumPWM > config.maximumPWM) {
			realMin = config.maximumPWM;
			realMax = config.minimumPWM;
		} else {
			realMin = config.minimumPWM;			
			realMax = config.maximumPWM;
		}
		
		// do minimum:
		// simply divide by scaling factor, as rounding ensures result is less than minimum
		double count = realMin / 16;		
		PololuMaestroMicro.setServoMinimum(config.channel, (int)count);

		// do maximum
		// add 16 and then divide to ensure result is greater than maximum
		count = (realMax + 16.0) / 16;
		PololuMaestroMicro.setServoMaximum(config.channel, (int)count);

		// determine if want to move home
		if (goHome) {
			// actually make target the home position
			// NOTE: if Maestro just powered up, will move at full speed!
			try {
				setTarget(config.homeAngle);
			} catch (NotBoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			while (PololuMaestroMicro.servoMoving(config.channel)) {
				// do nothing
			}
		}
	}
	
	public ServoConfig getConfiguration() {
		return this.config;
	}

	
	
	/**
	 * Gets the last target position.
	 * 
	 * It is possible the servo has not reached the target
	 * 
	 * @return last target requested
	 */
	public double getTarget() {
		return target;
	}
	
	/**
	 * Set the target for a servo in canonical form. This starts movement, but
	 * does not wait for completion of movement.
	 * 
	 * The Maestro specific code converts the canonical to device form using
	 * information in the associated ServoConfig.
	 * 
	 * @param servo 
	 * @param target
	 */
	public void setTarget(double target) throws NotBoundException, IndexOutOfBoundsException {
		// check for configuration
		if (config == null) {
			// throw exception if no config
			throw new NotBoundException("No servo configuration for servo " + this);
		} else {
			// check the target for being in bounds of configuration min/max
			if (target < config.minimumAngle || target > config.maximumAngle) {
				throw new IndexOutOfBoundsException("Target out of bounds");
			} else {
				// calculate device target pulse width in microseconds
				this.target = target;
				double cPWM = config.minimumPWM + ((target - config.minimumAngle) * config.factor);
				// calculate units for Maestro
				int dPWM = (int) (4 * cPWM); 
				PololuMaestroMicro.setTargetRaw(config.channel, dPWM);
			}
		}		
	}

	/**
	 * Gets the theoretical position
	 * 
	 * 
	 */
	public double getPosition() throws NotBoundException {
		double position = 0.0;

		System.err.println("The method getPostion() is erroroneous! Returns wrong thing!");
		// really need to return in degrees, using config info
		
		// 
		// check for configuration
		if (config == null) {
			// throw exception if no config
			throw new NotBoundException("No servo configuration for servo " + this);
		} else {
			// get the channel status and isolate the position  
			int[] status = PololuMaestroMicro.getChannelStatusRaw(this.config.channel);
			int pos = status[0];
			
		}
		return position;
	}
	

	@Override
	public String toString() {
		return "Servo: " + config.name + "\n  target: " + target+ "\n  " + config.toString();
	}

	public static void main(String[] args) throws AlreadyBoundException {
		
//		String servoString = "Base";
//		int servoChannel = 0;
		
//		ServoConfig config = new ServoConfig(servoString, servoChannel, 0, 0, 15.0, 165.0, 1085.0, 1923.0, 90.0);
		ServoConfig config = new ServoConfig("Shoulder", 1, 0, 0, 0.0, 90.0, 1938.0, 1053.0, 10.0);
		
//		System.out.println(config);
		
		ServoViaMaestro servo = new ServoViaMaestro();
		servo.setConfiguration(config, true);
		System.out.println(servo);

	}

}
