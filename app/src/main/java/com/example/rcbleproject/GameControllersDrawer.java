package com.example.rcbleproject;

import static com.example.rcbleproject.Container.currDisIdxKey;

import android.annotation.SuppressLint;
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
import com.example.rcbleproject.Database.DatabaseAdapterPortConnections;
import com.example.rcbleproject.Database.DatabaseAdapterProfilesControl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

@SuppressLint("ViewConstructor")
public class GameControllersDrawer extends SurfaceView implements SurfaceHolder.Callback {
    public final int maxNumOfDisplays;
    public final GridParams gridParams;
    private static ArrayList<ArrayList<BaseControlElement>> controlElements;
    private static List<List<PortConnection>>portConnections;
    private static ArrayList<Long> displayIDs;
    private static TreeMap<Long, BaseControlElement> controlElementTreeMap;

    private DrawingThread drawingThread;
    private final Paint paintBackground = new Paint();
    private final Paint paintGrid = new Paint();

    private final DatabaseAdapterElementsControl dbElementsControl;
    private final DatabaseAdapterProfilesControl dbProfilesControl;
    private final DatabaseAdapterDisplays dbDisplays;
    private final DatabaseAdapterPortConnections dbPortConnections;

    private final long profileID;
    private int countOfDisplays;
    private BaseControlElement focusableElement = null;
    private final ProfileControlActivity activity;
    private final HashMap<Integer, BaseControlElement> touchedElements = new HashMap<>();
    private Timer timerSenderCommands;

    private volatile boolean gridVisibility;
    private volatile int currentDisplayIndex;


    GameControllersDrawer(ProfileControlActivity profileControlActivity, DisplayMetrics displayMetrics,
                          DatabaseAdapterElementsControl dbAdapterElements,
                          DatabaseAdapterProfilesControl dbAdapterProfiles,
                          DatabaseAdapterDisplays dbDisplays, long profileID){
        super(profileControlActivity);
        activity = profileControlActivity;
        dbElementsControl = dbAdapterElements;
        dbProfilesControl = dbAdapterProfiles;
        dbPortConnections = Container.getDbPortConnections(activity);
        this.dbDisplays = dbDisplays;
        this.profileID = profileID;
        gridVisibility = dbAdapterProfiles.getProfileGridAlignment(profileID);

        gridParams = new GridParams(displayMetrics);
        paintBackground.setColor(profileControlActivity.getColor(R.color.honolulu_blue));

        paintGrid.setColor(Color.WHITE);
        paintGrid.setStyle(Paint.Style.FILL);

        maxNumOfDisplays = activity.getResources().getInteger(R.integer.maxNumOfDisplays);
    }

    public void startTimerSenderCmds(){
        timerSenderCommands = new Timer();
        timerSenderCommands.schedule(new TimerTask() {
            @Override
            public void run() {
                for (PortConnection portConn : portConnections.get(currentDisplayIndex)){
                    if (portConn.port == null || portConn.controllerAxis == null
                        || portConn.hub == null) continue;
                    if (portConn.port.portValue != portConn.controllerAxis.axisValue){
                        portConn.port.portValue = portConn.controllerAxis.axisValue;
                        Port port = portConn.port;
                        portConn.hub.setOutputPortCommand(activity, port.portNum, port.getDirection(),
                                portConn.controllerAxis.axisValue);
                    }
                }
            }
        }, 0, 1);
    }

    public void stopTimerSenderCmds(){
        timerSenderCommands.cancel();
    }

    public static ArrayList<ArrayList<BaseControlElement>> getElementsControl() {
        if (controlElements == null) controlElements = new ArrayList<>(getDisplayIDs().size());
        return controlElements;
    }

    public static ArrayList<Long> getDisplayIDs() {
        if (displayIDs == null) displayIDs = new ArrayList<>();
        return displayIDs;
    }

    public static BaseControlElement getElementByID(long elementID){
        if (controlElementTreeMap == null) return null;
        return controlElementTreeMap.get(elementID);
    }

    public int getCountOfDisplays(){ return countOfDisplays; }

