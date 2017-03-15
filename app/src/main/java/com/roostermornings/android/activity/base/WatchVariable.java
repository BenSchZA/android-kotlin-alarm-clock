/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity.base;

import java.util.Observable;

public class WatchVariable extends Observable {
    private int someVariable = 0;

    public void setWatchVariable(int someVariable) {
        synchronized (this) {
            this.someVariable = someVariable;
        }
        setChanged();
        notifyObservers();
    }

    public void incrementWatchVariable() {
        synchronized (this) {
            this.someVariable = someVariable + 1;
        }
        setChanged();
        notifyObservers();
    }

    public void clearWatchVariable() {
        this.someVariable = 0;
    }

    public synchronized int getWatchVariable() {
        return someVariable;
    }
}
