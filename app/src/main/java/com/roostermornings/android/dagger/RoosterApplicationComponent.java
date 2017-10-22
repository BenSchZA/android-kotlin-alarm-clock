/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.dagger;

import android.content.SharedPreferences;

import javax.inject.Singleton;

import dagger.Component;

import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.activity.DeviceAlarmFullScreenActivity;
import com.roostermornings.android.activity.DiscoverFragmentActivity;
import com.roostermornings.android.activity.FriendsFragmentActivity;
import com.roostermornings.android.activity.IntroFragmentActivity;
import com.roostermornings.android.activity.MessageStatusFragmentActivity;
import com.roostermornings.android.activity.MyAlarmsFragmentActivity;
import com.roostermornings.android.activity.NewAlarmFragmentActivity;
import com.roostermornings.android.activity.NewAudioFriendsActivity;
import com.roostermornings.android.activity.NewAudioRecordActivity;
import com.roostermornings.android.activity.ProfileActivity;
import com.roostermornings.android.activity.SettingsActivity;
import com.roostermornings.android.activity.SignInActivity;
import com.roostermornings.android.activity.SignupEmailActivity;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.adapter.MyAlarmsListAdapter;
import com.roostermornings.android.adapter_data.RoosterAlarmManager;
import com.roostermornings.android.adapter_data.ChannelManager;
import com.roostermornings.android.firebase.FA;
import com.roostermornings.android.fragment.NumberEntryDialogFragment;
import com.roostermornings.android.fragment.base.BaseFragment;
import com.roostermornings.android.fragment.friends.FriendsInviteFragment3;
import com.roostermornings.android.fragment.friends.FriendsMyFragment1;
import com.roostermornings.android.fragment.friends.FriendsRequestFragment2;
import com.roostermornings.android.fragment.intro.IntroFragment1;
import com.roostermornings.android.fragment.intro.IntroFragment2;
import com.roostermornings.android.fragment.message_status.MessageStatusReceivedFragment1;
import com.roostermornings.android.fragment.message_status.MessageStatusSentFragment2;
import com.roostermornings.android.fragment.new_alarm.NewAlarmFragment1;
import com.roostermornings.android.fragment.new_alarm.NewAlarmFragment2;
import com.roostermornings.android.media.MediaNotificationHelper;
import com.roostermornings.android.receiver.BackgroundTaskReceiver;
import com.roostermornings.android.receiver.DeviceAlarmReceiver;
import com.roostermornings.android.receiver.NetworkChangeReceiver;
import com.roostermornings.android.service.AudioService;
import com.roostermornings.android.service.MediaService;
import com.roostermornings.android.service.RoosterFirebaseMessagingService;
import com.roostermornings.android.service.UploadService;
import com.roostermornings.android.sqlutil.DeviceAlarmController;
import com.roostermornings.android.sqlutil.DeviceAlarmTableManager;
import com.roostermornings.android.sync.DownloadSyncAdapter;
import com.roostermornings.android.geolocation.GeoHashUtils;
import com.roostermornings.android.util.JSONPersistence;
import com.roostermornings.android.util.LifeCycle;
import com.roostermornings.android.widgets.AlarmToggleWidgetDataProvider;

/**
 * Annotates an interface or abstract class for which a fully-formed, dependency-injected implementation
 * is to be generated from a set of modules(). The generated class will have the name of the type
 * annotated with @Component prepended with Dagger. For example, @Component interface MyComponent {...}
 * will produce an implementation named DaggerMyComponent.
 */
@Component(modules = RoosterApplicationModule.class)
@Singleton
public interface RoosterApplicationComponent {

    //Members-injection methods have a single parameter and inject dependencies into each of the
    //Inject-annotated fields and methods of the passed instance.
    //A members-injection method may be void or return its single parameter as a convenience for chaining.

    void inject(RoosterApplicationModule roosterApplicationModule);
    void inject(BaseApplication baseApplication);
    void inject(BaseActivity baseActivity);
    void inject(BaseFragment baseFragment);

    void inject(DeviceAlarmFullScreenActivity activity);
    void inject(FriendsFragmentActivity activity);
    void inject(IntroFragmentActivity activity);
    void inject(MessageStatusFragmentActivity activity);
    void inject(DiscoverFragmentActivity activity);
    void inject(MyAlarmsFragmentActivity activity);
    void inject(NewAlarmFragmentActivity activity);
    void inject(NewAudioFriendsActivity activity);
    void inject(NewAudioRecordActivity activity);
    void inject(ProfileActivity activity);
    void inject(SettingsActivity activity);
    void inject(SignInActivity activity);
    void inject(SignupEmailActivity activity);

    void inject(FriendsInviteFragment3 fragment);
    void inject(FriendsMyFragment1 fragment);
    void inject(FriendsRequestFragment2 fragment);
    void inject(IntroFragment1 fragment);
    void inject(IntroFragment2 fragment);
    void inject(NewAlarmFragment1 fragment);
    void inject(NewAlarmFragment2 fragment);
    void inject(NumberEntryDialogFragment fragment);
    void inject(MessageStatusReceivedFragment1 fragment);
    void inject(MessageStatusSentFragment2 fragment);

    void inject(AudioService service);
    void inject(UploadService service);
    void inject(DeviceAlarmReceiver service);
    void inject(BackgroundTaskReceiver service);
    void inject(NetworkChangeReceiver service);
    void inject(RoosterFirebaseMessagingService service);

    void inject(FA roosterClass);
    void inject(DeviceAlarmController roosterClass);
    void inject(DeviceAlarmTableManager roosterClass);
    void inject(LifeCycle roosterClass);
    void inject(GeoHashUtils roosterClass);
    void inject(JSONPersistence jsonPersistence);
    void inject(ChannelManager roosterClass);
    void inject(MediaNotificationHelper roosterClass);
    void inject(MediaService roosterClass);
    void inject(RoosterAlarmManager roosterClass);
    void inject(AlarmToggleWidgetDataProvider roosterClass);

    void inject(DownloadSyncAdapter adapter);
    void inject(MyAlarmsListAdapter adapter);

    //Provision methods have no parameters and return an injected or provided type.
    //Each method may have a Qualifier annotation as well.

    SharedPreferences sharedPreferences();
}
