package org.literacybridge.androidtbloader.community;

import android.app.Fragment;

import org.literacybridge.androidtbloader.SingleFragmentActivity;

/**
 * Created by bill on 12/22/16.
 */

public class ChooseCommunityActivity extends SingleFragmentActivity {
    private static final String TAG = "TBL!:" + ChooseCommunityActivity.class.getSimpleName();

    @Override
    protected Fragment createFragment() {
        return new ChooseCommunityFragment();
    }
}
