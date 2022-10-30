package com.example.rcbleproject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.view.MotionEvent;

import java.util.ArrayList;

/**
 * Класс BaseControlElement содержит поля и методы для взаимодействия с элементом управления
 */

public abstract class BaseControlElement {
    protected Context context;
    protected int pointerID = -1;

    protected volatile float posX, posY;
    public volatile int elementSize;
    public volatile int elementIndex;
    public volatile boolean isElementLocked;

    public enum ControlElementType {JOYSTICK_XY, JOYSTICK_X, JOYSTICK_Y, BUTTON};

    public final long elementID;
    public final GridParams gridParams;
    public final Bitmap bitmapLock;

    public boolean focus = false;

    public BaseControlElement(long elementID, Context context, GridParams gridParams,
                              int elementIndex, int elementSize, boolean isGridVisible,
                              boolean isElementLocked, float pX, float pY){
        this.elementID = elementID;
        this.context = context;
        this.gridParams = gridParams;
        this.elementIndex = elementIndex;
        this.elementSize = elementSize;
        this.isElementLocked = isElementLocked;

        bitmapLock = BitmapFactory.decodeResource(context.getResources(), R.drawable.baseline_lock_black_18);

        posX = pX;
        posY = pY;
        if (isGridVisible) alignToTheGrid();
    }

    public abstract ControlElementType getType();
    public abstract String getName();

    public abstract void onDraw(Canvas canvas, ProfileControlActivity.MODE_TYPE mode);
    public abstract boolean contains(float pointerX, float pointerY);
    public abstract void onTouch(MotionEvent event, boolean isGridVisible);
    public abstract void onControl(int touchedPointerID, MotionEvent event);
    public abstract void alignToTheGrid();
    public abstract void setElementSize(int newElementSize);
    public abstract int getNumberOfAxes();
    public abstract String[] getAxesNames();

    protected abstract void checkOutDisplay();
    protected float square(float value){ return value*value; }

    /**
     *
     * @param canvas
     * @param paint
     * @param p1
     * @param p2
     * @param p3
     */
    protected void drawTriangle(Canvas canvas, Paint paint, PointF p1, PointF p2, PointF p3){
        Path path = new Path();
        path.setFillType(Path.FillType.EVEN_ODD);
        path.moveTo(p1.x, p1.y);
        path.lineTo(p2.x, p2.y);
        path.lineTo(p3.x, p3.y);
        path.close();

        canvas.drawPath(path, paint);
    }

    public float getPosX() { return posX; }
    public float getPosY() { return posY; }
    public int getElementSize() { return elementSize; }
}
