package org.flurry.servo;

import java.rmi.AlreadyBoundException;
//import java.rmi.NotBoundException;
import java.util.Scanner;

import org.flurry.pololu.maestro.micro.PololuMaestroMicro;
import org.flurry.pololu.maestro.micro.ServoConfig;
import org.flurry.pololu.maestro.micro.ServoViaMaestro;

/**
 * This guy moves to cell locations on a game board or bone yard
 * 
 * IMPORTANT NOTE: The angles that define movement to a cell in either the game or the yard
 * assume that movement for at least the last few degrees is always from the same direction
 * and at the slowest possible speed. Experience shows that violation of either of these
 * conditions result in gripper placement inaccuracies in both position over the cell and
 * height above the board.
 * 
 * @author gregflurry
 *
 */
public class ArmTTT {
	
//	static private final boolean DEBUG_CONFIG = true;
	static private final boolean DEBUG_CONFIG = false;
	
//	static private final boolean DEBUG_NORMAL = true;
	static private final boolean DEBUG_NORMAL = false;
	
//	static private final boolean DEBUG_NO_SERVO = true;
	static private final boolean DEBUG_NO_SERVO = false;
	
	final double diffMove = 5.0; // difference in angle needed to leverage dual movement
	final int fastSpeed = 3; // speed factor for fast movement during dual movement
	final int slowSpeed = 1; // speed factor for standard (slow) movement during configuration
								// and dual movement

	final double SHOULDER_NEUTRAL = 30.0; // neutral position for shoulder
	final double BASE_NEUTRAL = 130.0; // and base
	
	private static ArmTTT instance = null;
	
	static private double[] angleCurrent = new double[5]; // for the servos angle [ see numbers below]
	static private int BASE = 0;
	static private int SHOULDER = 1;
	static private int ELBOW = 2;
	static private int WRIST = 3;
	static private int GRIPPER = 4;
	
	// the servo instances
	static private Servo base = (Servo) new ServoViaMaestro();
	static private Servo shoulder = (Servo) new ServoViaMaestro();
	static private Servo elbow = (Servo) new ServoViaMaestro();
	static private Servo wrist = (Servo) new ServoViaMaestro();
	static private Servo gripper = (Servo) new ServoViaMaestro();
	
	// the game board cell information (angles)
	// base, shoulder, elbow, wrist
	// NOTE: always make shoulder move the last
	// NOTE: angles for "dropping" not picking (up)	
	// IMPORTANT: see additional note at top of file
	//                             base   shoulder      elbow   wrist
	private static double[] c00 = {83.0,  33.3,       107.71,  36.70 	};
	private static double[] c01 = {91.0,  33.2,       108.15,  36.70 	};
	private static double[] c02 = {99.2,  33.3,       107.71,  36.70 	};
	
	private static double[] c10 = {83.5,  39.5,        95.01,  43.31 	};
	private static double[] c11 = {90.7,  38.8,        96.09,  42.77 	};
	private static double[] c12 = {98.0,  39.5,        95.01,  43.31  	};
	
	private static double[] c20 = {84.0,  46.6,        80.14,  50.82 	};
	private static double[] c21 = {90.7,  45.7,        81.46,  50.15 	};
	private static double[] c22 = {97.1,  46.6,        80.14,  50.82 	};
		
	// all the game cell information
	private static double[][] game = {c00,c01,c02,c10,c11,c12,c20,c21,c22};
	

	// the bone yard cell information (angles) 
	// NOTE: angles for picking (up) not dropping
	// IMPORTANT: see additional note at top of file
	//                            base   shoulder      elbow   wrist
	private static double[] b0 = {150.0,  33.4,        108.48,  36.00 	};
	private static double[] b1 = {150.0,  39.4,        96.09,   42.77 	};
	private static double[] b2 = {150.0,  46.4,        81.23,   50.26 	};
	private static double[] b3 = {150.0,  54.3,        64.66,   58.14 	};
	
	// all yard cell information
	private static double[][] yard = {b0, b1, b2, b3}; 
	
	// set up for input for class debug
	private static Scanner input = null;


