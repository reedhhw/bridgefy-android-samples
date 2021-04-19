package com.bridgefy.samples.fileshare;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bridgefy.samples.fileshare.entities.Peer;
import com.bridgefy.sdk.client.BFBleProfile;
import com.bridgefy.sdk.client.BFEnergyProfile;
import com.bridgefy.sdk.client.BFEngineProfile;
import com.bridgefy.sdk.client.Bridgefy;
import com.bridgefy.sdk.client.BridgefyClient;
import com.bridgefy.sdk.client.Config;
import com.bridgefy.sdk.client.Device;
import com.bridgefy.sdk.client.Message;
import com.bridgefy.sdk.client.MessageListener;
import com.bridgefy.sdk.client.RegistrationListener;
import com.bridgefy.sdk.client.Session;
import com.bridgefy.sdk.client.StateListener;
import com.bridgefy.sdk.framework.exceptions.MessageException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private String TAG = "MainActivity";

    static final String INTENT_EXTRA_NAME = "peerName";
    static final String INTENT_EXTRA_UUID = "peerUuid";
    static final String INTENT_EXTRA_MSG  = "bridgefyFile";
    static final String INTENT_BROADCAST_MSG  = "bridgefyFileProgress";
    static final String INTENT_MSG_PROGRESS  = "bridgefyFileProgress";

    PeersRecyclerViewAdapter peersAdapter =
            new PeersRecyclerViewAdapter(new ArrayList<Peer>());


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Configure the Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        RecyclerView recyclerView = findViewById(R.id.peer_list);
        recyclerView.setAdapter(peersAdapter);

        Bridgefy.debug = BuildConfig.DEBUG;

        Bridgefy.initialize(getApplicationContext(), new RegistrationListener() {
            @Override
            public void onRegistrationSuccessful(BridgefyClient bridgefyClient) {
                // Start Bridgefy
                startBridgefy();
            }

            @Override
            public void onRegistrationFailed(int errorCode, String message) {
                Toast.makeText(getBaseContext(), getString(R.string.registration_error),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (isFinishing())
            Bridgefy.stop();
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }


    /**
     *      BRIDGEFY METHODS
     */
    private void startBridgefy() {
        Config.Builder builder=new Config.Builder();
        builder.setAntennaType(Config.Antenna.BLUETOOTH_LE);
        builder.setAutoConnect(true);
        builder.setEncryption(true);
        builder.setMaxConnectionRetries(3);
        builder.setBleProfile(BFBleProfile.BACKWARDS_COMPATIBLE);
        builder.setEnergyProfile(BFEnergyProfile.HIGH_PERFORMANCE);
        builder.setEngineProfile(BFEngineProfile.BFConfigProfileLongReach);
        Bridgefy.start(messageListener, stateListener,builder.build());
    }



    private final MessageListener messageListener = new MessageListener() {
        @Override
        public void onMessageReceived(Message message) {
            Log.i(TAG, "onMessageReceived: ");
            // direct messages carrying a Device name represent device handshakes
            if (message!= null && message.getContent()  != null && message.getContent().get("device_name") != null) {
                Peer peer = new Peer(message.getSenderId(),
                        (String) message.getContent().get("device_name"));
                peer.setNearby(true);
                peersAdapter.addPeer(peer);

                // any other direct bridgefyFile should be treated as such
            } else {
                Log.i(TAG, "onMessageReceived: sending broadcast to "+message.getSenderId());
                LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(
                        new Intent(message.getSenderId())
                                .putExtra(INTENT_EXTRA_MSG, message));
            }

        }

        @Override
        public void onMessageDataProgress(UUID message, long progress, long fullSize) {
            int currentProgress = (int) ((progress * 100) / fullSize);
            LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(
                    new Intent(INTENT_BROADCAST_MSG)
                    .putExtra(INTENT_MSG_PROGRESS, currentProgress)
            );
        }

        @Override
        public void onMessageFailed(Message message, MessageException e) {
            Log.e(TAG, "Message sent failed: " + message.getUuid() + " sender: " + message.getSenderId());
            LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(
                    new Intent(INTENT_BROADCAST_MSG).putExtra(INTENT_MSG_PROGRESS, 0)
            );
        }
    };


    StateListener stateListener = new StateListener() {
        @Override
        public void onDeviceConnected(final Device device, Session session) {
            // send our information to the Device
            HashMap<String, Object> map = new HashMap<>();
            map.put("device_name", Build.MANUFACTURER + " " + Build.MODEL);
            device.sendMessage(map);
        }

        @Override
        public void onDeviceLost(Device peer) {
            Log.w(TAG, "onDeviceLost: " + peer.getUserId());
            peersAdapter.removePeer(peer);
            LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(
                    new Intent(INTENT_BROADCAST_MSG).putExtra(INTENT_MSG_PROGRESS, 0)
            );
        }

        @Override
        public void onDeviceDetected(Device device) {

        }

        @Override
        public void onDeviceUnavailable(Device device) {

        }

        @Override
        public void onStartError(String message, int errorCode) {
            Log.e(TAG, "onStartError: " + message);

            if (errorCode == StateListener.INSUFFICIENT_PERMISSIONS) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            // Start Bridgefy
            startBridgefy();

        } else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
            Toast.makeText(this, "Location permissions needed to start peers discovery.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }


    /**
     *      RECYCLER VIEW CLASSES
     */
    class PeersRecyclerViewAdapter
            extends RecyclerView.Adapter<PeersRecyclerViewAdapter.PeerViewHolder> {

        private final List<Peer> peers;

        PeersRecyclerViewAdapter(List<Peer> peers) {
            this.peers = peers;
        }

        @Override
        public int getItemCount() {
            return peers.size();
        }

        void addPeer(Peer peer) {
            int position = getPeerPosition(peer.getUuid());
            if (position > -1) {
                peers.set(position, peer);
                notifyItemChanged(position);
            } else {
                peers.add(peer);
                notifyItemInserted(peers.size() - 1);
            }
        }

        void removePeer(Device lostPeer) {
            int position = getPeerPosition(lostPeer.getUserId());
            if (position > -1) {
                Peer peer = peers.get(position);
                peer.setNearby(false);
                peers.set(position, peer);
                notifyItemChanged(position);
            }
        }

        private int getPeerPosition(String peerId) {
            for (int i = 0; i < peers.size(); i++) {
                if (peers.get(i).getUuid().equals(peerId))
                    return i;
            }
            return -1;
        }

        @Override
        public PeerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.peer_row, parent, false);
            return new PeerViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final PeerViewHolder peerHolder, int position) {
            peerHolder.setPeer(peers.get(position));
        }

        class PeerViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            final TextView mContentView;
            Peer peer;

            PeerViewHolder(View view) {
                super(view);
                mContentView = view.findViewById(R.id.peerName);
                view.setOnClickListener(this);
            }

            void setPeer(Peer peer) {
                this.peer = peer;
                this.mContentView.setText(peer.getDeviceName());


                if (peer.isNearby()) {
                    this.mContentView.setTextColor(Color.BLACK);
                } else {
                    this.mContentView.setTextColor(Color.GRAY);
                }
            }

            public void onClick(View v) {
                startActivity(new Intent(getBaseContext(), FileActivity.class)
                        .putExtra(INTENT_EXTRA_NAME, peer.getDeviceName())
                        .putExtra(INTENT_EXTRA_UUID, peer.getUuid()));
            }
        }
    }
}
