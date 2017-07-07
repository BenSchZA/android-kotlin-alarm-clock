/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.sqlutil;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import com.roostermornings.android.sqldata.DeviceAlarmTableHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.roostermornings.android.sqldata.DeviceAlarmTableContract.AlarmTableEntry;

/**
 * Created by bscholtz on 2/14/17.
 */

public class DeviceAlarmTableManager {

    private static SQLiteDatabase sqLiteDatabase;

    private DeviceAlarm alarm;
    private Context context;

    //SQL Arguments
    private String TrueFalse = "0";
    private final static String TRUE = "1";
    private final static String FALSE = "0";

    public DeviceAlarmTableManager(Context context) {
        this.context = context;
        alarm = new DeviceAlarm();
    }

    Boolean insertAlarm(DeviceAlarm alarm, String setId) {

        SQLiteDatabase db = initDB();

        alarm.setSetId(setId);
        alarm.setChanged(true);

        ContentValues values = new ContentValues();

        values.put(AlarmTableEntry.COLUMN_SET_ID, alarm.getSetId());

        values.put(AlarmTableEntry.COLUMN_ENABLED, alarm.getEnabled());
        values.put(AlarmTableEntry.COLUMN_HOUR, alarm.getHour());
        values.put(AlarmTableEntry.COLUMN_MINUTE, alarm.getMinute());
        values.put(AlarmTableEntry.COLUMN_DAY, alarm.getDay());
        values.put(AlarmTableEntry.COLUMN_RECURRING, alarm.getRecurring());
        values.put(AlarmTableEntry.COLUMN_MILLIS, alarm.getMillis());
        values.put(AlarmTableEntry.COLUMN_ITERATION, 1);
        values.put(AlarmTableEntry.COLUMN_CHANNEL, alarm.getChannel());
        values.put(AlarmTableEntry.COLUMN_SOCIAL, alarm.isSocial());
        values.put(AlarmTableEntry.COLUMN_LABEL, alarm.getLabel());

        values.put(AlarmTableEntry.COLUMN_CHANGED, alarm.getChanged());

        try {
            // Inserting Row
            db.insertOrThrow(AlarmTableEntry.TABLE_NAME, null, values);
            return true;
        } catch (SQLiteConstraintException e) {
            e.printStackTrace();
            return false;
        }
    }

    void updateAlarmMillis(int piId, long Millis) {
        SQLiteDatabase db = initDB();
        String updateQuery = "UPDATE " + AlarmTableEntry.TABLE_NAME + " SET " + AlarmTableEntry.COLUMN_MILLIS + " = " + Millis + " WHERE " + AlarmTableEntry.COLUMN_PI_ID + " = " + piId + ";";

        db.execSQL(updateQuery);
    }

    public Boolean isSetInDB(String setId) {
        SQLiteDatabase db = initDB();

        String selectQuery = "SELECT * FROM " + AlarmTableEntry.TABLE_NAME + " WHERE " + AlarmTableEntry.COLUMN_SET_ID + " = '" + setId + "';";

        Cursor cursor = db.rawQuery(selectQuery, null);

        Boolean isSet = (cursor.getCount() > 0);
        cursor.close();

        return isSet;
    }

    public ArrayList<Integer> getAlarmClassDays(String setId) {
        ArrayList<DeviceAlarm> deviceAlarms = getAlarmSet(setId);
        ArrayList<Integer> alarmDays = new ArrayList<>();
        for (DeviceAlarm deviceAlarm:
             deviceAlarms) {
            alarmDays.add(deviceAlarm.getDay());
        }
        return alarmDays;
    }

    public ArrayList<DeviceAlarm> selectChanged() {
        SQLiteDatabase db = initDB();

        ArrayList<DeviceAlarm> alarmList;

        String selectQuery = "SELECT * FROM " + AlarmTableEntry.TABLE_NAME + " WHERE " + AlarmTableEntry.COLUMN_CHANGED + " = " + TRUE + ";";

        Cursor cursor = db.rawQuery(selectQuery, null);

        alarmList = extractAlarms(cursor);

        cursor.close();

        return alarmList;
    }

