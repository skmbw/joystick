package com.xuershangda.joystick.controller;

import android.content.Context;
import android.view.View;
import android.widget.RelativeLayout;

/**
 * @author yinlei
 * @since 2019-7-29 15:46
 */
public class SingleController extends DefaultController {
    public SingleController(Context context, RelativeLayout containerView) {
        super(context, containerView);
    }

    @Override
    public void createViews() {
        createRightControlTouchView();
        containerView.addView(rightControlTouchView);
    }

    @Override
    public void showViews(boolean showAnimation) {
        rightControlTouchView.clearAnimation();
        rightControlTouchView.setVisibility(View.VISIBLE);
    }
}
