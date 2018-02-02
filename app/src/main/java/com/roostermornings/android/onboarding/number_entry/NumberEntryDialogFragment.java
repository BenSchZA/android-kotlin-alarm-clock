/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.onboarding.number_entry;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.mobsandgeeks.saripaar.ValidationError;
import com.mobsandgeeks.saripaar.Validator;
import com.mobsandgeeks.saripaar.annotation.NotEmpty;
import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.R;
import com.roostermornings.android.dagger.RoosterApplicationComponent;
import com.roostermornings.android.firebase.FirebaseNetwork;
import com.roostermornings.android.keys.PrefsKey;
import com.roostermornings.android.util.Constants;
import com.roostermornings.android.util.Toaster;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class NumberEntryDialogFragment extends DialogFragment implements Validator.ValidationListener {

    @Inject
    SharedPreferences sharedPreferences;
    @Inject
    BaseApplication AppContext;

    @BindView(R.id.button_okay)
    Button buttonOkay;

    @BindView(R.id.button_later)
    Button buttonLater;

    boolean popup;

    @BindView(R.id.mobile_number_edit_text)
    @NotEmpty
    EditText mobileNumber;

    Validator validator;

    NumberEntryListener mListener;

    public static NumberEntryDialogFragment newInstance(boolean popup) {
        NumberEntryDialogFragment numberEntryDialogFragment = new NumberEntryDialogFragment();

        Bundle args = new Bundle();
        args.putBoolean(Constants.ARG_SHOW_DISMISS, popup);
        numberEntryDialogFragment.setArguments(args);

        return numberEntryDialogFragment;
    }

    protected void inject(RoosterApplicationComponent component) {
        component.inject(this);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof NumberEntryListener) {
            mListener = (NumberEntryListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inject(BaseApplication.Companion.getRoosterApplicationComponent());

        if(getArguments() != null) {
            popup = getArguments().getBoolean(Constants.ARG_SHOW_DISMISS, false);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.number_entry_dialog, container, false);
        ButterKnife.bind(this, view);

        if(popup) buttonLater.setVisibility(View.VISIBLE);

        // Instantiate Saripaar validator to validate fields with NotEmpty annotations
        validator = new Validator(this);
        validator.setValidationListener(this);

        return view;
    }

    @Override
    public void onStart()
    {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null)
        {
            Window window = dialog.getWindow();
            if(window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                int width = ViewGroup.LayoutParams.MATCH_PARENT;
                int height = ViewGroup.LayoutParams.MATCH_PARENT;
                window.setLayout(width, height);
            }
        }
    }

    @Override
    public void onValidationSucceeded() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PrefsKey.MOBILE_NUMBER_VALIDATED.name(), true);
        //If it was a popup, and number was validated, don't show again when invalidated (e.g. through profile)
        if(popup) editor.putBoolean(PrefsKey.MOBILE_NUMBER_ENTRY_DISMISSED.name(), true);
        editor.apply();

        String mobileNumberString = mobileNumber.getText().toString().trim();
        FirebaseNetwork.INSTANCE.updateProfileCellNumber(getContext(), mobileNumber.getText().toString().trim());

        mListener.onMobileNumberValidated(mobileNumberString);
        dismiss();
    }

    @Override
    public void onValidationFailed(List<ValidationError> errors) {
        for (ValidationError error : errors) {
            View view = error.getView();
            String message = error.getCollatedErrorMessage(getActivity());

            if (view instanceof TextInputEditText) {

                TextInputLayout parent = (TextInputLayout) view.getParent();
                parent.setError(message);

            } else {
                Toaster.makeToast(getActivity(), message, Toast.LENGTH_LONG);
            }
        }
    }

    @OnClick(R.id.button_okay)
    public void onOkayClick() {
        validator.validate();
    }

    @OnClick(R.id.button_later)
    public void onLaterClick() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PrefsKey.MOBILE_NUMBER_ENTRY_DISMISSED.name(), true);
        editor.apply();

        dismiss();
    }
}
