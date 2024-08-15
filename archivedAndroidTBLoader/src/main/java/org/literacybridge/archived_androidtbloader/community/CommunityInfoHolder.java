package org.literacybridge.archived_androidtbloader.community;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import org.literacybridge.archived_androidtbloader.R;
import org.literacybridge.archived_androidtbloader.checkin.LocationProvider;

/**
 * Reusable adapter to display Community Info.
 */

public class CommunityInfoHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    private CommunityInfoAdapter mAdapter;

    // This gets set every time the holder is bound.
    private CommunityInfo mCommunityInfo;

    // These persist                 
    private TextView mCommunityTextView;
    private TextView mProjectTextView;
    private TextView mDistanceTextView;

    CommunityInfoHolder(View itemView, CommunityInfoAdapter adapter) {
        super(itemView);

        mCommunityTextView = (TextView) itemView.findViewById(R.id.list_item_community_name);
        mProjectTextView = (TextView) itemView.findViewById(R.id.list_item_project_name);
        mDistanceTextView = (TextView) itemView.findViewById(R.id.list_item_distance);

        mAdapter = adapter;
        itemView.setOnClickListener(this);
    }

    void bindCommunityInfo(final CommunityInfo communityInfo) {
        mCommunityInfo = communityInfo;

        mCommunityTextView.setText(communityInfo.getName());
        mProjectTextView.setText(communityInfo.getProject());
        LocationProvider.getDistanceString(communityInfo);
        mDistanceTextView.setText(LocationProvider.getDistanceString(communityInfo));
    }

    @Override
    public void onClick(View v) {
        mAdapter.onCommunityClicked(mCommunityInfo);
    }
}
