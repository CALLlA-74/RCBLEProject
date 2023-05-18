package com.example.rcbleproject.Model;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.view.MotionEvent;

import com.example.rcbleproject.GridParams;
import com.example.rcbleproject.ViewAndPresenter.ProfileControlMenu.ProfileControlActivity;
import com.example.rcbleproject.R;
import com.example.rcbleproject.ViewAndPresenter.SettingPortConnectionsMenu.BaseParam;

import java.util.ArrayList;

/**
 * Класс BaseControlElement содержит поля и методы для взаимодействия с элементом управления
 */

public abstract class BaseControlElement {

    /**
     * Класс ControllerAxis включает поля и методы для взаимодействия с осью, как с базовым
     * параметром управляемого порта.
     * Ось - степень свободы движения элемента управления. Как правило, каждый элемент управления
     * имеет одну ось. Двухосевой джойстик - две. Также возможно и отсутствие осей (например вставки
     * текста и изображений).
     */
    public class ControllerAxis implements BaseParam {
        public final BaseControlElement parent;
        public final int axisNum;
        private final String axisName;

        public volatile int axisValue = 0;

        public ControllerAxis(BaseControlElement parent, String axisName, int axisNum, boolean hideAxisName){
            this.parent = parent;
            this.axisName = "#" + (parent.elementIndex + 1)
                    + (hideAxisName? "" : "\n" + parent.context.getString(R.string.axis_word) + " " + axisName);
            this.axisNum = axisNum;
        }

        @Override
        public String getName(){
            return axisName;
        }

        @Override
        public int getIconId(){
            return parent.getIconId();
        }

        @Override
        public int getMenuIconId() {return R.drawable.baseline_more_vert_20;}

        @Override
        public void act(Object obg){}

        @Override
        public boolean getAvailabilityForAct(){ return false; }
    }

    protected Context context;                  // используется для доступа к ресурсам приложения
    protected int pointerID = -1;               // id указателя, перехватившего управление элементом
    protected ArrayList<ControllerAxis> controllerAxes;  // массиив осей элемента управления

    protected volatile boolean isSettingsTouchedDown = false;  // флаг нажатия на настройки элемента
    protected volatile float posX, posY;        // координаты центра элемента управления
    public volatile int elementSize;            // коэффициент размера элемента управления
    public volatile int elementIndex;           // индекс элемента в списке элементов дисплея
    public volatile boolean isElementLocked;    // флаг блокировки элемента управления


    public enum ControlElementType {JOYSTICK_XY, JOYSTICK_X, JOYSTICK_Y, BUTTON, IMAGE, UNKNOWN}

    public final long elementID;                // id элемента в СУБД
    public final long displayID;                // id дисплея в СУБД, на котором находится элемент
    public final GridParams gridParams;         // параметры сетки для выравнивания на экране
    protected Bitmap bitmapLock,             // значок заблокированного элемента
                        bitmapSettings;         // значок параметров элемента

    protected String strResource = "";          // поле для хранения строкового ресурса элемента

    public boolean focus = false;               // флаг присутствия фокуса на элементе управления

    /**
     * Создает новый элемент управления с заданными параметрами.
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
    public BaseControlElement(long elementID, long displayID, Context context, GridParams gridParams,
                              int elementIndex, int elementSize, boolean isGridVisible,
                              boolean isElementLocked, float pX, float pY){
        this.elementID = elementID;
        this.displayID = displayID;
        this.context = context;
        this.gridParams = gridParams;
        this.elementIndex = elementIndex;
        this.elementSize = elementSize;
        this.isElementLocked = isElementLocked;
        controllerAxes = new ArrayList<>();

        bitmapLock = BitmapFactory.decodeResource(context.getResources(), R.drawable.baseline_lock_black_18);
        bitmapSettings = BitmapFactory.decodeResource(context.getResources(), R.drawable.settings_black);
        bitmapSettings = Bitmap.createScaledBitmap(bitmapSettings, bitmapLock.getWidth(),
                bitmapLock.getHeight(), false);

        posX = pX;
        posY = pY;
        this.strResource = strResource;
        if (isGridVisible) alignToTheGrid();
    }

    /**
     * Возвращает тип элемента управления.
     * @return тип элемента управления.
     */
    public abstract ControlElementType getType();

    /**
     * Возвращает название элемента управления.
     * @return название элемента управления.
     */
    public abstract String getName();

    /**
     * Устанавливает размер элемента управления.
     * @param newElementSize - новый коэффициент размера элемента.
     */
    public abstract void setElementSize(int newElementSize);

    /**
     * Отрисовывает элемент управления.
     * @param canvas - холст для отрисовки элемента.
     * @param mode - режим работы с профилем управления:
     *             GAME_MODE - игровой режим;
     *             EDIT_MODE - режим редактирования профиля;
     */
    public abstract void onDraw(Canvas canvas, ProfileControlActivity.MODE_TYPE mode);

    /**
     * Определяет находится ли указатель на элементе управления по координатам указателя.
     * @param pointerX - координата X указателя.
     * @param pointerY - координата Y указателя.
     * @return true - если указатель находится на элементе управления;
     *         false - в противном случае;
     */
    public abstract boolean contains(float pointerX, float pointerY);

    /**
     * Обрабатывает событие касания элемента в режиме редактирования профиля управления.
     * @param event - экземпляр жеста касания.
     * @param isToAlignToTheGrid - флаг режима выравнивания элементов по сетке.
     * @return true - если нажата кнопка настроек
     *         false - в ином случае
     */
    public abstract boolean onTouch(MotionEvent event, boolean isToAlignToTheGrid);

