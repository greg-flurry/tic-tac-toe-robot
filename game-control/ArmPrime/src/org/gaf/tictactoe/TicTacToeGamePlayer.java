/**
 * This contains the logic of moving, planning the robot (NAUGHT) moves, and
 * win detection. 
 * 
 * The board is represented by a two dimensional array of int.
 * 
 * Each cell (0,0) - (2,2) can be a -1 (X), 0 (open), 1 (O). 
 * 
 * So-called corner cells are (0,0), (0,2), (2,0), (2,2).
 * So-called edge cells are (0,1), (1,0), (1,2), (2,1)
 * The middle cell is (1,1)
 * 
 */
package org.gaf.tictactoe;

import org.gaf.ttt.common.TicTacToeGameBoard;

public class TicTacToeGamePlayer {
	
	// status of ttt board; see above for content meaning
	private TicTacToeGameBoard board = null;
	
	// moves made by opponent
	private int round = 0;
	
	// move of opponent:
	// round 1: corner, edge, middle
	// round 2: corner/corner, corner/edge (middle good guy), middle/anywhere, edge/anywhere
	// round 3,4: by this time, blocking or winning
	private enum MoveSeq {C, E, M, CC, CE, MX, EX, R3, R4};
	
	// records the sequence of the opponent
	private MoveSeq sequence; 
	
	// last opponent move
	private int[][] lastMoves = new int[2][2]; 
	
	/** constructor
	 * 
	 * Creates the game board for logic
	 */
	public TicTacToeGamePlayer() {
		// get a new board
		board = new TicTacToeGameBoard();
	}	
	

	/**
	 * Puts an opponent move on the board, determines robot move, makes it
	 * 
	 * @param oppMove by the opponent
	 * 
	 * @return coordinates of robot move, where [9,9] means draw
	 */
	public int[] makeOpponentMove(int[] oppMove) {

		// make opponent move
		makeMove(oppMove, TicTacToeGameBoard.CROSS);
		
		// plan a robot move
		int[] robotMove = planRobotMove();
		if (robotMove[0] == -1) { // found a draw!
			// should only happen after 3 or 4 moves
			// return the information that there is no move; in fact a draw
			robotMove[0] = TicTacToeGameBoard.DRAW;
			robotMove[1] = TicTacToeGameBoard.DRAW;
		} else { 	
			// make robot move
			makeMove(robotMove, TicTacToeGameBoard.NAUGHT);
		}
		
		return robotMove;
	}

	
	/**
	 * Determines if there is a a winner
	 * 
	 * @return 1, -1, or 99 means no winner yet
	 */
	public int checkForWinner() {

		int winner = TicTacToeGameBoard.UNKNOWN;
		
		// check for win where possible
		if (round == 3) { // could have a winner
			// check for winner
			winner = checkForWin();	
			if (winner == TicTacToeGameBoard.UNKNOWN) winner = TicTacToeGameBoard.UNKNOWN; // could still get a mistake allowing robot win
		} else if (round == 4) {
			winner = checkForWin();	
			if (winner == TicTacToeGameBoard.UNKNOWN) winner = TicTacToeGameBoard.DRAW; // after 4 moves no win possible
		}
		return winner;
	}
	
	private void makeMove(int[] aMove, int player) {
		int row = aMove[0];
		int col = aMove[1];
		board.setCell(aMove, player);
		if (player == TicTacToeGameBoard.CROSS) {
			// get round
			round++;
			// preserve previous move
			lastMoves[1][0] = lastMoves[0][0];
			lastMoves[1][1] = lastMoves[0][1];			
			// set last move
			lastMoves[0][0] = row;
			lastMoves[0][1] = col;
			// set sequence
			if (round == 1) {
				// middle
				if (row == 1 && col == 1) {
					sequence = MoveSeq.M;
				} else if ((row == 0 || row == 2) && (col == 0 || col == 2)) { // corner?
					sequence = MoveSeq.C;
				} else { // edge
				sequence = MoveSeq.E;
				}				
			} else if (round == 2) {
				switch (sequence) {
				case C: 
					// if second opponent move to corner
					if ((row == 0 || row == 2) && (col == 0 || col == 2)) { // corner
						// set the sequence
						sequence = MoveSeq.CC;
					} else { // move to edge (can't be middle)
						sequence = MoveSeq.CE;
					}
					break;
				case M:
					sequence = MoveSeq.MX;
					break;
				case E: 
					sequence = MoveSeq.EX;
					break;
				default:
					break;
				}				
			} else if (round == 3) {
				sequence = MoveSeq.R3;
			} else if (round == 4) {
				sequence = MoveSeq.R4;
			}
		}
	}
	
