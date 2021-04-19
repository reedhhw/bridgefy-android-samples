package com.bridgefy.samples.tic_tac_toe;

import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.bridgefy.samples.tic_tac_toe.entities.Move;
import com.bridgefy.samples.tic_tac_toe.entities.Player;
import com.bridgefy.samples.tic_tac_toe.entities.RefuseMatch;
import com.bridgefy.sdk.client.Bridgefy;
import com.squareup.otto.Subscribe;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.OnClick;

public class MatchActivity extends TicTacToeActivity {

    public static final String TAG = "MatchActivity";

    // a globaly available variable that identifies the current match
    public static String matchId;
    private boolean publicMatch = false;

    // a Participants object
    HashMap<Character, Player> participants;

    // score board
    @BindView(R.id.xScore)
    TextView scoreX;
    @BindView(R.id.oScore)
    TextView scoreO;
    @BindView(R.id.players)
    TextView players;

    private int sequence = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable the Up button
        ActionBar ab = getSupportActionBar();

        // get our match parameters
        String rivalString = getIntent().getStringExtra(Constants.INTENT_EXTRA_PLAYER);
        Move move = Move.create(getIntent().getStringExtra(Constants.INTENT_EXTRA_MOVE));

        // personal match
        if (rivalString != null) {
            rival = Player.create(rivalString);
            player = BridgefyListener.getPlayer();
            if (ab != null)
                ab.setTitle(rival.getNick());

            // initialize scores
            rival.setWins(0);
            player.setWins(0);

        // public match
        } else {
            rival  = move.getParticipants().get(X);
            player = move.getParticipants().get(O);
            publicMatch = true;

            // update views
            disableInputs();
            tv_turn.setVisibility(View.GONE);
            players.setVisibility(View.VISIBLE);
            players.setText("X: " + rival.getNick() + "\n" + "O: " + player.getNick());
            if (ab != null)
                ab.setTitle(rival.getNick() + "vs. " + player.getNick());
        }

        // create our participants object for the Move message
        participants = new HashMap<>();
        participants.put(X, player);
        participants.put(O, rival);

        // check if this Match was started with a corresponding matchId
        if (move != null)
            onMoveReceived(move);

