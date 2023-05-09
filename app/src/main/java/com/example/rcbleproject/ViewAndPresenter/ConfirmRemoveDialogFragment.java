package com.example.rcbleproject.ViewAndPresenter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import com.example.rcbleproject.ViewAndPresenter.AddingHubsMenu.IRemovableHub;
import com.example.rcbleproject.R;

public class ConfirmRemoveDialogFragment extends DialogFragment {
    public enum FragmentType {ProfileControl, Hub, Unknown}
    private IRemovable iRemovable;
    private IRemovableHub iRemovableHub;

    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        try {
            iRemovable = (IRemovable) context;
        } catch (Exception e) {}

        try {
            iRemovableHub = (IRemovableHub) context;
        } catch (Exception e) {}
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstance){
        super.onCreateDialog(savedInstance);
        FragmentType fragmentType = IntToFragmentType(getArguments().getInt("type"));
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        if (fragmentType == FragmentType.ProfileControl){
            long id = getArguments().getLong("object_id");
            String message = getArguments().getString("message");
            return builder
                    .setTitle("")
                    .setIcon(null)
                    .setMessage(message)
                    .setNegativeButton(getResources().getString(R.string.no), (dialog, which) -> iRemovable.cancel())
                    .setPositiveButton(getResources().getString(R.string.yes), (dialog, which) -> iRemovable.remove(id))
                    .create();
        }
        if (fragmentType == FragmentType.Hub){
            String address = getArguments().getString("object_id");
            String message = getArguments().getString("message");
            return builder
                    .setTitle("")
                    .setIcon(null)
                    .setMessage(message)
                    .setNegativeButton(getResources().getString(R.string.no), (dialog, which) -> iRemovableHub.cancel())
                    .setPositiveButton(getResources().getString(R.string.yes), (dialog, which) -> iRemovableHub.remove(address))
                    .create();
        }
        return builder.create();
    }

    private FragmentType IntToFragmentType(int type){
        switch (type){
            case 0: return FragmentType.ProfileControl;
            case 1: return FragmentType.Hub;
        }
        return FragmentType.Unknown;
    }
}
