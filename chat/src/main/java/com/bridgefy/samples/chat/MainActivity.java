package com.bridgefy.samples.chat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bridgefy.samples.chat.entities.Peer;
import com.bridgefy.sdk.client.Bridgefy;
import com.bridgefy.sdk.client.BridgefyClient;
import com.bridgefy.sdk.client.Device;
import com.bridgefy.sdk.client.Message;
import com.bridgefy.sdk.client.MessageListener;
import com.bridgefy.sdk.client.RegistrationListener;
import com.bridgefy.sdk.client.Session;
import com.bridgefy.sdk.client.StateListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private String TAG = "MainActivity";

    static final String INTENT_EXTRA_NAME = "peerName";
    static final String INTENT_EXTRA_UUID = "peerUuid";
    static final String INTENT_EXTRA_TYPE = "deviceType";
    static final String INTENT_EXTRA_MSG  = "message";
    static final String BROADCAST_CHAT    = "Broadcast";

    static final String PAYLOAD_DEVICE_TYPE  = "device_type";
    static final String PAYLOAD_DEVICE_NAME  = "device_name";
    static final String PAYLOAD_TEXT         = "text";

    PeersRecyclerViewAdapter peersAdapter =
            new PeersRecyclerViewAdapter(new ArrayList<>());


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


        if (isThingsDevice(this)) {
            //enabling bluetooth automatically
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            bluetoothAdapter.enable();
        }
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_broadcast:
                startActivity(new Intent(getBaseContext(), ChatActivity.class)
                        .putExtra(INTENT_EXTRA_NAME, BROADCAST_CHAT)
                        .putExtra(INTENT_EXTRA_UUID, BROADCAST_CHAT));
                return true;
        }
        return false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }


    /**
     *      BRIDGEFY METHODS
     */
    private void startBridgefy() {
        Bridgefy.start(messageListener, stateListener);
    }

    // TODO change for BridgefyUtils method
    public boolean isThingsDevice(Context context) {
        final PackageManager pm = context.getPackageManager();
        return pm.hasSystemFeature("android.hardware.type.embedded");
    }

    private MessageListener messageListener = new MessageListener() {
        @Override
        public void onMessageReceived(Message message) {
            // direct messages carrying a Device name represent device handshakes
            if (message.getContent().get(PAYLOAD_DEVICE_NAME) != null) {
                Peer peer = new Peer(message.getSenderId(),
                        (String) message.getContent().get(PAYLOAD_DEVICE_NAME));
                peer.setNearby(true);
                peer.setDeviceType(extractType(message));
                peersAdapter.addPeer(peer);
                Log.d(TAG, "Peer introduced itself: " + peer.getDeviceName());

            // any other direct message should be treated as such
            } else {
                String incomingMessage = (String) message.getContent().get("text");
                Log.d(TAG, "Incoming private message: " + incomingMessage);
                LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(
                        new Intent(message.getSenderId())
                                .putExtra(INTENT_EXTRA_MSG, incomingMessage));
            }

            // if it's an Android Things device, reply automatically
            if (isThingsDevice(MainActivity.this)) {
                Log.d(TAG, "I'm a bot. Responding message automatically.");
                HashMap<String, Object> content = new HashMap<>();
                content.put("text", "Beep boop. I'm a bot.");
                Message replyMessage = Bridgefy.createMessage(message.getSenderId(), content);
                Bridgefy.sendMessage(replyMessage);
            }
        }

        @Override
        public void onBroadcastMessageReceived(Message message) {
            // we should not expect to have connected previously to the device that originated
            // the incoming broadcast message, so device information is included in this packet
            String incomingMsg = (String) message.getContent().get(PAYLOAD_TEXT);
            String deviceName  = (String) message.getContent().get(PAYLOAD_DEVICE_NAME);
            Peer.DeviceType deviceType = extractType(message);

            Log.d(TAG, "Incoming broadcast message: " + incomingMsg);
            LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(
                    new Intent(BROADCAST_CHAT)
                            .putExtra(INTENT_EXTRA_NAME, deviceName)
                            .putExtra(INTENT_EXTRA_TYPE, deviceType)
                            .putExtra(INTENT_EXTRA_MSG,  incomingMsg));
        }
    };

    private Peer.DeviceType extractType(Message message) {
        int eventOrdinal;
        Object eventObj = message.getContent().get(PAYLOAD_DEVICE_TYPE);
        if (eventObj instanceof Double) {
            eventOrdinal = ((Double) eventObj).intValue();
        } else {
            eventOrdinal = (Integer) eventObj;
        }
        return Peer.DeviceType.values()[eventOrdinal];
    }

    StateListener stateListener = new StateListener() {
        @Override
        public void onDeviceConnected(final Device device, Session session) {
            Log.i(TAG, "onDeviceConnected: " + device.getUserId());
            // send our information to the Device
            HashMap<String, Object> map = new HashMap<>();
            map.put(PAYLOAD_DEVICE_NAME, Build.MANUFACTURER + " " + Build.MODEL);
            map.put(PAYLOAD_DEVICE_TYPE, Peer.DeviceType.ANDROID.ordinal());
            device.sendMessage(map);
        }

        @Override
        public void onDeviceLost(Device device) {
            Log.w(TAG, "onDeviceLost: " + device.getUserId());
            peersAdapter.removePeer(device);
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
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
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

                switch (peer.getDeviceType()) {
                    case ANDROID:
                        this.mContentView.setText(peer.getDeviceName() + " (android)");
                        break;

                    case IPHONE:
                        this.mContentView.setText(peer.getDeviceName() + " (iPhone)");
                        break;
                }

                if (peer.isNearby()) {
                    this.mContentView.setTextColor(Color.BLACK);
                } else {
                    this.mContentView.setTextColor(Color.GRAY);
                }
            }

            public void onClick(View v) {
                startActivity(new Intent(getBaseContext(), ChatActivity.class)
                        .putExtra(INTENT_EXTRA_NAME, peer.getDeviceName())
                        .putExtra(INTENT_EXTRA_UUID, peer.getUuid()));
            }
        }
    }
}
