package com.clara.rockpaperscissors;

import android.graphics.Color;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.clara.rockpaperscissors.Model.Game;
import com.clara.rockpaperscissors.Model.Player;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;


/*
*
* Rock Paper Scissors between two devices, with Firebase back end
*
* This game is driven by ValueListener callbacks, and event handlers on the UI components.
*
* TODO index on available?
* display image for choice (resize, or scale relative to device display size)
* test rotation, saving instance state - FIXME player loses their opponent when device is rotated
* more testing game state
*
* check for opponent going offline

* what if app crashes? how is data removed from db? Other player times out? Game is abandoned.
* player makes new entry every time app starts, so player will be ok. DBA will need to clear out db

*
* FIXME status messages could be improved
*
* FIXME sometimes two games being created in the database, two devices finding each other simultaneously.
* Could just ignore and pull score from each.
*
* TODO display scores
*
* */

public class MainActivity extends AppCompatActivity implements Contract.ViewInterface {

	//Log tag
	private static final String TAG = "MAIN ACTIVITY";

	//game play constants
//	private static final String ROCK = "rock";
//	private static final String PAPER = "paper";
//	private static final String SCISSORS = "scissors";


	//saved instance state bundle keys
	private static final String PLAYER_INSTANCE_STATE_KEY = "player_bundle_key";

	private TextView opponentStatusTV, resultTV;
	private ImageView opponentPlayIV, thisPlayerPlayIV, rockIV, paperIV, scissorsIV;

	ImageView[] playerChoices;   //Store the rock, paper, scissors ImageViews in an array for convenience, for operations that have to apply to all three

	private Player player;

	private RockPaperScissors game;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState != null) {

			Log.d(TAG, "restoring from instance state");

			player = savedInstanceState.getParcelable(PLAYER_INSTANCE_STATE_KEY);

		} else {

			game = new RockPaperScissors(this);
			player = game.createPlayer();

		}


		opponentPlayIV = (ImageView) findViewById(R.id.opponent_play_image);
		thisPlayerPlayIV = (ImageView) findViewById(R.id.player_play_image);

		rockIV = (ImageView) findViewById(R.id.play_rock);
		paperIV = (ImageView) findViewById(R.id.play_paper);
		scissorsIV = (ImageView) findViewById(R.id.play_scissors);

		//Add the three choices to the array for convenient operations on all three
		playerChoices = new ImageView[3];
		playerChoices[0] = rockIV; playerChoices[1] = paperIV ; playerChoices[2] = scissorsIV;

		opponentStatusTV = (TextView) findViewById(R.id.opponent_status_tv);
		resultTV = (TextView) findViewById(R.id.game_result_tv);

		//Listeners.... resultTV is invisible so the click listener will not fire until it is made visible
		//rock, paper, scissors ImageViews will be disabled so they can't be used until enabled.

		resultTV.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				opponentStatusTV.setText("awaiting opponent restart");
				game.userReset();
			}
		});


		for (ImageView view : playerChoices) {
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					played(view);
				}
			});
		}


		//Disable buttons until an opponent has been found, and game can begin

		opponentStatusTV.setText("Looking for an opponent");
		enableButtons(false);

		game.playerReady();

	}



	@Override
	public void newGameDisplayPlayChoices() {

		highlightUserPlay(null);

		opponentPlayIV.setVisibility(View.INVISIBLE);
		thisPlayerPlayIV.setVisibility(View.INVISIBLE);
		resultTV.setVisibility(View.INVISIBLE);

		enableButtons(true);

		opponentStatusTV.setText("Waiting for other player ready to start game, and make their choice...");

	}


	private void played(View view) {

		opponentStatusTV.setText("Thank you - awaiting opponent play");

		highlightUserPlay(view);
		enableButtons(false);

		switch (view.getId()) {
			case R.id.play_rock : {
				game.userPlayed(Play.ROCK);
				break;
			}

			case R.id.play_paper : {
				game.userPlayed(Play.PAPER);
				break;
			}

			case R.id.play_scissors : {
				game.userPlayed(Play.SCISSORS);
				break;
			}
		}

	}


	public void notifyOpponentHasPlayed() {
		opponentStatusTV.setText("Opponent has made their choice...");

	}


	/*this is for displaying the two user's plays */
	public void displayPlayChoicesMade(String playerPlay, String opponentPlay) {

		Log.d(TAG, "display results, this player " + playerPlay + " opponent play " + opponentPlay ) ;


		opponentStatusTV.setText("You both played...");

		thisPlayerPlayIV.setImageDrawable(getDrawable(playerPlay));
		opponentPlayIV.setImageDrawable(getDrawable(opponentPlay));

		thisPlayerPlayIV.setVisibility(View.VISIBLE);
		opponentPlayIV.setVisibility(View.VISIBLE);

	}



	@Override
	public void displayResults(Result result) {

		//todo show score

		Log.d(TAG, "display results, " + result);

		opponentStatusTV.setText("Game over!");

		String restartInstructions = " tap here to restart";

		if (result == Result.WON) {
			resultTV.setText("YOU WIN" + restartInstructions);
		}

		else if (result == Result.DRAW) {
			resultTV.setText("A draw" + restartInstructions);
		}

		else {
			resultTV.setText("OPPONENT WINS" + restartInstructions);
		}

		resultTV.setVisibility(View.VISIBLE);

	}





	@Override
	public void opponentConnectionLost() {

		Toast.makeText(this, "Lost connection to opponent", Toast.LENGTH_LONG).show();
		opponentStatusTV.setText("Connection to opponent lost, seeking new opponent");


	}




	@Override
	public void onPause() {
		super.onPause();
		Log.d(TAG, "on pause, this player " + player);
		game.playerActivityPauses();    //remove self from DB
	}

	@Override
	public void onResume(){
		super.onResume();
		Log.d(TAG, "on resume, this player " + player);

		//Add self back to DB
		game.playerActivityResumes();


	}

	@Override
	public void onSaveInstanceState(Bundle bundle) {
		bundle.putParcelable(PLAYER_INSTANCE_STATE_KEY, player);
	}



	private Drawable getDrawable(String play) {

		int drawableId = 0;
		switch (play) {
			case Play.ROCK:
				drawableId = R.drawable.rock;
				break;
			case Play.PAPER:
				drawableId = R.drawable.paper;
				break;
			case Play.SCISSORS:
				drawableId = R.drawable.scissors;
				break;
		}

		if (drawableId != 0) {
			return ContextCompat.getDrawable(this, drawableId);
		} else {
			Log.w(TAG, "Invalid opponent play choice, nothing to display");
			return null;
		}
	}


	private void enableButtons(boolean enabled) {
		rockIV.setEnabled(enabled);
		paperIV.setEnabled(enabled);
		scissorsIV.setEnabled(enabled);

	}


	//Draw yellow border on user's selected choice
	private void highlightUserPlay(View view) {

		//If view is null, unhighlight everything by removing any padding and setting the background to white
		//else highlight the View view by adding a yellow background, and setting padding to make the background slightly bigger than the image, so yellow background shows

		if (view == null) {
			for (ImageView v : playerChoices) {
				v.setBackgroundColor(Color.WHITE);
				v.setPadding(0, 0, 0, 0);
			}
		} else {
			view.setBackgroundColor(Color.YELLOW);
			view.setPadding(5, 5, 5, 5);
		}
	}


}
