/**
 * This supports the simply type of socket communication required by the Raspberry Pi based
 * subsystems. This includes the camera and the UI.
 * 
 */
package org.gaf.ttt.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

//import org.gaf.image_analysis.TicTacToeAnalyzer;

public class SocketCommunicator {
	
	static final boolean DEBUG = false;
//	static final boolean DEBUG = true;
	static final boolean DEBUG_T = false;
//	static final boolean DEBUG_T = true;
	
	// communication objects
	private Socket connection = null; // the connection
	private InputStream inputStream = null; // input stream
	PrintStream outputStream = null; // output stream
	
	/**
	 * This constructor does the initialization possible to set up the socket.
	 * 
	 * @param address is a array of bytes forming the 16 bit IP address
	 * @param port is the port number
	 * @throws IOException 
	 */
	public SocketCommunicator(byte[] address, int port) throws IOException {
	
		// validate IP address parameter
		InetAddress host = null;
		host = InetAddress.getByAddress(address);

		if (DEBUG) {
			String thadd = host.getHostAddress();
			System.out.println(thadd);
		}

		// get a connection
		connection = new Socket(host, port);
		connection.setSoTimeout(300000); // 5 minutes
		
		// create input and output streams
		inputStream = connection.getInputStream();
		outputStream = new PrintStream(connection.getOutputStream());
	
	}

	/**
	 * This constructor does the initialization possible to set up the socket.
	 * 
	 * This constructor default the port to 9000
	 * 
	 * @param address is a array of bytes forming the 16 bit IP address
	 * @throws IOException 
	 */
	public SocketCommunicator(byte[] address) throws IOException {
		
		// establish communication
		this(address, 9000);
	
//		// validate IP address parameter
//		InetAddress host = null;
//		host = InetAddress.getByAddress(address);
//
//		if (DEBUG) {
//			String thadd = host.getHostAddress();
//			System.out.println(thadd);
//		}
//
//		// get a connection
//		connection = new Socket(host, 9000);
//		connection.setSoTimeout(90000); // 90 seconds
//		
//		// create input and output streams
//		inputStream = connection.getInputStream();
//		outputStream = new PrintStream(connection.getOutputStream());
	
	}
	
	/**
	 * Sends a command to the target device. 
	 * 
	 * @param command 
	 * @throws IOException 
	 */
	public void sendCommand(String command) throws IOException {
		
		// send command
		outputStream.print(command);
		
	}

	
	/**
	 * Sends a command to the target device. Returns a "status" response.
	 * 
	 * Depending on the command, could be an data length, or state of push buttons.
	 * 
	 * @param command
	 * @return a status response  
	 * @throws IOException 
	 */
	public int sendCommandGetStatus(String command) throws IOException {
		
		int bytesRead = -1; // bytes read from one socket read
		int response = 0; // response to command
		
		// buffer into which to read data from socket
		byte[] content = new byte[16];
		
		if(DEBUG) System.out.println("Sending command: " + command);
		
		// send command
		outputStream.print(command);
		
		// get the 4 bytes that comprise the response
		response = 0;
		while ((bytesRead = inputStream.read(content, 0, 4)) != -1) {
			// System.out.println("read "+ bytesRead);
			if (bytesRead == 4)
				break;
		}
		// convert byte to integer
		if (bytesRead == 4) {
			response = ((content[3] & 0xff) << 24)
					| ((content[2] & 0xff) << 16)
					| ((content[1] & 0xff) << 8) | (content[0] & 0xff);
			if (DEBUG)
				System.out.println("Response: " + response);
		} else { // got serious error, like EOF
			System.out.println("EOF trying to get response!");
			throw new IOException("Error getting response to command: " + command);
		}

		return response;		
	}
	
	/**
	 * Sends a command to target device. Returns a "big data" in a byte array.
	 * 
	 * @param command
	 * @return a "big data" response
	 * @throws IOException 
	 */
	public byte[] sendCommandGetData(String command) throws IOException {
				
		// send command and get length of big data to read
		int dataLen = sendCommandGetStatus(command);
		
		// retrieve the data and return
		return getData(dataLen);
	}

