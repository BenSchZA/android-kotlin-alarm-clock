/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.sqldata;

import android.provider.BaseColumns;
import android.provider.MediaStore;

/**
 * Created by bscholtz on 2/14/17.
 */

public final class AudioTableContract {
    private AudioTableContract() {
    }

    public static class AudioTableEntry implements BaseColumns {
        public static final String TABLE_NAME = "audiofiles";

        //type 1 is channel type 0 is social
        public static final String COLUMN_TYPE = "type";

        public static final String COLUMN_ID = "id";
        public static final String COLUMN_QUEUE_ID = "queue_id";
        public static final String COLUMN_FILENAME = "filename";
        public static final String COLUMN_LISTENED = "listened";

        public static final String COLUMN_SENDER_ID = "sender_id";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_PICTURE = "picture";

        public static final String COLUMN_DATE_UPLOADED = "date_created";
    }

    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + AudioTableEntry.TABLE_NAME + " ("

                    + AudioTableEntry.COLUMN_TYPE + " INTEGER NULL, "

                    + AudioTableEntry.COLUMN_ID + " INTEGER PRIMARY KEY, "
                    + AudioTableEntry.COLUMN_QUEUE_ID + " INTEGER NOT NULL, "
                    + AudioTableEntry.COLUMN_FILENAME + " TEXT NOT NULL, "
                    + AudioTableEntry.COLUMN_LISTENED + " INTEGER DEFAULT 0, "

                    + AudioTableEntry.COLUMN_SENDER_ID + " TEXT NULL, "
                    + AudioTableEntry.COLUMN_NAME + " TEXT NULL, "
                    + AudioTableEntry.COLUMN_PICTURE + " TEXT NULL, "

                    + AudioTableEntry.COLUMN_DATE_UPLOADED + " INTEGER NOT NULL, "

                    //Ensure combination of IDs is unique
                    + "CONSTRAINT unique_id UNIQUE (" + AudioTableEntry.COLUMN_ID + "," + AudioTableEntry.COLUMN_QUEUE_ID + "), "
                    //If type is null then it is not a channel and constraint doesn't apply, if it is not null then unique constraint must be met to ensure one channel entered
                    + "CONSTRAINT unique_channel UNIQUE (" + AudioTableEntry.COLUMN_QUEUE_ID + "," + AudioTableEntry.COLUMN_TYPE + "));";

    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + AudioTableEntry.TABLE_NAME;
}
