package org.literacybridge.androidtbloader.installer;

import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.literacybridge.androidtbloader.R;
import org.literacybridge.androidtbloader.TBLoaderAppContext;
import org.literacybridge.androidtbloader.community.ChooseCommunityActivity;
import org.literacybridge.androidtbloader.content.ContentInfo;
import org.literacybridge.androidtbloader.content.ContentManager;
import org.literacybridge.androidtbloader.talkingbook.TalkingBookConnectionManager;
import org.literacybridge.androidtbloader.util.Config;
import org.literacybridge.androidtbloader.util.PathsProvider;
import org.literacybridge.core.fs.FsFile;
import org.literacybridge.core.fs.TbFile;
import org.literacybridge.core.fs.ZipUnzip;
import org.literacybridge.core.tbloader.DeploymentInfo;
import org.literacybridge.core.tbloader.ProgressListener;
import org.literacybridge.core.tbloader.TBDeviceInfo;
import org.literacybridge.core.tbloader.TBLoaderConfig;
import org.literacybridge.core.tbloader.TBLoaderCore;
import org.literacybridge.core.tbloader.TBLoaderUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.Executors;

import static android.app.Activity.RESULT_OK;
import static android.text.TextUtils.TruncateAt.MARQUEE;
import static org.literacybridge.core.tbloader.TBLoaderConstants.COLLECTED_DATA_SUBDIR_NAME;

/**
 * This implements the UI of the Loader portion of the application.
 */
public class UpdateFragment extends Fragment {
    private static final String TAG = UpdateFragment.class.getSimpleName();

    private final int REQUEST_CODE_GET_COMMUNITY = 101;

    private boolean mSimulateDevice;
    private TalkingBookConnectionManager mTalkingBookConnectionManager;
    private TalkingBookConnectionManager.TalkingBook mConnectedDevice;
    private TBDeviceInfo mConnectedDeviceInfo;
    private ContentManager mContentManager;
    private String mProject;
    private ArrayList<String> mCommunities;
    private String mCommunity;
    private String mLocation;
    private ContentInfo mContentInfo;
    private String mSrnPrefix = "b-";

    private TextView mProjectNameTextView;
    private TextView mContentUpdateNameTextView;
    private TextView mTalkingBookIdTextView;
    private TextView mTalkingBookWarningsTextView;
    private TextView mCommunityNameTextView;

    private TextView mUpdateStepTextView;
    private TextView mUpdateDetailTextView;
    private TextView mUpdateLogTextView;

    private CheckBox mRefreshFirmwareCheckBox;
    private LinearLayout mCommunityGroup;

    private Button mGoButton;
    private ProgressBar mSpinner;

    private boolean mUpdateInProgress = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getActivity().getIntent();
        mProject = intent.getStringExtra("project");
        mLocation = intent.getStringExtra("location");
        mCommunities = intent.getStringArrayListExtra("communities");
        // If only one community, don't prompt the user to select it.
        if (mCommunities != null && mCommunities.size() == 1) {
            mCommunity = mCommunities.get(0);
        }
        mTalkingBookConnectionManager = ((TBLoaderAppContext) getActivity().getApplicationContext()).getTalkingBookConnectionManager();
        mContentManager = ((TBLoaderAppContext) getActivity().getApplicationContext()).getContentManager();
        mContentInfo = mContentManager.getContentInfo(mProject);

