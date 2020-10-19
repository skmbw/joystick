package com.xuershangda.joystick.nav;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import com.xuershangda.joystick.R;
import com.xuershangda.joystick.listener.FingerTouchViewListener;
import com.xuershangda.joystick.view.FingerPaintImageView;

public class NavigationActivity extends AppCompatActivity {
    private static final String TAG = "NavigationActivity";
    private FingerPaintImageView fingerPaintImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        fingerPaintImageView = findViewById(R.id.finger);
        fingerPaintImageView.setOnClickListener(v -> {
            Log.d(TAG, "onCreate: finger view is clicked.");
        });

        fingerPaintImageView.setFingerTouchViewListener(new FingerTouchViewListener() {
            @Override
            public void onTouch(double x, double y) {

            }

            @Override
            public void onReset() {

            }

            @Override
            public void onActionDown(double x, double y) {

            }

            @Override
            public void onActionUp(double x, double y) {

            }
        });
    }
}