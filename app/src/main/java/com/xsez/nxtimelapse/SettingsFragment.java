package com.xsez.nxtimelapse;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by Xsez on 01.07.2014.
 */
public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }

}
