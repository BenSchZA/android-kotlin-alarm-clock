/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.util;

/**
 * Created by bscholtz on 08/03/17.
 */

import java.io.PrintWriter;
import java.io.StringWriter;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;

import com.roostermornings.android.activity.SplashActivity;

public class ExceptionHandler implements
        java.lang.Thread.UncaughtExceptionHandler {
    private final Activity context;
    private final String LINE_SEPARATOR = "\n";

    public ExceptionHandler(Activity c) {
        this.context = c;
    }

    public void uncaughtException(Thread thread, Throwable exception) {
        StringWriter stackTrace = new StringWriter();
        exception.printStackTrace(new PrintWriter(stackTrace));
        StringBuilder errorReport = new StringBuilder();
        errorReport.append("************ CAUSE OF ERROR ************\n\n");
        errorReport.append(stackTrace.toString());

        errorReport.append("\n************ DEVICE INFORMATION ***********\n");
        errorReport.append("Brand: ");
        errorReport.append(Build.BRAND);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("Device: ");
        errorReport.append(Build.DEVICE);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("Model: ");
        errorReport.append(Build.MODEL);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("Id: ");
        errorReport.append(Build.ID);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("Product: ");
        errorReport.append(Build.PRODUCT);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("\n************ FIRMWARE ************\n");
        errorReport.append("SDK: ");
        errorReport.append(Build.VERSION.SDK);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("Release: ");
        errorReport.append(Build.VERSION.RELEASE);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("Incremental: ");
        errorReport.append(Build.VERSION.INCREMENTAL);
        errorReport.append(LINE_SEPARATOR);

        Intent intent = new Intent(context, SplashActivity.class);
        intent.setFlags (Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("error", errorReport.toString());
        context.startActivity(intent);

        System.exit(1);
    }
}
