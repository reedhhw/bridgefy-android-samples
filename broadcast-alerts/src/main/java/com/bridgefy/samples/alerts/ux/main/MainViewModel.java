package com.bridgefy.samples.alerts.ux.main;

import android.os.Build;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.bridgefy.samples.alerts.model.Alert;
import com.bridgefy.sdk.client.Bridgefy;
import com.bridgefy.sdk.client.BridgefyClient;
import com.bridgefy.sdk.client.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainViewModel extends ViewModel {
    private int incomingMessageCount = 0;
    private int outgoingMessageCount = 0;

    private MutableLiveData<List<Alert>> alertsMutableLiveData;
    private MutableLiveData<Message> messageMutableLiveData;
    private MutableLiveData<BridgefyClient> bridgefyClientMutableLiveData;

    private MutableLiveData<String> incomingLiveData;
    private MutableLiveData<String> outgoingLiveData;

    private String number = "number";
    private String date_sent = "date_sent";
    private String device_name = "device_name";

    private List<Alert> alertList;

    public void init() {
        messageMutableLiveData = new MutableLiveData<>();
        bridgefyClientMutableLiveData = new MutableLiveData<>();
        alertsMutableLiveData = new MutableLiveData<>();

        incomingLiveData = new MutableLiveData<>();
        outgoingLiveData = new MutableLiveData<>();

        alertList = new ArrayList<>();
    }

    public void sendMessageBroadcast() {
        //assemble the data that we are about to send
        HashMap<String, Object> data = new HashMap<>();
        data.put(number, outgoingMessageCount);
        data.put(date_sent, Double.parseDouble("" + System.currentTimeMillis()));
        data.put(device_name, Build.MANUFACTURER + " " + Build.MODEL);
        Message message = new Message.Builder().setContent(data).build();

        //Broadcast messages are sent to anyone that can receive it
        String messageID = Bridgefy.sendBroadcastMessage(message);
        outgoingMessageCount++;
        outgoingLiveData.postValue(String.format("%d", outgoingMessageCount));
    }

    void incomingMessageBroadcast(Message message) {
        incomingLiveData.postValue(String.format("%d", incomingMessageCount));
        HashMap<String, Object> content = message.getContent();
        Alert alert = new Alert(message.getSenderId(), (Integer) content.get(number), (String) content.get(device_name), ((Double) content.get(date_sent)).longValue());
        if (!alertList.contains(alert))
        {
            alertList.add(alert);
            alertsMutableLiveData.setValue(alertList);
        }
        incomingMessageCount = alertList.size();
    }

    public MutableLiveData<Message> getMessageMutableLiveData() {
        return messageMutableLiveData;
    }

    public MutableLiveData<BridgefyClient> getBridgefyClientMutableLiveData() {
        return bridgefyClientMutableLiveData;
    }

    public MutableLiveData<List<Alert>> getAlertsMutableLiveData() {
        return alertsMutableLiveData;
    }

    public LiveData<String> getIncomingLiveData() {
        return incomingLiveData;
    }

    public LiveData<String> getOutgoingLiveData() {
        return outgoingLiveData;
    }
}