package com.roostermornings.android.sqldata;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.roostermornings.android.domain.Alarm;
import com.roostermornings.android.sqlutil.DeviceAlarm;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.roostermornings.android.sqldata.DeviceAlarmTableContract.AlarmTableEntry;

/**
 * Created by bscholtz on 2/14/17.
 */

public class DeviceAlarmTableManager {

    private DeviceAlarm alarm;
    private Context context;
    private Calendar calendar = Calendar.getInstance();

    //SQL Arguments
    private String TrueFalse = "0";
    private final static String TRUE = "1";
    private final static String FALSE = "0";

    public DeviceAlarmTableManager(Context context) {
        this.context = context;
        alarm = new DeviceAlarm();
    }

    public void insertAlarm(DeviceAlarm alarm, long setId) {

        SQLiteDatabase db = initDB();

        alarm.setSetId(setId);
        alarm.setChanged(true);

        ContentValues values = new ContentValues();

        values.put(AlarmTableEntry.COLUMN_SET_ID, alarm.getSetId());

        values.put(AlarmTableEntry.COLUMN_HOUR, alarm.getHour());
        values.put(AlarmTableEntry.COLUMN_MINUTE, alarm.getMinute());
        values.put(AlarmTableEntry.COLUMN_DAY, alarm.getDay());
        values.put(AlarmTableEntry.COLUMN_RECURRING, alarm.getRecurring());
        values.put(AlarmTableEntry.COLUMN_VIBRATE, alarm.getVibrate());
        values.put(AlarmTableEntry.COLUMN_MILLIS, alarm.getMillis());

        values.put(AlarmTableEntry.COLUMN_CHANGED, alarm.getChanged());

        // Inserting Row
        db.insert(AlarmTableEntry.TABLE_NAME, null, values);
        db.close();
    }

    public void updateAlarmMillis(int piId, long Millis) {
        SQLiteDatabase db = initDB();
        String updateQuery = "UPDATE " + AlarmTableEntry.TABLE_NAME + " SET " + AlarmTableEntry.COLUMN_MILLIS + " = " + Millis + " WHERE " + AlarmTableEntry.COLUMN_PI_ID + " = " + piId + ";";
        db.execSQL(updateQuery);
        db.close();
    }

    public List<DeviceAlarm> selectChanged() {
        SQLiteDatabase db = initDB();

        List<DeviceAlarm> alarmList;

        String selectQuery = "SELECT * FROM " + AlarmTableEntry.TABLE_NAME + " WHERE " + AlarmTableEntry.COLUMN_CHANGED + " = " + TRUE + ";";

        Cursor cursor = db.rawQuery(selectQuery, null);

        alarmList = extractAlarms(cursor);

        cursor.close();
        db.close();

        return alarmList;
    }

    public List<DeviceAlarm> selectEnabled() {
        SQLiteDatabase db = initDB();

        List<DeviceAlarm> alarmList;

        String selectQuery = "SELECT * FROM " + AlarmTableEntry.TABLE_NAME + " WHERE " + AlarmTableEntry.COLUMN_ENABLED + " = " + TRUE + ";";

        Cursor cursor = db.rawQuery(selectQuery, null);

        alarmList = extractAlarms(cursor);

        cursor.close();
        db.close();

        return alarmList;
    }

    public void setAlarmChanged(long piId) {
        SQLiteDatabase db = initDB();
        String updateQuery = "UPDATE " + AlarmTableEntry.TABLE_NAME + " SET " + AlarmTableEntry.COLUMN_CHANGED + " = " + TRUE + " WHERE " + AlarmTableEntry.COLUMN_PI_ID + " = " + piId + ";";
        db.execSQL(updateQuery);
        db.close();
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
        db.close();
    }

    public void clearChanged(List<DeviceAlarm> alarmList) {
        SQLiteDatabase db = initDB();

        for (DeviceAlarm alarm :
                alarmList) {
            String updateQuery = "UPDATE " + AlarmTableEntry.TABLE_NAME + " SET " + AlarmTableEntry.COLUMN_CHANGED + " = " + FALSE + " WHERE " + AlarmTableEntry.COLUMN_PI_ID + " = " + alarm.getPiId() + ";";
            db.execSQL(updateQuery);
        }
        db.close();
    }

