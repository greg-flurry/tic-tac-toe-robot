/**
 * The tic tac toe playing robot master program
 * 
 */
package org.gaf.ttt.master;

import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.util.Scanner;

import org.flurry.servo.ArmTTT;
import org.gaf.tictactoe.TicTacToeGamePlayer;
import org.gaf.ttt.common.SocketCommunicator;
import org.gaf.ttt.common.TicTacToeGameBoard;
import org.gaf.ttt.image_analysis.TicTacToeAnalyzer;
import org.gaf.ttt.image_capture.ImageDigester;

import javax.activity.InvalidActivityException;


public class TicTacToeRobot {

	// debug stuff
//	final static boolean DEBUG = true; // general debug
	final static boolean DEBUG = false;
//	final static boolean DEBUG_A = true; // arm movement
	final static boolean DEBUG_A = false;
	final static boolean DEBUG_S = true; // show logical and physical state
//	final static boolean DEBUG_S = false;
	
	static final String ARM_MOVING = "show&blink;r;STAY BACK!\nArm CAN HURT YOU";
	static final String SET_UP = "show&wait;y;Set up for play\nL=Done";
	static final String HUMAN_MOVE = "show&wait;g;Your move.\nL=Done    Quit=R";
	static final String DRAW = "show&wait;w;Tie!\nL=Again   Quit=R";
	static final String ROBOT_WIN = "show&wait;w;Robot Wins! Push\nL=Again   Quit=R";
	static final String NEW_GAME = "show&wait;w;New Game?\nL=Again   Quit=R";
	static final String BAD_IMAGE = "show&wait;y;Bad image!\nL=Retry   Quit=R";
	static final String FAIR_PLAY = "show&wait;y;Play Fair! Push\nL=Again   Quit=R";
	static final String HUMAN_WIN = "show&wait;w;You Win! Push\nL=Again   Quit=R";
	static final String THINKING = "show;y;Thinking ...\nPlease wait.";
	
	// address for the camera, beeper, UI on two Pi
//	static final byte[] cameraAddress = { (byte) 192, (byte) 168, (byte) 1, (byte) 143 };
	static final byte[] beeperAddress = { (byte) 192, (byte) 168, (byte) 1, (byte) 143 };
	static final byte[] UIAddress = { (byte) 192, (byte) 168, (byte) 1, (byte) 179 };
	static final int beeperPort = 9011; // the other two default to 9000

	// status for the UI
	static final  int LEFT_BUTTON = 1;
	static final  int RIGHT_BUTTON = 2;

//	static SocketCommunicator camera = null;
	static SocketCommunicator ui = null;
	static SocketCommunicator beeper = null;
	static ArmTTT arm = null;
	
//	static ImageCapturer ic = null;
	static ImageDigester ic = null;
	
	static Scanner input = null;

