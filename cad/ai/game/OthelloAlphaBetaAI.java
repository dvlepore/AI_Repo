/*******************
* Christian A. Duncan
* Modified by: David Lepore, Alex Hutman, Steve
* CSC350: Intelligent Systems
* Spring 2019
*
* AI Game Client
* This project is designed to link to a basic Game Server to test
* AI-based solutions.
*
* OthelloAI:
*    This class is the main AI system for the Othello game.
*
* See README file for more details.
********************/
package cad.ai.game;

import java.util.*;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.*;

/***********************************************************
* The AI system for a OthelloGame.
*   Most of the game control is handled by the Server but
*   the move selection is made here - either via user or an attached
*   AI system.
***********************************************************/
public class OthelloAlphaBetaAI extends AbstractAI {
  public OthelloGame game;  // The game that this AI system is playing
  protected Random ran;
  public OthelloGame practiceGame;
  protected int maxDepth = 4;
  protected String fileName; 
  private Pattern r;
  private Matcher m;
  protected int[][] weightedScores = {
  	{100 , -10 , 8  ,  6 ,  6 , 8  , -10 ,  100},
  	{-10 , -25 ,  -4, -4 , -4 , -4 , -25 , -10 },
  	{8   ,  -4 ,   6,   4,   4,   6,  -4 ,  8  },
  	{6   ,  -4 ,   4,   0,   0,   4,  -4 ,  6  },
  	{6   ,  -4 ,   4,   0,   0,   4,  -4 ,  6  },
  	{8   ,  -4 ,   6,   4,   4,   6,  -4 ,  8  },
  	{-10 , -25 ,  -4, -4 , -4 , -4 , -25 , -10 },
  	{100 , -10 , 8  ,  6 ,  6 , 8  , -10 ,  100}
  };
  protected Map <String, Record> hmap;
  protected Stack <String> setOfMoves;
  protected int maxTurn = 5; 
  int cornerHeuristic = 0;
  int moblityHeuristic = 0;
  int stabiltyHeuristic = 0;
  int coinParityHeuristic = 0;
  private String pattern = "([0-1])#([0-7][a-h])#([0-9]+)-([0-9]+)-([0-9]+)#([0-9]+.[0-9]+(E-?[0-9]+)?)";
  protected BufferedReader reader;
  private  int[][] directions = new int[][]{{-1,-1}, {-1,0}, {-1,1},  {0,1}, {1,1},  {1,0},  {1,-1},  {0, -1}};
  private int turnNumber = 0;


  public OthelloAlphaBetaAI()
  {
  	this("C:/Ai_Repo/SimpleAI/cad/ai/game/firstMovesOthello.txt");
  }


  public OthelloAlphaBetaAI(String fileName) 
  {
  	this.fileName = fileName;
  	game = null;
  	ran = new Random();
  	practiceGame = new OthelloGame(-1, null, null, false, 0);
  	hmap = new HashMap<String, Record>();
  	setOfMoves = new Stack<>();
  	this.r = Pattern.compile(this.pattern);
  	int player;
  	String action;
  	String boardState;
  	int wins, losses, ties;
  	double score;
  	String line;

  	//Parse the records from the trained record file and put them on the hashmap
  	try {
  		reader = new BufferedReader(new FileReader(this.fileName));
  		while ((line = reader.readLine()) != null) {
  			this.m = this.r.matcher(line);
  			if (m.find()) {
  				player = Integer.parseInt(m.group(1));
  				action = m.group(2);
  				wins = Integer.parseInt(m.group(3));
  				losses = Integer.parseInt(m.group(4));
  				ties = Integer.parseInt(m.group(5));
  				String strscore = m.group(6);
  				score = Double.valueOf(strscore);
  				Record r = new Record(wins,losses,ties,score);
  				hmap.put(player+"#"+action,r);
  				System.out.println(player+""+action+""+wins+""+losses+""+ties+""+strscore);
  			}
  			else {
  				System.out.println("oh dear.");
  			}
  		}
  		reader.close();   
  	}
  	catch (Exception jeff) {
  		System.out.println("ERROR:" + jeff);
  	}

  }

  public synchronized void attachGame(Game g) {
  	game = (OthelloGame) g;
  	System.out.println("Alpha beta ai created as player " + game.getPlayer());
  }