	/**
	 * Plan a move by the good guy (robot)
	 * 
	 * Depends on knowing the opponent sequence and the first two opponent moves
	 * 
	 * @return coordinates of robot move; (-1,-1) for draw
	 */
	private int[] planRobotMove() {
		
		int move[] = new int[2];
				
		switch (sequence) {
		case C: 
			// take the middle 
			move[0] = 1; move[1] = 1;
			break;
		case CE: 
			// check for possible win and block
			move = checkForWinningMove(TicTacToeGameBoard.CROSS);
			if (move[0] == -1) { // nothing to block
				// move to corner that blocks both moves
				// can figure out which by looking at distance between row/col of moves
				int rowDiff = Math.abs(lastMoves[1][0] - lastMoves[0][0]);
				int colDiff = Math.abs(lastMoves[1][1] - lastMoves[0][1]);
				if (rowDiff < colDiff) {
					// the move is defined by corner row and edge col
					move[0] = lastMoves[1][0];
					move[1] = lastMoves[0][1];
				} else { 
					// the move is defined by edge row and corner col
					move[0] = lastMoves[0][0];
					move[1] = lastMoves[1][1];				
				}
			}
			break;
		case CC: 
			// must check to see if a win possible to determine blocking move
			move = checkForWinningMove(TicTacToeGameBoard.CROSS);
			if (move[0] == -1) { // nothing to block
				// pick an edge
				move[0] = 1; move[1] = 0;
			}
			break;
		case M: 
			// take any corner
			move[0] = 0; move[1] = 0; 
			break;
		case MX: // just block any possible win
			move = checkForWinningMove(TicTacToeGameBoard.CROSS);
			if (move[0] == -1) { // nothing to block
				// opponent move in corner opposite good guy last move
				// pick an empty corner
				if (board.getCell(0, 0) == TicTacToeGameBoard.EMPTY) { move[0] = 0; move[1] = 0;}
				if (board.getCell(0, 2) == TicTacToeGameBoard.EMPTY) { move[0] = 0; move[1] = 2;}
				if (board.getCell(2, 0) == TicTacToeGameBoard.EMPTY) { move[0] = 2; move[1] = 0;}
				if (board.getCell(2, 2) == TicTacToeGameBoard.EMPTY) { move[0] = 2; move[1] = 2;}				
			}			
			break;
		case E: 
			// move in a corner next to opponent move
			// if opp move to middle row
			if (lastMoves[0][0] == 1) {
				// move to same col, either above or below
				move[1] = lastMoves[0][1];
				move[0] = 0;
			} else {
				// move to same row, either left or right
				move[0] = lastMoves[0][0];
				move[1] = 0;				
			}
			break;
		case EX: // block any possible win
			move = checkForWinningMove(TicTacToeGameBoard.CROSS);
			if (move[0] == -1) { // nothing to block
				// move to middle
				move[0] = 1; move[1] = 1;
			}
			break;
		case R3: // win or block 
			// check for winning move by robot
			move = checkForWinningMove(TicTacToeGameBoard.NAUGHT);
			if (move[0] == -1) { // no winning move
				// check to see if need to block
				move = checkForWinningMove(TicTacToeGameBoard.CROSS);
				if (move[0] == -1) { // should check for move that enables a future robot win 
					move = checkForSetupMove();
				}				
			}
			break;
		case R4:
			// check for winning move by robot
			move = checkForWinningMove(TicTacToeGameBoard.NAUGHT);
			if (move[0] == -1) { // no winning move
				// check to see if need to block
				move = checkForWinningMove(TicTacToeGameBoard.CROSS);
				if (move[0] == -1) { // HAVE A DRAW! because nobody can win
					System.out.println("We seem to have found a DRAW!");
				}				
			}
			break;
		}
		return move;
	}

	
	/**
	 * Looks for a move that after made enables a future ROBOT win, if the opponent makes a mistake.
	 * 
	 * This can occur when have two open cells in conjunction with a robot token in a row,
	 * column, or diagonal.
	 * 
	 * @return move location that can enable a win on a later move
	 */
	private int[] checkForSetupMove() {
		int winner[] = {-1,-1};
						
		// check for row win (one O token and two empty cells)
		for (int i = 0; i < 3;  i++) { 
			// find if have one O and two empty
			int index = checkOneRobotTwoEmpty(board.getCell(i, 0), board.getCell(i, 1), board.getCell(i, 2));
			if (index != -1) { // have a valid set
				// set enabling row
				winner[0] = i;
				// set enabling col
				winner[1] = index;
				
				// return
				return winner;				
			}
		}
				
		// check for col win
		for (int i = 0; i < 3; i++) {
			// find if have one O and two empty
			int index = checkOneRobotTwoEmpty(board.getCell(0, i), board.getCell(1, i), board.getCell(2, i));
			if (index != -1) { // have a valid set
				// set enabling col
				winner[1] = i;
				// set enabling row
				winner[0] = index;
				
				// return
				return winner;				
			}			
		}

		
		// check for diag win
		// start at upper left
		// find if have one O and two empty
		int index = checkOneRobotTwoEmpty(board.getCell(0, 0), board.getCell(1, 1), board.getCell(2, 2));
		if (index != -1) { // have a valid set
			switch (index) {
			case 0: // should not happen, but do first cell anyway
				winner[0] = 0;
				winner[1] = 0;
				break;
			case 1: // second cell
				winner[0] = 1;
				winner[1] = 1;
				break;
			case 2: // third cell
				winner[0] = 2;
				winner[1] = 2;
				break;			
			}
			
			// return
			return winner;				
		}			
		
		// do upper right
		index = checkOneRobotTwoEmpty(board.getCell(0, 2), board.getCell(1, 1), board.getCell(2, 0));
		if (index != -1) { // have a valid set
			switch (index) {
			case 0: // should not happen, but do first cell anyway
				winner[0] = 0;
				winner[1] = 2;
				break;
			case 1: // second cell
				winner[0] = 1;
				winner[1] = 1;
				break;
			case 2: // third cell
				winner[0] = 2;
				winner[1] = 0;
				break;			
			}
			
			// return
			return winner;				
		}			
		
		// if get to here return no possible winner
		return winner;
	}

