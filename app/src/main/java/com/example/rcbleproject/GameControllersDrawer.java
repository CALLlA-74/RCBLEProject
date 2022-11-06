package com.example.rcbleproject;

import android.annotation.SuppressLint;
import android.app.usage.UsageEvents;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.example.rcbleproject.Database.DatabaseAdapterDisplays;
import com.example.rcbleproject.Database.DatabaseAdapterElementsControl;
import com.example.rcbleproject.Database.DatabaseAdapterProfilesControl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@SuppressLint("ViewConstructor")
public class GameControllersDrawer extends SurfaceView implements SurfaceHolder.Callback {
    public static final int maxDisplays = 5;

    private DrawingThread drawingThread;
    private final Paint paintBackground = new Paint();
    private final Paint paintGrid = new Paint();
    private final DatabaseAdapterElementsControl dbAdapterElementsControl;
    private final DatabaseAdapterProfilesControl dbAdapterProfilesControl;
    private final DatabaseAdapterDisplays dbDisplays;
    private final long profileID;
    private ArrayList<ArrayList<BaseControlElement>> controlElements;
    private ArrayList<Long> displayIDs;
    private int currentDisplayIndex;
    private int countOfDisplays;
    private BaseControlElement focusableElement = null;
    private final ProfileControlActivity activity;
    private final HashMap<Integer, BaseControlElement> touchedElements = new HashMap<>();

    public final GridParams gridParams;
    private volatile boolean gridVisibility;


    GameControllersDrawer(ProfileControlActivity profileControlActivity, DisplayMetrics displayMetrics,
                          DatabaseAdapterElementsControl dbAdapterElements,
                          DatabaseAdapterProfilesControl dbAdapterProfiles,
                          DatabaseAdapterDisplays dbDisplays, long profileID){
        super(profileControlActivity);
        activity = profileControlActivity;
        dbAdapterElementsControl = dbAdapterElements;
        dbAdapterProfilesControl = dbAdapterProfiles;
        this.dbDisplays = dbDisplays;
        this.profileID = profileID;
        gridVisibility = dbAdapterProfiles.getProfileGridAlignment(profileID);

        gridParams = new GridParams(displayMetrics);
        paintBackground.setColor(profileControlActivity.getColor(R.color.honolulu_blue));

        paintGrid.setColor(Color.WHITE);
        paintGrid.setStyle(Paint.Style.FILL);

        //Log.v("APP_TAG2", controlElements.get(0).getType().toString());
    }

    public int getCountOfDisplays(){ return countOfDisplays; }

    private void setFocusOnElementWithUpperIndex(){
        int len = controlElements.get(currentDisplayIndex).size();
        if (len <= 0) return;
        setFocus(controlElements.get(currentDisplayIndex).get(len - 1));
    }

    public void updateElementsControl(){
        ProfileControlActivity profileControlActivity = (ProfileControlActivity)getContext();
        SharedPreferences prefs = (profileControlActivity).getPreferences(Context.MODE_PRIVATE);
        currentDisplayIndex = prefs.getInt("current_display_index_"+profileID, 0);
        countOfDisplays = dbAdapterProfilesControl.getNumOfScreens(profileID);
        profileControlActivity.showCurrentDisplayNum(currentDisplayIndex, countOfDisplays);

        displayIDs = dbDisplays.getDisplaysByProfileID(profileID);
        controlElements = new ArrayList<>();
        for (Long displayID : displayIDs){
            ArrayList<BaseControlElement> elements = dbAdapterElementsControl.getElementsControlByDisplayID(
                    getContext(), displayID, gridParams, countOfDisplays, gridVisibility);
            controlElements.add(elements);
        }
        setFocusOnElementWithUpperIndex();
    }

    @SuppressLint("CommitPrefEdits")
    public void saveElementsParams(){
        SharedPreferences prefs = ((ProfileControlActivity)getContext()).getPreferences(Context.MODE_PRIVATE);
        prefs.edit().putInt("current_display_index_"+profileID, currentDisplayIndex).commit();

        for (int i = 0; i < displayIDs.size(); i++){
            if (displayIDs.get(i) == null) dbDisplays.insert(profileID, i);
            else dbDisplays.updateIndexByID(displayIDs.get(i), i);
        }
        for (ArrayList<BaseControlElement> display : controlElements)
            dbAdapterElementsControl.updateAllRows(display);
        dbAdapterProfilesControl.updateProfileGridAlignment(profileID, gridVisibility);
        dbAdapterProfilesControl.updateProfileNumOfDisplays(profileID, countOfDisplays);
    }

