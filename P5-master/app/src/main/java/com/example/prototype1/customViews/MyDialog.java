package com.example.prototype1.customViews;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.prototype1.R;

public class MyDialog extends DialogFragment {
    private Button button_cancel, button_confirm;
    private EditText eText_pinCode;

    public MyDialog() {
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View v = inflater.inflate(R.layout.layout_phone_confirm, null);
        eText_pinCode = v.findViewById(R.id.eText_codeField);
        button_confirm = v.findViewById(R.id.button_phone_pin_confirm);

        builder.setView(v);

        button_confirm.setOnClickListener(view -> Toast.makeText(getContext(), "Confirm is Clicked ", Toast.LENGTH_SHORT).show());
        button_cancel.setOnClickListener(view -> {
        });

        return builder.create();
    }


}