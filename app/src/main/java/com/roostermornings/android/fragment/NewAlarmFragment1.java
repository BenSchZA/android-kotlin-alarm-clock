package com.roostermornings.android.fragment;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;

import com.roostermornings.android.R;
import com.roostermornings.android.domain.Alarm;
import com.roostermornings.android.domain.AlarmChannel;
import com.roostermornings.android.domain.User;
import com.roostermornings.android.fragment.base.BaseFragment;
import com.roostermornings.android.util.RoosterUtils;

import java.util.Calendar;

import butterknife.BindView;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;


public class NewAlarmFragment1 extends BaseFragment {

    @BindView(R.id.new_alarm_time)
    TextView textViewAlarmTime;

    @BindView(R.id.days_parent)
    LinearLayout daysParentLinearLayout;

    private static final String ARG_USER_UID_PARAM = "user_uid_param";
    public static final String TAG = NewAlarmFragment1.class.getSimpleName();
    private String mUserUidParam;
    private IAlarmSetListener mListener;
    com.roostermornings.android.domain.Alarm mAlarm = new Alarm();



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

        mAlarm = mListener.getAlarmDetails();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = initiate(inflater, R.layout.fragment_new_alarm_fragment1, container, false);
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

    @OnClick(R.id.new_alarm_time)
    public void onTimeClick() {

        //DialogFragment newFragment = new TimePickerFragment(mAlarm);
        //newFragment.show(getFragmentManager(), "timePicker");

    }

    @OnClick({R.id.new_alarm_fragment1_alarm_day_mon,
            R.id.new_alarm_fragment1_alarm_day_tue,
            R.id.new_alarm_fragment1_alarm_day_wed,
            R.id.new_alarm_fragment1_alarm_day_thu,
            R.id.new_alarm_fragment1_alarm_day_fri,
            R.id.new_alarm_fragment1_alarm_day_sat,
            R.id.new_alarm_fragment1_alarm_day_sun})
    protected void selectEmbarkingNumber1(TextView selectedDay) {
        int day = Integer.parseInt((String) selectedDay.getTag());
        boolean selected = false;
        switch (day) {
            case 1:
                selected = mAlarm.isMonday();
                break;
            case 2:
                selected = mAlarm.isTuesday();
                break;
            case 3:
                selected = mAlarm.isWednesday();
                break;
            case 4:
                selected = mAlarm.isThursday();
                break;
            case 5:
                selected = mAlarm.isFriday();
                break;
            case 6:
                selected = mAlarm.isSaturday();
                break;
            case 7:
                selected = mAlarm.isSunday();
                break;
        }
        if (selected) selectedDay.setBackgroundResource(R.drawable.selectable_circle_background);
        else selectedDay.setBackgroundResource(R.drawable.selected_circle_background);
        selected = !selected;
        switch (day) {
            case 1:
                mAlarm.setMonday(selected);
                break;
            case 2:
                mAlarm.setTuesday(selected);
                break;
            case 3:
                mAlarm.setWednesday(selected);
                break;
            case 4:
                mAlarm.setThursday(selected);
                break;
            case 5:
                mAlarm.setFriday(selected);
                break;
            case 6:
                mAlarm.setSaturday(selected);
                break;
            case 7:
                mAlarm.setSunday(selected);
                break;
        }
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
            daysParentLinearLayout.setVisibility(View.VISIBLE);
        } else {
            mAlarm.setRecurring(false);
            daysParentLinearLayout.setVisibility(View.INVISIBLE);
        }
        mListener.setAlarmDetails(mAlarm);
    }

}
