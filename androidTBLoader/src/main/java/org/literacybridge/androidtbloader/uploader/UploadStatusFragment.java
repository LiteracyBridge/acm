package org.literacybridge.androidtbloader.uploader;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import org.literacybridge.androidtbloader.R;
import org.literacybridge.androidtbloader.TBLoaderAppContext;
import org.literacybridge.androidtbloader.signin.UserHelper;
import org.literacybridge.androidtbloader.util.Constants;
import org.literacybridge.core.tbloader.TBLoaderUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import static android.app.Activity.RESULT_OK;
import static org.literacybridge.androidtbloader.util.Constants.UTC;

/**
 * This implements the UI of the Loader portion of the application.
 */
public class UploadStatusFragment extends Fragment {
    private static final String TAG = "TBL!:" + UploadStatusFragment.class.getSimpleName();

    private final int REQUEST_CODE = 101;

    private static final TBLoaderAppContext context = TBLoaderAppContext.getInstance();

    private int successColor = ContextCompat.getColor(context, R.color.success);
    private int alertColor = ContextCompat.getColor(context, R.color.alert);

    private String mUserid;

    private TextView mUserNameTextView;
    private TextView mUploadWarningsTextView;
    private TextView mPendingUploadsNoneTextView;
    private TextView mCompletedUploadsNoneTextView;
    private RecyclerView mPendingUploadsRecyclerView;
    private RecyclerView mCompletedUploadsRecyclerView;
    private QueuedUploadItemAdapter mPendingItemsAdapter;
    private QueuedUploadItemAdapter mCompletedItemsAdapter;

    private List<UploadItem> mQueuedItems = new LinkedList<>();
    private List<UploadItem> mCompletedItems;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getActivity().getIntent();
        mUserid = intent.getStringExtra(Constants.USERID);
        if (mUserid == null || mUserid.length() == 0) {
            mUserid = UserHelper.getInstance().getUserId();
        }