    /**
    * Returns the Move as a String "rc" (e.g. 2b)
    **/
    public synchronized String computeMove() {
    	if (game == null)
    	{
    		System.err.println("CODE ERROR: AI is not attached to a game.");
    		return "0a";
    	}
    	char[][] board = (char[][]) game.getStateAsObject();
    	OthelloGame.Action bestAction = null;
    	int bestScore = Integer.MIN_VALUE;
      // First get the list of possible moves
      int player = game.getPlayer(); // Which player are we?
      char piece;
      int score;
      evalBoard(player, board);
      ArrayList<OthelloGame.Action> actions = game.getActions(player);
      for(OthelloGame.Action a : actions)
      {
      	char [][] copyBoard = result(board, a, game.getPlayer());
      	switch(copyBoard.length)
      	{
      		case 4:
      		score = minValue(copyBoard, Integer.MIN_VALUE, Integer.MAX_VALUE, this.maxDepth*2);
      		if (score > bestScore)
      		{
      			bestAction = a;
      			bestScore = score;
      		}
      		case 6:
      		score = minValue(copyBoard, Integer.MIN_VALUE, Integer.MAX_VALUE, this.maxDepth*3);
      		if (score > bestScore)
      		{
      			bestAction = a;
      			bestScore = score;
      		}
      		case 8:
      		score = minValue(copyBoard, Integer.MIN_VALUE, Integer.MAX_VALUE, 7);
      		if (score > bestScore)
      		{
      			bestAction = a;
      			bestScore = score;
      		}
      	}
      }
      
      if(this.maxTurn >= 0)
      {
      	String straction = bestAction.toString();
      	Record record = hmap.get(straction);
      	if(r == null)
      	{
      		record  = new Record(0,false);
      		hmap.put(game.getPlayer()+"#"+straction, record);
      	}
      	setOfMoves.push(new String (straction));
      }
      this.maxTurn -=1;
      this.turnNumber++;
      return bestAction.toString();
  }


  public char [][] result (char [][] board, OthelloGame.Action action, int player) {
  	practiceGame.updateState(player, board);
  	practiceGame.processMove(player, action.row, action.col);
  	return (char[][]) practiceGame.getStateAsObject();
  }

    /**
    * Away wishes to MINimize the score.
    * @param: The board state to determine minimum move
    **/
    private int minValue(char[][] board, int alpha, int beta, int depth)
    {
    	int curAlpha = alpha;
    	int curBeta = beta;

    	int turn = 1 - game.getPlayer();

    	if (depth <= 0) 
    	{
    		int utilValue = evalBoard(turn, board);
    		return utilValue;

    	}

      // Is this a terminal board
    	practiceGame.updateState(turn,board);
    	if (practiceGame.computeWinner())
    	{
    		int w = practiceGame.getWinner();
    		return game.getPlayer() == 0 ? practiceGame.getHomeScore() - practiceGame.getAwayScore() :
    		practiceGame.getAwayScore() - practiceGame.getHomeScore();
    	}
    	ArrayList<OthelloGame.Action> actions = practiceGame.getActions(turn);
    	if (actions == null || actions.size() == 0) {
        // No moves
    		return maxValue(board, curAlpha, curBeta, depth-1);
    	}
      // Determine Maximum value among all possible actions
      int bestScore = Integer.MAX_VALUE; // Positive "Infinity"
      for (OthelloGame.Action a : actions)
      {
      	char [][] copyBoard = result(board, a, turn);
      	bestScore = Math.min(bestScore, maxValue(copyBoard, curAlpha, curBeta, depth-1));

      	if (bestScore <= curAlpha) {
      		return bestScore;
      	}

      	curBeta = Math.min(curBeta, bestScore);
      }
      return bestScore;
  }
    /**
    * Home wishes to MAXimize the score.
    * @param: The board state to determine maximum move
    **/
    private int maxValue(char[] [] board, int alpha, int beta, int depth) {
    	int curAlpha = alpha;
    	int curBeta = beta;

      //System.out.println("Depth (maxValue) = " + depth);

    	int turn = game.getPlayer();

    	if (depth <= 0) 
    	{
    		int utilValue = evalBoard(turn, board);
    		return utilValue;

    	}
      // Is this a terminal board
    	practiceGame.updateState(turn, board);
    	if (practiceGame.computeWinner())
    	{
    		int w = practiceGame.getWinner();
    		return game.getPlayer() == 0 ? practiceGame.getHomeScore() - practiceGame.getAwayScore() :
    		practiceGame.getAwayScore() - practiceGame.getHomeScore();
    	}
    	ArrayList<OthelloGame.Action> actions = practiceGame.getActions(turn);
    	if (actions == null || actions.size() == 0) {
        // No moves
    		return minValue(board, curAlpha, curBeta, depth-1);
    	}
      // Determine Maximum value among all possible actions
      int bestScore = Integer.MIN_VALUE; // Negative "Infinity"
      for (OthelloGame.Action a: actions)
      {
      	char [][] copyBoard = result(board, a, turn);
      	bestScore = Math.max(bestScore, minValue(copyBoard, curAlpha, curBeta, depth-1));

      	if (bestScore >= curBeta) {
      		return bestScore;
      	}

      	curAlpha = Math.max(curAlpha, bestScore);
      }
      return bestScore;
  }

