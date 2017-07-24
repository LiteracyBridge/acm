package org.literacybridge.androidtbloader.uploader;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import org.literacybridge.androidtbloader.SingleFragmentActivity;
import org.literacybridge.androidtbloader.tbloader.TbLoaderFragment;

/**
 * Created by bill on 12/20/16. Many people like single-fragment activities, because
 * fragments may be more flexible and reusable. Maybe, maybe not, in this case, but
 * it costs essentially nothing to do it this way.
 */

public class UploadStatusActivity extends SingleFragmentActivity {
    private static final String TAG = "TBL!:" + UploadStatusActivity.class.getSimpleName();

    @Override
    protected Fragment createFragment() {
        String userid = getIntent().getStringExtra("userid");
        Log.d(TAG, String.format("Create fragment for user %s", userid));
        return new UploadStatusFragment();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