    private void setFocusOnElementWithUpperIndex(){
        int len = controlElements.get(currentDisplayIndex).size();
        if (len <= 0) {
            setFocus(null);
            return;
        }
        setFocus(controlElements.get(currentDisplayIndex).get(len - 1));
    }

    public void updateElementsControl(){
        ProfileControlActivity profileControlActivity = (ProfileControlActivity)getContext();
        SharedPreferences prefs = (profileControlActivity)
                .getSharedPreferences(currDisIdxKey,Context.MODE_PRIVATE);
        currentDisplayIndex = prefs.getInt("current_display_index_"+profileID, 0);
        countOfDisplays = dbProfilesControl.getNumOfScreens(profileID);
        profileControlActivity.showCurrentDisplayNum(currentDisplayIndex, countOfDisplays);

        displayIDs = dbDisplays.getDisplaysByProfileID(profileID);
        controlElements = new ArrayList<>(maxNumOfDisplays);
        portConnections = new ArrayList<>(maxNumOfDisplays);
        controlElementTreeMap = new TreeMap<>();
        for (Long displayID : displayIDs){
            ArrayList<BaseControlElement> elements = dbElementsControl.getElementsControlByDisplayID(
                    getContext(), displayID, gridParams, gridVisibility);
            controlElements.add(elements);
            for (BaseControlElement element : elements)
                controlElementTreeMap.put(element.elementID, element);

            List<PortConnection> portConns = dbPortConnections.getPortConnectionsByDisplayID(
                    displayID, activity);
            portConnections.add((ArrayList) portConns);
        }
        setFocusOnElementWithUpperIndex();
    }

    @SuppressLint({"CommitPrefEdits", "ApplySharedPref"})
    public void saveElementsParams(){

        SharedPreferences prefs = ((ProfileControlActivity)getContext())
                .getSharedPreferences(currDisIdxKey, Context.MODE_PRIVATE);
        prefs.edit().putInt("current_display_index_"+profileID, currentDisplayIndex).commit();

        for (int i = displayIDs.size() - 1; i >= 0; --i){
            if (displayIDs.get(i) == null || displayIDs.get(i) < 0) dbDisplays.insert(profileID, i);
            else dbDisplays.updateIndexByID(displayIDs.get(i), i);
        }
        for (ArrayList<BaseControlElement> display : controlElements)
            dbElementsControl.updateAllRows(display);
        dbProfilesControl.updateProfileGridAlignment(profileID, gridVisibility);
        dbProfilesControl.updateProfileNumOfDisplays(profileID, countOfDisplays);
    }

    public void removeElementControl(){
        dbElementsControl.deleteElementControlByID(focusableElement.elementID);
        controlElements.get(currentDisplayIndex).remove(focusableElement.elementIndex);
        for (int i = 0; i < controlElements.get(currentDisplayIndex).size(); i++)
            controlElements.get(currentDisplayIndex).get(i).elementIndex = i;
        setFocusOnElementWithUpperIndex();
    }

    public void addDisplay(){
        countOfDisplays++;
        displayIDs.add(currentDisplayIndex+1, dbDisplays.insert(profileID, currentDisplayIndex));
        controlElements.add(currentDisplayIndex+1, new ArrayList<>());
        portConnections.add(new ArrayList<>());
        currentDisplayIndex++;
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

    public int getCurrentDisplayIndex() {
        return currentDisplayIndex;
    }

    public int getNumOfDisplays() {
        return countOfDisplays;
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
                    BaseControlElement element = elementsOnDisplay.get(i);
                    if (element.contains(event.getX(pointerIndex), event.getY(pointerIndex))){
                        element.onTouch(event, gridVisibility);
                        setFocus(element);
                        return;
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
                    BaseControlElement element = elementsOnDisplay.get(i);
                    if (element.contains(event.getX(pointerIndex), event.getY(pointerIndex))){
                        int pointerID = event.getPointerId(pointerIndex);
                        element.onControl(pointerID, event);
                        touchedElements.put(pointerID, element);
                        return;
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
        if (element != null) focusableElement.focus = true;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder){
        drawingThread = new DrawingThread(holder);
        drawingThread.start();
        if (BuildConfig.DEBUG) Log.v("APP_TAG2", "draw");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

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
                                element.onDraw(c, activity.getMode());
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
