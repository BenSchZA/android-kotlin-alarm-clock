/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.analytics;

import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.activity.IntroFragmentActivity;

import java.lang.reflect.Type;

public abstract class FA {

    public static NoParam NoParam = new NoParam();
    private static class NoParam{
        NoParam NoParam(){
            return this;
        }
    }

    public abstract static class Event {
        public abstract class Alarm_creation_begin {
        }
        public abstract static class Alarm_edit_begin {
        }
        public abstract class Alarm_creation_completed {
            public abstract class Param {
                //Bool
                public final static String No_rooster_content = "No_rooster_content";
                public final static String Social_rooster_content = "Social_rooster_content";
                public final static String Channel_rooster_content = "Channel_rooster_content";
                public final static String Social_channel_rooster_content = "Social_channel_rooster_content";
            }
        }
        public abstract class Channel_selected {
            public abstract class Param {
                //String param
                public final static String Channel_title = "Channel_title";
            }
        }
        public abstract class Alarm_activated {
            public abstract class Param {
                //Bool
                public final static String Default_alarm_fail_safe = "Default_alarm_fail_safe";
                //Bool
                public final static String Alarm_content_stream = "Alarm_content_stream";
                //Bool
                public final static String Alarm_content_downloaded = "Alarm_content_downloaded";
                //Bool
                public final static String Default_alarm = "Default_alarm";
                //Number
                public final static String Social_content_received = "Social_content_received";
                //Number
                public final static String Channel_content_received = "Channel_content_received";
            }
        }
        //First play of rooster
        public abstract class Channel_unique_play {
        }
        public abstract class Channel_play {
        }
        //First play of rooster
        public abstract class Social_rooster_unique_play {
        }
        public abstract class Social_rooster_play {
        }
        public abstract class Alarm_snoozed {
            public abstract class Param {
                //Number param
                public final static String Alarm_activation_total_roosters = "Alarm_activation_total_roosters";
                //Number param
                public final static String Alarm_activation_index = "Alarm_activation_index";
                //Number param
                public final static String Alarm_activation_cycle_count = "Alarm_activation_cycle_count";
            }
        }
        public abstract class Alarm_dismissed extends Alarm_snoozed {
        }
        public abstract class Social_rooster_recorded {
        }
        public abstract class Social_rooster_recording_deleted {
        }
        public abstract class Social_rooster_sent {
            public abstract class Param {
                //Number param
                public final static String Social_rooster_receivers = "Social_rooster_receivers";
            }
        }
        public abstract class Invitation_to_join_rooster_sent {
            public abstract class Param {
                //String param
                public final static String Download_link_share_medium = "Download_link_share_medium";
            }
        }
        public abstract class Channel_previewed extends Channel_selected {
        }
        public abstract class Discover_channel_audio_played extends Channel_selected {
        }
        public abstract class Default_alarm_play {
        }
    }
    
    public static Boolean Log(Class<?> Event, String Param, Object entry) {
        Bundle bundle = new Bundle();
        FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(BaseApplication.AppContext);

        String eventString;

        if(Event == null) {
            throw new NullPointerException();
        } else {
            eventString = Event.toString();
            int eventStringPosition = eventString.lastIndexOf("$") + 1;
            eventString = eventString.substring(eventStringPosition);
        }

        if(Param == null || entry == null) {
            firebaseAnalytics.logEvent(eventString, null);
            return true;
        }
        else if(entry.getClass().isInstance(String.class)) {
            bundle.putString(Param, (String) entry);
            firebaseAnalytics.logEvent(eventString, bundle);
            return true;
        }
        else if(entry.getClass().isInstance(Integer.class)) {
            bundle.putInt(Param, (Integer) entry);
            firebaseAnalytics.logEvent(eventString, bundle);
            return true;
        }
        else if (entry.getClass().isInstance(Boolean.class)) {
            bundle.putBoolean(Param, (Boolean) entry);
            firebaseAnalytics.logEvent(eventString, bundle);
            return true;
        }
        else {
            return false;
        }
    }
}
