/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.util;

import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.domain.Alarm;
import com.roostermornings.android.domain.ChannelRooster;
import com.roostermornings.android.domain.Contact;
import com.roostermornings.android.domain.User;
import com.roostermornings.android.geolocation.GeoHashUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Named;

import static com.roostermornings.android.util.JSONPersistence.SharedPrefsKeys.KEY_ALARMS_ARRAY;
import static com.roostermornings.android.util.JSONPersistence.SharedPrefsKeys.KEY_ALARM_CHANNEL_ROOSTERS_ARRAY;
import static com.roostermornings.android.util.JSONPersistence.SharedPrefsKeys.KEY_CHANNEL_ROOSTERS_ARRAY;
import static com.roostermornings.android.util.JSONPersistence.SharedPrefsKeys.KEY_CHANNEL_STORY_ITERATION;
import static com.roostermornings.android.util.JSONPersistence.SharedPrefsKeys.KEY_USER_FRIENDS_ARRAY;
import static com.roostermornings.android.util.JSONPersistence.SharedPrefsKeys.KEY_USER_GEOHASH_ENTRY_ARRAY;
import static com.roostermornings.android.util.JSONPersistence.SharedPrefsKeys.KEY_USER_INVITABLE_CONTACTS_ARRAY;

/**
 * JSONPersistence Class
 *
 * A class for persisting, and safely fetching, various objects (predominantly Arrays) from
 * shared prefs in JSON String form.
 *
 * e.g. alarms, friends, channels etc.
 *
 * @author bscholtz
 * @version 1
 * @since 15/05/17
 */

public class JSONPersistence {
    private static Gson gson = new Gson();

    public class SharedPrefsKeys {
        public static final String KEY_CHANNEL_STORY_ITERATION = "KEY_CHANNEL_STORY_ITERATION";
        public static final String KEY_USER_FRIENDS_ARRAY = "KEY_USER_FRIENDS_ARRAY";
        public static final String KEY_USER_INVITABLE_CONTACTS_ARRAY = "KEY_USER_INVITABLE_CONTACTS_ARRAY";
        public static final String KEY_USER_CONTACTS_NUMBER_NAME_PAIRS_MAP = "KEY_USER_CONTACTS_NUMBER_NAME_PAIRS_MAP";
        public static final String KEY_CHANNEL_ROOSTERS_ARRAY = "KEY_CHANNEL_ROOSTERS_ARRAY";
        public static final String KEY_ALARM_CHANNEL_ROOSTERS_ARRAY = "KEY_ALARM_CHANNEL_ROOSTERS_ARRAY";
        public static final String KEY_ALARMS_ARRAY = "KEY_ALARMS_ARRAY";
        public static final String KEY_USER_GEOHASH_ENTRY_ARRAY = "KEY_USER_GEOHASH_ENTRY_ARRAY";
    }

    @Inject @Named("default") SharedPreferences defaultSharedPreferences;

    public JSONPersistence() {
        BaseApplication.getRoosterApplicationComponent().inject(this);
    }

    private JSONObject getJSONObject(String key) {
        String JSONString = "";
        if(defaultSharedPreferences != null) JSONString = defaultSharedPreferences.getString(key, "");
        if(JSONString.isEmpty()) return new JSONObject();
        try {
            return new JSONObject(JSONString);
        } catch (JSONException e) {
            e.printStackTrace();
            return new JSONObject();
        }
    }

    private void putJSONObject(String key, JSONObject jsonObject) {
        if(defaultSharedPreferences != null) {
            defaultSharedPreferences.edit().putString(key, jsonObject.toString()).apply();
        }
    }

    private String getJSONString(String key) {
        String JSONString = "";
        if(defaultSharedPreferences != null) JSONString = defaultSharedPreferences.getString(key, "");
        return JSONString;
    }

    private void putJSONString(String key, String Json) {
        if(defaultSharedPreferences != null) {
            defaultSharedPreferences.edit().putString(key, Json).apply();
        }
    }

