/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.sqldata;

import android.provider.BaseColumns;

/**
 * Created by bscholtz on 2/14/17.
 */

public final class DeviceAlarmTableContract {
    private DeviceAlarmTableContract() {
    }

    public static class AlarmTableEntry implements BaseColumns {
        public static final String TABLE_NAME = "alarms";

        public static final String COLUMN_SET_ID = "set_id";
        public static final String COLUMN_PI_ID = "pi_id";

        public static final String COLUMN_HOUR = "hour";
        public static final String COLUMN_MINUTE = "minute";
        public static final String COLUMN_DAY = "day";
        public static final String COLUMN_RECURRING = "recurring";
        public static final String COLUMN_MILLIS = "millis";

        public static final String COLUMN_LABEL = "label";
        public static final String COLUMN_RINGTONE = "ringtone";
        public static final String COLUMN_VIBRATE = "vibrate";
        public static final String COLUMN_ENABLED = "enabled";
        public static final String COLUMN_CHANGED = "changed";

        public static final String COLUMN_DATE_CREATED = "date_created";
    }

    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + AlarmTableEntry.TABLE_NAME + " ("

                    + AlarmTableEntry.COLUMN_PI_ID + " INTEGER PRIMARY KEY, "
                    + AlarmTableEntry.COLUMN_SET_ID + " INTEGER NOT NULL, "

                    + AlarmTableEntry.COLUMN_HOUR + " INTEGER NOT NULL, "
                    + AlarmTableEntry.COLUMN_MINUTE + " INTEGER NOT NULL, "
                    + AlarmTableEntry.COLUMN_DAY + " INTEGER NOT NULL, "
                    + AlarmTableEntry.COLUMN_RECURRING + " INTEGER NOT NULL, "
                    + AlarmTableEntry.COLUMN_MILLIS + " INTEGER, "

                    + AlarmTableEntry.COLUMN_LABEL + " TEXT, "

                    + AlarmTableEntry.COLUMN_RINGTONE + " INTEGER DEFAULT 0, "
                    + AlarmTableEntry.COLUMN_VIBRATE + " INTEGER DEFAULT 0, "
                    + AlarmTableEntry.COLUMN_ENABLED + " INTEGER DEFAULT 1, "
                    + AlarmTableEntry.COLUMN_CHANGED + " INTEGER DEFAULT 0, "
                    + AlarmTableEntry.COLUMN_DATE_CREATED + " DATE DEFAULT (datetime('now','localtime')), "
                    //Ensure combination of IDs is unique
                    + "CONSTRAINT unique_id UNIQUE (" + AlarmTableEntry.COLUMN_SET_ID + "," + AlarmTableEntry.COLUMN_PI_ID + ") );";

    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + AlarmTableEntry.TABLE_NAME;
}
