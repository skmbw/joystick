package com.xuershangda.joystick.nav;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import com.xuershangda.joystick.R;

public class NavigationActivity extends AppCompatActivity {
    private static final String TAG = "NavigationActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        findViewById(R.id.finger).setOnClickListener(v -> {
            Log.d(TAG, "onCreate: finger view is clicked.");
        });
    }
}