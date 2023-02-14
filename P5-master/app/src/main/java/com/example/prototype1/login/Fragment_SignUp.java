package com.example.prototype1.login;

import android.content.Context;
import android.os.Bundle;
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

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.prototype1.R;
import com.example.prototype1.auth.AuthSingleton;
import com.example.prototype1.utils.Hashing;
import com.example.prototype1.utils.Traveler;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

import java.security.NoSuchAlgorithmException;


public class Fragment_SignUp extends Fragment implements View.OnClickListener {
    private static final String TAG = "fragment_SignUp";

    //widgets
    private Button button_signUp, button_back;
    private EditText eText_email, eText_pass, eText_confPass;
    private CheckBox cBox;
    private TextView tView_termsOfService, tView_privacy_pol, tView_signIn;
    private FragmentManager fragmentManager;
    private RelativeLayout layout_login;

    //context
    private Context mContext;

    //authentication:
    Hashing hash = new Hashing();

    //google-com.example.prototype1.auth
    private AuthSingleton mAuthSingleton;
    private FirebaseAuth mAuth;
    private FirebaseApp mApp;
    private Traveler mtravler;

    public Fragment_SignUp() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment__sign_up, container, false);
        assert getActivity() != null;
        mContext = getActivity();
        mApp = FirebaseApp.initializeApp(mContext);
        mApp = FirebaseApp.getInstance();
        mAuthSingleton = AuthSingleton.getInst(mContext);
        mAuth = mAuthSingleton.mAuth();
        mtravler = new Traveler();

        initView(v);
        buttonListeners(v);

        return v;
    }

    private void buttonListeners(View v) {
        button_back.setOnClickListener(this);
        button_signUp.setOnClickListener(this);
        tView_signIn.setOnClickListener(this);

    }

    private void initView(View v) {
        assert getActivity() != null;//asserting to avoid nullPointer
        button_signUp = v.findViewById(R.id.sendConfButton);
        button_back = v.findViewById(R.id.backToLogin_button);
        eText_email = v.findViewById(R.id.email_editText);
        eText_pass = v.findViewById(R.id.pass_editText);
        eText_confPass = v.findViewById(R.id.conf_pass_editText);
        fragmentManager = getActivity().getSupportFragmentManager();
        layout_login = v.findViewById(R.id.login_top);
        cBox = v.findViewById(R.id.checkbox_register);
        tView_signIn = v.findViewById(R.id.have_no_acc_text2);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.backToLogin_button:
            case R.id.have_no_acc_text2:
                mtravler.goToWithFlags(mContext, LoginActivity.class);
                break;

            case R.id.sendConfButton:
                try {
                    registerNewUser();
                } catch (Exception e) {
                    Log.e(TAG, "onClick: Error registerNewUser: " + e.getMessage());
                }
                break;
        }
    }


    private void registerNewUser() throws NoSuchAlgorithmException {
        //todo register new user with obtained credentials: email and password
        String email = eText_email.getText().toString();
        String password = eText_pass.getText().toString();
        String conf_password = eText_confPass.getText().toString();

        //// check if email and other fields are not empty
        if (!email.isEmpty() && cBox.isChecked()) {
            if (checkPass(password, conf_password)) {
                // TODO: 11/8/20 replace this with auth singlton predifined methods
                //  here we register after checking everything is compliant
                mAuthSingleton.createUserWithEmailAndPassword(email, password);

            }
        } else {
            Toast.makeText(mContext, "PLEASE type valid info and check the box to proceed.", Toast.LENGTH_LONG).show();
        }
    }

    //checks the password for compliance and if identical with confirmed password
    private boolean checkPass(@NonNull String password, @NonNull String conf_password) throws NoSuchAlgorithmException {
        boolean isEverythingOKey = false;
        boolean isPassLong = hash.isLongEnough(password);
        boolean pass_has_SpecialChar = hash.hasSpecial(password);
        boolean pass_hasNumber = hash.hasDigits(password);
        boolean pass_hasUpperCase = hash.hasUpperCase(password);
        boolean has_sameHash = hash.mySHA256(password).equals(hash.mySHA256(conf_password));

        Log.d(TAG, "checkPass: Password is :  " + isPassLong + " chars long");
        Log.d(TAG, "checkPass: password has special char :  " + pass_has_SpecialChar);
        Log.d(TAG, "checkPass: password has number char :  " + pass_hasNumber);
        Log.d(TAG, "checkPass: password has upper char :  " + pass_hasUpperCase);
        Log.d(TAG, "checkPass: password has same hash value :  " + has_sameHash);
        //todo check password for length, special chars, numbers, uppercase and hash authenticity.
        if (password.length() > 0 && conf_password.length() > 0) {
            if (isPassLong && pass_has_SpecialChar && pass_hasNumber && pass_hasUpperCase) {
                if (has_sameHash) {
                    isEverythingOKey = true;
                    //todo here we can start with the registration
                    Toast.makeText(mContext, "Password is up to the standards", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(mContext, "Password and confirmed password are not identical", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(mContext, R.string.your_password_should_be_minimum_10_characters_with_one_upper_case_numbers_and_special_chars_i_e, Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(mContext, "No password is provided. Please enter your password", Toast.LENGTH_LONG).show();
        }
        Log.d(TAG, "checkPass: isEverythingOKey: " + isEverythingOKey);
        return isEverythingOKey;
    }

}
