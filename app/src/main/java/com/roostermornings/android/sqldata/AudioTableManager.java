package com.roostermornings.android.sqldata;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.roostermornings.android.domain.AlarmQueue;

/**
 * Created by bscholtz on 2/14/17.
 */

public class AudioTableManager {

    private Context context;

    //SQL Arguments
    private final static String TRUE = "1";
    private final static String FALSE = "0";

    public AudioTableManager(Context context) {
        this.context = context;
    }


    public void insertAudioFile(AlarmQueue queue) {

        SQLiteDatabase db = initDB();

        ContentValues values = new ContentValues();

        values.put(AudioTableContract.AudioTableEntry.COLUMN_FILENAME, queue.getAudio_file_url());
        values.put(AudioTableContract.AudioTableEntry.COLUMN_QUEUE_ID, queue.getQueue_id());
        values.put(AudioTableContract.AudioTableEntry.COLUMN_ALARM_ID, queue.getAlarm_id());

        // Inserting Row
        db.insert(AudioTableContract.AudioTableEntry.TABLE_NAME, null, values);
        db.close();
    }

    private SQLiteDatabase initDB() {
        AudioTableHelper dbHelper = new AudioTableHelper(context);
        return dbHelper.getWritableDatabase();
    }
}