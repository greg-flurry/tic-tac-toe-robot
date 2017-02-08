/** This class represents the Pololu Micro Maestro servo controller.
 * 
 * It is a static class as there should only be a single instance of
 * the USB device that represents the Maestro.
 * 
 * @author gregflurry
 *
 */

package org.flurry.pololu.maestro.micro;

import java.nio.channels.NotYetBoundException;
import java.util.List;

import javax.usb.UsbControlIrp;
import javax.usb.UsbDevice;
import javax.usb.UsbDeviceDescriptor;
import javax.usb.UsbDisconnectedException;
import javax.usb.UsbException;
import javax.usb.UsbHostManager;
import javax.usb.UsbHub;
import javax.usb.UsbServices;


public class PololuMaestroMicro {

	// the USB device for the Maestro
	private static UsbDevice maestro = null;
	
	// the information for identifying the Micro Maestro
	private static final int VENDOR_ID = 0x1ffb;
	private static final int PRODUCT_ID = 0x0089;
	
	// javax.usb codes
	private static final byte OUT_REQUEST = javax.usb.UsbConst.REQUESTTYPE_TYPE_VENDOR | 
			javax.usb.UsbConst.REQUESTTYPE_DIRECTION_OUT;
	private static final byte IN_REQUEST = javax.usb.UsbConst.REQUESTTYPE_TYPE_VENDOR | 
			javax.usb.UsbConst.REQUESTTYPE_DIRECTION_IN;
	
	// Maestro command codes for USB communication
	private static final byte REQUEST_SET_TARGET = (byte)0x85;
	private static final byte REQUEST_SET_SERVO_VARIABLE = (byte)0x84;
	private static final byte REQUEST_GET_VARIABLES = (byte)0x83;
	private static final byte REQUEST_SET_PARAMETER = (byte)0x82;
    private static final byte REQUEST_GET_PARAMETER = (byte)0x81;
	
	// the size in bytes of the data block for GET_VARIABLES
	// the first 98 bytes are general data, the rest are status per channel
	private static final int MAESTRO_MICRO_DATA_SIZE = 140;
	private static final int MAESTRO_MICRO_SERVO_OFFSET = 98;
	// number of bytes per channel block
	protected static final int BYTES_PER_SERVO_STATUS_BLOCK = 7;
	
	// parameter "addresses" [for channel 0]
	private static final int PARAMETER_SERVO_HOME = 30; // [for servo 0] 2 byte home position (0=off; 1=ignore)
	private static final int PARAMETER_SERVO_MIN = 32; // [for servo 0] 1 byte min allowed value (x2^6) 
	private static final int PARAMETER_SERVO_MAX = 33; // [for servo 0] 1 byte max allowed value (x2^6)
//	private static final int PARAMETER_SERVO_NEUTRAL = 34; // [for servo 0] 2 byte neutral position
//	private static final int PARAMETER_SERVO_RANGE = 36; // [for servo 0] 1 byte range
//	private static final int PARAMETER_SERVO_SPEED = 37; // [for servo 0] 1 byte (5 mantissa,3 exponent) us per 10ms
//	private static final int PARAMETER_SERVO_ACCELERATION = 38; // [for servo 0] 1 byte speed changes that much every 10ms
	private static final int BYTES_PER_SERVO_PARAMETER_BLOCK = 9; // use to get to other servos
	
	
	/**
	 * This static initializer gets called when the class is loaded. 
	 * 
	 * It finds the Micro Maestro USB device so can send commands and
	// retrieve data.
	 * 
	 */
	static {
//		System.out.println("i got loaded!");
		
		try {
			// get the list of USB services
			final UsbServices services = UsbHostManager.getUsbServices();
			// get the root virtual hub
	        final UsbHub hub = (UsbHub) services.getRootUsbHub();
	        
	        // loop thru devices on hub to find micro Maestro
	        for (UsbDevice device: (List<UsbDevice>) hub.getAttachedUsbDevices()) {
	        	// get the device descriptor
	        	UsbDeviceDescriptor devDesc = device.getUsbDeviceDescriptor();
	        	// match the vendor and product IDs
	    		if ((VENDOR_ID == devDesc.idVendor()) && (PRODUCT_ID == devDesc.idProduct())) {
	    			// save the device
	    			maestro = device;
	    			break;
	    		}
	        }
	        // make sure found it
	        if (maestro == null) {
	        	System.err.println("Micro Maestro NOT FOUND! Make sure device is attached.");
	        	throw new NotYetBoundException();
	        }

		} catch (UsbDisconnectedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		} catch (UsbException ex) {
			System.out.println(ex.getMessage());		
		}
	
	}
	
