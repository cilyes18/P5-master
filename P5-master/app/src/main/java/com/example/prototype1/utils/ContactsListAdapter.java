package com.example.prototype1.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import com.bumptech.glide.Glide;
import com.example.prototype1.R;
import com.example.prototype1.main.MessageFragment;
import com.example.prototype1.model.Chat;
import com.example.prototype1.model.Message;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class ContactsListAdapter extends ArrayAdapter<Chat> {
    private static final String TAG = "ContactsListAdapter";

    private SharedPreferences shared_pref;
    private List<Chat> chatList;

    private FragmentManager fragmentManager;
    private CircleImageView mProfileImg;
    private String currentUserId, userName;
    private DatabaseReference messRef;

    private Context mContext;
    private Traveler mTraveler;
    private CryptoUtil cryptoUtil;

    public ContactsListAdapter(Context context, ArrayList<Chat> chatList, FragmentManager fragmentManager,
                               String currentUserId, DatabaseReference messRef) {
        super(context, R.layout.chat_item, chatList);
        this.mContext = context;
        this.fragmentManager = fragmentManager;
        this.chatList = chatList;
        this.currentUserId = currentUserId;
        this.messRef = messRef;

        mTraveler = new Traveler();
        try {
            cryptoUtil = new CryptoUtil(mContext);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        final View viewHolder = inflater.inflate(R.layout.chat_item, parent, false);

        Chat currentChat = chatList.get(position);

        mProfileImg = viewHolder.findViewById(R.id.chat_pic);
        TextView chatNameView = viewHolder.findViewById(R.id.chat_field);
        TextView last_message = viewHolder.findViewById(R.id.last_message);

        LinearLayout layout = viewHolder.findViewById(R.id.user_item_layout);

        //todo put picture to chat
        Glide.with(getContext())
                .load(R.mipmap.ic_launcher)
                .fitCenter()
                .circleCrop()
                .into(mProfileImg);

        if (currentChat.isGroup()) {
            chatNameView.setText(currentChat.getChatName());
        } else {
            //if it's a 1-1 to display other person's name
            String chatName = currentChat.getChatName();
            String[] displayName = chatName.split("_");

            shared_pref = PreferenceManager.getDefaultSharedPreferences(mContext);
            userName = shared_pref.getString("userName", userName);
            for (String otherParticipant : displayName) {
                if (!userName.equals(otherParticipant)) {
                    chatNameView.setText(otherParticipant);
                }
            }
        }
        getLastMessageToDisplay(currentChat, last_message);

        layout.setOnClickListener(v -> {
            // implementation for displaying the comments for each post
            Bundle bundle = new Bundle();
            bundle.putString("chatId", currentChat.getChatId());

            mTraveler.goToFragmentWithArguments(fragmentManager, new MessageFragment(), R.id.main_frame_layout, bundle);
        });

        return viewHolder;
    }


    private void getLastMessageToDisplay(Chat currentChat, TextView last_message) {
        ArrayList<Message> messageList = new ArrayList<>();
        try {
            //todo make it a query and if there is no message display nothing
            Query query = messRef.orderByChild(currentChat.getChatId());
            query.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot chatsInMessages : dataSnapshot.getChildren()) {
                        if (Objects.equals(chatsInMessages.getKey(), currentChat.getChatId()) && chatsInMessages.exists()) {
                            for (DataSnapshot messageSnapshot : chatsInMessages.getChildren()) {
                                if (dataSnapshot.hasChildren() && messageSnapshot.exists()) {
                                    Message message = Objects.requireNonNull(messageSnapshot.getValue(Message.class));

                                    if (currentChat.getMembers().containsKey(currentUserId)) {
                                        String decryptedMessage = "";
                                        try {
                                            if (cryptoUtil.checkSecretKeyForDecryption(message.getMessageId())) {
                                                cryptoUtil.decryptMessageKeFromSessionKey(message.getEncryptedAESKey(), currentChat.getChatId(), message.getMessageId());
                                            }
                                            decryptedMessage = cryptoUtil.startDecrypting(message.getContent(), message.getMessageId());
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                        if (message.getSenderId().equals(currentUserId)) {
                                            message = new Message(message.getMessageId(), message.getSenderId(),
                                                    "You: " + decryptedMessage, message.getTimestamp(), message.getUrlAttached(), message.getEncryptedAESKey());
                                            messageList.add(message);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    sortAndDisplayCurrentChatMessages(messageList, last_message);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.d(TAG, "onCancelled: Canceled." + databaseError.getMessage());
                }
            });

        } catch (NullPointerException e) {
            Toast.makeText(mContext, "Something went wrong!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onDataChange: error: " + e.getMessage());
        }
    }

    private void sortAndDisplayCurrentChatMessages(ArrayList<Message> messageList, TextView last_message) {
        messageList.sort((e1, e2) -> e1.getTimestamp().compareTo(e2.getTimestamp()));

        if (messageList.size() == 0) {
            last_message.setText(R.string.no_messages);
        } else {
            Message getLastMessage = messageList.get(messageList.size() - 1);
            String getLastMessageEncrypted = getLastMessage.getContent();

            last_message.setText(getLastMessageEncrypted);
        }
    }
}