    public void updateAlarmLabel(String label) {
        //Update the label of the next occuring alarm to show whether a download has failed, or other notices
        DeviceAlarm deviceAlarm = getNextPendingAlarm();
        String setId = "";
        if(deviceAlarm != null) {
            setId = deviceAlarm.getSetId();
            if(setId == null) return;
        }

        SQLiteDatabase db = initDB();
        String updateQuery = "UPDATE " + AlarmTableEntry.TABLE_NAME + " SET " + AlarmTableEntry.COLUMN_LABEL + " = '" + label + "' WHERE " + AlarmTableEntry.COLUMN_SET_ID + " LIKE \"%" + setId + "%\";";

        db.execSQL(updateQuery);
    }

    ArrayList<DeviceAlarm> selectEnabled() {
        SQLiteDatabase db = initDB();

        ArrayList<DeviceAlarm> alarmList;

        String selectQuery = "SELECT * FROM " + AlarmTableEntry.TABLE_NAME + " WHERE " + AlarmTableEntry.COLUMN_ENABLED + " = " + TRUE + ";";

        Cursor cursor = db.rawQuery(selectQuery, null);

        alarmList = extractAlarms(cursor);

        cursor.close();

        return alarmList;
    }

    public void setAlarmChanged(long piId) {
        SQLiteDatabase db = initDB();
        String updateQuery = "UPDATE " + AlarmTableEntry.TABLE_NAME + " SET " + AlarmTableEntry.COLUMN_CHANGED + " = " + TRUE + " WHERE " + AlarmTableEntry.COLUMN_PI_ID + " = " + piId + ";";
        db.execSQL(updateQuery);
    }

    public void setAlarmEnabled(long piId, boolean enabled) {
        SQLiteDatabase db = initDB();
        if(enabled){
            TrueFalse = TRUE;
        }
        else{
            TrueFalse = FALSE;
        }
        String updateQuery = "UPDATE " + AlarmTableEntry.TABLE_NAME + " SET " + AlarmTableEntry.COLUMN_ENABLED + " = " + TrueFalse + " WHERE " + AlarmTableEntry.COLUMN_PI_ID + " = " + piId + ";";
        db.execSQL(updateQuery);
    }

    private ArrayList<DeviceAlarm> selectSetEnabled(String setId) {
        SQLiteDatabase db = initDB();

        ArrayList<DeviceAlarm> alarmList;

        String selectQuery = "SELECT * FROM " + AlarmTableEntry.TABLE_NAME + " WHERE " + AlarmTableEntry.COLUMN_ENABLED + " = " + TRUE + " AND " + AlarmTableEntry.COLUMN_SET_ID + " LIKE \"%" + setId + "%\";";

        Cursor cursor = db.rawQuery(selectQuery, null);

        alarmList = extractAlarms(cursor);

        cursor.close();

        return alarmList;
    }

    public boolean isSetEnabled(String setId) {
        try{
            return getAlarmSet(setId) != null
                    && getAlarmSet(setId).size() > 0
                    && getAlarmSet(setId).size() == selectSetEnabled(setId).size();
        } catch(NullPointerException e) {
            e.printStackTrace();
            return false;
        }
    }

    void setSetEnabled(String setId, boolean enabled) {
        SQLiteDatabase db = initDB();
        if(enabled){
            TrueFalse = TRUE;
        }
        else{
            TrueFalse = FALSE;
        }
        String updateQuery = "UPDATE " + AlarmTableEntry.TABLE_NAME + " SET " + AlarmTableEntry.COLUMN_ENABLED + " = " + TrueFalse + " WHERE " + AlarmTableEntry.COLUMN_SET_ID + " LIKE \"%" + setId + "%\";";
        db.execSQL(updateQuery);
    }

    void setSetChanged(String setId, boolean enabled) {
        if(enabled){
            TrueFalse = TRUE;
        }
        else{
            TrueFalse = FALSE;
        }
        SQLiteDatabase db = initDB();
        String updateQuery = "UPDATE " + AlarmTableEntry.TABLE_NAME + " SET " + AlarmTableEntry.COLUMN_CHANGED + " = " + TrueFalse + " WHERE " + AlarmTableEntry.COLUMN_SET_ID + " LIKE \"%" + setId + "%\";";
        db.execSQL(updateQuery);

    }

