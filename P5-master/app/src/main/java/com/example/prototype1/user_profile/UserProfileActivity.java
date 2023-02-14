package com.example.prototype1.user_profile;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.prototype1.Db.DbSingleton;
import com.example.prototype1.R;
import com.example.prototype1.auth.AuthSingleton;
import com.example.prototype1.login.LoginActivity;
import com.example.prototype1.model.User;
import com.example.prototype1.utils.CryptoUtil;
import com.example.prototype1.utils.SettingsAdapter;
import com.example.prototype1.utils.Traveler;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.NoSuchAlgorithmException;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserProfileActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "UserProfileActivity";

    private CircleImageView profilePic;
    private TextView userName, uName;
    private ListView optionsSettingsListView;

    private Context mContext;

    private SettingsAdapter mAdapter;
    private String[] settingsList;
    private String setting;

    //google-auth

    //Firebase
    private AuthSingleton mAuthSingleton;
    private DbSingleton dbSingleton;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore mFirestore;
    private FirebaseDatabase mDatabase;


    private DatabaseReference user_ref, dh_ref;
    private Traveler mTraveler;
    private CryptoUtil cryptoUtil;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_user);
        mContext = getApplicationContext();

        try {
            connectFirebase();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        initView();
        buttonListener();
        displayLoggedUser();

        displaySettings();
    }

    private void connectFirebase() throws NoSuchAlgorithmException {
        mAuthSingleton = AuthSingleton.getInst(mContext);
        dbSingleton = DbSingleton.getInstance();
        mAuth = mAuthSingleton.mAuth();
        mDatabase = dbSingleton.getDbInstance();
        currentUser = mAuthSingleton.mAuth().getCurrentUser();
        mFirestore = FirebaseFirestore.getInstance();

        user_ref = dbSingleton.getUsers_ref();
        dh_ref = dbSingleton.getDh_ref();
        mTraveler = new Traveler();
        cryptoUtil = new CryptoUtil(mContext);
    }


    private void initView() {
        profilePic = findViewById(R.id.userProfile_pic);
        userName = findViewById(R.id.user_name);
        uName = findViewById(R.id.username);
        optionsSettingsListView = findViewById(R.id.options_settings);

        Toolbar toolbar = findViewById(R.id.user_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("UserProfileActivity");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void buttonListener() {
    }

    private void displaySettings() {
        settingsList = getResources().getStringArray(R.array.settings_array);
        setAdapter(settingsList);
    }

    private void setAdapter(String[] list) {
        mAdapter = new SettingsAdapter(getApplicationContext(), list);
        optionsSettingsListView.setAdapter(mAdapter);

        optionsSettingsListView.setOnItemClickListener((parent, view, position, id) -> {
            setting = settingsList[position];
            setSettings(setting);
        });
    }

    private void displayLoggedUser() {
        try {
            user_ref.child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    final User user = Objects.requireNonNull(snapshot.getValue(User.class));
                    userName.setText(user.getName());
                    uName.setText(user.getName());

                    String photoRef = user.getProfileUrl();
                    //retrive user information from the database
                    if (photoRef.equals("no_photo")) {
                        profilePic.setImageResource(R.drawable.ic_signin);
                    } else {
                        try {
                            Glide.with(mContext).load(photoRef).centerCrop().into(profilePic);
                            Log.d(TAG, "onDataChange: found" + photoRef);
                        } catch (Exception e) {
                            Log.e(TAG, "onDataChange: exception", e.getCause());
                            profilePic.setImageResource(R.drawable.ic_signin);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.d(TAG, "onCancelled: Canceled.");
                }
            });


        } catch (NullPointerException e) {
            Log.d(TAG, "onDataChange: error: " + e.getMessage());
        }
    }

    //case "Change picture":
//        Toast.makeText(mContext, "change picture", Toast.LENGTH_SHORT).show();
//                break;
    private void setSettings(String setting) {
        switch (setting) {
            case "Delete Account":
                Toast.makeText(mContext, R.string.request_delete_account, Toast.LENGTH_LONG).show();
                break;
            case "Change password":
                mAuth.sendPasswordResetEmail(Objects.requireNonNull(currentUser.getEmail())).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, R.string.check_inbox, Toast.LENGTH_LONG).show();
                    } else
                        Toast.makeText(this, "Error: " +
                                Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                });
                break;

            case "Log out":
                mAuth.signOut();
                mTraveler.goToWithFlags(this, LoginActivity.class);
                Toast.makeText(mContext, "Logging out", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onClick(View v) {

    }
}
