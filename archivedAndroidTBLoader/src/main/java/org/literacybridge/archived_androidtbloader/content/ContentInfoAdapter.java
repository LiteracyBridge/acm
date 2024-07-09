package org.literacybridge.archived_androidtbloader.content;

import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.literacybridge.archived_androidtbloader.R;

import java.util.List;

/**
 * This is the Adapter for the RecyclerView in the Manage Content page.
 */
class ContentInfoAdapter extends RecyclerView.Adapter<ContentInfoHolder> {
    private ManageContentActivity mManageContentActivity;
    private volatile List<ContentInfo> mContentInfos;

    /**
     * Initialize with the containing Activity, and the list of ContentInfos.
     * @param manageContentActivity containing the RecyclerView
     * @param contentInfos to be shown
     */
    ContentInfoAdapter(
        ManageContentActivity manageContentActivity,
        List<ContentInfo> contentInfos) {
        this.mManageContentActivity = manageContentActivity;
        this.mContentInfos = contentInfos;
    }

    /**
     * True if any of the ContentInfos are in a downloading state. Note that there should be at
     * most one in that state.
     * @return true if any ContentInfo is currently downloading.
     */
    boolean isDownloadInProgress() {
        return getDownloadingItem() != null;
    }

    /**
     * Gets the currently downloading ContentInfo, if any.
     * @return the downloading ContentInfo, or null if no download in progress.
     */
    ContentInfo getDownloadingItem() {
        for (ContentInfo info: mContentInfos) {
            if (info.isDownloading())
                return info;
        }
        return null;
    }

    @Override
    public ContentInfoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(mManageContentActivity);
        View view = layoutInflater.inflate(R.layout.list_item_deployment_package_2, parent, false);
        return new ContentInfoHolder(mManageContentActivity, view);
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
