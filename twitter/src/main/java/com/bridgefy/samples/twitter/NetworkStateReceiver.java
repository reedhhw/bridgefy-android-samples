package com.bridgefy.samples.twitter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

/**
 * @author dekaru on 11/28/17.
 */
public class NetworkStateReceiver extends BroadcastReceiver {

    private static String TAG = "NetworkStateReceiver";

    public static String WIFI_STATE_CONNECTED = "WIFI_STATE_CONNECTED";

    @Override
    public void onReceive(Context context, Intent intent) {
        NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        if (info != null && info.isConnected()) {
            Log.i(TAG, "isConnected!");
            LocalBroadcastManager.getInstance(context).sendBroadcast(
                    new Intent(WIFI_STATE_CONNECTED));
        }
    }
}
