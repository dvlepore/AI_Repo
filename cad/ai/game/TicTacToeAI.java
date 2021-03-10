/*******************
 * Christian A. Duncan
 * CSC350: Intelligent Systems
 * Spring 2019
 *
 * AI Game Client
 * This project is designed to link to a basic Game Server to test
 * AI-based solutions.
 * See README file for more details.
 ********************/
package cad.ai.game;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.*;

/***********************************************************
 * The AI system for a TicTacToeGame.
 *   Most of the game control is handled by the Server but
 *   the move selection is made here - either via user or an attached
 *   AI system.
 ***********************************************************/
public class TicTacToeAI extends AbstractAI {
    public TicTacToeGame game;  // The game that this AI system is playing
    protected Random ran;
    protected Map<String, Record> hmap;
    protected Stack<String> setOfMoves ;
    protected BufferedReader reader;
    protected String fileName;
    private String pattern = "#([0-9]+)-([0-9]+)-([0-9]+)#([0-9]+.[0-9]+(E-?[0-9]+)?)";
    private Pattern r;
    private Matcher m;
    private int numGames = 0;
	private static final double DEFAULT_BALANCER = 0.0;
    private double balancer = 100.0;
	public TicTacToeAI()
	{
		this("C:/Ai_Repo/SimpleAI/cad/ai/game/TTT-MasterRaceAI.txt");
	}
    public TicTacToeAI(String fileName) 
    {
    	this.fileName = fileName;
    	game = null;
    	ran = new Random();
    	hmap = new HashMap<String, Record>();
    	setOfMoves =  new Stack<>();
    	this.r = Pattern.compile(this.pattern);
    	String line;
    	String boardState;
    	int wins, losses, ties;
    	double score;
    	try {
    		reader = new BufferedReader(new FileReader(this.fileName));
    		while ((line = reader.readLine()) != null) {
    			this.m = this.r.matcher(line);
    			if (m.find()) {
    				boardState = line.substring(0,9);
    				wins = Integer.parseInt(m.group(1));
    				losses = Integer.parseInt(m.group(2));
    				ties = Integer.parseInt(m.group(3));
    				String strscore = m.group(4);
    				score = Double.valueOf(strscore);
    				Record r = new Record(wins,losses,ties,score);
    				hmap.put(boardState,r);
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

    public synchronized void attachGame(Game g) 
	{
    	game = (TicTacToeGame) g;
		
    }
    
    /**
     * Returns the Move as a String "S"
     *    S=Slot chosen (0-8)
     **/
    public synchronized String computeMove() 
    {
    	if (game == null) {
    		System.err.println("CODE ERROR: AI is not attached to a game.");
    		return "0";
    	}
    	char[] board = (char[]) game.getStateAsObject();
		char player;
		if(game.getPlayer() == 0)
		{
			player = 'X';
		}
		else
		{
			player = 'O';
		}
    	int openSlots = 0;
    	int i = 0;
    	for (i = 0; i < board.length; i++) 
		{
    		if (board[i] == ' ')
    		{				
    			openSlots++;
    		}
    	}
    	balancer = DEFAULT_BALANCER * Math.pow(0.95, 9-openSlots);
		double score = 0;
		double maxScore = -1;
		String str = "";
    	for(int x = 0; x < board.length; x++)
		{
			double randomNum = Math.random()*balancer;
			if(board[x] == ' ')
			{	
				String copyBoard = new String (board);
				StringBuilder strbld = new StringBuilder(copyBoard);
				strbld.setCharAt(x,player);
				str = strbld.toString();
				Record record = hmap.get(str);
				if(record == null)
				{
					record  = new Record(0,false);
					hmap.put(str, record);
				}
					score = record.getScore()+ randomNum;
					if(score > maxScore)
					{
						maxScore = score;
						i = x;
					} 
			}
		}
		if(player == 'X')
		{
			board[i] = 'X';
		}
		else
		{
			board[i] = 'O';
		}
		setOfMoves.push(new String (board));
		return "" + i;
	}

    /**
     * Inform AI who the winner is
     *   result is either (H)ome win, (A)way win, (T)ie
     **/
    @Override
    public synchronized void postWinner(char result) 
    {
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
    			hmap.put(setOfMovesArr[i], newRecord); 
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
    		Object [] keys = hmap.keySet().toArray();
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
}