  public int evalBoard(int player, char[][] board)
  {
  	this.coinParityHeuristic = calcuateCoinParity(player,board);
  	this.cornerHeuristic = calcuateCornerHeuristic(player, board);
  	this.stabiltyHeuristic = calcualteStabilityHeuristic(player,board);
  	return this.coinParityHeuristic+this.cornerHeuristic+this.stabiltyHeuristic;
  }
  private int calcualteStabilityHeuristic(int player, char [] [] board)
  {   
  	boolean playerIsValid =  false;
  	boolean oppIsValid = false;
  	int heurValue; 
  	int opp;
  	char playerChar;
  	char oppChar;
  	if(player == 0)
  	{
  		opp = 1;
  		playerChar = 'X';
  		oppChar = 'O';
  	}
  	else
  	{
  		opp = 0;
  		playerChar = 'O';
  		oppChar = 'X';
  	}
  	int playerHeur = 0 , oppHeur = 0;
  	for( int x = 0; x< board.length; x++)
  	{
  		for(int y = 0; y < board[0].length; y++)
  		{

  			playerIsValid = ourBoardValidMove(board,playerChar, x, y);
  			oppIsValid = ourBoardValidMove(board, oppChar, x, y);
  			if(playerIsValid == true && ((x == 0 || y == 1) || (x == 1 || y == 0) || (x == 1 || y == 1) || 
  				(x == 0 || y == 6) || (x == 1 || y == 7) || (x == 1 || y == 6) ||
  				(x == 6 || y == 0) || (x == 7 || y == 1) || (x == 6 || y == 1) ||
  				(x == 6 || y == 7) || (x == 7 || y == 6) || (x == 6 || y == 6) ))
  			{
  				playerHeur -=1;
  			}
  			else if(oppIsValid == true && ((x == 0 || y == 1) || (x == 1 || y == 0) || (x == 1 || y == 1) || 
  				(x == 0 || y == 6) || (x == 1 || y == 7) || (x == 1 || y == 6) ||
  				(x == 6 || y == 0) || (x == 7 || y == 1) || (x == 6 || y == 1) ||
  				(x == 6 || y == 7) || (x == 7 || y == 6) || (x == 6 || y == 6) ))
  			{
  				oppHeur -=1;
  			}
  			else
  			{
  				if(playerIsValid)
  					playerHeur += 1;
  				if(oppIsValid)
  					oppHeur += 1;
  			}
  		}
  	}
  	if(playerHeur + oppHeur != 0)
  	{
  		int myname = Math.abs(playerHeur);
  		int jeff = Math.abs(oppHeur);
  		heurValue = 25 * (playerHeur - oppHeur)/(myname+jeff);       
  	}
  	else
  	{
  		heurValue = 0;
  	}

  	return heurValue; 
  }
  private int calcuateCoinParity(int player, char [] [] board)
  { 
  	int [] pieces = countPieces(board);
  	String str  = Arrays.toString(pieces);
  	return player == 0 ?  25 * (pieces[0]- pieces[1])/(pieces[0]+ pieces[1]) : 
  	25 * (pieces[1] -pieces[0])/(pieces[1]+ pieces[0]);
  }
  private int calcuateCornerHeuristic(int player, char [][] board)
  {
  	int endHeur = 0;
  	int cornerHeurvalue =0;
  	int oppcornerHeurValue =0;
  	int [] heurValues = countCornerPieces(player, board);
  	if(player == 0)
  	{
  		cornerHeurvalue = heurValues[0];
  		oppcornerHeurValue= heurValues[1];
  	}
  	else
  	{
  		cornerHeurvalue = heurValues[1];
  		oppcornerHeurValue= heurValues[0];
  	}
  	if(cornerHeurvalue + oppcornerHeurValue != 0)
  	{
  		endHeur = 30 * (cornerHeurvalue- oppcornerHeurValue)/(cornerHeurvalue+oppcornerHeurValue);
  	}
  	else
  	{
  		endHeur = 0;
  	}
  	return endHeur;
  }
  private int [] countCornerPieces(int player, char[][] board) 
  {
  	int boardLength = board[0].length-1;
  	int[] lol = new int[6];
  	lol[0]=0; lol [1]=0; lol[2]=0; lol [3]=0; lol[4]=0; lol[5] =0;
  	int[] x = {0,0,boardLength,boardLength};
  	int[] y = {0,boardLength,0,boardLength};
  	int [] corners = new int[4];
  	char [] unlikelyCorners =  {
  		board[0][1], 
  		board[1][0], 
  		board[1][1], 
  		board[0][6],
  		board[1][7],
  		board[1][6],
  		board[6][0],
  		board[7][1],
  		board[6][1],
  		board[6][7],
  		board[7][6],
  		board[6][6]
  	};
  	char [] potentialCorners= {
  		board[0][2], 
  		board[2][0], 
  		board[2][2], 
  		board[0][5],
  		board[2][7],
  		board[2][5],
  		board[5][0],
  		board[7][2],
  		board[5][2],
  		board[7][5],
  		board[5][7],
  		board[5][5]
  	};
      // lol[0] = number of perm corners home has
      // lol[1] = the number of perm corners away has
      // lol[2] = number of unlikely/ pieces that home has
      // lol[3] = number of unlikely pieces away has
      //lol[4] = number of potential pieces home have 
      // lol[5] =  number of potential pieces away  has

  	for (int i=0; i<4; i++)
  	{
  		if(board[x[i]][y[i]] == 'X')
  		{
  			lol[0] += 4;
  			corners[i] = 1;
  		}
  		if(board[x[i]][y[i]] == 'O') 
  		{
  			lol[1] += 4;
  			corners[i] = 2;
  		}
  		if(board[x[i]][y[i]] == ' ')
  		{
  			corners[i] = 3;
  		}
  	}

  	int index = 0; 
  	for(int m = 0; m < 12; m++)
  	{
  		if(unlikelyCorners[m] == 'X' && (corners[index] == 2|| corners[index]== 3 ))
  		{
  			if(m % 3 == 2)
  			{
  				lol[3] -= 4;
  			}
  			else
  				lol[3] -=3;
  		}
  		if(unlikelyCorners[m] == 'O'&& (corners[index] == 1 || corners[index]== 3 ))
  		{
  			if(m % 3 == 2)
  			{
  				lol[3] -= 4;
  			}
  			else
  				lol[3] -=3;
  		}
  		if(potentialCorners[m] == 'X')
  		{
  			if(m % 3 == 2)
  			{
  				lol[3] += 1;
  			}
  			else
  				lol[3] +=2;
  		}
  		if(potentialCorners[m] == 'O')
  		{
  			if(m % 3 == 2)
  			{
  				lol[3] += 1;
  			}
  			else
  				lol[3] +=2; 
  		}
  		if(m + 2 % 4 == 0)
  		{
  			index++;
  		}
  	}
  	int homeCornerHeur = lol[0] + lol[2] + lol[4];
  	int awayCornerHeur = lol[1] + lol[3] + lol[5];
  	return new int [] {homeCornerHeur, awayCornerHeur};
  }
    /**
    * Inform AI who the winner is
    *   result is either (H)ome win, (A)way win, (T)ie
    **/
    @Override
    public synchronized void postWinner(char result) {
      // This AI probably wants to store what it has learned
      // about this particular game.
    	String [] setOfMovesArr = setOfMoves.toArray(new String [0]);
    	setOfMoves.clear();
    	int player = game.getPlayer();

    // Determine if we won (2), lost (0), or tied (1)
    	int res = 0;
    if (result == 'T') res = 1;  // We ties
    else if (result == 'H') {
      if (player == 0) res = 2; // We won
      else res = 0; // We lost
    } else { // result == 'A'
      if (player == 1) res = 2; // We won
      else res = 0; // We lost
  }

  for(int i = 0; i < setOfMovesArr.length; i++)
  {
  	if(hmap.containsKey(setOfMovesArr[i]))
  	{
  		hmap.get(setOfMovesArr[i]).updateRecord(res);
  	}
  	else
  	{
  		Record newRecord = new Record(player, res);
  		hmap.put(game.getPlayer()+"#"+setOfMovesArr[i], newRecord); 

  	}
  } 
      game = null;  // No longer playing a game though.
  }

