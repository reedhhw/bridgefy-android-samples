package com.bridgefy.samples.fileshare;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bridgefy.samples.fileshare.entities.BridgefyFile;
import com.bridgefy.sdk.client.Bridgefy;
import com.bridgefy.sdk.client.Message;
import com.nbsp.materialfilepicker.MaterialFilePicker;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class FileActivity  extends AppCompatActivity {

    private static final String TAG = "file_activity";
    private String conversationName;
    private String conversationId;

    MessagesRecyclerViewAdapter messagesAdapter = new MessagesRecyclerViewAdapter(new ArrayList<>());
    private ProgressDialog progressDialog;

    private BroadcastReceiver conversationReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "onReceive: received message to adapter");
            Message message = intent.getParcelableExtra(MainActivity.INTENT_EXTRA_MSG);
            byte[] fileBytes = message.getData();
            BridgefyFile bridgefyFile = new BridgefyFile((String)message.getContent().get("file"));
            bridgefyFile.setData(fileBytes);
            bridgefyFile.setDeviceName(intent.getStringExtra(MainActivity.INTENT_EXTRA_NAME));
            bridgefyFile.setDirection(BridgefyFile.INCOMING_FILE);
            messagesAdapter.addMessage(bridgefyFile);
        }
    };

    private BroadcastReceiver progressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int progres = intent.getIntExtra(MainActivity.INTENT_MSG_PROGRESS, 0);
            if (progressDialog != null)
            {
                progressDialog.setProgress(progres);
                if (progres == 100 || progres == 0)
                    progressDialog.dismiss();
            }

        }
    };

    @BindView(R.id.toolbar) Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file);
        ButterKnife.bind(this);

        // recover our Peer object
        conversationName = getIntent().getStringExtra(MainActivity.INTENT_EXTRA_NAME);
        conversationId   = getIntent().getStringExtra(MainActivity.INTENT_EXTRA_UUID);

        // Configure the Toolbar
        setSupportActionBar(toolbar);

        // Enable the Up button
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle(conversationName);
            ab.setDisplayHomeAsUpEnabled(true);
        }
        Log.i("C", "onCreate: conversation id is "+conversationId);

        // register the receiver to listen for incoming bridgefyFiles
        LocalBroadcastManager.getInstance(getBaseContext()).registerReceiver(conversationReceiver, new IntentFilter(conversationId));

        LocalBroadcastManager.getInstance(getBaseContext()).registerReceiver(progressReceiver, new IntentFilter(MainActivity.INTENT_BROADCAST_MSG));

        // configure the recyclerview
        RecyclerView messagesRecyclerView = findViewById(R.id.message_list);
        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(this);
        messagesRecyclerView.setLayoutManager(mLinearLayoutManager);
        messagesRecyclerView.setAdapter(messagesAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getBaseContext()).unregisterReceiver(conversationReceiver);
        LocalBroadcastManager.getInstance(getBaseContext()).unregisterReceiver(progressReceiver);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }


    @OnClick({R.id.fab})
    public void onMessageSend(View v) {


        new MaterialFilePicker()
                .withActivity(this)
                .withRequestCode(1987)
                .withHiddenFiles(true) // Show hidden files and folders
                .start();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1987 && data != null) {
            String filePath = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);
            Log.i(TAG, "onActivityResult: file path " + filePath);
            File file = new File(filePath);
            byte fileContent[] = new byte[(int) file.length()];

            try {
                FileInputStream fin = new FileInputStream(file);
                fin.read(fileContent);
                HashMap<String, Object> content = new HashMap<>();
                content.put("file", file.getName());

                Message.Builder builder = new Message.Builder();
                Message message = builder.setReceiverId(conversationId).setContent(content).setData(fileContent).build();
                message.setUuid(Bridgefy.sendMessage(message));
                BridgefyFile bridgefyFile = new BridgefyFile(filePath);
                bridgefyFile.setDirection(BridgefyFile.OUTGOING_FILE);
                bridgefyFile.setData(fileContent);
                messagesAdapter.addMessage(bridgefyFile);

                progressDialog = new ProgressDialog(FileActivity.this);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setIndeterminate(false);
                progressDialog.setTitle(file.getName());
                progressDialog.setMax(100);
                progressDialog.setProgress(1);
                progressDialog.setCancelable(false);
                progressDialog.show();

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }



    /**
     *      RECYCLER VIEW CLASSES
     */
    class MessagesRecyclerViewAdapter
            extends RecyclerView.Adapter<MessagesRecyclerViewAdapter.MessageViewHolder> {

        private final List<BridgefyFile> bridgefyFiles;

        MessagesRecyclerViewAdapter(List<BridgefyFile> bridgefyFiles) {
            this.bridgefyFiles = bridgefyFiles;
        }

        @Override
        public int getItemCount() {
            return bridgefyFiles.size();
        }

        void addMessage(BridgefyFile bridgefyFile) {
            bridgefyFiles.add(0, bridgefyFile);
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            return bridgefyFiles.get(position).getDirection();
        }

        @Override
        public MessageViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View messageView = null;

            switch (viewType) {
                case BridgefyFile.INCOMING_FILE:
                    messageView = LayoutInflater.from(viewGroup.getContext()).
                            inflate((R.layout.message_row_incoming), viewGroup, false);
                    break;
                case BridgefyFile.OUTGOING_FILE:
                    messageView = LayoutInflater.from(viewGroup.getContext()).
                            inflate((R.layout.message_row_outgoing), viewGroup, false);
                    break;
            }

            return new MessageViewHolder(messageView);
        }

        @Override
        public void onBindViewHolder(final MessageViewHolder messageHolder, int position) {
            messageHolder.setBridgefyFile(bridgefyFiles.get(position));
        }

        class MessageViewHolder extends RecyclerView.ViewHolder {
            final TextView txtMessage;
            BridgefyFile bridgefyFile;

            MessageViewHolder(View view) {
                super(view);
                txtMessage = view.findViewById(R.id.txtMessage);
            }

            void setBridgefyFile(BridgefyFile bridgefyFile) {
                this.bridgefyFile = bridgefyFile;
                    this.txtMessage.setText(bridgefyFile.getFilePath() + " File size "+bridgefyFile.getData().length);

            }
        }
    }
}
