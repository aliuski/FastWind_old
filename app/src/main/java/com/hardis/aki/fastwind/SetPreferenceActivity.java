package com.hardis.aki.fastwind;

import android.app.Activity;
import android.os.Bundle;
/**
 * Created by aki on 17.2.2018.
 */

public class SetPreferenceActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new UserSettingActivity()).commit();
    }
}
