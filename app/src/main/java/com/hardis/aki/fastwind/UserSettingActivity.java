package com.hardis.aki.fastwind;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by aki on 11.10.2015.
 */
public class UserSettingActivity extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
    }
}
