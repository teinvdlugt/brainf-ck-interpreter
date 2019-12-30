package com.teinvdlugt.android.brainfuckinterpreter;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Keyboard extends FrameLayout implements View.OnLongClickListener {

    private OnTypeListener onTypeListener;
    private boolean backspaceLongClicking = false;

    public interface OnTypeListener {
        void onTypeCharacter(char character);
        void onClickBackspace();
    }

    private void init() {
        View view = inflate(getContext(), R.layout.layout_keyboard, null);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        addView(view, lp);

        // Backspace long-click (See also this.onLongClick)
        View backspace = findViewById(R.id.button23);
        backspace.setOnLongClickListener(this);
        backspace.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP)
                    backspaceLongClicking = false;
                return false;
            }
        });

        // Set button listeners
        findViewById(R.id.button11).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onTypeListener != null) onTypeListener.onTypeCharacter('+');
            }
        });
        findViewById(R.id.button12).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onTypeListener != null) onTypeListener.onTypeCharacter('-');
            }
        });
        findViewById(R.id.button13).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onTypeListener != null) onTypeListener.onTypeCharacter('.');
            }
        });
        findViewById(R.id.button14).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onTypeListener != null) onTypeListener.onTypeCharacter(',');
            }
        });
        findViewById(R.id.button21).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onTypeListener != null) onTypeListener.onTypeCharacter('<');
            }
        });
        findViewById(R.id.button22).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onTypeListener != null) onTypeListener.onTypeCharacter('>');
            }
        });
        // button23 is backspace
        findViewById(R.id.button31).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onTypeListener != null) onTypeListener.onTypeCharacter('[');
            }
        });
        findViewById(R.id.button32).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onTypeListener != null) onTypeListener.onTypeCharacter(']');
            }
        });
        findViewById(R.id.button33).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onTypeListener != null) onTypeListener.onTypeCharacter(' ');
            }
        });
        findViewById(R.id.button34).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onTypeListener != null) onTypeListener.onTypeCharacter('\n');
            }
        });

        // Set backspace clickListener
        backspace.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onTypeListener != null) onTypeListener.onClickBackspace();
            }
        });
    }

    @Override
    public boolean onLongClick(View v) {
        // Called when backspace is long clicked
        backspaceLongClicking = true;
        if (onTypeListener != null) {
            final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (onTypeListener != null) onTypeListener.onClickBackspace();
                }
            };
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (backspaceLongClicking) {
                        post(runnable);
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException ignored) {}
                    }
                }
            }).start();
        }

        return false;
    }

    public void setEditText(final EditText editText) {
        onTypeListener = new OnTypeListener() {
            @Override
            public void onTypeCharacter(char character) {
                final int selection = editText.getSelectionStart();
                editText.getText().insert(selection, Character.toString(character));
            }

            @Override
            public void onClickBackspace() {
                final int selection = editText.getSelectionStart();
                if (selection > 0)
                    editText.getText().replace(selection - 1, selection, "");
            }
        };
    }

    // A few overrides to prevent problems with the
    // backspace thread. Similar to onPause() in an Activity.
    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility != View.VISIBLE) backspaceLongClicking = false;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        backspaceLongClicking = false;
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (!hasWindowFocus) backspaceLongClicking = false;
    }

    public Keyboard(Context context) {
        super(context);
        init();
    }

    public Keyboard(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /*public void resetBackgrounds() {
        boolean keyFeedback = PreferenceManager.getDefaultSharedPreferences(getContext())
                .getBoolean(MainActivity.KEYBOARD_FEEDBACK, true);
        View[] buttons = new View[]{button1, button2, button3, button4, button5,
                button6, button7, button8, button9, button0, backspace};
        if (keyFeedback) {
            TypedValue outValue = new TypedValue();
            getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            for (View button : buttons)
                button.setBackgroundResource(outValue.resourceId);
        } else {
            for (View button : buttons) {
                button.setBackground(null);
            }
        }
    }*/
}
