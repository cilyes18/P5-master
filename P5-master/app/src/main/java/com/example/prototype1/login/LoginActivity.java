package com.example.prototype1.login;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.example.prototype1.Db.DbSingleton;
import com.example.prototype1.R;
import com.example.prototype1.auth.AuthSingleton;
import com.example.prototype1.main.MainActivity;
import com.example.prototype1.permissions.MyPermissions;
import com.example.prototype1.utils.Traveler;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.example.prototype1.ENUMS.VARS.READ_PHONE_STATE;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "LoginActivity";
    private Button loginButton;
    private EditText emailEditText;
    private TextView textView_signUp, texView_forgotPass;
    private Context mContext;

    private EditText passwordEditText;
    private ProgressBar loadingProgressBar;
    private FragmentManager fragmentManager;
    private RelativeLayout layout_login;

    //auth
    private FirebaseAuth loginAuth;
    private AuthSingleton mAuthSingleton;
    private FirebaseUser current_user;
    private FirebaseDatabase mDatabase;

    //
    private boolean isPermissionGranted;

    // TODO remember to check if the user agreed to the permissions, otherwise block the device.
    private MyPermissions mPermissions;
    private Traveler mTraveler;

    //vars
    private String email, password;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mContext = getApplicationContext();
        fragmentManager = getSupportFragmentManager();
        mTraveler = new Traveler();
        mPermissions = new MyPermissions(this);
        mPermissions.permissionsReaState();
        mAuthSingleton = AuthSingleton.getInst(mContext);
        Log.d(TAG, "onCreate: isPermissionGramnted: " + isPermissionGranted);
        isPermissionGranted = mPermissions.isPermissionGranted(0);
        Log.d(TAG, "onCreate: isPermissionGramnted: " + isPermissionGranted);
        prefs = getSharedPreferences("prefs_phone_verified", MODE_PRIVATE);

        connectFirebase();
        initView();
        buttonListener();

        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count < 10) {
                    loginButton.setBackgroundColor(getColor(R.color.colorLightGray));
                    loginButton.setTextColor(getColor(R.color.colorGray));
                    loginButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().length() >= 10) {
                    loginButton.setTextColor(Color.BLACK);
                    loginButton.setBackgroundColor(getColor(R.color.colorGray));
                    loginButton.setEnabled(true);
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (current_user != null && mAuthSingleton.isUserCompliant(current_user)) {
            mTraveler.goToWithFlags(mContext, MainActivity.class);
        } else {

            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("prefs_phone_verified", false);
            editor.apply();
            loginAuth.signOut();
        }

    }

//    private boolean rechedLimitTime(){
//        long sevenHours = 25200000;
//        return differenceInTime() > sevenHours;
//    }

    // TODO: 11/8/20 see how to move these methods to be in auth class
//    private long differenceInTime(){
//            long now = System.currentTimeMillis();
//            long lastTime = current_user.getMetadata().getLastSignInTimestamp();
//            return now - lastTime;
//
//
//    }

    private void connectFirebase() {
        loginAuth = mAuthSingleton.mAuth();
        current_user = loginAuth.getCurrentUser();
        mDatabase = DbSingleton.getmDatabase();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == READ_PHONE_STATE) {
            if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                isPermissionGranted = true;
            }
        }
    }

    private void buttonListener() {
        loginButton.setOnClickListener(this);
        textView_signUp.setOnClickListener(this);
        texView_forgotPass.setOnClickListener(this);
    }

    private void initView() {
        loginButton = findViewById(R.id.button_sign_login);
        emailEditText = findViewById(R.id.email_field);
        passwordEditText = findViewById(R.id.password);
        loadingProgressBar = findViewById(R.id.loading);
        layout_login = findViewById(R.id.login_top);
        texView_forgotPass = findViewById(R.id.textView_forgotpass);
        textView_signUp = findViewById(R.id.textView_signupButton);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_sign_login:
                email = emailEditText.getText().toString();
                password = passwordEditText.getText().toString();
                login(email, password);
                break;
            case R.id.textView_signupButton:
                mTraveler.goFragment(fragmentManager, new Fragment_SignUp(), R.id.container_login);
                layout_login.setVisibility(View.GONE);
                break;
            case R.id.textView_forgotpass:
                mTraveler.goFragment(fragmentManager, new fragment_ForgotPassword(), R.id.container_login);
                layout_login.setVisibility(View.GONE);
                break;
        }
    }

    private void login(String email, String password) {
        if (isPermissionGranted) {
            if (!email.isEmpty() && !password.isEmpty()) {
                mAuthSingleton.signInWithEmailAndPassword(email, password);
            } else {
                Toast.makeText(mContext, "Please type your email and password to proceed", Toast.LENGTH_SHORT).show();
                loginAuth.signOut();//todo: Mo's notes: security stuff: close auth and databases connections when things are canceled or failed
                mDatabase.goOffline();//todo: Mo's notes: security stuff: close auth and databases connections when things are canceled or failed
                Toast.makeText(getApplicationContext(), "Please verify your account.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(mContext, "Please grant permissions to continue", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        mTraveler.removeFromStack(getSupportFragmentManager());
        layout_login.setVisibility(View.VISIBLE);
    }

}

