/** This class represents the configuration of an more or less canonical 
 * servo. 
 * 
 * The following fields are available
 * - name: the name of the servo
 * - channel: the Maestro channel number [maybe auto-generated]
 * - speed: servo speed (0 = fast as possible, higher is slower)
 * - acceleration: servo acceleration (0 = fast as possible, higher is slower)
 * - minimum: minimum travel (degrees)
 * - maximum: maximum travel (degrees)
 *   
 * @author gregflurry
 *
 */

package org.flurry.pololu.maestro.micro;

public class ServoConfig {
	
	public String name = null;
	public int channel = 99; // an invalid number
	public int speed = 0;
	public int acceleration = 0;
	public double minimumAngle = 0;
	public double maximumAngle = 0;
	public double minimumPWM = 0; // probably invalid
	public double maximumPWM = 0;
	public double homeAngle = 0; // the home angle; where should start on power up...;
								// 0 means unused (defaults to neutral)
		
	public double factor = 0; // this is computed (microseconds/degree)

	
//	public double minimumDeg = 0; // probably invalid
	 // * @param minimumDeg limits anti-clockwise travel; in degrees 
	 // * @param scale factor to take degrees to microseconds (microseconds/degree)

	// necessary constructor
	/**
	 * Constructor for a servo configuration
	 * 
	 * @param name the name of the servo; String
	 * @param channel the channel number or servo number; 0-5 for Micro Maestro
	 * @param speed the speed limit factor (see User's Guide); 0 = no limit
	 * @param acceleration the limit on acceleration (see User's Guide); 0-255; 0 = no limit
	 * @param minimumAngle maximum travel; in degrees
	 * @param maximumAngle maximum travel; in degrees
	 * @param minimumPWM width of PWM pulse for minimum degrees; in microseconds 
	 * @param maximumPWM width of PWM pulse for maximum travel; in microseconds 
	 * @param homeAngle angle for "home" position 
	 */
	public ServoConfig(String name, int channel, int speed, int acceleration, 
			double minimumAngle, double maximumAngle, 
			double minimumPWM, double maximumPWM, double homeAngle) {
		
		// set up the config
		this.name = name;
		this.channel = channel;
		this.speed = speed;
		this.acceleration = acceleration;
		this.minimumAngle = minimumAngle;
		this.maximumAngle = maximumAngle;
		this.minimumPWM = minimumPWM;
		this.maximumPWM = maximumPWM;
		this.homeAngle = homeAngle; 
		this.factor = (maximumPWM - minimumPWM) / (maximumAngle - minimumAngle) ;
		
	}
	
	@Override
	public String toString() {
		return "ServoConfig:: name: " + name + ";  channel: " + channel + ";  speed: " + speed +
				"; acceleration: " + acceleration + 
				"; minimumAngle: " + minimumAngle + ";  maximumAngle: " + maximumAngle + 
				"; minimumPWM: " + minimumPWM + "; maximumPWM: " + maximumPWM + 
				"; homeAngle: " + homeAngle +
				"; factor: " + factor;
	}
	
}

