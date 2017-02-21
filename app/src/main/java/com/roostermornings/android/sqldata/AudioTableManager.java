package com.roostermornings.android.sqldata;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import static com.roostermornings.android.sqldata.AudioTableContract.AudioTableEntry;

import com.roostermornings.android.domain.AlarmQueue;
import com.roostermornings.android.domain.DeviceAudioQueueItem;

import java.util.ArrayList;
import java.util.List;

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

        values.put(AudioTableEntry.COLUMN_FILENAME, queue.getAudio_file_url());
        values.put(AudioTableEntry.COLUMN_QUEUE_ID, queue.getQueue_id());
        values.put(AudioTableEntry.COLUMN_ALARM_ID, queue.getAlarm_id());

        values.put(AudioTableEntry.COLUMN_SENDER_ID, queue.getSender_id());
        values.put(AudioTableEntry.COLUMN_SENDER_NAME, queue.getUser_name());
        values.put(AudioTableEntry.COLUMN_SENDER_PIC, queue.getProfile_pic());

        // Inserting Row
        db.insert(AudioTableEntry.TABLE_NAME, null, values);
        db.close();
    }

    public List<DeviceAudioQueueItem> extractAudioFiles() {

        SQLiteDatabase db = initDB();

        String selectQuery = "SELECT * FROM " + AudioTableEntry.TABLE_NAME + ";";

        Cursor cursor = db.rawQuery(selectQuery, null);

        List<DeviceAudioQueueItem> audioList = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                DeviceAudioQueueItem audioFile = new DeviceAudioQueueItem();

                audioFile.setAlarm_id(cursor.getLong(cursor.getColumnIndex(AudioTableEntry.COLUMN_ALARM_ID)));
                audioFile.setDate_created(cursor.getString(cursor.getColumnIndex(AudioTableEntry.COLUMN_DATE_CREATED)));
                audioFile.setFilename(cursor.getString(cursor.getColumnIndex(AudioTableEntry.COLUMN_FILENAME)));
                audioFile.setId(cursor.getInt(cursor.getColumnIndex(AudioTableEntry.COLUMN_ID)));
                audioFile.setQueue_id(cursor.getString(cursor.getColumnIndex(AudioTableEntry.COLUMN_QUEUE_ID)));
                audioFile.setSender_id(cursor.getString(cursor.getColumnIndex(AudioTableEntry.COLUMN_SENDER_ID)));
                audioFile.setSender_name(cursor.getString(cursor.getColumnIndex(AudioTableEntry.COLUMN_SENDER_NAME)));
                audioFile.setSender_pic(cursor.getString(cursor.getColumnIndex(AudioTableEntry.COLUMN_SENDER_PIC)));

                audioList.add(audioFile);

            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return audioList;
    }

    private SQLiteDatabase initDB() {
        AudioTableHelper dbHelper = new AudioTableHelper(context);
        return dbHelper.getWritableDatabase();
    }
}