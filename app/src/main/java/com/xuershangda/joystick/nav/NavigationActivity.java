package com.xuershangda.joystick.nav;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xuershangda.joystick.R;
import com.xuershangda.joystick.listener.FingerTouchViewListener;
import com.xuershangda.joystick.utils.BigDecimalUtils;
import com.xuershangda.joystick.view.FingerPaintImageView;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static com.xuershangda.joystick.nav.Consts.API_GET_MAP;
import static com.xuershangda.joystick.nav.Consts.API_GET_POS;
import static com.xuershangda.joystick.nav.Consts.API_SET_GOAL;
import static com.xuershangda.joystick.nav.Consts.API_SET_POS;
import static com.xuershangda.joystick.nav.Consts.HOST;

public class NavigationActivity extends AppCompatActivity {
    public static final String BOUNDARY = "--tda67ajd9km3zs05dha991piq90cm0bf43vd--";
    private static final String TAG = "NavigationActivity";
    private FingerPaintImageView mFingerPaintImageView;
    private volatile AtomicBoolean startFlag = new AtomicBoolean(false);
    private volatile AtomicBoolean endFlag = new AtomicBoolean(false);
    private float startX;
    private float startY;
    private float endX;
    private float endY;

    private float goalStartX;
    private float goalStartY;
    private float goalEndX;
    private float goalEndY;

    private double screenWidth;
    private double mapWidth;
    private double mapHeight;
    private double scaleRate;

    private static final String START_POINT = "start_point";
    private static final String START_ORI = "start_ori";
    private static final String END_POINT = "end_point";
    private static final String END_ORI = "end_ori";
    private static final String CURRENT_POINT = "current_point";

