package com.example.prototype1.permissions;

import android.Manifest;
import android.app.Activity;

import androidx.core.app.ActivityCompat;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static androidx.core.content.ContextCompat.checkSelfPermission;
import static com.example.prototype1.ENUMS.VARS.DO_READ_EXTERNAL_STORAGE;
import static com.example.prototype1.ENUMS.VARS.DO_WRITE_EXTERNAL_STORAGE;
import static com.example.prototype1.ENUMS.VARS.READ_PHONE_STATE;
import static com.example.prototype1.ENUMS.VARS.REQUEST_CAMERA;

public class MyPermissions {
    private static final String TAG = "MyPermissions";

    private final Activity callingActivity;

    private final static String[] permissions = {Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    public MyPermissions(Activity callingActivity) {
        this.callingActivity = callingActivity;
    }

    public void permissionsCamera() {
        ActivityCompat.requestPermissions(callingActivity, permissions, REQUEST_CAMERA | DO_READ_EXTERNAL_STORAGE | DO_WRITE_EXTERNAL_STORAGE);
    }

    public void permissionsReaState() {
        ActivityCompat.requestPermissions(callingActivity, permissions, READ_PHONE_STATE);
    }

    public static String[] getPermissions() {
        return permissions;
    }

    public boolean isPermissionGranted(int position) {
        return PERMISSION_GRANTED == checkSelfPermission(callingActivity.getApplicationContext(), getPermissions()[position]);
    }
}
