package com.bridgefy.samples.nearby;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bridgefy.sdk.BuildConfig;
import com.bridgefy.sdk.client.Bridgefy;
import com.bridgefy.sdk.client.BridgefyClient;
import com.bridgefy.sdk.client.Device;
import com.bridgefy.sdk.client.Message;
import com.bridgefy.sdk.client.MessageListener;
import com.bridgefy.sdk.client.RegistrationListener;
import com.bridgefy.sdk.client.Session;
import com.bridgefy.sdk.client.StateListener;
import com.bridgefy.sdk.framework.exceptions.MessageException;

import java.util.ArrayList;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;


public class DevicesActivity extends AppCompatActivity {

    private final String TAG = "DevicesActivity";

    @BindView(R.id.devices_recycler_view)
    RecyclerView devicesRecyclerView;
    DevicesAdapter devicesAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devices);
        ButterKnife.bind(this);

        // initialize the DevicesAdapter and the RecyclerView
        devicesAdapter = new DevicesAdapter();
        devicesRecyclerView.setAdapter(devicesAdapter);
        devicesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // check that we have Location permissions
        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            initializeBridgefy();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Bridgefy.stop();
    }


    /**
     *      BRIDGEFY INITIALIZATION
     */

    RegistrationListener registrationListener = new RegistrationListener() {
        @Override
        public void onRegistrationSuccessful(BridgefyClient bridgefyClient) {
            Log.i(TAG, "onRegistrationSuccessful: current userId is: " + bridgefyClient.getUserUuid());
            Log.i(TAG, "Device Rating " + bridgefyClient.getDeviceProfile().getRating());
            Log.i(TAG, "Device Evaluation " + bridgefyClient.getDeviceProfile().getDeviceEvaluation());

            // Start the Bridgefy SDK
            Bridgefy.start(messageListener, stateListener);
        }

        @Override
        public void onRegistrationFailed(int errorCode, String message) {
            Log.e(TAG, "onRegistrationFailed: failed with ERROR_CODE: " + errorCode + ", MESSAGE: " + message);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(DevicesActivity.this, "Bridgefy registration did not succeed.", Toast.LENGTH_LONG).show();
                }
            });
        }
    };

    private void initializeBridgefy() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (isThingsDevice(this)) {
            // enabling bluetooth automatically
            bluetoothAdapter.enable();
        }
        Bridgefy.debug = BuildConfig.DEBUG;
        //Always use steady context objects to avoid leaks
        Bridgefy.initialize(getApplicationContext(), registrationListener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializeBridgefy();
        } else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
            Toast.makeText(this, "Location permissions needed to start devices discovery.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }


    /**
     *       BRIDGEFY WORKFLOW LISTENERS
     */
    private void sendMessage(Device device) {
        // construir objeto de mensaje
        HashMap<String, Object> data = new HashMap<>();
        data.put("manufacturer ", Build.MANUFACTURER);
        data.put("model", Build.MODEL);
        device.sendMessage(data);

        Log.d(TAG, "Message sent!");
    }

    StateListener stateListener = new StateListener() {
        @Override
        public void onDeviceConnected(Device device, Session session) {
            Log.i(TAG, "Device found: " + device.getUserId());
            sendMessage(device);
        }

        @Override
        public void onDeviceLost(Device device) {
            Log.w(TAG, "Device lost: " + device.getUserId());
        }

        @Override
        public void onDeviceDetected(Device device) {

        }

        @Override
        public void onDeviceUnavailable(Device device) {

        }


        @Override
        public void onStarted() {
            super.onStarted();
            Log.i(TAG, "onStarted: Bridgefy started");
        }

        @Override
        public void onStartError(String s, int i) {
            super.onStartError(s, i);
            Log.e(TAG, "onStartError: " + s + " " + i);
        }

        @Override
        public void onStopped() {
            super.onStopped();
            Log.w(TAG, "onStopped: Bridgefy stopped");
        }
    };

    MessageListener messageListener = new MessageListener() {
        @Override
        public void onMessageReceived(Message message) {
            String s = message.getContent().get("manufacturer ") + " " + message.getContent().get("model");
            Log.d(TAG, "Message Received: " + message.getSenderId() + ", content: " + s);
            devicesAdapter.addDevice(s);
        }

        @Override
        public void onMessageFailed(Message message, MessageException e) {
            Log.e(TAG, "Message failed", e);
        }

        @Override
        public void onMessageSent(Message message) {
            Log.d(TAG, "Message sent to: " + message.getReceiverId());
        }

        @Override
        public void onMessageReceivedException(String s, MessageException e) {
            Log.e(TAG, e.getMessage());

        }
    };


    /**
     *      ADAPTER
     */

    public boolean isThingsDevice(Context context) {
        final PackageManager pm = context.getPackageManager();
        return pm.hasSystemFeature("android.hardware.type.embedded");
    }

    public class DevicesAdapter extends RecyclerView.Adapter<DeviceViewHolder> {
        // the list that holds our incoming devices
        ArrayList<String> devices;

        DevicesAdapter() {
            devices = new ArrayList<>();
        }

        @Override
        public int getItemCount() {
            return devices.size();
        }

        boolean addDevice(String device) {
            if (!devices.contains(device)) {
                devices.add(device);
                notifyItemInserted(devices.size() - 1);
                return true;
            }

            return false;
        }

        void removeDevice(Device device) {
            int position = devices.indexOf(device);
            if (position > -1) {
                devices.remove(position);
                notifyItemRemoved(position);
            }
        }

        @Override
        public DeviceViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View deviceView = LayoutInflater.from(viewGroup.getContext()).
                    inflate((R.layout.device_row), viewGroup, false);
            return new DeviceViewHolder(deviceView);
        }

        @Override
        public void onBindViewHolder(DeviceViewHolder deviceViewHolder, int position) {
            deviceViewHolder.setDevice(devices.get(position));
        }
    }

    class DeviceViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.txt_device)
        TextView deviceView;

        DeviceViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }

        void setDevice(String device) {
            deviceView.setText(device);
        }
    }
}
