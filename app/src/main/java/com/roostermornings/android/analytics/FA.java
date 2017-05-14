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

import static com.roostermornings.android.BaseApplication.firebaseAnalytics;


//FA stands for Firebase Analytics - defines relationship between Event and Param, handles analytic logging in background thread
public abstract class FA {

    public abstract static class Event {
        public abstract class Alarm_creation_begin {
        }
        public abstract static class Alarm_edit_begin {
        }
        public abstract class Alarm_creation_completed {
            public abstract class Param {
                //Bool
                public final static String Social_roosters_enabled = "Social_roosters_enabled";
                public final static String Channel_selected = "Channel_selected";
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
                public final static String Data_loaded = "Data_loaded";
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
        public abstract class Social_rooster_recording_error {
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
        public abstract class Channel_info_viewed extends Channel_selected {
        }
        public abstract class Explore_channel_rooster_played extends Channel_selected {
        }
        public abstract class Default_alarm_play {
            public abstract class Param {
                //Bool
                public final static String Fatal_failure = "Fatal_failure";
                //Bool
                public final static String Attempt_to_play = "Attempt_to_play";
            }
        }
        public abstract class Memory_warning {
            public abstract class Param {
                //Long
                public final static String Memory_in_use = "Memory_in_use";
            }
        }
    }
    
    public static void Log(final Class<?> Event, final String Param, final Object entry) {
        new Thread() {
            @Override
            public void run() {

                Bundle bundle = new Bundle();

                String eventString;

                if (Event == null) {
                    throw new NullPointerException();
                } else {
                    eventString = Event.toString();
                    int eventStringPosition = eventString.lastIndexOf("$") + 1;
                    eventString = eventString.substring(eventStringPosition);
                }

                if (Param == null || entry == null) {
                    firebaseAnalytics.logEvent(eventString, null);
                } else if (entry.getClass().isInstance(String.class)) {
                    bundle.putString(Param, (String) entry);
                    firebaseAnalytics.logEvent(eventString, bundle);
                } else if (entry.getClass().isInstance(Integer.class)) {
                    bundle.putInt(Param, (Integer) entry);
                    firebaseAnalytics.logEvent(eventString, bundle);
                } else if (entry.getClass().isInstance(Boolean.class)) {
                    bundle.putBoolean(Param, (Boolean) entry);
                    firebaseAnalytics.logEvent(eventString, bundle);
                } else {
                }
            }
        }.run();
    }
}
