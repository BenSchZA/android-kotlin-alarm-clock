/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity.base;

import java.util.Observable;
import java.util.Observer;

public class WatchObserver implements Observer {
    public void observe(Observable o) {
        o.addObserver(this);
    }

    @Override
    public void update(Observable o, Object arg) {
        int someVariable = ((WatchVariable) o).getWatchVariable();
        System.out.println("Variable changed to " + someVariable);
    }
}
