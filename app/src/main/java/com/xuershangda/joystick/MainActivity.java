package com.xuershangda.joystick;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.xuershangda.joystick.controller.DefaultController;
import com.xuershangda.joystick.listener.JoystickTouchViewListener;
import com.xuershangda.joystick.nav.Consts;
import com.xuershangda.joystick.utils.BigDecimalUtils;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.NoConnectionPendingException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.json.Json;
import javax.json.JsonObject;

import edu.wpi.rail.jrosbridge.JRosbridge;
import edu.wpi.rail.jrosbridge.messages.geometry.Twist;
import edu.wpi.rail.jrosbridge.messages.geometry.Vector3;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private Double mBaseSpeed = 0.05D;
    private Double mBaseTurn = 0.1D;
    private Double mSpeed = 0D;
    private Double mTurnSpeed = 0D;
    private BlockingDeque<Double[]> mBlockingQueue;
    private TextView mDirectView;
    private TextView mLeftWheel;
    private TextView mRightWheel;
//    private Handler mHandler;
    private StartLoopEventHandler mStartLoopEventHandler;
    private RobotTeleopTask mTeleopTask;

    private ScheduledExecutorService mScheduler;
    private volatile AtomicInteger mStop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        mBlockingQueue = new LinkedBlockingDeque<>();
        mDirectView = findViewById(R.id.direction);
//        Button speedUp = findViewById(R.id.speedUp);
//        Button speedDown = findViewById(R.id.speedDown);
        mLeftWheel = findViewById(R.id.leftWheel);
        mRightWheel = findViewById(R.id.rightWheel);

//        speedUp.setOnClickListener(view -> {
//            mSpeed = BigDecimalUtils.add(mSpeed, 0.1);
//            if (mSpeed > 2D) {
//                mSpeed = 2D;
//            }
//            Log.d(TAG, "speedUp: 加速0.1D. mSpeed=" + mSpeed);
//            mStop.set(1);
//            try {
//                mBlockingQueue.put(new Double[]{mSpeed, mTurnSpeed});
//            } catch (InterruptedException e) {
//                Log.i(TAG, "Speed Up the robot InterruptedException.");
//            }
//        });
//        speedDown.setOnClickListener(view -> {
//            mSpeed = BigDecimalUtils.subtract(mSpeed, 0.1);
//            if (mSpeed < 0) {
//                mSpeed = 0D;
//            }
//            Log.d(TAG, "speedDown: 减速0.1D. mSpeed=" + mSpeed);
//            mStop.set(1);
//            try {
//                mBlockingQueue.put(new Double[]{mSpeed, mTurnSpeed});
//            } catch (InterruptedException e) {
//                Log.i(TAG, "Speed Down the robot InterruptedException.");
//            }
//        });

