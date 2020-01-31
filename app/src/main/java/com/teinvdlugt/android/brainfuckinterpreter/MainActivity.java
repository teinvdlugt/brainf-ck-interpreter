package com.teinvdlugt.android.brainfuckinterpreter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.analytics.FirebaseAnalytics;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends AppCompatActivity implements
        InputDialogFragment.InputGivenListener, Interpreter.Listener {
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

    private EditText editText;
    private TextView outputTV;
    private Button clearOutputButton; // Only on x-large devices
    private Keyboard keyboard;
    private ViewGroup root; // To show SnackBars on

    private Interpreter interpreter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        // Initialize saved variables
        int delay = PreferenceManager.getDefaultSharedPreferences(this)
                .getInt(DELAY_PREFERENCE, 0);
        current_output_mode = PreferenceManager.getDefaultSharedPreferences(this)
                .getInt(OUTPUT_MODE_PREFERENCE, 0);
        if (current_output_mode < 0 || current_output_mode > 4) current_output_mode = 0;

        // Setup recyclerView
        RecyclerView cellsRecyclerView = findViewById(R.id.cellRecyclerView);
        cellsRecyclerView.setItemAnimator(null);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        cellsRecyclerView.setLayoutManager(layoutManager);
        CellsAdapter adapter = new CellsAdapter(this, layoutManager);
        cellsRecyclerView.setAdapter(adapter);

        // Setup other views
        root = findViewById(R.id.root);
        editText = findViewById(R.id.editText);
        outputTV = findViewById(R.id.output_textView);
        clearOutputButton = findViewById(R.id.clearOutputButton);
        keyboard = findViewById(R.id.keyboard);
        disableSoftKeyboard(editText, true);
        keyboard.setEditText(editText);
        editText.requestFocus();
        setupKeyboardSwitchButton();

        // Create interpreter
        interpreter = new Interpreter(this, this, adapter, delay);
    }

    private boolean inAppKeyboard = true;

    private void setupKeyboardSwitchButton() {
        findViewById(R.id.keyboard_switch_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (inAppKeyboard) {
                    // Hide in-app keyboard
                    keyboard.setVisibility(View.GONE);
                    // Enable soft keyboard
                    disableSoftKeyboard(editText, false);
                    // Show soft keyboard
                    ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                            .showSoftInput(editText, InputMethodManager.SHOW_FORCED);
                    inAppKeyboard = false;
                } else {
                    // Hide soft keyboard
                    ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                            .hideSoftInputFromWindow(editText.getWindowToken(), 0);
                    // Disable soft keyboard
                    disableSoftKeyboard(editText, true);
                    // Show in-app keyboard
                    keyboard.setVisibility(View.VISIBLE);
                    inAppKeyboard = true;
                }
                // Focus editText
                editText.post(new Runnable() {
                    @Override
                    public void run() {
                        editText.requestFocus();
                    }
                });
            }
        });
    }


    // MENU OPTIONS


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        if (interpreter.isRunning()) {
            menu.findItem(R.id.run).setIcon(R.mipmap.ic_stop_white_24dp);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.run:
                onClickRun();
                return true;
            case R.id.delay:
                onClickDelay();
                return true;
            case R.id.menu_hello_world:
                onClickHelloWorldExample();
                return true;
            case R.id.menu_output:
                onClickOutput();
                return true;
            case R.id.menu_clear_output:
                onClickClearOutput(null);
                return true;
            case R.id.menu_save:
                onClickSave();
                return true;
            case R.id.menu_load:
                onClickLoad();
                return true;
            default:
                return false;
        }
    }

    private void onClickRun() {
        if (interpreter.isRunning()) {
            stop();
        } else {
            run();
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "execute_code_button");
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
        }
    }

    private void onClickHelloWorldExample() {
        if (editText.length() != 0) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.hello_world_example)
                    .setMessage(R.string.hello_world_description)
                    .setPositiveButton(R.string.hello_world_positive_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            stop();
                            editText.setText(HELLO_WORLD_CODE);
                        }
                    }).setNegativeButton(R.string.cancel, null)
                    .create().show();
        } else {
            stop();
            editText.setText(HELLO_WORLD_CODE);
        }
    }

    private void onClickDelay() {
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
                        interpreter.setDelay(values[which]);
                        PreferenceManager.getDefaultSharedPreferences(MainActivity.this)
                                .edit().putInt(DELAY_PREFERENCE, values[which]).apply();

                        dialog.dismiss();
                    }
                }).create().show();
    }

    private void onClickOutput() {
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
    }

    public void onClickClearOutput(View view) {
        outputTV.setText("");
        outputTV.setVisibility(View.GONE);
        if (clearOutputButton != null) clearOutputButton.setVisibility(View.GONE);
    }

    public void onClickSave() { // TODO warn when overwriting file
        final String code = editText.getText().toString();

        // Check if there is something to save
        if (code.trim().isEmpty()) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.nothing_to_save)
                    .setPositiveButton(R.string.ok, null)
                    .create().show();
            return;
        }

        // Show filename dialog
        showEditTextDialog(this, new EditTextDialogListener() {
            @Override
            public void onPositive(String text) {
                boolean success = IOUtils.save(MainActivity.this, code, text);
                if (success)
                    Snackbar.make(root, R.string.file_saved, Snackbar.LENGTH_LONG).show();
                else
                    new AlertDialog.Builder(MainActivity.this)
                            .setMessage(R.string.file_save_error)
                            .setPositiveButton(R.string.ok, null)
                            .create().show();
            }
        }, null);
    }

    interface EditTextDialogListener {
        void onPositive(String text);
    }

    /**
     * initialText will be filled in EditText and selected
     */
    public static void showEditTextDialog(final Context context, final EditTextDialogListener listener, String initialText) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_filename, null);
        final EditText editText = view.findViewById(R.id.filename_editText);
        editText.setText(initialText);
        editText.selectAll();
        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(view)
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        listener.onPositive(editText.getText().toString());
                    }
                }).setNegativeButton(R.string.cancel, null)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        // Hide keyboard
                        editText.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                ((InputMethodManager) context.getSystemService(INPUT_METHOD_SERVICE))
                                        .hideSoftInputFromWindow(editText.getWindowToken(), 0);
                            }
                        }, 5); // Sketchy but can't find another way
                    }
                })
                .create();
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0)
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                else
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        dialog.show();

        // Focus and show keyboard
        editText.requestFocus();
        view.post(new Runnable() {
            @Override
            public void run() {
                ((InputMethodManager) context.getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(editText, 0);
            }
        });
    }

    public void onClickLoad() {
        startActivity(new Intent(this, FilesActivity.class));
    }


    // INTERPRETER METHODS


    private void run() {
        outputTV.setText("");
        outputTV.setVisibility(View.GONE);
        if (clearOutputButton != null) clearOutputButton.setVisibility(View.GONE);

        interpreter.run(editText.getText().toString());

        invalidateOptionsMenu();
    }

    private void stop() {
        interpreter.stop();
        invalidateOptionsMenu();
    }

    @Override
    public void doOutput(byte output) {
        final String outputStr;
        switch (current_output_mode) {
            case OUTPUT_MODE_ASCII:
                outputStr = String.valueOf((char) output);
                break;
            case OUTPUT_MODE_DEC:
                outputStr = Integer.toString(output);
                break;
            case OUTPUT_MODE_BIN:
                outputStr = Integer.toBinaryString(output);
                break;
            case OUTPUT_MODE_OCT:
                outputStr = Integer.toOctalString(output);
                break;
            case OUTPUT_MODE_HEX:
                outputStr = Integer.toHexString(output);
                break;
            default:
                outputStr = null;
        }
        if (outputStr != null) {
            outputTV.setVisibility(View.VISIBLE);
            if (clearOutputButton != null)
                clearOutputButton.setVisibility(View.VISIBLE);
            // Insert space if not in ASCII-output mode
            if (outputTV.length() != 0 && current_output_mode != OUTPUT_MODE_ASCII)
                outputTV.append(" ");
            outputTV.append(outputStr);
        }
    }

    @Override
    public void askForInput() {
        DialogFragment dialog = new InputDialogFragment();
        dialog.show(getSupportFragmentManager(), "InputDialogFragment");
    }

    @Override
    public void onInputGiven(byte input) {
        interpreter.continueOnInput(input);
    }

    @Override
    public void onCancelled() {
        // When input dialog is cancelled
        interpreter.stop();
        invalidateOptionsMenu();
    }

    @Override
    public void onFinished() {
        invalidateOptionsMenu();
    }

    @Override
    public void onError() {
        outputTV.setVisibility(View.VISIBLE);
        if (clearOutputButton != null)
            clearOutputButton.setVisibility(View.VISIBLE);
        outputTV.setText(R.string.error);
    }

    @Override
    public void onErrorMaximumCells() {
        outputTV.setVisibility(View.VISIBLE);
        if (clearOutputButton != null)
            clearOutputButton.setVisibility(View.VISIBLE);
        outputTV.setText(getString(R.string.error_maximum_cells, CellsLayout.MAX_CELL_AMOUNT));
    }


    @Override
    protected void onPause() {
        super.onPause();
        stop();
    }

    public void disableSoftKeyboard(final EditText et, boolean disable) { // TODO renew
        if (disable) {
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
}
