package com.xuershangda.joystick.model;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * 速度指令，携带该指令的时间，超时的指令抛弃（结合延时队列）。
 */
public class SpeedModel implements Delayed {
    /**
     * 该条指令发出的时间，或者是未来的某一时刻的时间
     */
    private long time;
    /**
     * 线速度
     */
    private double linear;
    /**
     * 角速度
     */
    private double angular;

    public String getDirect() {
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

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public double getLinear() {
        return linear;
    }

    public void setLinear(double linear) {
        this.linear = linear;
    }

    public double getAngular() {
        return angular;
    }

    public void setAngular(double angular) {
        this.angular = angular;
    }

    /**
     * 返回0或者负值，表示对象已经到期了
     *
     * @param unit TimeUnit.NANOSECONDS
     * @return 延迟是否到期
     */
    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(time - System.nanoTime(), TimeUnit.NANOSECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        if (o instanceof SpeedModel) {
            SpeedModel model = (SpeedModel) o;
            long delayedTime = model.getTime();
            if (delayedTime < this.time) {
                return 1;
            } else if (delayedTime == time) {
                return 0;
            } else {
                return -1;
            }
        }
        return 0;
    }

}
