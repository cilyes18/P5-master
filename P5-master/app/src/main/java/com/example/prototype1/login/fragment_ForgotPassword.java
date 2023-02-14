package com.example.prototype1.login;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.prototype1.R;
import com.example.prototype1.auth.AuthSingleton;
import com.example.prototype1.utils.Hashing;
import com.example.prototype1.utils.Traveler;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

public class fragment_ForgotPassword extends Fragment implements View.OnClickListener {
    private static final String TAG = "fragment_ForgotPassword";
    private Button sendConfButton, button_back;
    private EditText eText_email;
    private CheckBox cBox;
    private TextView page_title, tView_privacy_pol, we_send;
    private FragmentManager fragmentManager;
    private RelativeLayout layout_login;

    //context
    private Context mContext;

    //authentication:
    Hashing hash = new Hashing();

    //google-auth
    private FirebaseAuth mAuth;
    private FirebaseApp mApp;
    private AuthSingleton mAuthSingleton;

    //var
    private Traveler mTraveler;


    public fragment_ForgotPassword() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View v = inflater.inflate(R.layout.fragment__forgot_password, container, false);
        assert getActivity() != null;
        mContext = getActivity();
        mApp = FirebaseApp.initializeApp(mContext);
        mApp = FirebaseApp.getInstance();
        mAuthSingleton = AuthSingleton.getInst(mContext);

        mAuth = mAuthSingleton.mAuth();
        mTraveler = new Traveler();

        initView(v);
        buttonListeners();
        return v;
    }

    private void buttonListeners() {
        button_back.setOnClickListener(this);
        sendConfButton.setOnClickListener(this);
    }

    private void initView(View v) {

        button_back = v.findViewById(R.id.backToLogin_button);
        sendConfButton = v.findViewById(R.id.sendConfButton);
        eText_email = v.findViewById(R.id.email_editText);
        we_send = v.findViewById(R.id.we_send_hint);
        page_title = v.findViewById(R.id.page_title);
    }

    private void sendPassResetMail() {
        String email = eText_email.getText().toString();
        if (!TextUtils.isEmpty(email)) {
            mAuthSingleton.sendPasswordResetEmail(email);
        } else {
            eText_email.setError("Required.");
            eText_email.requestFocus();
            Toast.makeText(getContext(), "Please type a valid email", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.backToLogin_button:
                mTraveler.goToWithFlags(mContext, LoginActivity.class);
                break;

            case R.id.sendConfButton:
                Log.d(TAG, "onClick: successful: ");
                sendPassResetMail();
                break;
        }
    }

}