package org.literacybridge.androidtbloader.tbloader;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;

import org.literacybridge.androidtbloader.SingleFragmentActivity;

/**
 * Created by bill on 12/20/16. Many people like single-fragment activities, because
 * fragments may be more flexible and reusable. Maybe, maybe not, in this case, but
 * it costs essentially nothing to do it this way.
 */

public class TbLoaderActivity extends SingleFragmentActivity {
    private static final String TAG = TbLoaderActivity.class.getSimpleName();

    @Override
    protected Fragment createFragment() {
        String project = getIntent().getStringExtra("project");
        Log.d(TAG, String.format("Create fragment for project %s", project));
        return new TbLoaderFragment();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
