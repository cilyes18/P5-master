package com.example.prototype1.main;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.security.crypto.MasterKey;

import com.bumptech.glide.Glide;
import com.example.prototype1.Db.DbSingleton;
import com.example.prototype1.R;
import com.example.prototype1.auth.AuthSingleton;
import com.example.prototype1.model.Chat;
import com.example.prototype1.model.DHModel;
import com.example.prototype1.model.User;
import com.example.prototype1.utils.CryptoUtil;
import com.example.prototype1.utils.Traveler;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.util.Base64.DEFAULT;

@RequiresApi(api = Build.VERSION_CODES.O)
public class CreateChatFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "CreateChatFragment";

    private EditText mSearchUsers;
    private Button mDoneCreateChat;
    private ListView mListView;
    private final List<User> userList = new ArrayList<>();

    private final HashMap<String, Boolean> selectedGroup = new HashMap<>();
    private final HashMap<User, Boolean> checkedUsers = new HashMap<>();
    private UserListAdapter userListAdapter;
    private Context mContext;
    private FragmentManager fragmentManager;
    private final Traveler mTraveler = new Traveler();

    //Firebase
    private AuthSingleton mAuthSingleton;
    private DbSingleton dbSingleton;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseDatabase mDatabase;

    private DatabaseReference userRef, chatRef, messRef, dh_ref;
    private CryptoUtil cryptoUtil;
    private SharedPreferences shared_pref;
    private SharedPreferences.Editor editor;
    private final String waiting = "waiting";
    private String myUid, myUserName;

    //User Info Pop-Up Widgets
    private AlertDialog.Builder dialogBuilder;
    private AlertDialog userInfo;
    private CircleImageView mProfilePicture;
    private TextView mUserName, mEmail, mPhoneNumber;

    private MasterKey mainKey;

    public CreateChatFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        fragmentManager = requireActivity().getSupportFragmentManager();

        connectFirebase();
        getUsers();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_create_chat, container, false);
        findWidgets(v);


        mSearchUsers.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                searchChatByName();
            }
        });

        listenToDH();

        return v;
    }

    private void connectFirebase() {
        mAuthSingleton = AuthSingleton.getInst(mContext);
        dbSingleton = DbSingleton.getInstance();
        mDatabase = DbSingleton.getmDatabase();
        mAuth = mAuthSingleton.mAuth();
        currentUser = mAuthSingleton.mAuth().getCurrentUser();
        myUid = currentUser.getUid();

        chatRef = dbSingleton.getChat_ref();
        messRef = dbSingleton.getMessage_ref();
        userRef = dbSingleton.getUsers_ref();
//        dh_ref = dbSingleton.getDh_ref();
        dh_ref = mDatabase.getReference("ECDH_EX");

        try {
            cryptoUtil = new CryptoUtil(mContext);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        shared_pref = PreferenceManager.getDefaultSharedPreferences(mContext);
        editor = shared_pref.edit();
        editor.putBoolean("waitingDone", false);
        editor.apply();

        try {
            mainKey = new MasterKey.Builder(mContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    private void findWidgets(View view) {
        mSearchUsers = view.findViewById(R.id.create_chat_search_user);
        mDoneCreateChat = view.findViewById(R.id.create_chat_done);

        mListView = view.findViewById(R.id.create_chat_user_list);

        mDoneCreateChat.setOnClickListener(this);
    }

    private void getUsers() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    if (dataSnapshot.hasChildren() && ds.exists()) {
                        final User user = ds.getValue(User.class);

                        if (!Objects.requireNonNull(user).getUserId().equals(myUid)) {
                            userList.add(user);
                        } else {
                            myUserName = user.getName();
                        }
                    }
                }
                setAdapter(userList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, "onCancelled: cancelled");
            }
        });
    }

    private void searchChatByName() {
        String keyword = mSearchUsers.getText().toString();
        if (!TextUtils.isEmpty(keyword)) {
            List<User> searchedUsers = new ArrayList<>();
            for (User userToUse : userList) {
                if (userToUse.getName().toLowerCase().contains(keyword.toLowerCase())) {
                    searchedUsers.add(userToUse);
                }
            }
            setAdapter(searchedUsers);
        } else {
            setAdapter(userList);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void create_ECDH_exchange(HashMap<String, Boolean> members) {
        try {
            String node_id = dh_ref.push().getKey();

            HashMap<String, String> myPublicKey = new HashMap<>(); // hashMap for uid and public key
            HashMap<String, String> signedKey = new HashMap<>(); // hashMap for uid and public key
            HashMap<String, String> aesMessageWithKey = new HashMap<>(); // hashMap for uid and public key

            members.put(myUid, true);

            byte[] pubKey = cryptoUtil.myPublic(); //generating public public key
            String puKey_string = Base64.encodeToString(pubKey, DEFAULT); //encoding public public key
            myPublicKey.put(myUid, puKey_string);  //put mine (admin) pub-key

//            String signed = cryptoUtil.signData(node_id); //signing node key
            String signed = cryptoUtil.signData(puKey_string); //signing personalPubKey key
            signedKey.put(myUid, signed); //put my signature

            Query dh_query = dh_ref.child(Objects.requireNonNull(node_id)).orderByKey();

            dh_query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.exists()) {
                        DHModel dhModel = new DHModel(node_id, myUid, myPublicKey, members, signedKey, aesMessageWithKey);

                        dh_ref.child(node_id).setValue(dhModel).addOnSuccessListener(task -> {
                            Log.d(TAG, "onDataChange: successfully created: ");
                        }).addOnFailureListener(e -> {
                            Log.d(TAG, "onDataChange: createDHExchange_error: " + e.getMessage());
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.d(TAG, "onCancelled: Db_error: " + error.getMessage());
                }
            });
        } catch (Exception e) {
            Log.d(TAG, "createDHExchange: error: " + e.getMessage());
        }

    }

    private void listenToDH() {
        Query query = dh_ref.orderByKey();

        query.addValueEventListener(new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    synchronized (Objects.requireNonNull(snapshot)) {
                        try {
                            snapshot.wait(1000);
                            Log.d(TAG, "onDataChange: snapshot.key: " + snapshot.getKey());
                            if (snapshot.hasChildren()) {
                                for (DataSnapshot ds : snapshot.getChildren()) {
                                    DHModel model = Objects.requireNonNull(ds.getValue(DHModel.class));

                                    String ds_key = Objects.requireNonNull(ds.getKey());
                                    String admin_uid = model.getAdmin_uid();
                                    HashMap<String, Boolean> members = model.getMembers();

                                    String adminSignedKey = "";
                                    String adminPublicKey = "";
                                    HashMap<String, String> publicKeys = model.getPublicKeys(); // hashMap for uid and public key
                                    HashMap<String, String> signedKey = model.getSignature(); // hashMap for uid and public key
                                    for (String adminSignKey : signedKey.keySet()) {
                                        if (adminSignKey.equals(admin_uid)) {
                                            adminSignedKey = signedKey.get(adminSignKey);
                                        }
                                    }
                                    for (String adminPubKey : publicKeys.keySet()) {
                                        if (adminPubKey.equals(model.getAdmin_uid())) {
                                            adminPublicKey = publicKeys.get(adminPubKey);
                                        }
                                    }

                                    if (members.containsKey(myUid)) {
                                        if ((model.getPublicKeys().size() < model.getMembers().size())
                                                && !model.getPublicKeys().containsKey(myUid)) {
                                            if (!admin_uid.equals(myUid)) {
                                                if (cryptoUtil.verifySigned(adminPublicKey, adminSignedKey)) {
                                                    postMyPublicKey(ds, model, publicKeys, signedKey);
                                                } else {
                                                    Toast.makeText(requireActivity(), "Alert! This is not the right user.", Toast.LENGTH_SHORT).show();
                                                }
                                                return;
                                            }
                                        }
                                    }

                                    //this context it's 2 members size
                                    if (model.getPublicKeys().size() == model.getMembers().size()) {
                                        String otherMemberSignedKey = "";
                                        String otherMemberPublicKey = "";

                                        for (String otherMemberSignKey : signedKey.keySet()) {
                                            if (!otherMemberSignKey.equals(admin_uid)) {
                                                otherMemberSignedKey = signedKey.get(otherMemberSignKey);
                                            }
                                        }
                                        for (String otherMemberPubKey : publicKeys.keySet()) {
                                            if (!otherMemberPubKey.equals(admin_uid)) {
                                                otherMemberPublicKey = publicKeys.get(otherMemberPubKey);
                                            }
                                        }

                                        if (myUid.equals(admin_uid)) {
                                            Log.d(TAG, "onDataChange: node_key:  " + ds_key);
                                            if (!ds.child("aes_map").exists()) {
                                                if (cryptoUtil.verifySigned(otherMemberPublicKey, otherMemberSignedKey)) {
                                                    calculateAndSaveSharedSecret(ds_key, admin_uid, otherMemberPublicKey, model);
                                                    List<User> checkedUsers = checkUserList(userList);
                                                    createChat(ds_key, members, checkedUsers.get(0).getName() + "_" + myUserName, false);
                                                    return;
                                                } else {
                                                    Toast.makeText(requireActivity(), "Alert! This is not the right user.", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        } else {
                                            if (cryptoUtil.verifySigned(adminPublicKey, adminSignedKey)) {
                                                calculateAndSaveSharedSecret(ds_key, admin_uid, adminPublicKey, model);
                                                startMessageFragment(model.getNode_Id());
                                            } else {
                                                Toast.makeText(requireActivity(), "Alert! This is not the right user.", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    }
//                                    query.removeEventListener(this);
                                }
                            }

                        } catch (Exception e) {
                            Log.d(TAG, "onDataChange: waiting error: " + e.getMessage());
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d(TAG, "onDataChange: DatabaseError: " + error.getMessage());
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void postMyPublicKey(@NonNull DataSnapshot ds, @NonNull final DHModel model,
                                 HashMap<String, String> publicKeys, HashMap<String, String> signedKey) {
        // TODO: 11/26/20 here the user is not admin so steps are:


        byte[] pubKey = cryptoUtil.myPublic();
        String puKey_string = Base64.encodeToString(pubKey, DEFAULT);


        publicKeys.put(myUid, puKey_string);
        //todo now the public keys hashmap should have both parties public keys so we just update the node

        String node_key = ds.getKey();
        Query query = dh_ref.child(Objects.requireNonNull(node_key)).orderByKey();
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Log.d(TAG, "onDataChange: snapshot: " + snapshot.getKey());

//                    String signed = cryptoUtil.signData(model.getNode_Id());
                    String signed = cryptoUtil.signData(puKey_string);
                    signedKey.put(myUid, signed);

                    DHModel localModel = new DHModel(model.getNode_Id(), model.getAdmin_uid(),
                            publicKeys, model.getMembers(), signedKey, model.getAesMessageWithKey());

                    dh_ref.child(localModel.getNode_Id()).setValue(localModel).addOnSuccessListener(task -> {
                        Log.d(TAG, "onDataChange: successfully created: " + task);
                    }).addOnFailureListener(e -> {
                        Log.d(TAG, "onDataChange: createDHExchange_error: " + e.getMessage());
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d(TAG, "onCancelled: DatabaseError: " + error.getMessage());
            }
        });

    }

    private void calculateAndSaveSharedSecret(@NonNull String node_key, String admin_uid, String otherPubKey, DHModel model) {
        try {
            //todo organize this shit

            String myUid = currentUser.getUid();
            Log.d(TAG, "addResultKey: Am I admin of this keyExchange: " + (admin_uid.equals(myUid)));
            HashMap<String, String> pubKeys = model.getPublicKeys();
            HashMap<String, Boolean> members = model.getMembers();

            byte[] decodedPuKey = Base64.decode(otherPubKey, 0);  //decoding back

            cryptoUtil.saveSharedKey(decodedPuKey, node_key);  // TODO: 12/17/20 save the key to keystore to retrieve later with THIS DH_ID as alias.
            Log.d(TAG, "addResultKey: other_user_uid: " + admin_uid);

            //create the chat:
//todo           createSingleChat(node_key, members);
        } catch (Exception e) {
            Log.d(TAG, "onDataChange: exception: " + e.getMessage());
        }
    }

    private void createUserInfoDialog(User selectedUser) {
        String profilePic = selectedUser.getProfileUrl(),
                userName = selectedUser.getName(),
                email = selectedUser.getEmail(),
                phoneNumber = selectedUser.getPhone();
        dialogBuilder = new AlertDialog.Builder(getContext());
        final View userInfoPopUp = getLayoutInflater().inflate(R.layout.popup_user_info, null);

        mProfilePicture = userInfoPopUp.findViewById(R.id.userProfile_pic);
        mUserName = userInfoPopUp.findViewById(R.id.user_name);
        mEmail = userInfoPopUp.findViewById(R.id.user_email);
        mPhoneNumber = userInfoPopUp.findViewById(R.id.user_phone_number);

        Glide.with(this)
                .load(profilePic)
                .circleCrop()
                .into(mProfilePicture);
        mUserName.setText(userName);
        mEmail.setText(email);
        mPhoneNumber.setText(phoneNumber);


        dialogBuilder.setView(userInfoPopUp);
        userInfo = dialogBuilder.create();
        userInfo.show();
    }

    private void setCreateChatDialogue(List<User> checkedUsers) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("Create Chat");
        final EditText input = new EditText(mContext);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Create",
                (dialog, which) -> {
                    String chatName = input.getText().toString();
                    checkedUsers.forEach(user -> selectedGroup.put(user.getUserId(), false));
                    //this is for the group
                    String chatId = "chatId";

                    createChat(chatId, selectedGroup, chatName, true);
                });

        builder.setNegativeButton("Cancel",
                (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private List<User> checkUserList(List<User> currentUserList) {
        List<User> checkedUserListId = new ArrayList<>();
        for (User user : currentUserList) {
            if (user.isCheckedForChatCreation()) {
                checkedUserListId.add(user);
            }
        }
        return checkedUserListId;
    }

    private void createChat(@NonNull String chatID, HashMap<String, Boolean> checkedUsers, String chatName, boolean isGroup) {
        List<String> messageList = new ArrayList<>();

        Query query = chatRef.orderByKey().equalTo(chatID);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Log.d(TAG, "onDataChange: chat already exists");
                } else {
                    checkedUsers.put(myUid, true);
                    final Chat chat = new Chat(chatID, myUid, checkedUsers, messageList, isGroup, chatName);
                    chatRef.child(Objects.requireNonNull(chatID)).setValue(chat).addOnSuccessListener(aVoid -> {
                        Toast.makeText(requireActivity(), R.string.group_successfully_created, Toast.LENGTH_SHORT).show();

                        startMessageFragment(chatID);

                    }).addOnFailureListener(e -> {
                        Toast.makeText(requireActivity(), "Error creating the chat" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "onDataChange: ", e.getCause());
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d(TAG, "onCancelled: Exception " + error.getMessage());
            }
        });
    }

    private void startMessageFragment(String chatId) {
        Bundle bundle = new Bundle();
        bundle.putString("chatId", chatId);

        mTraveler.goToFragmentWithArguments(fragmentManager, new MessageFragment(), R.id.main_frame_layout, bundle);
    }

    private void setAdapter(List<User> userList) {
        userListAdapter = new UserListAdapter(requireContext(), userList);
        mListView.setAdapter(userListAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.create_chat_done:
                List<User> checkedUsers = checkUserList(userList);
                if (checkedUsers.size() > 1) {
                    setCreateChatDialogue(checkedUsers);
                } else if (checkedUsers.size() == 1) {
                    selectedGroup.put(checkedUsers.get(0).getUserId(), false);
//                    createChat(selectedGroup, checkedUsers.get(0).getName() + "_" + myUserName, false);
                    create_ECDH_exchange(selectedGroup);
                } else {
                    Toast.makeText(requireActivity(), R.string.no_group_creation, Toast.LENGTH_SHORT).show();
                }
                break;
//
//            case R.id.aesSent:
//
////                sendMessage();
//                break;
//            case R.id.aesReceive:
//
////                receiveMessage();
//                break;
        }
    }

    //    -------------USER LIST ADAPTER maybe move to utils as well??-------------
    private class UserListAdapter extends ArrayAdapter<User> {

        private List<User> userList;

        public UserListAdapter(@NonNull Context context, List<User> userList) {
            super(context, R.layout.create_chat_list_view_row, userList);
            mContext = context;
            this.userList = userList;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            User user = userList.get(position);

            LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View row = layoutInflater.inflate(R.layout.create_chat_list_view_row, parent, false);

            LinearLayout rowLayout = row.findViewById(R.id.row_layout);

            CheckBox checkUser = row.findViewById(R.id.row_checkbox);
            CircleImageView userProfilePic = row.findViewById(R.id.row_circle_image_view);
            TextView userName = row.findViewById(R.id.row_user_name);

            Glide.with(getContext())
                    .load(user.getProfileUrl())
                    .fitCenter()
                    .centerCrop()
                    .into(userProfilePic);
            userName.setText(user.getName());

            userCheckBox(checkUser, user.isCheckedForChatCreation());

            checkUser.setOnCheckedChangeListener((compoundButton, isChecked) -> {
//                checkedUsers.put(userList.get(position),isChecked);
                userList.get(position).setCheckedForChatCreation(isChecked);
            });

//            userCheckBox(checkUser, user.isCheckedForChatCreation());

            //todo see if it can be of any use

//            checkUser.setOnCheckedChangeListener((compoundButton, isChecked) -> {
//                if (isChecked) {
//                    User u = userList.get(position);
//                    String id = u.getUserId();
//                    boolean isAdmin = currentUser.getUid().equals(id);
//                    selectedGroup.put(u.getUserId(), isAdmin);
//                }
//            });

            rowLayout.setOnLongClickListener(view -> {
                createUserInfoDialog(user);
                return false;
            });

            rowLayout.setOnClickListener(view -> {
                userCheckBox(checkUser);
            });

            return row;
        }

        private void userCheckBox(CheckBox checkBox) {
            checkBox.setChecked(!checkBox.isChecked());
        }

        private void userCheckBox(CheckBox checkBox, boolean isCheckedInUserList) {
            checkBox.setChecked(isCheckedInUserList);
        }

    }


}