        // register this activity on the Otto plugin (not a part of the Bridgefy framework)
        BridgefyListener.getOttoBus().register(this);
    }

    @Override
    protected void onDestroy() {
        // unregister this activity from the Otto plugin (not a part of the Bridgefy framework)
        if (BridgefyListener.getOttoBus()!=null)
        BridgefyListener.getOttoBus().unregister(this);

        if (isFinishing()) {
            Log.w(TAG, "Setting matchId to null");
            matchId = null;
        }

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_match, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        endMatch();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_exit:
                endMatch();
                return true;
        }
        return false;
    }

    private void endMatch() {
        if (matchId != null && !publicMatch) {
            Bridgefy.sendBroadcastMessage(new RefuseMatch(matchId, false).toHashMap());
            MainActivity.dropMatch(matchId);
        }
        finish();
    }

    @Override
    void sendMove(int[][] board) {
        // generate this game's match Id
        if (matchId == null) {
            matchId = generateMatchId();
            Log.d(TAG, "Starting Match with: " + rival.getNick());
            Log.d(TAG, "            matchId: " + matchId);
        }

        // create the Move object
        Move move = new Move(matchId, ++sequence, board);
        move.setParticipants(participants);

        // log
        Log.d(TAG, "Sending Move for matchId: " + matchId);
        Log.d(TAG, "... " + move.toString());

        // preserve the Move locally and send it as a message
        onMoveReceived(move);
        MainActivity.onMoveReceived(move);
        Bridgefy.sendBroadcastMessage(move.toHashMap());

        // implement a timeout for the current match
        new ScheduledThreadPoolExecutor(1).schedule(() -> {
            Log.w(TAG, "Timeout for matchId: " + matchId);
            endMatch();
        }, 25, TimeUnit.MINUTES);
    }

    @Override
    // TODO actualizar textview indicando al ganador cuando acabe una partida como espectador
    void sendWinner() {
        incrementWin(turn);
        updateScores();
        tv_turn.setText(getString(R.string.you_win));

        // create the Move object
        Move move = new Move(matchId, ++sequence, board);
        move.setParticipants(participants);
        move.setWinner(myTurnChar == X ? 1 : 2);

        // log
        Log.d(TAG, "Sending Move for matchId: " + matchId);
        Log.d(TAG, "... " + move.toString());

        // preserve the Move locally and send it as a message
        onMoveReceived(move);
        MainActivity.onMoveReceived(move);
        Bridgefy.sendBroadcastMessage(move.toHashMap());
    }

    @Override
    void sendDraw(int[][] board) {
        // create the Move object
        Move move = new Move(matchId, ++sequence, board);
        move.setParticipants(participants);
        move.setWinner(-1);

        // log
        Log.d(TAG, "Sending Draw for matchId: " + matchId);
        Log.d(TAG, "... " + move.toString());

        // preserve the Move locally and send it as a message
        onMoveReceived(move);
        MainActivity.onMoveReceived(move);
        Bridgefy.sendBroadcastMessage(move.toHashMap());
    }

    @OnClick({R.id.button_new_match})
    void newMatch() {
        // clear the board
        btnNewMatch.setVisibility(View.GONE);
        initializeBoard();
    }


    /**
     *      OTTO EVENT BUS LISTENER
     *      These events are managed via the Otto plugin, which is not a part of the Bridgefy framework.
     */
    @Subscribe
    public void onMoveReceived(Move move) {
        // work only with Move objects from our current match or if a match hasn't been set yet
        if (move.getMatchId().equals(matchId) || matchId == null) {
            if (move.getSequence() > sequence) {
                // get a reference to our matchId
                if (matchId == null) {
                    matchId = move.getMatchId();
                    Log.d(TAG, "Starting Match with: " + rival.getNick());
                    Log.d(TAG, "            matchId: " + matchId);
                } else {
                    Log.d(TAG, "Move received for matchId: " + move.getMatchId());
                    Log.d(TAG, "... " + move.toString());
                }

                // enable the controls again if they had stopped before
                if (matchStopped && !publicMatch) {
                    initializeBoard();
                    matchStopped = false;
                }

                if (move.getWinner() == 0) {
                    // if the other player started the match, switch the symbols
                    if (move.getSequence() % 2 == 1) {
                        turn = O;
                        myTurnChar = O;
                        participants.put(O, player);
                        participants.put(X, rival);
                    }
                    updateTurnView(publicMatch);
                } else if (move.getWinner() == -1) {
                    stopMatch(!myTurn);
                    tv_turn.setText(getString(R.string.draw));
                } else {
                    // update the scores
                    participants.get(X).setWins(move.getParticipants().get(X).getWins());
                    participants.get(O).setWins(move.getParticipants().get(O).getWins());
                    updateScores();

                    // update the turn text
                    tv_turn.setText(String.format(getString(R.string.their_win), rival.getNick()));
                    stopMatch(true);
                }

                // switch the turn
                myTurn = true;
                turn = myTurnChar;

                // set the board to its current status and update the sequence
                resetBoard(move.getBoard());
                this.sequence = move.getSequence();
            } else {
                Log.w(TAG, "Dumping Move object with an expired seq.");
                Log.w(TAG, "... " + move.toString());
            }
        }
    }

    protected void stopMatch(boolean myTurn) {
        matchStopped = true;

        disableInputs();

        // show the new match button if we can start the game again
        if (myTurn && !publicMatch)
            btnNewMatch.setVisibility(View.VISIBLE);
    }

    private void updateTurnView(boolean publicMatch) {
        // TODO preservar información del turno
//        if (publicMatch)
//            tv_turn.setText(String.format(getString(R.string.their_turn),
//                    participants.get(myTurnChar).getNick(), String.valueOf(flipChar(myTurnChar))));
//        else
            tv_turn.setText(String.format(getString(R.string.your_turn),
                    String.valueOf(myTurnChar)));
    }

    // answer automatically if the current device is an Android Things device
    @Subscribe
    public void respondMoveIfThingsDevice(String incomingMatchId) {
        if (matchId != null && matchId.equals(incomingMatchId) &&
//                player.getNick().equals("Nexus 5X")) {
                BridgefyListener.isThingsDevice((getApplicationContext()))) {
            if (!matchStopped) {
                int[][] board = makeRandomMove();
                resetBoard(board);
                sendMove(board);
            }
        }
    }

    private void incrementWin(char winner) {
        int newScore = participants.get(winner).getWins() + 1;
        participants.get(winner).setWins(newScore);
    }

    private void updateScores() {
        scoreX.setText(String.valueOf(participants.get(X).getWins()));
        scoreO.setText(String.valueOf(participants.get(O).getWins()));
    }

    @Subscribe
    public void onMatchRefused(RefuseMatch refuseMatch) {
        Log.d(TAG, "RefuseMatch received for matchId: " + refuseMatch.getMatchId());
        if (refuseMatch.getMatchId().equals(matchId)) {
            Toast.makeText(getBaseContext(),
                                String.format(getString(R.string.match_rejected), rival.getNick()),
                                Toast.LENGTH_LONG).show();
            finish();
        }
    }


    private String generateMatchId() {
        return UUID.randomUUID().toString().substring(0, 5);
    }
}