    public void setChannelStoryIteration(String channelId, Integer iteration) {
        SQLiteDatabase db = initDB();

        String updateQuery = "UPDATE " + AlarmTableEntry.TABLE_NAME + " SET " + AlarmTableEntry.COLUMN_ITERATION + " = " + String.valueOf(iteration) + " WHERE " + AlarmTableEntry.COLUMN_CHANNEL + " = '" + channelId + "';";

        db.execSQL(updateQuery);
    }

    public Integer getChannelStoryIteration(String channelId) {
        SQLiteDatabase db = initDB();

        String selectQuery = "SELECT * FROM " + AlarmTableEntry.TABLE_NAME + " WHERE " + AlarmTableEntry.COLUMN_CHANNEL + " = '" + channelId + "';";

        Cursor cursor = db.rawQuery(selectQuery, null);
        if(!checkValidCursor(cursor)) return null;
        cursor.moveToFirst();

        Integer storyIteration = cursor.getInt(cursor.getColumnIndex(AlarmTableEntry.COLUMN_ITERATION));

        cursor.close();
        return storyIteration;
    }

    void clearChanged() {
        SQLiteDatabase db = initDB();
        String updateQuery = "UPDATE " + AlarmTableEntry.TABLE_NAME + " SET " + AlarmTableEntry.COLUMN_CHANGED + " = " + FALSE + ";";
        db.execSQL(updateQuery);
    }

    public DeviceAlarm getNextPendingAlarm() {
        SQLiteDatabase db = initDB();

        //String selectQuery = "SELECT MIN(" + AlarmTableEntry.COLUMN_MILLIS + ") FROM " + AlarmTableEntry.TABLE_NAME + " WHERE " + AlarmTableEntry.COLUMN_ENABLED + " = " + TRUE + ";";
        String selectQuery = "SELECT * FROM " + AlarmTableEntry.TABLE_NAME + " WHERE " + AlarmTableEntry.COLUMN_ENABLED + " = " + TRUE + " ORDER BY " + AlarmTableEntry.COLUMN_MILLIS + " ASC LIMIT 1;";
        Cursor cursor = db.rawQuery(selectQuery, null);
        if(!checkValidCursor(cursor)) return null;
        ArrayList<DeviceAlarm> alarmList = extractAlarms(cursor);

        cursor.close();
        return alarmList.get(0);
    }

    public Long getMillisOfNextPendingAlarmInSet(String setId) {
        SQLiteDatabase db = initDB();

        String selectQuery = "SELECT * FROM " + AlarmTableEntry.TABLE_NAME +
                " WHERE " + AlarmTableEntry.COLUMN_SET_ID + " LIKE \"%" + setId + "%\"" + " ORDER BY " + AlarmTableEntry.COLUMN_MILLIS + " ASC LIMIT 1;";

        Cursor cursor = db.rawQuery(selectQuery, null);
        if(!checkValidCursor(cursor)) return null;
        ArrayList<DeviceAlarm> alarmList = extractAlarms(cursor);

        cursor.close();
        return alarmList.get(0).getMillis();
    }

    ArrayList<DeviceAlarm> getAlarmSets() {
        SQLiteDatabase db = initDB();

        String selectQuery = "SELECT * FROM " + AlarmTableEntry.TABLE_NAME + " WHERE " + AlarmTableEntry.COLUMN_SET_ID + " IN (SELECT DISTINCT " + AlarmTableEntry.COLUMN_SET_ID + " AS id FROM " + AlarmTableEntry.TABLE_NAME + " ORDER BY " + AlarmTableEntry.COLUMN_SET_ID + " ASC);";

        Cursor cursor = db.rawQuery(selectQuery, null);

        ArrayList<DeviceAlarm> alarmSets = extractAlarms(cursor);

        cursor.close();
        return alarmSets;
    }

