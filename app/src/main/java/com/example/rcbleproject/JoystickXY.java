package com.example.rcbleproject;

import static java.lang.Math.abs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;

import java.util.ArrayList;

/**
 * Класс JoystickXY релизует поля и методы для взаимодействия с двух-осевым джойстиком
 */
public class JoystickXY extends BaseControlElement{
    private volatile float radius,                // радиус внешнего контура джойстика
                           stickPosX, stickPosY,  // координаты стика
                           stickRadius;           // радиус стика
    private volatile Paint paintBackground,       // параметры отрисовки внутреннего фона джойстика
                           paintBorder,           // параметры отрисовки границы джойстика
                           paintArrowAndStick,    // параметры отрисовки указателей и стика
                           paintTabletBoard;      // параметры отрисовки табличек условных обозначений

    private static class Board {
        public volatile float right, left,
                              top, bottom,
                              cornerRadius;
        public volatile float x, y;
    }

    private Board numBoard, lockBoard;  // Параметры табличек для номера и с "замком"

    private class Triangle {
        public PointF p1, p2, p3;
    }

    private Triangle upArrow, downArrow,         // Координаты точек треугольных указателей
                     leftArrow, rightArrow;

    public volatile float deltaX, deltaY;

    /**
     * Создает новый двух-осевой джойстик с заданными параметрами.
     * @param elementID - id элемента в СУБД.
     * @param context - используется для доступа к ресурсам приложения.
     * @param gridParams - параметры сетки для выравнивания на экране.
     * @param elementIndex - индекс элемента в списке элементов дисплея.
     * @param elementSize - коэффициент размера элемента управления.
     * @param isGridVisible - флаг режима выравнивания элементов по сетке.
     * @param isElementLocked - флаг блокировки элемента управления.
     * @param pX - координата X центра элемента управления.
     * @param pY - координата Y центра элемента управления.
     */
    public JoystickXY(long elementID, long displayID, Context context, GridParams gridParams,
                      int elementIndex, int elementSize, boolean isGridVisible,
                      boolean isElementLocked,  float pX, float pY){
        super(elementID, displayID, context, gridParams, elementIndex, elementSize, isGridVisible,
                isElementLocked, pX, pY);
        stickPosX = pX;
        stickPosY = pY;

        String[] axisNames = getAxesNames();
        controllerAxes = new ArrayList<>(axisNames.length);
        for (short i = 0; i < axisNames.length; ++i)
            controllerAxes.add(new ControllerAxis(this, axisNames[i], i, false));

        paintBackground = new Paint();
        paintBackground.setColor(context.getColor(R.color.black));
        paintBackground.setStyle(Paint.Style.FILL);
        paintBackground.setTextSize(gridParams.step);
        paintBackground.setAntiAlias(true);

        paintBorder = new Paint();
        paintBorder.setColor(context.getColor(R.color.white));
        paintBorder.setStyle(Paint.Style.FILL);
        paintBorder.setAntiAlias(true);

        paintArrowAndStick = new Paint();
        paintArrowAndStick.setColor(context.getColor(R.color.white));
        paintArrowAndStick.setStyle(Paint.Style.FILL);
        paintArrowAndStick.setAntiAlias(true);

        paintTabletBoard = new Paint();
        paintTabletBoard.setColor(context.getColor(R.color.white));
        paintTabletBoard.setAntiAlias(true);

        numBoard = new Board();
        lockBoard = new Board();

        upArrow = new Triangle();
        downArrow = new Triangle();
        leftArrow = new Triangle();
        rightArrow = new Triangle();

        setElementSize(elementSize);
        if (isGridVisible) alignToTheGrid();
    }

    /**
     * Создает новый двух-осевой джойстик с параметрами по умолчанию.
     * @param context - используется для доступа к ресурсам приложения.
     */
    public JoystickXY(Context context, long displayID){
        super(-1, displayID, context, null, 0, 0,
                false,false,0 ,0 );
    }

    /**
     * Возврщает тип элемента управления.
     * @return тип элемента управления.
     */
    @Override
    public ControlElementType getType() { return ControlElementType.JOYSTICK_XY; }

    /**
     * Возвращает название элемента управления.
     * @return название элемента управления.
     */
    @Override
    public String getName() { return context.getString(R.string.joystick_xy_name); }

    /**
     * Изменяет размер элемента управления.
     * @param newElementSize - новый коэффициент размера элемента.
     */
    @Override
    public void setElementSize(int newElementSize){
        elementSize = newElementSize;
        radius = (6 + elementSize)/2f*gridParams.step;
        stickRadius = radius / 3;
        recalculateJoystickParams();
    }

