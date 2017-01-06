package org.literacybridge.androidtbloader.installer;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;

import org.literacybridge.androidtbloader.SingleFragmentActivity;

/**
 * Created by bill on 12/20/16.
 */

public class UpdateActivity extends SingleFragmentActivity {
    private static final String TAG = UpdateActivity.class.getSimpleName();

    @Override
    protected Fragment createFragment() {
        String project = getIntent().getStringExtra("project");
        Log.d(TAG, String.format("Create fragment for project %s", project));
        return new UpdateFragment();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