    private void deleteAlarm(long piId) {
        SQLiteDatabase db = initDB();

        String execSql = "DELETE FROM " + AlarmTableEntry.TABLE_NAME + " WHERE " + AlarmTableEntry.COLUMN_PI_ID + " = " + piId + ";";

        db.execSQL(execSql);
        db.close();
    }

    public Cursor getAlarms() {
        SQLiteDatabase db = initDB();

        String selectQuery = "SELECT * FROM " + AlarmTableEntry.TABLE_NAME + ";";

        return db.rawQuery(selectQuery, null);
    }

    public DeviceAlarm getNextPendingAlarm() {
        SQLiteDatabase db = initDB();

        String selectQuery = "SELECT MIN(" + AlarmTableEntry.COLUMN_MILLIS + ") FROM " + AlarmTableEntry.TABLE_NAME + " WHERE " + AlarmTableEntry.COLUMN_ENABLED + " = TRUE;";

        Cursor cursor = db.rawQuery(selectQuery, null);
        List<DeviceAlarm> alarmList = extractAlarms(cursor);

        return alarmList.get(0);
    }

    public Cursor getAlarmSets() {
        SQLiteDatabase db = initDB();

        String selectQuery = "SELECT DISTINCT " + AlarmTableEntry.COLUMN_SET_ID + " AS _id," + AlarmTableEntry.COLUMN_SET_ID + "," + AlarmTableEntry.COLUMN_HOUR + "," + AlarmTableEntry.COLUMN_MINUTE + "," + AlarmTableEntry.COLUMN_ENABLED + " FROM " + AlarmTableEntry.TABLE_NAME +
                " ORDER BY " + AlarmTableEntry.COLUMN_SET_ID + " ASC ";

        return db.rawQuery(selectQuery, null);
    }

    public List<DeviceAlarm> getAlarmSet(long SetId) {
        SQLiteDatabase db = initDB();

        List<DeviceAlarm> alarmList;

        String selectQuery = "SELECT * FROM " + AlarmTableEntry.TABLE_NAME +
                " WHERE " + AlarmTableEntry.COLUMN_SET_ID + " = " + SetId;

        Cursor cursor = db.rawQuery(selectQuery, null);
        alarmList = extractAlarms(cursor);

        return alarmList;
    }

    public void deleteAlarmSet(long setId) {
        SQLiteDatabase db = initDB();

        String execSql = "DELETE FROM " + AlarmTableEntry.TABLE_NAME + " WHERE " + AlarmTableEntry.COLUMN_SET_ID + " = " + setId + ";";

        db.execSQL(execSql);
        db.close();
    }

    public int countAlarmSets() {
        SQLiteDatabase db = initDB();

        String selectQuery = "SELECT COUNT(DISTINCT " + AlarmTableEntry.COLUMN_SET_ID + ") AS c FROM " + AlarmTableEntry.TABLE_NAME + ";";

        Cursor cursor = db.rawQuery(selectQuery, null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);

        db.close();
        cursor.close();

        return count;
    }

    public Long returnFirstAlarmSetId() {
        SQLiteDatabase db = initDB();

        String selectQuery = "SELECT " + AlarmTableEntry.COLUMN_SET_ID + " FROM " + AlarmTableEntry.TABLE_NAME;

        Cursor cursor = db.rawQuery(selectQuery, null);

        cursor.moveToFirst();
        long setId;

        try {
            setId = cursor.getLong(cursor.getColumnIndex("set_id"));

            db.close();
            cursor.close();
            return setId;
        } catch (android.database.CursorIndexOutOfBoundsException e) {
            Log.e("CursorException:", e.toString());
            return null;
        }
    }

//    public Alarm getSingleAlarm(long setId, long piId)
//    {
//        SQLiteDatabase db = initDB();
//
//        Alarm alarm = new Alarm();
//
//        String selectQuery = "SELECT * FROM " + AlarmTableEntry.TABLE_NAME + " WHERE "
//                + AlarmTableEntry.COLUMN_SET_ID + " = " + setId + " AND " + AlarmTableEntry.COLUMN_PI_ID
//                + " = " + piId ;
//
//        Cursor cursor = db.rawQuery(selectQuery,null);
//
//        if (cursor.moveToFirst()) {
//            do {
//                alarm.setSetId(cursor.getInt(cursor.getColumnIndex("set_id")));
//            } while (cursor.moveToNext());
//        }
//        cursor.close();
//        db.close();
//        return alarm;
//    }

