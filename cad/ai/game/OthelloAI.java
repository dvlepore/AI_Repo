/*******************
 * Christian A. Duncan
 * Modified by: David Lepore Alex Hutman Steve
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

import java.util.Random;
import java.util.ArrayList;

/***********************************************************
 * The AI system for a OthelloGame.
 *   Most of the game control is handled by the Server but
 *   the move selection is made here - either via user or an attached
 *   AI system.
 ***********************************************************/
public class OthelloAI extends AbstractAI {
    public OthelloGame game;  // The game that this AI system is playing
    protected Random ran;
    public OthelloGame practiceGame;
    public OthelloAI() {
        game = null;
        ran = new Random();
        practiceGame = new OthelloGame(-1, null, null, false, 0);
    }
    public synchronized void attachGame(Game g) {
        game = (OthelloGame) g;
        System.out.println("Minimax ai created as player " + game.getPlayer());
    }

    /**
     * Returns the Move as a String "rc" (e.g. 2b)
     **/
    public synchronized String computeMove()
    {
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
        ArrayList<OthelloGame.Action> actions = game.getActions(player);
        for(OthelloGame.Action a : actions)
        {
            char [][] copyBoard = result(board, a, game.getPlayer());
            int score = minValue(copyBoard);
            if (score > bestScore)
            {
                bestAction = a;
                bestScore = score;
            }
        }


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
    private int minValue(char[][] board)
    {
        int turn = 1 - game.getPlayer();
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
            return maxValue(board);
        }
        // Determine Maximum value among all possible actions
        int bestScore = Integer.MAX_VALUE; // Positive "Infinity"
        for (OthelloGame.Action a : actions)
        {
            char [][] copyBoard = result(board, a, turn);
            bestScore = Math.min(bestScore, maxValue(copyBoard));
        }
        return bestScore;
    }
     /**
     * Home wishes to MAXimize the score.
     * @param: The board state to determine maximum move
     **/
    private int maxValue(char[] [] board) {
        int turn = game.getPlayer();
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
            return minValue(board);
        }
        // Determine Maximum value among all possible actions
        int bestScore = Integer.MIN_VALUE; // Negative "Infinity"
        for (OthelloGame.Action a: actions)
        {
            char [][] copyBoard = result(board, a, turn);
            bestScore = Math.max(bestScore, minValue(copyBoard));
        }
        return bestScore;
    }

    /**
     * Inform AI who the winner is
     *   result is either (H)ome win, (A)way win, (T)ie
     **/
    @Override
    public synchronized void postWinner(char result) {
        // This AI probably wants to store what it has learned
        // about this particular game.
        game = null;  // No longer playing a game though.
    }

    /**
     * Shutdown the AI - allowing it to save its learned experience
     **/
    @Override
    public synchronized void end() {
        // This AI probably wants to store (in a file) what
        // it has learned from playing all the games so far...
    }
}