    /**
     * Отрисовывает элемент управления.
     * @param canvas - холст для отрисовки элемента.
     * @param mode - режим работы с профилем управления:
     *             GAME_MODE - игровой режим;
     *             EDIT_MODE - режим редактирования профиля;
     */
    @SuppressLint("DrawAllocation")
    @Override
    public void onDraw(Canvas canvas, ProfileControlActivity.MODE_TYPE mode){
        if (canvas == null || paintArrowAndStick == null || paintBackground == null) return;
        if (paintBorder == null) return;
        canvas.drawCircle(posX, posY, radius, paintBorder);
        canvas.drawCircle(posX, posY, radius - 1, paintBackground);
        drawTriangle(canvas, paintArrowAndStick, upArrow.p1, upArrow.p2, upArrow.p3);
        drawTriangle(canvas, paintArrowAndStick, downArrow.p1, downArrow.p2, downArrow.p3);
        drawTriangle(canvas, paintArrowAndStick, leftArrow.p1, leftArrow.p2, leftArrow.p3);
        drawTriangle(canvas, paintArrowAndStick, rightArrow.p1, rightArrow.p2, rightArrow.p3);

        if (mode == ProfileControlActivity.MODE_TYPE.EDIT_MODE){
            if (paintTabletBoard == null) return;
            String num = "#" + (elementIndex+1);

            if (focus) paintTabletBoard.setColor(context.getColor(R.color.yellow));
            else paintTabletBoard.setColor(context.getColor(R.color.white));

            if (isElementLocked) {
                canvas.drawRoundRect(lockBoard.left, lockBoard.top,
                                     lockBoard.right, lockBoard.bottom,
                                     lockBoard.cornerRadius, lockBoard.cornerRadius,
                                     paintTabletBoard);
                canvas.drawBitmap(bitmapLock, lockBoard.x, lockBoard.y,
                                  paintBackground);
            }

            canvas.drawRoundRect(numBoard.left, numBoard.top,
                                 numBoard.right, numBoard.bottom,
                                 numBoard.cornerRadius, numBoard.cornerRadius,
                                 paintTabletBoard);
            canvas.drawText(num, numBoard.x, numBoard.y, paintBackground);
        }
        else {
            canvas.drawCircle(stickPosX, stickPosY, stickRadius, paintArrowAndStick);
        }
    }

    /**
     * Определяет находится ли указатель на элементе управления по координатам указателя.
     * @param pointerX - координата X указателя.
     * @param pointerY - координата Y указателя.
     * @return true - если указатель находится на элементе управления;
     *         false - в противном случае;
     */
    @Override
    public boolean contains(float pointerX, float pointerY){
        return square(pointerX - posX) + square(pointerY - posY) <= square(radius);
    }