	/**
	 * Sends a set target command to the Maestro.
	 * 
	 * @param channel = the channel or servo (0 based)
	 * @param target = the target position (pulse width in 0.25 microsecond)
	 */
	public static void setTargetRaw(int channel, int target) {

		// create the control IRP for sending the command out
		final UsbControlIrp irp = maestro.createUsbControlIrp(
        		OUT_REQUEST, REQUEST_SET_TARGET, (short) target, (short) channel);
        try {
        	// send the command
        	maestro.syncSubmit(irp);
		} catch (UsbDisconnectedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UsbException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Sets the speed for a channel in Maestro
	 * 
	 * @param channel canonical channel
	 * @param speed canonical speed
	 */
	public static void setSpeed(int channel, int speed) {

		setServoVariableRaw((short) channel, (short) speed); 
		
	}
	
	/**
	 * Sets the acceleration for a channel in Maestro
	 * 
	 * @param channel canonical channel
	 * @param acceleration canonical acceleration
	 */
	public static void setAcceleration(int channel, int acceleration) {

		setServoVariableRaw((short) (channel | 0x80), (short) acceleration); 
		
	}

	public static void setServoHome(int servo, int home) {
		// calculate the count
		setServoParameterRaw(PARAMETER_SERVO_HOME, (short)servo, (short)home, 2);
		
	}
	
	public static void setServoMinimum(int servo, int minimum) {
		setServoParameterRaw(PARAMETER_SERVO_MIN, (short)servo, (short)minimum, 1);
		
	}
	
	public static void setServoMaximum(int servo, int maximum) {
		setServoParameterRaw(PARAMETER_SERVO_MAX, (short)servo, (short)maximum, 1);
		
	}

	
	/**
	 * Sets a servo variable with raw parameters
	 * 
	 * @param channel raw channel
	 * @param value raw value
	 */
	private static void setServoVariableRaw(short channel, short value ) {
		// create the control IRP for sending the command out
		final UsbControlIrp irp = maestro.createUsbControlIrp(
        		OUT_REQUEST, REQUEST_SET_SERVO_VARIABLE, value, channel);
        try {
        	// send the command
        	maestro.syncSubmit(irp);
		} catch (UsbDisconnectedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UsbException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**/
	
	/**
	 * Reads the variables array from the device. The array contains
	 * general device info and servo status.
	 * 
	 * @return the variables array
	 */
	private static byte[] getVariablesRaw() {
		final UsbControlIrp request = maestro.createUsbControlIrp(IN_REQUEST, 
				REQUEST_GET_VARIABLES, (short) 0, (short) 0);
		byte[] data = new byte[MAESTRO_MICRO_DATA_SIZE];
		request.setData(data);

		// get the variables
		try {
			maestro.syncSubmit(request);
//			System.out.println("done");
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UsbDisconnectedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UsbException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return data;
	}
	
	/**
	 * Gets the status for a channel (can be a servo)
	 * 
	 * For a servo the return status array in in the order:
	 * - position
	 * - target (can be different than position if moving)
	 * - speed 
	 * - acceleration 
	 * 
	 * @param channel
	 * @return status array
	 */
	public static int[] getChannelStatusRaw(int channel) {
		// order is position, target, speed, acceleration (for a servo)
		int[] status = new int[4]; 
		
		// get the variables
		byte[] variables = getVariablesRaw();
		
		// get to desired channel
		int index = MAESTRO_MICRO_SERVO_OFFSET + (BYTES_PER_SERVO_STATUS_BLOCK * channel);
		
		// create position, target, speed, acceleration
		status[0] = asInt(variables[index], variables[index + 1]);
		status[1] = asInt(variables[index + 2], variables[index + 3]);
		status[2] = asInt(variables[index + 4], variables[index + 5]);
		status[3] = (int) variables[index + 6];
		
		return status;
	}
	
	/**
	 * Returns true if the indicated servo (channel) is moving
	 * 
	 * @param servo
	 * @return
	 */
	public static boolean servoMoving(int servo) {
		
		boolean moving = false;
		
		// get the variables
		byte[] variables = getVariablesRaw();
		
		// calculate offset to status block for channel
		int index = MAESTRO_MICRO_SERVO_OFFSET + (BYTES_PER_SERVO_STATUS_BLOCK * servo);
		
		// compare the position and target low and high bytes
		if ((variables[index] != variables[index + 2]) || 
				(variables[index + 1] != variables[index + 3])) {
			moving = true;
		}
		
		return moving;
	}
	
	/**
	 * Returns true if any of servos listed moving. This form looks for
	 * a contiguous block starting with 0.
	 * 
	 * A servo is moving if the position differs from the target
	 * 
	 * @param number of servos to check
	 * @return true if one or more of servos is moving
	 */
	public static boolean servosMoving(int number) {
		
		boolean moving = false;
		
		// get the variables
		byte[] variables = getVariablesRaw();
		
		// check each servo 
		for (int servo = 0; servo <= (number - 1); servo++) {
			// calculate offset to status block
			int index = MAESTRO_MICRO_SERVO_OFFSET + (BYTES_PER_SERVO_STATUS_BLOCK * servo);
			
			// compare the position and target low and high bytes
			if ((variables[index] != variables[index + 2]) || 
					(variables[index + 1] != variables[index + 3])) {
				moving = true;
				break;
			}
		}
		
		return moving;

	}
	
	/**
	 * Sets a servo parameter with raw parameters
	 * 
	 * @param parameter the offset to servp 0 parameter block
	 * @param channel raw channel
	 * @param value raw value of parameter
	 * @param length of value in bytes
	 */
	private static void setServoParameterRaw(int parameter, short channel, short value, int length) {
		
		// calculate the actual offset into servo parameter blocks
		int offset = parameter + (channel * BYTES_PER_SERVO_PARAMETER_BLOCK);
		
		// create index for IRP
		short index = (short) ((length << 8) + offset); // put length of value in high byte
		
		// create the control IRP for sending the command out
		final UsbControlIrp irp = maestro.createUsbControlIrp(
        		OUT_REQUEST, REQUEST_SET_PARAMETER, value, index);
        try {
        	// send the command
        	maestro.syncSubmit(irp);
		} catch (UsbDisconnectedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UsbException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Gets a controller parameter
	 * 
	 * @param parameter index into parameters
	 * @param channel servo channel
	 * @param length length of parameter in bytes
	 * @return
	 */
	private static int getParameterRaw(int parameter, short channel, int length) {

		// calculate the actual offset into servo parameter blocks
		int index = parameter + (channel * BYTES_PER_SERVO_PARAMETER_BLOCK);
		
		// create output buffer
		byte[] out = new byte[length];

		final UsbControlIrp request = maestro.createUsbControlIrp(IN_REQUEST, 
				REQUEST_GET_PARAMETER, (short) 0, (short) index);
		request.setData(out);
//		request.setData(new byte[MAESTRO_MICRO_DATA_SIZE]);
		try {
			maestro.syncSubmit(request);
//			System.out.println("done");
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UsbDisconnectedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UsbException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (length == 1) {
			return (int) (out[0] & 0xff);
		} else {			
//			System.out.println("b1:" + (int) (out[0] & 0xff) + " b2:" + (int) (out[1] & 0xff));
			return asInt(out[0], out[1]);
		}
		
	}
	

	
	protected static int asInt(byte lo, byte hi) {
		return (int) ((hi << 8) | (lo & 0xFF));
	}
	
	public static int getMinRaw(int channel) {
		
		int data = getParameterRaw(PARAMETER_SERVO_MIN, (short)channel, 1);
		System.out.println("min " + data);
		return data * 16;
	}

	public static int getMaxRaw(int channel) {
		
		int data = getParameterRaw(PARAMETER_SERVO_MAX, (short)channel, 1);
		System.out.println("max " + data);
		return data * 16;
	}
	
	public static int getHomeRaw(int channel) {
		
		int data = getParameterRaw(PARAMETER_SERVO_HOME, (short)channel, 2);
		System.out.println("home " + data);
		return data / 4;
	}

	
	public static void main(String[] args) {
//		UsbDevice dev = getMaestro();
		System.out.println("dev " + maestro );
//		System.out.println("dev "  );
		
//		setTargetRaw((short)0, (short)6000);

//		getVariablesRaw();
		
//		int[] status = getChannelStatusRaw(0);
//		status = getChannelStatusRaw(0);
		
//		int d2 = 0;
//		d2++;
		
//		setSpeed(0, 4);
		
//		setAcceleration(0, 2);
		
		int servo = 1;
		
		System.out.println("for servo " + servo);
		
//		setServoParameterRaw(PARAMETER_SERVO_HOME, (short)servo, (short)5600, 2);

		int data = getParameterRaw(PARAMETER_SERVO_HOME, (short)servo, 2);
		System.out.println("home " + data);

//		setServoParameterRaw(PARAMETER_SERVO_MIN, (short)servo, (short)67, 1);
		
		data = getParameterRaw(PARAMETER_SERVO_MIN, (short)servo, 1);
		System.out.println("min " + data);
		
//		setServoParameterRaw(PARAMETER_SERVO_MAX, (short)servo, (short)120, 1);

		data = getParameterRaw(PARAMETER_SERVO_MAX, (short)servo, 1);
		System.out.println("max " + data);


//		setSpeed(servo, 4);

		
//		data = getParameterRaw(PARAMETER_SERVO_NEUTRAL, (short)servo, 2);
//		System.out.println("neutral " + data);
//
//		data = getParameterRaw(PARAMETER_SERVO_RANGE, (short)servo, 1);
//		System.out.println("range " + data);

//		data = getParameterRaw(PARAMETER_SERVO_SPEED, (short)servo, 1);
//		System.out.println("speed " + data);
//
//		data = getParameterRaw(PARAMETER_SERVO_ACCELERATION, (short)servo, 1);
//		System.out.println("accel " + data);


		
		//		int data = getParameterRaw(32, (short)0, 1);
		
//		status = getChannelStatusRaw(0);
		
//		System.out.println("data " + data);
	}

}