	/**
	 * The main program that coordinates all the other components
	 * @param args
	 */
	public static void main(String[] args) {
		
		String promptForNewGame = ""; // prompt for determining if want new game
		int yardCell = 0; // available cell in bone yard

		// get a new Analyzer 
		TicTacToeAnalyzer analyzer = new TicTacToeAnalyzer();
		
		// set up for keyboard input
		input = new Scanner(System.in);
		
		// check to make sure that the USB remains stable
		waitForEnter("when Parallels, Windows 8.1, and Maestro Controller running.");
				
		// check to make sure camera and UI running
		waitForEnter("when Camera, Beeper, and UI programs running on Pi.");

		// catch exceptions due to communication errors and bad image processing or cheating
		try {
			// set up communication with the programs on two Pi
			ui = new SocketCommunicator(UIAddress); // defaults to port 9000
			beeper = new SocketCommunicator(beeperAddress, beeperPort);
			// set up new thread to deal with camera and analysis
			ic = new ImageDigester(analyzer);
			ic.start();
			
			// make sure the arm power is on
			waitForEnter("when ARM power turned on.");			

			// determine if want to move to config home or not
			boolean goHome = true;
	        System.out.println("SKIP move home during configuration? (y/n)");
			String doit = input.next();
			if (doit.equals("y")) goHome = false;
			
			// send warning about arm movement
			doBeeperBlip();
			
			// set up arm 
			arm = ArmTTT.getInstance(goHome);
			arm.goNeutral();
			if (DEBUG_A) waitForEnter("when think arm initialized.");

			// now loop on unit of game 
			boolean doGames = true;
			while (doGames) {
				
		/*
		 * Could get here with token picked and maybe beeper
		 *  -- if quit a game
		 *  
		 *  probably assume at neutral and just drop token so that can be used at setup;
		 *  may have to turn off beeper unless have moved to a short beep period 
		 */
				
				// ask for board setup
				ui.sendCommandGetStatus(SET_UP);
				
//				// check that game board is clear
//				waitForEnter("when game board is cleared.");
//
//				// check that bone yard is set up
//				waitForEnter("when bone yard is set up.");

				// point at first cell in bone yard
				yardCell = 0; 
				
				// get a new logical game player
				TicTacToeGamePlayer player = new TicTacToeGamePlayer();
				
				// 
				// now loop within a game for moves
				//
				boolean makeMoves = true;
				int status = 0; // button status from UI
				boolean tokenPicked = false; // indicates that have a token in arm already
				boolean needAlarm = false; // indicates that need alarm when drop token
				while (makeMoves) {
					// prompt the player to make a move
					status = ui.sendCommandGetStatus(HUMAN_MOVE);
					// determine action
					if (status == RIGHT_BUTTON) { // want to quit
						// set prompt for new game
						promptForNewGame = NEW_GAME;
						// terminate this game
						makeMoves = false;
						continue;
					} else { // assume made a move (placed X in empty cell)
						// prompt the player to wait for robot
						status = ui.sendCommandGetStatus(THINKING);

						// request an image and processing
						ic.startEpisode();

						// wait for image to be captured 
						ic.awaitDesiredState(ImageDigester.State.SIZE);;
						if (DEBUG) System.out.println("got image size");
						
			/*
			 * Here need to decide if really pick up token or already done
			 */
						// check necessary action
						if (tokenPicked) { // had some error and token already picked
							if (DEBUG) System.out.println("Aleady picked token");	
							// indicate that need alarm for drop
							needAlarm = true;
						} else {
							// pick up token while transferring image
							if (DEBUG) System.out.println("about to pick token");
							pickToken(yardCell);
							tokenPicked = true;
							needAlarm = false;
						}

						// wait for analysis of image to complete
						if (DEBUG) System.out.println("about to wait for analysis");
						ic.awaitDesiredState(ImageDigester.State.BOARD);;
						if (DEBUG) System.out.println("got analysis!");

						// determine what to do based on success (or not) of analysis
						if (!analyzer.getAnalysisResult()) {
							// serious error
							System.out.println("Probably major image processing problem. Check lighting.");

							// save scene for analysis
							analyzer.saveScene();
							
							/*
							 * Must remember that could get here with token in arm and warnings going
							 */
							
							// replace token previously picked
							replaceYardToken(yardCell);
							tokenPicked = false;
			/*
			 * Could put this in a loop of some sort, much like the "cheating" is in a loop
			 * - coudl display "bad photo" or something and ask to redo or quit							
			 */
							// just die for now
							promptForNewGame = BAD_IMAGE;
							makeMoves = false;
//							doGames = false;
							continue;
							
						} else { // successful analysis
							if (DEBUG) System.out.println("\nSUCCESSFUL ANALYSIS!\n");
							
							// get the physical board
							TicTacToeGameBoard physical = analyzer.getBoardState();
							if (DEBUG_S) System.out.println("Physical\n" + physical + "\n");	
							
							// get the logical board
							TicTacToeGameBoard logical = player.getBoardState();
							if (DEBUG_S) System.out.println("Logical\n" + logical + "\n");	
							
							// now check the physical board for single new X
							int[] moveX = null;
							try {
								// determine if the board is logically correct and there is a single new X
								moveX = physical.comparePhysicalLogical(logical);
								if (DEBUG) System.out.println("X move = (" + moveX[0] + "," + moveX[1] + ")");

							} catch (InvalidActivityException ex) {
								System.out.println("Exception: " + ex.getMessage());
								// save scene
								analyzer.saveScene();
								// send error message 
								status = ui.sendCommandGetStatus(FAIR_PLAY);
								// determine action
								if (status == RIGHT_BUTTON) { // want to quit game
									// replace token in yard
									replaceYardToken(yardCell);
									
									// set prompt for new game
									promptForNewGame = NEW_GAME;
									// terminate this game
									makeMoves = false;
								} 
								
				/*
				 * Must remember that could get here with token in arm and warnings going
				 */
							
								// try again for valid play
								/* NOTE: the arm has a token, but will not move again */
								continue;

							}
							
							// update logical game with opponent move; determine robot move;
							// this updates the logical board with both moves
							int[] moveO = player.makeOpponentMove(moveX);
							if (DEBUG) System.out.println("O move = (" + moveO[0] + "," + moveO[1] + ")");

							// check for draw
							if (moveO[0] == TicTacToeGameBoard.DRAW) {
								if (DEBUG) System.out.println("Draw1!");
																
								// get a superfluous move to an empty cell; find draw later
								moveO = physical.findEmptyCell();
							}
							
							// place O token on correct cell
							if (DEBUG) System.out.println("about to drop token");
							DropToken(moveO, needAlarm);
							tokenPicked = false;
							needAlarm = false;
							
							// point at next yard position
							yardCell++;
							
							// check for a winner
							int winner = player.checkForWinner();
							if (winner == TicTacToeGameBoard.NAUGHT) {
								if (DEBUG) System.out.println("Robot WINS!");
								// set prompt for new game
								promptForNewGame = ROBOT_WIN;
								
								// terminate this game
								break;
							} else if (winner == TicTacToeGameBoard.CROSS) { 
								if (DEBUG) System.out.println("Opponent WINS! It is a MIRACLE!");
								// set prompt for new game
								promptForNewGame = HUMAN_WIN;
								
								// terminate this game
								break;
							} else if (winner == TicTacToeGameBoard.DRAW) { 
								if (DEBUG) System.out.println("Draw!");
								// set prompt for new game
								promptForNewGame = DRAW;
								
								// terminate this game
								break;
							} else if (winner == TicTacToeGameBoard.UNKNOWN) {
								if (DEBUG) System.out.println("Too early to tell who wins.");
							} 
						}									
					}
				} // makeMove (in game)				
				
				// determine action
				status = ui.sendCommandGetStatus(promptForNewGame);
				if (status == RIGHT_BUTTON) { // want to quit
					// terminate play
					doGames = false;
				} 				
			} // doGames (new game)		
		
		} catch (IOException e) {
			System.out.println("IO Error:" + e.getMessage());
			e.printStackTrace();
		} catch (AlreadyBoundException e) {
			System.out.println("Servo Error:" + e.getMessage());
			e.printStackTrace();
		} 
		
		// clean up
		try {
			ui.close();
			beeper.close();
			ic.terminate();
		} catch (IOException e) {
			System.out.println("Cleanup IO Error:" + e.getMessage());
			e.printStackTrace();
		}

		System.out.println("Done!");		
	}
	
