package com.xuershangda.joystick.nav;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.xuershangda.joystick.R;

/**
 * 跟随Activity，开始跟随、停止跟随、切换跟随
 *
 * @author yinlei
 * @since 2020-10-27
 */
public class FollowActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_follow);

        setTitle(R.string.follow);

        findViewById(R.id.follow_target).setOnClickListener(v -> {

        });
    }
}