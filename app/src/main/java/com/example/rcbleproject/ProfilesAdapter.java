package com.example.rcbleproject;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.rcbleproject.Database.DatabaseAdapterProfilesControl;

public class ProfilesAdapter extends BaseAppCursorAdapter {

    private final DatabaseAdapterProfilesControl dbProfilesAdapter;
    private final ProfilesActivity activity;
    private final boolean isReverse;          // если true, профили будут выводить в обратном порядке

    public ProfilesAdapter(ProfilesActivity context, int resource, DatabaseAdapterProfilesControl adapter,
                           boolean isReverse){
        super(context, resource, adapter.getProfiles_cursor(isReverse), adapter.getColumns(),
                new int[]{R.id.et_name, R.id.tv_name, R.id.bt_delete,
                            R.id.bt_cancel, R.id.bt_ok}, 0);
        this.isReverse = isReverse;
        dbProfilesAdapter = adapter;
        activity = context;
    }


    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent){
        View convertView = inflater.inflate(this.layout, parent, false);
        ViewHolder holder = new ViewHolder(convertView);
        convertView.setTag(holder);
        return convertView;
    }

    public void bindView(View convertView, Context context, Cursor cursor){
        final ViewHolder holder = (ViewHolder) convertView.getTag();;
        holder.id = cursor.getLong(cursor.getColumnIndexOrThrow(dbProfilesAdapter.ID));

        holder.et_profile_name.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) resetEditingView();
            return false;
        });

        holder.bt_delete_profile.setOnClickListener(v -> {
            long id = ((ViewHolder)((View)v.getParent()).getTag()).id;
            if (BuildConfig.DEBUG) Log.v("APP_TAG", "delete. id = " + id);
            ProfileControl profileControl = dbProfilesAdapter.getProfile(id);

            ConfirmRemoveDialogFragment dialog = new ConfirmRemoveDialogFragment();
            Bundle args = new Bundle();
            args.putLong("object_id", id);
            args.putString("message", activity.getResources().getString(R.string.confirm_msg_profile) + " \""
                            + profileControl.getName() + "\" ?");
            dialog.setArguments(args);
            dialog.show(activity.getSupportFragmentManager(), activity.getResources().getString(R.string.app_name));
        });

        holder.bt_cancel.setOnClickListener(v -> { cancelEdit(); });

        holder.bt_ok.setOnClickListener(v -> {
            ViewHolder vh = ((ViewHolder)editingView.getTag());
            activity.hideKeyboard(vh.et_profile_name);
            resetEditingView();
        });

        holder.tv_profile_name.setText(cursor.getString(cursor.getColumnIndexOrThrow(dbProfilesAdapter.PROFILE_NAME)));
        if (BuildConfig.DEBUG)
            Log.v("APP_TAG", "get view. id = " + cursor.getLong(cursor.getColumnIndexOrThrow(dbProfilesAdapter.ID)));
    }

    public long addProfile(String name){
        long id = dbProfilesAdapter.insert(name);
        if (BuildConfig.DEBUG) Log.v("APP_TAG", "addProf. id = " + id);
        swapCursor(dbProfilesAdapter.getProfiles_cursor(isReverse));
        //setFocusOnEditText(getView((isReverse? 0 : getCount() - 1), ), id);
        return id;
    }

    public void removeProfile(long id){
        dbProfilesAdapter.delete(id);
        swapCursor(dbProfilesAdapter.getProfiles_cursor(isReverse));
    }


    public void cancelEdit(){
        if (editingView == null) return;
        ViewHolder vh = ((ViewHolder)editingView.getTag());
        setMode(Mode.view_mode, vh);
        activity.hideKeyboard(vh.et_profile_name);
        editingView = null;
    }

    public void setMode(Mode mode, ViewHolder vh){
        switch (mode){
            case view_mode:
                vh.et_profile_name.setVisibility(View.GONE);
                vh.et_profile_name.setText("");
                vh.bt_ok.setVisibility(View.GONE);
                vh.bt_cancel.setVisibility(View.GONE);
                vh.tv_profile_name.setVisibility(View.VISIBLE);
                vh.bt_delete_profile.setVisibility(View.VISIBLE);
                activity.setImageButtonVisibility(View.VISIBLE);
                break;
            case edit_mode:
                activity.setImageButtonVisibility(View.GONE);
                vh.tv_profile_name.setVisibility(View.GONE);
                vh.bt_delete_profile.setVisibility(View.GONE);
                vh.bt_ok.setVisibility(View.VISIBLE);
                vh.bt_cancel.setVisibility(View.VISIBLE);
                vh.et_profile_name.setVisibility(View.VISIBLE);
        }
    }

    public void setFocusOnEditText(View view){
        if (view == null) return;
        ViewHolder holder = (ViewHolder) view.getTag();
        if (holder == null) return;
        resetEditingView();
        setEditingView(view);
        setMode(Mode.edit_mode, holder);
        holder.et_profile_name.setText(holder.tv_profile_name.getText());
        holder.et_profile_name.setSelectAllOnFocus(true);
        holder.et_profile_name.requestFocus();
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(holder.et_profile_name, InputMethodManager.SHOW_IMPLICIT);
    }

    public void setEditingView(View editingView) {
        this.editingView = editingView;
    }

    @Override
    public void resetEditingView(){
        if (editingView == null) return;
        ViewHolder holder = (ViewHolder) editingView.getTag();
        if (holder == null) return;
        saveChanges(holder);
        editingView = null;
    }

    private void saveChanges(ViewHolder vh){
        String newName = vh.et_profile_name.getText().toString();
        if (newName.length() <= 0){
            setMode(Mode.view_mode, vh);
            return;
        }
        vh.tv_profile_name.setText(newName);
        dbProfilesAdapter.updateProfileName(vh.id, newName);
        setMode(Mode.view_mode, vh);
        swapCursor(dbProfilesAdapter.getProfiles_cursor(isReverse));
    }

    private class ViewHolder{
        final TextView tv_profile_name;
        final EditText et_profile_name;
        final ImageView bt_delete_profile;
        final ImageView bt_ok;
        final ImageView bt_cancel;
        long id = 0;

        ViewHolder(View view){
            tv_profile_name = view.findViewById(R.id.tv_name);
            et_profile_name = view.findViewById(R.id.et_name);
            bt_delete_profile = view.findViewById(R.id.bt_delete);
            bt_ok = view.findViewById(R.id.bt_ok);
            bt_cancel = view.findViewById(R.id.bt_cancel);
        }
    }
}
