package com.example.innometrics.fragments;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;

import com.example.innometrics.R;

/**
 * Standard fragment for settings
 */
public class SettingsFragment extends PreferenceFragment{
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //it is all we have to do. PreferenceFragment will change default preferences for us.
        addPreferencesFromResource(R.xml.preferences);
    }
}
