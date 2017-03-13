package com.roostermornings.android.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class InternetHelper {

        public static boolean noInternetConnection(Context context) {
            ConnectivityManager connectivityManager
                    = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            if (activeNetworkInfo == null || !activeNetworkInfo.isConnected()) {
                return true;
            }

            return false;
        }
}
