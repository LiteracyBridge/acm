package org.literacybridge.androidtbloader;

import android.app.Fragment;
import android.os.Bundle;

public class MainActivity extends SingleFragmentActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected Fragment createFragment() {
        return new DeploymentPackageListFragment();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
