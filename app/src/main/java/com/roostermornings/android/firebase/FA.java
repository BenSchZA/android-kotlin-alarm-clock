/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.firebase;

import android.os.Bundle;
import android.support.annotation.Keep;

import com.roostermornings.android.BaseApplication;

//FA stands for Firebase Analytics - defines relationship between Event and Param, handles analytic logging in background thread
@Keep
public abstract class FA {
    public abstract static class UserProp {
        public abstract class sign_in_method {
            public final static String Google = "Google";
            public final static String Facebook = "Facebook";
            public final static String Email = "Password";
            public final static String Unknown = "Unknown";
        }
        public abstract class social_rooster_sender {
            //Integer of average number of roosters sent, as shared pref
            public final static String shared_pref_sent_roosters = "shared_pref_sent_roosters";
        }
        public abstract class uses_explore {
            public final static String shared_pref_uses_explore = "shared_pref_uses_explore";

            public final static String completed_explore_playback = "completed_explore_playback";
            public final static String started_explore_playback = "started_explore_playback";
            public final static String no = "no";
        }
        public abstract class user_favourites {

        }
    }

    public abstract static class Event {
        public abstract class onboarding_intro_viewed {
        }
        public abstract class onboarding_number_provided {
        }
        public abstract class onboarding_first_entry {
        }
        public abstract class alarm_creation_begin {
        }
        public abstract class alarm_edit_begin {
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
        public abstract class channel_unique_play extends channel_selected {
            public abstract class Param {
                //String param
                public final static String channel_title = "channel_title";
            }
        }
        public abstract class channel_play extends channel_selected {
            public abstract class Param {
                //String param
                public final static String channel_title = "channel_title";
            }
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
            public abstract class Param {
                //Number param
                public final static String alarm_activation_total_roosters = "alarm_activation_total_roosters";
                //Number param
                public final static String alarm_activation_index = "alarm_activation_index";
                //Number param
                public final static String alarm_activation_cycle_count = "alarm_activation_cycle_count";
            }
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
        public abstract class social_rooster_received {
        }
        public abstract class invitation_to_join_rooster_sent {
            public abstract class Param {
                //String param
                public final static String download_link_share_medium = "download_link_share_medium";
            }
        }
        public abstract class active_day {
        }
        public abstract class channel_info_viewed extends channel_selected {
            public abstract class Param {
                //String param
                public final static String channel_title = "channel_title";
            }
        }
        public abstract class explore_channel_rooster_play extends channel_selected {
            public abstract class Param {
                //String param
                public final static String channel_title = "channel_title";
            }
        }
        public abstract class default_alarm_play {
            public abstract class Param {
                //Bool
                public final static String fatal_failure = "fatal_failure";
                //Bool
                public final static String attempt_to_play = "attempt_to_play";
            }
        }
        public abstract class action_url_click {
            public abstract class Param {
                //String param
                public final static String channel_title = "channel_title";
                public final static String action_url = "action_url";
            }
        }
        public abstract class roosters_channel_favourite extends channel_selected {
            public abstract class Param {
                //String param
                public final static String channel_title = "channel_title";
            }
        }
        public abstract class roosters_channel_share extends channel_selected {
        }
        public abstract class roosters_channel_play extends channel_selected {
        }
        public abstract class roosters_social_favourite {
        }
        public abstract class roosters_social_share {
        }
        public abstract class roosters_social_play {
        }
        public abstract class memory_warning {
            public abstract class Param {
                //Long
                public final static String memory_in_use = "memory_in_use";
            }
        }
    }

    public static void SetUserProp(final Class<?> UserProp, final String Prop) {
        new Thread() {
            @Override
            public void run() {

                String eventString;

                if (UserProp == null) {
                    throw new NullPointerException();
                } else {
                    eventString = UserProp.toString();
                    int eventStringPosition = eventString.lastIndexOf("$") + 1;
                    eventString = eventString.substring(eventStringPosition);
                }

                if (Prop != null) {
                    BaseApplication.Companion.getFirebaseAnalytics().setUserProperty(eventString, Prop);
                }
            }
        }.run();
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
                    BaseApplication.Companion.getFirebaseAnalytics().logEvent(eventString, null);
                } else if (entry instanceof String) {
                    bundle.putString(Param, (String) entry);
                    BaseApplication.Companion.getFirebaseAnalytics().logEvent(eventString, bundle);
                } else if (entry instanceof Integer) {
                    bundle.putInt(Param, (Integer) entry);
                    BaseApplication.Companion.getFirebaseAnalytics().logEvent(eventString, bundle);
                } else if (entry instanceof Boolean) {
                    bundle.putBoolean(Param, (Boolean) entry);
                    BaseApplication.Companion.getFirebaseAnalytics().logEvent(eventString, bundle);
                }
            }
        }.run();
    }
    
    public static void LogMany(final Class<?> Event, final String[] Params, final Object[] entries) {
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

                if(Params == null || entries == null) {
                    BaseApplication.Companion.getFirebaseAnalytics().logEvent(eventString, null);
                    return;
                }

                int index = -1;
                for (String Param:
                     Params) {
                    index++;
                    Object entry = entries[index];
                    if (Param != null && entry != null) {
                        if (entry instanceof String) {
                            bundle.putString(Param, (String) entry);
                        } else if (entry instanceof Integer) {
                            bundle.putInt(Param, (Integer) entry);
                        } else if (entry instanceof Boolean) {
                            bundle.putBoolean(Param, (Boolean) entry);
                        }
                    }
                }
                if(bundle.isEmpty()) bundle = null;
                BaseApplication.Companion.getFirebaseAnalytics().logEvent(eventString, bundle);
            }
        }.run();
    }
}