    public ArrayList<DeviceAlarm> getAlarmSet(String setId) {
        SQLiteDatabase db = initDB();

        ArrayList<DeviceAlarm> alarmList;

        String selectQuery = "SELECT * FROM " + AlarmTableEntry.TABLE_NAME +
                " WHERE " + AlarmTableEntry.COLUMN_SET_ID + " LIKE \"%" + setId + "%\";";

        Cursor cursor = db.rawQuery(selectQuery, null);
        alarmList = extractAlarms(cursor);

        cursor.close();
        return alarmList;
    }

    private void deleteAlarm(long piId) {
        SQLiteDatabase db = initDB();

        String execSql = "DELETE FROM " + AlarmTableEntry.TABLE_NAME + " WHERE " + AlarmTableEntry.COLUMN_PI_ID + " = " + piId + ";";

        db.execSQL(execSql);
    }

    void deleteAlarmSet(String setId) {
        SQLiteDatabase db = initDB();

        String execSql = "DELETE FROM " + AlarmTableEntry.TABLE_NAME + " WHERE " + AlarmTableEntry.COLUMN_SET_ID + " LIKE \"%" + setId + "%\";";

        db.execSQL(execSql);
    }

    public int countAlarmSets() {
        SQLiteDatabase db = initDB();

        String selectQuery = "SELECT COUNT(DISTINCT " + AlarmTableEntry.COLUMN_SET_ID + ") AS c FROM " + AlarmTableEntry.TABLE_NAME + ";";

        Cursor cursor = db.rawQuery(selectQuery, null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);

        cursor.close();

        return count;
    }

    private SQLiteDatabase initDB() {
        DeviceAlarmTableHelper dbHelper = DeviceAlarmTableHelper.getInstance(context);
        return dbHelper.getWritableDatabase();
    }

    private Boolean checkValidCursor(Cursor cursor) {
        return ((cursor != null) && (cursor.getCount() > 0));
    }

    private ArrayList<DeviceAlarm> extractAlarms(Cursor cursor) {
        ArrayList<DeviceAlarm> alarmList = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                alarm = new DeviceAlarm();

                alarm.setSetId(cursor.getString(cursor.getColumnIndex(AlarmTableEntry.COLUMN_SET_ID)));
                alarm.setPiId(cursor.getInt(cursor.getColumnIndex(AlarmTableEntry.COLUMN_PI_ID)));

                alarm.setHour(cursor.getInt(cursor.getColumnIndex(AlarmTableEntry.COLUMN_HOUR)));
                alarm.setMinute(cursor.getInt(cursor.getColumnIndex(AlarmTableEntry.COLUMN_MINUTE)));
                alarm.setDay(cursor.getInt(cursor.getColumnIndex(AlarmTableEntry.COLUMN_DAY)));
                alarm.setRecurring(cursor.getInt(cursor.getColumnIndex(AlarmTableEntry.COLUMN_RECURRING)) > 0);
                alarm.setMillis(cursor.getLong(cursor.getColumnIndex(AlarmTableEntry.COLUMN_MILLIS)));

                alarm.setSocial(cursor.getInt(cursor.getColumnIndex(AlarmTableEntry.COLUMN_SOCIAL)) > 0);
                alarm.setChannel(cursor.getString(cursor.getColumnIndex(AlarmTableEntry.COLUMN_CHANNEL)));
                alarm.setLabel(cursor.getString(cursor.getColumnIndex(AlarmTableEntry.COLUMN_LABEL)));
                alarm.setEnabled(cursor.getInt(cursor.getColumnIndex(AlarmTableEntry.COLUMN_ENABLED)) > 0);
                alarm.setChanged(cursor.getInt(cursor.getColumnIndex(AlarmTableEntry.COLUMN_CHANGED)) > 0);

                alarm.setDateCreated(cursor.getString(cursor.getColumnIndex(AlarmTableEntry.COLUMN_DATE_CREATED)));
                alarmList.add(alarm);

            } while (cursor.moveToNext());
        }

        cursor.close();

        return alarmList;
    }

}
