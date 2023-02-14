package com.example.prototype1.auth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.prototype1.login.LoginActivity;
import com.example.prototype1.utils.Hashing;
import com.example.prototype1.utils.Traveler;
import com.google.android.gms.internal.firebase_auth.zzff;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FederatedAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseUserMetadata;
import com.google.firebase.auth.MultiFactor;
import com.google.firebase.auth.MultiFactorInfo;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.util.List;

@SuppressLint("ParcelCreator")
public class MyFirebaseUser extends FirebaseUser {
    private static final String TAG = "UserSingleton";
    private Context mContext;
    private FirebaseAuth mAuth;
    private Traveler mTraveler = new Traveler();
    private Hashing hash = new Hashing();
    private FirebaseUser current_F_user;

    public MyFirebaseUser(Context context, FirebaseAuth auth) {
        super();
        mContext = context;
        mAuth = auth;
        current_F_user = auth.getCurrentUser();

    }

    public FirebaseUser getCurrent_F_user() {
        return mAuth.getCurrentUser();
    }

    @NonNull
    @Override
    public Task<Void> reauthenticate(@NonNull AuthCredential authCredential) {
        boolean isok = current_F_user != null && current_F_user.isEmailVerified() && !current_F_user.isAnonymous();
        if (isok && authCredential.getProvider().equals("password")) {
            return current_F_user.reauthenticate(authCredential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "reauthenticate: success: " + task.isSuccessful());
                }
            }).addOnFailureListener(e -> {
                Log.d(TAG, "reauthenticate: error: " + e.getMessage());
            });
        } else
            return null;
    }

    @NonNull
    @Override
    public Task<AuthResult> reauthenticateAndRetrieveData(@NonNull AuthCredential authCredential) {
        return null;
    }

    @NonNull
    @Override
    public Task<AuthResult> startActivityForReauthenticateWithProvider(@NonNull Activity activity, @NonNull FederatedAuthProvider federatedAuthProvider) {
        return null;
    }

    @NonNull
    @Override
    public Task<AuthResult> linkWithCredential(@NonNull AuthCredential authCredential) {
        return null;
    }

    @NonNull
    @Override
    public Task<AuthResult> startActivityForLinkWithProvider(@NonNull Activity activity, @NonNull FederatedAuthProvider federatedAuthProvider) {
        return null;
    }

    @NonNull
    @Override
    public Task<AuthResult> unlink(@NonNull String s) {
        return null;
    }

    @NonNull
    @Override
    public Task<Void> updateProfile(@NonNull UserProfileChangeRequest userProfileChangeRequest) {
        return null;
    }

    @NonNull
    @Override
    public Task<Void> updateEmail(@NonNull String s) {
        return null;
    }

    @NonNull
    @Override
    public Task<Void> updatePhoneNumber(@NonNull PhoneAuthCredential phoneAuthCredential) {
        return null;
    }

    @NonNull
    @Override
    public Task<Void> updatePassword(@NonNull String s) {
        boolean hasDigit = hash.hasDigits(s),
                hasSpecChar = hash.hasSpecial(s),
                hasNum = hash.hasDigits(s),
                longEnough = hash.isLongEnough(s);
        Task<Void> updatePassTask = current_F_user.updatePassword(s);
        if (longEnough && hasDigit && hasSpecChar && hasNum) {

            return updatePassTask.addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(mContext, "Pass updated Successfully", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> {
                Log.d(TAG, "updatePassword: error: " + e.getMessage());
                Toast.makeText(mContext, "Pass update error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        } else
            return null;
    }


    @NonNull
    @Override
    public Task<Void> delete() {

        if (mAuth != null && mAuth.getCurrentUser() != null) {
            Task<Void> deleteUserTask = current_F_user.delete().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    mAuth.signOut();
                    mTraveler.goToWithFlags(mContext, LoginActivity.class);

                }
            }).addOnFailureListener(e -> {
                Log.d(TAG, "delete user error: " + e.getMessage());
            });

            return deleteUserTask;
        } else return null;
    }

    @NonNull
    @Override
    public Task<Void> sendEmailVerification() {
        if (mContext != null && mAuth != null) {
            return current_F_user.sendEmailVerification().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(mContext, "We sent you an email verification, check your inbox and click on th link to verify", Toast.LENGTH_SHORT).show();
                    mAuth.signOut();
                    Log.d(TAG, "sendEmailVerification: ");
                }
                mAuth.signOut();

            }).addOnFailureListener(e -> {
                Toast.makeText(mContext, "Email not sent! " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.d(TAG, "verifyUser: Auth_errror: " + e.getMessage());
                mAuth.signOut();

            });
        }
        return null;

    }

    @NonNull
    @Override
    public Task<Void> sendEmailVerification(@NonNull ActionCodeSettings actionCodeSettings) {
        return super.sendEmailVerification(actionCodeSettings);
    }

    @NonNull
    @Override
    public Task<Void> verifyBeforeUpdateEmail(@NonNull String s) {
        return super.verifyBeforeUpdateEmail(s);
    }

    @NonNull
    @Override
    public Task<Void> verifyBeforeUpdateEmail(@NonNull String s, @Nullable ActionCodeSettings actionCodeSettings) {
        return super.verifyBeforeUpdateEmail(s, actionCodeSettings);
    }


    @NonNull
    @Override
    public String getUid() {

        return current_F_user.getUid();
    }

    @NonNull
    @Override
    public String getProviderId() {
        return current_F_user.getProviderId();
    }

    @Override
    public boolean isAnonymous() {
        return current_F_user.isAnonymous();
    }

    @Nullable
    @Override
    public List<String> zza() {
        return current_F_user.zza();
    }

    @NonNull
    @Override
    public List<? extends UserInfo> getProviderData() {

        return null;
    }

    @NonNull
    @Override
    public FirebaseUser zza(@NonNull List<? extends UserInfo> list) {
        return null;
    }

    @Override
    public FirebaseUser zzb() {
        return null;
    }

    @NonNull
    @Override
    public FirebaseApp zzc() {
        return null;
    }

    @Nullable
    @Override
    public String getDisplayName() {
        return current_F_user.getDisplayName();
    }

    @Nullable
    @Override
    public Uri getPhotoUrl() {
        return current_F_user.getPhotoUrl();
    }

    @Nullable
    @Override
    public String getEmail() {
        return current_F_user.getEmail();
    }

    @Nullable
    @Override
    public String getPhoneNumber() {
        return current_F_user.getPhoneNumber();
    }

    @Override
    public boolean isEmailVerified() {
        return current_F_user.isEmailVerified();
    }

    @Nullable
    @Override
    public String getTenantId() {
//        return mUser.getTenantId();
        return null;
    }

    @NonNull
    @Override
    public zzff zzd() {
        return null;
    }

    @Override
    public void zza(@NonNull zzff zzff) {
    }

    @NonNull
    @Override
    public String zze() {
        return null;
    }

    @NonNull
    @Override
    public String zzf() {
        return null;
    }

    @Nullable
    @Override
    public FirebaseUserMetadata getMetadata() {
        return current_F_user.getMetadata();
    }

    @NonNull
    @Override
    public MultiFactor getMultiFactor() {
        return null;
    }

    @Override
    public void zzb(List<MultiFactorInfo> list) {

    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }

}