    public void removeElementControl(){
        dbAdapterElementsControl.deleteElementControlByID(focusableElement.elementID);
        controlElements.get(currentDisplayIndex).remove(focusableElement.elementIndex);
        for (int i = 0; i < controlElements.get(currentDisplayIndex).size(); i++)
            controlElements.get(currentDisplayIndex).get(i).elementIndex = i;
        setFocusOnElementWithUpperIndex();
    }

    public void addDisplay(){
        countOfDisplays++;
        currentDisplayIndex++;
        displayIDs.add(currentDisplayIndex, dbDisplays.insert(profileID, currentDisplayIndex));
        controlElements.add(currentDisplayIndex, new ArrayList<>());
        ((ProfileControlActivity)getContext()).showCurrentDisplayNum(currentDisplayIndex, countOfDisplays);
        setFocusOnElementWithUpperIndex();
    }

    public void removeDisplay(){
        dbDisplays.deleteDisplayByID(displayIDs.get(currentDisplayIndex));
        if (currentDisplayIndex >= countOfDisplays - 1) {
            currentDisplayIndex--;
            controlElements.remove(currentDisplayIndex + 1);
            displayIDs.remove(currentDisplayIndex + 1);
        }
        else {
            controlElements.remove(currentDisplayIndex);
            displayIDs.remove(currentDisplayIndex);
        }
        countOfDisplays--;
        ((ProfileControlActivity)getContext()).showCurrentDisplayNum(currentDisplayIndex, countOfDisplays);
        setFocusOnElementWithUpperIndex();
    }

    public void nextDisplay(){
        currentDisplayIndex++;
        if (currentDisplayIndex >= countOfDisplays) currentDisplayIndex = 0;
        ((ProfileControlActivity)getContext()).showCurrentDisplayNum(currentDisplayIndex, countOfDisplays);
        setFocusOnElementWithUpperIndex();
    }

    public void prevDisplay(){
        currentDisplayIndex--;
        if (currentDisplayIndex < 0) currentDisplayIndex = countOfDisplays - 1;
        ((ProfileControlActivity)getContext()).showCurrentDisplayNum(currentDisplayIndex, countOfDisplays);
        setFocusOnElementWithUpperIndex();
    }

    public void setGridVisibility(boolean visibility){
        if (drawingThread == null) return;
        if (gridVisibility != visibility){
            ArrayList<BaseControlElement> elementsOnCurrentDisplay = controlElements.get(currentDisplayIndex);
            for (BaseControlElement controlElement: elementsOnCurrentDisplay)
                controlElement.alignToTheGrid();
        }
        gridVisibility = visibility;
    }

    public boolean getGridVisibility(){
        return gridVisibility;
    }
    public String[] getElementAxesNames() { return focusableElement.getAxesNames(); }
    public long getFocusableElementID() { return focusableElement.elementID; }
    public boolean isFocused() {
        if (focusableElement == null) return false;
        return true;
    }

    public boolean getElementLocking() { return focusableElement.isElementLocked; }
    public void setElementLocking(boolean locking){ focusableElement.isElementLocked = locking; }

    public long getCurrentDisplayID() { return displayIDs.get(currentDisplayIndex); }
    public int getCountOfElements() { return controlElements.get(currentDisplayIndex).size(); }

    public int getElementSize() { return focusableElement.getElementSize(); }
    public void setElementSize(int newElementSize) {
        if (focusableElement == null) return;
        focusableElement.setElementSize(newElementSize);
    }