    /**
     * Обрабатывает событие касания элемента в игровом режиме работы с профилем управления.
     * @param touchedPointerID - id указателя.
     * @param event - экземпляр жеста касания.
     */
    public abstract void onControl(int touchedPointerID, MotionEvent event);

    /**
     * Выравнивает элемент управления по узлам сетки.
     */
    public abstract void alignToTheGrid();

    /**
     * Возвращает количество осей элемента управления.
     * @return количество осей элемента управления.
     */
    public abstract int getNumberOfAxes();

    /**
     * Возвращает массив названий осей элемента управления.
     * @return массив строк названий осей элемента управления.
     */
    public abstract String[] getAxesNames();

    /**
     * Возвращает id ресурса иконки элемента управления.
     * @return id ресурса иконки элемента управления.
     */
    public abstract int getIconId();

    /**
     * Предотвращает выход элемента управления за пределы видимой области экрана.
     */
    protected abstract void checkOutDisplay();

    /**
     * Определяет касание значка настроек на элементе
     * @param event параметры касания
     * @return true - если каснулись значка настроек
     *         false - в противном случае
     */
    protected abstract boolean onTouchSettings(MotionEvent event);

    /**
     * Возводит полученное значение во вторую степень.
     * @param value - значение для возведения в степень.
     * @return полученное значение во второй степени.
     */
    protected float square(float value){ return value*value; }

    /**
     * По заданным точкам p1, p2, p3 строит треугольник в экзепляре canvas
     * @param canvas - полотно для отрисовки в surface
     * @param paint - параметры отрисовки
     * @param p1 - 1-я вершина треугольника
     * @param p2 - 2-я вершина треугольника
     * @param p3 - 3-я вершина треугольника
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

    /**
     * @return значение x-координаты элемента управления.
     */
    public float getPosX() { return posX; }

    /**
     * @return значение y-координаты элемента управления.
     */
    public float getPosY() { return posY; }

    /**
     * @return значение коэффициента размера элемента управления.
     */
    public int getElementSize() { return elementSize; }

    /**
     * Возвращает элемент управления заданного типа.
     * @param elementType - тип создаваемого элемента управления.
     * @param elementID - id элемента в СУБД.
     * @param context - используется для доступа к ресурсам приложения.
     * @param gridParams - параметры сетки для выравнивания на экране.
     * @param elementIndex - индекс элемента в списке элементов дисплея.
     * @param elementSize - коэффициент размера элемента управления.
     * @param isGridVisible - флаг режима выравнивания элементов по сетке.
     * @param isElementLocked - флаг блокировки элемента управления.
     * @param pX - координата X центра элемента управления.
     * @param pY - координата Y центра элемента управления.
     * @return элемент управления заданного типа.
     */
    public static BaseControlElement getElementControl(ControlElementType elementType,
                                                       long elementID, long displayID,
                                                       Context context, GridParams gridParams,
                                                       int elementIndex, int elementSize,
                                                       boolean isGridVisible, boolean isElementLocked,
                                                       float pX, float pY, String strResource){
        switch (elementType){
            case JOYSTICK_XY:
                return new JoystickXY(elementID, displayID, context, gridParams, elementIndex,
                        elementSize, isGridVisible, isElementLocked, pX, pY);
            case JOYSTICK_X:
                return new JoystickX(elementID, displayID, context, gridParams, elementIndex,
                        elementSize, isGridVisible, isElementLocked, pX, pY);
            case JOYSTICK_Y:
                return new JoystickY(elementID, displayID, context, gridParams, elementIndex,
                        elementSize, isGridVisible, isElementLocked, pX, pY);
            case IMAGE:
                return new Image(elementID, displayID, context, gridParams, elementIndex,
                        elementSize, isGridVisible, isElementLocked, pX, pY, strResource);
        }
        return null;
    }

    /**
     * Возвращает список всех типов элементов управления с параметрами по умолчанию.
     * Подходит для отображения всех типов элементов управления.
     * @param context - используется для доступа к ресурсам приложения.
     * @return список всех типов элементов управления с параметрами по умолчанию.
     */
    public static ArrayList<BaseControlElement> getAllDefaultElementControlTypes(Context context,
                                                                                 long displayID,
                                                                                 int elementIndex,
                                                                                 float pX,
                                                                                 float pY){
        ArrayList<BaseControlElement> list = new ArrayList<>();
        int elementSize = context.getResources().getInteger(R.integer.defElementSize);
        list.add(new JoystickXY(context, displayID, elementIndex, elementSize, pX, pY));
        list.add(new JoystickX(context, displayID, elementIndex, elementSize, pX, pY));
        list.add(new JoystickY(context, displayID, elementIndex, elementSize, pX, pY));
        list.add(new Image(context, displayID, elementIndex, elementSize, pX, pY));
        return list;
    }

    /**
     * Вовзращает тип элемента управления из перечисления enum ControlElementType.
     * по его целочисленному представлению.
     * @param val - целочисленное представление типа элемента.
     * @return целочисленное представление типа элемента, иначе ControlElementType.UNKNOWN.
     */
    public static ControlElementType IntToControlElementType(int val){
        try {
            return ControlElementType.values()[val];
        } catch (Exception e){
            return ControlElementType.UNKNOWN;
        }
    }

    /**
     * Возвращает оси элемента управления.
     * @return список осей элемента управления.
     */
    public ArrayList<ControllerAxis> getControllerAxes(){
        return controllerAxes;
    }

    /**
     * Возвращает значение строкового ресурса.
     * @return значение поля strResource.
     */
    public String getStrResource() {
        return strResource;
    }

    /**
     * Возвращает флаг необходимости в доступе к галерее.
     * @return true - если нужен доступ к галерее.
     *         false - доступ не требуется.
     */
    public boolean getGalleryAccess(){
        return false;
    }
}
