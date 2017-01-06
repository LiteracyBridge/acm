package org.literacybridge.androidtbloader;

import android.app.Fragment;
import android.os.Bundle;

public class OldMainActivity extends SingleFragmentActivity {
    private static final String TAG = OldMainActivity.class.getSimpleName();

    @Override
    protected Fragment createFragment() {
        return new DeploymentPackageListFragment();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
