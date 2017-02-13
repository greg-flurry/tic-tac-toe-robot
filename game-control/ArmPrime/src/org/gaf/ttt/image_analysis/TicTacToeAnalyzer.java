/**
 * This provides analysis of a captured image. It "finds" the game board in the image.
 * It finds all the cells in the image. It processes cells to detect a token and determines
 * if a cell is empty or contains a X (or not a O). 
 * 
 * IMPORTANT NOTE: The OpenCV native library must be loaded for this class to function
 */

package org.gaf.ttt.image_analysis;

import java.awt.image.ImagingOpException;

import org.gaf.ttt.common.TicTacToeGameBoard;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Range;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

public class TicTacToeAnalyzer {
	
//	private static final boolean DEBUG = true;
	private static final boolean DEBUG = false;
	
	// for debug
	private Mat draw = null;
	
//	// possible cell finding results include normal states and this
//	public static final int NO_REG = -100; // indicates could not find registration circle
	
	// ROI used to crop the game board; will likely have to be adjusted on occasion
//	private static Rect gameROI = new Rect(1425, 885, 625, 625); 
	private static Rect gameROI = new Rect(1435, 875, 625, 625); 

	private Mat scene = null; // the full scene captured by camera (here only for debug)	
	private Mat game = null; // the game board cropped from the scene
	private Mat grayGame = null; // the gray scale rendering of the game board

	// the set of cell corners for the game board
	private static final int dim = 4;
	private Point[][] cellCorner = new Point[dim][dim];
	
	// the individual cells extracted from the game board
	Mat[][] cell = new Mat[3][3];
	
	// the game board status
	TicTacToeGameBoard gameStatus = null;
	
	// result of the analysis
	boolean boardOK = false;
	
	// the cell type detector used
	CellTypeDetectorCircle detector = new CellTypeDetectorCircle();

	/** 
	 * Generic constructor. Note that could include setImage() in here
	 */
	public TicTacToeAnalyzer() {
		if (DEBUG) System.out.println("TicTacToeAnalyzer");
		gameStatus = new TicTacToeGameBoard();

		// Load the native OpenCV library.
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}
	
	/**
	 * Given the original scene, crops to game board, which is saved for further 
	 * processing. Also creates and saves a gray scale rendering of game board to 
	 * support various processing.
	 * 
	 * @param inputImage a byte array assumed to represent a 2592x1944 pixel image of the platform
	 */
	private void setImage(byte[] inputImage) {
    	
		// create OpenCV image for entire scene
    	if (DEBUG) System.out.println("image size: " + inputImage.length);
		MatOfByte mob = new MatOfByte(inputImage);
		scene = Highgui.imdecode(mob, Highgui.IMREAD_COLOR);
		if (DEBUG) {
			Highgui.imwrite("scene.png", scene);	
			System.out.println("image size: " + scene.size());			
		}
		
		// crop to create an image that is just the game board
		game = new Mat(scene, gameROI);
		if (DEBUG) {
			System.out.println("game-" + game);	
			Highgui.imwrite("game.png", game);
		}
		
		// create gray scale of original game board
		grayGame = new Mat();
		Imgproc.cvtColor(game, grayGame, Imgproc.COLOR_BGR2GRAY);

		// blur the gray scale
		Imgproc.blur(grayGame, grayGame, new Size(4,4) );
		
		if (DEBUG) {
			// output image
			Highgui.imwrite("blurred.png", grayGame);
			// make copy of source for debugging 
			draw = new Mat();		
			game.copyTo(draw);
		}
	}
	
	
	/**
	 * Finds cell corners to facilitate creation of individual cell images
	 * 
	 * Uses the gray scale image created by setImage.
	 */
	private void findCorners() throws ImagingOpException {
				
		// set radius for drawing circles (debug)
		final int radius = 4;
		
		// locations of 4 outside corners ROI
		final int TY = 10; // top y
		final int LX = 10; // left x
		final int RX = 585; // right x
		final int BY = 565; // bottom y
		
		final int WX = 40; // size ROI
		final int WY = 40;

		// go find all the corners
		// define region of interest TL
		int rx = LX;
		int ry = TY;
		int rw = WX;
		int rh = WY;

		try {
		
			Point pTL = findCorner(rx, ry, rw, rh);
			if (DEBUG) {
				System.out.println("TL: " + pTL);						
				Core.circle(draw, pTL, radius, new Scalar(0, 0, 255), -1, 8, 0);
			}
	
			// define region of interest TR
			rx = RX;
			ry = TY;
	
			Point pTR = findCorner(rx, ry, rw, rh);
			if (DEBUG) {
				System.out.println("TR: " + pTR);
				Core.circle(draw, pTR, radius, new Scalar(0, 0, 255), -1, 8, 0);
			}
			
			// define region of interest BL
			rx = LX;
			ry = BY;
	
			Point pBL = findCorner(rx, ry, rw, rh);
			if (DEBUG) {
				System.out.println("BL: " + pBL);
				Core.circle(draw, pBL, radius, new Scalar(0, 0, 255), -1, 8, 0);
			}
	
			// define region of interest BR
			rx = RX;
			ry = BY;
	
			Point pBR = findCorner(rx, ry, rw, rh);
			if (DEBUG) {
				System.out.println("BR: " + pBR);
				Core.circle(draw, pBR, radius, new Scalar(0, 0, 255), -1, 8, 0);
			}
			
			// get width and height to aid in finding interior corners
			double width =  (((pTR.x - pTL.x) + (pBR.x - pBL.x)) / 2);
			double height =  (((pBL.y - pTL.y) + (pBR.y - pTR.y)) / 2);
			if (DEBUG) System.out.println("width: " + width + "  height:" + height);
	
			// set up backoff for slop
			int slop = 15; // pixels
							
			// find all the cell corners
			for (int row = 0; row < dim; row++) {
				for (int col = 0; col < dim; col++) {
					rx = ((int) (pTL.x + (width * col/3))) - slop;
					ry = ((int) (pTL.y + (height * row/3))) - slop;
					if (DEBUG) { 
						System.out.println("ROI: rx = " + rx + " ry = " + ry + " w = " + rw + " h = " + rh);
					}
					cellCorner[row][col] = findCorner(rx, ry, rw, rh);
					if (DEBUG) { 
						System.out.println("C" + row + col + ": " + cellCorner[row][col]);
						Core.circle(draw, cellCorner[row][col], 4, new Scalar(255, 0, 0), -1, 8, 0);
					}
				}
			}
			
			// output the debug image
			if (DEBUG) {
				Highgui.imwrite("final.png", draw);
			}
		
		} catch (Exception ex) {
			System.out.println("TicTacToeAnalyzer: findCorners() got some problem here");
			System.out.println(ex.getMessage());
			throw new ImagingOpException("EXCEPTION!!! failure to find corners");

		}
	}
	
