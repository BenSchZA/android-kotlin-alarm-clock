/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.util;

import android.content.SharedPreferences;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;

import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
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
import static com.roostermornings.android.util.JSONPersistence.SharedPrefsKeys.KEY_DATE_LOCK;
import static com.roostermornings.android.util.JSONPersistence.SharedPrefsKeys.KEY_MEDIA_ITEMS_ARRAY;
import static com.roostermornings.android.util.JSONPersistence.SharedPrefsKeys.KEY_CHANNEL_STORY_ITERATION;
import static com.roostermornings.android.util.JSONPersistence.SharedPrefsKeys.KEY_ROOSTER_USER;
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
    private static Gson gson;

    public class SharedPrefsKeys {
        public static final String KEY_CHANNEL_STORY_ITERATION = "KEY_CHANNEL_STORY_ITERATION";
        public static final String KEY_DATE_LOCK = "KEY_DATE_LOCK";
        public static final String KEY_USER_FRIENDS_ARRAY = "KEY_USER_FRIENDS_ARRAY";
        public static final String KEY_USER_INVITABLE_CONTACTS_ARRAY = "KEY_USER_INVITABLE_CONTACTS_ARRAY";
        public static final String KEY_USER_CONTACTS_NUMBER_NAME_PAIRS_MAP = "KEY_USER_CONTACTS_NUMBER_NAME_PAIRS_MAP";
        public static final String KEY_MEDIA_ITEMS_ARRAY = "KEY_MEDIA_ITEMS_ARRAY";
        public static final String KEY_ALARM_CHANNEL_ROOSTERS_ARRAY = "KEY_ALARM_CHANNEL_ROOSTERS_ARRAY";
        public static final String KEY_ALARMS_ARRAY = "KEY_ALARMS_ARRAY";
        public static final String KEY_USER_GEOHASH_ENTRY_ARRAY = "KEY_USER_GEOHASH_ENTRY_ARRAY";
        public static final String KEY_ROOSTER_USER = "KEY_ROOSTER_USER";
    }

    @Inject @Named("default") SharedPreferences defaultSharedPreferences;

    public JSONPersistence() {
        BaseApplication.getRoosterApplicationComponent().inject(this);

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(MediaBrowserCompat.MediaItem.class, new MediaItemInstanceCreator());
        gson = gsonBuilder.create();
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

    private void clearJSONString(String key) {
        if(defaultSharedPreferences != null) {
            defaultSharedPreferences.edit().remove(key).apply();
        }
    }

    class MediaItemInstanceCreator implements InstanceCreator<MediaBrowserCompat.MediaItem> {
        public MediaBrowserCompat.MediaItem createInstance(Type type) {
            MediaDescriptionCompat mediaDescription = new MediaDescriptionCompat.Builder().setMediaId("mediaId").build();
            return new MediaBrowserCompat.MediaItem(mediaDescription, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
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

    public User getRoosterUser() {
        User user = new User();
        try {
            if(getJSONString(KEY_ROOSTER_USER) != null) {
                Type type = new TypeToken<User>(){}.getType();
                if(gson.fromJson(getJSONString(KEY_ROOSTER_USER), type) != null) {
                    return gson.fromJson(getJSONString(KEY_ROOSTER_USER), type);
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return null;
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setRoosterUser(User user) {
        if(user == null) return;
        try {
            putJSONString(KEY_USER_FRIENDS_ARRAY, gson.toJson(user));
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

    public ArrayList<MediaBrowserCompat.MediaItem> getMediaItems() {
        ArrayList<MediaBrowserCompat.MediaItem> returnArray = new ArrayList<>();
        try {
            if(getJSONString(KEY_MEDIA_ITEMS_ARRAY) != null) {
                Type type = new TypeToken<ArrayList<MediaBrowserCompat.MediaItem>>(){}.getType();
                if(gson.fromJson(getJSONString(KEY_MEDIA_ITEMS_ARRAY), type) != null) {
                    return gson.fromJson(getJSONString(KEY_MEDIA_ITEMS_ARRAY), type);
                } else {
                    return returnArray;
                }
            } else {
                return returnArray;
            }
        } catch(Exception e) {
            e.printStackTrace();
            clearJSONString(KEY_MEDIA_ITEMS_ARRAY);
            Crashlytics.logException(e);
            return returnArray;
        }
    }

    public void setMediaItems(ArrayList<MediaBrowserCompat.MediaItem> mMediaItems) {
        if(mMediaItems == null) return;
        try {
            putJSONString(KEY_MEDIA_ITEMS_ARRAY, gson.toJson(mMediaItems));
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

    public void setDateLock(String lockedItem, Long currentTime) {
        if(lockedItem == null) return;
        try {
            putJSONObject(KEY_DATE_LOCK, getJSONObject(KEY_DATE_LOCK).put(lockedItem, currentTime));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public long getDateLock(String lockedItem) {
        if(lockedItem == null) return 0L;
        try {
            if(!getJSONObject(KEY_DATE_LOCK).get(lockedItem).toString().isEmpty()) {
                try {
                    return Long.valueOf(getJSONObject(KEY_DATE_LOCK).get(lockedItem).toString());
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    return 0L;
                }
            } else {
                return 0L;
            }
        } catch (JSONException e) {
            return 0L;
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
