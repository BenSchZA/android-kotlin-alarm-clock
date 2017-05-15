/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.roostermornings.android.BaseApplication;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONPersistence {
    private static Gson gson = new Gson();

    private static final String KEY_CHANNEL_STORY_ITERATION = "KEY_CHANNEL_STORY_ITERATION";
    private Context context;

    private JSONPersistence(){}

    public JSONPersistence(Context context) {
        this.context = context;
    }

    private JSONObject getJSONData(String key) {
        String JSONString = "";
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if(sharedPreferences != null) JSONString = sharedPreferences.getString(key, "");
        if(JSONString.isEmpty()) return new JSONObject();
        try {
            return new JSONObject(JSONString);
        } catch (JSONException e) {
            e.printStackTrace();
            return new JSONObject();
        }
    }

    private void putJSONData(String key, JSONObject jsonObject) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if(sharedPreferences != null) {
            sharedPreferences.edit().putString(key, jsonObject.toString()).apply();
        }
    }

    public void setStoryIteration(String channelRoosterUID, Integer iteration) {
        if(channelRoosterUID == null || iteration == null) return;
        try {
            putJSONData(KEY_CHANNEL_STORY_ITERATION, getJSONData(KEY_CHANNEL_STORY_ITERATION).put(channelRoosterUID, iteration));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public int getStoryIteration(String channelRoosterUID) {
        if(channelRoosterUID == null) return 1;
        try {
            if(!getJSONData(KEY_CHANNEL_STORY_ITERATION).get(channelRoosterUID).toString().isEmpty()) {
                try {
                    return Integer.valueOf(getJSONData(KEY_CHANNEL_STORY_ITERATION).get(channelRoosterUID).toString());
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    return 1;
                }
            } else {
                return 1;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return 1;
        }
    }
}
