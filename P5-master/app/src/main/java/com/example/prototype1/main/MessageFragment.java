package com.example.prototype1.main;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.security.crypto.MasterKey;

import com.example.prototype1.Db.DbSingleton;
import com.example.prototype1.R;
import com.example.prototype1.auth.AuthSingleton;
import com.example.prototype1.model.Chat;
import com.example.prototype1.model.Dao_Log;
import com.example.prototype1.model.Message;
import com.example.prototype1.utils.CryptoUtil;
import com.example.prototype1.utils.MessageListAdapter;
import com.example.prototype1.utils.Traveler;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import org.jetbrains.annotations.NotNull;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

import static com.example.prototype1.utils.CryptoUtil.getCurrentTimestamp;

public class MessageFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "MessageFragment";

    private ImageButton attach, send_button, mic;
    private TextView chatNameToDisplay, last_seen;
    private EditText send_message_field;
    private InputMethodManager imm;

    private MessageListAdapter mess_Adapter;
    private ListView listView;

    private ArrayList<Message> messageList = new ArrayList<>();
    private ArrayList<String> messageStringList = new ArrayList<>();
    private Message currentMessage;
    private String chatId, userName;
    private Chat currentChat;

    //CryptoUtil
    private Context mContext;
    private SharedPreferences shared_pref;
    private SharedPreferences.Editor editor;
    private CryptoUtil cryptoUtil;
    private Traveler traveler;
    private FragmentManager fragmentManager;

    //Firebase
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseDatabase mDatabase;

    private DatabaseReference chatRef, messRef, userRef, logRef;

    private DbSingleton dbSingleton;
    private AuthSingleton mAuthSingleton;
    private MasterKey mainKey;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();

        fragmentManager = requireActivity().getSupportFragmentManager();
        requireActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);

        cryptoPrep();
        connectFirebase();

        Bundle bundle = this.getArguments();
        chatId = Objects.requireNonNull(bundle).getString("chatId");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_message, container, false);
        findWidgets(view);
        buttonListeners(view);

        getCurrentChat();
        getMessageList();

        return view;
    }

    private void findWidgets(View view) {
        setHasOptionsMenu(true);

        Toolbar toolbar = view.findViewById(R.id.top_toolbar);
        Objects.requireNonNull(((MainActivity) getActivity())).setSupportActionBar(toolbar);
        Objects.requireNonNull(((MainActivity) getActivity()).getSupportActionBar()).setTitle("Messages");
        Objects.requireNonNull(((MainActivity) getActivity()).getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> getActivity().onBackPressed());

        chatNameToDisplay = view.findViewById(R.id.chatName_message);
        last_seen = view.findViewById(R.id.last_seen);
        listView = view.findViewById(R.id.message_list);
        send_message_field = view.findViewById(R.id.send_message);
        checkMessageInput();
    }

    private void buttonListeners(View view) {
        attach = view.findViewById(R.id.imageButton_addMedia);
        send_button = view.findViewById(R.id.sendMessage_button);
        mic = view.findViewById(R.id.mic);

        attach.setOnClickListener(this);
        send_button.setOnClickListener(this);
        mic.setOnClickListener(this);
    }

    private void connectFirebase() {
        mAuthSingleton = AuthSingleton.getInst(mContext);
        dbSingleton = DbSingleton.getInstance();
        mDatabase = DbSingleton.getmDatabase();
        mAuth = mAuthSingleton.mAuth();
        currentUser = mAuthSingleton.mAuth().getCurrentUser();

        chatRef = dbSingleton.getChat_ref();
        messRef = dbSingleton.getMessage_ref();
        userRef = dbSingleton.getUsers_ref();
        logRef = dbSingleton.getLog_ref();
    }

    private void cryptoPrep() {
        try {
            cryptoUtil = new CryptoUtil(mContext);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        traveler = new Traveler();
        shared_pref = PreferenceManager.getDefaultSharedPreferences(mContext);
        editor = shared_pref.edit();
        editor.putBoolean("hideMess", false);
        editor.putBoolean("prepareForEdit", false);
        editor.apply();
        userName = shared_pref.getString("userName", userName);
//        try {
//            mainKey = new MasterKey.Builder(mContext)
//                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
//                    .build();
//        } catch (GeneralSecurityException | IOException e) {
//            e.printStackTrace();
//        }
    }

    private void getCurrentChat() {
        Query query = chatRef.orderByKey().equalTo(chatId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        if (dataSnapshot.hasChildren() && ds.exists()) {
                            currentChat = Objects.requireNonNull(ds.getValue(Chat.class));
                            String chatName = currentChat.getChatName();
                            String[] displayName = chatName.split("_");

                            shared_pref = PreferenceManager.getDefaultSharedPreferences(mContext);
                            userName = shared_pref.getString("userName", userName);
                            for (String otherParticipant : displayName) {
                                if (!userName.equals(otherParticipant)) {
                                    chatNameToDisplay.setText(otherParticipant);
                                }
                            }
                            if (currentChat.getMessagesInChat() != null) {
                                messageStringList = (ArrayList<String>) currentChat.getMessagesInChat();
                            } else {
                                Toast.makeText(mContext, R.string.no_mess_in_chat, Toast.LENGTH_SHORT).show();
                            }
//                            try {
//                                cryptoUtil.getSecretKeyForDecryption(chatId);
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                            }
                        }
                    }
                } else {
                    Toast.makeText(mContext, R.string.could_not_retrieve_chat, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(mContext, R.string.something_went_wrong, Toast.LENGTH_LONG).show();
                Log.d(TAG, "onCancelled: cancelled" + databaseError.getMessage());
            }
        });

    }

    private void getMessageList() {
        try {
            //todo make it a query and if there is no message display nothing
            Query query = messRef.orderByChild(chatId);
            query.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    messageList.clear();
                    for (DataSnapshot chatsInMessages : dataSnapshot.getChildren()) {
                        if (Objects.equals(chatsInMessages.getKey(), chatId) && chatsInMessages.exists()) {
                            for (DataSnapshot messageSnapshot : chatsInMessages.getChildren()) {
                                if (dataSnapshot.hasChildren() && messageSnapshot.exists()) {
                                    Message message = Objects.requireNonNull(messageSnapshot.getValue(Message.class));

                                    if (currentChat.getMembers().containsKey(currentUser.getUid())) {
                                        String decryptedMessage = "";
                                        try {
                                            if (cryptoUtil.checkSecretKeyForDecryption(message.getMessageId())) {
                                                cryptoUtil.decryptMessageKeFromSessionKey(message.getEncryptedAESKey(), chatId, message.getMessageId());
                                            }
                                            decryptedMessage = cryptoUtil.startDecrypting(message.getContent(), message.getMessageId());
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }

                                        message = new Message(message.getMessageId(), message.getSenderId(),
                                                decryptedMessage, message.getTimestamp(), message.getUrlAttached(), message.getEncryptedAESKey());
                                        messageList.add(message);
                                    }
                                } else {
                                    Toast.makeText(mContext, R.string.no_message_yet, Toast.LENGTH_SHORT).show();
                                    listView.removeAllViews();
                                }
                            }
                        }
                    }
                    sortAndDisplayCurrentChatMessages();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.d(TAG, "onCancelled: Canceled." + databaseError.getMessage());
                }
            });

        } catch (Exception e) {
            Toast.makeText(mContext, "Something went wrong!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onDataChange: error: " + e.getMessage());
        }
    }
    //todo take care of case where there are no messages
    private void sortAndDisplayCurrentChatMessages() {
        messageList.sort((e1, e2) -> e1.getTimestamp().compareTo(e2.getTimestamp()));

        String[] currentDayTimeStamp = getCurrentTimestamp(mContext).split("__");
        String[] currentDayDayAndYear = currentDayTimeStamp[0].split("-");

        if (messageList.size() > 0) {
            //todo take care of display
            String[] lastMessageTimeStamp = messageList.get(messageList.size() - 1).getTimestamp().split("__");
            String[] lastMessDayAndYear = lastMessageTimeStamp[0].split("-");
            String[] lastMessHoursAndMinutes = lastMessageTimeStamp[1].split(":");

            if ((Integer.parseInt(currentDayDayAndYear[2]) - Integer.parseInt(lastMessDayAndYear[2]) == 1)) {
                last_seen.setText(String.format("%s %s", mContext.getString(R.string.last_seen_time), mContext.getString(R.string.yesterday)));
            } else if ((Integer.parseInt(currentDayDayAndYear[2]) - Integer.parseInt(lastMessDayAndYear[2]) == 0)) {
                last_seen.setText(String.format("%s %s:%s", mContext.getString(R.string.last_seen_time), lastMessHoursAndMinutes[0], lastMessHoursAndMinutes[1]));
            }
        } else {
            last_seen.setText(R.string.no_messages);
        }
        setAdapter(messageList, currentUser.getUid());
    }

    private void addChatMessage() {
        String messageId = messRef.child(chatId).push().getKey();

        Query query = messRef.child(chatId).orderByKey().equalTo(messageId);
        try {
            query.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Log.d(TAG, "onDataChange: message already exists: " + snapshot.exists());
                    } else {
                        String content = send_message_field.getText().toString();
                        List<String> encryptedTextAndKey = cryptoUtil.encryptMessage(content, messageId, chatId);
                        String encryptedText = encryptedTextAndKey.get(0);
                        String encryptedSessionKey = encryptedTextAndKey.get(1);

                        //"keyToDecrypt"
                        Message message = new Message(messageId, currentUser.getUid(),
                                encryptedText, getCurrentTimestamp(mContext), "attached", encryptedSessionKey);
                        mess_Adapter.notifyDataSetChanged();
                        query.removeEventListener(this);

                        sendChatMessage(Objects.requireNonNull(messageId), message);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.d(TAG, "onCancelled: cancelled");
                }
            });
        } catch (NullPointerException e) {
            Log.e(TAG, "onDataChange: error: " + e.getMessage());
        }
    }

    private void sendChatMessage(@NotNull String messageId, @NotNull Message message) {
        messageList.add(message);
        messageStringList.add(message.getMessageId());
        currentChat = new Chat(currentChat.getChatId(), currentChat.getAdminId(), currentChat.getMembers(),
                messageStringList, currentChat.isGroup(), currentChat.getChatName());

        chatRef.child(chatId).setValue(currentChat)
                .addOnSuccessListener(chatSuccess -> {
                    messRef.child(chatId).child(messageId).setValue(message)
                            .addOnSuccessListener(messSuccess -> {
                                setupLogSend();
                                Toast.makeText(mContext, R.string.mess_success_add, Toast.LENGTH_SHORT).show();
                                send_message_field.setText("");
                                send_message_field.clearFocus();
                                imm.hideSoftInputFromWindow(send_message_field.getWindowToken(), 0);

                                getMessageList();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(requireActivity(), R.string.groupOrMessage_unsuccessful_validation, Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                {
                    Toast.makeText(requireActivity(), R.string.groupOrMessage_unsuccessful_validation, Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteMessageFromMessageTable(@NotNull Message currentMessage) {
        try {
            Query query = messRef.child(chatId).orderByKey().equalTo(currentMessage.getMessageId());
            query.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        if (dataSnapshot.hasChildren() && ds.exists()) {
                            final Message message = Objects.requireNonNull(ds.getValue(Message.class));
                            if (currentMessage.getMessageId().equals(ds.getKey())) {
                                messRef.child(chatId).child(currentMessage.getMessageId()).removeValue()
                                        .addOnSuccessListener(aVoid -> {
                                            deleteMessageFromChats(message);
                                            messageList.remove(message);
                                            messageStringList.remove(message.getMessageId());
                                            mess_Adapter.notifyDataSetChanged();
                                            Toast.makeText(mContext, R.string.mess_success_delete, Toast.LENGTH_SHORT).show();

                                            setupLogUpdateOrDelete("deleted the message");
                                            query.removeEventListener(this);
                                            getMessageList();
                                        })
                                        .addOnFailureListener(e -> Toast.makeText(requireActivity(), R.string.groupOrMessage_unsuccessful_validation,
                                                Toast.LENGTH_SHORT).show());
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.d(TAG, "onCancelled: Canceled.");
                }
            });
        } catch (NullPointerException e) {
            messRef.removeValue();
            Log.e(TAG, "onDataChange: error: " + e.getMessage());
        }
    }

    private void deleteMessageFromChats(@NotNull Message currentMessage) {
        try {
            Query query = chatRef.child(chatId).child("messagesInChat").orderByValue();
            query.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        if (ds.exists() && (currentMessage.getMessageId().equals(ds.getValue()))) {
                            //todo delete from chat query
                            chatRef.child(chatId).child("messagesInChat")
                                    .child(Objects.requireNonNull(ds.getKey())).removeValue()
                                    .addOnSuccessListener(aVoid ->
                                    {
                                        query.removeEventListener(this);
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(requireActivity(),
                                            R.string.groupOrMessage_unsuccessful_validation, Toast.LENGTH_SHORT).show());
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.d(TAG, "onCancelled: Canceled.");
                }
            });
        } catch (NullPointerException e) {
            messRef.removeValue();
            Log.e(TAG, "onDataChange: error: " + e.getMessage());
        }
    }

    private void updateMessage(@NotNull Message currentMessage, @NotNull String content) {
        try {
            Query query = messRef.child(chatId).orderByKey().equalTo(currentMessage.getMessageId());
            query.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.exists()) {
                        Log.d(TAG, "onDataChange: There is no such message exists: " + snapshot.exists());
                    } else {
                        for (DataSnapshot messageSnapShot : snapshot.getChildren()) {
                            if (snapshot.hasChildren() && messageSnapShot.exists()) {
                                Message message = Objects.requireNonNull(messageSnapShot.getValue(Message.class));

                                if (currentMessage.getMessageId().equals(messageSnapShot.getKey())) {
                                    int messagePosition = messageList.indexOf(currentMessage);
                                    List<String> encryptedTextAndKey = cryptoUtil.encryptMessage(content, message.getMessageId(), chatId);
                                    String encryptedText = encryptedTextAndKey.get(0);

                                    message = new Message(message.getMessageId(), message.getSenderId(),
                                            encryptedText,
                                            message.getTimestamp(), message.getUrlAttached(), message.getEncryptedAESKey());

                                    Message finalMessage = message;
                                    messRef.child(chatId).child(message.getMessageId()).setValue(message)
                                            .addOnSuccessListener(aVoid -> {
                                                messageList.set(messagePosition, finalMessage);
                                                mess_Adapter.notifyDataSetChanged();

                                                setupLogUpdateOrDelete("updated the message");
                                                getMessageList();
                                                shared_pref.edit().putBoolean("prepareForEdit", false).apply();
                                                send_message_field.setText("");
                                                send_message_field.clearFocus();
                                                imm.hideSoftInputFromWindow(send_message_field.getWindowToken(), 0);
                                                Toast.makeText(mContext, "Message has been updated", Toast.LENGTH_SHORT).show();
                                                query.removeEventListener(this);
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(requireActivity(), R.string.groupOrMessage_unsuccessful_validation, Toast.LENGTH_SHORT).show();
                                            });
                                }
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.d(TAG, "onCancelled: cancelled");
                }
            });
        } catch (NullPointerException e) {
            messRef.removeValue();
            Log.d(TAG, "onDataChange: error: " + e.getMessage());
        }
    }

    //todo hide the message for the user
    private void hideMessage(@NotNull Message message) {
        messageList.remove(message);
        shared_pref.edit().putBoolean("hideMess", true).apply();
        editor.commit();
        getMessageList();
    }

    private void setupLogSend() {
        String eventId = logRef.push().getKey();
        for (String receiverId : currentChat.getMembers().keySet()) {
            if (!receiverId.equals(currentUser.getUid())) {
                Dao_Log log = new Dao_Log(eventId, currentUser.getUid(), receiverId, "sent a message", getCurrentTimestamp(mContext));
                logRef.child(Objects.requireNonNull(eventId)).setValue(log.toStringSendMessage());
            }
        }
    }

    private void setupLogUpdateOrDelete(String performedAction) {
        String eventId = logRef.push().getKey();
        Dao_Log log = new Dao_Log(eventId, currentUser.getUid(), performedAction, getCurrentTimestamp(mContext));
        logRef.child(Objects.requireNonNull(eventId)).setValue(log.toStringDeleteOrUpdateMessage());
    }

    private void setAdapter(ArrayList<Message> messageList, String userId) {
        mess_Adapter = new MessageListAdapter(mContext, messageList, userId);

        listView.setAdapter(mess_Adapter);
        listView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);

        //to scroll the messageList view to bottom on data change
        mess_Adapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                listView.setSelection(mess_Adapter.getCount() - 1);
            }
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            currentMessage = messageList.get(position);
            setMessageDialog(currentMessage);
        });

    }

    private void setMessageDialog(@NotNull Message message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        String[] settings_array;
        if (message.getSenderId().equals(currentUser.getUid())) {
            settings_array = getResources().getStringArray(R.array.personal_message_settings_array);
            builder.setTitle(R.string.dialog_title_message)
                    .setItems(R.array.personal_message_settings_array, (dialog, which) -> {
                        for (String item : settings_array) {
                            int positionOfItem = Arrays.asList(settings_array).indexOf(item);
                            if (positionOfItem == which) {
                                switch (item) {
                                    case "Copy":
                                        ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                                        ClipData clip = ClipData.newPlainText("copy_text", message.getContent());
                                        clipboard.setPrimaryClip(clip);
                                        Toast.makeText(mContext, mContext.getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show();
                                        break;
                                    case "Delete":
                                        deleteMessageFromMessageTable(currentMessage);
                                        break;
                                    case "Edit":
                                        shared_pref.edit().putBoolean("prepareForEdit", true).apply();
                                        send_message_field.setText(message.getContent());
                                        //todo display the keyboard automatically
                                        send_message_field.findFocus();
                                        send_message_field.requestFocus();
                                        boolean isShowing = imm.showSoftInput(send_message_field, InputMethodManager.SHOW_IMPLICIT);
                                        if (!isShowing) {
                                            requireActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
                                        }
                                        Toast.makeText(mContext, R.string.check_message_box, Toast.LENGTH_LONG).show();
                                        break;
                                    case "Forward":
                                        Toast.makeText(mContext, "clicked Forward", Toast.LENGTH_SHORT).show();
                                        //todo keep in clipboard and open contacts page.
                                        break;
                                    case "Reply":
                                        message.useAsReply(true);
                                        //todo keep text/picture + the reply itself on bottom.
                                        Toast.makeText(mContext, "clicked Reply", Toast.LENGTH_SHORT).show();
                                        break;
                                }
                            }
                        }
                    });
        } else {
            settings_array = getResources().getStringArray(R.array.different_user_message_settings_array);
            builder.setTitle(R.string.dialog_title_message)
                    .setItems(R.array.different_user_message_settings_array, (dialog, which) -> {
                        for (String item : settings_array) {
                            int positionOfItem = Arrays.asList(settings_array).indexOf(item);
                            if (positionOfItem == which) {
                                switch (item) {
                                    case "Copy":
                                        ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                                        ClipData clip = ClipData.newPlainText("copy_text", message.getContent());
                                        clipboard.setPrimaryClip(clip);
                                        Toast.makeText(mContext, mContext.getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show();
                                        break;
                                    case "Delete":
                                        hideMessage(message);
                                        Toast.makeText(mContext, "Message successfully deleted", Toast.LENGTH_SHORT).show();
                                        break;
                                    case "Forward":
                                        Toast.makeText(mContext, "clicked Forward", Toast.LENGTH_SHORT).show();
                                        break;
                                    case "Reply":
                                        Toast.makeText(mContext, "clicked Reply", Toast.LENGTH_SHORT).show();
                                        break;
                                }
                            }
                        }
                    });
        }

        AlertDialog dialog = builder.create();

        dialog.show();
    }

    private void checkMessageInput() {
        //here we show the send button
        send_message_field.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                if (charSequence.length() > 0) {
                    send_button.setVisibility(View.VISIBLE);
                    send_button.setEnabled(true);
                    mic.setVisibility(View.INVISIBLE);
                    mic.setEnabled(false);
                } else {
                    send_button.setVisibility(View.INVISIBLE);
                    send_button.setEnabled(false);
                    mic.setVisibility(View.VISIBLE);
                    mic.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.imageButton_addMedia:
                Toast.makeText(getActivity(), "attach", Toast.LENGTH_SHORT).show();

                break;
            case R.id.sendMessage_button:
                String content = send_message_field.getText().toString();
                if (shared_pref.getBoolean("prepareForEdit", true)) {
                    if (!content.equals("")) {
                        updateMessage(currentMessage, content);
                    } else {
                        throw new IllegalArgumentException();
                    }
                } else {
                    addChatMessage();
                    Toast.makeText(getActivity(), R.string.message_sent_successfully, Toast.LENGTH_LONG).show();
                }
                break;

            case R.id.mic:
                Toast.makeText(getActivity(), "mic", Toast.LENGTH_SHORT).show();
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
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_message_settings, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.change_name_for_conversation:
                renameChat();

                break;
            case R.id.leave_conversation:
                leaveChat();

                break;
        }
        return false;
    }

    private void renameChat() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

        builder.setTitle(mContext.getString(R.string.change_name_for_conversation));
        final EditText input = new EditText(mContext);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(currentChat.getChatName());
        builder.setView(input);

        builder.setPositiveButton("Change",
                (dialog, which) -> {
                    String chatName = input.getText().toString();
                    if (!TextUtils.isEmpty(chatName)) {
                        changeNameOfConversation(chatName);
                    }
                });

        builder.setNegativeButton("Cancel",
                (dialog, which) -> dialog.cancel());

        builder.show();
    }


    private void changeNameOfConversation(@NotNull String newChatName) {
        if (currentChat.getAdminId().equals(currentUser.getUid())) {
            Query query = chatRef.orderByKey().equalTo(chatId);
            query.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            if (dataSnapshot.hasChildren() && ds.exists()) {
                                Chat newChat = Objects.requireNonNull(ds.getValue(Chat.class));
                                chatNameToDisplay.setText(newChatName);
                                newChat = new Chat(newChat.getChatId(), newChat.getAdminId(), newChat.getMembers(),
                                        newChat.getMessagesInChat(), newChat.isGroup(), newChatName);

                                chatRef.child(chatId).setValue(newChat)
                                        .addOnSuccessListener(aVoid -> {
                                            send_message_field.setText("");
                                            send_message_field.clearFocus();
                                            imm.hideSoftInputFromWindow(send_message_field.getWindowToken(), 0);
                                            getMessageList();

                                            Toast.makeText(requireActivity(), R.string.chat_successfully_renamed, Toast.LENGTH_SHORT).show();
                                            query.removeEventListener(this);
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(requireActivity(), R.string.groupOrMessage_unsuccessful_validation, Toast.LENGTH_SHORT).show();
                                        });
                            }
                        }
                    } else {
                        Toast.makeText(mContext, R.string.could_not_retrieve_chat, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(mContext, R.string.something_went_wrong, Toast.LENGTH_LONG).show();
                    Log.d(TAG, "onCancelled: cancelled" + databaseError.getMessage());
                }
            });
        }
    }

    private void leaveChat() {
        AlertDialog.Builder leaveConversation = new AlertDialog.Builder(mContext);
        AlertDialog.Builder youSure = new AlertDialog.Builder(mContext);

        leaveConversation.setTitle(mContext.getString(R.string.leave_conversation));
        youSure.setTitle(mContext.getString(R.string.sure_leave_conversation));

        leaveConversation.setPositiveButton("Yes",
                (dialog, which) -> {
                    youSure.setPositiveButton("Yes",
                            (sureDialog, sureWhich) -> {
                                leaveCurrentConversation();
                            })
                            .setNegativeButton("No",
                                    (sureDialog, sureWhich) -> {
                                        sureDialog.cancel();
                                        dialog.cancel();
                                    }).show();
                }).setNegativeButton("Cancel",
                (dialog, which) -> dialog.cancel()).show();
    }

    private void leaveCurrentConversation() {
        Query query = chatRef.orderByKey().equalTo(chatId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        if (dataSnapshot.hasChildren() && ds.exists()) {
                            Chat newChat = Objects.requireNonNull(ds.getValue(Chat.class));

                            HashMap<String, Boolean> remainingUsers = new LinkedHashMap<>();

                            for (String member : Objects.requireNonNull(newChat).getMembers().keySet()) {
                                if (member.equals(currentUser.getUid())) {
                                    boolean isCurrentUserAdmin = newChat.getMembers().get(member);

                                    newChat.getMembers().remove(currentUser.getUid());
                                    remainingUsers = newChat.getMembers();
                                    if (newChat.getMembers().size() > 0) {
                                        if (isCurrentUserAdmin) {
                                            remainingUsers.replace(remainingUsers.keySet().iterator().next(),
                                                    false, true);
                                        }
                                    }
                                }
                            }

                            if (newChat.getMembers().size() > 0) {
                                newChat = new Chat(newChat.getChatId(), newChat.getMembers().keySet().iterator().next(), remainingUsers,
                                        newChat.getMessagesInChat(), newChat.isGroup(), newChat.getChatName());

                                chatRef.child(chatId).setValue(newChat)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(requireActivity(), R.string.chat_successfully_left, Toast.LENGTH_SHORT).show();
                                            query.removeEventListener(this);
                                            traveler.goToWithout(mContext, MainActivity.class);
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(requireActivity(), R.string.groupOrMessage_unsuccessful_validation, Toast.LENGTH_SHORT).show();
                                        });
                            } else {
                                chatRef.child(chatId).removeValue()
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(requireActivity(), R.string.chat_successfully_left, Toast.LENGTH_SHORT).show();
                                            query.removeEventListener(this);
                                            traveler.goToWithout(mContext, MainActivity.class);
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(requireActivity(), R.string.groupOrMessage_unsuccessful_validation, Toast.LENGTH_SHORT).show();
                                        });
                            }

                        }
                    }
                } else {
                    Toast.makeText(mContext, R.string.could_not_retrieve_chat, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(mContext, R.string.something_went_wrong, Toast.LENGTH_LONG).show();
                Log.d(TAG, "onCancelled: cancelled" + databaseError.getMessage());
            }
        });
    }

}