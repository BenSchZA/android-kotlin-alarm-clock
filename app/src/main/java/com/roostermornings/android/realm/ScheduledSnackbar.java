package com.roostermornings.android.realm;

import com.google.gson.annotations.Expose;
import com.roostermornings.android.snackbar.SnackbarManager;

import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;

/**
 * Created by bscholtz on 2017/11/15.
 */

public class ScheduledSnackbar extends RealmObject {
    @Expose
    @PrimaryKey
    public String jsonSnackbarQueueElement = "";
    @Expose
    public String localDisplayClassName = "";
    @Expose
    public Long displayTime = -1L;

    @Ignore
    public SnackbarManager.Companion.SnackbarQueueElement snackbarQueueElement = new SnackbarManager.Companion.SnackbarQueueElement();
}
