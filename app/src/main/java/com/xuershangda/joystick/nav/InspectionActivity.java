package com.xuershangda.joystick.nav;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.xuershangda.joystick.R;

/**
 * 巡检Activity，设置巡检的点
 *
 * @author yinlei
 * @since 2020-10-27
 */
public class InspectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inspection);

        setTitle(R.string.inspection);
    }
}