	/**
	 * Finds a corner in a region of the game board defined by the parameters.
	 * 
	 * Uses the grey scale blurred image
	 * 
	 * @param roiX
	 * @param roiY
	 * @param roiW
	 * @param roiH
	 * @return
	 * @throws ImagingOpException
	 */
	private Point findCorner(int roiX, int roiY, int roiW, int roiH) 
		throws ImagingOpException {
		
		try {		
			// now look at subset of image
			Mat subset = new Mat(grayGame, new Rect(roiX, roiY, roiW, roiH));
	
			if (DEBUG) Highgui.imwrite("graysub.png", subset);
	
			// create mat for results
			MatOfPoint corners = new MatOfPoint();
	
			// set parameters for search
			double qualityLevel = 0.01;
			double minDistance = 10;
			int blockSize = 13;
			boolean useHarrisDetector = true;
			double k = 0.04;
	
			// Apply corner detection
			Imgproc.goodFeaturesToTrack(
					subset, corners, 1, qualityLevel, minDistance, new Mat(),
					blockSize, useHarrisDetector, k);
	
			// check number of corners detected
			Point[] cns = corners.toArray();
			
			if (DEBUG) System.out.println("corners detected: " + cns.length);
			
			// make sure found only a single corner
			if (cns.length != 1.0) {			
				for (Point pt : cns) {							
					Point dot = new Point(pt.x + (double) roiX, pt.y + (double) roiY);
					System.out.println("corner: " + dot);		
					Core.circle(grayGame, dot, 2, new Scalar(0, 255, 255), 1, 8, 0);				
				}
				Highgui.imwrite("blurred.png", grayGame);
				
	
				throw new ImagingOpException("TicTacToeAnalyzer: findCorner() EXCEPTION!!! Multiple corners detected in " + game + "\n" +
						" with ROI: " + subset);
			}
			
			// create a point from the resulting corner information
			Point dot = new Point(cns[0].x + (double) roiX, cns[0].y + (double) roiY);
			return dot;

		} catch (CvException ex) {
			System.out.println("TicTacToeAnalyzer: findCorner(): Low level Cv exception.");
			throw new ImagingOpException("EXCEPTION!!! Low level OpenCV exception in findCorner()");
		}
	}


