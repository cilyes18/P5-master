package com.example.prototype1.utils;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.prototype1.R;
import com.example.prototype1.model.Message;

import java.util.ArrayList;
import java.util.List;

public class MessageListAdapter extends ArrayAdapter<Message> {

    private List<Message> messageList;
    private RelativeLayout singleMessageContainer;
    private TextView incomingMessage, replyOrForward;
    private Context mContext;
    private String userId;

    public MessageListAdapter(Context context, ArrayList<Message> messageList, String userId) {
        super(context, R.layout.text_bubble, messageList);
        this.mContext = context;
        this.messageList = messageList;
        this.userId = userId;
    }

    public int getCount() {
        return this.messageList.size();
    }

    public Message getItem(int index) {
        return this.messageList.get(index);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View row = convertView;
        if (row == null) {
            LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(R.layout.text_bubble, parent, false);
        }

        singleMessageContainer = row.findViewById(R.id.singleMessageContainer);

        Message currentMessage = messageList.get(position);

        incomingMessage = row.findViewById(R.id.singleMessage);
        incomingMessage.setText(currentMessage.getContent());
        boolean inOrNot = currentMessage.isIncoming(currentMessage.getSenderId(), userId);
        incomingMessage.setBackgroundResource(inOrNot
                ? R.drawable.shape_bg_outgoing_bubble : R.drawable.shape_bg_incoming_bubble);

        if (currentMessage.isReplyOrForward()) {
            replyOrForward = row.findViewById(R.id.replyOrForward);
            replyOrForward.setText(currentMessage.getContent());
            replyOrForward.setBackgroundResource(inOrNot
                    ? R.drawable.reply_mess : R.drawable.reply_mess);
            replyOrForward.setVisibility(View.VISIBLE);
        }

        singleMessageContainer.setGravity(inOrNot ? Gravity.END : Gravity.START);

        return row;
    }

}
