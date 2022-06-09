package com.example.rcbleproject;

import static java.lang.Math.abs;
import static java.lang.Math.min;
import static java.lang.Math.sqrt;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;

import java.util.ArrayList;

public class JoystickXY extends BaseControlElement{
    private volatile float radius,
                           stickPosX,
                           stickPosY,
                           stickRadius;
    private volatile Paint paintBackground, paintBorder, paintPointer, paintTabletBoard;

    public volatile float deltaX,
                          deltaY;

    public JoystickXY(long elementID, Context context, GridParams gridParams, int elementIndex,
                      int elementSize, boolean isGridVisible, boolean isElementLocked,  float pX,
                      float pY){
        super(elementID, context, gridParams, elementIndex, elementSize, isGridVisible,isElementLocked,
                pX, pY);
        if (isGridVisible) alignToTheGrid();
        stickPosX = pX;
        stickPosY = pY;
        radius = (3 + elementSize)*gridParams.step;
        stickRadius = 0.75f*elementSize*gridParams.step;
        paintBackground = new Paint();
        paintBackground.setColor(context.getColor(R.color.black));
        paintBackground.setStyle(Paint.Style.FILL);
        paintBackground.setAntiAlias(true);

        paintBorder = new Paint();
        paintBorder.setColor(context.getColor(R.color.white));
        paintBorder.setStyle(Paint.Style.FILL);
        paintBorder.setAntiAlias(true);

        paintPointer = new Paint();
        paintPointer.setColor(context.getColor(R.color.white));
        paintPointer.setStyle(Paint.Style.FILL);
        paintPointer.setAntiAlias(true);

        paintTabletBoard = new Paint();
        paintTabletBoard.setColor(context.getColor(R.color.white));
        paintTabletBoard.setAntiAlias(true);
        paintPointer.setStyle(Paint.Style.FILL);
        paintPointer.setAntiAlias(true);
    }

    public JoystickXY(Context context){
        super(-1, context, null, 0, 0,
                false,false,0 ,0 );
    }

    @Override
    public ControlElementType getType() { return ControlElementType.JOYSTICK_XY; }

    @Override
    public String getName() { return context.getString(R.string.joystick_xy_name); }

    @Override
    public boolean contains(float pointerX, float pointerY){
        return square(abs(pointerX - posX)) + square(abs(pointerY - posY)) <= square(radius);
    }

    @Override
    public void setElementSize(int newElementSize){
        elementSize = newElementSize;
        radius = (3 + elementSize)*gridParams.step;
        stickRadius = 0.75f*elementSize*gridParams.step;
    }

    @Override
    public int getNumberOfAxes() { return 2; }

    @Override
    public String[] getAxesNames() {
        return new String[] {"X", "Y"};
    }

    @SuppressLint("DrawAllocation")
    @Override
    public void onDraw(Canvas canvas, ProfileControlActivity.MODE_TYPE mode){
        if (canvas == null || paintPointer == null || paintBackground == null) return;
        canvas.drawCircle(posX, posY, radius, paintBorder);
        canvas.drawCircle(posX, posY, radius - 1, paintBackground);
        drawTriangle(canvas, paintPointer, new PointF(posX, posY-radius*0.95f),             // верхний указатель
                                    new PointF(posX-radius*0.1f, posY-radius*0.85f),
                                    new PointF(posX+radius*0.1f, posY-radius*0.85f));
        drawTriangle(canvas, paintPointer, new PointF(posX, posY+radius*0.95f),             // нижний указатель
                new PointF(posX-radius*0.1f, posY+radius*0.85f),
                new PointF(posX+radius*0.1f, posY+radius*0.85f));
        drawTriangle(canvas, paintPointer, new PointF(posX-radius*0.95f, posY),             // левый указатель
                new PointF(posX-radius*0.85f, posY+radius*0.1f),
                new PointF(posX-radius*0.85f, posY-radius*0.1f));
        drawTriangle(canvas, paintPointer, new PointF(posX+radius*0.95f, posY),             // правый указатель
                new PointF(posX+radius*0.85f, posY+radius*0.1f),
                new PointF(posX+radius*0.85f, posY-radius*0.1f));

        if (mode == ProfileControlActivity.MODE_TYPE.EDIT_MODE){
            String num = "#" + (elementIndex+1);
            Rect boundsNum = new Rect();
            paintBackground.getTextBounds(num, 0, num.length(), boundsNum);
            if (focus) paintTabletBoard.setColor(context.getColor(R.color.yellow));
            else paintTabletBoard.setColor(context.getColor(R.color.white));
            paintBackground.setTextSize(gridParams.step);
            float rightNum, leftNum, topNum, bottomNum, borderRadiusNum;

            if (!isElementLocked){
                rightNum = posX - gridParams.step*0.5f;
                leftNum = rightNum - boundsNum.width() - gridParams.step;
                topNum = posY - gridParams.step*0.75f;
                bottomNum = posY + gridParams.step*0.75f;
                borderRadiusNum = gridParams.step*0.75f;
            }
            else {
                rightNum = posX - gridParams.step*0.5f;
                leftNum = rightNum - boundsNum.width() - gridParams.step;
                topNum = posY - gridParams.step*0.75f;
                bottomNum = posY + gridParams.step*0.75f;
                borderRadiusNum = gridParams.step*0.75f;

                float leftLock = posX + gridParams.step*0.5f,
                        rightLock = leftLock + gridParams.step + bitmapLock.getWidth(),
                        topLock = topNum,
                        bottomLock = bottomNum,
                        borderRadiusLock = borderRadiusNum;
                canvas.drawRoundRect(leftLock, topLock, rightLock, bottomLock, borderRadiusLock,
                        borderRadiusLock, paintTabletBoard);
            /*canvas.drawText(lock, leftLock + gridParams.step*0.5f,
                    posY+gridParams.step*0.25f, paintBackground);*/
                canvas.drawBitmap(bitmapLock, leftLock + gridParams.step*0.5f,
                        posY - bitmapLock.getHeight()/2, paintBackground);
            }

            canvas.drawRoundRect(leftNum, topNum, rightNum, bottomNum, borderRadiusNum, borderRadiusNum,
                    paintTabletBoard);
            canvas.drawText(num, leftNum + gridParams.step*0.5f,
                    posY+gridParams.step*0.25f, paintBackground);
        }
        else {
            canvas.drawCircle(stickPosX, stickPosY, stickRadius, paintPointer);
        }
    }