        mCompletedItems = UploadService.getUploadHistory();
        // We must manually refresh the mQueuedItems, because it's a priority queue, with no itemAt()
        mQueuedItems.addAll(UploadService.getUploadQueue());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_upload, container, false);
        
        // The actionbar has a "title" property that is set from the activity's "label=" property
        // from the AndroidManifest file. Here, we make the toolbar work like an action bar.
        Toolbar toolbar = (Toolbar) view.findViewById(R.id.main_toolbar);
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
        // The toolbar also *contains* a TextView with an id of main_toolbar_title.
        TextView main_title = (TextView) view.findViewById(R.id.main_toolbar_title);
        main_title.setText("");

        // We want a "back" button (sometimes called "up"), but we don't want back navigation, but
        // to simply end this activity without setting project or community.
        ((AppCompatActivity)getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View view){
                getActivity().finish();
            }
        });

        // User name
        mUserNameTextView = (TextView) view.findViewById(R.id.upload_userid);
        mUserNameTextView.setText(mUserid);
        
        // Field for any warning text.
        mUploadWarningsTextView = (TextView)view.findViewById(R.id.upload_warnings);
        String error = UploadService.getErrorMessage();
        mUploadWarningsTextView.setText(error);

        mPendingUploadsNoneTextView = (TextView) view.findViewById(R.id.pending_none);
        mCompletedUploadsNoneTextView = (TextView) view.findViewById(R.id.completed_none);
        mPendingUploadsNoneTextView.setVisibility(mQueuedItems.size() == 0 ? View.VISIBLE : View.GONE);
        mCompletedUploadsNoneTextView.setVisibility(mCompletedItems.size() == 0 ? View.VISIBLE : View.GONE);

        mPendingUploadsRecyclerView = (RecyclerView) view.findViewById(R.id.pending_uploads_recycler);
        mPendingUploadsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mPendingUploadsRecyclerView.getRecycledViewPool().setMaxRecycledViews(0, 0);

        mPendingItemsAdapter = new QueuedUploadItemAdapter();
        mPendingItemsAdapter.setQueuedUploadItems(mQueuedItems);
        mPendingUploadsRecyclerView.setAdapter(mPendingItemsAdapter);

        mCompletedUploadsRecyclerView = (RecyclerView) view.findViewById(R.id.completed_uploads_recycler);
        mCompletedUploadsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mCompletedUploadsRecyclerView.getRecycledViewPool().setMaxRecycledViews(0, 0);

        mCompletedItemsAdapter = new QueuedUploadItemAdapter();
        mCompletedItemsAdapter.setQueuedUploadItems(mCompletedItems);
        mCompletedUploadsRecyclerView.setAdapter(mCompletedItemsAdapter);

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "UploadStatus Fragment paused");
        LocalBroadcastManager.getInstance(context).unregisterReceiver(mMessageReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "UploadStatus Fragment resumed");
        LocalBroadcastManager.getInstance(context).registerReceiver(
            mMessageReceiver, new IntentFilter(UploadService.UPLOADER_STATUS_EVENT));
    }

    /**
     * Handle the results of activities that we start.
     * @param requestCode The request code that we used in startActivityForResult
     * @param resultCode The result code that the activity set at its exit.
     * @param data Any extra data returned by the activity.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE:
                break;
        }
    }

    /**
     * Button to open App Notifications configuration.
     */
    private OnClickListener configureIconListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent();
            intent.putExtra("configure", true);
            getActivity().setResult(RESULT_OK, intent);
            getActivity().finish();
        }
    };

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(UploadService.UPLOADER_STATUS_EVENT)) {
                Log.d(TAG, "Dataset changed");
                mQueuedItems.clear();
                mQueuedItems.addAll(UploadService.getUploadQueue());
                mPendingUploadsNoneTextView.setVisibility(
                    mQueuedItems.size() == 0 ? View.VISIBLE : View.GONE);
                mCompletedUploadsNoneTextView.setVisibility(
                    mCompletedItems.size() == 0 ? View.VISIBLE : View.GONE);

                mPendingItemsAdapter.notifyDataSetChanged();
                mCompletedItemsAdapter.notifyDataSetChanged();
                if (mUserid == null || mUserid.length() == 0) {
                    mUserid = UserHelper.getInstance().getUserId();
                    mUserNameTextView.setText(mUserid);
                }
            }
        }
    };

    private class QueuedUploadItemHolder extends RecyclerView.ViewHolder {
        private TextView mFileNameTextView;
        private TextView mFileSizeTextView;
        private TextView mFileStatusTextView;
        private TextView mFileTimeTextView;
        private TextView mFileDateTextView;

        QueuedUploadItemHolder(View itemView) {
            super(itemView);

            mFileNameTextView = (TextView) itemView.findViewById(R.id.list_item_update_file_name);
            mFileSizeTextView = (TextView) itemView.findViewById(R.id.list_item_update_file_size);
            mFileStatusTextView = (TextView) itemView.findViewById(R.id.list_item_update_transfer_status);
            mFileTimeTextView = (TextView) itemView.findViewById(R.id.list_item_update_transfer_time);
            mFileDateTextView = (TextView) itemView.findViewById(R.id.list_item_upload_timestamp);
        }

        private void bindQueuedUploadItem(final UploadItem uploadItem) {
            mFileNameTextView.setText(uploadItem.file.getName());
            mFileSizeTextView.setText(TBLoaderUtils.getBytesString(uploadItem.size));
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.US);
            df.setTimeZone(UTC);
            String filesDate = df.format(uploadItem.timestamp);
            mFileDateTextView.setText(filesDate);
            if (uploadItem.elapsedMillis > 0) {
                mFileTimeTextView.setText(String.format("%01.3f s", uploadItem.elapsedMillis/1000.0));
                if (uploadItem.success) {
                    mFileStatusTextView.setText("✓");
                    mFileStatusTextView.setTextColor(successColor);
                } else {
                    mFileStatusTextView.setText("✗");
                    mFileStatusTextView.setTextColor(alertColor);
                }
            } else {
                mFileStatusTextView.setText("⌛︎");
                mFileStatusTextView.setTextColor(alertColor);
            }
        }
    }

    private class QueuedUploadItemAdapter extends RecyclerView.Adapter<QueuedUploadItemHolder> {
        private volatile List<UploadItem> mItems;

        private void setQueuedUploadItems(List<UploadItem> items) {
            mItems = items;
        }

        @Override
        public QueuedUploadItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater.inflate(R.layout.list_item_upload, parent, false);
            return new QueuedUploadItemHolder(view);
        }

        @Override
        public void onBindViewHolder(QueuedUploadItemHolder holder, int position) {
            holder.bindQueuedUploadItem(mItems.get(position));
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }
    }


}
