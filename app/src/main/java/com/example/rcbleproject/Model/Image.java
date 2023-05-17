package com.example.rcbleproject.Model;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;

import com.example.rcbleproject.GridParams;
import com.example.rcbleproject.R;
import com.example.rcbleproject.ViewAndPresenter.BaseAppActivity;
import com.example.rcbleproject.ViewAndPresenter.ProfileControlMenu.ProfileControlActivity;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;

public class Image extends BaseControlElement{

    private volatile float width,  height;        // размеры внешнего контура джойстика
    private volatile Paint paintBackground,       // параметры отрисовки внутреннего фона джойстика
            paintBorder,           // параметры отрисовки границы джойстика
            paintTabletBoard;      // параметры отрисовки области для условных обозначений

    private static class ImageConfig{
        private volatile Bitmap bitmapImage,        // изображение
                scaledBitmapImage;  // изображение, масштабированное под размеры рамки
        private volatile float hwcoef,       // отношение высоты к ширине изображения
                               left, top;    // параметры для отрисовки
    }

    private static class Board {
        public volatile float right, left,
                top, bottom,
                cornerRadius;
        public volatile float x, y;
    }

    private Board numBoard, lockBoard, // Параметры табличек для номера и с "замком"
            border, settingsBoard;     // Параметры границы элемента и таблички настроек
    private ImageConfig imageConfig;        // параметры изображения

    public volatile float deltaX, deltaY;

    public Image(long elementID, long displayID, Context context, GridParams gridParams,
                     int elementIndex, int elementSize, boolean isGridVisible,
                     boolean isElementLocked, float pX, float pY, String strResource){
        super(elementID, displayID, context, gridParams, elementIndex, elementSize, isGridVisible,
                isElementLocked, pX, pY);
        this.strResource = strResource;

        paintBackground = new Paint();
        paintBackground.setColor(context.getColor(R.color.black));
        paintBackground.setStyle(Paint.Style.FILL);
        paintBackground.setTextSize(gridParams.step);
        paintBackground.setAntiAlias(true);

        paintBorder = new Paint();
        paintBorder.setColor(context.getColor(R.color.white));
        paintBorder.setStyle(Paint.Style.FILL);
        paintBorder.setAntiAlias(true);

        paintTabletBoard = new Paint();
        paintTabletBoard.setColor(context.getColor(R.color.white));
        paintTabletBoard.setAntiAlias(true);

        numBoard = new Board();
        lockBoard = new Board();
        border = new Board();
        settingsBoard = new Board();

        imageConfig = new ImageConfig();
        /*imageConfig.bitmapImage = BitmapFactory
                .decodeResource(context.getResources(), R.drawable.image_icon);
        imageConfig.hwcoef = (float) imageConfig.bitmapImage.getHeight() / imageConfig
                .bitmapImage.getWidth();*/
        updateStrResource(strResource);

        setElementSize(elementSize);
        if (isGridVisible) alignToTheGrid();
    }

    /**
     * Создает новый одно-осевой горизонтальный джойстик с параметрами по умолчанию.
     * @param context - используется для доступа к ресурсам приложения.
     */
    public Image(Context context, long displayID, int elementIndex, int elementSize,
                     float pX, float pY){
        super(-1, displayID, context, null, elementIndex, elementSize,
                false,false, pX, pY);
    }

    /**
     * Возврщает тип элемента управления.
     * @return тип элемента управления.
     */
    @Override
    public ControlElementType getType() { return ControlElementType.IMAGE; }

    /**
     * Возвращает название элемента управления.
     * @return название элемента управления.
     */
    @Override
    public String getName() { return context.getString(R.string.image_name); }

    /**
     * Изменяет размер элемента управления.
     * @param newElementSize - новый коэффициент размера элемента.
     */
    @Override
    public void setElementSize(int newElementSize){
        elementSize = newElementSize;
        width = (2 + elementSize)*gridParams.step;
        height = width * imageConfig.hwcoef;
        float koeff = height/gridParams.step;
        koeff += (((int)(koeff * 1000))%1000 > 0? 1 : 0);
        height = gridParams.step * ((int)koeff);

        recalculateImageParams();
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
        if (canvas == null ||  paintBackground == null) return;
        if (paintBorder == null) return;
        canvas.drawRoundRect(border.left, border.top,
                border.right, border.bottom,
                border.cornerRadius, border.cornerRadius, paintBorder);
        canvas.drawRoundRect(border.left + 1, border.top + 1,
                border.right - 1, border.bottom - 1,
                border.cornerRadius - 1, border.cornerRadius - 1,
                paintBackground);
        canvas.drawBitmap(imageConfig.scaledBitmapImage, imageConfig.left, imageConfig.top,
                null);

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
        return pointerX >= min(border.left, numBoard.left)
                && pointerX <= max(border.right, settingsBoard.right)
                && pointerY >= border.top && pointerY <= border.bottom;
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
                recalculateImageParams();
                break;
            case MotionEvent.ACTION_UP:
                checkOutDisplay();
                if (isToAlignToTheGrid) alignToTheGrid();
                break;
        }

