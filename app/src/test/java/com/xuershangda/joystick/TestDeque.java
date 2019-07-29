package com.xuershangda.joystick;

import com.xuershangda.joystick.utils.BigDecimalUtils;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @author yinlei
 * @since 2019-7-24 14:58
 */
public class TestDeque {
    public static void main(String[] args) throws Exception {

        double dd = BigDecimalUtils.add(-0.3, -0.5);

        BlockingDeque<String> blockingDeque = new LinkedBlockingDeque<>();

        blockingDeque.put("1");
        blockingDeque.put("2");
        blockingDeque.put("3");

        System.out.println(blockingDeque.size());
        String d1 = blockingDeque.take();
        System.out.println(blockingDeque.size());
        String d2 = blockingDeque.take();
        System.out.println(blockingDeque.size());
        String d3 = blockingDeque.take();
        System.out.println(blockingDeque.size());

        blockingDeque.put("4");
        blockingDeque.put("5");

        System.out.println(blockingDeque.size());
        blockingDeque.clear();
        String d4 = blockingDeque.take();
        System.out.println(blockingDeque.size());
    }
}
