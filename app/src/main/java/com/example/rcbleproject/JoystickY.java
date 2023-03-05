package com.example.rcbleproject;

import static java.lang.Math.abs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.view.MotionEvent;

import java.util.ArrayList;

/**
 * Класс JoystickY релизует поля и методы для взаимодействия с одно-осевым вертикальным
 * джойстиком.
 */
public class JoystickY extends BaseControlElement{
    private volatile float height,                // высота внешнего контура джойстика
                           stickPosX, stickPosY,  // координаты стика
                           stickRadius;           // радиус стика
    private volatile Paint paintBackground,       // параметры отрисовки внутреннего фона джойстика
                           paintBorder,           // параметры отрисовки границы джойстика
                           paintArrowAndStick,          // параметры отрисовки указателей и стика
                           paintTabletBoard;      // параметры отрисовки области для условных обозначений

    private static class Board {
        public volatile float right, left,
                              top, bottom,
                              cornerRadius;
        public volatile float x, y;
    }

    private Board numBoard, lockBoard, border;  // Параметры табличек для номера и с "замком"

    private class Triangle {
        public PointF p1, p2, p3;
    }

    private Triangle upArrow, downArrow;      // Координаты точек треугольных указателей

    public volatile float deltaX, deltaY;

    public JoystickY(long elementID, long displayID, Context context, GridParams gridParams,
                     int elementIndex, int elementSize, boolean isGridVisible,
                     boolean isElementLocked, float pX, float pY){
        super(elementID, displayID, context, gridParams, elementIndex, elementSize, isGridVisible,
                isElementLocked, pX, pY);

        if (isGridVisible) alignToTheGrid();
        stickPosX = pX;
        stickPosY = pY;

        String[] axisNames = getAxesNames();
        controllerAxes = new ArrayList<>(axisNames.length);
        for (short i = 0; i < axisNames.length; ++i)
            controllerAxes.add(new ControllerAxis(this, axisNames[i], i, true));

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
        border = new Board();

        upArrow = new Triangle();
        downArrow = new Triangle();

        setElementSize(elementSize);
        if (isGridVisible) alignToTheGrid();
    }

    /**
     * Создает новый одно-осевой горизонтальный джойстик с параметрами по умолчанию.
     * @param context - используется для доступа к ресурсам приложения.
     */
    public JoystickY(Context context, long displayID){
        super(-1, displayID, context, null, 0, 0,
                false,false,0 ,0 );
    }

    /**
     * Возврщает тип элемента управления.
     * @return тип элемента управления.
     */
    @Override
    public ControlElementType getType() { return ControlElementType.JOYSTICK_Y; }

    /**
     * Возвращает название элемента управления.
     * @return название элемента управления.
     */
    @Override
    public String getName() { return context.getString(R.string.joystick_y_name); }

