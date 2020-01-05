package com.teinvdlugt.android.brainfuckinterpreter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class InputDialogFragment extends DialogFragment {
    public static final String INPUT_TYPE = "input_type";

    private EditText editText;
    private Spinner spinner;
    private InputGivenListener listener;

    public interface InputGivenListener {
        void onInputGiven(byte input);
        /**
         * When dialog is cancelled or dismissed. May be called multiple times.
         */
        void onCancelled();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_input, null);
        editText = dialogView.findViewById(R.id.inputEditText);
        spinner = dialogView.findViewById(R.id.inputSpinner);
        initSpinner();
        setSavedInputType();

        builder.setTitle(R.string.input_dialog_title)
                .setView(dialogView)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        getActivity().getPreferences(0).edit().putInt(INPUT_TYPE, spinner.getSelectedItemPosition()).apply();

                        String text = editText.getText().toString();
                        byte result;
                        try {
                            if (text.isEmpty()) throw new NumberFormatException();
                            else if (spinner.getSelectedItemPosition() == 0)
                                result = Byte.parseByte(text);
                            else result = (byte) text.charAt(0);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                            result = 0;
                            Toast.makeText(getContext(), "Invalid input, entering 0", Toast.LENGTH_SHORT).show();
                        }

                        if (listener != null) listener.onInputGiven(result);
                    }
                });
        return builder.create();
    }

    private void initSpinner() {
        String[] options = {getString(R.string.decimal), getString(R.string.ascii)};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (i == 0) editText.setInputType(InputType.TYPE_NUMBER_FLAG_SIGNED);
                else if (i == 1) editText.setInputType(InputType.TYPE_CLASS_TEXT);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
    }

    private void setSavedInputType() {
        int savedInputType = getActivity().getPreferences(0).getInt(INPUT_TYPE, 0);
        spinner.setSelection(savedInputType);
        if (savedInputType == 0) editText.setInputType(InputType.TYPE_NUMBER_FLAG_SIGNED);
        else editText.setInputType(InputType.TYPE_CLASS_TEXT);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        listener = (InputGivenListener) context;
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        listener.onCancelled();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        listener.onCancelled();
    }
}
