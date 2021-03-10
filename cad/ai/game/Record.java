package cad.ai.game;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;

public class Record 
{
	//Test
	String record;
	int player;
	public double score = 0;
	public static double INITIAL_SCORE = 0.5; // could be 0.8
	public static double alpha = 0.2; //Blending value 
	public int wins = 0;
	public int losses = 0;
	public int ties = 0;
	public Record(int player, boolean isAtEnd)
	{
		this.score = INITIAL_SCORE;
		
	}
	public Record (int player, int result)
	{
		this.player = player;
		this.score = INITIAL_SCORE;
			updateRecord(result);
		this.record = ReturnRecord();
	}
	public Record( int wins, int losses, int ties, double score)
	{
		this.wins = wins;
		this.losses = losses;
		this.ties = ties;
		this.score = score;
		this.record = ReturnRecord(); 
	}
	
	// result = 0 (LOSS), 1 (TIE), 2 (WIN)
	public void updateRecord(int result)
	{
		if (result == 2) {
			this.wins++;
			this.score = this.score * (1 - alpha) + 1 * alpha;
		} else if (result == 0) {
			this.losses++;
			this.score = this.score * (1 - alpha) + 0 * alpha; 
		} else if (result == 1) {
			this.ties++;
			this.score = this.score * (1 - alpha) + .5 * alpha; 
		}
		
		if (this.score > 1.0 || this.score < 0.0) System.err.println("CODING ERROR: " + this.score);
	}
	public double getScore() 
	{
		return score; 
	}
	public String ReturnRecord()
	{
		return ""+this.wins+"-"+this.losses+"-"+this.ties;
	}
	public String ReturnScore()
	{
		return ""+score+"";
	}
}