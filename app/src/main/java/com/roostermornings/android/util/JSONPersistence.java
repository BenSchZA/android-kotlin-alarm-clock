/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.roostermornings.android.domain.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class JSONPersistence {
    private static Gson gson = new Gson();

    private static final String KEY_CHANNEL_STORY_ITERATION = "KEY_CHANNEL_STORY_ITERATION";
    private static final String KEY_USER_FRIENDS_ARRAY = "KEY_USER_FRIENDS_ARRAY";

    private Context context;

    private JSONPersistence(){}

    public JSONPersistence(Context context) {
        this.context = context;
    }

    private JSONObject getJSONObject(String key) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        String JSONString = "";
        if(sharedPreferences != null) JSONString = sharedPreferences.getString(key, "");
        if(JSONString.isEmpty()) return new JSONObject();
        try {
            return new JSONObject(JSONString);
        } catch (JSONException e) {
            e.printStackTrace();
            return new JSONObject();
        }
    }

    private void putJSONObject(String key, JSONObject jsonObject) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        if(sharedPreferences != null) {
            sharedPreferences.edit().putString(key, jsonObject.toString()).apply();
        }
    }

    private String getJSONString(String key) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        String JSONString = "";
        if(sharedPreferences != null) JSONString = sharedPreferences.getString(key, "");
        return JSONString;
    }

    private void putJSONString(String key, String Json) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        if(sharedPreferences != null) {
            sharedPreferences.edit().putString(key, Json).apply();
        }
    }

    public ArrayList<User> getFriends() {
        ArrayList<User> returnArray = new ArrayList<>();
        try {
            if(getJSONString(KEY_USER_FRIENDS_ARRAY) != null) {
                Type type = new TypeToken<ArrayList<User>>(){}.getType();
                if(gson.fromJson(getJSONString(KEY_USER_FRIENDS_ARRAY), type) != null) {
                    return gson.fromJson(getJSONString(KEY_USER_FRIENDS_ARRAY), type);
                } else {
                    return returnArray;
                }
            } else {
                return returnArray;
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return returnArray;
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            return returnArray;
        }
    }

    public void setFriends(ArrayList<User> mUsers) {
        if(mUsers == null) return;
        try {
            putJSONString(KEY_USER_FRIENDS_ARRAY, gson.toJson(mUsers));
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public void setStoryIteration(String channelRoosterUID, Integer iteration) {
        if(channelRoosterUID == null || iteration == null) return;
        try {
            putJSONObject(KEY_CHANNEL_STORY_ITERATION, getJSONObject(KEY_CHANNEL_STORY_ITERATION).put(channelRoosterUID, iteration));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public int getStoryIteration(String channelRoosterUID) {
        if(channelRoosterUID == null) return 1;
        try {
            if(!getJSONObject(KEY_CHANNEL_STORY_ITERATION).get(channelRoosterUID).toString().isEmpty()) {
                try {
                    return Integer.valueOf(getJSONObject(KEY_CHANNEL_STORY_ITERATION).get(channelRoosterUID).toString());
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
