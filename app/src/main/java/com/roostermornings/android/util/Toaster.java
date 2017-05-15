/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.util;

import android.content.Context;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

public class Toaster {

    private static Toast myToast;
    private static ToastyToast toastyToast = new ToastyToast();

    private Toaster() {
    }

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

    //Make toast, and don't allow people to queue for toast... only one static toast allowed
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

    //Quickly! Get rid of all toast!
    public static boolean burnTheToast() {
        if(myToast != null) {
            myToast.cancel();
            return true;
        }
        return false;
    }
}
