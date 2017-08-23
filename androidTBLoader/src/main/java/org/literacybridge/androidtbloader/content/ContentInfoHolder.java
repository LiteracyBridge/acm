package org.literacybridge.androidtbloader.content;

import android.annotation.SuppressLint;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import org.literacybridge.androidtbloader.BuildConfig;
import org.literacybridge.androidtbloader.R;
import org.literacybridge.androidtbloader.TBLoaderAppContext;
import org.literacybridge.androidtbloader.util.Util;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * This class is the ViewHolder for the RecyclerView of the Manage Content page.
 * <p>
 * It also (and mainly) implements the logic of displaying one item in the Manage Content page.
 * <p>
 * It contains the "Download" button for items, and tells the ManageContentActivity to show and
 * hide the "Cancel Download" button as downloads begin and end.
 */
class ContentInfoHolder extends RecyclerView.ViewHolder {
    private static final String TAG = "TBL!:" + ContentInfoHolder.class.getSimpleName();

    @SuppressLint("SimpleDateFormat")
    private static final SimpleDateFormat EXPIRATION_DATE_FORMAT = new SimpleDateFormat(
        "dd.MM.yyyy", Locale.US);

    private ManageContentActivity mManageContentActivity;
    // This gets set when the holder is bound.
    private ContentInfo mContentInfo;

    private TextView mProjectTextView;
    private TextView mVersionTextView;
    private TextView mExpirationTextView;

    private Button mDownloadButton;
    private final TextView mStatusLabel;
    private ProgressBar mProgressBar;

