package com.example.prototype1.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.prototype1.R;

public class SettingsAdapter extends ArrayAdapter<String> {

    private String[] settingsList;
    private TextView settingsItem;
    private Context mContext;

    public SettingsAdapter(Context context, String[] settingsList) {
        super(context, R.layout.settings_item, settingsList);
        this.mContext = context;
        this.settingsList = settingsList;
    }

    public int getCount() {
        return this.settingsList.length;
    }

    public String getItem(int index) {
        return this.settingsList[index];
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View row = convertView;
        if (row == null) {
            LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(R.layout.settings_item, parent, false);
        }

        settingsItem = row.findViewById(R.id.settings_item_id);
        String currentOption = settingsList[position];

        settingsItem.setText(currentOption);

        return row;
    }

}
