/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.fragment;

import com.roostermornings.android.domain.Alarm;

/**
 * Created by steven on 2017/02/16.
 */

public interface IAlarmSetListener {

    public void setAlarmDetails(Alarm alarm);
    public Alarm getAlarmDetails();
    public void retrieveAlarmDetailsFromSQL();
    public void setNextButtonCaption(String text);
}
