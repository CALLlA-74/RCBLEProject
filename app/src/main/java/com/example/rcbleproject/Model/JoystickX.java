package com.example.rcbleproject.Model;

import static java.lang.Math.abs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.view.MotionEvent;

import com.example.rcbleproject.GridParams;
import com.example.rcbleproject.ViewAndPresenter.ProfileControlMenu.ProfileControlActivity;
import com.example.rcbleproject.R;

import java.util.ArrayList;

/**
 * Класс JoystickX релизует поля и методы для взаимодействия с одно-осевым горизонтальным
 * джойстиком.
 */
public class JoystickX extends BaseControlElement{
    private volatile float width,                 // ширина внешнего контура джойстика
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

    private Board numBoard, lockBoard, // Параметры табличек для номера и с "замком"
            border, settingsBoard;     // Параметры границы элемента и таблички настроек

    private class Triangle {
        public PointF p1, p2, p3;
    }

    private Triangle leftArrow, rightArrow;      // Координаты точек треугольных указателей

    public volatile float deltaX, deltaY;

    public JoystickX(long elementID, long displayID, Context context, GridParams gridParams,
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
        settingsBoard = new Board();

        leftArrow = new Triangle();
        rightArrow = new Triangle();

        setElementSize(elementSize);
        if (isGridVisible) alignToTheGrid();
    }

    /**
     * Создает новый одно-осевой горизонтальный джойстик с параметрами по умолчанию.
     * @param context - используется для доступа к ресурсам приложения.
     */
    public JoystickX(Context context, long displayID, int elementIndex, int elementSize,
                     float pX, float pY){
        super(-1, displayID, context, null, elementIndex, elementSize,
                false,false, pX, pY);
    }

    /**
     * Возврщает тип элемента управления.
     * @return тип элемента управления.
     */
    @Override
    public ControlElementType getType() { return ControlElementType.JOYSTICK_X; }

    /**
     * Возвращает название элемента управления.
     * @return название элемента управления.
     */
    @Override
    public String getName() { return context.getString(R.string.joystick_x_name); }

    /**
     * Изменяет размер элемента управления.
     * @param newElementSize - новый коэффициент размера элемента.
     */
    @Override
    public void setElementSize(int newElementSize){
        elementSize = newElementSize;
        width = (20 + elementSize)*gridParams.step;
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
        drawTriangle(canvas, paintArrowAndStick, leftArrow.p1, leftArrow.p2, leftArrow.p3);
        drawTriangle(canvas, paintArrowAndStick, rightArrow.p1, rightArrow.p2, rightArrow.p3);

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

            canvas.drawRoundRect(settingsBoard.left, settingsBoard.top,
                    settingsBoard.right, settingsBoard.bottom,
                    settingsBoard.cornerRadius, settingsBoard.cornerRadius,
                    paintTabletBoard);
            canvas.drawBitmap(bitmapSettings, settingsBoard.x, settingsBoard.y, paintBackground);
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
     * @param isToAlignToTheGrid - флаг режима выравнивания элементов по сетке.
     * @return true - если нажата кнопка настроек
     *         false - в ином случае
     */
    @Override
    public boolean onTouch(MotionEvent event, boolean isToAlignToTheGrid){
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN){
            if (onTouchSettings(event)) {
                isSettingsTouchedDown = true;
            }
        }
        else if (event.getActionMasked() == MotionEvent.ACTION_UP){
            if (isSettingsTouchedDown && onTouchSettings(event)){
                isSettingsTouchedDown = false;
                return true;
            }
        }
        else {
            isSettingsTouchedDown = false;
        }

        if (isElementLocked) return false;

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
                if (isToAlignToTheGrid) alignToTheGrid();
                break;
        }

