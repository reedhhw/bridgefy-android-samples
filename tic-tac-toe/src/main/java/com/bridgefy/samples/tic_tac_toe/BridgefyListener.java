package com.bridgefy.samples.tic_tac_toe;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;

import com.bridgefy.samples.tic_tac_toe.entities.Event;
import com.bridgefy.samples.tic_tac_toe.entities.Move;
import com.bridgefy.samples.tic_tac_toe.entities.Player;
import com.bridgefy.samples.tic_tac_toe.entities.RefuseMatch;
import com.bridgefy.sdk.client.Bridgefy;
import com.bridgefy.sdk.client.Device;
import com.bridgefy.sdk.client.Message;
import com.bridgefy.sdk.client.MessageListener;
import com.bridgefy.sdk.client.Session;
import com.bridgefy.sdk.client.StateListener;
import com.google.gson.Gson;
import com.squareup.otto.Bus;

/**
 * @author dekaru on 5/10/17.
 */
public class BridgefyListener {

    private static final String TAG = "BridgefyListener";

    private static BridgefyListener instance;

    // This sample app uses the Otto Event Bus to communicate between app components easily.
    // The Otto plugin is not a part of the Bridgefy framework
    private Bus ottoBus;
    private Context context;

    private Player mPlayer;


    private BridgefyListener(Context context, Bus ottoBus) {
        this.context = context;
        this.ottoBus = ottoBus;
    }

    static void initialize(Context context, String uuid, String username) {
        instance = new BridgefyListener(context, new Bus());
        instance.setmPlayer(new Player(uuid, username));
    }

    static void release() {
        instance = null;
    }


    /**
     *      BRIDGEFY LISTENER IMPLEMENTATIONS
     */
    private StateListener stateListener = new StateListener() {
        @Override
        public void onDeviceConnected(Device device, Session session) {
            // send a handshake to nearby devices
            device.sendMessage(mPlayer.toHashMap());
        }

        @Override
        public void onDeviceLost(Device device) {
            // let our components know that a device is no longer in range
            ottoBus.post(device);
        }

        @Override
        public void onDeviceDetected(Device device) {

        }

        @Override
        public void onDeviceUnavailable(Device device) {

        }

        @Override
        public void onStarted() {
            Log.i(TAG, "onStarted()");
        }

        @Override
        public void onStartError(String s, int i) {
            Log.e(TAG, s);
        }

        @Override
        public void onStopped() {
            Log.w(TAG, "onStopped()");
        }
    };

    private MessageListener messageListener = new MessageListener() {
        @Override
        public void onMessageReceived(Message message) {
            // identify the type of incoming event
            Event.EventType eventType = extractType(message);
            switch (eventType) {
                case FIRST_MESSAGE:
                    // recreate the Player object from the incoming message
                    // post the found object to our activities via the Otto plugin
                    ottoBus.post(Player.create(message));
                    break;
            }
        }

        @Override
        public void onBroadcastMessageReceived(Message message) {
            // build a TicTacToe Move object from our incoming Bridgefy Message
            Event.EventType eventType = extractType(message);
            switch (eventType) {
                case MOVE_EVENT:
                    final Move move = Move.create(message);
                    // log
                    Log.d(TAG, "Move received for matchId: " + move.getMatchId());
                    Log.d(TAG, "... " + move.toString());

                    // start the MatchActivity if we are on a Things Device
                    if (isThingsDevice(context) && MatchActivity.matchId == null) {
//                    if (MatchActivity.matchId == null && BridgefyListener.getUuid().equals("b649bdea-43fb-44ae-afce-02866d9933f4")) {
                        // get a reference to our player object
                        int pos = MainActivity.playersAdapter.getPlayerPosition(move.getOtherUuid());
                        if (pos > -1) {
                            final Player player = MainActivity.playersAdapter.matchPlayers.get(pos).getPlayer();
                            if (player != null) {
                                // start the activity with the incoming player
                                Intent intent = new Intent(context, MatchActivity.class)
                                        .putExtra(Constants.INTENT_EXTRA_PLAYER, player.toString())
                                        .putExtra(Constants.INTENT_EXTRA_MOVE, move.toString());
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(intent);

                                // repost to our newly created activity so it responds automatically
                                new Handler().postDelayed(() -> {
                                    // post this event via the Otto plugin so our components can update their views
                                    ottoBus.post(move);
                                    // answer automatically if the current device is an Android Things device
                                    ottoBus.post(move.getMatchId());
                                }, 1500);
                            } else {
                                Log.w(TAG, "Incoming player unknown.");
                            }
                        } else {
                            Log.w(TAG, "Incoming player unknown.");
                        }
                    }

                    // post this event via the Otto plugin so our components can update their views
                    ottoBus.post(move);

                    // answer automatically if the current device is an Android Things device
                    ottoBus.post(move.getMatchId());
                    break;

                case REFUSE_MATCH:
                    // recreate the RefuseMatch object from the incoming message
                    // post the found object to our activities via the Otto plugin
                    ottoBus.post(RefuseMatch.create(message));

                    // let iPhone devices know we're available (not required on Android)


                    Bridgefy.sendBroadcastMessage(new Event<>(
                            Event.EventType.AVAILABLE,
                            this).toHashMap());
                    break;

                case AVAILABLE:
                    Log.d(TAG, "AVAILABLE event not implemented.");
                    break;

                default:
                    Log.d(TAG, "Unrecognized Event received: " +
                            new Gson().toJson(message.getContent().toString()));
                    break;
            }

            // TODO make moves persistent

            // if it's not a Move object from our current match, create a notification
//            if (!move.getMatchId().equals(MatchActivity.getCurrentMatchId())) {
//                // TODO create a notification for the incoming move
//            }
        }

        private Event.EventType extractType(Message message) {
            int eventOrdinal;
            Object eventObj = message.getContent().get("event");
            if (eventObj instanceof Double) {
                eventOrdinal = ((Double) eventObj).intValue();
            } else {
                eventOrdinal = (Integer) eventObj;
            }
            return Event.EventType.values()[eventOrdinal];
        }
    };


    /**
     *      UTILS METHODS
     */

    static boolean isThingsDevice(Context context) {
        final PackageManager pm = context.getPackageManager();
        return pm.hasSystemFeature("android.hardware.type.embedded");
    }


    /**
     *      GETTERS
     */

    static Bus getOttoBus() {
        return instance.ottoBus;
    }

    static StateListener getStateListener() {
        return instance.stateListener;
    }

    static MessageListener getMessageListener() {
        return instance.messageListener;
    }

    public static Player getPlayer() {
        return instance.mPlayer;
    }

    void setmPlayer(Player mPlayer) {
        this.mPlayer = mPlayer;
    }
}
