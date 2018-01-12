/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;

import com.roostermornings.android.domain.local.Contact;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * MyContactsController Class
 *
 * This class allows you to retrieve, validate, and process local contacts, as well as
 * load JSON file assets (country codes) and ISO code mappings (ZA -> +27)
 *
 * @author bscholtz
 * @version 1
 * @since 04/03/17
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

    /**
     * Get a map of all contact names, with a set of their unique contact numbers (some contacts
     * will have multiple contacts, and we need to fetch them all for checking which contacts
     * have Rooster installed)
     * @return HashMap - name : list of numbers
     */

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

    /**
     * Get a list of unique NSN numbers mapped to the user's name.
     * @return as above
     */

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

    /**
     * Convert the country ISO code to country prefix (ZA -> +27)
     * @return String of country prefix, e.g. +27
     * @see CountryISOToPrefix
     */

    private String getDefaultCountryCode() {
        TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);

        //Convert the ISO number to a valid country code
        String ISONumber = "";
        if(tm != null) {
            ISONumber = tm.getSimCountryIso().toUpperCase();
        }
        return CountryISOToPrefix.prefixFor(ISONumber);
    }

    /**
     * Takes a String, entry, and removes invalid characters for a properly formatted number
     * e.g. A '+' character can only appear at index 0
     * @param entry String phone number
     * @return processed String with valid characters and placement
     */

    public static String clearInvalidCharacters(String entry) {
        if(!StrUtils.notNullOrEmpty(entry)) return "";

        entry = entry.replaceAll("[^0-9+]", "");

        //Ensure only "+" char is at index 0
        if(entry.contains("+")) {
            int plusCount = entry.length() - entry.replace("+", "").length();

            if(!(entry.indexOf("+") == 0)) {
                entry = entry.replace("+", "");
            } else if(plusCount > 1) {
                StringBuilder entryString = new StringBuilder(entry);
                while(entryString.lastIndexOf("+") > 0) {
                    entryString.setCharAt(entryString.lastIndexOf("+"), Character.MIN_VALUE);
                }
                return entryString.toString();
            }
        }

        return entry;
    }

    /**
     * Checks if entry has invalid characters or placement, so that it can be processed by
     * clearInvalidCharacters function.
     * @param entry
     * @return boolean containsInvalidCharacters? If true, process
     */

    public static boolean containsInvalidCharacters(String entry) {
        boolean entryContainsPlusInvalidPosition = entry.contains("+") && !entry.startsWith("+");
        boolean entryContainsInvalidCharacters = !entry.equals(entry.replaceAll("[^0-9+]", ""));

        return entryContainsInvalidCharacters || entryContainsPlusInvalidPosition;
    }

    /**
     * Using getContactNumbers function first, process these into Contact objects, with:
     *  - Primary number
     *  - Name
     *  - HashMap of unique by NSN numbers split into NSNNumber, and ISO number
     *
     * @return ArrayList of processed Contact objects
     */

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

    /**
     * Get an ArrayList of NSN numbers to send to Node.
     * @return All the NSN numbers (keySet) from each Contact
     */

    public ArrayList<String> getNodeNumberList() {
        ArrayList<Contact> contactArray = getContacts();

        ArrayList<String> phoneNumberList = new ArrayList<>();

        for (Contact contact:
                contactArray) {
            phoneNumberList.addAll(contact.getNumbers().keySet());
        }
        return phoneNumberList;
    }

    /**
     * On contact number entry, process the contact number to remove invalid characters.
     * @param contactNumber User's entered contact number
     * @return processed contactNumber
     */

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

    /**
     * Take a contact number and split it into a valid NSN, ISO pair.
     * @param contactNumber User's entered contact number
     * @return StringPair of NSNNumber : ISONumber
     * @see com.roostermornings.android.util.StrUtils.StringPair
     */

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

    /**
     * Check a specific node ina JSON Object to see if it contains a string.
     * This is used for searching the country codes for valid contact number lengths.
     * @param json_o The JSON Object of country codes, in this case
     * @param node The specific node, e.g. "kThreeDigitCodes", to search in
     * @param find The string to find.
     * @return a boolean indicating if the string was found
     */

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

    /**
     * A .json file is passed in using the file name found in the android "assets" directory.
     * @param JSONFile .json file name
     * @return a JSON string to be parsed into a JSON object
     */

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
