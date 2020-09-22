package com.xuershangda.joystick;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.xuershangda.joystick.controller.DefaultController;
import com.xuershangda.joystick.listener.JoystickTouchViewListener;
import com.xuershangda.joystick.utils.BigDecimalUtils;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.json.Json;
import javax.json.JsonObject;

import edu.wpi.rail.jrosbridge.JRosbridge;
import edu.wpi.rail.jrosbridge.messages.geometry.Twist;
import edu.wpi.rail.jrosbridge.messages.geometry.Vector3;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String HOST = "http://10.1.163.96:9090/";
    private OkHttpClient mOkHttpClient;
    private Double mLeftSpeed = 0D;
    private Double mRightSpeed = 0D;
    private BlockingDeque<Double[]> mBlockingDeque;
    private TextView mTextView;
    private TextView mBaseSpeedView;
    private TextView mDirectView;
    private TextView mDrivingMode;
    private double baseSpeed = 0.5D;
    private volatile AtomicInteger drivingMode = new AtomicInteger(0);
    private TextView mLeftWheel;
    private TextView mRightWheel;
    private Handler mHandler;

    private SocketChannel mSocketChannel;
    private Selector mSelector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        mOkHttpClient = new OkHttpClient();
        mBlockingDeque = new LinkedBlockingDeque<>();
        mTextView = findViewById(R.id.sway);
        mBaseSpeedView = findViewById(R.id.baseSpeed);
        mDirectView = findViewById(R.id.direction);
        Button speedUp = findViewById(R.id.speedUp);
        Button speedDown = findViewById(R.id.speedDown);
        mDrivingMode = findViewById(R.id.drivingMode);
        mLeftWheel = findViewById(R.id.leftWheel);
        mRightWheel = findViewById(R.id.rightWheel);

        speedUp.setOnClickListener(view -> baseSpeed = BigDecimalUtils.add(baseSpeed, 0.1));
        speedDown.setOnClickListener(view -> baseSpeed = BigDecimalUtils.subtract(baseSpeed, 0.1));

        mHandler = new UpdateViewHandler(this);

        // 不能放在上面，因为view还没有初始化，肯定找不到这个布局
        RelativeLayout viewGroup = findViewById(R.id.joyStickView);

        DefaultController defaultController = new DefaultController(MainActivity.this, viewGroup);
        defaultController.createViews();
        defaultController.showViews(false);

        defaultController.setRightTouchViewListener(new JoystickTouchViewListener() {
            @Override
            public void onTouch(float horizontalPercent, float verticalPercent) {
                Log.d(TAG, "onTouch right: " + horizontalPercent + ", " + verticalPercent);
                Double[] speeds = mapSpeedMode(horizontalPercent, verticalPercent);
                Double leftSpeed = speeds[0];
                Double rightSpeed = speeds[1];

                if (Math.abs(BigDecimalUtils.subtract(leftSpeed, mLeftSpeed)) <= 0.02
                        && Math.abs(BigDecimalUtils.subtract(rightSpeed, mRightSpeed)) <= 0.02) {
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
                mBlockingDeque.clear(); // 清空队列中的指令，没有发送完全的都不需要了

                Message msg = mHandler.obtainMessage(1);
                mHandler.sendMessage(msg);

                // 发送最后一个指令，停止运动
                ByteBuffer byteBuffer = createMessageContent(0);

                try {
                    mSocketChannel.register(mSelector, SelectionKey.OP_WRITE, byteBuffer);
                } catch (ClosedChannelException e) {
                    Log.e(TAG, "onReset: mSocketChannel 已关闭.", e);
                }
            }

            @Override
            public void onActionDown() {
                Log.d(TAG, "onActionDown: right");
            }

            @Override
            public void onActionUp() {
                Log.d(TAG, "onActionUp: right, stop.");
            }
        });

        defaultController.setLeftTouchViewListener(new JoystickTouchViewListener() {
            @Override
            public void onTouch(float horizontalPercent, float verticalPercent) {
                Log.d(TAG, "onTouch left: " + horizontalPercent + ", " + verticalPercent);
                speedUp(horizontalPercent, verticalPercent);
            }

            @Override
            public void onReset() {
                // 回到控制点，停止
                Log.d(TAG, "onReset: left, driving mode = " + drivingMode.toString());
                // 重置显示信息
                drivingMode.set(0);
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

        Thread thread = new Action();
        thread.start();
    }

    public ByteBuffer createMessageContent(int speed) {
        // 发送最后一个指令，停止运动
        Vector3 angular = new Vector3(0, 0, 0);
        Vector3 linear = new Vector3(speed, 0, 0);
        Twist twist = new Twist(linear, angular);
        twist.setMessageType(Twist.TYPE);

        // 事实上，现在是发送socket数据，不使用原来的协议也是OK的，现在仍然使用，兼容以后
        // System.currentTimeMillis()精度可能不够，但是现在没有使用这个字段
        String publishId = "publish_chatter_" + System.currentTimeMillis();
        JsonObject call = Json.createObjectBuilder()
                .add(JRosbridge.FIELD_OP, JRosbridge.OP_CODE_PUBLISH)
                .add(JRosbridge.FIELD_ID, publishId)
                .add(JRosbridge.FIELD_TOPIC, "chatter_forward")
                .add(JRosbridge.FIELD_MESSAGE, twist.toJsonObject()).build();

        byte[] messageBytes = call.toString().getBytes();
        int contentLength = messageBytes.length;
        StringBuilder headerLengthString = new StringBuilder(String.valueOf(contentLength));
        int strLen = headerLengthString.length();
        // python无法直接接收int转成的byte[],现在使用string代替，多占了6个字节而已
        if (strLen < 10) {
            for (int i = strLen; i < 10; i++) {
                headerLengthString.append("_");
            }
        }

        byte[] headerBytes = headerLengthString.toString().getBytes();
        byte[] contentBytes = new byte[contentLength + 10];
        System.arraycopy(headerBytes, 0, contentBytes, 0, 10);
        System.arraycopy(messageBytes, 0, contentBytes, 10, contentLength);

        return ByteBuffer.wrap(contentBytes);
    }

    public void startRosService() throws IOException {
        try {
            // 初始化客户端
            mSocketChannel = SocketChannel.open();
            mSocketChannel.configureBlocking(false);
            mSelector = Selector.open();
            // 注册连接事件
            mSocketChannel.register(mSelector, SelectionKey.OP_CONNECT);
            // 发起连接
            mSocketChannel.connect(new InetSocketAddress("localhost", 9090));
            // 轮询处理
            while (true) {
                if (mSocketChannel.isOpen()) {
                    // 在注册的键中选择已准备就绪的事件
                    mSelector.select();
                    try {
                        // 控制发送的节奏
                        TimeUnit.MILLISECONDS.sleep(100);
                    } catch (InterruptedException e) {
                        Log.d(TAG, "startRosService: 控制发送节奏，线程中断。");
                    }
                    // 已选择键集
                    Set<SelectionKey> keys = mSelector.selectedKeys();
                    Iterator<SelectionKey> iterator = keys.iterator();
                    // 处理准备就绪的事件
                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        // 删除当前键，避免重复消费
                        iterator.remove();
                        // 连接
                        if (key.isConnectable()) {
                            // 在非阻塞模式下connect也是非阻塞的，所以要确保连接已经建立完成
                            while (!mSocketChannel.finishConnect()) {
                                System.out.println("连接中");
                            }
                            // 连接成功，注册写事件
                            mSocketChannel.register(mSelector, SelectionKey.OP_WRITE);
                        }

                        if (key.isWritable()) { // 通道可写入数据了
                            mSocketChannel.write((ByteBuffer) key.attachment());

//                            Vector3 angular = new Vector3(0, 0, 0);
//                            Vector3 linear = new Vector3(1, 0, 0);
//                            Twist twist = new Twist(linear, angular);
//                            twist.setMessageType(Twist.TYPE);
//
//                            // 事实上，现在是发送socket数据，不使用原来的协议也是OK的，现在仍然使用，兼容以后
//                            // System.currentTimeMillis()精度可能不够，但是现在没有使用这个字段
//                            String publishId = "publish_chatter_" + System.currentTimeMillis();
//                            JsonObject call = Json.createObjectBuilder()
//                                    .add(JRosbridge.FIELD_OP, JRosbridge.OP_CODE_PUBLISH)
//                                    .add(JRosbridge.FIELD_ID, publishId)
//                                    .add(JRosbridge.FIELD_TOPIC, "chatter_forward")
//                                    .add(JRosbridge.FIELD_MESSAGE, twist.toJsonObject()).build();
//
//                            byte[] messageBytes = call.toString().getBytes();
//                            int contentLength = messageBytes.length;
//                            StringBuilder headerLengthString = new StringBuilder(String.valueOf(contentLength));
//                            int strLen = headerLengthString.length();
//                            // python无法直接接收int转成的byte[],现在使用string代替，多占了6个字节而已
//                            if (strLen < 10) {
//                                for (int i = strLen; i < 10; i++) {
//                                    headerLengthString.append("_");
//                                }
//                            }
//
//                            byte[] headerBytes = headerLengthString.toString().getBytes();
//                            byte[] contentBytes = new byte[contentLength + 10];
//                            System.arraycopy(headerBytes, 0, contentBytes, 0, 10);
//                            System.arraycopy(messageBytes, 0, contentBytes, 10, contentLength);
//
//                            ByteBuffer byteBuffer = ByteBuffer.wrap(contentBytes);
//                            mSocketChannel.write(byteBuffer);
//                            mSocketChannel.register(mSelector, SelectionKey.OP_WRITE);
                        }
                        // 处理输入事件，服务端的返回数据，事实上，不需要处理，因为不与server交互
                        if (key.isReadable()) {
//                            ByteBuffer byteBuffer = ByteBuffer.allocate(1024 * 4);
//                            int len = 0;
//                            // 捕获异常，因为在服务端关闭后会发送FIN报文，会触发read事件，但连接已关闭,此时read()会产生异常
//                            try {
//                                if ((len = mSocketChannel.read(byteBuffer)) > 0) {
//                                    System.out.println("接收到來自服务器的消息\t");
//                                    System.out.println(new String(byteBuffer.array(), 0, len));
//                                }
//                            } catch (IOException e) {
//                                System.out.println("服务器异常，请联系客服人员!正在关闭客户端.........");
//                                key.cancel();
//                                mSocketChannel.close();
//                            }
                        }
                    }
                } else {
                    break;
                }
            }

        } catch (IOException e) {
            Log.e(TAG, "server error. " + e.getMessage());
        }
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
                    double diff = BigDecimalUtils.subtract(leftSpeed, rightSpeed);
                    String driving = "手动控制";
                    if (drivingMode.compareAndSet(1, 1)) {
                        Log.d(TAG, "run: DrivingMode = " + drivingMode);
                        if (leftSpeed >= 0 && rightSpeed >= 0) {
                            driving = "锁定直行";
                            rightSpeed = Math.max(leftSpeed, rightSpeed);
                            leftSpeed = rightSpeed;
                        } else if (leftSpeed <= 0 && rightSpeed <= 0) {
                            driving = "锁定后退";
                            rightSpeed = Math.min(leftSpeed, rightSpeed);
                            leftSpeed = rightSpeed;
                        }
                    } else if (drivingMode.compareAndSet(2, 2)) {
                        Log.d(TAG, "run: DrivingMode = " + drivingMode);
                        if (leftSpeed <= 0 && rightSpeed <= 0) {
                            driving = "锁定后退";
                            rightSpeed = Math.min(leftSpeed, rightSpeed);
                            leftSpeed = rightSpeed;
                        } else {
                            if (leftSpeed >= 0 && rightSpeed >= 0) {
                                driving = "锁定直行";
                                rightSpeed = Math.max(leftSpeed, rightSpeed);
                                leftSpeed = rightSpeed;
                            }
                        }
                    } else {
                        Log.d(TAG, "run: DrivingMode = " + drivingMode);
                        driving = "手动控制";
                    }
                    String dm = driving;
                    // 记录上一次的速度
                    mLeftSpeed = leftSpeed;
                    mRightSpeed = rightSpeed;
                    runOnUiThread(() -> {
                        mTextView.setText(String.format("%s%s", getString(R.string.sway), diff));
                        mBaseSpeedView.setText(String.format("%s%s", getString(R.string.baseSpeed), baseSpeed));
                        mDrivingMode.setText(String.format("%s%s", getString(R.string.drivingMode), dm));
                        mLeftWheel.setText(String.format("%s%s", getString(R.string.leftWheel), mLeftSpeed));
                        mRightWheel.setText(String.format("%s%s", getString(R.string.rightWheel), mRightSpeed));
                    });
//                    String url = HOST + "teleop/5/" + leftSpeed + "/" + rightSpeed;
//                    call(url, Collections.emptyMap(), new Callback() {
//                        @Override
//                        public void onFailure(@NotNull Call call, @NotNull IOException e) {
//                            Log.e(TAG, "onFailure: ", e);
//                        }
//
//                        @Override
//                        public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
//                            ResponseBody body = response.body();
//                            if (body != null) {
//                                Log.i(TAG, "onResponse: " + body.string());
//                            }
//                        }
//                    });

//                    Vector3 angular = new Vector3(0, 0, 0);
//                    Vector3 linear = new Vector3(1, 0, 0);
//                    Twist twist = new Twist(linear, angular);
//                    twist.setMessageType(Twist.TYPE);
//
//                    // 事实上，现在是发送socket数据，不使用原来的协议也是OK的，现在仍然使用，兼容以后
//                    // System.currentTimeMillis()精度可能不够，但是现在没有使用这个字段
//                    String publishId = "publish_chatter_" + System.currentTimeMillis();
//                    JsonObject call = Json.createObjectBuilder()
//                            .add(JRosbridge.FIELD_OP, JRosbridge.OP_CODE_PUBLISH)
//                            .add(JRosbridge.FIELD_ID, publishId)
//                            .add(JRosbridge.FIELD_TOPIC, "chatter_forward")
//                            .add(JRosbridge.FIELD_MESSAGE, twist.toJsonObject()).build();
//
//                    byte[] messageBytes = call.toString().getBytes();
//                    int contentLength = messageBytes.length;
//                    StringBuilder headerLengthString = new StringBuilder(String.valueOf(contentLength));
//                    int strLen = headerLengthString.length();
//                    // python无法直接接收int转成的byte[],现在使用string代替，多占了6个字节而已
//                    if (strLen < 10) {
//                        for (int i = strLen; i < 10; i++) {
//                            headerLengthString.append("_");
//                        }
//                    }
//
//                    byte[] headerBytes = headerLengthString.toString().getBytes();
//                    byte[] contentBytes = new byte[contentLength + 10];
//                    System.arraycopy(headerBytes, 0, contentBytes, 0, 10);
//                    System.arraycopy(messageBytes, 0, contentBytes, 10, contentLength);

                    ByteBuffer byteBuffer = createMessageContent(1);

                    mSocketChannel.register(mSelector, SelectionKey.OP_WRITE, byteBuffer);
                }
            } catch (Exception e) {
                Log.e(TAG, "run: Action 阻塞队列出错。", e);
            }
        }
    }

    private void speedUp(double hp, double vp) {
        double y = BigDecimalUtils.round(vp, 2);

        if (y > 0) {
            drivingMode.set(1);
        } else {
            drivingMode.set(2);
        }
    }

    private Double[] mapSpeedMode(double hp, double vp) {
        double x = BigDecimalUtils.round(hp, 2);
        double y = BigDecimalUtils.round(vp, 2);

        Double[] speeds = new Double[2];
        double leftWheel;
        double rightWheel;

        String direct;

        if (y > 0) { // 前进 y > 0; y > 0.1
            if (x > 0) { // 右转，左轮速度 > 右轮速度
                rightWheel = baseSpeed;
                leftWheel = BigDecimalUtils.add(baseSpeed, x);
                direct = "右转";
            } else if (x < 0) { // 左转，右轮速度 > 左轮速度
                leftWheel = baseSpeed;
                rightWheel = BigDecimalUtils.add(baseSpeed, Math.abs(x));
                direct = "左转";
            } else { // 向前直行
                leftWheel = baseSpeed;
                rightWheel = baseSpeed;
                direct = "前进";
            }
        } else if (y < 0) { // 后退 y < 0
            if (x > 0) { // 右转
                rightWheel = -baseSpeed;
                leftWheel = BigDecimalUtils.add(-baseSpeed, -x);
                direct = "右后转";
            } else if (x < 0) { // 左转
                leftWheel = -baseSpeed;
                rightWheel = BigDecimalUtils.add(-baseSpeed, x);
                direct = "左后转";
            } else { // 向后直行
                leftWheel = -baseSpeed;
                rightWheel = -baseSpeed;
                direct = "后退";
            }
        } else {
            leftWheel = 0;
            rightWheel = 0;
            direct = "停止";
        }
//        else { // 停下 -0.1 <= y <= 0.1
//            if (x > 0) { // 90度右转
//                leftWheel = baseSpeed;
//                rightWheel = -baseSpeed;
//                direct = "90度右转";
//            } else if (x < 0) { // 90度左转
//                leftWheel = -baseSpeed;
//                rightWheel = baseSpeed;
//                direct = "90度左转";
//            } else { // 停止
//                leftWheel = 0;
//                rightWheel = 0;
//                direct = "停止";
//            }
//        }

        runOnUiThread(() -> mDirectView.setText(String.format("%s%s", getString(R.string.direction), direct)));

        speeds[0] = leftWheel;
        // 右轮速度快，两个轮子的速度不同步
        speeds[1] = rightWheel;

        return speeds;
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

    private static class UpdateViewHandler extends Handler {

        private WeakReference<MainActivity> mReference;

        UpdateViewHandler(MainActivity activity) {
            this.mReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    MainActivity activity = mReference.get();
                    activity.mTextView.setText(String.format("%s%s", activity.getString(R.string.sway), 0));
                    activity.mDirectView.setText(String.format("%s%s", activity.getString(R.string.direction), "停止"));
                    activity.mBaseSpeedView.setText(String.format("%s%s", activity.getString(R.string.baseSpeed), activity.baseSpeed));
                    activity.mDrivingMode.setText(String.format("%s%s", activity.getString(R.string.drivingMode), "手动控制"));
                    activity.mLeftWheel.setText(String.format("%s%s", activity.getString(R.string.leftWheel), 0));
                    activity.mRightWheel.setText(String.format("%s%s", activity.getString(R.string.rightWheel), 0));
            }
        }
    }
}