	/**
	 * Picks up a token from the bone yard. 
	 * 
	 * Assumes arm at neutral position on entering. Leaves arm in neutral position.
	 * 
	 * Assumes that the pick time is longer than image transfer time and so does not
	 * stop the beep it starts.
	 * 
	 * @param yardCell where to pick up the taken
	 * @throws IOException 
	 */
	private static void pickToken(int yardCell) throws IOException {
		// send warning about arm movement
		doBeeperBlip();
		
		// go pick up token from yard
		if (DEBUG_A) {
			System.out.println("Moving to bone(" + yardCell + ")");
		}
		arm.goYard(yardCell);
		
		// grab the disc that should be there
		if (DEBUG_A) {
			System.out.println("Grippping");
		}
		arm.setGripper(ArmTTT.GRIPPER_CLOSED);
		
		// go to neutral
		if (DEBUG_A) {
			System.out.println("Move neutral");
		}
		arm.goNeutral();
		
//		// turn off beeper
//		beeper.sendCommand("stop");

		// wait for arm movement to complete
		if (DEBUG_A) waitForEnter("when think arm movement finished.");

	}

	
	/**
	 * Drops a token on the game board.
	 * 
	 * Assumes arm at neutral position on entering. Leaves arm in neutral position.
	 * 
	 * Assumes that the pick time is longer than image transfer time and so does not
	 * start a beep.
	 * 
	 * @param gameCell where to drop the token
	 * @param needAlarm indicates that need alarm before start movement
	 * @throws IOException 
	 */
	private static void DropToken(int[] gameCell, boolean needAlarm) throws IOException {
		
		if (needAlarm) {
			// send warning about arm movement
			doBeeperBlip();
		}
		
		// move to game position
		if (DEBUG_A) {
			System.out.println("Move to board (" + gameCell[0] + "," + gameCell[1] + ")");
		}
		arm.goGame(gameCell[0], gameCell[1]);
		
		// drop disc
		if (DEBUG_A) {
			System.out.println("Drop");
		}
		arm.setGripper(ArmTTT.GRIPPER_OPEN);
		
		// go neutral again
		if (DEBUG_A) {
			System.out.println("Move neutral");
		}
		arm.goNeutral();	
		
		// turn off beeper
		beeper.sendCommand("stop");

		// wait for arm movement to complete
		if (DEBUG_A) waitForEnter("when think arm movement finished.");

	}
	

