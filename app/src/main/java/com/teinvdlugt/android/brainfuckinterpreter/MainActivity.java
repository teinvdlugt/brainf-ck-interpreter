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
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.Arrays;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends AppCompatActivity implements
        InputDialogFragment.InputGivenListener, Interpreter.Listener {
    public static final String DAY_NIGHT_THEME_PREF = "theme";
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
    private CoordinatorLayout coordinatorLayout; // To attach SnackBars to

    private Interpreter interpreter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        AppCompatDelegate.setDefaultNightMode(PreferenceManager.getDefaultSharedPreferences(this)
                .getInt(DAY_NIGHT_THEME_PREF, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));

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
        coordinatorLayout = findViewById(R.id.coordinator_layout);
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
            case R.id.night_theme:
                onClickTheme();
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
        stop();
        if (editText.length() != 0) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.hello_world_example)
                    .setMessage(R.string.hello_world_description)
                    .setPositiveButton(R.string.hello_world_positive_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            loadHelloWorldCode();
                        }
                    }).setNegativeButton(R.string.cancel, null)
                    .create().show();
        } else {
            loadHelloWorldCode();
        }
    }

    /**
     * Loads Hello World code into editText and shows Undo option.
     */
    private void loadHelloWorldCode() {
        final String oldCode = editText.getText().toString();
        editText.setText(HELLO_WORLD_CODE);
        editText.setSelection(editText.getText().length());
        Snackbar.make(coordinatorLayout, R.string.hello_world_example_loaded, 4000)
                .setAction(R.string.undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        editText.setText(oldCode);
                        editText.setSelection(editText.getText().length());
                    }
                })
                .show();
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
        final List<String> existingFilenames = IOUtils.loadFilenameList(this);
        showEditTextDialog(this, new EditTextDialogListener() {
            @Override
            public boolean enableSaveButtonOnTextChange(CharSequence text) {
                return text.length() != 0 && !existingFilenames.contains(text.toString());
            }

            @Override
            public void onPositive(String text) {
                boolean success = IOUtils.save(MainActivity.this, code, text);
                if (success)
                    Snackbar.make(coordinatorLayout, R.string.file_saved, Snackbar.LENGTH_LONG).show();
                else
                    new AlertDialog.Builder(MainActivity.this)
                            .setMessage(R.string.file_save_error)
                            .setPositiveButton(R.string.ok, null)
                            .create().show();
            }
        }, null, editText);
    }

    interface EditTextDialogListener {
        boolean enableSaveButtonOnTextChange(CharSequence text);
        void onPositive(String text);
    }

    /**
     * initialText will be filled in EditText and selected.
     * {@code aView} should be a View from the activity, and is used to hide the Keyboard.
     */
    public static void showEditTextDialog(final Context context, final EditTextDialogListener listener, String initialText, final View aView) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_filename, null);
        final EditText editText = view.findViewById(R.id.filename_editText);
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
                        aView.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                ((InputMethodManager) context.getSystemService(INPUT_METHOD_SERVICE))
                                        .hideSoftInputFromWindow(aView.getWindowToken(), 0);
                            }
                        }, 5); // Sketchy but can't find another way
                    }
                })
                .create();
        dialog.show();
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (listener.enableSaveButtonOnTextChange(s))
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                else
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        editText.setText(initialText);
        editText.selectAll();

        // Focus and show keyboard
        editText.requestFocus();
        view.post(new Runnable() {
            @Override
            public void run() {
                ((InputMethodManager) context.getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(editText, 0);
            }
        });
    }

    // Interactions with FilesActivity

    private static final int FILES_ACTIVITY_RQ = 1;
    public static final String SCRIPT_EXTRA = "script_string";
    public static final String FILENAME_EXTRA = "filename_string";

    public void onClickLoad() {
        startActivityForResult(new Intent(this, FilesActivity.class), FILES_ACTIVITY_RQ);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == FILES_ACTIVITY_RQ
                && resultCode == RESULT_OK
                && data != null && data.hasExtra(SCRIPT_EXTRA)) {
            final String oldCode = editText.getText().toString();
            editText.setText(data.getStringExtra(SCRIPT_EXTRA));
            editText.setSelection(editText.getText().length());
            String filename = data.getStringExtra(FILENAME_EXTRA);
            Snackbar.make(coordinatorLayout, getString(R.string.file_loaded_format, filename), 4000)
                    .setAction(R.string.undo, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            editText.setText(oldCode);
                            editText.setSelection(editText.getText().length());
                        }
                    })
                    .show();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    private void onClickTheme() {
        final int[] options = new int[]{AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM};
        String[] optionTexts = new String[]{getString(R.string.day_theme), getString(R.string.night_theme), getString(R.string.system_default_theme)};
        int currentSelectedIndex = Arrays.asList(AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM).indexOf(
                PreferenceManager.getDefaultSharedPreferences(this)
                        .getInt(DAY_NIGHT_THEME_PREF, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));

        new AlertDialog.Builder(this)
                .setTitle(R.string.theme)
                .setSingleChoiceItems(optionTexts, currentSelectedIndex, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit()
                                .putInt(DAY_NIGHT_THEME_PREF, options[which]).apply();
                        AppCompatDelegate.setDefaultNightMode(options[which]);
                        dialog.dismiss();
                    }
                }).create().show();
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
