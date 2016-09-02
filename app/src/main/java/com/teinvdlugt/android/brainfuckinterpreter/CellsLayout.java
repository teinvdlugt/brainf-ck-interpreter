package com.teinvdlugt.android.brainfuckinterpreter;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


public class CellsLayout extends LinearLayout {
    public static final int MAX_CELL_AMOUNT = 30000; // TODO Too much views/memory? Try RecyclerView
    public static final int INITIAL_CELL_AMOUNT = 100;

    private List<Cell> cells = new ArrayList<>();
    private int pointer = 0;

    public void setText(int which, byte text) {
        cells.get(which).setText(Byte.toString(text));
    }

    public void movePointer(int which) {
        if (which >= cells.size()) {
            for (int i = 0; i < which - cells.size() + 1; i++) {
                Cell tv = new Cell(getContext());
                tv.setText("0");
                addView(tv);
                cells.add(tv);
            }
        }
        if (pointer < cells.size())
            cells.get(pointer).setPointed(false);
        pointer = which;
        cells.get(which).setPointed(true);
    }

    private void init() {
        for (int i = 0; i < INITIAL_CELL_AMOUNT; i++) {
            Cell tv = new Cell(getContext());
            tv.setText("0");
            addView(tv);
            cells.add(tv);
        }
    }

    public void clearAllBytes() {
        if (cells.size() > INITIAL_CELL_AMOUNT) {
            removeViews(INITIAL_CELL_AMOUNT, cells.size() - INITIAL_CELL_AMOUNT);
            cells.removeAll(cells.subList(INITIAL_CELL_AMOUNT, cells.size() - 1));
        }
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
