package com.xuershangda.joystick.listener;

public interface FingerTouchViewListener {
    void onTouch(float x, float y);

    void onReset();

    /**
     * 按下
     */
    void onActionDown(float x, float y);

    /**
     * 手指抬起
     */
    void onActionUp(float x, float y);
}
