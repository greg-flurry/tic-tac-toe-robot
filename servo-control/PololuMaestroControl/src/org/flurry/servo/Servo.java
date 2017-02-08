/**
 * This represents a canonical servo. 
 * 
 * The travel for a servo is expected to be from 0 degrees (minimum travel) to N degrees,
 * (maximum travel) where N is generally 90 or 180 degrees.
 * 
 * 
 * During setup of a servo, the user should measure the PWM pulse width at the minimum
 * and maximum travel points. Best done with the Pololu Maestro Control Center.
 * 
 * @author gregflurry
 *
 */

package org.flurry.servo;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;

import org.flurry.pololu.maestro.micro.ServoConfig;

public interface Servo {
	
	
	/**
	 * Gets the last target position.
	 * 
	 * It is possible the servo has not reached the target
	 * 
	 * @return last target requested
	 */
	public double getTarget();

	/**
	 * Sets the target position
	 * 
	 * @param target, 0-N in degrees
	 * @throws NotBoundException 
	 */
	public void setTarget(double target) throws NotBoundException, IndexOutOfBoundsException;

	/**
	 * Gets the "actual" position
	 * 
	 * @return position of servo (in degrees)
	 */
	public double getPosition() throws NotBoundException;
	
	
	/**
	 * Sets the configuration. 
	 * 
	 * @param config for the servo
	 * @param goHome indicates whether to actually move to the home position in the config
	 */
	public void setConfiguration(ServoConfig config, boolean goHome) throws AlreadyBoundException;

	/**
	 * Gets configuration
	 * 
	 * @return configuration
	 */
	public ServoConfig getConfiguration();
	
	
	@Override
	public String toString();

}
