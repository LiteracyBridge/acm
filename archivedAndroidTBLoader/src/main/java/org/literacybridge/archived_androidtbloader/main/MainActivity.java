package org.literacybridge.archived_androidtbloader.main;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import org.literacybridge.archived_androidtbloader.SingleFragmentActivity;
import org.literacybridge.archived_androidtbloader.util.Constants;
import org.literacybridge.core.fs.OperationLog;

/**
 * Created by bill on 12/23/16.
 */

public class MainActivity extends SingleFragmentActivity {
    private static final String TAG = "TBL!:" + MainActivity.class.getSimpleName();

    @Override
    protected Fragment createFragment() {
        Log.d(TAG, String.format("Create fragment"));
        return new MainFragment();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        OperationLog.close();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra("TODO", "exit");
        intent.putExtra(Constants.EXIT_APPLICATION, true);
        setResult(RESULT_OK, intent);
        finish();
    }
}
