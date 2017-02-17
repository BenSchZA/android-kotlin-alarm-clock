package com.roostermornings.android.fragment;

import com.roostermornings.android.domain.Alarm;

/**
 * Created by steven on 2017/02/16.
 */

public interface IAlarmSetListener {

    public void setAlarmDetails(Alarm alarm);
    public Alarm getAlarmDetails();
}
