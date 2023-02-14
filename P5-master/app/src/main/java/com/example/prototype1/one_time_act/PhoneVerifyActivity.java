package com.example.prototype1.one_time_act;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.prototype1.R;
import com.example.prototype1.auth.AuthSingleton;
import com.example.prototype1.camera.MyCamera;
import com.example.prototype1.login.LoginActivity;
import com.example.prototype1.main.MainActivity;
import com.example.prototype1.model.User;
import com.example.prototype1.permissions.MyPermissions;
import com.example.prototype1.utils.Hashing;
import com.example.prototype1.utils.Traveler;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.example.prototype1.ENUMS.VARS.DO_READ_EXTERNAL_STORAGE;
import static com.example.prototype1.ENUMS.VARS.DO_WRITE_EXTERNAL_STORAGE;
import static com.example.prototype1.ENUMS.VARS.REQUEST_CAMERA;

public class PhoneVerifyActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "PhoneVerifyActivity";

    private static final String VERIFICATION_PROGRESS = "VERIFICATION_PROGRESS_KEY";
    private static final String SUSPICIOUS_PROGRESS = "SUSPICIOUS_PROGRESS_KEY";
    private static final String WRONG_PHONE_COUNTER = "WRONG_PHONE_COUNTER_KEY";
    private static final String URI_TRACKER = "URI_TRACKER_KEY";

    //view
    private CircleImageView profile;
    private EditText eText_phone, eText_name;
    private Button button_confirm_phone, button_confirm_name;
    private Context mContext;
    private ProgressBar mProgressBar;
    private TextView status;
    private RelativeLayout phoneVerifyLayout, nameLayout;

    //VARS
    private boolean is_verified = false, is_suspicious = false;
    private int wrongPhone_counter = 0, verificationAttempt_counter = 0;


    //com.example.prototype1.auth & DB
    private FirebaseAuth mAuth;
    private AuthSingleton mAuthSingleton;
    private FirebaseUser currenFuser;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private PhoneAuthProvider pap;
    private FirebaseDatabase mDatabase;
    private DatabaseReference myRef, phone_regRef, usersRef;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference profilePicStorage;

    //camera
    private MyCamera camera;
    private MyPermissions mPermissions; //this made public bc we will need it in other activities.
    private Traveler mTraveler; //this made public bc we will need it in other activities.
    private User user;
    private boolean permissionGranted = false;
    private Uri mUri;
    private SharedPreferences prefs;

    // todo  this activity check if user have phone : if not then this is first time.
    // todo ?? show phone field  and button to confirm<<<>>> handle all result  cases
    //todo  ?? if have phone, start verification proccess <<>> handle all result cases

    private void changePrefState(boolean trueOrFalse) {
        Log.d(TAG, "changePrefState: isPhone_verified: " + isAlreadyPhoneVerified());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("prefs_phone_verified", trueOrFalse);
        editor.apply();
        Log.d(TAG, "changePrefState: isPhone_verified: " + isAlreadyPhoneVerified());
    }

    private boolean isAlreadyPhoneVerified() {
        return prefs.getBoolean("prefs_phone_verified", is_verified);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(VERIFICATION_PROGRESS, is_verified);
        outState.putInt(WRONG_PHONE_COUNTER, wrongPhone_counter);
        outState.putBoolean(SUSPICIOUS_PROGRESS, is_suspicious);
        outState.putParcelable(URI_TRACKER, mUri); //Todo: Mo's Note Uri abstract class implements Parcelable.
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_verify);
        mContext = getApplicationContext();
        mPermissions = new MyPermissions(this);
        mPermissions.permissionsCamera();
        mTraveler = new Traveler();
        camera = new MyCamera(this);
        permissionGranted = mPermissions.isPermissionGranted(1) && mPermissions.isPermissionGranted(2) && mPermissions.isPermissionGranted(3);
        prefs = getSharedPreferences("prefs_phone_verified", MODE_PRIVATE);
        is_verified = prefs.getBoolean("prefs_phone_verified", false);
        //getting any restored action back, to avoid losing display or avoiding the verification process
        initLayout();
        buttonListeners();
        retrieveSaved(savedInstanceState);
        initAuth();

        button_confirm_phone.setOnClickListener(click -> {
            String phone = eText_phone.getText().toString();
            if (!phone.isEmpty()) {
                checkNumberValid(phone);
            } else {
                Toast.makeText(mContext, "Type a phone number and a name", Toast.LENGTH_SHORT).show();
            }
        });
        //name  and picture update swipe-button
        button_confirm_name.setOnClickListener(click -> {
            String name = eText_name.getText().toString();
            if (!name.isEmpty()) {
                if (mUri != null) {
                    uploadImage(mUri, name);
                } else Toast.makeText(mContext, "Take a picture", Toast.LENGTH_SHORT).show();
                // TODO: 11/8/20 update the image first then the name

            } else {
                Toast.makeText(mContext, "Type a valid name", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadImage(Uri uri, String name) {
        String id = user.getUserId();
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setTitle("Uploading and updating info. Please wait");
        progressDialog.show();

        if (uri != null) {
            profilePicStorage = mFirebaseStorage.getReference("users_prof").child(id);
            profilePicStorage.putFile(uri).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    profilePicStorage.getDownloadUrl().addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()) {
                            String url = String.valueOf(task1.getResult());
                            updateUSerWIthRest(id, name, url);
                            progressDialog.dismiss();
                        }

                    }).addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Log.d(TAG, "onFailure: getURL error: " + e.getMessage());
                    });
                }
            }).addOnCanceledListener(progressDialog::dismiss)
                    .addOnFailureListener(e -> {
                        Log.d(TAG, "uploadImage: error: " + e.getMessage());
                        progressDialog.dismiss();

                    })
                    .addOnProgressListener(snapshot -> {
                        double progress = (100.0 * snapshot.getBytesTransferred() / snapshot.getTotalByteCount());
                        progressDialog.setMessage("uploaded " + (int) progress + "%");
                    });

        }
    }

    private void updateUSerWIthRest(String id, String name, String url) {
        Query query = usersRef.orderByKey().equalTo(id);
        query.keepSynced(false);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User userComplete = new User(id, name, user.getEmail(), user.getPhone(), url, false);
                    usersRef.child(id).setValue(userComplete).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            mTraveler.goToWithFlags(mContext, MainActivity.class);
                        }
                    }).addOnFailureListener(e -> {
                        Log.d(TAG, "onDataChange: Db_error: :" + e.getMessage());
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void updateUserPhone(String ph) {
        // TODO: 11/8/20 finish here !!!!
        Query query = usersRef.orderByKey().equalTo(user.getUserId());
        query.keepSynced(false);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    usersRef.child(user.getUserId())
                            .child("phone").setValue(ph)
                            .addOnCompleteListener(task -> {
                                Log.d(TAG, "onComplete: Update_phone_Task: " + task.isSuccessful());
                                updateUIForName(user);
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d(TAG, "onCancelled: Db_error: " + error.getMessage());
            }
        });

    }

    private void buttonListeners() {
        profile.setOnClickListener(this);
    }

    private void initAuth() {
        mAuthSingleton = AuthSingleton.getInst(mContext);
        mAuth = mAuthSingleton.mAuth();
        currenFuser = mAuth.getCurrentUser();
        pap = PhoneAuthProvider.getInstance();
        mDatabase = FirebaseDatabase.getInstance();
        mFirebaseStorage = FirebaseStorage.getInstance();
        myRef = mDatabase.getReference();
        usersRef = myRef.child("users");
        phone_regRef = myRef.child("Phone_Reg");
        user = getUser(currenFuser);

    }

    //first method called to check the user data. if he has phone we update UI accordingly
    private User getUser(@NonNull final FirebaseUser currentUser) {
        ((Runnable) () -> {
            Query userQuery = usersRef.child(currentUser.getUid());
            userQuery.keepSynced(false);
            userQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        user = snapshot.getValue(User.class);
                        updateUIForPhone(user);
                        // TODO: 11/8/20 come back here and finish

                    } else {
                        // TODO: 11/2/20 if user node doesnt exists we should logout and go back to Login
                        mAuth.signOut();
                        mTraveler.goToWithFlags(mContext, LoginActivity.class);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.d(TAG, "onCancelled: Db_error: " + error.getMessage());
                }
            });
        }).run();

        return user;

    }

    private void updateUIForPhone(User user) {
        if (user.getPhone().equals("no_phone")) {
            mProgressBar.setVisibility(View.GONE);
            eText_phone.setVisibility(View.VISIBLE);
            button_confirm_phone.setVisibility(View.VISIBLE);
            button_confirm_phone.setEnabled(true);
        } else {
            Hashing hashing = new Hashing();
            try {
                String phonehash = hashing.mySHA256(user.getPhone());
                Log.d(TAG, "updateUIForPhone: " + phonehash);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

            checkNumberValid(user.getPhone());
        }

    }

    private void updateUIForName(User user) {
        if (user.getName().equals("no_name")) {
            phoneVerifyLayout.setVisibility(View.GONE);
            phoneVerifyLayout.setClickable(false);
            nameLayout.setVisibility(View.VISIBLE);
        } else
            mTraveler.goToWithFlags(mContext, MainActivity.class);
    }

    private void initLayout() {
        profile = findViewById(R.id.imageView_oneTimeProfile);
        eText_phone = findViewById(R.id.eText_oneTimePNumber);
        eText_name = findViewById(R.id.eText_oneTimeName);
        eText_phone.clearFocus();
        button_confirm_phone = findViewById(R.id.button_oneTime_send_phone);
        button_confirm_name = findViewById(R.id.button_oneTime_send_name_picture);
        mProgressBar = findViewById(R.id.progressBar_verifyPhone);
        status = findViewById(R.id.phone_verify_status);
        phoneVerifyLayout = findViewById(R.id.phone_identification_layout);
        nameLayout = findViewById(R.id.layout_name_photo);
        button_confirm_name.setOnClickListener(this);
        button_confirm_phone.setOnClickListener(this);
    }

    private void retrieveSaved(Bundle savedInstanceState) {

        if (savedInstanceState != null) {
            onRestoreInstanceState(savedInstanceState); // restoring data from previous session.
            wrongPhone_counter = savedInstanceState.getInt(WRONG_PHONE_COUNTER);
            is_verified = savedInstanceState.getBoolean(VERIFICATION_PROGRESS);
            is_suspicious = savedInstanceState.getBoolean(SUSPICIOUS_PROGRESS);
            mUri = savedInstanceState.getParcelable(URI_TRACKER);
            Log.d(TAG, "onCreate: restored boolean: is_verified== " + is_verified);
            Log.d(TAG, "onCreate: restored int: wrongPhone_counter== " + wrongPhone_counter);
            Log.d(TAG, "onCreate: restored boolean: is_suspicious== " + is_suspicious);
            Log.d(TAG, "onCreate: restored URI: is_suspicious== " + mUri);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: here we are");

    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: here we are");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: here we are");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: here we are");
    }

    /**
     * @param phone the phone provided by the user.
     * @usage checks if the phone exists on database, else disconnect warn the user.
     */
    private void checkNumberValid(@NonNull String phone) {
        if (wrongPhone_counter == 3) {
            Log.d(TAG, "checkNumberValid: counter is Max trials " + wrongPhone_counter);
            Toast.makeText(mContext, "HEY WHAT ARE YOU DOING?", Toast.LENGTH_SHORT).show();
            mAuth.signOut();
            mTraveler.goToWithFlags(mContext, LoginActivity.class);
            //todo maybe i should do/notify some/thing/one!!!!

        }
        Query query = phone_regRef.orderByValue().equalTo(phone);
        query.keepSynced(false);
        query.limitToLast(1);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Log.d(TAG, "onDataChange: Phone is Valid");
                    Log.d(TAG, "onDataChange: Snapshot: " + snapshot.getValue());
                    Log.d(TAG, "onDataChange: Snapshot: " + snapshot.getKey());
                    verifyNumber(phone);

                } else {
                    wrongPhone_counter++;
                    Log.d(TAG, "onDataChange: counter value: " + wrongPhone_counter);
                    Log.d(TAG, "onDataChange: isVerified : " + is_verified);
                }
            }


            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d(TAG, "onCancelled: Cancellation error: " + error.getMessage());
                wrongPhone_counter++;
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA && requestCode == DO_READ_EXTERNAL_STORAGE && requestCode == DO_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[1] == PERMISSION_GRANTED
                    && grantResults[2] == PERMISSION_GRANTED && grantResults[3] == PERMISSION_GRANTED) {
                permissionGranted = true;
            }
        }
    }

    //works like a charm
    private void verifyNumber(@NonNull String phone) {
        // TODO: 11/1/20 basically, from my point of view, technically speaking without SHA1 cannot use phone verification

        pap.verifyPhoneNumber(phone, 60, TimeUnit.SECONDS, this,
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                        String sms = phoneAuthCredential.getSmsCode();
                        Log.d(TAG, "onVerificationCompleted: firebase sms: " + sms);

                        if (!sms.isEmpty()) {
                            is_verified = true;
                            is_suspicious = false;
                            status.setTextColor(getResources().getColor(R.color.green));
                            status.setText("Verification successful");
                            changePrefState(is_verified);
                            if (user.getPhone().equals("no_phone")) {
                                updateUserPhone(phone);
                            }
                            updateUIForName(user);

                        } else {
                            phoneAuthCredential.zza(false);
                            Log.d(TAG, "onVerificationCompleted: SMS NOT MATCHING!");
                            is_suspicious = true; // label user as suspicious
                            is_verified = false;

                        }

                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            Log.d(TAG, "onVerificationFailed: invalid number: " + e.getMessage());
                            Toast.makeText(mContext, "Invalid credentials: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            is_verified = false;
                            is_suspicious = false; //user may enter wrong number

                        } else if (e instanceof FirebaseTooManyRequestsException) {
                            //Todo. Mo's : here we can ban the user from using our service since he reached the maximum sms tries.
                            Log.d(TAG, "onVerificationFailed: Too many requests: " + e.getMessage());
                            Toast.makeText(mContext, "Too many requests. your account is blocked for suspicious activities", Toast.LENGTH_SHORT).show();
                            button_confirm_phone.setActivated(false);
                            is_verified = false;
                            is_suspicious = true; //here the user will be labeled as suspicious.
                            mAuth.signOut();
                            //Todo here we should handle this situation with care.
                            mTraveler.goToWithFlags(mContext, LoginActivity.class);

                        } else {
                            Toast.makeText(mContext, "Verification error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            status.setTextColor(getResources().getColor(R.color.orange));
                            status.setText("Verification Failed! Try again by clicking the send button");
                            Log.d(TAG, "onVerificationFailed: invalid number: " + e.getMessage());//none of the above
                            is_verified = false;
                            is_suspicious = false;
                            updateUIForPhone(user);
                        }
                    }

                    @Override
                    public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        super.onCodeSent(s, forceResendingToken);
                        Log.d(TAG, "onCodeSent: String s: " + s);
                        Log.d(TAG, "onCodeSent: forceResendingToken : " + forceResendingToken);
                        status.setText("Code sent!");
                        is_verified = false;
                        is_suspicious = false;
                    }

                    @Override
                    public void onCodeAutoRetrievalTimeOut(@NonNull String s) {
                        super.onCodeAutoRetrievalTimeOut(s);
                        Log.d(TAG, "onCodeAutoRetrievalTimeOut: String s: " + s);
                        status.setTextColor(getResources().getColor(R.color.orange));
                        status.setText("verification timed out, try again");
                        PhoneAuthProvider.ForceResendingToken.zza();
                    }

                });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        mAuthSingleton.signOut();
        mTraveler.goToWithFlags(mContext, LoginActivity.class);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.imageView_oneTimeProfile) {
            Log.d(TAG, "onClick: image clicked");
            if (permissionGranted) {
                camera.takePicture();
            } else {
                Toast.makeText(mContext, "Please grant The camera permission to proceed!", Toast.LENGTH_SHORT).show();
            }

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CAMERA && resultCode == RESULT_OK) {
            assert data != null;
            setUri(camera.getUri());
            Glide.with(this).load(getUri()).centerCrop().into(profile);
        } else
            Toast.makeText(mContext, "Result error: " + resultCode, Toast.LENGTH_SHORT).show();

    }

    private void setUri(Uri uri) {
        mUri = uri;
    }

    private Uri getUri() {
        return mUri;
    }
}