	/**
	 * Determines if a set of cells is valid candidate for a set up for win.
	 * 
	 * @param cell0
	 * @param cell1
	 * @param cell2
	 * @return the index of the last empty cell as a move candidate, else -1 
	 */
	private int checkOneRobotTwoEmpty(int cell0, int cell1, int cell2) {
		int numO = 0; // number of O cells in set
		int numE = 0; // number of empty cells in set
		int lastE = -1; // index of last empty found
		
		// find number of O and empty
		if (cell0 == TicTacToeGameBoard.NAUGHT) {
			numO++;
		} else if (cell0 == TicTacToeGameBoard.EMPTY) {
			numE++;
			lastE = 0;
		}
		if (cell1 == TicTacToeGameBoard.NAUGHT) {
			numO++;
		} else if (cell1 == TicTacToeGameBoard.EMPTY) {
			numE++;
			lastE = 1;
		}
		if (cell2 == TicTacToeGameBoard.NAUGHT) {
			numO++;
		} else if (cell2 == TicTacToeGameBoard.EMPTY) {
			numE++;
			lastE = 2;
		}

		// now make sure there is one O and two empty
		if (numO == 1 && numE == 2) { // have good stuff
			// return the index of the last empty
			return lastE;
		} else { // not valid
			// return invalid candidate
			return -1;
		}
	}
	