        ////////////////////////////////////////////////////////////////////////////////
        // Debug code
        SharedPreferences userPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        TBLoaderAppContext tbLoaderAppContext = (TBLoaderAppContext) getActivity().getApplicationContext();
        boolean isDebug = tbLoaderAppContext.isDebug();
        mSimulateDevice = userPrefs.getBoolean("pref_simulate_device", false);
        if (isDebug && mSimulateDevice) {
            InputStream tbStream = getResources().openRawResource(R.raw.demotbbackupzip);
            File tbZipFile = new File(PathsProvider.getLocalTempDirectory(), "simulated_tb.zip");
            File tbFile = new File(PathsProvider.getLocalTempDirectory(), "simulated_tb");
            TbFile tbZip = new FsFile(tbZipFile);
            TbFile tbRoot = new FsFile(tbFile);
            tbRoot.deleteDirectory();
            try {
                tbZip.createNew(tbStream);
                ZipUnzip.unzip(tbZipFile, tbFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mTalkingBookConnectionManager.setSimulatedTalkingBook(new TalkingBookConnectionManager.TalkingBook(tbRoot, "B-000CFFFF", "B-000CFFFF"));
        }
        // Debug code
        ////////////////////////////////////////////////////////////////////////////////

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_loader, container, false);

        // Project name and initial value.
        mProjectNameTextView = (TextView) view.findViewById(R.id.checkin_project_name);
        mProjectNameTextView.setText(mProject);
        mContentUpdateNameTextView = (TextView)view.findViewById(R.id.content_update_name);
        File contentUpdateDirectory = PathsProvider.getLocalContentUpdateDirectory(mProject);
        mContentUpdateNameTextView.setText(contentUpdateDirectory.getName());

        // Talking Book ID, aka serial number, and initial value.
        mTalkingBookIdTextView = (TextView)view.findViewById(R.id.talking_book_id);
        mRefreshFirmwareCheckBox = (CheckBox)view.findViewById(R.id.refresh_firmware);

        // Field for any warning text.
        mTalkingBookWarningsTextView = (TextView)view.findViewById(R.id.talking_book_warnings);
        mTalkingBookWarningsTextView.setText("");

        // Community. Show the values if we already know them.
        mCommunityNameTextView = (TextView)view.findViewById(R.id.community_name);
        mCommunityGroup = (LinearLayout)view.findViewById(R.id.loader_community);
        mCommunityNameTextView.setSelected(true);
        mCommunityGroup.setOnClickListener(setCommunityListener);

        mRefreshFirmwareCheckBox = (CheckBox)view.findViewById(R.id.refresh_firmware);

        // ProgressListener display
        mUpdateStepTextView = (TextView)view.findViewById(R.id.update_step);
        mUpdateDetailTextView = (TextView)view.findViewById(R.id.update_detail);
        mUpdateLogTextView = (TextView)view.findViewById(R.id.update_log);

        mSpinner = (ProgressBar)view.findViewById(R.id.progressBar1);
        mSpinner.setVisibility(View.GONE);
        mGoButton = (Button)view.findViewById(R.id.button_go);
        mGoButton.setOnClickListener(goClickListener);

        if (mCommunity != null && mCommunity.length() > 0) {
            setCommunityName(mCommunity);
        }
        updateTbConnectionStatus(mTalkingBookConnectionManager.getConnectedTalkingBook());

        setButtonState();

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        mTalkingBookConnectionManager
                .setTalkingBookConnectionEventListener(null);
    }

    @Override
    public void onResume() {
        super.onResume();
        mTalkingBookConnectionManager
                .setTalkingBookConnectionEventListener(talkingBookConnectionEventListener);
        updateTbConnectionStatus(mTalkingBookConnectionManager.canAccessConnectedDevice());
        setButtonState();
    }

    /**
     * Handle the results of activities that we start.
     * @param requestCode The request code that we used in startActivityForResult
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_GET_COMMUNITY:
                onGotCommunity(resultCode, data);
                break;
        }
    }

    private void onGotCommunity(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (data.hasExtra("selected")) {
                final String community = data.getStringExtra("selected");
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setCommunityName(community);
                    }
                });
            }
        }
    }

    private void setCommunityName(String community) {
        mCommunity = community;
        mCommunityNameTextView.setText(mCommunity);
        mCommunityNameTextView.setEllipsize(MARQUEE);
        mCommunityNameTextView.setSingleLine(true);
        mCommunityNameTextView.setMarqueeRepeatLimit(-1);
        mCommunityNameTextView.setSelected(true);
        setButtonState();
    }

    /**
     * This is it, the Big Red Switch. Do the update.
     */
    private OnClickListener goClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mUpdateStepTextView.setText("");
            mUpdateDetailTextView.setText("");
            mUpdateLogTextView.setText("");
            mSpinner.setVisibility(View.VISIBLE);
            mUpdateInProgress = true;
            setButtonState();

