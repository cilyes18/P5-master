package com.example.prototype1.camera;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.FaceDetector;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

import static com.example.prototype1.ENUMS.VARS.REQUEST_CAMERA;

//todo check if can make camera work with face detection then return a boolean
public class MyCamera extends FaceDetector {
    private static final String TAG = "MyCamera";

    private Activity callingActivity;
    @NonNull
    private Context mContext;
    private Uri mUri;

    @Override
    public int findFaces(Bitmap bitmap, Face[] faces) {
        return faces.length;

    }

    public MyCamera(@NonNull Activity activity) {
        super(480, 360, 1);
        this.callingActivity = activity;
        this.mContext = activity.getApplicationContext();
    }


    public void takePicture() {
        String dateStamp = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date(System.currentTimeMillis()));
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Profile Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera");
        values.put(MediaStore.Images.Media.DATE_ADDED, dateStamp); // we cannot use date taken bc it requires higher SDK(29min)
        this.mUri = callingActivity.getApplicationContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(Objects.requireNonNull(mContext).getPackageManager()) != null) {
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, this.mUri);
            callingActivity.startActivityForResult(cameraIntent, REQUEST_CAMERA);
        }

    }


    public Uri getUri() {
        return this.mUri; // remember to point with 'THIS' annotation to implicitly return this variable
    }

    public Bitmap mBitmap(Uri uri) throws IOException {
        Bitmap bitmap = MediaStore.Images.Media.getBitmap(callingActivity.getContentResolver(), uri);
        bitmap.setWidth(480);
        bitmap.setHeight(360);
        return bitmap;

    }


}
