package com.example.prototype1.main;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.example.prototype1.Db.DbSingleton;
import com.example.prototype1.R;
import com.example.prototype1.auth.AuthSingleton;
import com.example.prototype1.model.Chat;
import com.example.prototype1.model.User;
import com.example.prototype1.user_profile.UserProfileActivity;
import com.example.prototype1.utils.ContactsListAdapter;
import com.example.prototype1.utils.Traveler;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.Executor;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";

    private ImageButton search, addNewChat;
    private TextView app_logo_and_name;
    private CircleImageView userProfile_pic;
    private FloatingActionButton fab;
    private EditText search_text;
    private ListView listView;

    private RelativeLayout not_approved_layout, mainLayout;
    private ContactsListAdapter mAdapter;

    //CryptoUtil
    private Context mContext;
    private SharedPreferences shared_pref;
    private SharedPreferences.Editor editor;
    private Traveler traveler;
    private FragmentManager fragmentManager;

    //biometrics
    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    private ArrayList<User> userList = new ArrayList<>();

    //Firebase
    private AuthSingleton mAuthSingleton;
    private DbSingleton dbSingleton;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore mFirestore;
    private FirebaseDatabase mDatabase;

    private DatabaseReference userRef, chatRef, messRef;
    private boolean approved = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = getApplicationContext();
        executor = ContextCompat.getMainExecutor(mContext);
        fragmentManager = getSupportFragmentManager();

        connectFirebase();
        setupCryptoUtil();

        initLayout();
        setButtonListeners();

        listenToAdmin();

        bio_prompt();
        promptInfo();
    }

    private void connectFirebase() {
        mAuthSingleton = AuthSingleton.getInst(mContext);
        mAuth = Objects.requireNonNull(mAuthSingleton).mAuth();
        dbSingleton = DbSingleton.getInstance();
        mDatabase = Objects.requireNonNull(dbSingleton).getDbInstance();
        mAuth = mAuthSingleton.mAuth();
        currentUser = mAuthSingleton.mAuth().getCurrentUser();
        mFirestore = FirebaseFirestore.getInstance();

        userRef = dbSingleton.getUsers_ref();
        chatRef = dbSingleton.getChat_ref();
        messRef = dbSingleton.getMessage_ref();
    }

    private void setupCryptoUtil() {

        traveler = new Traveler();
        shared_pref = PreferenceManager.getDefaultSharedPreferences(mContext);
        editor = shared_pref.edit();
        editor.apply();
    }

    private void initLayout() {
        search_text = findViewById(R.id.search_text);
        userProfile_pic = findViewById(R.id.userProfilePic);
        app_logo_and_name = findViewById(R.id.app_logo_and_name);
        listView = findViewById(R.id.contacts_list);
        not_approved_layout = findViewById(R.id.main_notApproved_layout);
        mainLayout = findViewById(R.id.activity_main_layout);
    }

    private void setButtonListeners() {
        search = findViewById(R.id.search_button);
        addNewChat = findViewById(R.id.add_new_chat);
        fab = findViewById(R.id.fab);

        userProfile_pic.setOnClickListener(this);
        search.setOnClickListener(this);
        addNewChat.setOnClickListener(this);
        fab.setOnClickListener(this);
    }

    private void listenToAdmin() {
        String id = currentUser.getUid();

        DocumentReference user_doc_ref = mFirestore.collection("users").document(id);
        user_doc_ref.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.d(TAG, "listenToAdmin: error: " + error.getLocalizedMessage());
            }
            if (value != null && value.exists()) {
                Log.d(TAG, "onEvent: value: " + value.toString());
                String app = (String) value.get("status");
                if (app.equals("approved")) {
                    approved = true;
                    Toast.makeText(mContext, "Approved by admin. You can use the app", Toast.LENGTH_SHORT).show();
                } else {
                    approved = false;
                }
                changeLayout(approved);
            }
        });
    }

    private void changeLayout(boolean approved) {
        if (approved) {
            not_approved_layout.setVisibility(View.GONE);
            mainLayout.setVisibility(View.VISIBLE);
        } else {
            not_approved_layout.setVisibility(View.VISIBLE);
            mainLayout.setVisibility(View.GONE);
        }
    }

    private void displayUserInformation() {
        Query query = userRef.orderByKey().equalTo(currentUser.getUid());

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    if (dataSnapshot.hasChildren() && ds.exists()) {
                        final User user = Objects.requireNonNull(ds.getValue(User.class));
                        editor.putString("userName", user.getName());
                        editor.commit();
                        if (user.getUserId().equals(currentUser.getUid())) {
                            if (!user.getProfileUrl().equals("no_photo")) {
                                Glide.with(mContext)
                                        .load(user.getProfileUrl())
                                        .circleCrop()
                                        .into(userProfile_pic);
                            } else {
                                Glide.with(mContext)
                                        .load(R.mipmap.ic_launcher)
                                        .circleCrop()
                                        .into(userProfile_pic);
                            }

                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, "onCancelled: cancelled" + databaseError.getMessage());
            }
        });

    }

    private void displayChats() {
        ArrayList<Chat> chatList = new ArrayList<>();
        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                chatList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    if (dataSnapshot.hasChildren() && ds.exists()) {
                        final Chat chat = Objects.requireNonNull(ds.getValue(Chat.class));
                        if (chat.getMembers().containsKey(currentUser.getUid())) {
                            chatList.add(chat);
                        }
                    }
                }
                setAdapter(chatList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, "onCancelled: cancelled" + databaseError.getMessage());
            }
        });

    }

    //todo SEARCH FUNCTIONALITY for chat (Robert)
    public void getUserFromDatabase() {
        String keyword = search_text.getText().toString();

        if (!TextUtils.isEmpty(keyword)) {

            ((Runnable) () -> userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    userList.clear();
                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        if (dataSnapshot.hasChildren() && ds.exists()) {
                            final User user = Objects.requireNonNull(ds.getValue(User.class));
                            String username = user.getName();
                            String email = user.getEmail();

                            if (username.compareToIgnoreCase("no_name") != 0) {
                                if (username.contains(keyword) || email.contains(keyword)) {
                                    userList.add(user);
                                    //todo should be a chat adapter
//                                    setAdapter(userList);
                                } else {
                                    userList.clear();// each time empty the list so you don't have results from previous search!
                                    Toast.makeText(getApplicationContext(), "No match found", Toast.LENGTH_SHORT).show();
                                    mAdapter.notifyDataSetChanged();
                                }
                            }

                            mAdapter.notifyDataSetChanged();
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.d(TAG, "onCancelled: cancelled");
                }
            })).run();


        } else
            Toast.makeText(getApplicationContext(), "Please type a keyword", Toast.LENGTH_SHORT).show();
        //this is how you "remove all views/ empty the list) because userList is empty
        setAdapter(new ArrayList<>());
        mAdapter.notifyDataSetChanged();

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.search_button:
                Toast.makeText(this, "search", Toast.LENGTH_LONG).show();
                getUserFromDatabase();
                break;
            case R.id.add_new_chat:
                Fragment createChatFragment = fragmentManager.findFragmentById(R.id.main_frame_layout);

                if (createChatFragment == null) {
                    try {
                        createChatFragment = new CreateChatFragment();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    fragmentTransaction
                            .addToBackStack(null)
                            .add(R.id.main_frame_layout, createChatFragment)
                            .commit();
                }

                break;
            case R.id.userProfilePic:
                //todo I don't know why but it leaves the app with the traveller
//                traveler.goToWithout(this, UserProfileActivity.class);

                startActivity(new Intent(this, UserProfileActivity.class));
//                System.gc();
                break;
            case R.id.fab:
                //todo get real users + add security to button
                biometricPrompt.authenticate(promptInfo);

                break;
        }
    }

    private void promptInfo() {
        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Verifying device owner identity")
                .setSubtitle("Please touch the finger print for authentication")
                .setNegativeButtonText("Exit")
                .setConfirmationRequired(true)
                .build();

        // Prompt appears when user clicks "Log in".
        // Consider integrating with the keystore to unlock cryptographic operations,
        // if needed by your app.
    }

    private void bio_prompt() {
        biometricPrompt = new BiometricPrompt(this,
                executor, new BiometricPrompt.AuthenticationCallback() {
            int counter = 0;

            @Override
            public void onAuthenticationError(int errorCode,
                                              @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);

                Toast.makeText(getApplicationContext(),
                        "Authentication error: " + errString, Toast.LENGTH_SHORT)
                        .show();
                userProfile_pic.setClickable(false);
                addNewChat.setClickable(false);

//                new Handler().postDelayed(() -> {
//                    mAuthSingleton.signOut();
//                    traveler.goToWithFlags(mContext, LoginActivity.class);
//
//                }, Toast.LENGTH_LONG);
            }

            @Override
            public void onAuthenticationSucceeded(
                    @NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);

                Toast.makeText(getApplicationContext(),
                        "Authentication succeeded!", Toast.LENGTH_SHORT).show();

                userProfile_pic.setClickable(true);
                addNewChat.setClickable(true);

                displayUserInformation();
                displayChats();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();

                Toast.makeText(getApplicationContext(), "Authentication failed",
                        Toast.LENGTH_SHORT)
                        .show();
                userProfile_pic.setClickable(false);
                addNewChat.setClickable(false);
//                displayUserInformation();
//                displayChats();

                counter++;
//                if (counter == 5) {
//                    new Handler().postDelayed(() -> {
//                                Toast.makeText(getApplicationContext(), "You went over the limited amount of tries",
//                                        Toast.LENGTH_LONG)
//                                        .show();
//                                mAuthSingleton.signOut();
//                                traveler.goToWithFlags(mContext, LoginActivity.class);
////                                finish();
//
//                            }
//                            , Toast.LENGTH_LONG);
//                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        biometricPrompt.authenticate(promptInfo);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public static void popBackStack(FragmentManager manager){
        FragmentManager.BackStackEntry first = manager.getBackStackEntryAt(0);
        manager.popBackStack(first.getId(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        //really ugly but works
        if(fragmentManager.getBackStackEntryCount()==1){
            popBackStack(fragmentManager);
        }

        biometricPrompt.authenticate(promptInfo);
    }

    @Override
    public void onStateNotSaved() {
        super.onStateNotSaved();
    }

    private void setAdapter(ArrayList<Chat> list) {
        mAdapter = new ContactsListAdapter(getApplicationContext(), list, fragmentManager, currentUser.getUid(), messRef);
        listView.setAdapter(mAdapter);
    }

}