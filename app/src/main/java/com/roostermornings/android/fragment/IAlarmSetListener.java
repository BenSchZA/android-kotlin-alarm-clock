/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.fragment;

import com.roostermornings.android.domain.database.Alarm;

/**
 * Created by steven on 2017/02/16.
 */

public interface IAlarmSetListener {

    void setAlarmDetails(Alarm alarm);
    Alarm getAlarmDetails();
    void configureAlarmDetails();
    void setNextButtonCaption(String text);
}
