/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.util.Pair;

import com.roostermornings.android.R;
import com.roostermornings.android.domain.Contact;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.sql.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by bscholtz on 04/03/17.
 */

public class MyContactsController {

    String TAG = MyContactsController.class.getSimpleName();

    private Context context;

    private ArrayList<Contact> contactArray = new ArrayList<>();
    private JSONObject countryCodes;
    private JSONArray countryCodesArray;

    public MyContactsController(Context c) {
        this.context = c;
    }

    private HashMap<String, Set<String>> getContactNumbers() {
        //Stores a map of name with a set of unique numbers
        HashMap<String, Set<String>> uniqueContactMap = new HashMap<>();

        ContentResolver cr = context.getContentResolver();

        //Ensure only contacts with phone number returned
        String selection = ContactsContract.Contacts.HAS_PHONE_NUMBER
                + "=1";

        //Include DISPLAY_NAME, NUMBER, TYPE columns in cursor
        String[] projection = {ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.TYPE};

        //Sort by DISPLAY_NAME in ascending order
        String sortOrder = ContactsContract.Contacts.DISPLAY_NAME
                + " COLLATE LOCALIZED ASC";

        Cursor phones = cr.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, selection, null, sortOrder);

        if(phones == null) return null;
        //If the cursor is not empty, continue
        if (phones.getCount() > 0) {
            //Iterate over all row entries
            while (phones.moveToNext()) {
                //Retrieve the name, number, and type - store this in Set to be added to HashMap
                String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                Set<String> uniqueContactNumbers = uniqueContactMap.get(name);
                if(uniqueContactNumbers == null) uniqueContactNumbers = new HashSet<>();
                String number = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                int type = phones.getInt(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                switch (type) {
                    case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
                        break;
                    case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
                        break;
                    case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
                        break;
                }

                //Remove all spaces, to match duplicates
                number = number.replace(" ", "");
                uniqueContactNumbers.add(number);
                uniqueContactMap.put(name, uniqueContactNumbers);
            }
            phones.close();
        }
        return uniqueContactMap;
    }

    public HashMap<String, String> getNumberNamePairs() {
        HashMap<String, String> numberNamePairs = new HashMap<>();
        ArrayList<Contact> contactArray = getContacts();

        for (Contact contact:
                contactArray) {
            for (String number:
                    contact.getNumbers().keySet()) {
                numberNamePairs.put(number, contact.getName());
            }
        }
        return numberNamePairs;
    }

    private String getDefaultCountryCode() {
        TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);

        //Convert the ISO number to a valid country code
        String ISONumber;
        ISONumber = tm.getSimCountryIso().toUpperCase();
        return CountryISOToPrefix.prefixFor(ISONumber);
    }

    public ArrayList<Contact> getContacts() {
        //If contacts have already been retrieved, then abort
        if(!contactArray.isEmpty()) return contactArray;

        HashMap<String, Set<String>> uniqueContactMap = getContactNumbers();
        String NSNNumber;
        String ISONumber;
        StrUtils.StringPair isoNsnPair;

        try {
            countryCodesArray = new JSONArray(loadJSONFromAsset("CountryCodes.json"));
            countryCodes = countryCodesArray.getJSONObject(0);
        } catch (org.json.JSONException e) {
            e.printStackTrace();
        }

        for (String name:
                uniqueContactMap.keySet()) {
            Contact contact = new Contact(name);
            for (String contactNumber:
                    uniqueContactMap.get(name)) {
                try {
                    contactNumber = contactNumber.replaceAll("[^0-9+]", "");
                    isoNsnPair = processContactCountry(contactNumber);
                    ISONumber = isoNsnPair.getV();
                    NSNNumber = isoNsnPair.getK();
                    contact.addNumber(NSNNumber, ISONumber);
                } catch (NullPointerException e){
                    e.printStackTrace();
                }
            }
            contactArray.add(contact);
        }

        return contactArray;
    }

    public ArrayList<String> getNodeNumberList() {
        ArrayList<Contact> contactArray = getContacts();

        ArrayList<String> phoneNumberList = new ArrayList<>();

        for (Contact contact:
                contactArray) {
            for (String NSNNumber:
                 contact.getNumbers().keySet()) {
                phoneNumberList.add(NSNNumber);
            }
        }
        return phoneNumberList;
    }

    public String processUserContactNumber(String contactNumber) {
        try {
            countryCodesArray = new JSONArray(loadJSONFromAsset("CountryCodes.json"));
            countryCodes = countryCodesArray.getJSONObject(0);
        } catch (org.json.JSONException e) {
            e.printStackTrace();
        }

        try {
            contactNumber = contactNumber.replaceAll("[^0-9+]", "");
            //Return NSN number
            return  processContactCountry(contactNumber).getK();
        } catch (NullPointerException e){
            e.printStackTrace();
            return "";
        }
    }

    private StrUtils.StringPair processContactCountry(String contactNumber) {
        String NSNNumber = "";
        String ISONumber = "";

        if (!StrUtils.notNullOrEmpty(contactNumber)) return new StrUtils.StringPair<>("","");

        if (countryCodesArray != null
                && countryCodes != null
                && contactNumber.charAt(0) == '+') {

            if (contactNumber.length() > 4 && inJSONNode(countryCodes, "kThreeDigitCodes", contactNumber.substring(1, 4))) {
                if (contactNumber.charAt(4) == 0) NSNNumber = contactNumber.substring(5);
                else NSNNumber = contactNumber.substring(4);
                ISONumber = contactNumber.replace(NSNNumber, "");
            } else if (contactNumber.length() > 3 && inJSONNode(countryCodes, "kTwoDigitCodes", contactNumber.substring(1, 3))) {
                if (contactNumber.charAt(3) == 0) NSNNumber = contactNumber.substring(4);
                else NSNNumber = contactNumber.substring(3);
                ISONumber = contactNumber.replace(NSNNumber, "");
            } else if (contactNumber.length() > 2 && inJSONNode(countryCodes, "kOneDigitCodes", contactNumber.substring(1, 2))) {
                if (contactNumber.charAt(2) == 0) NSNNumber = contactNumber.substring(3);
                else NSNNumber = contactNumber.substring(2);
                ISONumber = contactNumber.replace(NSNNumber, "");
            }
        } else {
            if (contactNumber.charAt(0) == '0') NSNNumber = contactNumber.substring(1);
            else NSNNumber = contactNumber;
            //If no valid country code can be extracted, then assume a local number
            ISONumber = getDefaultCountryCode();
        }

        if(!StrUtils.notNullOrEmpty(NSNNumber)) NSNNumber = "";
        return new StrUtils.StringPair<>(NSNNumber, ISONumber);
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
