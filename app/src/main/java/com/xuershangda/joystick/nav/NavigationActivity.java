package com.xuershangda.joystick.nav;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.xuershangda.joystick.R;
import com.xuershangda.joystick.listener.FingerTouchViewListener;
import com.xuershangda.joystick.view.FingerPaintImageView;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

public class NavigationActivity extends AppCompatActivity {
    public static final String BOUNDARY = "--tda67ajd9km3zs05dha991piq90cm0bf43vd--";
    private static final String TAG = "NavigationActivity";
    private FingerPaintImageView mFingerPaintImageView;
    private AtomicBoolean startPosition = new AtomicBoolean(false);
    private AtomicBoolean endPoint = new AtomicBoolean(false);
    private float startX;
    private float startY;
    private float endX;
    private float endY;

    private float endPointStartX;
    private float endPointStartY;
    private float endPointEndX;
    private float endPointEndY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.navigation);

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
                if (startPosition.compareAndSet(false, false)) {
                    startX = x;
                    startY = y;
                }
            }

            @Override
            public void onActionUp(float x, float y) {
                Log.d(TAG, "onActionUp: x=" + x + ", y=" + y);
                // 设置起点，以抬起，结束为准
                if (startPosition.compareAndSet(false, true)) {
                    endX = x;
                    endY = y;
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
//            this.mFingerPaintImageView.clear();
            this.mFingerPaintImageView.setInEditMode(true);
            this.endPoint.set(false);
        });

        findViewById(R.id.set_endpoint).setOnClickListener(v -> {
//            this.mFingerPaintImageView.clear();
            this.mFingerPaintImageView.setInEditMode(true);
            this.startPosition.set(false);
        });

        findViewById(R.id.follow).setOnClickListener(v -> {
            Intent intent = new Intent(this, FollowActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.inspection).setOnClickListener(v -> {
            Intent intent = new Intent(this, InspectionActivity.class);
            startActivity(intent);
        });
    }

    private void getImage() {
        String url = "http://";

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
                    runOnUiThread(() -> {
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
        String url = "";
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
                    InputStream stream = body.byteStream();
                } else {
                    Log.e(TAG, "onResponse: 获取位置响应为空。");
                    runOnUiThread(() -> {
                        Toast.makeText(NavigationActivity.this, "获取位置响应为空。", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    public void setStartPoint() {
        Log.i(TAG, "setStartPoint: 设置起点位置。");
        reportPosition();
    }

    public void setEndPoint() {
        Log.i(TAG, "setEndPoint: 设置终点位置。");
        reportPosition();
    }

    private void reportPosition() {
        String url = "http://";
        Map<String, Float> params = new HashMap<>();
        params.put("x", 23F);
        params.put("y", 33F);
        params.put("angle", 234F);

        postJson(url, params, new Callback() {
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
                    InputStream stream = body.byteStream();
//                    final Bitmap bitmap = BitmapFactory.decodeStream(stream);
//                    runOnUiThread(() -> {
//                        mFingerPaintImageView.setImageBitmap(bitmap);
//                    });
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

    public int getScreenWidth() {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        return dm.widthPixels;
    }
}