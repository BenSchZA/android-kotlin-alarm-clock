/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.domain.database;

import com.google.firebase.database.IgnoreExtraProperties;
import com.roostermornings.android.domain.database.User;

import java.util.ArrayList;

/**
 * Created by steven on 2017/02/22.
 */

@IgnoreExtraProperties
public class Users {

    public ArrayList<User> users;

}
