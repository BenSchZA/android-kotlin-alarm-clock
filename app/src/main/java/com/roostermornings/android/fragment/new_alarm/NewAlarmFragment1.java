/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.fragment.new_alarm;

import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.roostermornings.android.BuildConfig;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.NewAlarmFragmentActivity;
import com.roostermornings.android.adapter.MyAlarmsListAdapter;
import com.roostermornings.android.domain.Alarm;
import com.roostermornings.android.fragment.IAlarmSetListener;
import com.roostermornings.android.fragment.base.BaseFragment;
import com.roostermornings.android.sqlutil.DeviceAlarmController;
import com.roostermornings.android.sqlutil.DeviceAlarmTableManager;
import com.roostermornings.android.util.RoosterUtils;

import java.util.Calendar;

import butterknife.BindView;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;


public class NewAlarmFragment1 extends BaseFragment{

    @BindView(R.id.new_alarm_time)
    TextView textViewAlarmTime;

    @BindView(R.id.days_parent1)
    LinearLayout daysParentLinearLayout1;

    @BindView(R.id.days_parent2)
    LinearLayout daysParentLinearLayout2;

    @BindView(R.id.new_alarm_fragment1_alarm_day_mon)
    TextView textViewMonday;

    @BindView(R.id.new_alarm_fragment1_alarm_day_tue)
    TextView textViewTuesday;

    @BindView(R.id.new_alarm_fragment1_alarm_day_wed)
    TextView textViewWednesday;

    @BindView(R.id.new_alarm_fragment1_alarm_day_thu)
    TextView textViewThursday;

    @BindView(R.id.new_alarm_fragment1_alarm_day_fri)
    TextView textViewFriday;

    @BindView(R.id.new_alarm_fragment1_alarm_day_sat)
    TextView textViewSaturday;

    @BindView(R.id.new_alarm_fragment1_alarm_day_sun)
    TextView textViewSunday;

    @BindView(R.id.new_alarm_fragment1_switch_audio)
    SwitchCompat switchAudio;

    @BindView(R.id.new_alarm_fragment1_switch_recurring)
    SwitchCompat switchRecurring;

    @BindView(R.id.new_alarm_fragment1_delete_alarm)
    Button deleteAlarm;


    private static final String ARG_USER_UID_PARAM = "user_uid_param";
    public static final String TAG = NewAlarmFragment1.class.getSimpleName();
    private String mUserUidParam;
    private IAlarmSetListener mListener;
    Alarm mAlarm = new Alarm();
    DeviceAlarmController deviceAlarmController;
    DeviceAlarmTableManager deviceAlarmTableManager;

    Calendar cal = Calendar.getInstance();

    int hour = cal.get(Calendar.HOUR_OF_DAY);
    int minute = cal.get(Calendar.MINUTE);
    TimePickerDialog mTimePickerDialog;


    public NewAlarmFragment1() {
        // Required empty public constructor
    }

