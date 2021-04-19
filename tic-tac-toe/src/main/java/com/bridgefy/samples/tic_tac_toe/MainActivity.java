package com.bridgefy.samples.tic_tac_toe;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bridgefy.samples.tic_tac_toe.entities.MatchPlayerHolder;
import com.bridgefy.samples.tic_tac_toe.entities.Move;
import com.bridgefy.samples.tic_tac_toe.entities.Player;
import com.bridgefy.samples.tic_tac_toe.entities.RefuseMatch;
import com.bridgefy.sdk.client.Bridgefy;
import com.bridgefy.sdk.client.BridgefyClient;
import com.bridgefy.sdk.client.Device;
import com.bridgefy.sdk.client.RegistrationListener;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    public final static String TAG = "MainActivity";

    private boolean isRegistered = false;
    String username;

    @BindView(R.id.users_toolbar)
    Toolbar toolbar;
    @BindView(R.id.players_recycler_view)
    RecyclerView playersRecyclerView;
    static PlayersAdapter playersAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        // load our username
        username = getSharedPreferences(Constants.PREFS_NAME, 0).getString(Constants.PREFS_USERNAME, null);

        if (BridgefyListener.isThingsDevice(this)) {
            //if this device is running Android Things, don't go through any UI interaction and
            //start right away
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            //enabling bluetooth automatically
            bluetoothAdapter.enable();

            username = Build.MANUFACTURER + " " + Build.MODEL;
            Bridgefy.debug = BuildConfig.DEBUG;
            // initialize the Bridgefy framework
            Bridgefy.initialize(getBaseContext(), registrationListener);
            setupList();

        } else {
            // check that we have permissions, otherwise fire the IntroActivity
            if ((ContextCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) ||
                    (username == null)) {
                startActivity(new Intent(getBaseContext(), IntroActivity.class));
                finish();
            } else {
                // initialize the Bridgefy framework
                Bridgefy.initialize(getBaseContext(), registrationListener);
                setupList();
            }
        }
    }

    private void setupList() {
        // initialize the PlayersAdapter and the RecyclerView
        playersAdapter = new PlayersAdapter();
        playersRecyclerView.setAdapter(playersAdapter);
        playersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
//            playersRecyclerView.addItemDecoration(new DividerItemDecoration(this,
//                    DividerItemDecoration.VERTICAL));
    }

    @Override
    protected void onDestroy() {
        // check that the activity is actually finishing before freeing resources
        if (isRegistered && isFinishing()) {
            // unregister the Otto bus and free up resources
            BridgefyListener.getOttoBus().unregister(this);
            BridgefyListener.release();

            // stop bridgefy operations
            Bridgefy.stop();
        }

        super.onDestroy();
    }


    /**
     *      OTTO EVENT BUS LISTENER
     *      These events are managed via the Otto plugin, which is not a part of the Bridgefy framework.
     */
    @Subscribe
    public void onPlayerFound(Player player) {
        Log.d(TAG, "Player found: " + player.getNick());
        playersAdapter.addPlayer(player);
    }

    @Subscribe
    public void onPlayerLost(Device player) {
        // The Player.uuid field is created with the first 5 digits of the Device.uuid field
        Log.w(TAG, "Player lost: " + player.getUserId());
        playersAdapter.removePlayer(player.getUserId());
    }

    @Subscribe
    public static void onMoveReceived(Move move) {
        // Add the Move to our corresponding match
        playersAdapter.addMove(move);
    }

    @Subscribe
    public void onMatchRefused(RefuseMatch refuseMatch) {
        // drop the match we just received
        playersAdapter.dropMatch(refuseMatch.getMatchId());
    }

    public static void dropMatch(String matchId) {
        playersAdapter.dropMatch(matchId);
    }


    /**
     *      BRIDGEFY REGISTRATION LISTENER
     */
    private RegistrationListener registrationListener = new RegistrationListener() {
        @Override
        public void onRegistrationSuccessful(BridgefyClient bridgefyClient) {
            Log.i(TAG, "onRegistrationSuccessful:");
            Log.i(TAG, "... Device Rating " + bridgefyClient.getDeviceProfile().getRating());
            Log.i(TAG, "... Device Evaluation " + bridgefyClient.getDeviceProfile().getDeviceEvaluation());

            // initialize our EventListener
            BridgefyListener.initialize(getApplicationContext(), bridgefyClient.getUserUuid(), username);

            // register this activity as a Bus listener
            BridgefyListener.getOttoBus().register(MainActivity.this);

            // set this activity as StateListener
            Bridgefy.start(
                    BridgefyListener.getMessageListener(),
                    BridgefyListener.getStateListener());

            isRegistered = true;
        }

        @Override
        public void onRegistrationFailed(int i, String s) {
            Log.w(TAG, "onRegistrationFailed: " + s);
            Toast.makeText(getBaseContext(), getString(R.string.error), Toast.LENGTH_LONG).show();
            startActivity(new Intent(getBaseContext(), IntroActivity.class));
            finish();
        }
    };


    /**
     *      PLAYER ADAPTER CLASS
     */
    public class PlayersAdapter extends RecyclerView.Adapter<PlayerViewHolder> {

        // the list that holds our incoming players and their match id fields
        ArrayList<MatchPlayerHolder> matchPlayers;

        PlayersAdapter() {
            matchPlayers = new ArrayList<>();
        }


        @Override
        public int getItemCount() {
            return matchPlayers.size();
        }

        void addPlayer(Player player) {
            MatchPlayerHolder mph = new MatchPlayerHolder(player);
            int playerPosition = getPlayerPosition(player.getUuid());
            if (playerPosition == -1) {
                matchPlayers.add(mph);
                notifyItemInserted(matchPlayers.size() - 1);
            }
        }

        void addMove(Move move) {
            // find and replace the Player row if it exists
            int otherPlayerPosition = getPlayerPosition(move.getOtherUuid());
            if (otherPlayerPosition > -1) {
                Log.i(TAG, "Updating from Player to Move");
                matchPlayers.get(otherPlayerPosition).setMove(move);
                notifyItemChanged(otherPlayerPosition);

            } else {
                // otherwise, look for an existing Move object that matches this Match Id
                int otherMovePosition = getMovePosition(move.getMatchId());
                if (otherMovePosition > -1) {
                    Log.i(TAG, "Updating Move");
                    matchPlayers.get(otherMovePosition).setMove(move);
                    notifyItemChanged(otherMovePosition);

                } else {
                    // if nothing was found, add the Move as a new MatchPlayer entity
                    Log.i(TAG, "Adding Move from third party.");
                    matchPlayers.add(new MatchPlayerHolder(move));
                    notifyItemInserted(matchPlayers.size() - 1);
                }
            }
        }

        int getPlayerPosition(String uuid) {
            for (int i = 0; i < matchPlayers.size(); i++) {
                if (matchPlayers.get(i).getPlayer() != null &&
                        matchPlayers.get(i).getPlayer().getUuid().equals(uuid))
                    return i;
            }
            return -1;
        }

        int getMovePosition(String matchId) {
            for (int i = 0; i < matchPlayers.size(); i++) {
                if (matchPlayers.get(i).getMove() != null &&
                        matchPlayers.get(i).getMove().getMatchId().equals(matchId))
                    return i;
            }
            return -1;
        }

        void removePlayer(String playerId) {
            for (int i = 0; i < matchPlayers.size(); i++) {
                if (matchPlayers.get(i).getPlayer() != null &&
                        matchPlayers.get(i).getPlayer().getUuid().equals(playerId)) {
                    matchPlayers.remove(i);
                    notifyItemRemoved(i);
                }
            }
        }

        void dropMatch(String matchId) {
            for (int i = 0; i < matchPlayers.size(); i++) {
                if (matchPlayers.get(i).getMove() != null &&
                        matchPlayers.get(i).getMove().getMatchId().equals(matchId)) {

                    if (matchPlayers.get(i).getPlayer() != null) {
                        matchPlayers.get(i).setMove(null);
                        notifyItemChanged(i);
                    } else {
                        matchPlayers.remove(i);
                        notifyItemRemoved(i);
                    }
                }
            }
        }

        @Override
        public PlayerViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View playerView = LayoutInflater.from(viewGroup.getContext()).
                    inflate((R.layout.player_row), viewGroup, false);
            return new PlayerViewHolder(playerView);
        }

        @Override
        public void onBindViewHolder(PlayerViewHolder playerViewHolder, int position) {
            playerViewHolder.setMatchPlayerHolder(matchPlayers.get(position));
        }
    }

    class PlayerViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @BindView(R.id.txt_player)
        TextView playerView;

        MatchPlayerHolder matchPlayerHolder;

        PlayerViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            view.setOnClickListener(this);
        }

        void setMatchPlayerHolder(MatchPlayerHolder mph) {
            this.matchPlayerHolder = mph;

            // most common case are nearby players which might include a Match information
            if (mph.getPlayer() != null) {
                playerView.setText(mph.getPlayer().getNick());

                // A bold style marks player with ongoing matches
                if (mph.getMatchId() != null) {
                    playerView.setTypeface(null, Typeface.BOLD);
                }

            // but a MPH object with just a Move child means we're watching someone else play
            } else if (mph.getMove() != null) {
                playerView.setText(
                        mph.getMove().getParticipants().get(TicTacToeActivity.O).getNick() + " vs. " +
                        mph.getMove().getParticipants().get(TicTacToeActivity.X).getNick());
            }
        }

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getBaseContext(), MatchActivity.class)
                    .putExtra(Constants.INTENT_EXTRA_MOVE, matchPlayerHolder.getMoveString());
            if (matchPlayerHolder.getPlayer() != null)
                intent.putExtra(Constants.INTENT_EXTRA_PLAYER, matchPlayerHolder.getPlayer().toString());
            startActivity(intent);
        }
    }
}
