package org.literacybridge.androidtbloader.main;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;

import org.literacybridge.androidtbloader.SingleFragmentActivity;

/**
 * Created by bill on 12/23/16.
 */

public class MainActivity extends SingleFragmentActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected Fragment createFragment() {
        String project = getIntent().getStringExtra("project");
        Log.d(TAG, String.format("Create fragment for project %s", project));
        return new MainFragment();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

}
