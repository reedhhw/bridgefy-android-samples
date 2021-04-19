package com.bridgefy.samples.tic_tac_toe;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class IntroActivity extends AppCompatActivity {

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
        txtUsername.setText(Build.MANUFACTURER + " " + Build.MODEL);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            startMainActivity();
        else
            Toast.makeText(this, "Location access is required to discover nearby users", Toast.LENGTH_LONG).show();
    }

    @OnClick({R.id.btn_start})
    public void onButtonStart(View view) {
        // check again that we have Location permissions
        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // get ready to rock
            startMainActivity();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.ACCESS_COARSE_LOCATION }, 0);
        }
    }

    private void startMainActivity() {
        // save our username locally
        getSharedPreferences(Constants.PREFS_NAME, 0).edit().putString(Constants.PREFS_USERNAME,
                txtUsername.getText().toString()).apply();

        // fire the Main Activity and finish this one
        startActivity(
                new Intent(getBaseContext(), MainActivity.class)
                        .putExtra(Constants.PREFS_USERNAME, txtUsername.getText().toString()));
        finish();
    }
}