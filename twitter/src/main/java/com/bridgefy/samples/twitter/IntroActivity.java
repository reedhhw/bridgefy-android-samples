package com.bridgefy.samples.twitter;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.bridgefy.sdk.client.Bridgefy;
import com.bridgefy.sdk.client.BridgefyClient;
import com.bridgefy.sdk.client.Device;
import com.bridgefy.sdk.client.RegistrationListener;
import com.bridgefy.sdk.client.Session;
import com.bridgefy.sdk.client.StateListener;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class IntroActivity extends AppCompatActivity {

    private String TAG = "IntroActivity";
    static final String INTENT_USERNAME = "peerName";
    String username;

    @BindView(R.id.txt_username)
    public EditText txtUsername;

    @BindView(R.id.btn_start)
    public Button btnStart;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
        ButterKnife.bind(this);

        // set a default name
        username = Build.MODEL.split(" ")[0].length() >= 7 ?
                Build.MODEL.split(" ")[0] : Build.MODEL.replaceAll("\\s","");
        txtUsername.setText(username);;

        // Configure the Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        // Enabling bluetooth automatically
        if (isThingsDevice(this)) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            bluetoothAdapter.enable();
        }
    }


    /**
     *      BRIDGEFY FRAMEWORK METHODS AND OBJECTS
     */
    @OnClick({R.id.btn_start})
    public void initializeBridgefy(View view) {
        // Initialize Bridgefy
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

    private void startBridgefy() {
        Bridgefy.start(null, stateListener);
    }

    StateListener stateListener = new StateListener() {
        @Override
        public void onStarted() {
            Log.i(TAG, "BRIDGEFY FRAMEWORK STARTED!!");
            Toast.makeText(getBaseContext(), "Bridgefy Framework started!", Toast.LENGTH_SHORT).show();

            // Get the username
            String usernameInput = txtUsername.getText().toString().replaceAll("\\s","");
            if (usernameInput.length() > 0)
                username = usernameInput;

            // start the TimelineActivity
            startActivity(new Intent(getBaseContext(), TimelineActivity.class)
                              .putExtra(INTENT_USERNAME, username));
        }

        @Override
        public void onStopped() {
            Log.w(TAG, "BRIDGEFY FRAMEWORK STOPPED!!");
            Toast.makeText(getBaseContext(), "Bridgefy Framework STOPPED!!", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onStartError(String message, int errorCode) {
            Log.e(TAG, "onStartError: " + message);
            if (errorCode == StateListener.INSUFFICIENT_PERMISSIONS) {
                ActivityCompat.requestPermissions(IntroActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
            } else {
                Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onDeviceConnected(final Device device, Session session) {
            Log.i(TAG, "onDeviceConnected: " + device.getUserId());
        }

        @Override
        public void onDeviceLost(Device device) {
            Log.w(TAG, "onDeviceLost: " + device.getUserId());
        }

        @Override
        public void onDeviceDetected(Device device) {

        }

        @Override
        public void onDeviceUnavailable(Device device) {

        }
    };

    public boolean isThingsDevice(Context context) {
        final PackageManager pm = context.getPackageManager();
        return pm.hasSystemFeature("android.hardware.type.embedded");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Start Bridgefy
            startBridgefy();

        } else {
            Toast.makeText(this, "Location permissions needed to start peers discovery.", Toast.LENGTH_SHORT).show();
        }
    }
}
