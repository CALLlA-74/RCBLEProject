package com.example.rcbleproject;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

public class ConfirmRemoveDialogFragment extends DialogFragment {
    private Removable removable;

    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        removable = (Removable) context;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstance){
        super.onCreateDialog(savedInstance);
        long id = getArguments().getLong("object_id");
        String message = getArguments().getString("message");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        return builder
                .setTitle("")
                .setIcon(null)
                .setMessage(message)
                .setNegativeButton(getResources().getString(R.string.no), (dialog, which) -> removable.cancel())
                .setPositiveButton(getResources().getString(R.string.yes), (dialog, which) -> removable.remove(id))
                .create();
    }
}
