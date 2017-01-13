package org.literacybridge.androidtbloader.content;

import android.app.Fragment;
import android.os.Bundle;

import org.literacybridge.androidtbloader.SingleFragmentActivity;

public class ManageContentActivity extends SingleFragmentActivity {
    private static final String TAG = ManageContentActivity.class.getSimpleName();

    @Override
    protected Fragment createFragment() {
        return new ManageContentFragment();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
