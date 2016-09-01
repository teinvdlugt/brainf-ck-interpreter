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
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.analytics.FirebaseAnalytics;

public class MainActivity extends AppCompatActivity implements BackspaceButton.BackspaceListener {
    public static final String DELAY_PREFERENCE = "delay";
    private static final String HELLO_WORLD_CODE = "++++++++[>++++[>++>+++>+++>+<<<<-]>+>+>->>+[<]<-]>>.>---.+++++++..+++.>>.<-.<.+++.------.--------.>>+.>++.";
    private static final int OUTPUT_MODE_ASCII = 0;
    private static final int OUTPUT_MODE_DEC = 1;
    private static final int OUTPUT_MODE_BIN = 2;
    private static final int OUTPUT_MODE_OCT = 3;
    private static final int OUTPUT_MODE_HEX = 4;
    private static final String OUTPUT_MODE_PREFERENCE = "output_mode";
    private int current_output_mode = 0;

    private FirebaseAnalytics firebaseAnalytics;

    private EditText et;
    private TextView outputTV;
    private CellsLayout cellsLayout;
    private Button clearOutputButton; // Only on x-large devices

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        setContentView(R.layout.activity_main);

        delay = PreferenceManager.getDefaultSharedPreferences(this)
                .getInt(DELAY_PREFERENCE, 0);

        current_output_mode = PreferenceManager.getDefaultSharedPreferences(this)
                .getInt(OUTPUT_MODE_PREFERENCE, 0);
        if (current_output_mode < 0 || current_output_mode > 4) current_output_mode = 0;

        BackspaceButton backspace = (BackspaceButton) findViewById(R.id.backspace_key);
        backspace.setBackspaceListener(this);

        et = (EditText) findViewById(R.id.editText);
        outputTV = (TextView) findViewById(R.id.output_textView);
        clearOutputButton = (Button) findViewById(R.id.clearOutputButton);
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
                    Bundle bundle = new Bundle();
                    bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "execute_code_button"); // TODO change to ITEM_NAME?
                    firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
                }
                return true;
            case R.id.delay:
                final String[] entries = {getString(R.string.no_delay),
                        getString(R.string.delay_ms_format, 2),
                        getString(R.string.delay_ms_format, 10),
                        getString(R.string.delay_ms_format, 50),
                        getString(R.string.delay_ms_format, 100),
                        getString(R.string.delay_ms_format, 500),
                        getString(R.string.delay_ms_format, 1000)};
                final int[] values = {0, 2, 10, 50, 100, 500, 1000};
                final int checked = PreferenceManager.getDefaultSharedPreferences(this).getInt(DELAY_PREFERENCE, 2);
                final int checkedIndex = checked == 0 ? 0 : checked == 2 ? 1 : checked == 10 ? 2 : checked == 50 ? 3 :
                        checked == 100 ? 4 : checked == 500 ? 5 : checked == 1000 ? 6 : -1;

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
            case R.id.menu_hello_world:
                if (et.length() != 0) {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.hello_world_example)
                            .setMessage(R.string.hello_world_description)
                            .setPositiveButton(R.string.hello_world_positive_button, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    loadHelloWorldExample();
                                }
                            }).setNegativeButton(R.string.cancel, null)
                            .create().show();
                } else {
                    loadHelloWorldExample();
                }
                return true;
            case R.id.menu_output:
                new AlertDialog.Builder(this)
                        .setTitle(R.string.output_in)
                        .setSingleChoiceItems(R.array.output_modes, current_output_mode, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                current_output_mode = which;
                                PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit()
                                        .putInt(OUTPUT_MODE_PREFERENCE, current_output_mode).apply();
                                dialog.dismiss();
                            }
                        }).create().show();
                return true;
            case R.id.menu_clear_output:
                onClickClearOutput(null);
            default:
                return false;
        }
    }

    public void onClickClearOutput(View view) {
        outputTV.setText("");
        outputTV.setVisibility(View.GONE);
        if (clearOutputButton != null) clearOutputButton.setVisibility(View.GONE);
    }

    private void loadHelloWorldExample() {
        stop();
        et.setText(HELLO_WORLD_CODE);
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
                                        if (clearOutputButton != null) clearOutputButton.setVisibility(View.VISIBLE);
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
                                        if (clearOutputButton != null) clearOutputButton.setVisibility(View.VISIBLE);
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
                            if (input == -1 || input == 255 /* Weird bytes sometimes say that they're 255 */) {
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
                            final String text;
                            switch (current_output_mode) {
                                case OUTPUT_MODE_ASCII:
                                    text = String.valueOf((char) bytes[ptr]);
                                    break;
                                case OUTPUT_MODE_DEC:
                                    text = Integer.toString(bytes[ptr]);
                                    break;
                                case OUTPUT_MODE_BIN:
                                    text = Integer.toBinaryString(bytes[ptr]);
                                    break;
                                case OUTPUT_MODE_OCT:
                                    text = Integer.toOctalString(bytes[ptr]);
                                    break;
                                case OUTPUT_MODE_HEX:
                                    text = Integer.toHexString(bytes[ptr]);
                                    break;
                                default:
                                    text = null;
                            }
                            if (text != null)
                                outputTV.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        outputTV.setVisibility(View.VISIBLE);
                                        if (clearOutputButton != null) clearOutputButton.setVisibility(View.VISIBLE);
                                        // Insert space if not in ASCII-output mode
                                        if (outputTV.length() != 0 && current_output_mode != OUTPUT_MODE_ASCII)
                                            outputTV.append(" ");
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
                                if (clearOutputButton != null) clearOutputButton.setVisibility(View.VISIBLE);
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
