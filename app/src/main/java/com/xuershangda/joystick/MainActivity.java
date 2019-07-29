package com.xuershangda.joystick;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.xuershangda.joystick.controller.DefaultController;
import com.xuershangda.joystick.listener.JoystickTouchViewListener;
import com.xuershangda.joystick.utils.BigDecimalUtils;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private DefaultController mDefaultController;
    private static final String HOST = "http://10.70.10.112:8000/";
    private OkHttpClient mOkHttpClient;
    private Double mLeftSpeed = 0D;
    private Double mRightSpeed = 0D;
    private BlockingDeque<Double[]> mBlockingDeque;
    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mOkHttpClient = new OkHttpClient();
        mBlockingDeque = new LinkedBlockingDeque<>();
        mTextView = findViewById(R.id.sway);

        // 不能放在上面，因为view还没有初始化，肯定找不到这个布局
        RelativeLayout viewGroup = findViewById(R.id.joyStickView);

        mDefaultController = new DefaultController(MainActivity.this, viewGroup);
        mDefaultController.createViews();
        mDefaultController.showViews(false);

        mDefaultController.setLeftTouchViewListener(new JoystickTouchViewListener() {
            @Override
            public void onTouch(float horizontalPercent, float verticalPercent) {
                Log.d(TAG, "onTouch left: " + horizontalPercent + ", " + verticalPercent);
                Double[] speeds = mapSpeedMode3(horizontalPercent, verticalPercent);
                Double leftSpeed = speeds[0];
                Double rightSpeed = speeds[1];

                if (Math.abs(BigDecimalUtils.subtract(leftSpeed, mLeftSpeed)) <= 0.01
                        && Math.abs(BigDecimalUtils.subtract(rightSpeed, mRightSpeed)) <= 0.01) {
                    Log.d(TAG, "onTouch: 速度变化太小，忽略。mLeftSpeed=" + mLeftSpeed + ", mRightSpeed="
                            + mRightSpeed + ", leftSpeed=" + leftSpeed + ", rightSpeed=" + rightSpeed);
                    return;
                }

                try {
                    Log.d(TAG, "onTouch: mLeftSpeed=" + mLeftSpeed + ", mRightSpeed="
                            + mRightSpeed + ", leftSpeed=" + leftSpeed + ", rightSpeed=" + rightSpeed);
                    mLeftSpeed = leftSpeed;
                    mRightSpeed = rightSpeed;
                    Log.d(TAG, "onTouch: 插入控制命令到队列成功。");
                    mBlockingDeque.put(speeds);
                    Log.d(TAG, "onTouch: task size=[" + mBlockingDeque.size() + "]");
                } catch (Exception e) {
                    Log.e(TAG, "onTouch: 产生任务错误。", e);
                }
            }

            @Override
            public void onReset() {
                // 回到控制点，停止
                Log.d(TAG, "onReset: left. stop.");
                Log.d(TAG, "onReset: clear BlockingDeque, task size=[" + mBlockingDeque.size() + "]");
                mBlockingDeque.clear();

                String url = HOST + "teleop/0";
                call(url, Collections.emptyMap(), new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        Log.e(TAG, "reset onFailure: ", e);
                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                        ResponseBody body = response.body();
                        if (body != null) {
                            Log.i(TAG, "reset onResponse: " + body.string());
                        }
                    }
                });
            }

            @Override
            public void onActionDown() {
                Log.d(TAG, "onActionDown: left");
            }

            @Override
            public void onActionUp() {
                Log.d(TAG, "onActionUp: left, stop.");
            }
        });

        mDefaultController.setRightTouchViewListener(new JoystickTouchViewListener() {
            @Override
            public void onTouch(float horizontalPercent, float verticalPercent) {
                Log.d(TAG, "onTouch right: " + horizontalPercent + ", " + verticalPercent);
                Double[] speeds = mapSpeed(horizontalPercent, verticalPercent);
                Double leftSpeed = speeds[0];
                Double rightSpeed = speeds[1];

                if (Math.abs(BigDecimalUtils.subtract(leftSpeed, mLeftSpeed)) <= 0.01
                        && Math.abs(BigDecimalUtils.subtract(rightSpeed, mRightSpeed)) <= 0.01) {
                    Log.d(TAG, "onTouch: 速度变化太小，忽略。mLeftSpeed=" + mLeftSpeed + ", mRightSpeed="
                            + mRightSpeed + ", leftSpeed=" + leftSpeed + ", rightSpeed=" + rightSpeed);
                    return;
                }

                try {
                    Log.d(TAG, "onTouch: mLeftSpeed=" + mLeftSpeed + ", mRightSpeed="
                            + mRightSpeed + ", leftSpeed=" + leftSpeed + ", rightSpeed=" + rightSpeed);
                    mLeftSpeed = leftSpeed;
                    mRightSpeed = rightSpeed;
                    Log.d(TAG, "onTouch: 插入控制命令到队列成功。");
                    mBlockingDeque.put(speeds);
                    Log.d(TAG, "onTouch: task size=[" + mBlockingDeque.size() + "]");
                } catch (Exception e) {
                    Log.e(TAG, "onTouch: 产生任务错误。", e);
                }
            }

            @Override
            public void onReset() {
                // 回到控制点，停止
                Log.d(TAG, "onReset: right. stop.");
                Log.d(TAG, "onReset: clear BlockingDeque, task size=[" + mBlockingDeque.size() + "]");
                mBlockingDeque.clear();

                String url = HOST + "teleop/0";
                call(url, Collections.emptyMap(), new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        Log.e(TAG, "reset onFailure: ", e);
                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                        ResponseBody body = response.body();
                        if (body != null) {
                            Log.i(TAG, "reset onResponse: " + body.string());
                        }
                    }
                });
            }

            @Override
            public void onActionDown() {
                Log.d(TAG, "onActionDown: right");
            }

            @Override
            public void onActionUp() {
                Log.d(TAG, "onActionUp: right, stop.");

//                String url = HOST + "teleop/1";
//                call(url, Collections.emptyMap(), new Callback() {
//                    @Override
//                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
//                        Log.e(TAG, "onActionUp onFailure: ", e);
//                    }
//
//                    @Override
//                    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
//                        ResponseBody body = response.body();
//                        if (body != null) {
//                            Log.i(TAG, "onActionUp onResponse: " + body.string());
//                        }
//                    }
//                });
            }
        });

        Thread thread = new Action();
        thread.start();
    }

    public class Action extends Thread {
        @Override
        public void run() {
            try {
                for (;;) {
                    Double[] speeds = mBlockingDeque.take();

                    Log.d(TAG, "run: 从队列中获取控制命令成功。taskNumber=[" + mBlockingDeque.size() + "]");

                    Double leftSpeed = speeds[0];
                    Double rightSpeed = speeds[1];
                    Log.d(TAG, "onTouch: leftWheel=" + leftSpeed + ", rightWheel=" + rightSpeed);

                    mLeftSpeed = leftSpeed;
                    mRightSpeed = rightSpeed;
                    runOnUiThread(() -> {
                        double diff = BigDecimalUtils.subtract(leftSpeed, rightSpeed);
                        mTextView.setText(String.format("%s%s", getString(R.string.sway), diff));
                    });
                    String url = HOST + "teleop/5/" + leftSpeed + "/" + rightSpeed;
                    call(url, Collections.emptyMap(), new Callback() {
                        @Override
                        public void onFailure(@NotNull Call call, @NotNull IOException e) {
                            Log.e(TAG, "onFailure: ", e);
                        }

                        @Override
                        public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                            ResponseBody body = response.body();
                            if (body != null) {
                                Log.i(TAG, "onResponse: " + body.string());
                            }
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "run: Action 阻塞队列出错。", e);
            }
        }
    }

    private Double[] mapSpeedMode3(double hp, double vp) {
        double baseSpeed = 1D;
        double x = BigDecimalUtils.round(hp, 2);
        double y = BigDecimalUtils.round(vp, 2);

        Double[] speeds = new Double[2];
        double leftWheel;
        double rightWheel;

        if (y > 0.1) { // 前进 y > 0
            if (x > 0) { // 右转，左轮速度 > 右轮速度
                rightWheel = baseSpeed;
                leftWheel = BigDecimalUtils.add(baseSpeed, x);
            } else if (x < 0) { // 左转，右轮速度 > 左轮速度
                leftWheel = baseSpeed;
                rightWheel = BigDecimalUtils.add(baseSpeed, Math.abs(x));
            } else { // 向前直行
                leftWheel = baseSpeed;
                rightWheel = baseSpeed;
            }
        } else if (y < -0.1) { // 后退 y < 0
            if (x > 0) { // 右转
                rightWheel = -baseSpeed;
                leftWheel = BigDecimalUtils.add(-baseSpeed, -x);
            } else if (x < 0) { // 左转
                leftWheel = -baseSpeed;
                rightWheel = BigDecimalUtils.add(-baseSpeed, x);
            } else { // 向后直行
                leftWheel = -baseSpeed;
                rightWheel = -baseSpeed;
            }
        } else { // 停下 -0.1 <= y <= 0.1
            if (x > 0) { // 90度右转
                leftWheel = baseSpeed;
                rightWheel = -baseSpeed;
            } else if (x < 0) { // 90度左转
                leftWheel = -baseSpeed;
                rightWheel = baseSpeed;
            } else { // 停止
                leftWheel = 0;
                rightWheel = 0;
            }
        }

        speeds[0] = leftWheel;
        speeds[1] = rightWheel;

        return speeds;
    }

    /**
     * 因为jetbot没有转向系统，所以转向需要通过速度来映射。
     * 不同的角度映射到左右轮子不同的速度上。
     *
     * @param hp X轴角度百分比
     * @param vp Y轴角度百分比
     */
    private Double[] mapSpeed(double hp, double vp) {
        // 进行四舍五入
        hp = BigDecimalUtils.round(hp, 2);
        vp = BigDecimalUtils.round(vp, 2);

        Double[] speeds = new Double[2];

        if (hp > 0 && vp > 0) { // 第一象限，右转
            if (hp <= vp) {
                double diff = BigDecimalUtils.subtract(vp, hp);
                speeds[0] = BigDecimalUtils.add(hp, diff);
                speeds[1] = diff;
            } else {
                double diff = BigDecimalUtils.subtract(hp, vp);
                speeds[0] = BigDecimalUtils.add(vp, diff);
                speeds[1] = diff;
            }
            Log.d(TAG, "mapSpeed: 右转");
        } else if (hp == 0 && vp > 0) { // 正Y轴，向前直行
            speeds[0] = vp;
            speeds[1] = vp;
            Log.d(TAG, "mapSpeed: 向前直行");
        } else if (hp < 0 && vp > 0) { // 第二象限，左转
            double absHp = Math.abs(hp);
            if (absHp <= vp) {
                double diff = BigDecimalUtils.subtract(vp, absHp);
                speeds[0] = diff;
                speeds[1] = BigDecimalUtils.add(absHp, diff);
            } else {
                double diff = BigDecimalUtils.subtract(absHp, vp);
                speeds[0] = diff;
                speeds[1] = BigDecimalUtils.add(vp, diff);
            }
            Log.d(TAG, "mapSpeed: 左转");
        } else if (hp < 0 && vp == 0) { // 负X轴，向左直行
            speeds[0] = -hp;
            speeds[1] = -hp;
            Log.d(TAG, "mapSpeed: 向左直行");
        } else if (hp < 0 && vp < 0) { // 第三象限
            if (hp >= vp) {
                double diff = BigDecimalUtils.subtract(hp, vp); // >= 0
                speeds[0] = BigDecimalUtils.add(-hp, diff);
                speeds[1] = diff;
            } else {
                double diff = BigDecimalUtils.subtract(vp, hp); // > 0
                speeds[0] = BigDecimalUtils.add(-vp, diff);
                speeds[1] = diff;
            }
            Log.d(TAG, "mapSpeed: 左后转");
        } else if (hp == 0 && vp < 0) { // 负Y轴，向后倒退
            speeds[0] = vp;
            speeds[1] = vp;
            Log.d(TAG, "mapSpeed: 向后倒退");
        } else if (hp > 0 && vp < 0) { // 第四象限
            double absVp = Math.abs(vp);
            if (hp >= absVp) {
                double diff = BigDecimalUtils.subtract(hp, absVp);
                speeds[0] = BigDecimalUtils.add(absVp, diff);
                speeds[1] = diff;
            } else {
                double diff = BigDecimalUtils.subtract(absVp, hp);
                speeds[0] = BigDecimalUtils.add(hp, diff);
                speeds[1] = diff;
            }
            Log.d(TAG, "mapSpeed: 右后转");
        } else if (hp > 0 && vp == 0) { // 正X轴，向右直行
            speeds[0] = hp;
            speeds[1] = hp;
            Log.d(TAG, "mapSpeed: 向右直行");
        }
        return speeds;
    }

    /**
     * 因为jetbot没有转向系统，所以转向需要通过速度来映射。
     * 不同的角度映射到左右轮子不同的速度上。上下是前进后退，左右数值的大小，代表转弯的角度。
     *
     * @param hp X轴角度百分比
     * @param vp Y轴角度百分比
     */
    private Double[] mapSpeedMode2(double hp, double vp) {
        double x = BigDecimalUtils.round(hp, 2);
        double y = BigDecimalUtils.round(vp, 2);

        Double[] speeds = new Double[2];
        double leftWheel;
        double rightWheel;

        if (y > 0) { // 前进
            if (x > 0) { // 右转，左轮速度 > 右轮速度
                if (y >= x) {
                    leftWheel = y;
                    rightWheel = x;
                } else {
                    leftWheel = x;
                    rightWheel = y;
                }
            } else if (x < 0) { // 左转，右轮速度 > 左轮速度
                double absX = Math.abs(x);
                if (y >= absX) {
                    leftWheel = absX;
                    rightWheel = y;
                } else {
                    leftWheel = y;
                    rightWheel = absX;
                }
            } else { // 向前直行
                leftWheel = y;
                rightWheel = y;
            }
            // 速度太小，有时候轮子不转
//            if (leftWheel < 0.5 || rightWheel < 0.5) {
//                leftWheel = BigDecimalUtils.add(leftWheel, 0.5);
//                rightWheel = BigDecimalUtils.add(rightWheel, 0.5);
//            }
        } else if (y < 0) { // 后退
            double absY = Math.abs(y);
            if (x > 0) { // 右转
                if (absY >= x) {
                    leftWheel = y;
                    rightWheel = -x;
                } else {
                    leftWheel = -x;
                    rightWheel = y;
                }
            } else if (x < 0) { // 左转
                double absX = Math.abs(x);
                if (absY >= absX) {
                    leftWheel = x;
                    rightWheel = y;
                } else {
                    leftWheel = y;
                    rightWheel = x;
                }
            } else { // 向后直行
                leftWheel = y;
                rightWheel = y;
            }
//            if (leftWheel > -0.5 || rightWheel > -0.5) {
//                leftWheel = BigDecimalUtils.add(leftWheel, -0.5);
//                rightWheel = BigDecimalUtils.add(rightWheel, -0.5);
//            }
        } else { // 停下
            if (x > 0) { // 90度右转
                leftWheel = 1;
                rightWheel = -1;
            } else if (x < 0) { // 90度左转
                leftWheel = -1;
                rightWheel = 1;
            } else { // 停止
                leftWheel = 0;
                rightWheel = 0;
            }
        }

        speeds[0] = leftWheel;
        speeds[1] = rightWheel;

        return speeds;
    }

    private static View getRootView(Context context) {
        Activity activity = (Activity) context;
        return activity.getWindow().getDecorView().findViewById(android.R.id.content);
    }

    /**
     * OkHttpClient 封装了参数设置
     * @param url url
     * @param params 参数
     * @param callback OkHttpClient Call对象 回调
     */
    protected void call(String url, Map<String, Object> params, Callback callback) {

        Log.i(TAG, "okhttp call, url=[" + url + "]");
        OkHttpClient okHttpClient = mOkHttpClient; // 如果不是单例，华为手机很容易堆栈溢出
        MultipartBody.Builder builder = new MultipartBody.Builder("asdfSKMBW123456VTEFLF98765dd").setType(MultipartBody.FORM);

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof List) {
                List listValue = (List) value;
                for (Object obj : listValue) {
                    if (obj instanceof File) {
                        File file = (File) obj;
                        RequestBody body = RequestBody.create(MediaType.parse("application/octet-stream"), file);
                        builder.addFormDataPart(name, file.getName(), body);
                    } else {
                        builder.addFormDataPart(name, obj.toString());
                    }
                }
            }
        }

        Log.i(TAG, "BasicActivity call: okhttp create parameter finished.");

        // Multipart body must have at least one part
        // 如果一个参数都没有就不能添加body了，防止没有登录的情况
        Request.Builder requestBuilder = new Request.Builder();

        requestBuilder.url(url);
        if (!params.isEmpty()) {
            requestBuilder.post(builder.build());
        }
        Request request = requestBuilder.build();

        Call call = okHttpClient.newCall(request);
        call.enqueue(callback);
    }
}
