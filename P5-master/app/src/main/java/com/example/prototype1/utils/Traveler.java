package com.example.prototype1.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import static com.example.prototype1.main.MainActivity.popBackStack;

public class Traveler {
    private static final String TAG = "Traveler";

    private static final Intent mIntent = new Intent();

    public Traveler() {
    }

    public void goToWithFlags(@NonNull Context current_activity, Class<? extends Activity> destinationClass) {
        mIntent.setClass(current_activity, destinationClass);
        mIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        current_activity.startActivity(mIntent);
    }

    public void goFragment(@NonNull FragmentManager fragmentManager, Fragment fragment, @NonNull int id) {
        Fragment fragmentToUse = fragmentManager.findFragmentById(id);
        if (fragmentToUse == null) {
            fragmentToUse = fragment;
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.addToBackStack(String.valueOf(fragmentToUse));
            fragmentTransaction.add(id, fragmentToUse).commit();
        }
    }

    public void goToFragmentWithArguments(@NonNull FragmentManager fragmentManager, Fragment fragment,
                                          @NonNull int id, @NonNull Bundle bundle) {
        Fragment fragmentToUse = fragmentManager.findFragmentById(id);
        if (fragmentToUse == null) {
            fragmentToUse = fragment;
            fragmentToUse.setArguments(bundle);
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction
                    .add(id, fragmentToUse)
                    .addToBackStack(String.valueOf(fragmentToUse))
                    .commit();
        } else {
            fragmentToUse = fragment;
            fragmentToUse.setArguments(bundle);
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction
                    .replace(id, fragmentToUse)
                    .addToBackStack(String.valueOf(fragmentToUse))
                    .commit();
        }
    }


    public void goToWithout(@NonNull Context current_activity, @NonNull Class<? extends Activity> destinationClass) {
        mIntent.setClass(current_activity, destinationClass);
        current_activity.startActivity(mIntent);
    }

    public void removeFromStack(FragmentManager fragmentManager) {
        fragmentManager.popBackStack();
        fragmentManager.beginTransaction().commit();
    }

}
