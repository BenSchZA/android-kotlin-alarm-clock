package com.roostermornings.android.util;

import com.roostermornings.android.domain.Alarm;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by steven on 2017/02/15.
 */

public class RoosterUtils {

    public static String getAlarmTimeFromHourAndMinute(Alarm alarm) {

        String alarmHour = (alarm.getHour() < 10 ? "0" : "") + alarm.getHour();
        String alarmMinute = (alarm.getMinute() < 10 ? "0" : "") + alarm.getMinute();
        return alarmHour + ":" + alarmMinute;

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
