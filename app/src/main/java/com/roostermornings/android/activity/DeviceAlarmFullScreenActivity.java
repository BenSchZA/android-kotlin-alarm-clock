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
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.dagger.RoosterApplicationComponent;
import com.roostermornings.android.firebase.FA;
import com.roostermornings.android.service.AudioService;
import com.roostermornings.android.sqlutil.DeviceAlarmController;
import com.roostermornings.android.sqlutil.DeviceAudioQueueItem;
import com.roostermornings.android.util.Constants;
import com.roostermornings.android.util.StrUtils;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;

public class DeviceAlarmFullScreenActivity extends BaseActivity {

    public static final String TAG = DeviceAlarmFullScreenActivity.class.getSimpleName();

    AudioService mAudioService;
    private boolean mBound = false;
    private BroadcastReceiver receiver;
    DeviceAudioQueueItem audioItem = new DeviceAudioQueueItem();

    private int alarmCount = 1;
    private int alarmPosition = 1;

    boolean actionUrlClicked;

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

    @BindView(R.id.alarm_action_button)
    Button alarmActionButton;

    @Inject DeviceAlarmController deviceAlarmController;

    @Override
    protected void inject(RoosterApplicationComponent component) {
        component.inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inject(BaseApplication.getRoosterApplicationComponent());

        //Used to ensure alarm shows over lock-screen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                +WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                +WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        //This method hides the status bar, in reality this isn't very practical, mainly visual, so removed
//        if(RoosterUtils.hasJellyBean()) {
//            View decorView = getWindow().getDecorView();
//            // Hide the status bar.
//            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
//            decorView.setSystemUiVisibility(uiOptions);
//            // Remember that you should never show the action bar if the
//            // status bar is hidden, so hide that too if necessary.
//            ActionBar actionBar = getActionBar();
//            if(actionBar != null) actionBar.hide();
//        } else {
//            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        }

        initialize(R.layout.activity_device_alarm_full_screen);

        setDayNightTheme();

        //Bind to audio service to allow playback and pausing of alarms in background
        Intent intent = new Intent(this, AudioService.class);
        //0 indicates that service should not be restarted
        bindService(intent, mAudioServiceConnection, 0);

        //Set volume rocker to alarm stream
        setVolumeControlStream(AudioManager.STREAM_ALARM);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mBound) unbindService(mAudioServiceConnection);
        try {
            if (receiver != null)
                unregisterReceiver(receiver);
        } catch(IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onUserLeaveHint()
    {
        //Detect when user elects to leave activity (home button, etc.)
        super.onUserLeaveHint();

        //Check if event as a result of action url click
        if(!actionUrlClicked) {
            if (mAudioService != null) mAudioService.snoozeAudioState();
            if (mBound) unbindService(mAudioServiceConnection);
            mBound = false;
            finish();
            startNextActivity();
        }
        //Clear flag
        actionUrlClicked = false;
    }

    @Override
    public void onBackPressed() {
        if(mAudioService!=null) mAudioService.snoozeAudioState();
        if(mBound) unbindService(mAudioServiceConnection);
        mBound = false;
        finish();
        startNextActivity();
    }

    @OnClick(R.id.alarm_snooze_button)
    protected void onAlarmSnoozeButtonClicked() {
        if(mAudioService!=null) mAudioService.snoozeAudioState();
        if(mBound) unbindService(mAudioServiceConnection);
        mBound = false;
        finish();
        startNextActivity();
    }

    @OnClick(R.id.alarm_dismiss)
    protected void onAlarmDismissButtonClicked() {
        if(mAudioService!=null) mAudioService.dismissAlarm();
        if(mBound) unbindService(mAudioServiceConnection);
        mBound = false;
        finish();
        startNextActivity();
    }

    private void startNextActivity() {
        startActivity(new Intent(this, MessageStatusFragmentActivity.class));
    }

    @OnClick(R.id.alarm_action_button)
    public void onActionButtonClicked() {
        actionUrlClicked = true;
        if(StrUtils.notNullOrEmpty(audioItem.getAction_url())) {
            try {
                Uri uri = Uri.parse(audioItem.getAction_url());
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
                FA.LogMany(FA.Event.action_url_click.class, new String[]{FA.Event.action_url_click.Param.channel_title, FA.Event.action_url_click.Param.action_url}, new Object[]{audioItem.getName(), audioItem.getAction_url()});
            } catch (Exception e) {
                e.printStackTrace();
                alarmActionButton.setVisibility(View.GONE);
                actionUrlClicked = false;
            }
        }
    }

    @OnClick(R.id.skip_previous)
    protected void onSkipPreviousButtonClicked() {
        if(mAudioService!=null) mAudioService.skipPrevious();
    }

    @OnClick(R.id.skip_next)
    protected void onSkipNextButtonClicked() {
        if(mAudioService!=null) mAudioService.skipNext();
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

            //Attach broadcast receiver which updates alarm display UI using serializable extra
            attachAudioServiceBroadCastReceiver();

            //Replace image and name with message if no Roosters etc.
            setDefaultDisplayProfile(true);
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
                try {
                    switch(intent.getAction()){
                        case Constants.ACTION_ALARMDISPLAY:
                            //Update alarm UI based on audio service broadcast
                            audioItem = (DeviceAudioQueueItem) intent.getExtras().getSerializable("audioItem");
                            alarmPosition = intent.getIntExtra("alarmPosition", alarmPosition);
                            alarmCount = intent.getIntExtra("alarmCount", alarmCount);
//                            if (intent.getBooleanExtra("multipleAudioFiles", false)) {
//                                skipNext.setVisibility(View.VISIBLE);
//                                skipPrevious.setVisibility(View.VISIBLE);
//                            } else {
//                                skipNext.setVisibility(View.INVISIBLE);
//                                skipPrevious.setVisibility(View.INVISIBLE);
//                            }
                            setAlarmUI();
                            break;
                        case Constants.ACTION_ALARMTIMESUP:
                            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                                    + WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                                    +WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
                            finish();
                            break;
                        default:
                            break;
                    }
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
        };
        registerReceiver(receiver, intentFilter);
        //Once receiver is registered, try update UI from service
        mAudioService.updateAlarmUI(false);
    }

    protected void setAlarmUI() {
        if(audioItem != null) {
            if(StrUtils.notNullOrEmpty(audioItem.getAction_title()) && StrUtils.notNullOrEmpty(audioItem.getAction_url())) {
                alarmActionButton.setText(audioItem.getAction_title());
                alarmActionButton.setVisibility(View.VISIBLE);
            }
        }

        if (audioItem != null && StrUtils.notNullOrEmpty(audioItem.getPicture()) && StrUtils.notNullOrEmpty(audioItem.getName())) {
            //Both image and title are valid
            setProfilePic(audioItem.getPicture());
            txtSenderName.setText(audioItem.getName());
        } else if(audioItem != null && StrUtils.notNullOrEmpty(audioItem.getName())) {
            //Set default image, keep title
            setDefaultDisplayProfile(false);
            txtSenderName.setText(audioItem.getName());
        } else {
            //Set default image and title
            setDefaultDisplayProfile(true);
        }
        txtAlarmCount.setText(String.format("%s of %s", alarmPosition, alarmCount));
    }

    protected void setProfilePic(String url) {
        //Reset scale type
        imgSenderPic.setScaleType(ImageView.ScaleType.FIT_CENTER);

        Picasso.with(DeviceAlarmFullScreenActivity.this).load(url)
                .resize(600, 600)
                .centerCrop()
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
                        setDefaultDisplayProfile(false);
                    }
                });
    }

    protected void setDefaultDisplayProfile(boolean overwriteTitle) {
        //Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.logo_icon);
        //Drawable d = new BitmapDrawable(getResources(), bm);
        //Set max width to 600 pixels, as with channel image
        imgSenderPic.setMaxWidth(600);
        //Set center inside to ensure logo scaled to fit inside 600 px max width
        imgSenderPic.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        //Set logo as default image
        imgSenderPic.setImageResource(R.drawable.logo_icon);
        //Clear background (grey circle)
        imgSenderPic.setBackground(null);
        //Only overwrite title if it is not available
        if(overwriteTitle) {
            txtSenderName.setText(R.string.alarm_default_name);
        }
    }
}