    public ArrayList<Contact> getInvitableContacts() {
        ArrayList<Contact> returnArray = new ArrayList<>();
        try {
            if(getJSONString(KEY_USER_INVITABLE_CONTACTS_ARRAY) != null) {
                Type type = new TypeToken<ArrayList<Contact>>(){}.getType();
                if(gson.fromJson(getJSONString(KEY_USER_INVITABLE_CONTACTS_ARRAY), type) != null) {
                    return gson.fromJson(getJSONString(KEY_USER_INVITABLE_CONTACTS_ARRAY), type);
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

    public void setInvitableContacts(ArrayList<Contact> contacts) {
        if(contacts == null) return;
        try {
            putJSONString(KEY_USER_INVITABLE_CONTACTS_ARRAY, gson.toJson(contacts));
        } catch (IllegalStateException e) {
            e.printStackTrace();
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

    public ArrayList<ChannelRooster> getChannelRoosters() {
        ArrayList<ChannelRooster> returnArray = new ArrayList<>();
        try {
            if(getJSONString(KEY_CHANNEL_ROOSTERS_ARRAY) != null) {
                Type type = new TypeToken<ArrayList<ChannelRooster>>(){}.getType();
                if(gson.fromJson(getJSONString(KEY_CHANNEL_ROOSTERS_ARRAY), type) != null) {
                    return gson.fromJson(getJSONString(KEY_CHANNEL_ROOSTERS_ARRAY), type);
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

    public void setChannelRoosters(ArrayList<ChannelRooster> mRoosters) {
        if(mRoosters == null) return;
        try {
            putJSONString(KEY_CHANNEL_ROOSTERS_ARRAY, gson.toJson(mRoosters));
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<ChannelRooster> getNewAlarmChannelRoosters() {
        ArrayList<ChannelRooster> returnArray = new ArrayList<>();
        try {
            if(getJSONString(KEY_ALARM_CHANNEL_ROOSTERS_ARRAY) != null) {
                Type type = new TypeToken<ArrayList<ChannelRooster>>(){}.getType();
                if(gson.fromJson(getJSONString(KEY_ALARM_CHANNEL_ROOSTERS_ARRAY), type) != null) {
                    return gson.fromJson(getJSONString(KEY_ALARM_CHANNEL_ROOSTERS_ARRAY), type);
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

    public void setNewAlarmChannelRoosters(ArrayList<ChannelRooster> mRoosters) {
        if(mRoosters == null) return;
        try {
            putJSONString(KEY_ALARM_CHANNEL_ROOSTERS_ARRAY, gson.toJson(mRoosters));
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<Alarm> getAlarms() {
        ArrayList<Alarm> returnArray = new ArrayList<>();
        try {
            if(getJSONString(KEY_ALARMS_ARRAY) != null) {
                Type type = new TypeToken<ArrayList<Alarm>>(){}.getType();
                if(gson.fromJson(getJSONString(KEY_ALARMS_ARRAY), type) != null) {
                    return gson.fromJson(getJSONString(KEY_ALARMS_ARRAY), type);
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

    public void setAlarms(ArrayList<Alarm> mAlarms) {
        if(mAlarms == null) return;
        try {
            putJSONString(KEY_ALARMS_ARRAY, gson.toJson(mAlarms));
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<GeoHashUtils.UserGeoHashEntry> getUserGeoHashEntries() {
        ArrayList<GeoHashUtils.UserGeoHashEntry> returnArray = new ArrayList<>();
        try {
            if(getJSONString(KEY_USER_GEOHASH_ENTRY_ARRAY) != null) {
                Type type = new TypeToken<ArrayList<GeoHashUtils.UserGeoHashEntry>>(){}.getType();
                if(gson.fromJson(getJSONString(KEY_USER_GEOHASH_ENTRY_ARRAY), type) != null) {
                    return gson.fromJson(getJSONString(KEY_USER_GEOHASH_ENTRY_ARRAY), type);
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

    public void setUserGeoHashEntries(ArrayList<GeoHashUtils.UserGeoHashEntry> mEntries) {
        if(mEntries == null) return;
        try {
            putJSONString(KEY_USER_GEOHASH_ENTRY_ARRAY, gson.toJson(mEntries));
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
            return 1;
        }
    }
}