    public void onTouch(View view, MotionEvent event){
        ArrayList<BaseControlElement> elementsOnDisplay = controlElements.get(currentDisplayIndex);
        int act = event.getActionMasked();
        int pointerIndex = event.getActionIndex();
        if (activity.getMode() == ProfileControlActivity.MODE_TYPE.EDIT_MODE){
            if (act == MotionEvent.ACTION_DOWN){
                for (int i = elementsOnDisplay.size() - 1; i >= 0; i--){
                    switch (elementsOnDisplay.get(i).getType()){
                        case JOYSTICK_XY:
                            JoystickXY element = (JoystickXY) elementsOnDisplay.get(i);
                            if (element.contains(event.getX(pointerIndex), event.getY(pointerIndex))){
                                element.onTouch(event, gridVisibility);
                                setFocus(element);
                                return;
                            }
                            break;
                        case JOYSTICK_X:
                        case JOYSTICK_Y:
                    }
                }
            }
            else if (focusableElement != null && focusableElement.contains(event.getX(pointerIndex), event.getY(pointerIndex))) {
                focusableElement.onTouch(event, gridVisibility);
            }
        }
        else {
            if (act == MotionEvent.ACTION_POINTER_DOWN || act == MotionEvent.ACTION_DOWN){
                for (int i = elementsOnDisplay.size() - 1; i >= 0; i--){
                    switch (elementsOnDisplay.get(i).getType()){
                        case JOYSTICK_XY:
                            JoystickXY element = (JoystickXY) elementsOnDisplay.get(i);
                            if (element.contains(event.getX(pointerIndex), event.getY(pointerIndex))){
                                int pointerID = event.getPointerId(pointerIndex);
                                element.onControl(pointerID, event);
                                touchedElements.put(pointerID, element);
                                return;
                            }
                            break;
                        case JOYSTICK_X:
                        case JOYSTICK_Y:
                    }
                }
            }
            else if (act == MotionEvent.ACTION_UP || act == MotionEvent.ACTION_POINTER_UP) {
                int pointerID = event.getPointerId(event.getActionIndex());
                if (touchedElements.containsKey(pointerID)) {
                    touchedElements.get(pointerID).onControl(pointerID, event);
                    touchedElements.remove(pointerID);
                }
            }
            else {
                for (int idx = 0; idx < event.getPointerCount(); idx++){
                    int pointerID = event.getPointerId(idx);
                    if (touchedElements.containsKey(pointerID)){
                        touchedElements.get(pointerID).onControl(pointerID, event);
                    }
                }
            }
        }
    }

    public void setFocus(BaseControlElement element){
        if (focusableElement != null) focusableElement.focus = false;
        focusableElement = element;
        focusableElement.focus = true;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder){
        drawingThread = new DrawingThread(holder);
        drawingThread.start();
        if (BuildConfig.DEBUG) Log.v("APP_TAG2", "draw");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (BuildConfig.DEBUG) Log.v("APP_TAG2", "draw update");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        drawingThread.setStop();
        boolean retry = true;
        while (retry){
            try {
                drawingThread.join();
                retry = false;
            }
            catch (InterruptedException e){
                if (BuildConfig.DEBUG)
                    Log.e(getResources().getString(R.string.app_tag), e.toString());
            }
        }
        if (BuildConfig.DEBUG) Log.v("APP_TAG2", "clear");
        drawingThread = null;
    }



    class DrawingThread extends Thread{
        private SurfaceHolder holder;
        private volatile boolean running;

        DrawingThread(SurfaceHolder holder){
            running = true;
            this.holder = holder;
        }

        public void setStop(){
            running = false;
        }

        @Override
        public void run(){
            while (running){
                Canvas c = holder.lockCanvas();
                if (c != null){
                    try{
                        c.drawRect(0, 0, c.getWidth(), c.getHeight(), paintBackground);
                        if (gridVisibility && activity.getMode() == ProfileControlActivity.MODE_TYPE.EDIT_MODE){
                            for (int i = gridParams.left; i <= c.getWidth(); i+= gridParams.step)
                                for (int j = gridParams.top; j <= c.getHeight(); j+= gridParams.step){
                                    c.drawCircle(i, j, 1, paintGrid);
                                }
                        }
                        if (currentDisplayIndex < controlElements.size()){
                            ArrayList<BaseControlElement> elementsOnDisplay = controlElements.get(currentDisplayIndex);
                            for (BaseControlElement element : elementsOnDisplay){
                                switch (element.getType()){
                                    case JOYSTICK_XY:
                                        ((JoystickXY)element).onDraw(c, activity.getMode());
                                        break;
                                    case JOYSTICK_X:
                                    case JOYSTICK_Y:
                                }
                            }
                        }
                    }
                    finally {
                        holder.unlockCanvasAndPost(c);
                    }
                }
            }
        }
    }
}
