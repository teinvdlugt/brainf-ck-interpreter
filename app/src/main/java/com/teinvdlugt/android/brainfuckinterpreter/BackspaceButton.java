package com.teinvdlugt.android.brainfuckinterpreter;

import android.content.Context;
import androidx.core.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ImageButton;


public class BackspaceButton extends ImageButton {

    private BackspaceListener listener;
    private GestureDetectorCompat detector;
    private boolean stillDown = false;

    public interface BackspaceListener {
        void onBackspaceInvoked();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        detector.onTouchEvent(event);
        if (event.getActionMasked() == MotionEvent.ACTION_UP) stillDown = false;

        return super.onTouchEvent(event);
    }

    public void setBackspaceListener(BackspaceListener listener) {
        this.listener = listener;
    }

    private void init() {
        detector = new GestureDetectorCompat(getContext(), new GestureListener());
    }

    public BackspaceButton(Context context) {
        super(context);
        init();
    }

    public BackspaceButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BackspaceButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            if (listener != null) listener.onBackspaceInvoked();
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            stillDown = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (stillDown) {
                        if (listener != null) {
                            post(new Runnable() {
                                @Override
                                public void run() {
                                    listener.onBackspaceInvoked();
                                }
                            });
                        }
                        try {
                            Thread.sleep(75);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            }).start();
        }
    }
}