    /**
     * Изменяет размер элемента управления.
     * @param newElementSize - новый коэффициент размера элемента.
     */
    @Override
    public void setElementSize(int newElementSize){
        elementSize = newElementSize;
        height = (10 + elementSize)*gridParams.step;
        stickRadius = 3 * gridParams.step / 2.f;
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
        canvas.drawRoundRect(border.left, border.top,
                             border.right, border.bottom,
                             border.cornerRadius, border.cornerRadius, paintBorder);
        canvas.drawRoundRect(border.left + 1, border.top + 1,
                            border.right - 1, border.bottom - 1,
                              border.cornerRadius - 1, border.cornerRadius - 1,
                                 paintBackground);
        drawTriangle(canvas, paintArrowAndStick, upArrow.p1, upArrow.p2, upArrow.p3);
        drawTriangle(canvas, paintArrowAndStick, downArrow.p1, downArrow.p2, downArrow.p3);

        if (mode == ProfileControlActivity.MODE_TYPE.EDIT_MODE){
            if (paintTabletBoard == null) return;
            String num = "#" + (elementIndex+1);

            if (focus) paintTabletBoard.setColor(context.getColor(R.color.yellow));
            else paintTabletBoard.setColor(context.getColor(R.color.white));
            paintBackground.setTextSize(gridParams.step);

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
        return pointerX >= border.left && pointerX <= border.right &&
                pointerY >= border.top && pointerY <= border.bottom;
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

        border.left = posX - stickRadius;
        border.right = posX + stickRadius;
        border.top = posY - height/2;
        border.bottom = posY + height/2;
        border.cornerRadius = stickRadius;

        String num = "#" + (elementIndex+1);
        Rect boundsNum = new Rect();
        paintBackground.getTextBounds(num, 0, num.length(), boundsNum);
        numBoard.right = posX + (boundsNum.width() + gridParams.step)/2.f;
        numBoard.left = numBoard.right - boundsNum.width() - gridParams.step;
        numBoard.top = posY - gridParams.step*2;
        numBoard.bottom = posY - gridParams.step*0.5f;
        numBoard.cornerRadius = gridParams.step*0.75f;
        numBoard.x = numBoard.left + gridParams.step*0.5f;
        numBoard.y = numBoard.bottom - gridParams.step*0.5f;//posY + gridParams.step*0.25f;

        lockBoard.right = posX + (boundsNum.width() + gridParams.step)/2.f;
        lockBoard.left = lockBoard.right - boundsNum.width() - gridParams.step;
        lockBoard.top = posY + gridParams.step*0.5f;
        lockBoard.bottom = posY + gridParams.step*2f;
        lockBoard.cornerRadius = numBoard.cornerRadius;
        lockBoard.x = lockBoard.left + gridParams.step*0.5f;
        lockBoard.y = (lockBoard.top + lockBoard.bottom)/2 - bitmapLock.getHeight()/2f;

        upArrow.p1 = new PointF(posX, posY-height/2+3*stickRadius*0.1f);
        upArrow.p2 = new PointF(posX+3*stickRadius*0.1f, posY-height/2+3*stickRadius*0.2f);
        upArrow.p3 = new PointF(posX-3*stickRadius*0.1f, posY-height/2+3*stickRadius*0.2f);

        downArrow.p1 = new PointF(posX, posY+height/2-3*stickRadius*0.1f);
        downArrow.p2 = new PointF(posX+3*stickRadius*0.1f, posY+height/2-3*stickRadius*0.2f);
        downArrow.p3 = new PointF(posX-3*stickRadius*0.1f, posY+height/2-3*stickRadius*0.2f);
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
        float halfHeight = height/2;

        if (abs(posY - pointerY) < (halfHeight - stickRadius))
            stickPosY = pointerY;
        else{
            if (pointerY < posY)
                stickPosY = posY - (halfHeight - stickRadius);
            else
                stickPosY = posY + (halfHeight - stickRadius);
        }

        /*TODO обновить значение в управляемых портах*/
    }

    /**
     * Выравнивает элемент управления по узлам сетки.
     */
    @Override
    public void alignToTheGrid(){
        if (gridParams == null) return;
        float left_X = posX-stickRadius;                  // x-координата самой левой точки внешнего круга джойстика
        float node_X = (int)((left_X-gridParams.left)/gridParams.step)
                * gridParams.step + gridParams.left;     // x-координата ближайшего узла, левее джойстика
        float shift_X = left_X-node_X;          //???
        if (abs(gridParams.step - shift_X) < abs(shift_X))
            shift_X = shift_X - gridParams.step;
        posX -= shift_X;
        stickPosX = posX;

        float left_Y = posY-height/2;                       // y-координата самой верхней точки внешнего круга джойстика
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
    public int getNumberOfAxes() { return 1; }

    /**
     * Возвращает названия осей элемента управления.
     * @return массив строк названий осей элемента управления.
     */
    @Override
    public String[] getAxesNames() {
        return new String[] {"Y"};
    }

    /**
     * Возвращает id ресурса иконки элемента управления.
     * @return id ресурса иконки элемента управления.
     */
    @Override
    public int getIconId() {
        return R.drawable.joystick_y;
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
