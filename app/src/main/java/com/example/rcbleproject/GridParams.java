package com.example.rcbleproject;

import android.util.DisplayMetrics;
import android.util.TypedValue;

public final class GridParams {
    public final int left, top, step, displayWidth, displayHeight;

    public GridParams(DisplayMetrics dm){
        step = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, 3, dm);
        int height = dm.heightPixels/step*step;        // высота сетки в px
        int width = dm.widthPixels/step*step;        // ширина сетки в px
        displayHeight = dm.heightPixels;
        displayWidth = dm.widthPixels;

        left = (dm.widthPixels - width)/2;
        top = (dm.heightPixels - height)/2;
    }
}
