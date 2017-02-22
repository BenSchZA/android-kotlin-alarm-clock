package com.roostermornings.android.sqldata;

import android.provider.BaseColumns;

/**
 * Created by bscholtz on 2/14/17.
 */

public final class AudioTableContract {
    private AudioTableContract() {
    }

    public static class AudioTableEntry implements BaseColumns {
        public static final String TABLE_NAME = "audiofiles";

        public static final String COLUMN_ID = "id";
        public static final String COLUMN_QUEUE_ID = "queue_id";
        public static final String COLUMN_FILENAME = "filename";
        public static final String COLUMN_ALARM_ID = "alarm_id";

        public static final String COLUMN_SENDER_ID = "sender_id";
        public static final String COLUMN_SENDER_NAME = "sender_name";
        public static final String COLUMN_SENDER_PIC = "sender_pic";

        public static final String COLUMN_DATE_UPLOADED = "date_created";
    }

    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + AudioTableEntry.TABLE_NAME + " ("

                    + AudioTableEntry.COLUMN_ID + " INTEGER PRIMARY KEY, "
                    + AudioTableEntry.COLUMN_QUEUE_ID + " INTEGER NOT NULL, "
                    + AudioTableEntry.COLUMN_FILENAME + " TEXT NOT NULL, "
                    + AudioTableEntry.COLUMN_ALARM_ID + " STRING NULL, "

                    + AudioTableEntry.COLUMN_SENDER_ID + " STRING NULL, "
                    + AudioTableEntry.COLUMN_SENDER_NAME + " STRING NULL, "
                    + AudioTableEntry.COLUMN_SENDER_PIC + " STRING NULL, "

                    + AudioTableEntry.COLUMN_DATE_UPLOADED + " INTEGER NOT NULL, "

                    //Ensure combination of IDs is unique
                    + "CONSTRAINT unique_id UNIQUE (" + AudioTableEntry.COLUMN_ID + "," + AudioTableEntry.COLUMN_QUEUE_ID + ") );";

    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + AudioTableEntry.TABLE_NAME;
}
