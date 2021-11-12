package org.literacybridge.androidtbloader.content;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import android.os.Handler;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import org.literacybridge.androidtbloader.R;
import org.literacybridge.androidtbloader.TBLoaderAppContext;

public class ManageContentActivity extends AppCompatActivity {
    private static final String TAG = "TBL!:" + ManageContentActivity.class.getSimpleName();

    private SwipeRefreshLayout mSwipeRefreshLayout;
    ContentInfoAdapter mAdapter;

    ContentManager mContentManager;
    private Button mCancelButton;
    private RecyclerView mContentInfoRecyclerView;
    private AlertDialog mUserDialog;
    private TextView mNoContentText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_content);

        mContentManager = TBLoaderAppContext.getInstance().getContentManager();
        createView();
    }


    private void createView() {

        // The actionbar has a "title" property that is set from the activity's "label=" property
        // from the AndroidManifest file. Here, we make the toolbar work like an action bar.
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        // The toolbar also *contains* a TextView with an id of main_toolbar_title.
        TextView main_title = (TextView) findViewById(R.id.main_toolbar_title);
        main_title.setText("");

        // We want a "back" button (sometimes called "up"), but we don't want back navigation, but
        // to simply end this activity without setting project or community.
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if (shouldDoBackPressed()) {
                    finish();
                }
            }
        });

        mContentInfoRecyclerView = (RecyclerView) findViewById(
            R.id.deployment_package_recycler_view);
        mContentInfoRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mContentInfoRecyclerView.getRecycledViewPool().setMaxRecycledViews(0, 0);

        mAdapter = new ContentInfoAdapter(this, mContentManager.getContentList());
        mContentInfoRecyclerView.setAdapter(mAdapter);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Don't refresh content list halfway through download -- might see partial data
                // and think it's not current.
                if (isDownloadInProgress()) {
                    mSwipeRefreshLayout.setRefreshing(false);
                } else {
                    mSwipeRefreshLayout.setRefreshing(true);
                    mContentManager.refreshContentList();
                }
            }
        });

        mCancelButton = (Button) findViewById(R.id.manage_content_button_cancel);
        mCancelButton.setOnClickListener(mOnCancelListener);

        mNoContentText = (TextView) findViewById(R.id.manage_content_no_content);
        setMessageVisibility();
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(TBLoaderAppContext.getInstance()).unregisterReceiver(mMessageReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ContentManager.CONTENT_LIST_CHANGED_EVENT);
        LocalBroadcastManager.getInstance(TBLoaderAppContext.getInstance()).registerReceiver(
            mMessageReceiver, filter);
        if (!isDownloadInProgress()) {
            mContentManager.fetchContentList();
        }
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ContentManager.CONTENT_LIST_CHANGED_EVENT)) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setMessageVisibility();
                        mAdapter.notifyDataSetChanged();
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        }
    };

    private void setMessageVisibility() {
        boolean haveContent = mContentManager.getContentList().size() > 0;
        mNoContentText.setVisibility(haveContent ? View.GONE : View.VISIBLE);
        mSwipeRefreshLayout.setVisibility(haveContent ? View.VISIBLE : View.GONE);
    }

    private void closeUserDialog() {
        if (mUserDialog != null) {
            mUserDialog.dismiss();
            mUserDialog = null;
        }
    }
    public void maybeRemoveContent(final ContentInfo mContentInfo) {
        String title = "Really remove content?";
        String body = String.format("Deployment:  %1$s\nProject:  %2$s",
            mContentInfo.getVersionedDeployment(), mContentInfo.getProgramId());
        closeUserDialog();
        final AlertDialog.Builder builder = new AlertDialog.Builder(ManageContentActivity.this);
        builder.setTitle(title)
            .setMessage(body)
            .setNeutralButton("Remove content", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    closeUserDialog();
                    mContentManager.removeLocalContent(mContentInfo);
                }
            })
            .setNegativeButton("Don't remove", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    closeUserDialog();
                }
            });
        mUserDialog = builder.show();
    }


    /**
     * Listener for the cancel button. When clicked, prompt the user for confirmation, then
     * cancel the download or unzipping. The download will continue while the confirmation
     * dialog is open, and if it finishes, the dialog is dismissed.
     */
    private View.OnClickListener mOnCancelListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final ContentInfo currentDownload = mAdapter.getDownloadingItem();
            if (currentDownload != null) {
                String title = "Really stop download?";
                String body = String.format("Deployment:  %1$s\nProject:  %2$s",
                    currentDownload.getVersionedDeployment(), currentDownload.getProgramId());
                closeUserDialog();
                final AlertDialog.Builder builder = new AlertDialog.Builder(ManageContentActivity.this);
                builder.setTitle(title)
                    .setMessage(body)
                    .setNeutralButton("Stop Download", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            closeUserDialog();
                            currentDownload.cancel();
                        }
                    })
                    .setNegativeButton("Continue Download", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            closeUserDialog();
                        }
                    });
                mUserDialog = builder.show();
            }
        }
    };

    void enableCancel(ContentInfoHolder holder) {
        mCancelButton.setVisibility(View.VISIBLE);
        mCancelButton.setText(holder==null?"Cancel Refresh":"Cancel Download");

        if (holder != null) {
            // We need to wait a little while before scrolling, because the view needs to
            // re-layout first, with the cancel button visible.
            final int position = holder.getLayoutPosition();
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mContentInfoRecyclerView.smoothScrollToPosition(position);
                }
            }, 100);
        }
    }

    void disableCancel() {
        mCancelButton.setVisibility(View.GONE);
        closeUserDialog();
    }

    boolean isDownloadInProgress() {
        return mAdapter.isDownloadInProgress();
    }
    private boolean shouldDoBackPressed() {
        return !isDownloadInProgress();
    }

    /**
     * Take care of popping the fragment back stack or finishing the activity
     * as appropriate.
     */
    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed");
        // Defer to the fragment.
        if (!shouldDoBackPressed()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown");
        //replaces the default 'Back' button action
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!shouldDoBackPressed()) {
                Log.d(TAG, "Back Key Ignored");
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

}