	/**
	 * Looks for any possible row or column or diagonal winning move (useful after
	 * a second move by both players).
	 * 
	 * @param player that might win
	 * @return winning move location or (-1,-1) for no win possible
	 */
	private int[] checkForWinningMove(int player) {
		int winner[] = {-1,-1};
		
		// set metric for possible win
		int metric = 2 * player;
				
		// check for row win		
		for (int i = 0; i < 3;  i++) { 
			if (board.getCell(i, 0) + board.getCell(i, 1) + board.getCell(i, 2) == metric) { // win possible
				// set blocking row
				winner[0] = i;
				// find blocking move, and set blocking col
				if (board.getCell(i, 0) == TicTacToeGameBoard.EMPTY) winner[1] = 0;
				if (board.getCell(i, 1) == TicTacToeGameBoard.EMPTY) winner[1] = 1;
				if (board.getCell(i, 2) == TicTacToeGameBoard.EMPTY) winner[1] = 2;
				return winner;
			}
		}
				
		// check for col win
		for (int i = 0; i < 3; i++) {
			if (board.getCell(0, i) + board.getCell(1, i) + board.getCell(2, i) == metric) { // win possible
				// set blocking col
				winner[1] = i;
				// find blocking move, and set blocking row
				if (board.getCell(0, i) == 0) winner[0] = 0;
				if (board.getCell(1, i) == 0) winner[0] = 1;
				if (board.getCell(2, i) == 0) winner[0] = 2;
				return winner;
			}
		}
		
		// check for diag win
		// start at upper left
		if ((board.getCell(0, 0) + board.getCell(1, 1) + board.getCell(2, 2)) == metric) { // win possible
			if (board.getCell(0, 0) == TicTacToeGameBoard.EMPTY) {
				winner[0] = 0;
				winner[1] = 0;
			}
			if (board.getCell(1, 1) == TicTacToeGameBoard.EMPTY) {
				winner[0] = 1;
				winner[1] = 1;
			}
			if (board.getCell(2, 2) == TicTacToeGameBoard.EMPTY) {
				winner[0] = 2;
				winner[1] = 2;
			}
			return winner;
		}
		
		// do upper right
		if ((board.getCell(0, 2) + board.getCell(1, 1) + board.getCell(2, 0)) == metric) { // win possible
			if (board.getCell(0, 2) == TicTacToeGameBoard.EMPTY) {
				winner[0] = 0;
				winner[1] = 2;
			}
			if (board.getCell(1, 1) == TicTacToeGameBoard.EMPTY) {
				winner[0] = 1;
				winner[1] = 1;
			}
			if (board.getCell(2, 0) == TicTacToeGameBoard.EMPTY) {
				winner[0] = 2;
				winner[1] = 0;
			}
			return winner;
		}		
		
		return winner;
	}

	
	/**
	 * Looks for a row or column or diagonal win by either player.
	 * 
	 * @return the player that won or UNKNOW (99) for no winner
	 */
	private int checkForWin() {
		int winner = TicTacToeGameBoard.UNKNOWN;
				
		// check for row win		
		for (int i = 0; i < 3;  i++) { 
			if (Math.abs(board.getCell(i, 0) + board.getCell(i, 1) + board.getCell(i, 2)) == 3) { // win
				// set winner
				winner = board.getCell(i, 0);
				return winner;
			}
		}
				
		// check for col win
		for (int i = 0; i < 3; i++) {
			if (Math.abs(board.getCell(0, i) + board.getCell(1, i) + board.getCell(2, i)) == 3) { // win 
				// set winner
				winner = board.getCell(0, i);
				return winner;
			}
		}
		
		// check for diag win
		// start at upper left
		if (Math.abs(board.getCell(0, 0) + board.getCell(1, 1) + board.getCell(2, 2)) == 3) { // win 
			// set winner
			winner = board.getCell(1, 1);
			return winner;
		}
		
		// do upper right
		if (Math.abs(board.getCell(0, 2) + board.getCell(1, 1) + board.getCell(2, 0)) == 3) { // win 
			// set winner
			winner = board.getCell(1, 1);
			return winner;
		}		
		
		return winner;
	}
	

	/**
	 * Returns the logical board state as the game progresses.
	 * 
	 * This should only be called after makeOppenentMove() with a valid return, 
	 * i.e., no DRAW
	 * 
	 * @return the logical board state
	 */
	public TicTacToeGameBoard getBoardState() {
		return board;
	}
	
	@Override
	public String toString() {
		
		return board.toString();
		
	}
	
}
