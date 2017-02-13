/**
 * This class retrieves a captured image from the remote camera. 
 * 
 * The implementation is a Thread intended to be run in parallel with the main program.
 * The design allows a series capture "episodes" to run. Each episode gets one and only 
 * one image. 
 * 
 * The process has a number of states. 
 * -- WORKING means that an episode has started, with no results yet
 * -- SIZE means that the image has been capture by the camera and is ready for transfer
 * -- IMAGE means the image has been transferred
 * 
 * Methods allow a caller to start an episode and wait on the state changes. For example, 
 * after the state changes to SIZE, it is possible to do other things while the thread
 * effects the transfer, e.g., move the arm.
 * 
 * There is no synchronization of data access while in the IMAGE state. That said, the 
 * state cannot change from IMAGE to WORKING unless the caller initiates an episode.
 * 
 */
package org.gaf.ttt.image_capture;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import org.gaf.ttt.common.SocketCommunicator;
import org.gaf.ttt.image_analysis.TicTacToeAnalyzer;
import org.opencv.core.Core;

public class ImageDigester extends Thread {
	
	private static final boolean DEBUG = false;

    private volatile boolean running = true; // governs thread life
    private volatile boolean paused = true; // governs episode start/stop; starts paused
    private final Object pauseLock = new Object(); // episode synchronization "lock"
    private final Object dataLock = new Object(); // state (of acquisition) lock
    
    // the state of image acquisition
    // WORKING = capture initiated, no size no image
    // SIZE = image captured and have size, but not transmitted
    // IMAGE = image transmitted and available
    // BOARD = image analyzed to provide game board
    private State captureState = State.WORKING;
    public enum State {WORKING, SIZE, IMAGE, BOARD};
   
	private SocketCommunicator commo = null; // communication capability
	
	private TicTacToeAnalyzer ta = null;

	private byte[] imageBytes = null; // image captured; only valid when state = IMAGE

    /**
     * Constructor that creates the communication capability
     * @throws IOException
     */
    public ImageDigester(TicTacToeAnalyzer ttta) throws IOException {
		if (DEBUG) System.out.println("New ImageDigester ...");	
		
		// save analyzer
		this.ta = ttta;	
		
		// address for camera
		byte[] addr = { (byte) 192, (byte) 168, (byte) 1, (byte) 143 };
				
		// establish communication with the camera
		commo = new SocketCommunicator(addr);
	}

    /** 
     * Run method primarily supports a succession of image capture episodes until terminated.
     * It leverages flags and the pauseLock to do so.
     * 
     * Uses the dataLock via various methods to manage image transfer.
     * 
     */
    @Override
    public void run () {
        while (running) {
            synchronized (pauseLock) {
                if (!running) { // may have changed while waiting to synchronize on pauseLock
                    break; // so can terminate
                }
                if (paused) {
                    try {
                    	// wait till signaled to start episode
                    	/* Thread blocks until another thread calls pauseLock.notifyAll().
                    	 * Calling wait() relinquishes the synchronized lock this thread
                    	 * holds on pauseLock so another thread can acquire the lock to
                    	 * call notifyAll().                  */
                          pauseLock.wait(); 
                    } catch (InterruptedException ex) {
                        break;
                    }
                    if (!running) { // running might have changed since we paused
                        break;
                    }
                }
            }
            // capture activities for an episode start here
			// request an image
            signalStateChange(State.WORKING);
            if (DEBUG) System.out.println("ImageDigester: State: " + captureState);
            
			try {
				// send a command to get a picture and return the data length
				int dataLen = commo.sendCommandGetStatus("send_pic");
				
				// signal got length
	            signalStateChange(State.SIZE);
				
	            // now retrieve the data
	            imageBytes = commo.getData(dataLen);
				
			} catch (IOException e) {
				System.out.println("Communication error with camera!");
				e.printStackTrace();
				terminate();
			}
			
			// signal that finished transfer
            signalStateChange(State.IMAGE);

            if (DEBUG) System.out.println("ImageDigester: State: " + captureState);
            
            // now analyze image
			boolean imageOK = ta.analyzeImage(imageBytes);
            System.out.println("Result of analysis: " + imageOK);
            
			// signal that finished analysis
            signalStateChange(State.BOARD);
           
            System.out.println("think actually did analysis");

                     
            // now pause to wait for next episode
            pause();
        }
        
        // 
        try {
        	// close communications
			commo.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        if (DEBUG) System.out.println("All done");
    }

    /**
     * Terminates the thread
     */
    public void terminate() {
        running = false;
        // start an episode so that can bail
        startEpisode();
    }

    /**
     * In effect stops an episode and allows the thread to get ready for another episode.
     */
    private void pause() {
        paused = true;
    }

    /**
     * Starts an episode.
     */
    public void startEpisode() {
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll(); // Unblocks thread
        }
    }
    
    /**
     * Waits for a change in capture state. 
     * 
     * @return the state just entered
     */
    public State awaitStateChange() {   	
    	// wait on the state to change
    	synchronized (dataLock) {
    		try {
				dataLock.wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}		
		}
    	
    	return captureState;
    }
    
    /**
     * Waits for a change in capture state to the desired state.
     */
    public void awaitDesiredState(State desiredState) {   	   	
		if (DEBUG) System.out.println("ImageDigester: about to wait state change to " + desiredState);
		if (desiredState == captureState) { // already in state so just return
			// do nothing
		} else { // must assume state change has not yet occurred
			while (true) {
		    	// wait on the state to change
				State myState = awaitStateChange();
				if (myState == desiredState) {
					if (DEBUG) System.out.println("ImageDigester: got state change to " + myState);
					break;
				} 					
			}
		}
    }

    
    private void signalStateChange(State newState) {
        synchronized (dataLock) {
        	captureState = newState;
        	dataLock.notifyAll(); // Unblocks thread
        }
    	
    }
    
    public byte[] getData() {
    	return imageBytes;
    }

	
	public static void main(String[] args) throws IOException {
		
		// Load the native OpenCV library.
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
		// enable keyboard input
		Scanner input = new Scanner(System.in);
		
		// get a new capturer
		ImageDigester ic = new ImageDigester(new TicTacToeAnalyzer());
		ic.start();
		
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
//				System.out.println("decision:" + decision);
				
//				commo.sendCommand("set_wb:" + decision);
				
			} else if (decision.equals("i")) {

				// request an image
				ic.startEpisode();
				
//				while (true) {
//					State myState = ic.awaitStateChange();
//					if (myState == State.SIZE) {
//						System.out.println("got size");
//						break;
//					} 					
//				}
//
//				while (true) {
//					State myState = ic.awaitStateChange();
//					if (myState == State.IMAGE) {
//						System.out.println("got image");
//						break;
//					} 					
//				}
				
				ic.awaitDesiredState(State.SIZE);;
				System.out.println("got size");
				
				try {
					Thread.sleep(10000);  // delay 10 seconds to emulate arm movement
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

//				ic.awaitDesiredState(State.IMAGE);;
//				System.out.println("got image");

				ic.awaitDesiredState(State.BOARD);;
				System.out.println("got image & analysis");

				
				byte[] imageBytes = ic.getData();
				System.out.println("Got: " + imageBytes.length);

				Path pathw = FileSystems.getDefault().getPath("", "scene.jpg");
				Files.write(pathw, imageBytes);
			}		
		}
		
		ic.terminate();
		input.close();

//	} catch (IOException e) {
//		System.out.println("IO Error:" + e.getMessage());
//		e.printStackTrace();
//
//	}

	System.out.println("Done!");

	}

}
