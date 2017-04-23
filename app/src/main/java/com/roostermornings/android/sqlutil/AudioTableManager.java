/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.sqlutil;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import com.roostermornings.android.sqldata.AudioTableHelper;
import com.roostermornings.android.util.Constants;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;

import static com.roostermornings.android.sqldata.AudioTableContract.AudioTableEntry;

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


    public Boolean insertSocialAudioFile(DeviceAudioQueueItem queue) {

        SQLiteDatabase db = initDB();

        ContentValues values = new ContentValues();

        //Type defines whether the audio file is a channel or social Rooster
        values.put(AudioTableEntry.COLUMN_FILENAME, queue.getFilename());
        values.put(AudioTableEntry.COLUMN_QUEUE_ID, queue.getQueue_id());

        values.put(AudioTableEntry.COLUMN_SENDER_ID, queue.getSender_id());
        values.put(AudioTableEntry.COLUMN_NAME, queue.getName());
        values.put(AudioTableEntry.COLUMN_PICTURE, queue.getPicture());
        values.put(AudioTableEntry.COLUMN_DATE_UPLOADED, queue.getDate_created());

        try {
            // Inserting Row
            db.insertOrThrow(AudioTableEntry.TABLE_NAME, null, values);
        } catch(SQLiteConstraintException e){
            e.printStackTrace();
            db.close();
            return false;
        }
        db.close();
        updateRoosterCount();
        return true;
    }

    public Boolean insertChannelAudioFile(DeviceAudioQueueItem queue) {

        SQLiteDatabase db = initDB();

        ContentValues values = new ContentValues();

        //Type defines whether the audio file is a channel or social Rooster
        values.put(AudioTableEntry.COLUMN_TYPE, 1);
        values.put(AudioTableEntry.COLUMN_FILENAME, queue.getFilename());
        values.put(AudioTableEntry.COLUMN_QUEUE_ID, queue.getQueue_id());

        values.put(AudioTableEntry.COLUMN_SENDER_ID, queue.getSender_id());
        values.put(AudioTableEntry.COLUMN_NAME, queue.getName());
        values.put(AudioTableEntry.COLUMN_PICTURE, queue.getPicture());
        values.put(AudioTableEntry.COLUMN_DATE_UPLOADED, queue.getDate_created());

        try {
            // Inserting Row
            db.insertOrThrow(AudioTableEntry.TABLE_NAME, null, values);
        } catch(SQLiteConstraintException e){
            e.printStackTrace();
            db.close();
            return false;
        }
        db.close();
        return true;
    }

    public void removeAudioEntry(DeviceAudioQueueItem deviceAudioQueueItem){
        SQLiteDatabase db = initDB();

        File file = new File(context.getFilesDir() + "/" + deviceAudioQueueItem.getFilename());
        if(!file.delete()) return;

        String execSql = "DELETE FROM " + AudioTableEntry.TABLE_NAME + " WHERE " + AudioTableEntry.COLUMN_ID + " = " + deviceAudioQueueItem.getId() + ";";

        db.execSQL(execSql);
        db.close();

        updateRoosterCount();
    }

    public void setChannelAudioFileName(String channelId, String channelAudioFileName) {
        SQLiteDatabase db = initDB();

        String updateQuery = "UPDATE " + AudioTableEntry.TABLE_NAME + " SET " + AudioTableEntry.COLUMN_FILENAME + " = '" + channelAudioFileName + "' WHERE " + AudioTableEntry.COLUMN_QUEUE_ID + " = '" + channelId + "';";

        db.execSQL(updateQuery);
        db.close();
    }
    
    public void removeChannelAudioEntries(String channelId) {
        SQLiteDatabase db = initDB();

        for (DeviceAudioQueueItem deviceAudioQueueItem:
                extractAlarmChannelAudioFiles(channelId)) {
            File file = new File(context.getFilesDir(), deviceAudioQueueItem.getFilename());
            file.delete();
        }

        String execSql = "DELETE FROM " + AudioTableEntry.TABLE_NAME + " WHERE " + AudioTableEntry.COLUMN_QUEUE_ID + " = '" + channelId + "' AND " + AudioTableEntry.COLUMN_TYPE + " = 1" + ";";
        db.execSQL(execSql);

        db.close();
    }

    public void updateRoosterCount() {
        //Every time audio file is removed from or inserted in db, count number of social roosters for notification
        //Send broadcast message to notify all receivers of new notification
        Intent intent = new Intent("rooster.update.ROOSTER_NOTIFICATION");
        intent.putExtra(Constants.EXTRA_SOCIAL_ROOSTERS, countSocialAudioFiles());
        context.sendBroadcast(intent);
    }

    //TODO: check that this works...
    public void purgeSocialAudioFiles() {
        //Purge audio files older than 2 weeks

        for (DeviceAudioQueueItem deviceAudioQueueItem:
                extractSocialAudioFiles()) {
            if(deviceAudioQueueItem.getDate_created() < (calendar.getTimeInMillis() - 1209600000)) removeAudioEntry(deviceAudioQueueItem);
        }
    }

    public void removeAllSocialAudioItems() {
        for (DeviceAudioQueueItem deviceAudioQueueItem:
             extractSocialAudioFiles()) {
            removeAudioEntry(deviceAudioQueueItem);
        }
    }

    public void removeAllChannelAudioFiles() {
        for (DeviceAudioQueueItem deviceAudioQueueItem:
             extractAllChannelAudioFiles()) {
            removeChannelAudioEntries(deviceAudioQueueItem.getQueue_id());
        }
    }

    private void clearAudioFiles(){
        SQLiteDatabase db = initDB();

        String execSql = "DELETE FROM " + AudioTableEntry.TABLE_NAME + ";";

        db.execSQL(execSql);
        db.execSQL("VACUUM");

        db.close();
    }

    public ArrayList<DeviceAudioQueueItem> extractSocialAudioFiles() {

        SQLiteDatabase db = initDB();

        String selectQuery = "SELECT * FROM " + AudioTableEntry.TABLE_NAME + " WHERE " + AudioTableEntry.COLUMN_TYPE + " = " + FALSE + " OR " + AudioTableEntry.COLUMN_TYPE + " IS NULL;";

        Cursor cursor = db.rawQuery(selectQuery, null);
        ArrayList<DeviceAudioQueueItem> deviceAudioQueueItems = extractAudioFiles(cursor);
        db.close();

        return deviceAudioQueueItems;
    }

    public Integer countSocialAudioFiles() {
        SQLiteDatabase db = initDB();

        String selectQuery = "SELECT * FROM " + AudioTableEntry.TABLE_NAME + " WHERE " + AudioTableEntry.COLUMN_TYPE + " = " + FALSE + " OR " + AudioTableEntry.COLUMN_TYPE + " IS NULL;";

        Cursor cursor = db.rawQuery(selectQuery, null);
        Integer count = cursor.getCount();
        cursor.close();
        db.close();

        return count;
    }

    public ArrayList<DeviceAudioQueueItem> extractAlarmChannelAudioFiles(String channelQueueId) {

        SQLiteDatabase db = initDB();

        String selectQuery = "SELECT * FROM " + AudioTableEntry.TABLE_NAME + " WHERE " + AudioTableEntry.COLUMN_TYPE + " = " + TRUE + " AND " + AudioTableEntry.COLUMN_QUEUE_ID + " = " + "'" + channelQueueId + "'" + ";";

        Cursor cursor = db.rawQuery(selectQuery, null);
        ArrayList<DeviceAudioQueueItem> deviceAudioQueueItems = extractAudioFiles(cursor);
        db.close();

        return deviceAudioQueueItems;
    }

    public ArrayList<DeviceAudioQueueItem> extractAllChannelAudioFiles() {
        SQLiteDatabase db = initDB();

        String selectQuery = "SELECT * FROM " + AudioTableEntry.TABLE_NAME + " WHERE " + AudioTableEntry.COLUMN_TYPE + " = " + TRUE + ";";

        Cursor cursor = db.rawQuery(selectQuery, null);
        ArrayList<DeviceAudioQueueItem> deviceAudioQueueItems = extractAudioFiles(cursor);
        db.close();
        //TODO: returning empty! occasionally
        return deviceAudioQueueItems;
    }

    public Boolean isChannelAudioInDatabase(String channelId) {
        SQLiteDatabase db = initDB();

        String selectQuery = "SELECT 1 FROM " + AudioTableEntry.TABLE_NAME + " WHERE " + AudioTableEntry.COLUMN_TYPE + " = " + TRUE + " AND " + AudioTableEntry.COLUMN_QUEUE_ID + " LIKE \"%" + channelId + "%\";";

        Cursor cursor = db.rawQuery(selectQuery, null);

        if(cursor.getCount() > 0) {
            db.close();
            cursor.close();
            return true;
        } else {
            db.close();
            cursor.close();
            return false;
        }
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
        ArrayList<DeviceAudioQueueItem> deviceAudioQueueItems = extractAudioFiles(cursor);
        db.close();

        return deviceAudioQueueItems;
    }

    private ArrayList<DeviceAudioQueueItem> extractAudioFiles(Cursor cursor) {
        ArrayList<DeviceAudioQueueItem> audioList = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                DeviceAudioQueueItem audioFile = new DeviceAudioQueueItem();

                audioFile.setType(cursor.getInt(cursor.getColumnIndex(AudioTableEntry.COLUMN_TYPE)));
                audioFile.setDate_created(cursor.getLong(cursor.getColumnIndex(AudioTableEntry.COLUMN_DATE_UPLOADED)));
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