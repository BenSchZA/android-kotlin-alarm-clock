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

import com.roostermornings.android.firebase.FA;
import com.roostermornings.android.firebase.FirebaseNetwork;
import com.roostermornings.android.sqldata.AudioTableHelper;
import com.roostermornings.android.util.Constants;
import com.roostermornings.android.util.RoosterUtils;

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

    public Boolean insertSocialAudioFile(DeviceAudioQueueItem audioItem) {

        SQLiteDatabase db = initDB();

        ContentValues values = new ContentValues();

        //Type defines whether the audio file is a channel or social Rooster
        values.put(AudioTableEntry.COLUMN_TYPE, Constants.AUDIO_TYPE_SOCIAL);
        values.put(AudioTableEntry.COLUMN_FAVOURITE, audioItem.getFavourite());
        values.put(AudioTableEntry.COLUMN_FILENAME, audioItem.getFilename());
        values.put(AudioTableEntry.COLUMN_QUEUE_ID, audioItem.getQueue_id());

        values.put(AudioTableEntry.COLUMN_SENDER_ID, audioItem.getSender_id());
        values.put(AudioTableEntry.COLUMN_NAME, audioItem.getName());
        values.put(AudioTableEntry.COLUMN_PICTURE, audioItem.getPicture());
        values.put(AudioTableEntry.COLUMN_DATE_UPLOADED, audioItem.getDate_uploaded());
        values.put(AudioTableEntry.COLUMN_SOURCE_URL, audioItem.getSource_url());
        values.put(AudioTableEntry.COLUMN_DATE_CREATED, calendar.getTimeInMillis());

        try {
            // Inserting Row
            db.insertOrThrow(AudioTableEntry.TABLE_NAME, null, values);
        } catch(SQLiteConstraintException e){
            e.printStackTrace();
            return false;
        }
        updateRoosterCount();

        // Log event to enable tallying of social roosters received for user_metrics
        FA.Log(FA.Event.social_rooster_received.class, null, null);
        return true;
    }

    public Boolean insertChannelAudioFile(DeviceAudioQueueItem audioItem) {

        SQLiteDatabase db = initDB();

        ContentValues values = new ContentValues();

        //Type defines whether the audio file is a channel or social Rooster
        values.put(AudioTableEntry.COLUMN_TYPE, Constants.AUDIO_TYPE_CHANNEL);
        values.put(AudioTableEntry.COLUMN_FAVOURITE, audioItem.getFavourite());
        values.put(AudioTableEntry.COLUMN_LISTENED, audioItem.getListened());
        values.put(AudioTableEntry.COLUMN_FILENAME, audioItem.getFilename());
        values.put(AudioTableEntry.COLUMN_QUEUE_ID, audioItem.getQueue_id());

        values.put(AudioTableEntry.COLUMN_SENDER_ID, audioItem.getSender_id());
        values.put(AudioTableEntry.COLUMN_NAME, audioItem.getName());
        values.put(AudioTableEntry.COLUMN_PICTURE, audioItem.getPicture());
        values.put(AudioTableEntry.COLUMN_DATE_UPLOADED, audioItem.getDate_uploaded());
        values.put(AudioTableEntry.COLUMN_ACTION_TITLE, audioItem.getAction_title());
        values.put(AudioTableEntry.COLUMN_ACTION_URL, audioItem.getAction_url());
        values.put(AudioTableEntry.COLUMN_SOURCE_URL, audioItem.getSource_url());
        //If audioItem has date created entry, use it, otherwise use current time
        //This is used when cloning a channel audioItem for use in the "Roosters" page
        if(audioItem.getDate_created() == null) {
            values.put(AudioTableEntry.COLUMN_DATE_CREATED, calendar.getTimeInMillis());
        } else {
            values.put(AudioTableEntry.COLUMN_DATE_CREATED, audioItem.getDate_created());
        }

        try {
            // Inserting Row
            db.insertOrThrow(AudioTableEntry.TABLE_NAME, null, values);
        } catch(SQLiteConstraintException e){
            e.printStackTrace();
            return false;
        }
        return true;
    }



    public void removeAudioEntry(DeviceAudioQueueItem deviceAudioQueueItem){
        SQLiteDatabase db = initDB();

        File file = new File(context.getFilesDir(), deviceAudioQueueItem.getFilename());
        file.delete();

        String execSql = "DELETE FROM " + AudioTableEntry.TABLE_NAME + " WHERE " + AudioTableEntry.COLUMN_ID + " = " + deviceAudioQueueItem.getId() + ";";

        db.execSQL(execSql);

        updateRoosterCount();
    }

    private void setChannelAudioFileName(String channelId, String channelAudioFileName) {
        SQLiteDatabase db = initDB();

        String updateQuery = "UPDATE " + AudioTableEntry.TABLE_NAME + " SET " + AudioTableEntry.COLUMN_FILENAME + " = '" + channelAudioFileName + "' WHERE " + AudioTableEntry.COLUMN_QUEUE_ID + " = '" + channelId + "';";

        db.execSQL(updateQuery);
    }
    
    private void removeChannelAudioEntries(String channelId) {
        SQLiteDatabase db = initDB();

        //If file is not contained in either today or favourite list, delete it
        ArrayList<DeviceAudioQueueItem> todaysChannels = extractTodayChannelAudioFiles();
        ArrayList<DeviceAudioQueueItem> favouriteChannels = extractFavouriteChannelAudioFiles();

        for (DeviceAudioQueueItem deviceAudioQueueItem:
                extractAlarmChannelAudioFiles(channelId)) {
            if((todaysChannels == null || todaysChannels.contains(deviceAudioQueueItem))
                    && (favouriteChannels == null || favouriteChannels.contains(deviceAudioQueueItem)) ) {
                File file = new File(context.getFilesDir(), deviceAudioQueueItem.getFilename());
                file.delete();
            }
        }

        String execSql = "DELETE FROM " + AudioTableEntry.TABLE_NAME + " WHERE " + AudioTableEntry.COLUMN_QUEUE_ID + " = '" + channelId + "' AND " + AudioTableEntry.COLUMN_TYPE + " = " + Constants.AUDIO_TYPE_CHANNEL + ";";
        db.execSQL(execSql);
    }

    public void updateRoosterCount() {
        //Every time audio file is removed from or inserted in db, count number of social roosters for notification
        //Send broadcast message to notify all receivers of new notification
        Intent intent = new Intent("rooster.update.ROOSTER_NOTIFICATION");
        intent.putExtra(Constants.EXTRA_SOCIAL_ROOSTERS, countUnheardSocialAudioFiles());
        context.sendBroadcast(intent);
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
    }

    public ArrayList<DeviceAudioQueueItem> extractSocialAudioFiles() {

        SQLiteDatabase db = initDB();

        String selectQuery = "SELECT * FROM " + AudioTableEntry.TABLE_NAME + " WHERE " + AudioTableEntry.COLUMN_TYPE + " = " + FALSE + " OR " + AudioTableEntry.COLUMN_TYPE + " IS NULL;";

        Cursor cursor = db.rawQuery(selectQuery, null);
        ArrayList<DeviceAudioQueueItem> deviceAudioQueueItems = extractAudioFiles(cursor);
        cursor.close();

        return deviceAudioQueueItems;
    }

    public ArrayList<DeviceAudioQueueItem> extractUnheardSocialAudioFiles() {

        SQLiteDatabase db = initDB();

        String selectQuery = "SELECT * FROM " + AudioTableEntry.TABLE_NAME + " WHERE (" + AudioTableEntry.COLUMN_TYPE + " = " + FALSE + " OR " + AudioTableEntry.COLUMN_TYPE + " IS NULL) AND "
                + AudioTableEntry.COLUMN_LISTENED + " = " + FALSE + ";";

        Cursor cursor = db.rawQuery(selectQuery, null);
        ArrayList<DeviceAudioQueueItem> deviceAudioQueueItems = extractAudioFiles(cursor);
        cursor.close();

        return deviceAudioQueueItems;
    }

    public ArrayList<DeviceAudioQueueItem> extractTodaySocialAudioFiles() {

        SQLiteDatabase db = initDB();

        String selectQuery = "SELECT * FROM " + AudioTableEntry.TABLE_NAME + " WHERE (" + AudioTableEntry.COLUMN_TYPE + " = " + Constants.AUDIO_TYPE_SOCIAL + " OR " + AudioTableEntry.COLUMN_TYPE + " IS NULL) AND "
                + AudioTableEntry.COLUMN_DATE_CREATED + " > " + (calendar.getTimeInMillis() - Constants.TIME_MILLIS_1_DAY)
                + " AND " + AudioTableEntry.COLUMN_LISTENED + " = " + TRUE + ";";

        Cursor cursor = db.rawQuery(selectQuery, null);
        ArrayList<DeviceAudioQueueItem> deviceAudioQueueItems = extractAudioFiles(cursor);
        cursor.close();

        return deviceAudioQueueItems;
    }

    public ArrayList<DeviceAudioQueueItem> extractFavouriteSocialAudioFiles() {

        SQLiteDatabase db = initDB();

        String selectQuery = "SELECT * FROM " + AudioTableEntry.TABLE_NAME + " WHERE (" + AudioTableEntry.COLUMN_TYPE + " = " + Constants.AUDIO_TYPE_SOCIAL + " OR " + AudioTableEntry.COLUMN_TYPE + " IS NULL) AND "
                + AudioTableEntry.COLUMN_FAVOURITE + " = " + Constants.AUDIO_TYPE_FAVOURITE_TRUE + ";";

        Cursor cursor = db.rawQuery(selectQuery, null);
        ArrayList<DeviceAudioQueueItem> deviceAudioQueueItems = extractAudioFiles(cursor);
        cursor.close();

        return deviceAudioQueueItems;
    }

    public ArrayList<DeviceAudioQueueItem> extractTodayChannelAudioFiles() {

        SQLiteDatabase db = initDB();

        String selectQuery = "SELECT * FROM " + AudioTableEntry.TABLE_NAME + " WHERE " + AudioTableEntry.COLUMN_TYPE + " = " + Constants.AUDIO_TYPE_CHANNEL + " AND "
                + AudioTableEntry.COLUMN_DATE_CREATED + " > " + (calendar.getTimeInMillis() - Constants.TIME_MILLIS_1_DAY)
                + " AND " + AudioTableEntry.COLUMN_LISTENED + " = " + TRUE + ";";

        Cursor cursor = db.rawQuery(selectQuery, null);
        ArrayList<DeviceAudioQueueItem> deviceAudioQueueItems = extractAudioFiles(cursor);
        cursor.close();

        return deviceAudioQueueItems;
    }

    public ArrayList<DeviceAudioQueueItem> extractFavouriteChannelAudioFiles() {

        SQLiteDatabase db = initDB();

        String selectQuery = "SELECT * FROM " + AudioTableEntry.TABLE_NAME + " WHERE " + AudioTableEntry.COLUMN_TYPE + " = " + Constants.AUDIO_TYPE_CHANNEL + " AND "
                + AudioTableEntry.COLUMN_FAVOURITE + " = " + Constants.AUDIO_TYPE_FAVOURITE_TRUE  + ";";

        Cursor cursor = db.rawQuery(selectQuery, null);
        ArrayList<DeviceAudioQueueItem> deviceAudioQueueItems = extractAudioFiles(cursor);
        cursor.close();

        return deviceAudioQueueItems;
    }

    private void updateDateCreated(int ID) {
        SQLiteDatabase db = initDB();

        String updateQuery = "UPDATE " + AudioTableEntry.TABLE_NAME + " SET " + AudioTableEntry.COLUMN_DATE_CREATED + " = " + calendar.getTimeInMillis() + " WHERE " + AudioTableEntry.COLUMN_ID + " = " + ID + ";";

        db.execSQL(updateQuery);
    }

    public Integer countUnheardSocialAudioFiles() {
        SQLiteDatabase db = initDB();

        String selectQuery = "SELECT * FROM " + AudioTableEntry.TABLE_NAME + " WHERE (" + AudioTableEntry.COLUMN_TYPE + " = " + FALSE + " OR " + AudioTableEntry.COLUMN_TYPE + " IS NULL) AND " + AudioTableEntry.COLUMN_LISTENED + " = " + FALSE + ";";

        Cursor cursor = db.rawQuery(selectQuery, null);
        Integer count = cursor.getCount();

        if(count < 0) count = 0;

        cursor.close();

        return count;
    }

    public ArrayList<DeviceAudioQueueItem> extractAlarmChannelAudioFiles(String channelQueueId) {

        SQLiteDatabase db = initDB();

        String selectQuery = "SELECT * FROM " + AudioTableEntry.TABLE_NAME + " WHERE " + AudioTableEntry.COLUMN_TYPE + " = " + TRUE + " AND " + AudioTableEntry.COLUMN_QUEUE_ID + " = " + "'" + channelQueueId + "'" + ";";

        Cursor cursor = db.rawQuery(selectQuery, null);
        ArrayList<DeviceAudioQueueItem> audioItems = extractAudioFiles(cursor);

        cursor.close();

        return audioItems;
    }

    private ArrayList<DeviceAudioQueueItem> extractAllChannelAudioFiles() {
        SQLiteDatabase db = initDB();

        String selectQuery = "SELECT * FROM " + AudioTableEntry.TABLE_NAME + " WHERE " + AudioTableEntry.COLUMN_TYPE + " = " + TRUE + ";";

        Cursor cursor = db.rawQuery(selectQuery, null);
        ArrayList<DeviceAudioQueueItem> audioItems = extractAudioFiles(cursor);

        cursor.close();

        return audioItems;
    }

    public ArrayList<String> extractAllAudioFileNames() {
        SQLiteDatabase db = initDB();

        ArrayList<String> audioFileNameList = new ArrayList<>();

        String selectQuery = "SELECT " + AudioTableEntry.COLUMN_FILENAME + " FROM " + AudioTableEntry.TABLE_NAME + ";";

        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                audioFileNameList.add(cursor.getString(cursor.getColumnIndex(AudioTableEntry.COLUMN_FILENAME)));
            } while (cursor.moveToNext());
        }
        cursor.close();

        return audioFileNameList;
    }

    public Boolean isChannelAudioInDatabase(String channelId) {
        SQLiteDatabase db = initDB();

        String selectQuery = "SELECT 1 FROM " + AudioTableEntry.TABLE_NAME + " WHERE " + AudioTableEntry.COLUMN_TYPE + " = " + TRUE + " AND " + AudioTableEntry.COLUMN_QUEUE_ID + " LIKE \"%" + channelId + "%\";";

        Cursor cursor = db.rawQuery(selectQuery, null);

        if(cursor.getCount() > 0) {
            cursor.close();
            return true;
        } else {
            cursor.close();
            return false;
        }
    }

    public DeviceAudioQueueItem extractChannelAudioItem(String channelId) {
        SQLiteDatabase db = initDB();

        String selectQuery = "SELECT * FROM " + AudioTableEntry.TABLE_NAME + " WHERE " + AudioTableEntry.COLUMN_TYPE + " = " + Constants.AUDIO_TYPE_CHANNEL + " AND " + AudioTableEntry.COLUMN_QUEUE_ID + " LIKE \"%" + channelId + "%\" LIMIT 1;";

        Cursor cursor = db.rawQuery(selectQuery, null);

        ArrayList<DeviceAudioQueueItem> audioItems = extractAudioFiles(cursor);
        cursor.close();

        if(!audioItems.isEmpty()) {
            return audioItems.get(0);
        } else {
            return null;
        }
    }

    public boolean isChannelAudioURLFresh(String channelId, String URL) {
        SQLiteDatabase db = initDB();

        String selectQuery = "SELECT 1 FROM " + AudioTableEntry.TABLE_NAME + " WHERE " + AudioTableEntry.COLUMN_TYPE + " = " + Constants.AUDIO_TYPE_CHANNEL + " AND " + AudioTableEntry.COLUMN_QUEUE_ID + " LIKE \"%" + channelId + "%\"" + " AND " + AudioTableEntry.COLUMN_SOURCE_URL + " LIKE \"%" + URL + "%\";";

        Cursor cursor = db.rawQuery(selectQuery, null);

        if(cursor.getCount() > 0) {
            //Audio is fresh
            cursor.close();
            return true;
        } else {
            //Audio is not fresh, remove, and clone if appropriate for display in "Roosters" page
            cursor.close();
            checkChannelClone(channelId);
            removeChannelAudioEntries(channelId);
            return false;
        }
    }

    private void checkChannelClone(String channelId) {
        SQLiteDatabase db = initDB();

        String selectQuery = "SELECT * FROM " + AudioTableEntry.TABLE_NAME + " WHERE " + AudioTableEntry.COLUMN_TYPE + " = " + Constants.AUDIO_TYPE_CHANNEL + " AND " + AudioTableEntry.COLUMN_QUEUE_ID + " LIKE \"%" + channelId + "%\"" +
                " AND " + AudioTableEntry.COLUMN_LISTENED + " = " + TRUE +
                " AND (" + AudioTableEntry.COLUMN_DATE_CREATED + " > " + (calendar.getTimeInMillis() - Constants.TIME_MILLIS_1_DAY) +
                " OR " + AudioTableEntry.COLUMN_FAVOURITE + " = " + TRUE + ");";

        Cursor cursor = db.rawQuery(selectQuery, null);
        ArrayList<DeviceAudioQueueItem> audioItems = extractAudioFiles(cursor);

        if(cursor.getCount() > 0) {
            //Channel is listened, and within day or favourite, therefore clone before deletion

            //Clone audio item for today's roosters page
            DeviceAudioQueueItem cloneAudioItem = new DeviceAudioQueueItem(audioItems.get(0));
            cloneAudioItem.setQueue_id(RoosterUtils.createRandomUID(10));
            insertChannelAudioFile(cloneAudioItem);

            cursor.close();
        } else {
            cursor.close();
        }
    }

    public void purgeStagnantChannelAudio() {
        DeviceAlarmTableManager deviceAlarmTableManager = new DeviceAlarmTableManager(context);

        //Stagnant?
        // -> date_created older than a week
        // -> no alarms set with matching channel
        SQLiteDatabase db = initDB();

        String selectQuery = "SELECT * FROM " + AudioTableEntry.TABLE_NAME + " WHERE " + AudioTableEntry.COLUMN_TYPE + " = " + Constants.AUDIO_TYPE_CHANNEL
                + " AND " + AudioTableEntry.COLUMN_DATE_CREATED + " < " + (calendar.getTimeInMillis() - Constants.TIME__MILLIS_1_WEEK)
                + " AND " + AudioTableEntry.COLUMN_FAVOURITE + " = " + FALSE + ";";
        Cursor cursor = db.rawQuery(selectQuery, null);

        //Find lost children
        ArrayList<String> alarmChannels = new ArrayList<>();
        for (DeviceAlarm alarmSet:
                deviceAlarmTableManager.getAlarmSets()) {
            alarmChannels.add(alarmSet.getChannel());
        }
        for (DeviceAudioQueueItem oldChannelAudio:
                extractAudioFiles(cursor)) {
            String oldChannelAudioID = oldChannelAudio.getQueue_id();
            if(!alarmChannels.contains(oldChannelAudioID)) removeChannelAudioEntries(oldChannelAudioID);
        }

        cursor.close();
    }

    public void purgeStagnantSocialAudio() {
        SQLiteDatabase db = initDB();

        String selectQuery = "SELECT * FROM " + AudioTableEntry.TABLE_NAME + " WHERE (" + AudioTableEntry.COLUMN_TYPE + " = " + Constants.AUDIO_TYPE_SOCIAL + " OR " + AudioTableEntry.COLUMN_TYPE + " IS NULL) AND "
                + AudioTableEntry.COLUMN_DATE_CREATED + " < " + (calendar.getTimeInMillis() - Constants.TIME_MILLIS_1_DAY)
                + " AND " + AudioTableEntry.COLUMN_FAVOURITE + " = " + FALSE + ";";

        Cursor cursor = db.rawQuery(selectQuery, null);
        ArrayList<DeviceAudioQueueItem> deviceAudioQueueItems = extractAudioFiles(cursor);
        cursor.close();

        for (DeviceAudioQueueItem audioItem:
                deviceAudioQueueItems) {
            removeAudioEntry(audioItem);
        }
    }

    public Boolean isSocialAudioInDatabase(String socialId) {
        SQLiteDatabase db = initDB();

        String selectQuery = "SELECT 1 FROM " + AudioTableEntry.TABLE_NAME + " WHERE " + AudioTableEntry.COLUMN_TYPE + " = " + Constants.AUDIO_TYPE_SOCIAL + " AND " + AudioTableEntry.COLUMN_QUEUE_ID + " LIKE \"%" + socialId + "%\";";

        Cursor cursor = db.rawQuery(selectQuery, null);

        if(cursor.getCount() > 0) {
            cursor.close();
            return true;
        } else {
            cursor.close();
            return false;
        }
    }

    public void setFavourite(int ID, boolean favourite) {
        SQLiteDatabase db = initDB();

        int booleanInt;
        if(favourite) booleanInt = Constants.AUDIO_TYPE_FAVOURITE_TRUE;
        else booleanInt = Constants.AUDIO_TYPE_FAVOURITE_FALSE;

        String updateQuery = "UPDATE " + AudioTableEntry.TABLE_NAME + " SET " + AudioTableEntry.COLUMN_FAVOURITE + " = " + booleanInt + " WHERE " + AudioTableEntry.COLUMN_ID + " = " + ID + ";";

        db.execSQL(updateQuery);
    }

    public void setListened(DeviceAudioQueueItem audioItem) {
        SQLiteDatabase db = initDB();

        FirebaseNetwork.INSTANCE.setListened(audioItem.getSender_id(), audioItem.getQueue_id());
        updateDateCreated(audioItem.getId());

        String updateQuery = "UPDATE " + AudioTableEntry.TABLE_NAME + " SET " + AudioTableEntry.COLUMN_LISTENED + " = " + TRUE + " WHERE " + AudioTableEntry.COLUMN_ID + " = " + audioItem.getId() + ";";

        db.execSQL(updateQuery);
    }

    public void clearListened(int ID) {
        SQLiteDatabase db = initDB();

        String updateQuery = "UPDATE " + AudioTableEntry.TABLE_NAME + " SET " + AudioTableEntry.COLUMN_LISTENED + " = " + FALSE + " WHERE " + AudioTableEntry.COLUMN_ID + " = " + ID + ";";

        db.execSQL(updateQuery);
    }

    public ArrayList<DeviceAudioQueueItem> selectListened() {
        SQLiteDatabase db = initDB();

        String selectQuery = "SELECT * FROM " + AudioTableEntry.TABLE_NAME + " WHERE " + AudioTableEntry.COLUMN_LISTENED + " = " + TRUE + ";";

        Cursor cursor = db.rawQuery(selectQuery, null);
        ArrayList<DeviceAudioQueueItem> deviceAudioQueueItems = extractAudioFiles(cursor);
        cursor.close();

        return deviceAudioQueueItems;
    }

    public ArrayList<DeviceAudioQueueItem> selectListenedByChannel(String queueId) {
        SQLiteDatabase db = initDB();

        String selectQuery = "SELECT * FROM " + AudioTableEntry.TABLE_NAME + " WHERE "
                + AudioTableEntry.COLUMN_LISTENED + " = " + TRUE +
                " AND " + AudioTableEntry.COLUMN_TYPE + " = " + Constants.AUDIO_TYPE_CHANNEL +
                " AND " + AudioTableEntry.COLUMN_QUEUE_ID + " = '" + queueId + "';";

        Cursor cursor = db.rawQuery(selectQuery, null);
        ArrayList<DeviceAudioQueueItem> deviceAudioQueueItems = extractAudioFiles(cursor);
        cursor.close();

        return deviceAudioQueueItems;
    }

    private ArrayList<DeviceAudioQueueItem> extractAudioFiles(Cursor cursor) {
        ArrayList<DeviceAudioQueueItem> audioList = new ArrayList<>();

        if (cursor.moveToFirst()) {
            do {
                DeviceAudioQueueItem audioFile = new DeviceAudioQueueItem();

                audioFile.setType(cursor.getInt(cursor.getColumnIndex(AudioTableEntry.COLUMN_TYPE)));
                audioFile.setFavourite(cursor.getInt(cursor.getColumnIndex(AudioTableEntry.COLUMN_FAVOURITE)));
                audioFile.setDate_uploaded(cursor.getLong(cursor.getColumnIndex(AudioTableEntry.COLUMN_DATE_UPLOADED)));
                audioFile.setFilename(cursor.getString(cursor.getColumnIndex(AudioTableEntry.COLUMN_FILENAME)));
                audioFile.setId(cursor.getInt(cursor.getColumnIndex(AudioTableEntry.COLUMN_ID)));
                audioFile.setQueue_id(cursor.getString(cursor.getColumnIndex(AudioTableEntry.COLUMN_QUEUE_ID)));
                audioFile.setListened(cursor.getString(cursor.getColumnIndex(AudioTableEntry.COLUMN_LISTENED)));
                audioFile.setSender_id(cursor.getString(cursor.getColumnIndex(AudioTableEntry.COLUMN_SENDER_ID)));
                audioFile.setName(cursor.getString(cursor.getColumnIndex(AudioTableEntry.COLUMN_NAME)));
                audioFile.setPicture(cursor.getString(cursor.getColumnIndex(AudioTableEntry.COLUMN_PICTURE)));
                audioFile.setAction_title(cursor.getString(cursor.getColumnIndex(AudioTableEntry.COLUMN_ACTION_TITLE)));
                audioFile.setAction_url(cursor.getString(cursor.getColumnIndex(AudioTableEntry.COLUMN_ACTION_URL)));
                audioFile.setSource_url(cursor.getString(cursor.getColumnIndex(AudioTableEntry.COLUMN_SOURCE_URL)));
                audioFile.setDate_created(cursor.getLong(cursor.getColumnIndex(AudioTableEntry.COLUMN_DATE_CREATED)));

                audioList.add(audioFile);

            } while (cursor.moveToNext());
        }
        cursor.close();

        return audioList;
    }

    private SQLiteDatabase initDB() {
        AudioTableHelper dbHelper = AudioTableHelper.getInstance(context);
        return dbHelper.getWritableDatabase();
    }
}