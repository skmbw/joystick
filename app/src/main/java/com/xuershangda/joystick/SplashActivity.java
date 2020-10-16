package com.xuershangda.joystick;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.xuershangda.joystick.nav.NavigationActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        findViewById(R.id.navigation).setOnClickListener(v -> {
            Intent intent = new Intent(this, NavigationActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.operation).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        });
    }
}