    private SQLiteDatabase initDB() {
        DeviceAlarmTableHelper dbHelper = new DeviceAlarmTableHelper(context);
        return dbHelper.getWritableDatabase();
    }

    public List<DeviceAlarm> extractAlarms(Cursor cursor) {
        List<DeviceAlarm> alarmList = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                alarm = new DeviceAlarm();

                alarm.setSetId(cursor.getLong(cursor.getColumnIndex(AlarmTableEntry.COLUMN_SET_ID)));
                alarm.setPiId(cursor.getInt(cursor.getColumnIndex(AlarmTableEntry.COLUMN_PI_ID)));

                alarm.setHour(cursor.getInt(cursor.getColumnIndex(AlarmTableEntry.COLUMN_HOUR)));
                alarm.setMinute(cursor.getInt(cursor.getColumnIndex(AlarmTableEntry.COLUMN_MINUTE)));
                alarm.setDay(cursor.getInt(cursor.getColumnIndex(AlarmTableEntry.COLUMN_DAY)));
                alarm.setRecurring(cursor.getInt(cursor.getColumnIndex(AlarmTableEntry.COLUMN_RECURRING)) > 0);
                alarm.setMillis(cursor.getInt(cursor.getColumnIndex(AlarmTableEntry.COLUMN_MILLIS)));

                alarm.setLabel(cursor.getString(cursor.getColumnIndex(AlarmTableEntry.COLUMN_LABEL)));
                alarm.setRingtone(cursor.getInt(cursor.getColumnIndex(AlarmTableEntry.COLUMN_RINGTONE)));
                alarm.setVibrate(cursor.getInt(cursor.getColumnIndex(AlarmTableEntry.COLUMN_VIBRATE)) > 0);
                alarm.setEnabled(cursor.getInt(cursor.getColumnIndex(AlarmTableEntry.COLUMN_ENABLED)) > 0);
                alarm.setChanged(cursor.getInt(cursor.getColumnIndex(AlarmTableEntry.COLUMN_CHANGED)) > 0);

                alarm.setDateCreated(cursor.getString(cursor.getColumnIndex(AlarmTableEntry.COLUMN_DATE_CREATED)));
                alarmList.add(alarm);

            } while (cursor.moveToNext());
        }
        return alarmList;
    }

}

//        // Define a projection that specifies which columns from the database
//        // you will actually use after this query.
//        String[] projection = {
//                AlarmTableContract.AlarmTableEntry.COLUMN_PI_ID,
//                AlarmTableContract.AlarmTableEntry.COLUMN_CHANGED,
//        };
//
//        // Filter results WHERE "title" = 'My Title'
//        String selection = AlarmTableContract.AlarmTableEntry.COLUMN_CHANGED + " = ?";
//        String[] selectionArgs = { "1" };
//
//        // How you want the results sorted in the resulting Cursor
//        String sortOrder =
//                AlarmTableContract.AlarmTableEntry.COLUMN_CHANGED + " DESC";
//
//        Cursor cursor = db.query(
//                AlarmTableContract.AlarmTableEntry.TABLE_NAME,                     // The table to query
//                projection,                               // The columns to return
//                selection,                                // The columns for the WHERE clause
//                selectionArgs,                            // The values for the WHERE clause
//                null,                                     // don't group the rows
//                null,                                     // don't filter by row groups
//                sortOrder                                 // The sort order
//        );
//
//        List itemIds = new ArrayList<>();
//        while(cursor.moveToNext()) {
//            long itemId = cursor.getInt(
//                    cursor.getColumnIndexOrThrow(AlarmTableContract.AlarmTableEntry.COLUMN_PI_ID));
//            itemIds.add(itemId);
//        }
//        cursor.close();
