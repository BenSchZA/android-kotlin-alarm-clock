/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.sql.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by bscholtz on 04/03/17.
 */

public class MyContactsController {

    String TAG = MyContactsController.class.getSimpleName();

    private Context context;

    private ArrayList<String> contactsMap;
    private JSONObject countryCodes;
    private JSONArray countryCodesArray;

    public MyContactsController(Context c) {
        this.context = c;
    }

    private static ArrayList<String> getContacts(Context context) {
        ArrayList<String> result = new ArrayList<>();

        ContentResolver cr = context.getContentResolver();
        Cursor cursor = cr.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null, null);

        if(cursor == null) return null;
        if (cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                result.add(number);
            }
            cursor.close();
        }
        return result;
    }

    public ArrayList<String> processContacts() {
        contactsMap = getContacts(context);
        ArrayList<String> processedContactsArray = new ArrayList<>();
        String NSNNumber;

        try {
            countryCodesArray = new JSONArray(loadJSONFromAsset("CountryCodes.json"));
            countryCodes = countryCodesArray.getJSONObject(0);
        } catch (org.json.JSONException e) {
            e.printStackTrace();
        }

        for (String contactNumber:
             contactsMap) {
            try {
                contactNumber = contactNumber.replaceAll("[^0-9+]", "");
                NSNNumber = processContactCountry(contactNumber);
                if (NSNNumber.length() > 0) processedContactsArray.add(NSNNumber);
            } catch (NullPointerException e){
                e.printStackTrace();
            }
        }
        return processedContactsArray;
    }

    public String processContactCountry(String contactNumber) {
        String NSNNumber;
        NSNNumber = null;

        if (contactNumber == null) return "";

        if (countryCodesArray != null
                && countryCodes != null
                && contactNumber.charAt(0) == '+') {

            if (inJSONNode(countryCodes, "kThreeDigitCodes", contactNumber.substring(1, 4))) {
                if (contactNumber.charAt(4) == 0) NSNNumber = contactNumber.substring(5);
                else NSNNumber = contactNumber.substring(4);
            } else if (inJSONNode(countryCodes, "kTwoDigitCodes", contactNumber.substring(1, 3))) {
                if (contactNumber.charAt(3) == 0) NSNNumber = contactNumber.substring(4);
                else NSNNumber = contactNumber.substring(3);
            } else if (inJSONNode(countryCodes, "kOneDigitCodes", contactNumber.substring(1, 2))) {
                if (contactNumber.charAt(2) == 0) NSNNumber = contactNumber.substring(3);
                else NSNNumber = contactNumber.substring(2);
            }
        } else {
            if (contactNumber.charAt(0) == '0') {
                NSNNumber = contactNumber.substring(1);
            } else {
                NSNNumber = contactNumber;
            }
        }
        return NSNNumber;
    }

    private boolean inJSONNode(JSONObject json_o, String node, String find) {
        boolean inJSONNode;
        inJSONNode = false;
        try {
            Iterator<String> JSONKeyIterator = json_o.getJSONObject(node).keys();
            while (JSONKeyIterator.hasNext()) {
                if (JSONKeyIterator.next().contentEquals(find)) {
                    inJSONNode = true;
                    break;
                }
            }
        } catch (org.json.JSONException e) {
            e.printStackTrace();
        }
        return inJSONNode;
    }

    private String loadJSONFromAsset(String JSONFile) {
        String JSONString;
        try {
            InputStream is = context.getAssets().open(JSONFile);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            JSONString = new String(buffer, "UTF-8");
        } catch (java.io.IOException e) {
            e.printStackTrace();
            return null;
        }
        return JSONString;
    }
}
