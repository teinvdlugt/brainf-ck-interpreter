package com.teinvdlugt.android.brainfuckinterpreter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

public class Cell extends TextView {

    private Paint linePaint;
    private int pointedColor;
    private boolean pointed;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(0, 0, getWidth(), getHeight(), linePaint);
    }

    public void setPointed(boolean pointed) {
        this.pointed = pointed;
        if (pointed) {
            setBackgroundResource(R.color.pointerCell);
        } else {
            setBackgroundColor(Color.TRANSPARENT);
        }
    }

    private void init() {
        linePaint = new Paint();
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(3);
        linePaint.setAntiAlias(true);

        int _8dp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
        setPadding(_8dp, _8dp, _8dp, _8dp);
        setTextSize(24);
        setTextColor(Color.BLACK);
    }

    public Cell(Context context) {
        super(context);
        init();
    }

    public Cell(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public Cell(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
}
