package org.literacybridge.androidtbloader.recipient;

import android.app.Fragment;
import org.literacybridge.androidtbloader.SingleFragmentActivity;

/**
 * Created by bill on 12/22/16.
 */

public class RecipientChooserActivity extends SingleFragmentActivity {
    private static final String TAG = "TBL!:" + RecipientChooserActivity.class.getSimpleName();

    @Override
    protected Fragment createFragment() {
        return new RecipientChooserFragment();
    }
}
