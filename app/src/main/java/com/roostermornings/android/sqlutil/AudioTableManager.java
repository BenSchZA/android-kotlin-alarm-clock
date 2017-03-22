/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.sqlutil;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import static com.roostermornings.android.sqldata.AudioTableContract.AudioTableEntry;

import com.roostermornings.android.domain.DeviceAudioQueueItem;
import com.roostermornings.android.sqldata.AudioTableContract;
import com.roostermornings.android.sqldata.AudioTableHelper;
import com.roostermornings.android.sqldata.DeviceAlarmTableContract;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by bscholtz on 2/14/17.
 */

public class AudioTableManager {

    private Context context;
    private Calendar calendar = Calendar.getInstance();

    //SQL Arguments
    private final static String TRUE = "1";
    private final static String FALSE = "0";

    public AudioTableManager(Context context) {
        this.context = context;
    }


    public void insertAudioFile(DeviceAudioQueueItem queue, Boolean type) {

        SQLiteDatabase db = initDB();

        ContentValues values = new ContentValues();

        //Type defines whether the audio file is a channel or social Rooster
        values.put(AudioTableEntry.COLUMN_TYPE, type);
        values.put(AudioTableEntry.COLUMN_FILENAME, queue.getFilename());
        values.put(AudioTableEntry.COLUMN_QUEUE_ID, queue.getQueue_id());

        if(queue.getListened() == "TRUE") {
            values.put(AudioTableEntry.COLUMN_LISTENED, TRUE);
        }

        values.put(AudioTableEntry.COLUMN_SENDER_ID, queue.getSender_id());
        values.put(AudioTableEntry.COLUMN_NAME, queue.getName());
        values.put(AudioTableEntry.COLUMN_PICTURE, queue.getPicture());
        values.put(AudioTableEntry.COLUMN_DATE_UPLOADED, queue.getDate_created());

        // Inserting Row
        db.insert(AudioTableEntry.TABLE_NAME, null, values);
        db.close();
    }

    public void removeAudioFile(int ID){
        SQLiteDatabase db = initDB();

        String execSql = "DELETE FROM " + AudioTableEntry.TABLE_NAME + " WHERE " + AudioTableEntry.COLUMN_ID + " = " + ID + ";";

        db.execSQL(execSql);
        db.close();
    }

    //TODO: check that this works...
    public void purgeAudioFiles(){
        //Purge audio files older than 2 weeks
        SQLiteDatabase db = initDB();

        String execSql = "DELETE FROM " + AudioTableEntry.TABLE_NAME + " WHERE " + AudioTableEntry.COLUMN_DATE_UPLOADED +
                " in (SELECT q." + AudioTableEntry.COLUMN_DATE_UPLOADED + " FROM " + AudioTableEntry.TABLE_NAME + " q WHERE " +
                AudioTableEntry.COLUMN_DATE_UPLOADED + " < " + (calendar.getTimeInMillis() - 1209600000) + ");";

        db.execSQL(execSql);
        db.close();
    }

    public void clearAudioFiles(){
        SQLiteDatabase db = initDB();

        String execSql = "TRUNCATE TABLE " + AudioTableEntry.TABLE_NAME;

        db.execSQL(execSql);
        db.close();
    }

    public ArrayList<DeviceAudioQueueItem> extractSocialAudioFiles() {

        SQLiteDatabase db = initDB();

        String selectQuery = "SELECT * FROM " + AudioTableEntry.TABLE_NAME + " WHERE " + AudioTableEntry.COLUMN_TYPE + " = " + FALSE + ";";

        Cursor cursor = db.rawQuery(selectQuery, null);

        return extractAudioFiles(cursor);
    }

    public ArrayList<DeviceAudioQueueItem> extractAlarmChannelAudioFiles(String alarmUid) {

        SQLiteDatabase db = initDB();

        String selectQuery = "SELECT * FROM " + AudioTableEntry.TABLE_NAME + " WHERE " + AudioTableEntry.COLUMN_TYPE + " = " + TRUE + " AND " + AudioTableEntry.COLUMN_QUEUE_ID + " = " + "'" + alarmUid + "'" + ";";

        Cursor cursor = db.rawQuery(selectQuery, null);

        return extractAudioFiles(cursor);
    }

    public ArrayList<DeviceAudioQueueItem> extractAllChannelAudioFiles() {
        SQLiteDatabase db = initDB();

        String selectQuery = "SELECT * FROM " + AudioTableEntry.TABLE_NAME + " WHERE " + AudioTableEntry.COLUMN_TYPE + " = " + TRUE + ";";

        Cursor cursor = db.rawQuery(selectQuery, null);

        return extractAudioFiles(cursor);
    }

    public void setListened(int ID) {
        SQLiteDatabase db = initDB();

        String updateQuery = "UPDATE " + AudioTableEntry.TABLE_NAME + " SET " + AudioTableEntry.COLUMN_LISTENED + " = " + TRUE + " WHERE " + AudioTableEntry.COLUMN_ID + " = " + ID + ";";

        db.execSQL(updateQuery);
        db.close();
    }

    public ArrayList<DeviceAudioQueueItem> selectListened() {
        SQLiteDatabase db = initDB();

        String selectQuery = "SELECT * FROM " + AudioTableEntry.TABLE_NAME + " WHERE " + AudioTableEntry.COLUMN_LISTENED + " = " + TRUE + ";";

        Cursor cursor = db.rawQuery(selectQuery, null);

        return extractAudioFiles(cursor);
    }

    private ArrayList<DeviceAudioQueueItem> extractAudioFiles(Cursor cursor) {
        ArrayList<DeviceAudioQueueItem> audioList = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                DeviceAudioQueueItem audioFile = new DeviceAudioQueueItem();

                audioFile.setDate_created(cursor.getInt(cursor.getColumnIndex(AudioTableEntry.COLUMN_DATE_UPLOADED)));
                audioFile.setFilename(cursor.getString(cursor.getColumnIndex(AudioTableEntry.COLUMN_FILENAME)));
                audioFile.setId(cursor.getInt(cursor.getColumnIndex(AudioTableEntry.COLUMN_ID)));
                audioFile.setQueue_id(cursor.getString(cursor.getColumnIndex(AudioTableEntry.COLUMN_QUEUE_ID)));
                audioFile.setListened(cursor.getString(cursor.getColumnIndex(AudioTableEntry.COLUMN_LISTENED)));
                audioFile.setSender_id(cursor.getString(cursor.getColumnIndex(AudioTableEntry.COLUMN_SENDER_ID)));
                audioFile.setName(cursor.getString(cursor.getColumnIndex(AudioTableEntry.COLUMN_NAME)));
                audioFile.setPicture(cursor.getString(cursor.getColumnIndex(AudioTableEntry.COLUMN_PICTURE)));

                audioList.add(audioFile);

            } while (cursor.moveToNext());
        }
        cursor.close();

        return audioList;
    }

    private SQLiteDatabase initDB() {
        AudioTableHelper dbHelper = new AudioTableHelper(context);
        return dbHelper.getWritableDatabase();
    }
}