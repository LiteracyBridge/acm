package org.literacybridge.androidtbloader.dropbox;

import android.app.Fragment;

import org.literacybridge.androidtbloader.SingleFragmentActivity;

public class DropboxAccountPickerActivity extends SingleFragmentActivity {
    @Override
    protected Fragment createFragment() {
        return new DropboxAccountPickerFragment();
    }
}
