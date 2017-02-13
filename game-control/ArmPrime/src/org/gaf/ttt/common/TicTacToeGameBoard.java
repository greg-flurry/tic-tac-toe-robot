/**
 * This class holds the game board status in terms of cell content.
 * It also defines common constants used throughout the game.

 * The board is represented by a two dimensional array of int.
 * 
 * Each cell (0,0) - (2,2) can be a 2 (X), 0 (empty), 1 (O). 
 *
 */
package org.gaf.ttt.common;

import javax.activity.InvalidActivityException;


public class TicTacToeGameBoard {

	private static final boolean DEBUG = false;
	
	// possible cell content
//	public static final int NO_REG = -100; // indicates could not find registration circle
	public static final int EMPTY = 0;
	public static final int NAUGHT = 1;
	public static final int CROSS = -1;
	
	// other win states
	public static final int DRAW = 9;
	public static final int UNKNOWN = 99;
	
	// the game board itself
	private int[][] gameBoard = new int[3][3];
	

	/**
	 * Public constructor
	 * 
	 * Simply initializes the game board
	 */
	public TicTacToeGameBoard() {
		// initialize to empty
		for (int row=0; row<3; row++) {
			for (int col=0; col<3; col++) {
				gameBoard[row][col] = EMPTY;
			}
		}
	}
	
	/**
	 * Sets the value of a cell in the game board to the indicated player
	 * 
	 * @param cell (row, col)
	 * @param player
	 */
	public void setCell(int[] cell, int player) {
		gameBoard[cell[0]][cell[1]] =  player;
	}
	
	/**
	 * Sets the value of a cell in the game board to the indicated player
	 * 
	 * @param row
	 * @param col
	 * @param player
	 */
	public void setCell(int row, int col, int player) {
		gameBoard[row][col] =  player;
	}
	
	
	/**
	 * Gets the value of a game cell.
	 * 
	 * @param row
	 * @param col
	 * @return player or empty
	 */
	public int getCell(int row, int col) {
		return gameBoard[row][col];
	}
	
	/** 
	 * Compares one instance of the class to another instance of the class. The 
	 * assumption is that this method is called on the physical representation of
	 * the game board created by TicTacToeAnalyzer, which is compared with the 
	 * logical representation of the board kept by TicTacToeGamePlayer.
	 * 
	 * Assumes that "goodness" results when there is a single additional X in the
	 * physical board. That new X must appear in a cell that is free in the logical
	 * board. 
	 * 
	 * @param logical representation of the board 
	 * @return
	 * @throws InvalidActivityException for (1) messed up board; (2) no new X; (3) >1 new X
	 */
	public int[] comparePhysicalLogical(TicTacToeGameBoard logical) throws InvalidActivityException {
		
		int cntNewX = 0; // count of new X found in prior empty cells
		int[] newX = {40, 40}; // row and col of (last) X found in prior empty cells

		
		// first phase to compare "old stuff", i.e., compare the physical token
		// to the logical player; this checks for messing with the tokens on 
		// the physical board prior the the current move; ignore empty cells
		// second phase to find the single new X in a formerly empty cell
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				int logCell = logical.getCell(row, col);
				if (DEBUG) System.out.println("logical cell " + row + "," + col + " = " + logCell);
				if (DEBUG) System.out.println("physical cell " + row + "," + col + " = " + this.getCell(row, col));
				// check for physical non-empty cells = logical non-empty cells
				// and physical state when logical empty
				if (logCell != EMPTY) { // check to make sure physical and logical match
					if (DEBUG) System.out.println("not empty");
					if (this.getCell(row, col) != logCell) {
						throw new InvalidActivityException("BAD1: Prior tokens moved!");
					}						
				} else { // check to make sure still empty or contains an X
					if (DEBUG) System.out.println("empty");
					if (this.getCell(row, col) == CROSS) {
						if (DEBUG) System.out.println("found cross");
						// record that found and record cell location
						newX[0] = row; newX[1] = col;
						cntNewX++;
					}
				}
			}
		}
		
		// check for bad activity
		if (cntNewX == 0) {
			throw new InvalidActivityException("BAD2: No new X played!");			
		} else if (cntNewX > 1) {
			throw new InvalidActivityException("BAD3: Multiple new X played!");					
		} else {
			return newX;
		}
		
	}
	
	/**
	 * Finds an empty cell.
	 * 
	 * Primarily intended to allow a play by robot even in the event of a known tie. This
	 * is needed because pick token and process image in parallel.
	 * 
	 * @return first empty cell in board
	 */
	public int[] findEmptyCell() {

		int[] eCell = {-1,-1};		
		int row = 0; 
		boolean found = false;
		
//		for (int row = 0; row<3; row++) {
//			for (int col = 0; col<3; col++) {
//				if (gameBoard[row][col] == EMPTY) {
//					eCell[0] = row;
//					eCell[1] = col;
//					break;
//				}
//			}
//			if (gameBoard[row][eCell[1]] == EMPTY) break;
//		}

		while (row < 3 && !found) {
			for (int col = 0; col<3; col++) {
				if (gameBoard[row][col] == EMPTY) {
					eCell[0] = row;
					eCell[1] = col;
					// indicate done
					found = true;
					break;
				}
			}
			row++;
		}

		return eCell;
	}

	
	@Override
	public String toString() {	
		return (tokenToString(gameBoard[0][0]) + " | " + 
				tokenToString(gameBoard[0][1]) + " | " +  
				tokenToString(gameBoard[0][2]) + "\n " + 
		        "---------\n" +
		        tokenToString(gameBoard[1][0]) + " | " + 
		        tokenToString(gameBoard[1][1]) + " | " + 
		        tokenToString(gameBoard[1][2]) + "\n " +
		        "---------\n" +
		        tokenToString(gameBoard[2][0]) + " | " + 
		        tokenToString(gameBoard[2][1]) + " | " + 
		        tokenToString(gameBoard[2][2]));	
	}
	
	private String tokenToString(int token) {
		switch (token){
		case EMPTY: 
			return " ";
		case CROSS:
			return "X";
		case NAUGHT:
			return "O";
		default:
			return "?";
		}
	}

}
