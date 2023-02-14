package com.example.prototype1.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;

public class User implements Parcelable {

    private String userId,
            name,
            email,
            phone,
            profileUrl;
    private boolean isCheckedForChatCreation;

    public User() {
    }

    public User(String userId, String name, String email, String phone, String profileUrl, boolean isCheckedForChatCreation) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.profileUrl = profileUrl;
        this.isCheckedForChatCreation = isCheckedForChatCreation;
    }

    protected User(Parcel in) {
        userId = in.readString();
        name = in.readString();
        email = in.readString();
        phone = in.readString();
        profileUrl = in.readString();
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    @NotNull
    public String getUserId() {
        return userId;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public String getEmail() {
        return email;
    }

    @NotNull
    public String getPhone() {
        return phone;
    }

    @NotNull
    public String getProfileUrl() {
        return profileUrl;
    }

    @NotNull
    public boolean isCheckedForChatCreation() {
        return isCheckedForChatCreation;
    }

    @NotNull
    public void setCheckedForChatCreation(boolean checkedForChatCreation) {
        isCheckedForChatCreation = checkedForChatCreation;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(userId);
        dest.writeString(name);
        dest.writeString(email);
        dest.writeString(phone);
        dest.writeString(profileUrl);
    }

    @NonNull
    @Override
    public String toString() {

        String builder = "[ " +
                "ID: " + getUserId() + ", " +
                "Email: " + getEmail() + ", " +
                "Phone: " + getPhone() + ", " +
                "Photo_URL: " + getProfileUrl() + ", " +
                " ]\n\n";
        return builder;
    }
}
