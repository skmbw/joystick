package com.xuershangda.joystick;

import com.xuershangda.joystick.model.SpeedModel;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

public class SpeedModelTest {
    public static void main(String[] args) throws Exception {
        DelayQueue<SpeedModel> delayQueue = new DelayQueue<>();
        SpeedModel model = new SpeedModel();
        model.setTime(System.currentTimeMillis());
        model.setLinear(1);
        model.setAngular(0.2);
        delayQueue.add(model);

        TimeUnit.MILLISECONDS.sleep(500);

        SpeedModel model1 = new SpeedModel();
        model1.setTime(System.currentTimeMillis());
        model1.setLinear(0.8);
        model1.setAngular(0.3);
        delayQueue.add(model);

        while (true) {
            SpeedModel speedModel = delayQueue.take();
            System.out.println(speedModel.getTime());
        }
    }
}
