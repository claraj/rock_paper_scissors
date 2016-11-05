package com.clara.rockpaperscissors;

import android.provider.ContactsContract;
import android.util.Log;
import android.widget.Toast;

import com.clara.rockpaperscissors.Model.Game;
import com.clara.rockpaperscissors.Model.Player;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

/**
 * Created by clara on 11/5/16. Firebase IO.
 */
public class Firebase implements Contract.ModelInterface {

	private final static String TAG = "Firebase IO";

	///firebase IO here

	private FirebaseDatabase database;
	private DatabaseReference playersReference;
	private DatabaseReference gamesReference;

	//Firebase key constants
	private static final String PLAYERS = "players";
	private static final String GAMES = "games";


	public interface OpponentDatabaseCallback {
		void availableOpponentFound(Player op);
		void noOpponentAvailable(boolean error);
	}

	interface PlayerDatabaseCallback {
		//todo?
	}


	public Firebase() {
		database = FirebaseDatabase.getInstance();
		playersReference = database.getReference().child(PLAYERS);
		gamesReference = database.getReference().child(GAMES);
	}

	@Override
	public void savePlayerToDB(Player player) {

		if (player.key == null) {
			DatabaseReference ref = playersReference.push();
			ref.setValue(player);
			player.key = ref.getKey();
			Log.d(TAG, "Saved self to db, " + player);
		}

		else {

			//else if there is a key, update the player with this key
			DatabaseReference playerRef = playersReference.child(player.key);

			playerRef.setValue(player);

			Log.d(TAG, "Saved updated self in  db, " + player);
			player.key = playerRef.getKey();   //todo test ri
		}

	}


	@Override
	public void listenForDiscovery(Player thisPlayer, final OpponentDatabaseCallback callback) {

		//Has a game with this player set as player 2 been created?
		Query haveBeenPaired = gamesReference.orderByChild("player2key").equalTo(thisPlayer.key);

		haveBeenPaired.addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {

				Game temp = dataSnapshot.getValue(Game.class);

				Log.d(TAG, "Have been paired value event, game is " + temp);

				//first response from db may be that there are no results. Ignore.
				if (dataSnapshot.getValue() == null || temp.player1key == null || temp.player2key == null) {
					Log.d(TAG, "game from DB is empty, ignoring");
				}

				else {

					Game game = dataSnapshot.getValue(Game.class);
					game.key = dataSnapshot.getKey();
					//isPlayer1 = false;
					gamesReference.removeEventListener(this);

					Log.d(TAG, "Have been paired value event, game has data " + game);

					//fetch player 1's info, set as opponent

					Query getPlayer1 = playersReference.child(game.player1key);
					getPlayer1.addListenerForSingleValueEvent(new ValueEventListener() {
						@Override
						public void onDataChange(DataSnapshot dataSnapshot) {
							Player opponent = dataSnapshot.getValue(Player.class);
							opponent.key = dataSnapshot.getKey();
							Log.d(TAG, "Fetched opponent info " + opponent);

							callback.availableOpponentFound(opponent);
						}

						@Override
						public void onCancelled(DatabaseError databaseError) {
							Log.e(TAG, "fetch player 1", databaseError.toException());
						}
					});
				}
			}

			@Override
			public void onCancelled(DatabaseError databaseError) {
				Log.e(TAG, "listen for pairing", databaseError.toException());
			}
		});

	}


	@Override
	public void findOpponent(final String playerKey, final OpponentDatabaseCallback callback) {

		//Select a random opponent from the list of players online.

		//get the last entries sorted by key. Since they are added by key, which is sorted by date, this gets the most recent
		Query findOpponent = playersReference.orderByKey().limitToLast(30);

		findOpponent.addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {

//				if (opponent != null) {
//					//if there is already an opponent, return.
//					return;
//				}

				Player possibleOpponent;
				Player opponent = null;

				for (DataSnapshot ds : dataSnapshot.getChildren()) {
					possibleOpponent = ds.getValue(Player.class);

					//If this opponent is not us, and is available, select
					if (! ds.getKey().equals(playerKey) && possibleOpponent.available) {
						opponent = possibleOpponent;
						opponent.key = ds.getKey();
						break;
					}
				}

				if (opponent != null) {
					//stop listening for events and notify that opponent is available
					playersReference.removeEventListener(this);
					callback.availableOpponentFound(opponent);


				} else {
					callback.noOpponentAvailable(false); //no error, just no-one available
				}

			}


			@Override
			public void onCancelled(DatabaseError databaseError) {
				Log.e(TAG, "find opponent error", databaseError.toException());
				callback.noOpponentAvailable(true);
			}
		});


	}


	@Override
	public void awaitOpponentPlay(Player opponent, final Contract.GameInterface callback) {

		Query awaitOpponentPlay = playersReference.child(opponent.key);

		awaitOpponentPlay.addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {

				//This should happen once opponent plays
				//should have one result....

				Log.d(TAG, "Opponent data fetched, check if they have made their play");

				//todo what to do if opponent leaves game, this will have no results.
				//if opponent has left then notify user, do they want to search for another opponent(?)

				if (dataSnapshot == null || dataSnapshot.getValue(Player.class) == null) {
					Log.d(TAG, "No opponent found, searching for new opponent");
					// todo "Lost opponent, searching for new opponent", Toast.LENGTH_LONG).show();
					playersReference.removeEventListener(this);

					callback.opponentConnectionLost();
				}

				else {
					Player opponentData = dataSnapshot.getValue(Player.class);

					if (opponentData.played == null) {
						//ignore
						Log.d(TAG, "opponent has not yet played");
						return;

					} else {
						//opponent play has been made
						Player opponent = opponentData;
						opponentData.key = dataSnapshot.getKey();
						Log.d(TAG, "opponent has played" + opponent);
						callback.opponentPlayed(opponent);
						playersReference.removeEventListener(this);
					}
				}
			}

			@Override
			public void onCancelled(DatabaseError databaseError) {

			}
		});

	}

	@Override
	public void awaitOpponentReset(Player opponent, final Contract.GameInterface callback) {

		Query opponentReset = playersReference.child(opponent.key);
		opponentReset.addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {
				if (dataSnapshot.child("played").getValue() == null) {
					//opponent has reset
					callback.opponentReset();
				}
			}

			@Override
			public void onCancelled(DatabaseError databaseError) {
				Log.e(TAG, "Await opponent reset query", databaseError.toException());
			}
		});


	}


}
