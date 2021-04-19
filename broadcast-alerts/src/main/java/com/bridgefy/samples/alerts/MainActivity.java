package com.bridgefy.samples.alerts;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.bridgefy.samples.alerts.model.Alert;
import com.bridgefy.samples.alerts.ux.alerts.AlertFragment;
import com.bridgefy.samples.alerts.ux.main.MainFragment;
import com.bridgefy.samples.alerts.ux.main.MainViewModel;
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
import com.bridgefy.sdk.client.StateListener;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private String TAG = "BRIDGEFY_SAMPLE";

    private ArrayList<Alert> alertsData = new ArrayList<>();

    Config.Builder builder = new Config.Builder()
            .setAutoConnect(true)
            .setEngineProfile(BFEngineProfile.BFConfigProfileLongReach)
            .setEnergyProfile(BFEnergyProfile.BALANCED)
            .setBleProfile(BFBleProfile.EXTENDED_RANGE)
            .setMaxConnectionRetries(5)
            .setAntennaType(Config.Antenna.BLUETOOTH_LE);

    MainViewModel mainViewModel;

    NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        mainViewModel.init();
        if (isThingsDevice(this)) {
            // enabling bluetooth automatically
            bluetoothAdapter.enable();
        }


        Bridgefy.debug = BuildConfig.DEBUG;
        Bridgefy.initialize(getApplicationContext(), null, new RegistrationListener() {
            @Override
            public void onRegistrationSuccessful(BridgefyClient bridgefyClient) {
                // Important data can be fetched from the BridgefyClient object
                // deviceId.setText(bridgefyClient.getUserUuid());
                mainViewModel.getBridgefyClientMutableLiveData().postValue(bridgefyClient);
                // Once the registration process has been successful, we can start operations
                Bridgefy.start(messageListener, stateListener, builder.build());
            }

            @Override
            public void onRegistrationFailed(int i, String e) {
                Log.e(TAG, e);
            }
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();

        NavigationUI.setupWithNavController(toolbar, navController);
        NavigationUI.setupActionBarWithNavController(this, navController);
    }


    public boolean isThingsDevice(Context context) {
        final PackageManager pm = context.getPackageManager();
        return pm.hasSystemFeature("android.hardware.type.embedded");
    }

    StateListener stateListener = new StateListener()
    {
        @Override
        public void onStarted() {
            Log.i(TAG, "onStarted: Bridgefy started");

            if (isThingsDevice(getApplicationContext())) {
                ///for Android Things devices, we automatically send an alert as soon as possible
                //since there is no UI to do it
                //A message will be dispatched every 10 seconds
                final Handler handler = new Handler();

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        mainViewModel.sendMessageBroadcast();
                        handler.postDelayed(this, 10000);
                    }
                });
            }
        }

        @Override
        public void onStartError(String s, int i) {
            switch (i) {
                case (StateListener.INSUFFICIENT_PERMISSIONS):
                    //starting operations will fail if you haven't granted the necessary permissions
                    //request them and try again after they have been granted
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
                    break;
                case (StateListener.LOCATION_SERVICES_DISABLED):
                    //location in the device has been disabled
                    break;
            }
        }

        @Override
        public void onDeviceDetected(Device device) {

        }

        @Override
        public void onDeviceUnavailable(Device device) {

        }
    };

    MessageListener messageListener = new MessageListener()
    {
        @Override
        public void onBroadcastMessageReceived(Message message) {
            mainViewModel.getMessageMutableLiveData().postValue(message);
        }

    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //retry again after permissions have been granted
            Bridgefy.start(messageListener, stateListener, builder.build());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // always stop Bridgefy when it's no longer necessary
        Bridgefy.stop();
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}
