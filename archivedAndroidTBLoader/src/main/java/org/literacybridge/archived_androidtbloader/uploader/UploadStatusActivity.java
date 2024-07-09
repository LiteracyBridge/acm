package org.literacybridge.archived_androidtbloader.uploader;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import org.literacybridge.archived_androidtbloader.SingleFragmentActivity;
import org.literacybridge.archived_androidtbloader.util.Constants;

/**
 * Created by bill on 12/20/16. Many people like single-fragment activities, because
 * fragments may be more flexible and reusable. Maybe, maybe not, in this case, but
 * it costs essentially nothing to do it this way.
 */

public class UploadStatusActivity extends SingleFragmentActivity {
    private static final String TAG = "TBL!:" + UploadStatusActivity.class.getSimpleName();

    @Override
    protected Fragment createFragment() {
        String useremail = getIntent().getStringExtra(Constants.USEREMAIL);
        Log.d(TAG, String.format("Create upload status fragment for user %s", useremail));
        return new UploadStatusFragment();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
