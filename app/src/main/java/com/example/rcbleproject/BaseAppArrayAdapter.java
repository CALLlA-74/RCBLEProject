package com.example.rcbleproject;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.SimpleCursorAdapter;

import java.util.List;

public abstract class BaseAppArrayAdapter<T> extends ArrayAdapter<T> {
    protected final LayoutInflater inflater;
    protected final int layout;
    protected View editingView = null;
    protected View viewInConMenu = null;
    protected enum Mode{ view_mode, context_menu_mode, edit_mode };

    public BaseAppArrayAdapter(Context context, int resource, List<T> objects){
        super(context, resource, objects);
        inflater = LayoutInflater.from(context);
        layout = resource;
    }

    public abstract void resetEditingView();
    public abstract boolean cancelEdit();
}
