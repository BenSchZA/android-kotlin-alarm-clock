/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.util;

import android.os.Build;

import com.roostermornings.android.domain.Alarm;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by steven on 2017/02/15.
 */

public class RoosterUtils {

    public static boolean hasM() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    public static boolean hasLollipop() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public static boolean hasKitKat() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    public static boolean hasJellyBean() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }

    public static boolean hasGingerbread() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
    }

    public static Boolean notNull(Object object) {
        return object != null;
    }

    public static String getInitials(String displayName) {
        String initials = "";
        String tempStringArray[];
        if (notNull(displayName) && !"".equals(displayName)) {
            tempStringArray = displayName.split(" ", 2);
            for (String s : tempStringArray) {
                if(!s.isEmpty()) initials = initials.concat(String.valueOf(s.charAt(0)));
            }
        }
        return initials;
    }

    public static String setAlarmTimeFromHourAndMinute(Alarm alarm) {

        String alarmHour = (alarm.getHour() < 10 ? "0" : "") + alarm.getHour();
        String alarmMinute = (alarm.getMinute() < 10 ? "0" : "") + alarm.getMinute();
        return alarmHour + ":" + alarmMinute;

    }

    public static String createRandomFileName(int string) {
        Random random = new Random();
        String mRandomAudioFileName = "ABCDEFGHIJKLMNOP";
        StringBuilder stringBuilder = new StringBuilder(string);
        int i = 0;
        while (i < string) {
            stringBuilder.append(mRandomAudioFileName.
                    charAt(random.nextInt(mRandomAudioFileName.length())));

            i++;
        }
        return stringBuilder.toString();
    }

    public static String getAlarmDays(Alarm alarm) {

        ArrayList<String> days = new ArrayList<String>();
        if (alarm.isMonday()) days.add("Mon");
        if (alarm.isTuesday()) days.add("Tue");
        if (alarm.isWednesday()) days.add("Wed");
        if (alarm.isThursday()) days.add("Thu");
        if (alarm.isFriday()) days.add("Fri");
        if (alarm.isSaturday()) days.add("Sat");
        if (alarm.isSunday()) days.add("Sun");

        if (days.size() == 7) return "Every day";
        else if (days.size() == 5 && days.indexOf("Sun") == -1 && days.indexOf("Sat") == -1)
            return "Weekdays";
        else if (days.size() == 2 && days.indexOf("Sat") != -1 && days.indexOf("Sun") != -1)
            return "Weekend";
        String returnString = "";

        for (String day : days) {
            returnString += day + " ";
        }

        return returnString;

    }
}