        return false;
    }

    /**
     * Пересчитывает координаты внутренних элементов изображения.
     */
    private void recalculateImageParams(){
        if (paintBackground == null) return;

        border.left = posX - width / 2;
        border.right = posX + width / 2;
        border.top = posY - height / 2;
        border.bottom = posY + height / 2;

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

        settingsBoard.left = posX + gridParams.step*0.5f;
        settingsBoard.right = settingsBoard.left + gridParams.step + bitmapSettings.getWidth();
        settingsBoard.top = numBoard.top;
        settingsBoard.bottom = numBoard.bottom;
        settingsBoard.cornerRadius = numBoard.cornerRadius;
        settingsBoard.x = settingsBoard.left + gridParams.step*0.5f;
        settingsBoard.y = posY - bitmapSettings.getHeight() / 2.f;

        lockBoard.left = posX - gridParams.step*0.5f - bitmapLock.getWidth()/2f;
        lockBoard.right = lockBoard.left + gridParams.step + bitmapLock.getWidth();
        lockBoard.top = posY + gridParams.step*1.25f;
        lockBoard.bottom = lockBoard.top + gridParams.step*1.5f;
        lockBoard.cornerRadius = numBoard.cornerRadius;
        lockBoard.x = lockBoard.left + gridParams.step*0.5f;
        lockBoard.y = lockBoard.top + gridParams.step*0.75f - bitmapLock.getHeight() / 2.f;

        recalculateImageConfigParams();
    }

    private void recalculateImageConfigParams(){
        int newWidthImg = (int)(width - 4),
                newHeightImg = (int)(height - 4);
        imageConfig.scaledBitmapImage = Bitmap.createScaledBitmap(imageConfig.bitmapImage,
                newWidthImg, newHeightImg, false);
        imageConfig.left = posX - (float) imageConfig.scaledBitmapImage.getWidth()/2;
        imageConfig.top = posY - (float) imageConfig.scaledBitmapImage.getHeight()/2;
    }

    /**
     * Обрабатывает событие касания элемента в игровом режиме работы с профилем управления.
     * @param touchedPointerID - id указателя.
     * @param event - экземпляр жеста касания.
     */
    @Override
    public void onControl(int touchedPointerID, MotionEvent event){}

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

        float left_Y = posY-height/2;                       // y-координата самой верхней точки внешнего круга джойстика
        float node_Y = (int) ((left_Y-gridParams.top)/gridParams.step)
                * gridParams.step + gridParams.top;            // y-координата ближайшего узла, выше джойстика
        float shift_Y = left_Y - node_Y;
        if (Math.abs(gridParams.step - shift_Y) < abs(shift_Y))
            shift_Y = shift_Y - gridParams.step;
        posY -= shift_Y;

        recalculateImageParams();
    }

    /**
     * Возвращает количество осей элемента управления.
     * @return количество осей элемента управления.
     */
    @Override
    public int getNumberOfAxes() { return 0; }

    /**
     * Возвращает названия осей элемента управления.
     * @return массив строк названий осей элемента управления.
     */
    @Override
    public String[] getAxesNames() {
        return new String[] {};
    }

    /**
     * Возвращает id ресурса иконки элемента управления.
     * @return id ресурса иконки элемента управления.
     */
    @Override
    public int getIconId() {
        return R.drawable.image_icon;
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

    /**
     * Обновляет значение строкового ресурса и изменяет изображение.
     * @param newResource - новое значение ресурса.
     */
    @Override
    public void updateStrResource(String newResource){
        strResource = newResource;
        if (imageConfig == null || newResource == null) return;
        Uri uri = Uri.parse(strResource);
        Log.v("APPTAG999999", "uri: " + uri);
        Log.v("APPTAG999999", "old bitmap: " + imageConfig.bitmapImage);
        //ParcelFileDescriptor parcelFileDescriptor = null;
        Drawable imgDrawable = null;
        InputStream inputStream = null;
        try {
            /*parcelFileDescriptor =
                    context.getContentResolver().openFileDescriptor(uri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            imageConfig.bitmapImage = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            parcelFileDescriptor.close();*/
            inputStream = context.getContentResolver().openInputStream(uri);
            imgDrawable = Drawable.createFromStream(inputStream, uri.toString());
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("APPTAG999999", "nooooo! " + e);
            imageConfig.bitmapImage = BitmapFactory
                    .decodeResource(context.getResources(), R.drawable.image_icon);
        } finally {
            /*try {
                if (parcelFileDescriptor != null) {
                    parcelFileDescriptor.close();
                }
            } catch (IOException e) {
                Log.e("APPTAG999999", "nooooo! nooo!" + e);
            }*/
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (imgDrawable != null){
            imageConfig.bitmapImage = ((BitmapDrawable)imgDrawable).getBitmap();
        }
        Log.v("APPTAG999999", "new bitmap:"+imageConfig.bitmapImage);
        Log.v("APPTAG999999", "old koeff:"+imageConfig.hwcoef);
        imageConfig.hwcoef = (float) imageConfig.bitmapImage.getHeight() / imageConfig
                .bitmapImage.getWidth();
        Log.v("APPTAG999999", "new koeff:"+imageConfig.hwcoef);
        recalculateImageConfigParams();
    }

    /**
     * Возвращает флаг необходимости в доступе к галерее.
     * @return true - если нужен доступ к галерее.
     *         false - доступ не требуется.
     */
    public boolean getGalleryAccess(){
        return true;
    }
}
