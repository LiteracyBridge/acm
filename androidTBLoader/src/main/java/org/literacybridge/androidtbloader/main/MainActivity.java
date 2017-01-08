package org.literacybridge.androidtbloader.main;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import org.literacybridge.androidtbloader.SingleFragmentActivity;
import org.literacybridge.androidtbloader.signin.UserHelper;

import java.util.ArrayList;

/**
 * Created by bill on 12/23/16.
 */

public class MainActivity extends SingleFragmentActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected Fragment createFragment() {
        String project = getIntent().getStringExtra("project");
        Log.d(TAG, String.format("Create fragment for project %s", project));
        return new MainFragment();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra("TODO", "exit");
        setResult(RESULT_OK, intent);
        finish();
    }
}