    @Override
    public void onTouch(MotionEvent event, boolean isGridVisible){
        Log.v("APP_TAG3", "id: " + event.getPointerId(event.getActionIndex()) +
                " action: " + event.getAction() + " " + event.getActionMasked());
        if (isElementLocked) return;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_POINTER_UP:
                deltaX = event.getX()-posX;
                deltaY = event.getY()-posY;
                break;
            case MotionEvent.ACTION_MOVE:
                posX = (int)(event.getX() - deltaX);
                posY = (int)(event.getY() - deltaY);
                stickPosX = posX;
                stickPosY = posY;
                break;
            case MotionEvent.ACTION_UP:
                checkOutDisplay();
                if (isGridVisible) alignToTheGrid();
                break;
        }
    }

    @Override
    public void onControl(MotionEvent event){
        int act = event.getAction();
        if (act == MotionEvent.ACTION_DOWN || act == MotionEvent.ACTION_POINTER_DOWN){
            if (touchedPointerID == -1)
                touchedPointerID = event.getPointerId(event.getActionIndex());
            else return;
        }
        else if (act == MotionEvent.ACTION_UP || act == MotionEvent.ACTION_POINTER_UP){
            Log.v("APP_TAG33", act + " " + event.getPointerId(event.getActionIndex()));
            Log.v("APP_TAG33", "" + touchedPointerID);
            if (event.getPointerId(event.getActionIndex()) == touchedPointerID){
                touchedPointerID = -1;
                stickPosX = posX;
                stickPosY = posY;
            }
            return;
        }
        else if (event.getPointerId(event.getActionIndex()) != touchedPointerID) return;

        float delta = (posX - event.getX())*(posX - event.getX()) + (posY - event.getY())
                *(posY - event.getY());    // вычисляем расстояние от точки касания до центра джойстика
        if (delta <= radius*radius){           // если точка касания внутри джойстика, то
            stickPosX = event.getX();   // перемещаем стик в точку касания
            stickPosY = event.getY();
        }
        else {      // иначе помещаем его в точке пересечения границы джойстика и прямой,
                            // проведенной между точкой касания и центром джойстика
            float k = (event.getY() - posY)/(event.getX() - posX);  // тангенс угла наклона прямой
            float x = (float) (sqrt((radius*radius - k*k)/2) + posX);  // х-координата новой точки стика
            float y = k*(x-posX)+posY;  // х-координата новой точки стика
            stickPosX = x;
            stickPosY = y;
        }
    }

    @Override
    public void alignToTheGrid(){
        if (gridParams == null) return;
        float left_X = posX-radius;                           // x-координата самой левой точки внешнего круга джойстика
        float node_X = (int)((left_X-gridParams.left)/gridParams.step)
                * gridParams.step + gridParams.left;          // x-координата ближайшего узла, левее джойстика
        float shift_X = left_X-node_X;          //???
        if (abs(gridParams.step - shift_X) < abs(shift_X))
            shift_X = shift_X - gridParams.step;
        posX -= shift_X;
        stickPosX = posX;

        float left_Y = posY-radius;                            // y-координата самой верхней точки внешнего круга джойстика
        float node_Y = (int) ((left_Y-gridParams.top)/gridParams.step)
                * gridParams.step + gridParams.top;            // y-координата ближайшего узла, выше джойстика
        float shift_Y = left_Y - node_Y;
        if (abs(gridParams.step - shift_Y) < abs(shift_Y))
            shift_Y = shift_Y - gridParams.step;
        posY -= shift_Y;
        stickPosY = posY;
    }

    @Override
    protected void checkOutDisplay(){
        if (posX < 0) posX = 0;
        else if (posX > gridParams.displayWidth) posX = gridParams.displayWidth;

        if (posY < 0) posY = 0;
        else if (posY > gridParams.displayHeight) posY = gridParams.displayHeight;
    }
}