	/**
	 * Constructor for the single instance of an arm.
	 * 
	 * @param goHome indicates that after a servo is configured, it should move to the home position
	 * 				(this is necessary if the arm is currently positioned in a "dangerous" place,
	 *               meaning where about any movement could hit something without using the home
	 *               movements to ensure that does not happen).
	 * @throws AlreadyBoundException
	 */
	protected ArmTTT(boolean goHome) throws AlreadyBoundException {
		
		// configure the servos
		if (!DEBUG_NO_SERVO) {
			configServos(goHome);
			if (DEBUG_CONFIG) System.out.println("Configuration complete!");
		}
		
	}

	/** 
	 * Provides the single instance of the arm. Creates if needed.
	 * 
	 * @param goHome indicates whether or not to move to home during configuration
	 * @return the arm
	 * @throws AlreadyBoundException
	 */
	public static ArmTTT getInstance(boolean goHome) throws AlreadyBoundException {

		if (instance == null) {
			instance = new ArmTTT(goHome);
		}
		
		return instance;
	}

	/** 
	 * Moves gripper to a cell on the game board. Gripper positioned to drop token.
	 * 
	 * @param row of cell
	 * @param col of cell
	 */
	public void goGame(int row, int col) {
		// get the index into the cell info table
		
		// turn input into index in array
		int index = row * 3 + col;
		if (DEBUG_NORMAL) System.out.println("index = " + index);
				
		// get servo position info
		double[] angles = game[index];
		if (DEBUG_NORMAL) System.out.println("Going to: " + angles[BASE] + ", " +
				angles[SHOULDER]+ ", " +
				angles[ELBOW] + ", " +
				angles[WRIST]);
		
		// go to cell
		goAngles(angles);

	}

	/**
	 * Moves gripper to yard cell. Gripper positioned to pick up cell.
	 * 
	 * @param index of yard cell
	 */
	public void goYard(int index) {

		if (DEBUG_NORMAL) System.out.println("index = " + index);
				
		// get servo position info
		double[] angles = yard[index];
		if (DEBUG_NORMAL) System.out.println("Going to: " + angles[BASE] + ", " +
				angles[SHOULDER]+ ", " +
				angles[ELBOW] + ", " +
				angles[WRIST]);
		
		// go to cell
		goAngles(angles);

	}

	
	/**
	 * Move gripper to a position to pick or drop a token.
	 * 
	 * Assumes that arm starts in some "neutral" place that is valid and won't hit anything
	 * as move to designated angles.
	 * 
	 * Moves in order: base, elbow, wrist, shoulder to minimize possibility of interference with
	 * other tokens on game board.
	 * 
	 * @param angles the list of angles to which to move servos 
	 */
	private void goAngles(double[] angles) {
		
		/**
		 * Should insert error checking here!!
		 */
		if(DEBUG_NORMAL) {
			System.out.print("Current ");
			printAngle(angleCurrent);
			System.out.print("Target ");
			printAngle(angles);
		}

		// go to base angle
		servoGoDecrease(base, BASE, angles[BASE]);
    	
		// go to elbow angle
		servoGoIncrease(elbow, ELBOW, angles[ELBOW]);
		
		// go to wrist angle
		servoGoIncrease(wrist, WRIST, angles[WRIST]);
		
		// go to shoulder angle
		servoGoIncrease(shoulder, SHOULDER, angles[SHOULDER]);
		
	}
	
	/**
	 * Ensures that target angle is approached from a smaller angle (move towards board)
	 * 
	 * This should be used for shoulder, elbow, wrist
	 * 
	 * @param servo servo instance to move
	 * @param index of servo in order in arm (0 = base)
	 * @param angle to which to move
	 */
	private void servoGoIncrease(Servo servo, int index, double angle) {
		// check target vs current
		if (angleCurrent[index] > (angle - 1)) { // must first move to smaller angle
			// move to target - 1 
			servoGoDual(servo, index, angle - 1);			
		}
		
		// now move to actual target at slow speed
		servoGoDual(servo, index, angle);			
		
	}
	
