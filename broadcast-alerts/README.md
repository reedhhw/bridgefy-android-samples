# Bridgefy Broadcast Alerts Sample  

Simple app to send Alerts to nearby devices.

### Initialization

Done via an inline declaration. The API_KEY in this example is passed as a parameter to the initialize method,
as opposed to specified in the AndroidManifest.xml file 

```
Bridgefy.initialize(getApplicationContext(), "YOUR API KEY", new RegistrationListener() {
    @Override
    public void onRegistrationSuccessful(BridgefyClient bridgefyClient) {
        //Important data can be fetched from the BridgefyClient object
        deviceId.setText(bridgefyClient.getUserUuid());

        //Once the registration process has been successful, we can start operations
        Bridgefy.start(messageListener, stateListener);
    }
 
    @Override
    public void onRegistrationFailed(int i, String s) {
        Log.e(TAG, e);
    }
});
```

Note that it is not necessary to call the methods from the superclass. This is the same for other listeners 
such as the StateListener and MessageListener.

### StateListener and Android Things support

The Bridgefy SDK is compatible with Android Things devices, you might want to enable Bluetooth on those devices programmatically.

```
if (isThingsDevice(this)) {
    // enabling bluetooth automatically
    bluetoothAdapter.enable();
}
```

The StateListener offer valuable information should any errors occur during startup.
It is a good idea to always implement the onStartError(…) method.

```
StateListener stateListener = new StateListener() {
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
                    prepareMessage();
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
};
```
