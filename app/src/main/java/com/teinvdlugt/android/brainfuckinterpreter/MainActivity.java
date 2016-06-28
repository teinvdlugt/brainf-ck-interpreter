package com.teinvdlugt.android.brainfuckinterpreter;

import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements BackspaceButton.BackspaceListener {
    public static final String DELAY_PREFERENCE = "delay";

    private EditText et;
    private TextView outputTV;
    private CellsLayout cellsLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        delay = PreferenceManager.getDefaultSharedPreferences(this)
                .getInt(DELAY_PREFERENCE, 0);

        BackspaceButton backspace = (BackspaceButton) findViewById(R.id.backspace_key);
        backspace.setBackspaceListener(this);

        et = (EditText) findViewById(R.id.editText);
        outputTV = (TextView) findViewById(R.id.output_textView);
        cellsLayout = (CellsLayout) findViewById(R.id.cellsLayout);
        disableSoftKeyboard(et, true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        if (running) {
            menu.findItem(R.id.run).setIcon(R.mipmap.ic_stop_white_24dp);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.run:
                if (running) {
                    stop();
                } else {
                    run();
                }
                return true;
            case R.id.delay:
                final String[] entries = {getString(R.string.no_delay),
                        getString(R.string.delay_ms_format, 10),
                        getString(R.string.delay_ms_format, 50),
                        getString(R.string.delay_ms_format, 100),
                        getString(R.string.delay_ms_format, 500),
                        getString(R.string.delay_ms_format, 1000)};
                final int[] values = {0, 10, 50, 100, 500, 1000};
                final int checked = PreferenceManager.getDefaultSharedPreferences(this).getInt(DELAY_PREFERENCE, 0);
                final int checkedIndex = checked == 0 ? 0 : checked == 10 ? 1 : checked == 50 ? 2 :
                        checked == 100 ? 3 : checked == 500 ? 4 : checked == 1000 ? 5 : -1;

                new AlertDialog.Builder(this)
                        .setTitle(R.string.delay)
                        .setSingleChoiceItems(entries, checkedIndex, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                delay = values[which];
                                PreferenceManager.getDefaultSharedPreferences(MainActivity.this)
                                        .edit().putInt(DELAY_PREFERENCE, delay).apply();

                                dialog.dismiss();
                            }
                        }).create().show();
                return true;
            default:
                return false;
        }
    }

    private boolean running = false;
    private int delay = 0;
    private byte[] bytes = new byte[CellsLayout.MAX_CELL_AMOUNT];
    private int ptr = 0;
    private int i = 0;
    private String code;
    private byte input = -1;

    private void run() {
        bytes = new byte[CellsLayout.MAX_CELL_AMOUNT];
        ptr = 0;
        i = 0;
        input = -1;
        code = et.getText().toString();
        cellsLayout.clearAllBytes();
        outputTV.setText("");
        outputTV.setVisibility(View.GONE);
        cellsLayout.movePointer(0);

        running = true;
        invalidateOptionsMenu();

        interpret();
    }

    private void stop() {
        running = false;
        invalidateOptionsMenu();
    }

    private void interpret() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (i < code.length() && running) {
                    char token = code.charAt(i);
                    boolean wait = true;
                    try {
                        if (token == '>') {
                            ptr++;
                            i++;

                            if (ptr >= CellsLayout.MAX_CELL_AMOUNT) {
                                outputTV.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        outputTV.setVisibility(View.VISIBLE);
                                        outputTV.setText(getString(R.string.error_maximum_cells, CellsLayout.MAX_CELL_AMOUNT));
                                    }
                                });
                                break;
                            }

                            final int movedPointer = ptr;
                            cellsLayout.post(new Runnable() {
                                @Override
                                public void run() {
                                    cellsLayout.movePointer(movedPointer);
                                }
                            });
                        } else if (token == '<') {
                            ptr--;
                            i++;

                            if (ptr < 0) {
                                outputTV.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        outputTV.setVisibility(View.VISIBLE);
                                        outputTV.setText(R.string.error);
                                    }
                                });
                                break;
                            }

                            final int movedPointer = ptr;
                            cellsLayout.post(new Runnable() {
                                @Override
                                public void run() {
                                    cellsLayout.movePointer(movedPointer);
                                }
                            });
                        } else if (token == '+') {
                            bytes[ptr]++;
                            i++;

                            setCellText(ptr, bytes[ptr]);
                        } else if (token == '-') {
                            bytes[ptr]--;
                            i++;

                            setCellText(ptr, bytes[ptr]);
                        } else if (token == ',') {
                            if (input == -1 || input == 255 /* Weird bytes say that they're 255 */) {
                                cellsLayout.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        askForInput();
                                    }
                                });
                                break;
                            } else {
                                bytes[ptr] = input;
                                i++;
                                setCellText(ptr, input);
                                input = -1;
                            }
                        } else if (token == '.') {
                            final String text = String.valueOf((char) bytes[ptr]);
                            outputTV.post(new Runnable() {
                                @Override
                                public void run() {
                                    outputTV.setVisibility(View.VISIBLE);
                                    outputTV.append(text);
                                }
                            });

                            i++;
                        } else if (token == '[') {
                            if (bytes[ptr] == 0) {
                                i = matchingClosingBracket(i) + 1;
                            } else {
                                i++;
                            }
                        } else if (token == ']') {
                            if (bytes[ptr] == 0) {
                                i++;
                            } else {
                                i = matchingOpeningBracket(i) + 1;
                            }
                        } else {
                            // Invalid character: skip
                            i++;
                            wait = false;
                        }
                    } catch (StringIndexOutOfBoundsException e) {
                        // Exception can be thrown when invoking matchingClosingBracket()
                        // while there is none
                        e.printStackTrace();
                        outputTV.post(new Runnable() {
                            @Override
                            public void run() {
                                outputTV.setVisibility(View.VISIBLE);
                                outputTV.setText(R.string.error);
                            }
                        });
                        break;
                    }

                    if (wait) {
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                if (running) {
                    cellsLayout.post(new Runnable() {
                        @Override
                        public void run() {
                            running = false;
                            invalidateOptionsMenu();
                        }
                    });
                }
            }
        }).start();
    }

    private void setCellText(final int cellIndex, final byte text) {
        cellsLayout.post(new Runnable() {
            @Override
            public void run() {
                cellsLayout.setText(cellIndex, text);
            }
        });
    }

    private int matchingClosingBracket(int i) {
        int openingBrackets = 0;
        while (true) {
            i++;
            if (code.charAt(i) == '[') {
                openingBrackets++;
            } else if (code.charAt(i) == ']') {
                if (openingBrackets == 0) return i;
                openingBrackets--;
            }
        }
    }

    private int matchingOpeningBracket(int i) {
        int closingBrackets = 0;
        while (true) {
            i--;
            if (code.charAt(i) == ']') {
                closingBrackets++;
            } else if (code.charAt(i) == '[') {
                if (closingBrackets == 0) return i;
                closingBrackets--;
            }
        }
    }

    private void askForInput() {
        LinearLayout layout = new LinearLayout(this);
        int _16dp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        layout.setPadding(_16dp, _16dp, _16dp, _16dp);
        final EditText inputET = new EditText(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layout.addView(inputET, params);

        new AlertDialog.Builder(this)
                .setTitle(R.string.input_dialog_title)
                .setView(layout)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (inputET.length() == 0)
                            input = 0;
                        else
                            input = (byte) inputET.getText().toString().charAt(0);
                        running = true;
                        interpret();
                    }
                }).create().show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stop();
    }

    public void disableSoftKeyboard(final EditText et, boolean prevent) {
        if (prevent) {
            if (Build.VERSION.SDK_INT >= 21) {
                et.setShowSoftInputOnFocus(false);
            } else {
                et.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        int inputType = et.getInputType(); // backup the input type
                        et.setInputType(InputType.TYPE_NULL); // disable soft input
                        et.onTouchEvent(motionEvent); // call native handler
                        et.setInputType(inputType); // restore input type
                        et.setFocusable(true);
                        return true; // consume touch even
                    }
                });
            }
        } else {
            if (Build.VERSION.SDK_INT >= 21) {
                et.setShowSoftInputOnFocus(true);
            } else {
                et.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        return et.onTouchEvent(motionEvent);
                    }
                });
            }
        }
    }

    public void onClickIncrement(View view) {
        et.getText().insert(et.getSelectionEnd(), "+");
    }

    public void onClickDecrement(View view) {
        et.getText().insert(et.getSelectionEnd(), "-");
    }

    public void onClickBracketOpen(View view) {
        et.getText().insert(et.getSelectionEnd(), "[");
    }

    public void onClickBracketClose(View view) {
        et.getText().insert(et.getSelectionEnd(), "]");
    }

    public void onClickInput(View view) {
        et.getText().insert(et.getSelectionEnd(), ",");
    }

    public void onClickOutput(View view) {
        et.getText().insert(et.getSelectionEnd(), ".");
    }

    public void onClickPointerLeft(View view) {
        et.getText().insert(et.getSelectionEnd(), "<");
    }

    public void onClickPointerRight(View view) {
        et.getText().insert(et.getSelectionEnd(), ">");
    }

    @Override
    public void onBackspaceInvoked() {
        try {
            if (et.getSelectionStart() == et.getSelectionEnd()) {
                et.getText().delete(et.getSelectionStart() - 1, et.getSelectionEnd());
            } else {
                et.getText().delete(et.getSelectionStart(), et.getSelectionEnd());
            }
        } catch (IndexOutOfBoundsException ignored) {/* ignored */}
    }
}