	/**
	 * Returns a "big data" in a byte array. Simply retrieves data.
	 * 
	 * @param dataLen length of data to retrieve
	 * @return a "big data" response
	 * @throws IOException 
	 */
	public byte[] getData(int dataLen) throws IOException {
		
		// debug timing
		long mStartC = 0;
		long mStopC = 0;
		
		// accumulator for "big" data if needed
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		// the "big data" as a byte array
//		byte[] dataBytes = null;
	
		int bytesRead = -1; // bytes read from one socket read
		int totalRead = 0; // total bytes read to a certain point
		
		// buffer into which to read data from socket
		byte[] content = new byte[4096];
		int bytes2read = 4096; // get as much as possible from socket
		
		if (DEBUG_T) {
			// start time
			mStartC = System.currentTimeMillis();
		}
		
//		// send command and get length of big data to read
//		int dataLen = sendCommandGetStatus(command);

		// read from the stream to get the image
		totalRead = 0;
		while ((bytesRead = inputStream.read(content, 0, bytes2read)) != -1) {
			// System.out.println("read "+ bytesRead);
			baos.write(content, 0, bytesRead);
			totalRead += bytesRead;
			if (totalRead == dataLen) { // we are done
				break;
			} else { // must calculate how many to read next time
				if (dataLen - totalRead >= 4096) { // fill the array
					bytes2read = 4096;
				} else { // limit to just what is needed
					bytes2read = dataLen - totalRead;
				}
			}
		}
		
		if (DEBUG_T) {
			mStopC = System.currentTimeMillis(); // stop time
			System.out.println("image xfer time millisec: "
					+ (mStopC - mStartC));
		}

		if (DEBUG) System.out.println("Big data length " + totalRead);		

		return baos.toByteArray();		
	}
	

	
	/**
	 * Closes the connection
	 * @throws IOException 
	 * 
	 */
	public void close() throws IOException {
		connection.close();
	}
	
	public static void main(String[] args) {
		
		// address for camera
		byte[] addr = { (byte) 192, (byte) 168, (byte) 1, (byte) 143 };
		
		// enable keyboard input
		Scanner input = new Scanner(System.in);
		
		SocketCommunicator commo = null;
		
		// init socket communication
		try {
			commo = new SocketCommunicator(addr);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		boolean goOn = true;
		while (goOn) {
			System.out.println("Enter command: i (image); q (quit); w (white balance) ");
			String decision = input.nextLine();
			System.out.println("decision:" + decision);
			if (decision.equals("q")) {
				goOn = false;
				continue;
			} else if (decision.equals("w")) {
				System.out.println("Enter type: n (sunlight); d (shade); f (fluorescent); i (incandescent) ");
				decision = input.nextLine();
				System.out.println("decision:" + decision);
				
				commo.sendCommand("set_wb:" + decision);
				
			} else if (decision.equals("i")) {

				// request an image
				byte[] imageBytes = commo.sendCommandGetData("send_pic");

//				int number = commo.sendCommandGetStatus("send_pic");
//				System.out.println("length of image " + number + " now get it");
//				byte[] imageBytes = commo.getData(number);
				
				
				System.out.println("Got: " + imageBytes.length);

				Path pathw = FileSystems.getDefault().getPath("",
						"scene.jpg");
				Files.write(pathw, imageBytes);
			}

//			TicTacToeAnalyzer tttInspect = new TicTacToeAnalyzer();
//			tttInspect.initImage(imageBytes);
			
			// find out about cells
//			int type = 99;
//			for (int row=0; row<3; row++) {
//				for (int col=0; col<3; col++) {
//					type = tttInspect.getCellContent(row, col);
//					System.out.print("Cell (" + row + "," + col + ") ");
//					switch (type) {
//					case CellTypeDetector.NO_REG:
//						System.out.println("NO Registration!");
//						break;
//					case TicTacToeGameBoard.EMPTY:
//						System.out.println("Empty cell");
//						break;
//					case TicTacToeGameBoard.NAUGHT:
//						System.out.println("O cell");
//						break;
//					case TicTacToeGameBoard.CROSS:
//						System.out.println("X cell");
//						break;
//					}
//				}
//			}
//			
//			boolean OK = tttInspect.findBoardState();
//			if (!OK) {
//				System.out.println("Problem with image. Registration error!");
//			} else {
//				TicTacToeGameBoard status = tttInspect.getBoardState();
//				System.out.println(status);
//			}

			
		}
		commo.close();

	} catch (IOException e) {
		System.out.println("IO Error:" + e.getMessage());
		e.printStackTrace();

	}

	System.out.println("Done!");

	}

}
