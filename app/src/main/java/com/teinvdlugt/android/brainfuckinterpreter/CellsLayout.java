package com.teinvdlugt.android.brainfuckinterpreter;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


public class CellsLayout extends LinearLayout {

    private List<Cell> cells = new ArrayList<>();
    private int pointer = 0;

    public void setText(int which, byte text) {
        cells.get(which).setText(Byte.toString(text));
    }

    public void movePointer(int which) {
        cells.get(pointer).setPointed(false);
        pointer = which;
        cells.get(which).setPointed(true);
    }

    private void init() {
        for (int i = 0; i < 100; i++) {
            Cell tv = new Cell(getContext());
            tv.setText("0");
            addView(tv);
            cells.add(tv);
        }
    }

    public void clearAllBytes() {
        for (TextView tv : cells) {
            tv.setText("0");
        }
    }

    public CellsLayout(Context context) {
        super(context);
        init();
    }

    public CellsLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
}
