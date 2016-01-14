package com.hardis.aki.fastwind;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Created by aki on 11.10.2015.
 */
public class UserSettingActivity extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.settings);
    }
}
