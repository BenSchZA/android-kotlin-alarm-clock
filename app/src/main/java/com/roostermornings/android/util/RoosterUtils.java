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

    public static boolean hasO() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

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

    public static boolean hasJellyBeanMR2() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
    }

    public static boolean hasGingerbread() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
    }

    public static boolean hasIceCreamSandwich() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    }

    public static Boolean notNull(Object object) {
        return object != null;
    }

    public static String getInitials(String displayName) {
        try {
            String initials = "";
            String tempStringArray[];
            if (!"".equals(displayName)) {
                tempStringArray = displayName.split(" ", 2);
                for (String s : tempStringArray) {
                    if (!s.isEmpty())
                        initials = initials.concat(String.valueOf(s.charAt(0)).toUpperCase());
                }
            }
            return initials;
        } catch (NullPointerException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String setAlarmTimeFromHourAndMinute(Alarm alarm, boolean twentyFourFormat) {

        String alarmHour;
        String alarmMinute;
        if(twentyFourFormat) {
            alarmHour = (alarm.getHour() < 10 ? "0" : "") + alarm.getHour();
            alarmMinute = (alarm.getMinute() < 10 ? "0" : "") + alarm.getMinute();
        } else {
            if(alarm.getHour() > 12) {
                alarmHour = (alarm.getHour() < 10 ? "0" : "") + (alarm.getHour() - 12);
            } else {
                alarmHour = (alarm.getHour() < 10 ? "0" : "") + alarm.getHour();
            }
            alarmMinute = (alarm.getMinute() < 10 ? "0" : "") + alarm.getMinute();
        }
        return alarmHour + ":" + alarmMinute;
    }

    public static String createRandomUID(int string) {
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
