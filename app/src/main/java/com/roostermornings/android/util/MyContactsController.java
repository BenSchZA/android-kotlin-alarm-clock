/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.util;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by bscholtz on 04/03/17.
 */

public class MyContactsController {

    String TAG = MyContactsController.class.getSimpleName();

    private Context context;

    private Map<String, String> contactsMap;
    private JSONObject CountryCodes;
    private JSONArray CountryCodesArray;

    public MyContactsController(Context c) {
        this.context = c;
    }

    public ArrayList<String> processContacts() {
        contactsMap = getContacts(context);
        JSONObject JSONContactsObject = new JSONObject(contactsMap);
        JSONObject JSONProcessedContactsObject = new JSONObject();
        ArrayList<String> ProcessedContactsArray = new ArrayList<>();
        String NSNNumber;

        try {
            Iterator<String> JSONKeyIterator = JSONContactsObject.keys();
            while (JSONKeyIterator.hasNext()) {
                String key = JSONKeyIterator.next();
                String contactNumber = JSONContactsObject.get(key).toString();
                contactNumber = contactNumber.replaceAll("[^0-9+]", "");
                JSONContactsObject.put(key, contactNumber);
                NSNNumber = processContactCountry(contactNumber);
                JSONProcessedContactsObject.put(key, NSNNumber);
                ProcessedContactsArray.add(NSNNumber);
            }
        }
        catch (org.json.JSONException e){
            e.printStackTrace();
        }

        return ProcessedContactsArray;
    }

    public String processContactCountry(String contactNumber) {
        String NSNNumber;
        NSNNumber = null;

        try {
            CountryCodesArray = new JSONArray(loadJSONFromAsset("CountryCodes.json"));
            CountryCodes = CountryCodesArray.getJSONObject(0);
        } catch(org.json.JSONException e){
            e.printStackTrace();
        }

        if(contactNumber.charAt(0) == '+'){
            if(inJSONNode(CountryCodes, "kThreeDigitCodes", contactNumber.substring(1,4))){
                if(contactNumber.charAt(4) == 0) NSNNumber = contactNumber.substring(5);
                else NSNNumber = contactNumber.substring(4);
            }
            else if(inJSONNode(CountryCodes, "kTwoDigitCodes", contactNumber.substring(1,3))){
                if(contactNumber.charAt(3) == 0) NSNNumber = contactNumber.substring(4);
                else NSNNumber = contactNumber.substring(3);
            }
            else if(inJSONNode(CountryCodes, "kOneDigitCodes", contactNumber.substring(1,2))){
                if(contactNumber.charAt(2) == 0) NSNNumber = contactNumber.substring(3);
                else NSNNumber = contactNumber.substring(2);
            }
        } else{
            if(contactNumber.charAt(0) == '0'){
                NSNNumber = contactNumber.substring(1);
            }else{
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
            while(JSONKeyIterator.hasNext()){
                if(JSONKeyIterator.next().contentEquals(find)) {
                    inJSONNode = true;
                    break;
                }
            }
        }catch(org.json.JSONException e) {
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

    private static Map<String, String> getContacts(Context context)
    {
        Map<String, String> result = new HashMap<>();
        Cursor cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        while(cursor.moveToNext())
        {
            int phone_idx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            int name_idx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            String phone = cursor.getString(phone_idx);
            String name = cursor.getString(name_idx);
            result.put(name, phone);
        }
        cursor.close();

        return result;
    }
}
