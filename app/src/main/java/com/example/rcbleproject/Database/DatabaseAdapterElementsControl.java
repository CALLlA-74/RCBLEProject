package com.example.rcbleproject.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.rcbleproject.BaseAppActivity;
import com.example.rcbleproject.BaseControlElement;
import com.example.rcbleproject.GridParams;

import java.util.ArrayList;

public class DatabaseAdapterElementsControl extends DatabaseAdapter{
    public static final String TABLE_NAME = "elements_control";
    public static final String ID = "_id";
    public static final String ELEMENT_NUMBER = "element_number";
    public static final String DISPLAY_ID = "display_id";
    public static final String ELEMENT_TYPE = "element_type";
    public static final String ELEMENT_SIZE = "element_size";
    public static final String ELEMENT_BLOCKING = "element_blocking";
    public static final String NUMBER_OF_AXES = "number_of_axis";
    public static final String X_COORDINATE = "x_coordinate";
    public static final String Y_COORDINATE = "y_coordinate";

    public DatabaseAdapterElementsControl(BaseAppActivity context){
        super(context);
        open();
    }

    public static void createTable(SQLiteDatabase db){
        db.execSQL("CREATE TABLE enum_types_of_element (_id INTEGER PRIMARY KEY AUTOINCREMENT);");
        for (int i = 0; i < BaseControlElement.ControlElementType.values().length; i++){
            ContentValues contentValues = new ContentValues();
            contentValues.put("_id", i);
            db.insert("enum_types_of_element", null, contentValues);
        }

        db.execSQL("CREATE TABLE " + TABLE_NAME
                + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + ELEMENT_NUMBER + " INTEGER NOT NULL CHECK(" + ELEMENT_NUMBER + " >= 0), "
                + DISPLAY_ID + " INTEGER NOT NULL, "
                + ELEMENT_TYPE + " INTEGER NOT NULL, "
                + ELEMENT_SIZE + " INTEGER DEFAULT 4 CHECK(" + ELEMENT_SIZE + " >= 0 AND "
                    + ELEMENT_SIZE + " <= 20), "
                + ELEMENT_BLOCKING + " INTEGER DEFAULT 0 CHECK(" + ELEMENT_BLOCKING + " = 0 OR "
                    + ELEMENT_BLOCKING + " = 1), "
                + NUMBER_OF_AXES + " INTEGER DEFAULT 1 CHECK(" + NUMBER_OF_AXES + " = 1 OR "
                    + NUMBER_OF_AXES + " = 2),"
                + X_COORDINATE + " REAL NOT NULL, "
                + Y_COORDINATE + " REAL NOT NULL, "
                + "FOREIGN KEY (" + DISPLAY_ID +") REFERENCES "
                    + DatabaseAdapterDisplays.TABLE_NAME + "("
                        + DatabaseAdapterDisplays.ID + ") ON DELETE CASCADE, "
                + "FOREIGN KEY (" + ELEMENT_TYPE + ") REFERENCES enum_types_of_element (_id), "
                + "UNIQUE(" + DISPLAY_ID + ", " + ELEMENT_NUMBER + "));");
    }

    public long insert(int elementNumber, long displayId, int type, int size, int numOfAxes,
                       float x_coordinate, float y_coordinate){
        ContentValues contentValues = new ContentValues();
        contentValues.put(ELEMENT_NUMBER, elementNumber);
        contentValues.put(DISPLAY_ID, displayId);
        contentValues.put(ELEMENT_TYPE, type);
        contentValues.put(ELEMENT_SIZE, size);
        contentValues.put(NUMBER_OF_AXES, numOfAxes);
        contentValues.put(X_COORDINATE, x_coordinate);
        contentValues.put(Y_COORDINATE, y_coordinate);
        Log.v("APP_TAG3", "number:" + elementNumber);
        Log.v("APP_TAG3", "type:" + type);
        Log.v("APP_TAG3", "size:" + size);
        Log.v("APP_TAG3", "X, Y:" + x_coordinate + " " + y_coordinate);
        return database.insert(TABLE_NAME, null, contentValues);
    }

    public ArrayList<BaseControlElement> getElementsControlByDisplayID(Context context,
                                                                       long displayID,
                                                                       GridParams params,
                                                                       boolean isGridVisible){
        ArrayList<BaseControlElement> list = new ArrayList<>();

        Cursor cursor = getAllRowsByDisplayID(displayID);
        while (cursor.moveToNext()){
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(ID));
            int typeOfElement = cursor.getInt(cursor.getColumnIndexOrThrow(ELEMENT_TYPE));
            int elementIndex = cursor.getInt(cursor.getColumnIndexOrThrow(ELEMENT_NUMBER));
            int elementSize = cursor.getInt(cursor.getColumnIndexOrThrow(ELEMENT_SIZE));
            boolean elementBlocking = cursor.getInt(cursor.getColumnIndexOrThrow(ELEMENT_BLOCKING)) != 0;
            float posX = cursor.getFloat(cursor.getColumnIndexOrThrow(X_COORDINATE));
            float posY = cursor.getFloat(cursor.getColumnIndexOrThrow(Y_COORDINATE));
            list.add(BaseControlElement.getElementControl(BaseControlElement.IntToControlElementType(typeOfElement),
                                                          id, displayID, context, params, elementIndex,
                                                          elementSize, isGridVisible,
                                                          elementBlocking, posX, posY));
        }

        list.sort((BaseControlElement o1, BaseControlElement o2) -> {
            return o1.elementIndex - o2.elementIndex;
        });
        return list;
    }

    public void updateAllRows(ArrayList<BaseControlElement> elements){
        synchronized (DatabaseAdapterElementsControl.class){
            for (int j = 0; j < elements.size(); j++){
                ContentValues contentValues = new ContentValues();
                contentValues.put(ELEMENT_NUMBER, j);
                contentValues.put(ELEMENT_TYPE, elements.get(j).getType().ordinal());
                contentValues.put(ELEMENT_SIZE, elements.get(j).elementSize);
                contentValues.put(ELEMENT_BLOCKING, elements.get(j).isElementLocked);
                contentValues.put(NUMBER_OF_AXES, elements.get(j).getNumberOfAxes());
                contentValues.put(X_COORDINATE, elements.get(j).getPosX());
                contentValues.put(Y_COORDINATE, elements.get(j).getPosY());
                database.update(TABLE_NAME, contentValues,
                        ID + " = " + elements.get(j).elementID, null);
            }
        }

    }

    public synchronized long deleteElementControlByID(long elementID){
        return database.delete(TABLE_NAME, ID + " = " +  elementID, null);
    }

    public String[] getColumns(){
        return new String[]{ID, ELEMENT_NUMBER, DISPLAY_ID, ELEMENT_TYPE, ELEMENT_SIZE,
                ELEMENT_BLOCKING, NUMBER_OF_AXES, X_COORDINATE, Y_COORDINATE};
    }

    private Cursor getAllRowsByDisplayID(long displayID){
        return database.query(TABLE_NAME, getColumns(), DISPLAY_ID + " = " + displayID,
                null, null, null, ELEMENT_NUMBER + " ASC");
    }
}
