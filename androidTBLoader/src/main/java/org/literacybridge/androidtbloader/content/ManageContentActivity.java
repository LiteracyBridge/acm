package org.literacybridge.androidtbloader.content;

import android.app.Fragment;
import android.os.Bundle;

import android.util.Log;
import android.view.KeyEvent;
import org.literacybridge.androidtbloader.SingleFragmentActivity;

public class ManageContentActivity extends SingleFragmentActivity {
    private static final String TAG = "TBL!:" + ManageContentActivity.class.getSimpleName();
    private ManageContentFragment mManageContentFragment;

    @Override
    protected Fragment createFragment() {
        return new ManageContentFragment();
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
        if (mManageContentFragment != null && !mManageContentFragment.shouldDoBackPressed()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown");
        //replaces the default 'Back' button action
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mManageContentFragment == null || mManageContentFragment.shouldDoBackPressed()) {
                return false;
            }
        }
        return true;
    }

}
