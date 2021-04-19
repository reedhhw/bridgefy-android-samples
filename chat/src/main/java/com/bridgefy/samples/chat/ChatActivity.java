package com.bridgefy.samples.chat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bridgefy.samples.chat.entities.Message;
import com.bridgefy.samples.chat.entities.Peer;
import com.bridgefy.sdk.client.BFEngineProfile;
import com.bridgefy.sdk.client.Bridgefy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.bridgefy.samples.chat.MainActivity.BROADCAST_CHAT;
import static com.bridgefy.samples.chat.MainActivity.INTENT_EXTRA_NAME;
import static com.bridgefy.samples.chat.MainActivity.INTENT_EXTRA_UUID;
import static com.bridgefy.samples.chat.MainActivity.PAYLOAD_DEVICE_NAME;
import static com.bridgefy.samples.chat.MainActivity.PAYLOAD_DEVICE_TYPE;
import static com.bridgefy.samples.chat.MainActivity.PAYLOAD_TEXT;


public class ChatActivity extends AppCompatActivity {

    private String conversationName;
    private String conversationId;


    @BindView(R.id.txtMessage)
    EditText txtMessage;

    MessagesRecyclerViewAdapter messagesAdapter =
            new MessagesRecyclerViewAdapter(new ArrayList<Message>());


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        ButterKnife.bind(this);

        // recover our Peer object
        conversationName = getIntent().getStringExtra(INTENT_EXTRA_NAME);
        conversationId   = getIntent().getStringExtra(INTENT_EXTRA_UUID);

        // Configure the Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Enable the Up button
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle(conversationName);
            ab.setDisplayHomeAsUpEnabled(true);
        }

        // register the receiver to listen for incoming messages
        LocalBroadcastManager.getInstance(getBaseContext())
                .registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        Message message = new Message(intent.getStringExtra(MainActivity.INTENT_EXTRA_MSG));
                        message.setDeviceName(intent.getStringExtra(MainActivity.INTENT_EXTRA_NAME));
                        message.setDirection(Message.INCOMING_MESSAGE);
                        messagesAdapter.addMessage(message);
                    }
                }, new IntentFilter(conversationId));

        // configure the recyclerview
        RecyclerView messagesRecyclerView = findViewById(R.id.message_list);
        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setReverseLayout(true);
        messagesRecyclerView.setLayoutManager(mLinearLayoutManager);
        messagesRecyclerView.setAdapter(messagesAdapter);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }


    @OnClick({R.id.btnSend})
    public void onMessageSend(View v) {
        // get the message and push it to the views
        String messageString = txtMessage.getText().toString();
        if (messageString.trim().length() > 0) {
            // update the views
            txtMessage.setText("");
            Message message = new Message(messageString);
            message.setDirection(Message.OUTGOING_MESSAGE);
            messagesAdapter.addMessage(message);

            // create a HashMap object to send
            HashMap<String, Object> content = new HashMap<>();
            content.put(PAYLOAD_TEXT, messageString);

            // send text message to device(s)
            if (conversationId.equals(BROADCAST_CHAT)) {
                // we put extra information in broadcast packets since they won't be bound to a session
                content.put(PAYLOAD_DEVICE_NAME, Build.MANUFACTURER + " " + Build.MODEL);
                content.put(PAYLOAD_DEVICE_TYPE, Peer.DeviceType.ANDROID.ordinal());

                com.bridgefy.sdk.client.Message.Builder builder=new com.bridgefy.sdk.client.Message.Builder();
                builder.setContent(content);
                Bridgefy.sendBroadcastMessage(builder.build(),
                        BFEngineProfile.BFConfigProfileLongReach);
            } else {

                com.bridgefy.sdk.client.Message.Builder builder=new com.bridgefy.sdk.client.Message.Builder();
                builder.setContent(content).setReceiverId(conversationId);

                Bridgefy.sendMessage(builder.build(),
                        BFEngineProfile.BFConfigProfileLongReach);
            }
        }
    }


    /**
     *      RECYCLER VIEW CLASSES
     */
    class MessagesRecyclerViewAdapter
            extends RecyclerView.Adapter<MessagesRecyclerViewAdapter.MessageViewHolder> {

        private final List<Message> messages;

        MessagesRecyclerViewAdapter(List<Message> messages) {
            this.messages = messages;
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        void addMessage(Message message) {
            messages.add(0, message);
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            return messages.get(position).getDirection();
        }

        @Override
        public MessageViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View messageView = null;

            switch (viewType) {
                case Message.INCOMING_MESSAGE:
                    messageView = LayoutInflater.from(viewGroup.getContext()).
                            inflate((R.layout.message_row_incoming), viewGroup, false);
                    break;
                case Message.OUTGOING_MESSAGE:
                    messageView = LayoutInflater.from(viewGroup.getContext()).
                            inflate((R.layout.message_row_outgoing), viewGroup, false);
                    break;
            }

            return new MessageViewHolder(messageView);
        }

        @Override
        public void onBindViewHolder(final MessageViewHolder messageHolder, int position) {
            messageHolder.setMessage(messages.get(position));
        }

        class MessageViewHolder extends RecyclerView.ViewHolder {
            final TextView txtMessage;
            Message message;

            MessageViewHolder(View view) {
                super(view);
                txtMessage = view.findViewById(R.id.txtMessage);
            }

            void setMessage(Message message) {
                this.message = message;

                if (message.getDirection() == Message.INCOMING_MESSAGE &&
                        conversationId.equals(BROADCAST_CHAT)) {
                    this.txtMessage.setText(message.getDeviceName() + ":\n" + message.getText());
                } else {
                    this.txtMessage.setText(message.getText());
                }
            }
        }
    }
}
