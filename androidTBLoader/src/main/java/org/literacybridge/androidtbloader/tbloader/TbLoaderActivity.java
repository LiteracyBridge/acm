package org.literacybridge.androidtbloader.tbloader;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;

import android.view.KeyEvent;
import org.literacybridge.androidtbloader.SingleFragmentActivity;
import org.literacybridge.androidtbloader.util.Constants;

/**
 * Created by bill on 12/20/16. Many people like single-fragment activities, because
 * fragments may be more flexible and reusable. Maybe, maybe not, in this case, but
 * it costs essentially nothing to do it this way.
 */

public class TbLoaderActivity extends SingleFragmentActivity {
    private static final String TAG = "TBL!:" + TbLoaderActivity.class.getSimpleName();
    private TbLoaderFragment mTbLoaderFragment;

    @Override
    protected Fragment createFragment() {
        String project = getIntent().getStringExtra(Constants.PROJECT);
        Log.d(TAG, String.format("Create fragment for project %s", project));
        mTbLoaderFragment = new TbLoaderFragment();
        return mTbLoaderFragment;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Take care of popping the fragment back stack or finishing the activity
     * as appropriate.
     */
    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed");
        // Defer to the fragment.
        if (mTbLoaderFragment != null && !mTbLoaderFragment.shouldDoBackPressed()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown");
        //replaces the default 'Back' button action
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mTbLoaderFragment != null && !mTbLoaderFragment.shouldDoBackPressed()) {
                Log.d(TAG, "Back Key Ignored");
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
