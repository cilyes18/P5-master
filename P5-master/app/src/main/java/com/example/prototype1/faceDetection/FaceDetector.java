package com.example.prototype1.faceDetection;


import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

public class FaceDetector extends android.media.FaceDetector implements ImageAnalysis.Analyzer {
    private static final String TAG = "FaceDetector";

    @Override
    public int findFaces(Bitmap bitmap, Face[] faces) {
        return faces.length;
    }

    /**
     * Creates a FaceDetector, configured with the size of the images to
     * be analysed and the maximum number of faces that can be detected.
     * These parameters cannot be changed once the object is constructed.
     * Note that the width of the image must be even.
     *
     * @param width    the width of the image
     * @param height   the height of the image
     * @param maxFaces the maximum number of faces to identify
     */
    public FaceDetector(int width, int height, int maxFaces) {
        super(width, height, 1);
    }

    @Override
    public void analyze(@NonNull ImageProxy image) {
    }
}
