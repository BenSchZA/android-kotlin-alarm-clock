/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.analytics;

import android.os.Bundle;

import static com.roostermornings.android.BaseApplication.firebaseAnalytics;


//FA stands for Firebase Analytics - defines relationship between Event and Param, handles analytic logging in background thread
public abstract class FA {

    public abstract static class Event {
        public abstract class alarm_creation_begin {
        }
        public abstract static class alarm_edit_begin {
        }
        public abstract class alarm_creation_completed {
            public abstract class Param {
                //Bool
                public final static String social_roosters_enabled = "social_roosters_enabled";
                public final static String channel_selected = "channel_selected";
            }
        }
        public abstract class alarm_deleted extends channel_selected {
        }
        public abstract class channel_selected {
            public abstract class Param {
                //String param
                public final static String channel_title = "channel_title";
            }
        }
        public abstract class alarm_activated {
            public abstract class Param {
                //Bool
                public final static String data_loaded = "data_loaded";
                //Number
                public final static String social_content_received = "social_content_received";
                //Number
                public final static String channel_content_received = "channel_content_received";
            }
        }
        //First play of rooster
        public abstract class channel_unique_play {
        }
        public abstract class channel_play {
        }
        //First play of rooster
        public abstract class social_rooster_unique_play {
        }
        public abstract class social_rooster_play {
        }
        public abstract class alarm_snoozed {
            public abstract class Param {
                //Number param
                public final static String alarm_activation_total_roosters = "alarm_activation_total_roosters";
                //Number param
                public final static String alarm_activation_index = "alarm_activation_index";
                //Number param
                public final static String alarm_activation_cycle_count = "alarm_activation_cycle_count";
            }
        }
        public abstract class alarm_dismissed extends alarm_snoozed {
        }
        public abstract class social_rooster_recorded {
        }
        public abstract class social_rooster_recording_error {
        }
        public abstract class social_rooster_recording_deleted {
        }
        public abstract class social_rooster_sent {
            public abstract class Param {
                //Number param
                public final static String social_rooster_receivers = "social_rooster_receivers";
            }
        }
        public abstract class invitation_to_join_rooster_sent {
            public abstract class Param {
                //String param
                public final static String download_link_share_medium = "download_link_share_medium";
            }
        }
        public abstract class channel_info_viewed extends channel_selected {
        }
        public abstract class explore_channel_rooster_played extends channel_selected {
        }
        public abstract class default_alarm_play {
            public abstract class Param {
                //Bool
                public final static String fatal_failure = "fatal_failure";
                //Bool
                public final static String attempt_to_play = "attempt_to_play";
            }
        }
        public abstract class memory_warning {
            public abstract class Param {
                //Long
                public final static String memory_in_use = "memory_in_use";
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
