package com.clara.rockpaperscissors;

import android.util.Log;

import com.clara.rockpaperscissors.Model.Game;
import com.clara.rockpaperscissors.Model.Player;

/**
 * Created by clara on 11/5/16. The game logic
 */


enum Result {
	WON, LOST, DRAW;
}

//enum Play {
//	ROCK("rock"), PAPER("paper"), SCISSORS("scissors");
//	private final String play;
//	Play(String string) { this.play = string; }
//}   //wretched Java String enums :)

interface Play {
	final String ROCK = "rock";
	final String PAPER = "paper";
	final String SCISSORS = "scissors";
}


class RockPaperScissors implements Contract.GameInterface, Firebase.OpponentDatabaseCallback {


	private final static String TAG = "RPS GAME LOGIC";

	private boolean isPlayer1;   // Player 1 is the player that sets up the game.

	private Firebase firebase;


	private Contract.ViewInterface gameUI;   //MainActivity


	RockPaperScissors(Contract.ViewInterface gameUI)  {

		this.gameUI = gameUI;
		firebase = new Firebase();



	}

	private Player player;
	private Player opponent;


	//Game object

	Game game;

	@Override
	public Player createPlayer() {


		player = new Player(true, null, null);
		firebase.savePlayerToDB(player);
		//should update the key in the player object

		return player;

	}


	@Override
	public void playerReady() {

		//look for opponent

		Log.d(TAG, "looking for opponent for player " + player);
		firebase.findOpponent(player.key, this);

	}


	@Override
	public void availableOpponentFound(Player op) {

		gameUI.newGameDisplayPlayChoices();
		opponent = op;

	}


	@Override
	public void noOpponentAvailable(boolean error) {

		if (error) {
			//TODO play locally against computer?

		} else {
			//todo

			firebase.listenForDiscovery(player, this);
		}

	}


	@Override
	public void userPlayed(String play) {

		//user clicks button. Update firebase, await opponent play

		player.played = play;
		firebase.savePlayerToDB(player);
		firebase.awaitOpponentPlay(opponent, this);

		//todo timeout if opponent never plays

	}


	@Override
	public void opponentConnectionLost() {

		Log.d(TAG, "opponent connection lost, search for another");

		opponent = null;
		firebase.findOpponent(player.key, this);
		gameUI.opponentConnectionLost();

		//todo delete game, score etc.
	}


	@Override
	public void userReset() {

		player.played = null;
		firebase.savePlayerToDB(player);

		//new game

		firebase.awaitOpponentReset(opponent, this);


	}




	@Override
	public void opponentPlayed(Player opponent) {

		//Determine winner, tell UI to update

		///did  this player win?
		Result result = determineWinner();

		Log.d(TAG, "opponent status: " + opponent + " and result is " + result);

		//save new opponent state
		this.opponent = opponent;
		gameUI.displayPlayChoicesMade(opponent.played);
		gameUI.displayResults(result);

	}


	@Override
	public void opponentReset() {


		//new game!

		//todo test if opponent is still available

		availableOpponentFound(opponent);

	}



	@Override
	public void playerActivityPauses() {
		//todo remove player from DB
		//activity may be closing
	}

	@Override
	public void playerActivityResumes() {
		//save player to db
	}


	private Result determineWinner() {

		Log.d(TAG, "Determine winner from player " + player.played + " opp " + opponent.played + " " + Play.ROCK);

		//did we win?
		if ( player.played.equals(Play.ROCK) && opponent.played.equals(Play.SCISSORS) ||
				player.played.equals(Play.SCISSORS) && opponent.played.equals(Play.PAPER) ||
				player.played.equals(Play.PAPER) && opponent.played.equals(Play.ROCK)) {

			return Result.WON;
		}

		//Or a draw?
		else if (player.played.equals(opponent.played)) {

			return Result.DRAW;
		}

		else {
			return Result.LOST;
		}

	}



}
