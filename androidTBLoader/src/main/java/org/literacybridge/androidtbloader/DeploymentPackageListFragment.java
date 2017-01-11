package org.literacybridge.androidtbloader;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;

import org.literacybridge.androidtbloader.content.ContentInfo;
import org.literacybridge.androidtbloader.content.ContentManager;
import org.literacybridge.androidtbloader.util.Config;
import org.literacybridge.androidtbloader.util.PathsProvider;
import org.literacybridge.core.fs.FsFile;
import org.literacybridge.core.fs.OperationLog;
import org.literacybridge.core.fs.TbFile;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.app.Activity.RESULT_OK;

public class DeploymentPackageListFragment extends Fragment {
    private static final String TAG = DeploymentPackageListFragment.class.getSimpleName();

    @SuppressLint("SimpleDateFormat")
    private static final SimpleDateFormat EXPIRATION_DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy");

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mContentInfoRecyclerView;
    private ContentInfoAdapter mAdapter;

    ContentManager mContentManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mContentManager = ((TBLoaderAppContext) getActivity().getApplicationContext()).getContentManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_deployment_package_list, container, false);
        mContentInfoRecyclerView = (RecyclerView) view.findViewById(R.id.deployment_package_recycler_view);
        mContentInfoRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mContentInfoRecyclerView.getRecycledViewPool().setMaxRecycledViews(0, 0);

        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshLayout);

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Refresh items
                mSwipeRefreshLayout.setRefreshing(true);
                updateUI(true);
            }
        });

        return view;
    }


  @Override
    public void onResume() {
        super.onResume();
        updateUI(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_deployment_package_list, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_settings:
                Intent intent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void updateUI(boolean reloadRemotePackages) {
        if (mAdapter == null) {
            mAdapter = new ContentInfoAdapter();
            mContentInfoRecyclerView.setAdapter(mAdapter);
        }

        if (reloadRemotePackages) {
            final OperationLog.Operation op = OperationLog.startOperation("refreshcontentlist");
            mContentManager.refreshContentList(new ContentManager.ContentManagerListener() {

                @Override
                public void contentListChanged() {
                    List<ContentInfo> content = mContentManager.getContentList();
                    int local = 0;
                    int cloud = 0;
                    for (ContentInfo ci : content) {
                        if (ci.getDownloadStatus() == ContentInfo.DownloadStatus.DOWNLOADED) {
                            local++;
                        } else {
                            cloud++;
                        }
                    }
                    op.put("local", local);
                    op.put("cloud", cloud);
                    op.end();
                    mAdapter.setContentInfos(content);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mAdapter.notifyDataSetChanged();
                            mSwipeRefreshLayout.setRefreshing(false);
                        }
                    });
                }
            });
        }

    }

    ContentManager.ContentManagerListener contentManagerListener = new ContentManager.ContentManagerListener() {

        @Override
        public void contentListChanged() {
            mAdapter.setContentInfos(mContentManager.getContentList());
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mAdapter.notifyDataSetChanged();
                    mSwipeRefreshLayout.setRefreshing(false);
                }
            });
        }
    };

    private class ContentInfoHolder extends RecyclerView.ViewHolder {
        // This gets set when the holder is bound.
        private ContentInfo mContentInfo;

        private TextView mProjectTextView;
        private TextView mRevisionTextView;
        private TextView mExpirationTextView;

        private Button mDownloadButton;

        private ProgressBar mProgressBar;


        ContentInfoHolder(View itemView) {
            super(itemView);

            mProjectTextView = (TextView) itemView.findViewById(R.id.list_item_deployment_package_project);
            mRevisionTextView = (TextView) itemView.findViewById(R.id.list_item_deployment_package_rev);
            mExpirationTextView = (TextView) itemView.findViewById(R.id.list_item_deployment_package_expiration);

            mDownloadButton = (Button) itemView.findViewById(R.id.list_item_deployment_package_download_button);
            mDownloadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, String.format("Download clicked: %s", mContentInfo.toString()));
                   mContentManager.startDownload(mContentInfo, mTransferListener);
                }
           });

            mProgressBar = (ProgressBar) itemView.findViewById(R.id.list_item_deployment_package_progress_bar);
        }

        private TransferListener mTransferListener = new TransferListener() {

            private void update(final boolean notify) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setCurrentlyDownloading();
                        if (notify) {
                            mAdapter.notifyDataSetChanged();
                        }
                    }
                });
            }

            @Override
            public void onStateChanged(int id, TransferState state) {
                Log.d(TAG, String.format("Downloading content, state: %s", state.toString()));
                update(true);
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                bytesTotal = Math.max(bytesTotal, 1);
                Log.d(TAG, String.format("Downloading content, progress: %d/%d (%d%%)", bytesCurrent, bytesTotal, 100 * bytesCurrent / bytesTotal));
                update(false);
            }

            @Override
            public void onError(int id, Exception ex) {
                Log.d(TAG, "Downloading content, error: ", ex);
                update(true);
            }
        };


        private void setCurrentlyDownloading() {
            if (mContentInfo.isDownloading()) {
                mDownloadButton.setVisibility(View.INVISIBLE);
                mProgressBar.setVisibility(View.VISIBLE);
                mProgressBar.setProgress(mContentInfo.getProgress());
            } else {
                mDownloadButton.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.INVISIBLE);
            }
        }

        private void bindContentInfo(final ContentInfo contentInfo) {
            mContentInfo = contentInfo;
            mContentInfo.setTransferListener(mTransferListener);

            mProjectTextView.setText(contentInfo.getProjectName()); // strip 'ACM-'
            mRevisionTextView.setText(getString(R.string.deployment_package_revision, mContentInfo.getVersion()));
            final String expiration;
            if (contentInfo.hasExpiration()) {
                expiration = EXPIRATION_DATE_FORMAT.format(contentInfo.getExpiration());
            } else {
                expiration = getString(R.string.deployment_package_expiration_never);
            }
            mExpirationTextView.setText(getString(R.string.deployment_package_expiration, expiration));

            switch (contentInfo.getDownloadStatus()) {
                case NEVER_DOWNLOADED:
                    mDownloadButton.setVisibility(View.VISIBLE);
                    mProgressBar.setVisibility(View.INVISIBLE);
                    mDownloadButton.setEnabled(true);
                    mDownloadButton.setText(getString(R.string.deployment_package_download));
                    break;
                case DOWNLOAD_FAILED:
                    mDownloadButton.setVisibility(View.VISIBLE);
                    mProgressBar.setVisibility(View.INVISIBLE);
                    mDownloadButton.setEnabled(true);
                    mDownloadButton.setText(getString(R.string.deployment_package_download_retry));
                    break;
                case DOWNLOADED:
                    mDownloadButton.setVisibility(View.VISIBLE);
                    mProgressBar.setVisibility(View.INVISIBLE);
                    if (!contentInfo.isUpdateAvailable()) {
                        mDownloadButton.setEnabled(false);
                        mDownloadButton.setText(getString(R.string.deployment_package_download));
                    } else {
                        mDownloadButton.setEnabled(true);
                        mDownloadButton.setText(getString(R.string.deployment_package_download_update));
                    }
                    break;
            }

            setCurrentlyDownloading();
        }
    }

     private class ContentInfoAdapter extends RecyclerView.Adapter<ContentInfoHolder> {
        private volatile List<ContentInfo> mContentInfos;

        private void setContentInfos(List<ContentInfo> deploymentPackages) {
            mContentInfos = deploymentPackages;
        }

        @Override
        public ContentInfoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater.inflate(R.layout.list_item_deployment_package, parent, false);
            return new ContentInfoHolder(view);
        }

        @Override
        public void onBindViewHolder(ContentInfoHolder holder, int position) {
            holder.bindContentInfo(mContentInfos.get(position));
        }

        @Override
        public int getItemCount() {
            return mContentInfos != null ? mContentInfos.size() : 0;
        }
    }


}
