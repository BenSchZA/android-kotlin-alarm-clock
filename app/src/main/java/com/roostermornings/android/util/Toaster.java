/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.util;

import android.content.Context;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

/**
 * Toaster Class
 *
 * A class used for creating, managing, authenticating, and destroying Toasts.
 * This is some highly advanced Toast, butter side up.
 *
 * @author bscholtz
 * @version 1
 * @since 16/05/17
 */

public class Toaster {

    private static Toast myToast;
    private static ToastyToast toastyToast = new ToastyToast();

    private Toaster() {
    }

    /**
     * A custom Toast class that allows us to ensure a Toast is only shown to authenticated users,
     * in certain cases.
     */

    //Ensure toast is for a valid user, toast doesn't grow on trees
    public static class ToastyToast {
        public boolean checkTastyToast() {
            if(FirebaseAuth.getInstance().getCurrentUser() == null){
                myToast.cancel();
                return false;
            } else {
                return true;
            }
        }
    }

    /**
     * Make toast, and don't allow people to queue for toast... only one static toast allowed
     */

    public static ToastyToast makeToast(Context wheresMyToast, String toastedText, int toastyLength) {
        if(myToast == null) {
            myToast = Toast.makeText(wheresMyToast, toastedText, toastyLength);
            myToast.show();
        } else {
            myToast.setText(toastedText);
            myToast.setDuration(toastyLength);
            myToast.show();
        }
        return toastyToast;
    }

    /**
     * Quickly! Get rid of all toast!
     * @return whether the action was performed or not.
     */

    public static boolean burnTheToast() {
        if(myToast != null) {
            myToast.cancel();
            return true;
        }
        return false;
    }
}
