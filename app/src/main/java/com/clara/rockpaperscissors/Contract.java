package com.clara.rockpaperscissors;

import com.clara.rockpaperscissors.Model.Player;

/**
 * Created by admin on 11/5/16.
 */

public class Contract {

	interface ViewInterface {

		void newGameDisplayPlayChoices();
		void displayPlayChoicesMade(String playerChoice, String opponentChoice);
		void notifyOpponentHasPlayed();
		void displayResults(Result result);
		void opponentConnectionLost();


	}

	interface GameInterface {


		Player createPlayer();

		void playerReady();
		void userPlayed(String play);
		void userReset();


		void playerActivityPauses();
		void playerActivityResumes();

		//void opponentFound(Player opponent);
		void noOpponentAvailable(boolean error);
		void opponentPlayed(Player opponent);
		void opponentReset();

		void opponentConnectionLost();



	}

	//For connecting to Opponent

	interface ModelInterface {

		void findOpponent(String playerKey, Firebase.OpponentDatabaseCallback callback);
		void awaitOpponentPlay(Player player, GameInterface callback);
		void awaitOpponentReset(Player opponent, GameInterface callback);
		void listenForDiscovery(Player player, Firebase.OpponentDatabaseCallback callback);  //tooo hacky sort out

		void savePlayerToDB(Player player);
	}


}
