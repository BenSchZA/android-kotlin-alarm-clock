package com.roostermornings.android.util;

import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.roostermornings.android.domain.Alarm;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by steven on 2017/02/15.
 */

public class RoosterUtils {

    private static String mRandomAudioFileName = "ABCDEFGHIJKLMNOP";
    static Random random;

    public static boolean hasLollipop() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public static boolean hasGingerbread() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
    }

    public static String getInitials(String displayName) {
        String initials = "";
        String tempStringArray[];

        tempStringArray = displayName.split(" ", 2);

        for (String s:tempStringArray) {
            initials = initials.concat(String.valueOf(s.charAt(0)));
        }

        return initials;
    }

    public static String setAlarmTimeFromHourAndMinute(Alarm alarm) {

        String alarmHour = (alarm.getHour() < 10 ? "0" : "") + alarm.getHour();
        String alarmMinute = (alarm.getMinute() < 10 ? "0" : "") + alarm.getMinute();
        return alarmHour + ":" + alarmMinute;

    }

    public static String createRandomFileName(int string) {
        random = new Random();
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
        if (days.size() == 5 && days.indexOf("Sun") == -1 && days.indexOf("Sat") == -1)
            return "Weekdays";
        String returnString = "";

        for (String day : days) {
            returnString += day + " ";
        }

        return returnString;

    }
}
