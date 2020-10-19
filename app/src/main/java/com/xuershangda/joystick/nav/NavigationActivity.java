package com.xuershangda.joystick.nav;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import com.xuershangda.joystick.R;
import com.xuershangda.joystick.listener.FingerTouchViewListener;
import com.xuershangda.joystick.view.FingerPaintImageView;

import java.util.concurrent.atomic.AtomicBoolean;

public class NavigationActivity extends AppCompatActivity {
    private static final String TAG = "NavigationActivity";
    private FingerPaintImageView mFingerPaintImageView;
    private AtomicBoolean startPosition = new AtomicBoolean(false);
    private AtomicBoolean endPoint = new AtomicBoolean(false);
    private float startX;
    private float startY;

    private float endPointStartX;
    private float endPointStartY;
    private float endPointEndX;
    private float endPointEndY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mFingerPaintImageView = findViewById(R.id.finger);

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
                if (endPoint.compareAndSet(false, false)) {
                    endPointStartX = x;
                    endPointStartY = y;
                }
            }

            @Override
            public void onActionUp(float x, float y) {
                Log.d(TAG, "onActionUp: x=" + x + ", y=" + y);
                // 设置起点，以抬起，结束为准
                if (startPosition.compareAndSet(false, true)) {
                    startX = x;
                    startY = y;
                    mFingerPaintImageView.setInEditMode(false);
                }

                if (endPoint.compareAndSet(false, true)) {
                    endPointEndX = x;
                    endPointEndY = y;
                    mFingerPaintImageView.setInEditMode(false);
                }
            }
        });

        findViewById(R.id.click).setOnClickListener(v -> {
            this.mFingerPaintImageView.drawPoint(300, 440);
        });

        findViewById(R.id.clear).setOnClickListener(v -> {
            this.mFingerPaintImageView.clear();
        });

        findViewById(R.id.set_start_position).setOnClickListener(v -> {
            this.mFingerPaintImageView.clear();
            this.mFingerPaintImageView.setInEditMode(true);
            this.endPoint.set(false);
        });

        findViewById(R.id.set_endpoint).setOnClickListener(v -> {
            this.mFingerPaintImageView.clear();
            this.mFingerPaintImageView.setInEditMode(true);
            this.startPosition.set(false);
        });
    }
}