	/**
	 * Ensures that target angle is approached from a larger angle 
	 * 
	 * This should be used for base
	 * 
	 * @param servo servo instance to move
	 * @param index of servo in order in arm (0 = base)
	 * @param angle to which to move
	 */
	private void servoGoDecrease(Servo servo, int index, double angle) {
		// check target vs current
		if (angleCurrent[index] < (angle + 1)) { // must first move to smaller angle
			// move to target + 1 
			servoGoDual(servo, index, angle + 1);			
		}
		
		// now move to actual 
		servoGoDual(servo, index, angle);			
		
	}

	/**
	 * Move servo to desired angle at the current speed setting (set outside of method).
	 * The configured speed is slow, and the default speed is slow.
	 * 
	 * @param servo to move
	 * @param index of servo in order in arm (0 = base)
	 * @param angle to which to move
	 */
	private void servoGo(Servo servo, int index, double angle) {
		// go to servo angle
		if(DEBUG_NO_SERVO) {
			System.out.println("Fake movement of servo " + index + "!\n");
			angleCurrent[index] = angle;
		} else {
			if(DEBUG_NORMAL) {
				System.out.println("Servo " + servo.getConfiguration().name + " to " + angle + "?");
				String doit = input.next();
			}
			try {
				servo.setTarget(angle);
				while (PololuMaestroMicro.servosMoving(6)) {
					// do nothing
				}
				if(DEBUG_NORMAL) {
					System.out.println("Finished movement of " + servo.getConfiguration().name + "!\n");
				}
				angleCurrent[index] = angle;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Move servo to desired angle at fast speed.
	 * 
	 * @param servo to move
	 * @param index of servo in order in arm (0 = base)
	 * @param angle to which to move
	 */
	private void servoGoFast(Servo servo, int index, double angle) {
		// set fast speed
		PololuMaestroMicro.setSpeed(servo.getConfiguration().channel, fastSpeed);

		// move to target
		servoGo(servo, index, angle);
		
		// set slow speed
		PololuMaestroMicro.setSpeed(servo.getConfiguration().channel, slowSpeed);
	}
	
	/**
	 * Move servo to desired angle, at "high" speed for all but last few
	 * degrees of travel and  at "low" speed for final travel.
	 * 
	 * @param servo to move
	 * @param index of servo in order in arm (0 = base)
	 * @param angle to which to move
	 */
	private void servoGoDual(Servo servo, int index, double angle) {
		
		double fastTarget = 0;
		
		// go to servo angle
		if(DEBUG_NO_SERVO) {
			System.out.println("Fake movement of servo " + index + "!\n");
			angleCurrent[index] = angle;
		} else {
			if(DEBUG_NORMAL) {
				System.out.println("Servo " + servo.getConfiguration().name + " to " + angle + "?");
				String doit = input.next();
			}
			try {
				if (Math.abs(angle - servo.getTarget()) > diffMove) {
					if (DEBUG_NORMAL) System.out.println("Need to make dual movement");

					if (angle - servo.getTarget() > 0.0) { 
						fastTarget = angle - diffMove;
					} else {
						fastTarget = angle + diffMove;
					}
					if (DEBUG_NORMAL) System.out.println("Fast target = " + fastTarget);
					
					// make fast move towards target
					PololuMaestroMicro.setSpeed(servo.getConfiguration().channel, fastSpeed);
					servo.setTarget(fastTarget);
					while (PololuMaestroMicro.servosMoving(6)) {
						// do nothing					
					}
				}
				
				if (DEBUG_NORMAL) System.out.println("Now make slow move to real target");
				// make slow movement to target
				if(DEBUG_NORMAL) {
					System.out.println("Servo " + servo.getConfiguration().name + " to " + angle + "?");
					String doit = input.next();
				}
				PololuMaestroMicro.setSpeed(servo.getConfiguration().channel, slowSpeed);
				servo.setTarget(angle);
				while (PololuMaestroMicro.servosMoving(6)) {
					// do nothing			
				}
				if(DEBUG_NORMAL) {
					System.out.println("Finished movement of " + servo.getConfiguration().name + "!\n");
				}
				angleCurrent[index] = angle;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	

	
	/**
	 * Go to a neutral position. This moves only the shoulder base servos.
	 */
	public void goNeutral() {
		
		/**
		 * Should insert error checking here!!
		 */
		
		// go to shoulder angle
		//servoGoDual(shoulder, SHOULDER, SHOULDER_NEUTRAL);
		servoGoFast(shoulder, SHOULDER, SHOULDER_NEUTRAL);

		// go to base angle
		//servoGoDual(base, BASE, BASE_NEUTRAL);
		servoGoFast(base, BASE, BASE_NEUTRAL);

	}
	

	/**
	 * Configures the servos in the arm.
	 * 
	 * Contains a lot of device specific information, both for Maestro and for arm itself.
	 * 
	 * @throws AlreadyBoundException
	 */
	private void configServos(boolean goHome) throws AlreadyBoundException {

		// for debug
    	String doit = null; 

    	// channel numbers for servos on Maestro
		final int NUM_BASE_CHANNEL = 0;
		final int NUM_SHOULDER_CHANNEL = 1;
		final int NUM_ELBOW_CHANNEL = 2;
		final int NUM_WRIST_CHANNEL = 4;
		final int NUM_GRIPPER_CHANNEL = 5;
		
		boolean delayNeeded = false; // indicates should delay between servo configurations
		
		// get position of base 
		int[] status = PololuMaestroMicro.getChannelStatusRaw(NUM_BASE_CHANNEL);
		// determine if should delay between configuration of servos for safety
		if (status[0] == 0) { // then Maestro just powered on and should delay
			delayNeeded = true;
		}
		
		// initialize the servos
		
		/*
		 * Do the shoulder first because its position is key in keeping parts from 
		 * hitting something on the platform
		 */
		if (DEBUG_CONFIG) {
	        System.out.println("Config shoulder?");
	    	doit = input.next();
		}
		
		ServoConfig configShoulder = new ServoConfig("Shoulder", NUM_SHOULDER_CHANNEL, slowSpeed, 0, 0.0, 90.0, 1963.0, 1074.0, 10.0);
		angleCurrent[SHOULDER] = configShoulder.homeAngle;
		shoulder.setConfiguration(configShoulder, goHome);
		
		delay(delayNeeded);

		/*
		 * Do the base next to keep parts away from the camera tower and board
		 */
		if (DEBUG_CONFIG) {
	        System.out.println("Config base?");
	    	doit = input.next();
		}
    	
		ServoConfig configBase = new ServoConfig("Base", NUM_BASE_CHANNEL, slowSpeed, 0, 15.0, 165.0, 1088.0, 1923.0, 120.0);
		angleCurrent[BASE] = configBase.homeAngle;
		base.setConfiguration(configBase, goHome);

		delay(delayNeeded);

		if (DEBUG_CONFIG) {
	        System.out.println("Config elbow?");
	    	doit = input.next();
		}

    	ServoConfig configElbow = new ServoConfig("Elbow", NUM_ELBOW_CHANNEL, slowSpeed, 0, 0.0, 180.0, 949.0, 2042.0, 90.0);
		angleCurrent[ELBOW] = configElbow.homeAngle;
		elbow.setConfiguration(configElbow, goHome);

		delay(delayNeeded);

		if (DEBUG_CONFIG) {
	        System.out.println("Config wrist?");
	    	doit = input.next();
		}

    	ServoConfig configWrist = new ServoConfig("Wrist", NUM_WRIST_CHANNEL, slowSpeed, 0, 0.0, 90.0, 1050.0, 1932.0, 30.0);
		angleCurrent[WRIST] = configWrist.homeAngle;
		wrist.setConfiguration(configWrist, goHome);	
		
		if (DEBUG_CONFIG) {
	        System.out.println("Config gripper?");
	    	doit = input.next();
		}

    	ServoConfig configGripper = new ServoConfig("Gripper", NUM_GRIPPER_CHANNEL, slowSpeed, 0, 0.0, 1.0, 1410.0, 1570.0, 1.0);
		angleCurrent[GRIPPER] = configGripper.homeAngle;
		gripper.setConfiguration(configGripper, goHome);	
	}
	
	private void delay(boolean needed) {
		final int time = 2000;
		if (needed) {
			try {
				Thread.sleep(time);
			} catch (InterruptedException e) {
				System.out.println("Interrupted; blowing off delay. WATCH OUT!!");
				e.printStackTrace();
			}
		}	
	}
	
	/**
	 * Set the state of the gripper to open or closed. It opens at fast speed, closes at
	 * slow speed to ensure that token can be manipulated for proper gripping.
	 * 
	 * @param state 0 for closed, >0 for open
	 */
	public static final int GRIPPER_OPEN = 1;
	public static final int GRIPPER_CLOSED = 0;
	
	public void setGripper(int state) {
		if (state >= GRIPPER_OPEN) { // open gripper
			// make it open fast
			PololuMaestroMicro.setSpeed(gripper.getConfiguration().channel, fastSpeed);
			servoGo(gripper, GRIPPER, 1.0);
			// set back to slow
			PololuMaestroMicro.setSpeed(gripper.getConfiguration().channel, slowSpeed);

		} else { // close gripper
			servoGo(gripper, GRIPPER, 0);			
		}
	}
	
	/**
	 * A debug program. 
	 * 
	 * @param args
	 * @throws AlreadyBoundException
	 */
	public static void main(String[] args) throws AlreadyBoundException {
		
		input = new Scanner(System.in);

		// get the ArmTTT
		ArmTTT arm = ArmTTT.getInstance(true);

        // get cell/board
		while (true) {
			System.out.print("Enter location, either cnn (cell) or bn (bone), gn (gripper), or q (quit): ");
			String loc = input.next();
			if (loc.equals("q"))
				break;
//			System.out.println("token: " + loc);

			if (loc.startsWith("c")) { // game cell
				String a0 = loc.substring(1, 2);
//				System.out.println("a0 " + a0);
				String a1 = loc.substring(2, 3);
//				System.out.println("a1 " + a1);
				
				int cRow = Integer.parseInt(a0);
				int cCol = Integer.parseInt(a1);
				
				if (cRow > 2 || cCol > 2) { // bad input
					System.err.println("You entered bad cell number! Try again.");
					continue;
				} else {	
					System.out.println("Moving to cell(" + cRow + "," + cCol + ")");
					arm.goGame(cRow, cCol);
				}
				
				System.out.print("Ready to go to NEUTRAL position? ");
				loc = input.next();
				arm.goNeutral();
				
			}
			
			if (loc.startsWith("b")) { // bone cell
				String a0 = loc.substring(1, 2);
//				System.out.println("a0 " + a0);
				
				int index = Integer.parseInt(a0);
				
				if (index > 3 || index < 0) { // bad input
					System.err.println("You entered bad bone number! Try again.");
					continue;
				} else {	
					System.out.println("Moving to bone(" + index + ")");
					arm.goYard(index);
				}
							
				System.out.print("Ready to go to NEUTRAL position? ");
				loc = input.next();
				arm.goNeutral();
				
			}
			
			if (loc.startsWith("g")) { // gripper
				String a0 = loc.substring(1, 2);
				System.out.println("a0 " + a0);
				
				int state = Integer.parseInt(a0);
				System.out.println("state " + state);
				
				if (!(state == 0 || state == 1)) { // bad input
					System.err.println("You entered bad gripper state! Try again.");
					continue;
				} else {
					if (state == 0) {
						System.out.println("Close gripper");
						arm.setGripper(0);
					} else {
						System.out.println("Open gripper");
						arm.setGripper(1);
					} 
				}
							
				System.out.print("Ready to go to NEUTRAL position? ");
				loc = input.next();
				arm.goNeutral();
				
			}

		}
        input.close();
        System.out.println("Done!");
	}
	
	private void printAngle(double[] angle) {
		 System.out.println("ANGLES: base=" + angle[BASE] + 
				 "; shoulder=" + angle[SHOULDER] +
				 "; elbow=" + angle[ELBOW] +
				 "; wrist=" + angle[WRIST] );
	}



}