    /**
    * Shutdown the AI - allowing it to save its learned experience
    **/
    @Override
    public synchronized void end() 
    {
      // This AI probably wants to store (in a file) what
      // it has learned from playing all the games so far...
    	try {
    		FileWriter fw = new FileWriter(this.fileName, false);
    		BufferedWriter bw = new BufferedWriter(fw);
    		PrintWriter out = new PrintWriter(bw);
    		Object [] keys = this.hmap.keySet().toArray();
    		for (int i = 0; i < keys.length; i++) 
    		{

    			char separator = '#';
    			Record x = hmap.get(keys[i].toString());
    			out.print(keys[i].toString());
    			out.print(separator);
    			out.print(x.ReturnRecord());
    			out.print(separator);
    			out.print(x.ReturnScore());
    			out.println();          
    		}
    		out.flush();  
    		out.close(); 
    	} 
    	catch (Exception meme) 
    	{
    		System.out.println("ERROR: " + meme);
    	}
    }
    private int[] countPieces(char [][] board) {
    	int numX = 0;
    	int numO = 0;
    	int numBlank = 0;

    	for (char[] je : board) {
    		for (char ff : je) 
    		{
    			if(ff == 'X')
    			{
    				numX++;
    			}
    			if(ff == 'O')
    			{
    				numO++;
    			}
    			if(ff == ' ')
    			{
    				numBlank++;
    			}
    		}
    	}

    	return new int[] {numX, numO, numBlank};
    }

