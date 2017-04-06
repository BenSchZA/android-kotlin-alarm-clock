/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.receiver.DeviceAlarmReceiver;
import com.roostermornings.android.service.AudioService;
import com.roostermornings.android.sqlutil.DeviceAudioQueueItem;
import com.roostermornings.android.sqlutil.DeviceAlarmController;
import com.roostermornings.android.util.Constants;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import butterknife.BindView;
import butterknife.OnClick;

public class DeviceAlarmFullScreenActivity extends BaseActivity {

    DeviceAlarmController deviceAlarmController;

    public static final String TAG = DeviceAlarmFullScreenActivity.class.getSimpleName();

    AudioService mAudioService;
    private boolean mBound;
    private BroadcastReceiver receiver;
    DeviceAudioQueueItem audioItem;

    private int alarmCount;
    private int alarmPosition;

    private String alarmUid;

    @BindView(R.id.alarm_sender_pic)
    ImageView imgSenderPic;

    @BindView(R.id.alarm_sender_name)
    TextView txtSenderName;

    @BindView(R.id.alarm_count)
    TextView txtAlarmCount;

    @BindView(R.id.alarm_snooze_button)
    Button mButtonAlarmSnooze;

    @BindView(R.id.alarm_dismiss)
    TextView mButtonAlarmDismiss;

    @BindView(R.id.skip_next)
    Button skipNext;

    @BindView(R.id.skip_previous)
    Button skipPrevious;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(R.layout.activity_device_alarm_full_screen);
        //Used to ensure alarm shows over lock-screen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                +WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                +WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                +WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setDayNight();

        //Get alarm UID for relating current alarm to channel content
        alarmUid = getIntent().getStringExtra(Constants.EXTRA_UID);

        //Bind to audio service to allow playback and pausing of alarms in background
        Intent intent = new Intent(this, AudioService.class);
        startService(intent);
        //0 indicates that service should not be restarted
        bindService(intent, mAudioServiceConnection, 0);
        //Attach broadcast receiver which updates alarm display UI using serializable extra
        attachAudioServiceBroadCastReceiver();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mAudioService.processListenedChannels();
        if(mBound) unbindService(mAudioServiceConnection);
        unregisterReceiver(receiver);
    }

    @Override
    public void onBackPressed() {
        deviceAlarmController.snoozeAlarm(alarmUid);
        mAudioService.snoozeAudioState();
        if(mBound) unbindService(mAudioServiceConnection);
        mBound = false;
        finish();
    }

    @OnClick(R.id.alarm_snooze_button)
    protected void onAlarmSnoozeButtonClicked() {
        deviceAlarmController.snoozeAlarm(alarmUid);
        mAudioService.snoozeAudioState();
        if(mBound) unbindService(mAudioServiceConnection);
        mBound = false;
        finish();
    }

    @OnClick(R.id.alarm_dismiss)
    protected void onAlarmDismissButtonClicked() {
        mAudioService.endService(mAudioServiceConnection);
        if(mBound) unbindService(mAudioServiceConnection);
        mBound = false;
        finish();
    }

    @OnClick(R.id.skip_previous)
    protected void onSkipPreviousButtonClicked() {
        mAudioService.skipPrevious();
    }

    @OnClick(R.id.skip_next)
    protected void onSkipNextButtonClicked() {
        mAudioService.skipNext();
    }

    private ServiceConnection mAudioServiceConnection = new ServiceConnection() {
        // Called when the connection with the service is established
        public void onServiceConnected(ComponentName className, IBinder service) {
            // Because we have bound to an explicit
            // service that is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            AudioService.LocalBinder binder = (AudioService.LocalBinder) service;
            mAudioService = binder.getService();
            mBound = true;

            deviceAlarmController = new DeviceAlarmController(getApplicationContext());

            //TODO: This intent extra is not currently implemented
            if (getIntent().getBooleanExtra(Constants.EXTRA_TONE, false)) {
                mAudioService.startDefaultAlarmTone();
                //Replace image and name with message if no Roosters etc.
                setDefaultDisplayProfile();
            } else {
                mAudioService.startAlarmContent(alarmUid);
            }
        }

        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            Log.e(TAG, "onServiceDisconnected");
            mBound = false;
        }
    };

    private void attachAudioServiceBroadCastReceiver() {
        //Flag check for UI changes on load, broadcast receiver for changes while activity running
        //Broadcast receiver filter to receive UI updates
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_ALARMDISPLAY);
        intentFilter.addAction(Constants.ACTION_ALARMTIMESUP);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //do something based on the intent's action
                switch(intent.getAction()){
                    case Constants.ACTION_ALARMDISPLAY:
                        audioItem = (DeviceAudioQueueItem) intent.getExtras().getSerializable("audioItem");
                        alarmPosition = intent.getIntExtra("alarmPosition", alarmPosition);
                        alarmCount = intent.getIntExtra("alarmCount", alarmCount);
                        setAlarmUI();
                        break;
                    case Constants.ACTION_ALARMTIMESUP:
                        Intent wakefulIntent = getIntent().getParcelableExtra(Constants.DEVICE_ALARM_RECEIVER_WAKEFUL_INTENT);
                        DeviceAlarmReceiver.completeWakefulIntent(wakefulIntent);

                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                                +WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                                +WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

                        mAudioService.endService(mAudioServiceConnection);
                        break;
                    default:
                        break;
                }
            }
        };
        registerReceiver(receiver, intentFilter);
    }

    protected void setAlarmUI() {

        if (audioItem.getPicture() != null && audioItem.getPicture().length() != 0) {
            setProfilePic(audioItem.getPicture());
        } else {
            imgSenderPic.setBackground(getResources().getDrawable(R.drawable.alarm_profile_pic_circle));
        }
        txtSenderName.setText(audioItem.getName());
        txtAlarmCount.setText(String.format("%s of %s", alarmPosition, alarmCount));
    }

    protected void setProfilePic(String url) {

        Picasso.with(DeviceAlarmFullScreenActivity.this).load(url)
                .resize(400, 400)
                .into(imgSenderPic, new Callback() {
                    @Override
                    public void onSuccess() {
                        Bitmap imageBitmap = ((BitmapDrawable) imgSenderPic.getDrawable()).getBitmap();
                        RoundedBitmapDrawable imageDrawable = RoundedBitmapDrawableFactory.create(getResources(), imageBitmap);
                        imageDrawable.setCircular(true);
                        imageDrawable.setCornerRadius(Math.max(imageBitmap.getWidth(), imageBitmap.getHeight()) / 2.0f);
                        imgSenderPic.setImageDrawable(imageDrawable);
                    }

                    @Override
                    public void onError() {
                        setDefaultDisplayProfile();
                    }
                });
    }

    protected void setDefaultDisplayProfile() {
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.logo);
        Drawable d = new BitmapDrawable(getResources(), bm);
        txtSenderName.setText(R.string.alarm_default_name);
        imgSenderPic.setBackground(d);
    }
}
