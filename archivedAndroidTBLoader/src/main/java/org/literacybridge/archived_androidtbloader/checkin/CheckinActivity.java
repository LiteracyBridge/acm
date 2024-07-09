package org.literacybridge.archived_androidtbloader.checkin;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;

import org.literacybridge.archived_androidtbloader.SingleFragmentActivity;

/**
 * Created by bill on 12/23/16.
 */

public class CheckinActivity extends SingleFragmentActivity {
    private static final String TAG = "TBL!:" + CheckinActivity.class.getSimpleName();

    @Override
    protected Fragment createFragment() {
        Log.d(TAG, "Create checkin fragment");
        return new CheckinFragment();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

}