    // The methods below were copied from OthelloGame in order to use with a board, 
    // not an instance of an OthelloGame
    public boolean ourBoardValidMove(char [] [] board, char symbol, int row, int col) {
    	return
            board[row][col] == ' ' &&       // Space is open
            (ourflipDirection(board ,symbol, row, col, -1,  0, false) ||   // NORTH
             ourflipDirection(board ,symbol, row, col, +1,  0, false) ||   // SOUTH
             ourflipDirection(board ,symbol, row, col,  0, -1, false) ||   // WEST
             ourflipDirection(board ,symbol, row, col,  0, +1, false) ||   // EAST
             ourflipDirection(board ,symbol, row, col, -1, -1, false) ||   // NW
             ourflipDirection(board ,symbol, row, col, -1, +1, false) ||   // NE
             ourflipDirection(board ,symbol, row, col, +1, -1, false) ||   // SW
             ourflipDirection(board ,symbol, row, col, +1, +1, false));
        }
        private boolean ourflipDirection(char [] [] board, char symbol, int row, int col, int dr, int dc, boolean flip) {
        	int r, c, count;
        	boolean flipped = false;
        	for (r = row+dr, c = col+dc, count = 0; r >= 0 && r < board.length && c >= 0 && c < board.length && board[r][c] != ' ';
        		r += dr, c += dc, count++) {
        		if (board[r][c] == symbol) {
                // Found one
        			if (count > 0 && flip) 
                    // Flip the ones over between the two (if any!)
        				for (r -= dr, c -= dc; r != row || c != col; r -= dr, c -= dc) {
        					board[r][c] = symbol;
        				}
        				return count > 0;
        			}
        		}
        		return false;
        	}
        }