	/**
	 * Uses the corners detected to break the board into individual cell images.
	 * 
	 * It parses the game board image to produce 9 cells.
	 * 
	 * @return a 3x3 array of cell images (color)
	 */
	private Mat[][] parseGame() throws ImagingOpException {
		
		try {		
			// pad around to make sure just "white" around circle
			int pad = 5;
			
			// loop thru using the appropriate cell corners to define where to crop a cell
			// first look at an ROI approach to avoid copying
			int dim = 3;
			for (int row = 0; row < dim; row++) {
				for (int col = 0; col < dim; col++) {
					
					// get upper left and lower right
					Point ul = cellCorner[row][col];
					Point lr = cellCorner[row+1][col+1];
					
//					System.out.println(ul);
//					System.out.println(lr);
					
					// calculate a ROI using Range; account for padding
					Range colR = new Range(((int) ul.x) + pad, ((int) lr.x) - pad);
					Range rowR = new Range(((int) ul.y) + pad, ((int) lr.y) - pad);
					
//					System.out.println(colR);
//					System.out.println(rowR);
					
					cell[row][col] = game.submat(rowR, colR);
					
					// output the debug image
					if (DEBUG) Highgui.imwrite("cell" + row + col + ".png", cell[row][col]);

				}
			}		
		} catch (CvException ex) {
			System.out.println("TicTacToeAnalyzer: parseGame(): Low level Cv exception.");
			throw new ImagingOpException("EXCEPTION!!! Low level OpenCV exception in parseGame()");
		}

		
		return cell;
	}
	

	/** 
	 * Does initial processing of an image to produce the 3x3 cell image array. 
	 * 
	 * @param inputImage
	 */
	public void initImage(byte[] inputImage) throws ImagingOpException {
		
		// load the image and do initial processing
		setImage(inputImage);
		
		try {
			// find the corners that delineate the cells
			findCorners();

			// parse the game board to produce all the cells
			parseGame();

		} catch (Exception ex) {
			System.out.println(ex.getMessage());
			throw new ImagingOpException("EXCEPTION!!! " + ex.getMessage());
		}
						
	}
	
	/**
	 * Does complete analysis of an image.
	 * 
	 * @param inputImage
	 * @return true if image process successfully, false otherwise
	 */
	public boolean analyzeImage(byte[] inputImage) throws ImagingOpException { 
		
		boolean OK = false;

		try {
			// do initial processing
			initImage(inputImage);

			// process cells
			OK = findBoardState();
			
			// set the public status 
			this.boardOK = OK;

		} catch (Exception ex) {
			System.out.println("bad stuff going on");
			throw new ImagingOpException("EXCEPTION!!! " + ex.getMessage());
		}
				
		return OK;
		
	}
	
	/**
	 * Returns the goodness of the analysis
	 * 
	 * @return analysis OK or not
	 */
	public boolean getAnalysisResult() {
		return this.boardOK;
	}
	
	/**
	 * Determines the content of a single cell. Could be EMPTY, CROSS, NAUGHT.
	 * If there is an error condition, will get NO_REG, mean no registration circle found;
	 * this indicates a severe image processing problem, or perhaps a misplaced token.
	 * 
	 * @param row of the cell to examine
	 * @param col of the cell to examine
	 * @return cell type EMPTY, CROSS, NAUGHT, NO_REG (error)
	 */
	private int getCellContent(int row, int col) {
		if (DEBUG) System.out.println("Cell (" + row + "," + col + ")");
		// find cell type using the cell type detector
		int type = detector.findCellType(cell[row][col]);
		if (DEBUG) {
			System.out.println("Cell type: " + type);
			
			switch (type) {
			case CellTypeDetectorCircle.NO_REG:
				System.out.println("NO Registration!");
				break;
			case TicTacToeGameBoard.EMPTY:
				System.out.println("Empty cell");
				break;
			case TicTacToeGameBoard.NAUGHT:
				System.out.println("O cell");
				break;
			case TicTacToeGameBoard.CROSS:
				System.out.println("X cell");
				break;
			}
		}
		
		return type;
	}
	
	/**
	 * Finds the content of all cells in the game board.
	 * 
	 * @return indicates if board successfully processed (all cells determined)
	 */
	public boolean findBoardState() {
		
		boolean cellStatus = true;
		
		// find type of all cells
		int type = CellTypeDetectorCircle.NO_REG;
		for (int row=0; row<3; row++) {
			for (int col=0; col<3; col++) {
				// get type of cell
				type = getCellContent(row, col);
				
				// check status
				if (type == CellTypeDetectorCircle.NO_REG) { // error!
					System.out.println("\n\nREGISTRATION ERROR! Cell (" + row + "," + col + ") ");
					cellStatus = false;
				} else { // good stuff 
					// fill in board status
					gameStatus.setCell(row, col, type);
				}
			}
		}
		
		return cellStatus;
	}
	
	/** 
	 * Returns the analysis of the physical game board based on the image
	 * 
	 * @return the physical board state
	 */
	public TicTacToeGameBoard getBoardState() {
		return gameStatus;
	}
	
	/**
	 * Saves the scene (camera image) in scene.png
	 */
	public void saveScene() {
		Highgui.imwrite("scene.png", scene);			
	}
	
}
