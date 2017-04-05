/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.domain;

import com.google.firebase.database.IgnoreExtraProperties;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;

/**
 * Created by bscholtz on 06/03/17.
 */

@IgnoreExtraProperties
public class NodeUsers {

    @Expose
    public ArrayList<ArrayList<Friend>> users;

}
