package org.literacybridge.androidtbloader.community;

import android.app.Activity;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.literacybridge.androidtbloader.R;

import java.util.List;

/**
 * Reusable adapter to display community info.
 */
public class CommunityInfoAdapter extends RecyclerView.Adapter<CommunityInfoHolder> {
    public interface CommunityInfoAdapterListener {
        // Called when a community is clicked in the list view.
        void onCommunityClicked(CommunityInfo communityInfo);
    }
    private CommunityInfoAdapterListener mListener;
    private Activity mActivity;
    private List<CommunityInfo> mList;

    public CommunityInfoAdapter(Activity activity, List<CommunityInfo> list) {
        this(activity, list, null);
    }

    public CommunityInfoAdapter(Activity activity,
            List<CommunityInfo> list, CommunityInfoAdapterListener listener
    ) {
        mListener = listener;
        mActivity = activity;
        mList = list;
    }

    @Override
    public CommunityInfoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(mActivity);
        View view = layoutInflater.inflate(R.layout.list_item_community, parent, false);
        return new CommunityInfoHolder(view, this);
    }

    @Override
    public void onBindViewHolder(CommunityInfoHolder holder, int position) {
        holder.bindCommunityInfo(mList.get(position));
    }

    @Override
    public int getItemCount() {
        return mList != null ? mList.size() : 0;
    }

    void onCommunityClicked(CommunityInfo community) {
        if (mListener != null) {
            mListener.onCommunityClicked(community);
        }
    }
}
