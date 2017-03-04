package com.roostermornings.android.node_api;

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

public class NodeMyContactsAPI extends AsyncTask<String, Void, String> {

    String TAG = NodeMyContactsAPI.class.getSimpleName();

    private Context context;

    private Map<String, String> contactsMap;
    private JSONObject CountryCodes;
    private JSONArray CountryCodesArray;

    public NodeMyContactsAPI(Context c) {
        this.context = c;
    }

    @Override
    protected String doInBackground(String... params) {
        if(android.os.Debug.isDebuggerConnected())
            android.os.Debug.waitForDebugger();

        String response;
        response = processContacts();
        Log.d(TAG, response);
        publishProgress();

        return response;
    }

    @Override
    protected void onPreExecute() {
        Log.i(TAG, "onPreExecute");
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        Log.i(TAG, "onProgressUpdate");
    }

    private String processContacts() {
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
            //JSONContactsArray = new JSONArray("[" + JSONContactsObject.toString() + "]");
        }
        catch (org.json.JSONException e){
            e.printStackTrace();
        }

        HashMap<String, String> postDataParams = new HashMap<>();
        postDataParams.put("user_contacts", ProcessedContactsArray.toString());
        postDataParams.put("user_token_id", "F8dQ9n7JwRh2u9wKMyveNZ4BRyi2");

        return performPostCall("https://rooster-node.appspot-preview.com/api/my_contacts", postDataParams);
    }

    private String processContactCountry(String contactNumber) {
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

    private String  performPostCall(String requestURL,
                                   HashMap<String, String> postDataParams) {
        //http://stackoverflow.com/questions/9767952/how-to-add-parameters-to-httpurlconnection-using-post

        URL url;
        String response = "";
        try {
            url = new URL(requestURL);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(15000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(getPostDataString(postDataParams));

            writer.flush();
            writer.close();
            os.flush();
            os.close();
            int responseCode = conn.getResponseCode();

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line=br.readLine()) != null) {
                    response+=line;
                }
            }else{
                response = "";
            }
            conn.disconnect();
        }catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    private String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : params.entrySet()){
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }
        return result.toString();
    }

    private static Map<String, String> getContacts(Context context)
    {
        Map<String, String> result = new HashMap<String, String>();
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