            Executors.newSingleThreadExecutor().submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        installContentUpdate();
                    } catch (Exception e) {
                        Log.d(TAG, "Unexpected exception updating Talking Book", e);
                        /*
                         * It's not the friendliest thing to display an exception stack trace to the user.
                         * However, while the app is young, they'll happen, and we need to get the
                         * information back to the developer. And when the app is mature, and these
                         * no longer happen, well, we'll no longer show them to the user. And if, somehow,
                         * an exception DOES occur, we *really* need to get that back to the developer.
                         */
                        progressListenerListener.log(getStackTrace(e));
                        progressListenerListener.log("Unexpected exception:");
                    } finally {
                        // We always want to turn off the spinner, and re-enable the Go button.
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mSpinner.setVisibility(View.GONE);
                                mUpdateInProgress = false;
                                setButtonState();
                                setMessages();
                            }
                        });
                    }
                }
            });
        }
    };

    private static String getStackTrace(Throwable aThrowable) {
        Writer result = new StringWriter();
        PrintWriter printWriter = new PrintWriter(result);
        aThrowable.printStackTrace(printWriter);
        return result.toString();
    }

    /**
     * Let the user manually set the community. If there is a list passed in, limit them to that list.
     */
    private OnClickListener setCommunityListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mCommunities != null && mCommunities.size() == 1) { return; }
            ArrayList<String> list = mCommunities != null ? mCommunities : new ArrayList<>(mContentInfo.getCommunities());
            Intent intent = new Intent(getActivity(), ChooseCommunityActivity.class);
            intent.putExtra("list", list);
            startActivityForResult(intent, REQUEST_CODE_GET_COMMUNITY);
        }
    };

    /**
     * Set the Go button to enabled or disabled, depending on the application state.
     */
    private void setButtonState() {
        // Enable the Go button if we have a TB connected, have a location & a community,
        // and aren't already updating.
        boolean goEnabled = (mCommunity != null && mCommunity.length() > 0) &&
                (mConnectedDevice != null) &&
                !mUpdateInProgress;
        mGoButton.setAlpha(goEnabled ? 1 : 0.5f);
        mGoButton.setEnabled(goEnabled);
        setMessages();
    }

    /**
     * Sets any appropriate warning messages regarding mismatches between the selected community
     * and project vs the connected Talking Book's previous community and project.
     */
    private void setMessages() {
        if (mConnectedDeviceInfo != null) {
            String deviceCommunity = mConnectedDeviceInfo.getCommunityName();
            String deviceProject = mConnectedDeviceInfo.getProjectName();
            boolean projectMismatch =  !deviceProject.equalsIgnoreCase(mProject) &&
                    !deviceProject.equalsIgnoreCase("UNKNOWN");
            boolean communityMismatch = mCommunity != null && !deviceCommunity.equalsIgnoreCase(mCommunity) &&
                    !deviceCommunity.equalsIgnoreCase("UNKNOWN");
            if (projectMismatch || communityMismatch) {
                String message = "Warning: This Talking Book was previously part of";
                if (projectMismatch) message += " project " + deviceProject;
                if (mCommunity != null) {
                    if (projectMismatch && communityMismatch) message += " and";
                    if (communityMismatch) message += " community " + deviceCommunity;
                }
                mTalkingBookWarningsTextView.setText(message);
            }
        } else {
            mTalkingBookWarningsTextView.setText("");
        }

    }

    /**
     * Listens to updates from the TalkingBookConnectionManager, updates buttons.
     * @param connectedDevice
     */
    private TalkingBookConnectionManager.TalkingBookConnectionEventListener talkingBookConnectionEventListener =
        new TalkingBookConnectionManager.TalkingBookConnectionEventListener() {
            @Override
            public void onTalkingBookConnectEvent(final TalkingBookConnectionManager.TalkingBook connectedDevice) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateTbConnectionStatus(connectedDevice);
                        setButtonState();
                    }
                });
            }

            @Override
            public void onTalkingBookDisConnectEvent() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateTbConnectionStatus(null);
                        setButtonState();
                    }
                });
            }
        };

    private void updateTbConnectionStatus(TalkingBookConnectionManager.TalkingBook connectedDevice) {
        String srn = "";
        mConnectedDevice = connectedDevice;
        mConnectedDeviceInfo = null;
        if (connectedDevice != null) {
            srn = connectedDevice.getSerialNumber();
            mConnectedDeviceInfo = new TBDeviceInfo(mConnectedDevice.getTalkingBookRoot(),
                    mConnectedDevice.getDeviceLabel(),
                    mSrnPrefix);
        }
        mTalkingBookIdTextView.setText(srn);
        // This could be a place to update the srn prefix.
    }

    /**
     * Listens to progress from the tbloader, updates the progress display.
     */
    ProgressListener progressListenerListener = new ProgressListener() {
        @Override
        public void step(final Steps step) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mUpdateStepTextView.setText(step.description());
                    mUpdateDetailTextView.setText("");
                }
            });
        }

        @Override
        public void detail(final String detail) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mUpdateDetailTextView.setText(detail);
                }
            });
        }

        @Override
        public void log(final String line) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mUpdateLogTextView.setText(line + "\n" + mUpdateLogTextView.getText());
                }
            });
        }

        @Override
        public void log(final boolean append, final String line) {
            if (!append) {
                log(line);
            } else {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String oldValue = mUpdateLogTextView.getText().toString();
                        int nl = oldValue.indexOf("\n");
                        if (nl > 0) {
                            String pref = oldValue.substring(0, nl);
                            String suff = oldValue.substring(nl+1);
                            mUpdateLogTextView.setText(pref + line + "\n" + suff);
                        } else {
                            mUpdateLogTextView.setText(oldValue + line);
                        }
                    }
                });
            }
        }
    };

    /**
     * Updates the Talking Book
     */
    private void installContentUpdate() {
        TBDeviceInfo tbDeviceInfo = new TBDeviceInfo(mConnectedDevice.getTalkingBookRoot(),
                    mConnectedDevice.getDeviceLabel(),
                    mSrnPrefix);

        // The directory with images. {project}/content/{deployment}
        File contentUpdateDirectory = PathsProvider.getLocalContentUpdateDirectory(mProject);
        String contentUpdateName = contentUpdateDirectory.getName();
        TbFile contentUpdateTbFile = new FsFile(contentUpdateDirectory);

        // Where to gather the statistics and user recordings, to be uploaded.
//        TbFile collectionTbFile = new FsFile(PathsProvider.getUploadDirectory());
//        TbFile collectedDataDirectory = collectionTbFile.open(COLLECTED_DATA_SUBDIR_NAME);
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyyMMdd'T'HHmmss.SSS'Z'");
        df.setTimeZone(tz);
        String collectionTimestamp = df.format(new Date());

        File collectedDataDirectory = new File(PathsProvider.getLocalTempDirectory(), COLLECTED_DATA_SUBDIR_NAME + File.separator + collectionTimestamp);
        TbFile collectedDataTbFile = new FsFile(collectedDataDirectory);

        // Working storage.
        TbFile tempTbFile = new FsFile(PathsProvider.getLocalTempDirectory()).open("temp");
        tempTbFile.getParent().mkdirs();

        // Find the image with the community's language and/or group (such as a/b test group).
        String imageName = TBLoaderUtils.getImageForCommunity(contentUpdateDirectory, mCommunity);

        // What firmware comes with this content update?
        String firmwareRevision = TBLoaderUtils.getFirmwareVersionNumbers(contentUpdateTbFile);

        TBLoaderConfig tbLoaderConfig = new TBLoaderConfig.Builder()
                .withTbLoaderId(Config.getTbcdid())
                .withProject(mProject)
                .withSrnPrefix(mSrnPrefix)
                .withCollectedDataDirectory(collectedDataTbFile)
                .withTempDirectory(tempTbFile)
                .build();

        DeploymentInfo oldDeploymentInfo = tbDeviceInfo.createDeploymentInfo(mProject);
        String deviceSerialNumber = tbDeviceInfo.getSerialNumber();
        if (tbDeviceInfo.needNewSerialNumber()) {
            deviceSerialNumber = getNewDeviceSerialNumber();
        }

        DeploymentInfo newDeploymentInfo = new DeploymentInfo(deviceSerialNumber,
                mProject,
                contentUpdateName,
                imageName,
                collectedDataDirectory.getName(),
                null, // TODO: this should be the "deployment date", the first date the new content is deployed.
                firmwareRevision,
                mCommunity);

        TBLoaderCore core = new TBLoaderCore.Builder()
                .withTbLoaderConfig(tbLoaderConfig)
                .withTbDeviceInfo(tbDeviceInfo)
                .withDeploymentDirectory(contentUpdateTbFile)
                .withOldDeploymentInfo(oldDeploymentInfo)
                .withNewDeploymentInfo(newDeploymentInfo)
                .withLocation(mLocation)
                .withRefreshFirmware(mRefreshFirmwareCheckBox.isChecked())
                .withProgressListener(progressListenerListener)
                .build();
        TBLoaderCore.Result result = core.update();

        // Zip up the files, and give the .zip to the uploader.
        try {
            String collectedDataZipName = "tbcd" + Config.getTbcdid() + "/" + collectionTimestamp + ".zip";
            File uploadableZipFile = new File(PathsProvider.getUploadDirectory(), collectedDataZipName);

            // Zip all the files together. We don't really get any compression, but it collects them into
            // a single archive file.
            ZipUnzip.zip(collectedDataDirectory, uploadableZipFile, true);
            collectedDataTbFile.deleteDirectory();

            ((TBLoaderAppContext)getActivity().getApplicationContext()).getUploadManager().upload(uploadableZipFile);
        } catch (IOException e) {
            e.printStackTrace();
            progressListenerListener.log(getStackTrace(e));
            progressListenerListener.log("Exception zipping stats");
        }
    }

    /**
     * Allocates a new serial number, and builds the string from prefix, tb-loader device id, and the serial number.
     * NOTE: To avoid collisions with the PC assigned serial numbers, this sets the high bit of the tb-loader device id.
     * @return The serial number string.
     */
    private String getNewDeviceSerialNumber() {
        final String prefName = "device_serial_number_counter";
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        int intSrn = sharedPreferences.getInt(prefName, 0);
        int tbcdid = Integer.parseInt(Config.getTbcdid(), 16);
        tbcdid |= 0x8000;

        final SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
        sharedPreferencesEditor.putInt(prefName, intSrn + 1);
        sharedPreferencesEditor.apply();

        return String.format("%s%04x%04x", mSrnPrefix, tbcdid, intSrn).toUpperCase();
    }

}
