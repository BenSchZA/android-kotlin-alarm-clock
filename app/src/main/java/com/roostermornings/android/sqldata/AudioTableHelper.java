/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.sqldata;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import com.crashlytics.android.Crashlytics;
import com.google.firebase.crash.FirebaseCrash;

/**
 * Created by bscholtz on 2/14/17.
 */

public class AudioTableHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 5;
    public static final String DATABASE_NAME = "AudioTable.db";

    private static AudioTableHelper mInstance = null;

    public static AudioTableHelper getInstance(Context context) {

        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (mInstance == null) {
            mInstance = new AudioTableHelper(context);
        }
        return mInstance;
    }

    private AudioTableHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(com.roostermornings.android.sqldata.AudioTableContract.SQL_CREATE_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        //db.execSQL(AudioTableContract.SQL_DELETE_ENTRIES);
        if (newVersion > oldVersion) {
            try {
                String sqlExec = "ALTER TABLE " + AudioTableContract.AudioTableEntry.TABLE_NAME
                        + " ADD COLUMN " + AudioTableContract.AudioTableEntry.COLUMN_FAVOURITE
                        + " INTEGER DEFAULT 0";
                db.execSQL(sqlExec);
            } catch (SQLiteException e) {
                e.printStackTrace();
                Crashlytics.logException(e);
                FirebaseCrash.log(e.toString());
            }
            try {
                String sqlExec = "ALTER TABLE " + AudioTableContract.AudioTableEntry.TABLE_NAME
                        + " ADD COLUMN " + AudioTableContract.AudioTableEntry.COLUMN_SOURCE_URL
                        + " TEXT DEFAULT ''";
                db.execSQL(sqlExec);
            } catch (SQLiteException e) {
                e.printStackTrace();
                Crashlytics.logException(e);
                FirebaseCrash.log(e.toString());
            }
            try {
                String sqlExec = "ALTER TABLE " + AudioTableContract.AudioTableEntry.TABLE_NAME
                        + " ADD COLUMN " + AudioTableContract.AudioTableEntry.COLUMN_DATE_UPLOADED
                        + " INTEGER NOT NULL DEFAULT 0";
                db.execSQL(sqlExec);
            } catch (SQLiteException e) {
                e.printStackTrace();
                Crashlytics.logException(e);
                FirebaseCrash.log(e.toString());
            }
        }
        //onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

}