//        mHandler = new UpdateViewHandler(this);
        mStartLoopEventHandler = new StartLoopEventHandler(this);

        // 启动通信任务
        Thread thread = new Thread(() -> {
            mTeleopTask = new RobotTeleopTask();
            mTeleopTask.startSocketService();
        });
        thread.start();

        mScheduler = Executors.newScheduledThreadPool(3);
        mStop = new AtomicInteger(1);

        // 不能放在上面，因为view还没有初始化，肯定找不到这个布局
        RelativeLayout viewGroup = findViewById(R.id.joyStickView);

        DefaultController defaultController = new DefaultController(MainActivity.this, viewGroup);
        defaultController.createViews();
        defaultController.showViews(false);

        defaultController.setRightTouchViewListener(new JoystickTouchViewListener() {
            @Override
            public void onTouch(float horizontalPercent, float verticalPercent) {
                mStop.set(1);

                // 起步速度太大，连续发送多个指令，不好控制，减少指令的数量
                if (Math.abs(BigDecimalUtils.subtract((double) horizontalPercent, mSpeed)) <= mBaseSpeed
                        && Math.abs(BigDecimalUtils.subtract((double) verticalPercent, mTurnSpeed)) <= mBaseTurn) {
                    Log.d(TAG, "onTouch: 速度变化太小，忽略。");
                    return;
                }

//                Log.d(TAG, "onTouch right: " + horizontalPercent + ", " + verticalPercent);
                Double[] speeds = computeSpeed(horizontalPercent, verticalPercent);
                Double linearSpeed = speeds[0];
                Double angularSpeed = speeds[1];

                try {
//                    Log.d(TAG, "onTouch: mSpeed=" + mSpeed + ", mTurnSpeed="
//                            + mTurnSpeed + ", linearSpeed=" + linearSpeed + ", angularSpeed=" + angularSpeed);
                    mSpeed = linearSpeed;
                    mTurnSpeed = angularSpeed;
//                    Log.d(TAG, "onTouch: 插入控制命令到队列成功。");
                    mBlockingQueue.put(speeds);
//                    Log.d(TAG, "onTouch: task size=[" + mBlockingDeque.size() + "]");
                } catch (Exception e) {
                    Log.e(TAG, "onTouch: 产生任务错误。", e);
                }
            }

            @Override
            public void onReset() {
                // 回到控制点，停止
                Log.d(TAG, "onReset: right. stop.");
//                Log.d(TAG, "onReset: clear BlockingDeque, task size=[" + mBlockingQueue.size() + "]");
//                mBlockingQueue.clear(); // 清空队列中的指令，没有发送完全的都不需要了

                mSpeed = 0D;
                mTurnSpeed = 0D;

                mStop.set(2);
                // 发送最后一个指令，停止运动
                try {
                    mBlockingQueue.put(new Double[]{0D, 0D});
                } catch (InterruptedException e) {
                    Log.i(TAG, "onReset: stop the robot InterruptedException.");
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

//        defaultController.setLeftTouchViewListener(new JoystickTouchViewListener() {
//            @Override
//            public void onTouch(float horizontalPercent, float verticalPercent) {
//                Log.d(TAG, "onTouch left: " + horizontalPercent + ", " + verticalPercent);
//                speedUp(horizontalPercent, verticalPercent);
//            }
//
//            @Override
//            public void onReset() {
//                // 回到控制点，停止
//                Log.d(TAG, "onReset: left, driving mode = " + drivingMode.toString());
//                // 重置显示信息
//                drivingMode.set(0);
//            }
//
//            @Override
//            public void onActionDown() {
//                Log.d(TAG, "onActionDown: left");
//            }
//
//            @Override
//            public void onActionUp() {
//                Log.d(TAG, "onActionUp: left, stop.");
//            }
//        });
    }

    public ByteBuffer createMessageContent(double speed, double turn) {
        // 发送最后一个指令，停止运动
        Vector3 angular = new Vector3(0, 0, turn);
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

    public String getDirect(double linear, double angular) {
        String direct = "直行";
        if (linear >=0) {
            // [-0.1, 0.1]都认为是直行
            if (-0.1D <= angular && angular <=0.1D) {
                direct = "直行";
            } else if (angular > 0.1D) {
                direct = "直行左转";
            } else if (angular < -0.1D) {
                direct = "直行右转";
            }
        } else {
            if (-0.1D <= angular && angular <=0.1D) {
                direct = "后退";
            } else if (angular > 0.1D) {
                direct = "后退左转";
            } else if (angular < -0.1D) {
                direct = "后退右转";
            }
        }
        return direct;
    }

    public class RobotTeleopTask {
        private SocketChannel mSocketChannel;
        private Selector mSelector;

        public RobotTeleopTask() {
        }

        public void startSocketService() {
            // 新启动线程，否则socket会阻塞
            Thread thread = new Thread(this::selectorLoop);
            thread.start();
        }

        private void connect() {
            try {
                // 初始化客户端
                mSocketChannel = SocketChannel.open();
                mSocketChannel.configureBlocking(false);
                mSelector = Selector.open();
                // 注册连接事件
                mSocketChannel.register(mSelector, SelectionKey.OP_CONNECT);
                // 发起连接
//                mSocketChannel.connect(new InetSocketAddress("10.1.163.96", 9090));
                mSocketChannel.connect(new InetSocketAddress(Consts.IP, Consts.TCP_PORT));
            } catch (Exception e) {
                Log.e(TAG, "connect: error", e);
            }
        }

        private void selectorLoop() {
            // socket connect
            connect();
            // 轮询处理所有注册的监听事件
            while (true) {
                try {
                    if (mSocketChannel.isOpen()) {
                        // 在注册的键中选择已准备就绪的事件
                        int i = mSelector.select();
                        if (i <= 0) {
                            continue;
                        }
                        // 获取当前事件集
                        Set<SelectionKey> keySet = mSelector.selectedKeys();
                        Iterator<SelectionKey> iterator = keySet.iterator();
                        // 处理准备就绪的事件
                        while (iterator.hasNext()) {
                            SelectionKey key = iterator.next();

                            // 连接成功后，启动发送消息线程
                            if (key.isConnectable()) {
                                // 在非阻塞模式下connect也是非阻塞的，所以要确保连接已经建立完成
                                while (!mSocketChannel.finishConnect()) {
                                    Log.d(TAG, "SocketChannel finishConnect...");
                                }
                                // OutOfMemoryError: Could not allocate JNI Env
                                // Thread thread = new Thread(this::runLoop);
                                // thread.start();
                                Message msg = mStartLoopEventHandler.obtainMessage(1);
                                msg.sendToTarget();
                            }

//                            // 处理写事件，发送数据到服务端
//                            if (key.isWritable()) {
//                                Log.i(TAG, "startRosService: SocketChannel.write message.");
//                                try {
//                                    ByteBuffer byteBuffer = (ByteBuffer) key.attachment();
//                                    while (byteBuffer.hasRemaining()) {
//                                        mSocketChannel.write(byteBuffer);
//                                    }
//                                    // key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
//                                } catch (NotYetConnectedException e) {
//                                    runOnUiThread(() -> {
//                                        Toast.makeText(MainActivity.this, "网络连接错误。", Toast.LENGTH_SHORT).show();
//                                    });
//                                    connect();
//                                }
//                            }
//                            // 处理读事件，服务端的返回数据，事实上，不需要处理，因为不与server交互
//                            if (key.isReadable()) {
//                                Log.d(TAG, "startRosService: OP_READ 事件不需要处理。");
//                            }
                            // 删除当前键，避免重复消费
                            iterator.remove();
                        }
                    } else {
                        Log.i(TAG, "SocketChannel.isOpen is false.");
                        break;
                    }
                } catch (IOException | NoConnectionPendingException e) {
                    Log.e(TAG, "Server error. " + e.getMessage());
                    connect();
                    Log.e(TAG, "Will reconnect. ");
                }
            }
        }

        public void sendMessageLoop() {
            mScheduler.scheduleAtFixedRate(() -> {
                // 停止状态
                if (mStop.compareAndSet(4, 4)) {
                    return;
                }
                Double[] queueSpeeds = mBlockingQueue.pollLast();
                if (queueSpeeds != null) {
                    mSpeed = queueSpeeds[0];
                    mTurnSpeed = queueSpeeds[1];
                    mBlockingQueue.clear();
                }
                runOnUiThread(() -> {
                    String direct = getDirect(mSpeed, mTurnSpeed);
                    mDirectView.setText(String.format("%s%s", getString(R.string.direction), direct));
                    // 另外的线程，不能立刻看到mSpeed的改变，所以还不是0
                    mLeftWheel.setText(String.format("%s%s", getString(R.string.leftWheel), mSpeed));
                    mRightWheel.setText(String.format("%s%s", getString(R.string.rightWheel), mTurnSpeed));
                });

                ByteBuffer byteBuffer = createMessageContent(mSpeed, mTurnSpeed);
                try {
                    mSocketChannel.write(byteBuffer);
                } catch (IOException | NotYetConnectedException e) {
                    Log.e(TAG, "send message error.", e);
                    connect();
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "网络连接错误。", Toast.LENGTH_SHORT).show();
                    });
                }
                // 停止命令已经发送，等待触摸app手柄，激活
                if (mStop.getAndIncrement() != 1) { // 容错，发送2次停止指令
                    runOnUiThread(() -> {
                        // 现在已经停止，
                        String direct = getDirect(0, 0);
                        mDirectView.setText(String.format("%s%s", getString(R.string.direction), direct));
                        mLeftWheel.setText(String.format("%s%s", getString(R.string.leftWheel), mSpeed));
                        mRightWheel.setText(String.format("%s%s", getString(R.string.rightWheel), mTurnSpeed));
                    });
                }
            }, 0, 500, TimeUnit.MILLISECONDS);
//            for (;;) {
//                try {
//                    Double[] speeds = mBlockingDeque.take();
//
//                    Log.d(TAG, "run: 从队列中获取控制命令成功。taskNumber=[" + mBlockingDeque.size() + "]");
//
//                    Double leftSpeed = speeds[0];
//                    Double rightSpeed = speeds[1];
////                    Log.d(TAG, "onTouch: WheelSpeed=" + leftSpeed + ", AngularSpeed=" + rightSpeed);
//
//                    // 记录上一次的速度
//                    mSpeed = leftSpeed;
//                    mTurnSpeed = rightSpeed;
//                    // 可以换个地方
//                    runOnUiThread(() -> {
//                        mLeftWheel.setText(String.format("%s%s", getString(R.string.leftWheel), leftSpeed));
//                        mRightWheel.setText(String.format("%s%s", getString(R.string.rightWheel), rightSpeed));
//
//                        String direct;
//                        if (rightSpeed > 0) {
//                            direct = "左转";
//                        } else if (rightSpeed < 0) {
//                            direct = "右转";
//                        } else {
//                            direct = "直行";
//                        }
//                        mDirectView.setText(String.format("%s%s", getString(R.string.direction), direct));
//                    });
//
//                    ByteBuffer byteBuffer = createMessageContent(mSpeed, mTurnSpeed);
//                    try {
//                        mSocketChannel.write(byteBuffer);
//                    } catch (IOException | NotYetConnectedException e) {
//                        Log.e(TAG, "send message error.", e);
//                        connect();
//                        runOnUiThread(() -> {
//                            Toast.makeText(MainActivity.this, "网络连接错误。", Toast.LENGTH_SHORT).show();
//                        });
//                    }
//                } catch (InterruptedException e) {
//                    Log.e(TAG, "loop run: 阻塞队列出错。", e);
//                }
//            }
        }
    }

    /**
     * 根据手势，计算速度和方向。返回线速度和角速度。
     * <p>线速度控制速度(0, 2)开区间，负值算减速</p>
     * <p>角速度控制方向[-1, 1]闭区间，0直行，负数右转，正数左转</p>
     *
     * @param hx x轴数值
     * @param vy y轴数值
     * @return 返回线速度和角速度
     */
    private Double[] computeSpeed(double hx, double vy) {
        // 映射到角速度，控制方向，感觉这个值，不需要做转换，非常完美啊
        double x = BigDecimalUtils.round(hx, 2);
        // 映射到线速度，控制速度
        double y = BigDecimalUtils.round(vy, 2);
        Double[] speeds = new Double[2];

        double speed = y; // 速度，负数时后退
        double turn = -x; // 方向，坐标系是相反的
        double speedDiff = BigDecimalUtils.subtract(speed, mSpeed);
        double turnDiff = BigDecimalUtils.subtract(turn, mTurnSpeed);
        if (y > 0) { // 前进
            if (speedDiff > 0D) { // 在加速
                speed = BigDecimalUtils.add(mSpeed, mBaseSpeed); // 增加一个基点的速度
            } else if (speedDiff < 0D) { // 在减速
                speed = BigDecimalUtils.subtract(mSpeed, mBaseSpeed); // 减少一个基点的速度
            }

            if (turnDiff > 0D) { // 从第一象限到第二象限，加速转弯
                turn = BigDecimalUtils.add(mTurnSpeed, mBaseTurn);
            } else if (turnDiff < 0D) { // 从第二象限到第一象限，减速转弯
                turn = BigDecimalUtils.subtract(mTurnSpeed, mBaseTurn);
            }
        } else if (y < 0) { // 后退
            if (speedDiff < 0D) { // 在加速
                speed = BigDecimalUtils.subtract(mSpeed, mBaseSpeed); // 增加一个基点的速度
            } else if (speedDiff > 0D) { // 在减速
                speed = BigDecimalUtils.add(mSpeed, mBaseSpeed); // 减少一个基点的速度
            }

            if (turnDiff > 0D) { // 从第四象限向第三象限转
                turn = BigDecimalUtils.add(mTurnSpeed, mBaseTurn);
            } else if (turnDiff < 0D) { // 从第三象限向第四象限转
                turn = BigDecimalUtils.subtract(mTurnSpeed, mBaseTurn);
            }
        }

        speeds[0] = speed;
        speeds[1] = turn;

        return speeds;
    }

//    private static class SpeedDirectTask implements Runnable {
//
//        @Override
//        public void run() {
//
//        }
//    }

    private static class StartLoopEventHandler extends Handler {
        private volatile boolean init = false;
        private WeakReference<MainActivity> mReference;
        public StartLoopEventHandler(MainActivity activity) {
            this.mReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (!init) {
                        init = true;
                        MainActivity activity = mReference.get();
//                        activity.mScheduler.scheduleAtFixedRate(() -> {
//                        }, 0, 200, TimeUnit.MILLISECONDS);
                        Thread thread = new Thread(() -> {
                            activity.mTeleopTask.sendMessageLoop();
                        });
                        thread.start();
                    }
            }
        }

    }
}