    private ScheduledExecutorService scheduledExecutorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.navigation);

        setSupportActionBar(toolbar);
        mFingerPaintImageView = findViewById(R.id.finger);

        this.screenWidth = getScreenWidth();

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
                if (startFlag.get()) {
                    startX = x;
                    startY = y;
                    mFingerPaintImageView.setStrokeBlue(Color.BLUE);
                }
                if (endFlag.get()) {
                    goalStartX = x;
                    goalStartY = y;
                }
            }

            @Override
            public void onActionUp(float x, float y) {
                Log.d(TAG, "onActionUp: x=" + x + ", y=" + y);
                // 设置起点，以抬起，结束为准
                if (startFlag.get()) {
                    endX = x;
                    endY = y;
                    mFingerPaintImageView.clear();
                    mFingerPaintImageView.drawPoint(startX, startY, START_POINT);
                    mFingerPaintImageView.drawLine(startX, startY, endX, endY, START_ORI);
                    mFingerPaintImageView.setInEditMode(false);
                    // 获取终点在以起点为原点的坐标系的角度，顺时针
                    float angle = getAngle(startX, startY, endX, endY);
                    startFlag.set(false);
                    setStartPoint(startX, startY, angle);
                }

                if (endFlag.get()) {
                    goalEndX = x;
                    goalEndY = y;
                    mFingerPaintImageView.removeCube();
                    mFingerPaintImageView.drawPoint(goalStartX, goalStartY, END_POINT);
                    mFingerPaintImageView.drawLine(goalStartX, goalStartY, goalEndX, goalEndY, END_ORI);
                    mFingerPaintImageView.setInEditMode(false);
                    float angle = getAngle(goalStartX, goalStartY, goalEndX, goalEndY);
                    endFlag.set(false);
                    setEndPoint(startX, startY, angle);
                }
            }
        });
        // 获取地图图片
        getImage();

        // 轮询获取当前点
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            if (!endFlag.get() && !startFlag.get()) {
                getCurrentPoint();
            }
        }, 2, 3, TimeUnit.SECONDS);

        findViewById(R.id.clear).setOnClickListener(v -> {
            this.mFingerPaintImageView.clear();
            this.startX = 0;
            this.startY = 0;
            this.endX = 0;
            this.endY = 0;
        });

        findViewById(R.id.set_start_position).setOnClickListener(v -> {
            this.mFingerPaintImageView.clear();
            this.startFlag.set(true);
            this.mFingerPaintImageView.setInEditMode(true);
        });

        findViewById(R.id.set_endpoint).setOnClickListener(v -> {
            if (this.startX == 0 || this.endX == 0) {
                Toast.makeText(NavigationActivity.this, "请先标定机器人当前位置，设置起点和方向！", Toast.LENGTH_LONG).show();
                return;
            }
            this.endFlag.set(true);
            this.mFingerPaintImageView.setInEditMode(true);
        });
    }

    private void getImage() {
        String url = HOST + API_GET_MAP;

        call(url, Collections.emptyMap(), new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e(TAG, "onFailure: 获取地图图片错误。", e);
                runOnUiThread(() -> {
                    Toast.makeText(NavigationActivity.this, "获取地图图片错误。", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                ResponseBody body = response.body();
                if (body != null) {
                    InputStream stream = body.byteStream();
                    final Bitmap bitmap = BitmapFactory.decodeStream(stream);
                    mapWidth = bitmap.getWidth();
                    mapHeight = bitmap.getHeight();
                    scaleRate = getScaleRate();
                    runOnUiThread(() -> {
                        ViewGroup.LayoutParams para = mFingerPaintImageView.getLayoutParams();
                        para.height = (int) (mapHeight * scaleRate);
                        para.width = (int) (mapWidth * scaleRate);
                        mFingerPaintImageView.setLayoutParams(para);
                        mFingerPaintImageView.setImageBitmap(bitmap);
                    });
                } else {
                    Log.e(TAG, "onResponse: 获取地图图片响应为空。");
                    runOnUiThread(() -> {
                        Toast.makeText(NavigationActivity.this, "获取地图图片响应为空。", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    public void getCurrentPoint() {
        String url = HOST + API_GET_POS;
        postJson(url, null, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e(TAG, "onFailure: 获取位置错误。", e);
                runOnUiThread(() -> {
                    Toast.makeText(NavigationActivity.this, "获取位置错误。", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                ResponseBody body = response.body();
                if (body != null) {
                    byte[] bytes = body.bytes();
                    JSONObject jsonObject = (JSONObject) JSON.parse(bytes);
                    float pixelX = jsonObject.getFloatValue("piexl_x");
                    float pixelY = jsonObject.getFloatValue("piexl_y");

                    float x = getMapPixelPoint(pixelX);
                    float y = getMapPixelPoint(pixelY);
                    mFingerPaintImageView.drawPoint(x, y, CURRENT_POINT);
                } else {
                    Log.e(TAG, "onResponse: 获取位置响应为空。");
                    runOnUiThread(() -> {
                        Toast.makeText(NavigationActivity.this, "获取位置响应为空。", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    /**
     * 获取以原点为坐标系的，顺时针旋转的角度值
     *
     * @param zeroX 原点x坐标
     * @param zeroY 原点y坐标
     * @param pointX x坐标
     * @param pointY y坐标
     * @return 角度值
     */
    private float getAngle(float zeroX, float zeroY, float pointX, float pointY) {
        int quadrant = getQuadrant(zeroX, zeroY, pointX, pointY);
        Log.i(TAG, "getAngle: quadrant=" + quadrant);
        switch (quadrant) {
            case 1:
                // 对边
                double oppositeSide = pointX - zeroX; // 默认提升精度
                // 邻边
                double adjacentSide = zeroY - pointY;
                return computeAngle(oppositeSide, adjacentSide);
            case 2:
                oppositeSide = zeroY - pointY;
                adjacentSide = zeroX - pointX;
                return computeAngle(oppositeSide, adjacentSide);
            case 3:
                oppositeSide = zeroX - pointX;
                adjacentSide = pointY - zeroY;
                return computeAngle(oppositeSide, adjacentSide);
            case 4:
                oppositeSide = pointY - zeroY;
                adjacentSide = pointX - zeroX;
                return computeAngle(oppositeSide, adjacentSide);
            case 5:
                return 0; // return 就是break的语义
            case 6:
                return 90;
            case 7:
                return 180;
            case 8:
                return 270;
            default:
                return 0;
        }
    }

    /**
     * 根据正切值计算角度
     *
     * @param oppositeSide 对边
     * @param adjacentSide 邻边
     * @return 角度
     */
    private float computeAngle(double oppositeSide, double adjacentSide) {
        double tana = BigDecimalUtils.divide(oppositeSide, adjacentSide);
        double piAngle = Math.atan(tana);
        double angle = BigDecimalUtils.divide(BigDecimalUtils.multiply(piAngle, 180D), Math.PI);
        Log.i(TAG, "computeAngle: angle=" + angle);
        return (float) angle;
    }

    /**
     * 获取点(pointX, pointY)所在的象限
     *
     * @param zeroX 原点X
     * @param zeroY 原点Y
     * @param pointX x point
     * @param pointY y point
     * @return 象限
     */
    private int getQuadrant(float zeroX, float zeroY, float pointX, float pointY) {
        if (pointX > zeroX) { // 在1、4象限
            if (pointY < zeroY) { // 在第一象限
                return 1;
            } else if (pointY > zeroY) { // 第四象限
                return 4;
            } else { // 在Y+轴上
                return 6;
            }
        } else if (pointX < zeroX) { // 在2、3象限
            if (pointY < zeroY) { // 在第2象限
                return 2;
            } else if (pointY > zeroY) { // 在第三象限
                return 3;
            } else { // 在Y-轴上
                return 8;
            }
        } else {
            if (pointY < zeroY) { // 在X+轴上
                return 5;
            } else if (pointY > zeroY) { // 在X-轴上
                return 7;
            } else { // 原点
                return 9;
            }
        }
    }

    /**
     * 设置起点位置
     *
     * @param x x轴坐标
     * @param y y轴坐标
     * @param angle 机器人面向的角度
     */
    public void setStartPoint(float x, float y, float angle) {
        Log.i(TAG, "setStartPoint: 设置起点位置。");
        String url = HOST + API_SET_POS;
        try {
            reportPosition(x, y, angle, url);
        } catch (Exception e) {
            Log.e(TAG, "setStartPoint: error.", e);
        }
    }

    /**
     * 设置终点位置
     *
     * @param x x轴坐标
     * @param y y轴坐标
     * @param angle 机器人面向的角度
     */
    public void setEndPoint(float x, float y, float angle) {
        Log.i(TAG, "setEndPoint: 设置终点位置。");
        String url = HOST + API_SET_GOAL;
        try {
            reportPosition(x, y, angle, url);
        } catch (Exception e) {
            Log.e(TAG, "setEndPoint: error.", e);
        }
    }

    /**
     * 上报，设置机器人的位置
     *
     * @param x x轴坐标
     * @param y y轴坐标
     * @param angle 机器人面向的角度
     * @param url url
     */
    private void reportPosition(float x, float y, float angle, String url) {
        Map<String, Object> params = new HashMap<>();
        params.put("piexl_x", x);
        params.put("piexl_y", y);
        params.put("raw", angle);

        call(url, params, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e(TAG, "onFailure: 上报位置错误。", e);
                runOnUiThread(() -> {
                    Toast.makeText(NavigationActivity.this, "上报位置错误。", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                ResponseBody body = response.body();
                if (body != null) {
//                    InputStream stream = body.byteStream();
                } else {
                    Log.e(TAG, "onResponse: 上报位置响应为空。");
                    runOnUiThread(() -> {
                        Toast.makeText(NavigationActivity.this, "上报位置响应为空。", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    /**
     * OkHttpClient 封装了参数设置
     *
     * @param url      url
     * @param params   参数
     * @param callback OkHttpClient Call对象 回调
     */
    protected void call(String url, Map<String, Object> params, Callback callback) {
        Log.i(TAG, "call: OkHttp call");
        OkHttpClient okHttpClient = new OkHttpClient();
        MultipartBody.Builder builder = new MultipartBody.Builder(BOUNDARY).setType(MultipartBody.FORM);

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof List) {
                @SuppressWarnings("rawtypes")
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
            } else {
                if (value instanceof File) {
                    File file = (File) value;
                    RequestBody body = RequestBody.create(MediaType.parse("application/octet-stream"), file);
                    builder.addFormDataPart(name, file.getName(), body);
                } else {
                    builder.addFormDataPart(name, value.toString());
                }
            }
        }

        Log.i(TAG, "BasicActivity call: okhttp 构建参数完成");

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

    /**
     * OkHttpClient 封装了post json 请求的参数设置
     *
     * @param url      url
     * @param object   参数
     * @param callback OkHttpClient Call对象 回调
     */
    protected void postJson(String url, Object object, Callback callback) {
        Log.i(TAG, "OkHttp post json.");
        OkHttpClient okHttpClient = new OkHttpClient();

        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(url);

        if (object != null) {
            byte[] jsonBody = JSON.toJSONBytes(object);
            RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=UTF-8"), jsonBody);
            requestBuilder.post(body);
        }

        Log.i(TAG, "postJson: OkHttp 构建参数完成");

        Request request = requestBuilder.build();

        Call call = okHttpClient.newCall(request);
        call.enqueue(callback);
    }

    public double getScreenWidth() {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        return dm.widthPixels;
    }

    /**
     * 获得缩放比率，以屏幕的宽度为1，用屏幕宽度（现在是1080）除以地图图片宽度。
     *
     * @return 地图缩放比率
     */
    public double getScaleRate() {
        if (mapWidth != 0) {
            return BigDecimalUtils.divide(screenWidth, mapWidth, 2);
        }
        return 0;
    }

    /**
     * 获取地图的像素点
     *
     * @param xy x轴坐标或y轴坐标
     * @return 屏幕坐标转换为地图像素坐标
     */
    public float getMapPixelPoint(double xy) {
        return (float) BigDecimalUtils.multiply(xy, scaleRate);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.scheduledExecutorService != null && !this.scheduledExecutorService.isShutdown()) {
            this.scheduledExecutorService.shutdown();
        }
    }
}