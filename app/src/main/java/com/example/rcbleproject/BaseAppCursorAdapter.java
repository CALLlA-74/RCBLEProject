package com.example.rcbleproject;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SimpleCursorAdapter;

public abstract class BaseAppCursorAdapter extends SimpleCursorAdapter {
    protected final LayoutInflater inflater;
    protected final int layout;
    protected View editingView = null;
    protected enum Mode{ view_mode, edit_mode };

    public BaseAppCursorAdapter(Context context, int resource, Cursor cursor,
                                String[] columns, int[] views, int flags){
        super(context, resource, cursor, columns, views, flags);
        inflater = LayoutInflater.from(context);
        layout = resource;
    }

    public abstract void bindView(View convertView, Context context, Cursor cursor);
    public abstract void resetEditingView();
    public abstract void cancelEdit();
}