    ContentInfoHolder(ManageContentActivity manageContentAdapter, View itemView) {
        super(itemView);
        this.mManageContentActivity = manageContentAdapter;

        mProjectTextView = (TextView) itemView.findViewById(R.id.list_item_deployment_project);
        mVersionTextView = (TextView) itemView.findViewById(R.id.list_item_deployment_package_version);
        mExpirationTextView = (TextView) itemView.findViewById(
            R.id.list_item_deployment_expiration);

        mDownloadButton = (Button) itemView.findViewById(R.id.list_item_deployment_download_button);
        mDownloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, String.format("Download clicked: item %d, %s", getLayoutPosition(),
                    mContentInfo.toString()));
                if (mManageContentActivity.isDownloadInProgress()) {
                    Log.e(TAG, "Can't start download with download already in progress");
                    return;
                }
                mDownloadButton.setVisibility(View.VISIBLE);
                mManageContentActivity.enableCancel(ContentInfoHolder.this);
                mManageContentActivity.mContentManager.startDownload(mContentInfo,
                    mTransferListener);
            }
        });

        mStatusLabel = (TextView) itemView.findViewById(R.id.list_item_deployment_status_label);
        mProgressBar = (ProgressBar) itemView.findViewById(R.id.list_item_deployment_progress_bar);

        // Developer testing code -- long press to delete local content.
        if (TBLoaderAppContext.getInstance().getConfig().isAdvanced()) {
            itemView.setLongClickable(true);
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mManageContentActivity.mContentManager.removeLocalContent(mContentInfo);
                    return true;
                }
            });
        }
    }

    /**
     * Convenience function.
     *
     * @param resId The desired string's id.
     * @return The string.
     */
    private String getString(int resId) {
        return TBLoaderAppContext.getInstance().getString(resId);
    }

    private String getString(int resId, Object... formatArgs) {
        return TBLoaderAppContext.getInstance().getString(resId, formatArgs);
    }

    /**
     * Listener for the progress of downloads.
     */
    private ContentDownloader.DownloadListener mTransferListener = new ContentDownloader.DownloadListener() {
        long prevProgress = 0;

        private void update(final boolean notify) {
            mManageContentActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateView();
                    if (notify) {
                        mManageContentActivity.mAdapter.notifyDataSetChanged();
                    }
                    // This can only happen in the download listener. We only allow one download at a time,
                    // so, if this code runs, it is/was the active download. If not longer an active
                    // download, don't need the cancel button any more.
                    if (!mContentInfo.isDownloading()) {
                        mManageContentActivity.disableCancel();
                    }
                }
            });
        }

        @Override
        public void onUnzipProgress(int id, long current, long total) {
            if (BuildConfig.DEBUG) {
                total = Math.max(total, 1);
                long progress = 100 * current / total;
                if (progress != prevProgress)
                    Log.d(TAG,
                        String.format("Unzipping content, progress: %d/%d (%d%%)", current, total,
                            progress));
                prevProgress = progress;
            }
            update(false);
        }

        @Override
        public void onStateChanged(int id, TransferState state) {
            Log.d(
                TAG, String.format("Downloading content, state: %s", state.toString()));
            update(true);
        }

        @Override
        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
            if (BuildConfig.DEBUG) {
                bytesTotal = Math.max(bytesTotal, 1);
                long progress = 100 * bytesCurrent / bytesTotal;
                if (progress != prevProgress)
                    Log.d(TAG,
                        String.format("Downloading content, progress: %d/%d (%d%%)", bytesCurrent,
                            bytesTotal, progress));
                prevProgress = progress;
            }
            update(false);
        }

        @Override
        public void onError(int id, Exception ex) {
            Log.d(TAG, "Downloading content, error: ", ex);
            update(true);
        }
    };

    private void updateView() {
        switch (mContentInfo.getDownloadStatus()) {
        case NEVER_DOWNLOADED:
            mStatusLabel.setVisibility(View.INVISIBLE);
            mDownloadButton.setVisibility(View.VISIBLE);
            mDownloadButton.setEnabled(!mManageContentActivity.isDownloadInProgress());
            mDownloadButton.setText(
                getString(R.string.deployment_package_download));
            mProgressBar.setVisibility(View.INVISIBLE);
            mExpirationTextView.setVisibility(View.VISIBLE);
            break;
        case DOWNLOAD_FAILED:
            mStatusLabel.setVisibility(View.INVISIBLE);
            mDownloadButton.setVisibility(View.VISIBLE);
            mDownloadButton.setEnabled(!mManageContentActivity.isDownloadInProgress());
            mDownloadButton.setText(
                getString(R.string.deployment_package_download_retry));
            mProgressBar.setVisibility(View.INVISIBLE);
            mExpirationTextView.setVisibility(View.VISIBLE);
            break;
        case DOWNLOADED:
            if (mContentInfo.isUpdateAvailable()) {
                // Like 'NEVER_DOWNLOADED'.
                mStatusLabel.setVisibility(View.INVISIBLE);
                mDownloadButton.setVisibility(View.VISIBLE);
                mDownloadButton.setEnabled(!mManageContentActivity.isDownloadInProgress());
                mDownloadButton.setText(
                    getString(R.string.deployment_package_download_update));
                mExpirationTextView.setVisibility(View.VISIBLE);
            } else {
                mStatusLabel.setText(
                    getString(R.string.deployment_ready_to_use));
                mStatusLabel.setVisibility(View.VISIBLE);
                mDownloadButton.setEnabled(false);
                mDownloadButton.setVisibility(View.INVISIBLE);
                mExpirationTextView.setVisibility(View.GONE);
            }
            mProgressBar.setVisibility(View.INVISIBLE);
            break;
        case DOWNLOADING:
            mDownloadButton.setVisibility(View.INVISIBLE);
            mStatusLabel.setText(
                getString(R.string.list_item_deployment_downloading));
            mStatusLabel.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.VISIBLE);
            mProgressBar.setProgress(mContentInfo.getProgress());
            mExpirationTextView.setVisibility(View.VISIBLE);
            break;
        case PROCESSING:
            mDownloadButton.setVisibility(View.INVISIBLE);
            mStatusLabel.setText(
                getString(R.string.list_item_deployment_unzipping));
            mStatusLabel.setVisibility(View.VISIBLE);
            mProgressBar.setProgress(mContentInfo.getProgress());
            mProgressBar.setVisibility(View.VISIBLE);
            mExpirationTextView.setVisibility(View.VISIBLE);
            break;
        case WAITING:
            mDownloadButton.setVisibility(View.INVISIBLE);
            mStatusLabel.setText(
                getString(R.string.list_item_deployment_waiting));
            mStatusLabel.setVisibility(View.VISIBLE);
            mProgressBar.setProgress(mContentInfo.getProgress());
            mProgressBar.setVisibility(View.VISIBLE);
            mExpirationTextView.setVisibility(View.VISIBLE);
            break;
        }
    }

    /**
     * Bind this holder and its view to a different contentInfo. Updates view appropriately.
     * @param contentInfo to bind.
     */
    void bindContentInfo(final ContentInfo contentInfo) {
        mContentInfo = contentInfo;
        mContentInfo.setTransferListener(mTransferListener);

        // Update the parts of the view that never change.
        mProjectTextView.setText(contentInfo.getProjectName());
        mVersionTextView.setText(
            getString(R.string.deployment_version, mContentInfo.getVersion()));
//        final String expiration;
//        if (contentInfo.hasExpiration()) {
//            expiration = EXPIRATION_DATE_FORMAT.format(contentInfo.getExpiration());
//        } else {
//            expiration = getString(R.string.deployment_package_expiration_never);
//        }
//        mExpirationTextView.setText(
//            getString(R.string.deployment_package_expiration, expiration));
        final String size = Util.getBytesString(contentInfo.getSize());
        mExpirationTextView.setText(getString(R.string.deployment_size, size));

        // Update the dynamic parts of the view.
        updateView();
    }
}