    public static NewAlarmFragment1 newInstance(String param1) {
        NewAlarmFragment1 fragment = new NewAlarmFragment1();
        Bundle args = new Bundle();
        args.putString(ARG_USER_UID_PARAM, param1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mUserUidParam = getArguments().getString(ARG_USER_UID_PARAM);
        }

        deviceAlarmController = new DeviceAlarmController(getContext());
        deviceAlarmTableManager = new DeviceAlarmTableManager(getContext());

        mAlarm = mListener.getAlarmDetails();
        //Set current time
        mAlarm.setHour(hour);
        mAlarm.setMinute(minute);

        mTimePickerDialog = new TimePickerDialog(getContext(), new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                setAlarmTime(hourOfDay, minute);
            }
        }, hour, minute, true); //24h time
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = initiate(inflater, R.layout.fragment_new_alarm_step1, container, false);

        //If in edit mode, delete button should be visible and alarm details should be updated
        updateAlarmUIIfEdit();
        textViewAlarmTime.setText(RoosterUtils.setAlarmTimeFromHourAndMinute(mAlarm));
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof IAlarmSetListener) {
            mListener = (IAlarmSetListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement IAlarmSetListener");
        }

    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void setMenuVisibility(final boolean visible) {
        super.setMenuVisibility(visible);
        if (visible) {
            if (mListener != null) mListener.setNextButtonCaption(getString(R.string.next));
        }
    }

    protected void setAlarmTime(int hour, int minute) {
        this.hour = hour;
        this.minute = minute;
        mAlarm.setHour(hour);
        mAlarm.setMinute(minute);
        textViewAlarmTime.setText(RoosterUtils.setAlarmTimeFromHourAndMinute(mAlarm));
    }

    @OnClick(R.id.new_alarm_time)
    public void onTimeClick() {

        openTimePicker();

    }

    public void updateAlarmUIIfEdit() {
        if(deviceAlarmTableManager.isSetInDB(NewAlarmFragmentActivity.getCurrentAlarmId())) {
            mListener.retrieveAlarmDetailsFromSQL();
            deleteAlarm.setVisibility(View.VISIBLE);
        }
    }

    @OnClick(R.id.new_alarm_fragment1_delete_alarm)
    public void deleteAlarm() {
        try {
            DeviceAlarmController deviceAlarmController = new DeviceAlarmController(getContext());

            //Remove alarm *set* from local SQL database using retrieved Uid from firebase && Remove alarm from firebase
            deviceAlarmController.deleteAlarmSetGlobal(mAlarm.getUid());

            MyAlarmsListAdapter myAlarmsListAdapter = new MyAlarmsListAdapter();
            myAlarmsListAdapter.notifyDataSetChanged();

            startHomeActivity();
            Toast.makeText(getContext(), "Alarm deleted.", Toast.LENGTH_SHORT);
        } catch (NullPointerException e) {
            e.printStackTrace();
            if(BuildConfig.DEBUG) Toast.makeText(getContext(), "Oi! Don't delete me. Delete alarm failed!", Toast.LENGTH_SHORT);
        }
    }

    @OnClick({R.id.new_alarm_fragment1_alarm_day_mon,
            R.id.new_alarm_fragment1_alarm_day_tue,
            R.id.new_alarm_fragment1_alarm_day_wed,
            R.id.new_alarm_fragment1_alarm_day_thu,
            R.id.new_alarm_fragment1_alarm_day_fri,
            R.id.new_alarm_fragment1_alarm_day_sat,
            R.id.new_alarm_fragment1_alarm_day_sun})
    protected void selectWeekDay(TextView selectedDay) {
        int day = Integer.parseInt((String) selectedDay.getTag());
        boolean selected = false;
        switch (day) {
            case 1:
                selected = mAlarm.isMonday();
                mAlarm.setMonday(!selected);
                break;
            case 2:
                selected = mAlarm.isTuesday();
                mAlarm.setTuesday(!selected);
                break;
            case 3:
                selected = mAlarm.isWednesday();
                mAlarm.setWednesday(!selected);
                break;
            case 4:
                selected = mAlarm.isThursday();
                mAlarm.setThursday(!selected);
                break;
            case 5:
                selected = mAlarm.isFriday();
                mAlarm.setFriday(!selected);
                break;
            case 6:
                selected = mAlarm.isSaturday();
                mAlarm.setSaturday(!selected);
                break;
            case 7:
                selected = mAlarm.isSunday();
                mAlarm.setSunday(!selected);
                break;
        }
        selectedDay.setSelected(!selected);
        mListener.setAlarmDetails(mAlarm);
    }

    @OnCheckedChanged(R.id.new_alarm_fragment1_switch_audio)
    public void audioSwitchOnCheckedChanged(boolean isChecked) {

        if (isChecked) {
            mAlarm.setAllow_friend_audio_files(true);
        } else {
            mAlarm.setAllow_friend_audio_files(false);
        }
        mListener.setAlarmDetails(mAlarm);
    }

    @OnCheckedChanged(R.id.new_alarm_fragment1_switch_recurring)
    public void recurringSwitchOnCheckedChanged(boolean isChecked) {

        if (isChecked) {
            mAlarm.setRecurring(true);
        } else {
            mAlarm.setRecurring(false);
        }
        mListener.setAlarmDetails(mAlarm);
    }

    private void openTimePicker() {

        mTimePickerDialog.setTitle(getString(R.string.new_alarm_set_time));
        mTimePickerDialog.show();

    }


    public void setEditedAlarmSettings() {

        mAlarm = mListener.getAlarmDetails();

        textViewMonday.setBackgroundResource(R.drawable.selectable_circle_background);
        textViewTuesday.setBackgroundResource(R.drawable.selectable_circle_background);
        textViewWednesday.setBackgroundResource(R.drawable.selectable_circle_background);
        textViewThursday.setBackgroundResource(R.drawable.selectable_circle_background);
        textViewFriday.setBackgroundResource(R.drawable.selectable_circle_background);
        textViewSaturday.setBackgroundResource(R.drawable.selectable_circle_background);
        textViewSunday.setBackgroundResource(R.drawable.selectable_circle_background);

        if (mAlarm.isMonday()) {
            textViewMonday.setSelected(true);
        }
        if (mAlarm.isTuesday()) {
            textViewTuesday.setSelected(true);
        }
        if (mAlarm.isWednesday()) {
            textViewWednesday.setSelected(true);
        }
        if (mAlarm.isThursday()) {
            textViewThursday.setSelected(true);
        }
        if (mAlarm.isFriday()) {
            textViewFriday.setSelected(true);
        }
        if (mAlarm.isSaturday()) {
            textViewSaturday.setSelected(true);
        }
        if (mAlarm.isSunday()) {
            textViewSunday.setSelected(true);
        }

        this.hour = mAlarm.getHour();
        this.minute = mAlarm.getMinute();
        textViewAlarmTime.setText(RoosterUtils.setAlarmTimeFromHourAndMinute(mAlarm));
        mTimePickerDialog.updateTime(this.hour, this.minute);

        switchAudio.setChecked(mAlarm.isAllow_friend_audio_files());
        switchRecurring.setChecked(mAlarm.isRecurring());
    }

}