    /**
     * Обрабатывает событие касания элемента в режиме редактирования профиля управления.
     * @param event - экземпляр жеста касания.
     * @param isGridVisible - флаг режима выравнивания элементов по сетке.
     */
    @Override
    public void onTouch(MotionEvent event, boolean isGridVisible){
        if (isElementLocked) return;

        int pointerIndex = event.getActionIndex();
        float pointerX = event.getX(pointerIndex);
        float pointerY = event.getY(pointerIndex);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_POINTER_UP:
                deltaX = event.getX(pointerIndex)-posX;
                deltaY = event.getY(pointerIndex)-posY;
                break;
            case MotionEvent.ACTION_MOVE:
                posX = (int)(pointerX - deltaX);
                posY = (int)(pointerY - deltaY);
                stickPosX = posX;
                stickPosY = posY;
                recalculateJoystickParams();
                break;
            case MotionEvent.ACTION_UP:
                checkOutDisplay();
                if (isGridVisible) alignToTheGrid();
                break;
        }
    }

    /**
     * Пересчитывает координаты внутренних элементов джойстика.
     */
    private void recalculateJoystickParams(){
        if (paintBackground == null) return;

        String num = "#" + (elementIndex+1);
        Rect boundsNum = new Rect();
        paintBackground.getTextBounds(num, 0, num.length(), boundsNum);
        numBoard.right = posX - gridParams.step*0.5f;
        numBoard.left = numBoard.right - boundsNum.width() - gridParams.step;
        numBoard.top = posY - gridParams.step*0.75f;
        numBoard.bottom = posY + gridParams.step*0.75f;
        numBoard.cornerRadius = gridParams.step*0.75f;
        numBoard.x = numBoard.left + gridParams.step*0.5f;
        numBoard.y = posY + gridParams.step*0.25f;

        lockBoard.left = posX + gridParams.step*0.5f;
        lockBoard.right = lockBoard.left + gridParams.step + bitmapLock.getWidth();
        lockBoard.top = numBoard.top;
        lockBoard.bottom = numBoard.bottom;
        lockBoard.cornerRadius = numBoard.cornerRadius;
        lockBoard.x = lockBoard.left + gridParams.step*0.5f;
        lockBoard.y = posY - bitmapLock.getHeight() / 2.f;

        upArrow.p1 = new PointF(posX, posY-radius*0.95f);
        upArrow.p2 = new PointF(posX-radius*0.1f, posY-radius*0.85f);
        upArrow.p3 = new PointF(posX+radius*0.1f, posY-radius*0.85f);

        downArrow.p1 = new PointF(posX, posY+radius*0.95f);
        downArrow.p2 = new PointF(posX-radius*0.1f, posY+radius*0.85f);
        downArrow.p3 = new PointF(posX+radius*0.1f, posY+radius*0.85f);

        leftArrow.p1 = new PointF(posX-radius*0.95f, posY);
        leftArrow.p2 = new PointF(posX-radius*0.85f, posY+radius*0.1f);
        leftArrow.p3 = new PointF(posX-radius*0.85f, posY-radius*0.1f);

        rightArrow.p1 = new PointF(posX+radius*0.95f, posY);
        rightArrow.p2 = new PointF(posX+radius*0.85f, posY+radius*0.1f);
        rightArrow.p3 = new PointF(posX+radius*0.85f, posY-radius*0.1f);
    }

    /**
     * Обрабатывает событие касания элемента в игровом режиме работы с профилем управления.
     * @param touchedPointerID - id указателя.
     * @param event - экземпляр жеста касания.
     */
    @Override
    public void onControl(int touchedPointerID, MotionEvent event){
        int act = event.getActionMasked();
        if (act == MotionEvent.ACTION_DOWN || act == MotionEvent.ACTION_POINTER_DOWN){
            if (pointerID == -1)
                pointerID = touchedPointerID;
            else if (pointerID != touchedPointerID) return;
        }
        else if (act == MotionEvent.ACTION_UP || act == MotionEvent.ACTION_POINTER_UP){
            if (pointerID == touchedPointerID){
                pointerID = -1;
                stickPosX = posX;
                stickPosY = posY;
            }
            /*TODO обновить значение в управляемых портах*/
            return;
        }
        else if (act == MotionEvent.ACTION_MOVE){
            if (pointerID != touchedPointerID) return;
        }

        int pointerIndex = event.findPointerIndex(pointerID);
        float pointerX = event.getX(pointerIndex);
        float pointerY = event.getY(pointerIndex);

        if (abs(posX - pointerX) < (radius - stickRadius))
            stickPosX = pointerX;
        else{
            if (pointerX < posX)
                stickPosX = posX - (radius - stickRadius);
            else
                stickPosX = posX + (radius - stickRadius);
        }

        if (abs(posY - pointerY) < (radius - stickRadius))
            stickPosY = pointerY;
        else{
            if (pointerY < posY)
                stickPosY = posY - (radius - stickRadius);
            else
                stickPosY = posY + (radius - stickRadius);
        }

        /*TODO обновить значение в управляемых портах*/
    }

    /**
     * Выравнивает элемент управления по узлам сетки.
     */
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

        recalculateJoystickParams();
    }

    /**
     * Возвращает количество осей элемента управления.
     * @return количество осей элемента управления.
     */
    @Override
    public int getNumberOfAxes() { return 2; }

    /**
     * Возвращает названия осей элемента управления.
     * @return массив строк названий осей элемента управления.
     */
    @Override
    public String[] getAxesNames() {
        return new String[] {"X", "Y"};
    }

    /**
     * Возвращает id ресурса иконки элемента управления.
     * @return id ресурса иконки элемента управления.
     */
    @Override
    public int getIconId() {
        return R.drawable.joystick_xy;
    }

    /**
     * Предотвращает выход элемента управления за пределы видимой области экрана более, чем
     * на половину своих длины и ширины.
     */
    @Override
    protected void checkOutDisplay(){
        if (posX < 0) posX = 0;
        else if (posX > gridParams.displayWidth) posX = gridParams.displayWidth;

        if (posY < 0) posY = 0;
        else if (posY > gridParams.displayHeight) posY = gridParams.displayHeight;
    }
}
