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
    private FingerPaintImageView mFingerPaintImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mFingerPaintImageView = findViewById(R.id.finger);
//        mFingerPaintImageView.setOnClickListener(v -> {
//            Log.d(TAG, "onCreate: finger view is clicked.");
//            float x = v.getX();
//            float y = v.getY();
//            Log.d(TAG, "onCreate: finger view is click at(x=" + x + ", y=" + y + ").");
//        });

        mFingerPaintImageView.setFingerTouchViewListener(new FingerTouchViewListener() {
            @Override
            public void onTouch(float x, float y) {
                Log.d(TAG, "onTouch: x=" + x + ", y=" + y);
            }

            @Override
            public void onReset() {

            }

            @Override
            public void onActionDown(float x, float y) {
                Log.d(TAG, "onActionDown: x=" + x + ", y=" + y);
            }

            @Override
            public void onActionUp(float x, float y) {
                Log.d(TAG, "onActionUp: x=" + x + ", y=" + y);
                // 设置起点，以抬起，结束为准
            }
        });

        findViewById(R.id.testClickBtn).setOnClickListener(v -> {
//            this.mFingerPaintImageView.handleTouchEnd();
            this.mFingerPaintImageView.drawPoint(300, 440);
        });
    }
}