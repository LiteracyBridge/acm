package org.literacybridge.androidtbloader;

import android.app.Fragment;

public class SettingsActivity extends SingleFragmentActivity {
    @Override
    protected Fragment createFragment() {
        return new SettingsFragment();
    }
}