	/**
	 * Puts token back in yard.
	 * 
	 * @param yardCell position
	 * @throws IOException 
	 */
	private static void replaceYardToken(int yardCell) throws IOException {
		
		// send warning about arm movement
		doBeeperBlip();
		
		// go to yard position
		if (DEBUG_A) {
			System.out.println("Moving to bone(" + yardCell + ")");
		}
		arm.goYard(yardCell);
		
		// drop disc
		if (DEBUG_A) {
			System.out.println("Drop");
		}
		arm.setGripper(ArmTTT.GRIPPER_OPEN);
		
		// go to neutral
		if (DEBUG_A) {
			System.out.println("Move neutral");
		}
		arm.goNeutral();
	}
	
	private static void waitForEnter(String prompt) {
		
		boolean OK = false;
		while (!OK) {
			System.out.println("Hit <enter> " + prompt);
			String decision = input.nextLine();
			if (decision.equals("y") || (decision.equals(""))) {
				OK = true;
			}
		}
	}


	/**
	 * Beeps the beeper for a short time
	 * @throws IOException 
	 * 
	 */
	private static void doBeeperBlip() throws IOException {
		// send warning about arm movement
		beeper.sendCommand("start");
		ui.sendCommandGetStatus(ARM_MOVING);
		// sleep to make sure human gets a decent warning
		try {
			Thread.sleep(2500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// turn off beeper
		beeper.sendCommand("stop");
	}

	
	/*	
	private static boolean waitForYN(String prompt) {
		
		boolean OK = false;
		boolean response = false;
		while (!OK) {
			System.out.println(prompt + " Enter y(es) or n(o)");
			String decision = input.nextLine();
//			System.out.println("decision:" + decision);
			if (decision.equals("y")) {
				response = true;
				OK = true;
			} else if (decision.equals("n")) {
				response = false;
				OK = true;
			}
		}
		
		return response;
	}
*/
	
}