        return false;
    }

    /**
     * Пересчитывает координаты внутренних элементов джойстика.
     */
    private void recalculateJoystickParams(){
        if (paintBackground == null) return;

        border.left = posX - width / 2;
        border.right = posX + width / 2;
        border.top = posY - stickRadius;
        border.bottom = posY + stickRadius;
        border.cornerRadius = stickRadius;

        String num = "#" + (elementIndex+1);
        Rect boundsNum = new Rect();
        paintBackground.getTextBounds(num, 0, num.length(), boundsNum);
        numBoard.right = posX - gridParams.step*1.75f;
        numBoard.left = numBoard.right - boundsNum.width() - gridParams.step;
        numBoard.top = posY - gridParams.step*0.75f;
        numBoard.bottom = posY + gridParams.step*0.75f;
        numBoard.cornerRadius = gridParams.step*0.75f;
        numBoard.x = numBoard.left + gridParams.step*0.5f;
        numBoard.y = posY + gridParams.step*0.25f;

        lockBoard.left = posX - gridParams.step*0.75f;
        lockBoard.right = lockBoard.left + gridParams.step + bitmapLock.getWidth();
        lockBoard.top = numBoard.top;
        lockBoard.bottom = numBoard.bottom;
        lockBoard.cornerRadius = numBoard.cornerRadius;
        lockBoard.x = lockBoard.left + gridParams.step*0.5f;
        lockBoard.y = posY - bitmapLock.getHeight() / 2.f;

        settingsBoard.left = posX + gridParams.step*1.75f;
        settingsBoard.right = settingsBoard.left + gridParams.step + bitmapSettings.getWidth();
        settingsBoard.top = numBoard.top;
        settingsBoard.bottom = numBoard.bottom;
        settingsBoard.cornerRadius = numBoard.cornerRadius;
        settingsBoard.x = settingsBoard.left + gridParams.step*0.5f;
        settingsBoard.y = posY - bitmapSettings.getHeight() / 2.f;

        leftArrow.p1 = new PointF(posX-width/2+3*stickRadius*0.1f, posY);
        leftArrow.p2 = new PointF(posX-width/2+3*stickRadius*0.2f, posY+3*stickRadius*0.1f);
        leftArrow.p3 = new PointF(posX-width/2+3*stickRadius*0.2f, posY-3*stickRadius*0.1f);

        rightArrow.p1 = new PointF(posX+width/2-3*stickRadius*0.1f, posY);
        rightArrow.p2 = new PointF(posX+width/2-3*stickRadius*0.2f, posY+3*stickRadius*0.1f);
        rightArrow.p3 = new PointF(posX+width/2-3*stickRadius*0.2f, posY-3*stickRadius*0.1f);
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
                controllerAxes.get(0).axisValue = 0;
            }
            return;
        }
        else if (act == MotionEvent.ACTION_MOVE){
            if (pointerID != touchedPointerID) return;
        }

        int pointerIndex = event.findPointerIndex(pointerID);
        float pointerX = event.getX(pointerIndex);
        float halfWidth = width/2;

        if (abs(posX - pointerX) < (halfWidth - stickRadius))
            stickPosX = pointerX;
        else{
            if (pointerX < posX)
                stickPosX = posX - (halfWidth - stickRadius);
            else
                stickPosX = posX + (halfWidth - stickRadius);
        }

        controllerAxes.get(0).axisValue = (int)((posX - stickPosX)/(halfWidth - stickRadius)*100);
    }

    /**
     * Выравнивает элемент управления по узлам сетки.
     */
    @Override
    public void alignToTheGrid(){
        if (gridParams == null) return;
        float left_X = posX-width/2;                     // x-координата самой левой точки внешнего круга джойстика
        float node_X = (int)((left_X-gridParams.left)/gridParams.step)
                * gridParams.step + gridParams.left;     // x-координата ближайшего узла, левее джойстика
        float shift_X = left_X-node_X;          //???
        if (Math.abs(gridParams.step - shift_X) < abs(shift_X))
            shift_X = shift_X - gridParams.step;
        posX -= shift_X;
        stickPosX = posX;

        float left_Y = posY-stickRadius;                       // y-координата самой верхней точки внешнего круга джойстика
        float node_Y = (int) ((left_Y-gridParams.top)/gridParams.step)
                * gridParams.step + gridParams.top;            // y-координата ближайшего узла, выше джойстика
        float shift_Y = left_Y - node_Y;
        if (Math.abs(gridParams.step - shift_Y) < abs(shift_Y))
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
        return new String[] {"X"};
    }

    /**
     * Возвращает id ресурса иконки элемента управления.
     * @return id ресурса иконки элемента управления.
     */
    @Override
    public int getIconId() {
        return R.drawable.joystick_x;
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

    /**
     * Определяет касание значка настроек на элементе
     * @param event параметры касания
     * @return true - если каснулись значка настроек
     *         false - в противном случае
     */
    @Override
    protected boolean onTouchSettings(MotionEvent event){
        return event.getX() >= settingsBoard.left && event.getX() <= settingsBoard.right
                && event.getY() >= settingsBoard.top && event.getY() <= settingsBoard.bottom;
    }
}
