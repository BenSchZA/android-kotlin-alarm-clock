package com.roostermornings.android.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SignInActivity extends BaseActivity {

    String mMobileNumber = "";

    @BindView(R.id.already_user_textview)
    TextView mTextViewAlreadyUser;

    @BindView(R.id.signin_button_email)
    Button mButtonSigninEmail;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(R.layout.activity_sign_in);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            mMobileNumber = bundle.getString(getApplicationContext().getString(R.string.extras_mobile_number));
        }

    }

    @OnClick(R.id.already_user_textview)
    public void onAlreadyUserClicked() {

        Intent intent = new Intent(SignInActivity.this, SignupEmailActivity.class);
        intent.putExtra(getApplicationContext().getString(R.string.extras_mobile_number), mMobileNumber);
        intent.putExtra(getApplicationContext().getString(R.string.extras_already_user), true);
        startActivity(intent);

    }

    @OnClick(R.id.signin_button_email)
    public void onEmailSigninButtonClick() {

        Intent intent = new Intent(SignInActivity.this, SignupEmailActivity.class);
        intent.putExtra(getApplicationContext().getString(R.string.extras_mobile_number), mMobileNumber);
        startActivity(intent);


    }


}
