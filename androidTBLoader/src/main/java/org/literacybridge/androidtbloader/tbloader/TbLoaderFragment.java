package org.literacybridge.androidtbloader.tbloader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;



import org.literacybridge.androidtbloader.R;
import org.literacybridge.androidtbloader.TBLoaderAppContext;
import org.literacybridge.androidtbloader.checkin.CheckinActivity;
import org.literacybridge.androidtbloader.community.ChooseCommunityActivity;
import org.literacybridge.androidtbloader.community.CommunityInfo;
import org.literacybridge.androidtbloader.content.ContentInfo;
import org.literacybridge.androidtbloader.content.ContentManager;
import org.literacybridge.androidtbloader.talkingbook.TalkingBookConnectionManager;
import org.literacybridge.androidtbloader.util.Config;
import org.literacybridge.androidtbloader.util.PathsProvider;
import org.literacybridge.core.fs.FsFile;
import org.literacybridge.core.fs.OperationLog;
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
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

import static android.app.Activity.RESULT_OK;
import static android.text.TextUtils.TruncateAt.MARQUEE;
import static org.literacybridge.androidtbloader.util.Constants.ISO8601;
import static org.literacybridge.androidtbloader.util.Constants.UTC;
import static org.literacybridge.core.tbloader.TBLoaderConstants.COLLECTED_DATA_SUBDIR_NAME;

/**
 * This implements the UI of the Loader portion of the application.
 */
public class TbLoaderFragment extends Fragment {
    private static final String TAG = "TBL!:" + TbLoaderFragment.class.getSimpleName();

    private final int REQUEST_CODE_GET_COMMUNITY = 101;

    private TalkingBookConnectionManager mTalkingBookConnectionManager;
    private TalkingBookConnectionManager.TalkingBook mConnectedDevice;
    private TBDeviceInfo mConnectedDeviceInfo;
    private String mProject;
    private boolean mStatsOnly;
    private List<CommunityInfo> mCommunities;
    private CommunityInfo mCommunity;
    private String mLocation;
    private ContentInfo mContentInfo;
    private String mSrnPrefix = "b-";

    private TextView mTalkingBookIdTextView;
    private TextView mTalkingBookWarningsTextView;

    private TextView mCommunityNameTextView;

    private CheckBox mRefreshFirmwareCheckBox;

    private TextView mUpdateStepTextView;
    private TextView mUpdateDetailTextView;
    private TextView mUpdateLogTextView;

    private Button mGoButton;
    private ProgressBar mSpinner;

    private boolean mUpdateInProgress = false;
    private TextView mProjectNameTextView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getActivity().getIntent();
        mProject = intent.getStringExtra("project");
        mStatsOnly = intent.getBooleanExtra("statsonly", false);
        mLocation = intent.getStringExtra("location");
        if (!mStatsOnly) {
            mCommunities = CommunityInfo.parseExtra(intent.getStringArrayListExtra("communities"));
            // If only one community, don't prompt the user to select it.
            if (mCommunities != null && mCommunities.size() == 1) {
                mCommunity = mCommunities.get(0);
            }
        }
        mTalkingBookConnectionManager = ((TBLoaderAppContext) getActivity().getApplicationContext()).getTalkingBookConnectionManager();
        ContentManager mContentManager = ((TBLoaderAppContext) getActivity().getApplicationContext())
                .getContentManager();
        mContentInfo = mContentManager.getContentInfo(mProject);

        ////////////////////////////////////////////////////////////////////////////////
        // Debug code
        SharedPreferences userPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        TBLoaderAppContext tbLoaderAppContext = (TBLoaderAppContext) getActivity().getApplicationContext();
        boolean isDebug = tbLoaderAppContext.isDebug();
        boolean mSimulateDevice = userPrefs.getBoolean("pref_simulate_device", false);
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
            mTalkingBookConnectionManager.setSimulatedTalkingBook(new TalkingBookConnectionManager.TalkingBook(tbRoot, "B-000CFFFF", "B-000CFFFF", null));
        }
        // Debug code
        ////////////////////////////////////////////////////////////////////////////////

        OperationLog.Operation op = OperationLog.startOperation("CanAccessConnectedDevice");
        mTalkingBookConnectionManager.canAccessConnectedDevice();
        op.finish();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_update, container, false);

        // The actionbar has a "title" property that is set from the activity's "label=" property
        // from the AndroidManifest file. Here, we make the toolbar work like an action bar.
        Toolbar toolbar = (Toolbar) view.findViewById(R.id.main_toolbar);
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
        if (mStatsOnly) {
            ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(
                            R.string.updater_collect_stats_title);
        }
        // The toolbar also *contains* a TextView with an id of main_toolbar_title.
        TextView main_title = (TextView) view.findViewById(R.id.main_toolbar_title);
        main_title.setText("");

        // We want a "back" button (sometimes called "up"), but we don't want back navigation, but
        // to simply end this activity without setting project or community.
        toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        toolbar.setNavigationOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if (shouldDoBackPressed()) {
                    getActivity().finish();
                }
            }
        });

        // Project name and initial value.
        mProjectNameTextView = (TextView) view.findViewById(R.id.update_project_name);
        mProjectNameTextView.setText(mProject);

        if (mStatsOnly) {
            view.findViewById(R.id.loader_deployment).setVisibility(View.GONE);
        } else {
            TextView mContentUpdateNameTextView = (TextView) view.findViewById(
                R.id.content_update_name);
            File contentUpdateDirectory = PathsProvider.getLocalContentUpdateDirectory(mProject);
            mContentUpdateNameTextView.setText(contentUpdateDirectory.getName());
        }

        // Talking Book ID, aka serial number, and initial value.
        mTalkingBookIdTextView = (TextView)view.findViewById(R.id.talking_book_id);

        // Field for any warning text.
        mTalkingBookWarningsTextView = (TextView)view.findViewById(R.id.talking_book_warnings);
        mTalkingBookWarningsTextView.setText("");

        if (mStatsOnly) {
            mCommunityNameTextView = (TextView) view.findViewById(R.id.content_update_display_community);
            // Community selection is irrelevant for stats only; for that we just want to display the name
            // found on the Talking Book.
            view.findViewById(R.id.loader_community).setVisibility(View.GONE);
            view.findViewById(R.id.loader_display_community).setVisibility(View.VISIBLE);
        } else {
            // Community. Show the values if we already know them.
            mCommunityNameTextView = (TextView) view.findViewById(R.id.community_name);
            LinearLayout mCommunityGroup = (LinearLayout) view.findViewById(R.id.loader_community);
            mCommunityNameTextView.setSelected(true);
            mCommunityGroup.setOnClickListener(setCommunityListener);
        }

        mRefreshFirmwareCheckBox = (CheckBox)view.findViewById(R.id.refresh_firmware);
        if (mStatsOnly) {
            mRefreshFirmwareCheckBox.setVisibility(View.GONE);
            ((TextView)view.findViewById(R.id.update_step_label)).setText(getString(
                R.string.statistics_step_label));
        }

        // ProgressListener display
        mUpdateStepTextView = (TextView)view.findViewById(R.id.update_step);
        mUpdateDetailTextView = (TextView)view.findViewById(R.id.update_detail);
        mUpdateLogTextView = (TextView)view.findViewById(R.id.update_log);

        mSpinner = (ProgressBar)view.findViewById(R.id.progressBar1);
        mSpinner.setVisibility(View.GONE);
        mGoButton = (Button)view.findViewById(R.id.button_go);
        mGoButton.setOnClickListener(goClickListener);

        if (mCommunity != null) {
            setCommunityName(mCommunity);
        }

        return view;
    }

    boolean shouldDoBackPressed() {
        return !mUpdateInProgress;
    }


    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "Update Fragment paused");
        mTalkingBookConnectionManager
                .setTalkingBookConnectionEventListener(null);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "Update Fragment resumed");
        mTalkingBookConnectionManager
                .setTalkingBookConnectionEventListener(talkingBookConnectionEventListener);
        TalkingBookConnectionManager.TalkingBook connnectedDevice = mTalkingBookConnectionManager.getConnectedTalkingBook();
        updateTbConnectionStatus(connnectedDevice);
        setButtonState();
        mProgressListener.refresh();
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
            case REQUEST_CODE_GET_COMMUNITY:
                onGotCommunity(resultCode, data);
                break;
        }
    }

    private void onGotCommunity(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (data.hasExtra("selected")) {
                final CommunityInfo community = CommunityInfo.parseExtra(data.getStringExtra("selected"));
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setCommunityName(community);
                    }
                });
            }
        }
    }

    private void setCommunityName(CommunityInfo community) {
        mCommunity = community;
        mCommunityNameTextView.setText(mCommunity.getName());
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
            mProgressListener.clear();
            mSpinner.setVisibility(View.VISIBLE);
            mUpdateInProgress = true;
            setButtonState();

            Executors.newSingleThreadExecutor().submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        performOperation();
                    } catch (Exception e) {
                        Log.d(TAG, "Unexpected exception updating Talking Book", e);
                        /*
                         * It's not the friendliest thing to display an exception stack trace to the user.
                         * However, while the app is young, they'll happen, and we need to get the
                         * information back to the developer. And when the app is mature, and these
                         * no longer happen, well, we'll no longer show them to the user. And if, somehow,
                         * an exception DOES occur, we *really* need to get that back to the developer.
                         */
                        mProgressListener.log(getStackTrace(e));
                        mProgressListener.log("Unexpected exception:");
                    } finally {
                        mConnectedDeviceInfo = new TBDeviceInfo(mConnectedDevice.getTalkingBookRoot(),
                                mConnectedDevice.getDeviceLabel(),
                                mSrnPrefix);
                        final String srn = mConnectedDeviceInfo.getSerialNumber();
                        // We always want to turn off the spinner, and re-enable the Go button.
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mSpinner.setVisibility(View.INVISIBLE);
                                mUpdateInProgress = false;
                                mTalkingBookIdTextView.setText(srn);
                                setButtonState();
                                setMessages();
                                Toast.makeText(getActivity(), "It is now safe to disconnect the Talking Book.", Toast.LENGTH_SHORT).show();
                            }
                        });
                        unmount();
                    }
                }
            });
        }
    };

    private void unmount() {
        // This call doesn't actually do anything. Maybe someday Android will let us unmount
        // our own devices.
//        boolean success = mTalkingBookConnectionManager.unMount(mConnectedDevice);
        // This stinks from usability, but is the official way.
//        if (!success) {
//            Intent i = new Intent(android.provider.Settings.ACTION_MEMORY_CARD_SETTINGS);
//            startActivity(i);
//        }
    }

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
            List<CommunityInfo> list = mCommunities != null ? mCommunities :
                                       new ArrayList<>(mContentInfo.getCommunities().values());
            Intent intent = new Intent(getActivity(), ChooseCommunityActivity.class);
            intent.putExtra("communities", CommunityInfo.makeExtra(list));
            startActivityForResult(intent, REQUEST_CODE_GET_COMMUNITY);
        }
    };

    /**
     * Set the Go button to enabled or disabled, depending on the application state.
     */
    private void setButtonState() {
        // Enable the Go button if we have a TB connected, have a location & a community,
        // and aren't already updating.
        boolean goEnabled = (mStatsOnly || mCommunity != null) &&
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
        String message = "";
        if (mConnectedDeviceInfo != null) {
            String deviceCommunity = mConnectedDeviceInfo.getCommunityName();
            String deviceProject = mConnectedDeviceInfo.getProjectName();
            if (!mStatsOnly) {
                boolean projectMismatch = !deviceProject.equalsIgnoreCase(mProject) &&
                    !deviceProject.equalsIgnoreCase("UNKNOWN");
                boolean communityMismatch =
                    mCommunity != null && !deviceCommunity.equalsIgnoreCase(mCommunity.getName()) &&
                        !deviceCommunity.equalsIgnoreCase("UNKNOWN");
                if (projectMismatch || communityMismatch) {
                    message = "Warning: This Talking Book was previously part of";
                    if (projectMismatch)
                        message += " project " + deviceProject;
                    if (mCommunity != null) {
                        if (projectMismatch && communityMismatch)
                            message += " and";
                        if (communityMismatch)
                            message += " community " + deviceCommunity;
                    }
                }
            }
        }
        mTalkingBookWarningsTextView.setText(message);
    }

    /**
     * Listens to updates from the TalkingBookConnectionManager, updates buttons.
     */
    private TalkingBookConnectionManager.TalkingBookConnectionEventListener talkingBookConnectionEventListener =
        new TalkingBookConnectionManager.TalkingBookConnectionEventListener() {
            @Override
            public void onTalkingBookConnectEvent(final TalkingBookConnectionManager.TalkingBook connectedDevice) {
                Log.d(TAG, String.format("Saw new Talking Book: %s", connectedDevice));
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateTbConnectionStatus(connectedDevice);
                        // New TB connected, so clear any results from updating a previous TB.
                        mProgressListener.clear();
                        setButtonState();
                    }
                });
            }

            @Override
            public void onTalkingBookDisConnectEvent() {
                Log.d(TAG, "Disconnected Talking Book");
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
        // If there's an object, is it still the same object?
        if (mConnectedDevice == connectedDevice) {
            // Yep, nothing to do.
            return;
        }
        String srn = "";
        // Is there now a (new) device connected?
        if (connectedDevice != null) {
            mProgressListener.clear();
        }
        mConnectedDevice = connectedDevice;
        mConnectedDeviceInfo = null;
        if (connectedDevice != null) {
            srn = connectedDevice.getSerialNumber();
            mConnectedDeviceInfo = new TBDeviceInfo(mConnectedDevice.getTalkingBookRoot(),
                    mConnectedDevice.getDeviceLabel(),
                    mSrnPrefix);
        }
        mTalkingBookIdTextView.setText(srn);

        if (mStatsOnly) {
            String deviceCommunity = mConnectedDeviceInfo.getCommunityName();
            String deviceProject = mConnectedDeviceInfo.getProjectName();
            if (mProject == null || !deviceProject.equalsIgnoreCase("unknown")) {
                mProject = deviceProject;
            }
            mProjectNameTextView.setText(deviceProject);
            mCommunityNameTextView.setText(deviceCommunity);
        }
        // This could be a place to update the srn prefix.
    }

    /**
     * Listens to progress from the tbloader, updates the progress display.
     */
    abstract class MyProgressListener extends ProgressListener {
        abstract public void clear();
        abstract public void refresh();
        abstract public void extraStep(String step);
    }
    private MyProgressListener mProgressListener = new MyProgressListener () {
        private String mStep;
        private String mDetail;
        private String mLog;
        private Activity mActivity = null;

        private Activity activity() {
            if (mActivity == null) {
                mActivity = getActivity();
            }
            return mActivity;
        }
        
        public void clear() {
            mStep = mDetail = mLog = "";
            refresh();
        }

        public void refresh() {
            mUpdateStepTextView.setText(mStep);
            mUpdateDetailTextView.setText(mDetail);
            mUpdateLogTextView.setText(mLog);
        }

        /**
         * Not part of the progress interface; this activity has a few steps of its own.
         * @param step in string form.
         */
        public void extraStep(String step) {
            mStep = step;
            mDetail = "";
            activity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mUpdateStepTextView.setText(mStep);
                    mUpdateDetailTextView.setText(mDetail);
                }
            });
        }

        @Override
        public void step(final Steps step) {
            mStep = step.description();
            mDetail = "";
            activity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mUpdateStepTextView.setText(mStep);
                    mUpdateDetailTextView.setText(mDetail);
                }
            });
        }

        @Override
        public void detail(final String detail) {
            mDetail = detail;
            activity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mUpdateDetailTextView.setText(mDetail);
                }
            });
        }

        @Override
        public void log(final String line) {
            mLog = line + "\n" + mLog;
            activity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mUpdateLogTextView.setText(mLog);
                }
            });
        }

        @Override
        public void log(final boolean append, final String line) {
            if (!append) {
                mLog = line + "\n" + mLog;
            } else {
                // Find first (or only) line break
                int nl = mLog.indexOf("\n");
                if (nl > 0) {
                    // {old stuff from before line break} {new stuff} {line break} {old stuff from after line break}
                    String pref = mLog.substring(0, nl);
                    String suff = mLog.substring(nl + 1);
                    mLog = pref + line + "\n" + suff;
                } else {
                    // No line breaks, so simply append to anything already there.
                    mLog = mLog + line;
                }
            }
            activity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mUpdateLogTextView.setText(mLog);
                }
            });
        }
    };

    /**
     * Updates the Talking Book
     */
    private void performOperation() {
        OperationLog.Operation opLog = OperationLog.startOperation(
            mStatsOnly ? "CollectStatistics" : "UpdateTalkingBook");
        Config config = TBLoaderAppContext.getInstance().getConfig();
        TBDeviceInfo tbDeviceInfo = new TBDeviceInfo(mConnectedDevice.getTalkingBookRoot(),
                                                     mConnectedDevice.getDeviceLabel(),
                                                     mSrnPrefix);

        long startTime = System.currentTimeMillis();

        // Where to gather the statistics and user recordings, to be uploaded.
//        TbFile collectionTbFile = new FsFile(PathsProvider.getUploadDirectory());
//        TbFile collectedDataDirectory = collectionTbFile.open(COLLECTED_DATA_SUBDIR_NAME);
        String collectionTimestamp = ISO8601.format(new Date());
        DateFormat df = new SimpleDateFormat("yyyy/MM/dd", Locale.US);
        df.setTimeZone(UTC);
        String todaysDate = df.format(new Date());

        File collectedDataDirectory = new File(PathsProvider.getLocalTempDirectory(),
                                               COLLECTED_DATA_SUBDIR_NAME + File.separator
                                                   + collectionTimestamp);
        TbFile collectedDataTbFile = new FsFile(collectedDataDirectory);

        // Working storage.
        TbFile tempTbFile = new FsFile(PathsProvider.getLocalTempDirectory()).open("temp");
        tempTbFile.getParent().mkdirs();

        TBLoaderConfig tbLoaderConfig = new TBLoaderConfig.Builder()
            .withTbLoaderId(config.getTbcdid())
            .withProject(mProject)
            .withSrnPrefix(mSrnPrefix)
            .withCollectedDataDirectory(collectedDataTbFile)
            .withTempDirectory(tempTbFile)
            .build();

        DeploymentInfo oldDeploymentInfo = tbDeviceInfo.createDeploymentInfo(mProject);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTalkingBookWarningsTextView.setText(getString(R.string.do_not_disconnect));
            }
        });

        TBLoaderCore.Builder builder = new TBLoaderCore.Builder()
            .withTbLoaderConfig(tbLoaderConfig)
            .withTbDeviceInfo(tbDeviceInfo)
            .withOldDeploymentInfo(oldDeploymentInfo)
            .withLocation(mLocation)
            .withRefreshFirmware(mRefreshFirmwareCheckBox.isChecked())
            .withProgressListener(mProgressListener)
            .withStatsOnly(mStatsOnly);

        TBLoaderCore.Result result;
        if (mStatsOnly) {
            result = builder.build().collectStatistics();
        } else {
            // The directory with images. {project}/content/{deployment}
            File contentUpdateDirectory = PathsProvider.getLocalContentUpdateDirectory(mProject);
            DeploymentInfo newDeploymentInfo = getUpdateDeploymentInfo(opLog, tbDeviceInfo,
                                                        collectionTimestamp, todaysDate,
                                                        collectedDataDirectory,
                                                        contentUpdateDirectory
                                                        );
            // Add in the update specific data, then go!
            result = builder
                .withDeploymentDirectory(new FsFile(contentUpdateDirectory))
                .withNewDeploymentInfo(newDeploymentInfo)
                .build().update();
        }

        // Zip up the files, and give the .zip to the uploader.
        try {
            long zipStart = System.currentTimeMillis();
            mProgressListener.extraStep("Zipping statistics and user feedback");
            String collectedDataZipName = "collected-data/tbcd" + config.getTbcdid() + "/" + collectionTimestamp + ".zip";
            File uploadableZipFile = new File(PathsProvider.getLocalTempDirectory(), collectedDataZipName);

            // Zip all the files together. We don't really get any compression, but it collects them into
            // a single archive file.
            ZipUnzip.zip(collectedDataDirectory, uploadableZipFile, true);
            collectedDataTbFile.deleteDirectory();

            ((TBLoaderAppContext)getActivity().getApplicationContext()).getUploadService().uploadFileAsName(uploadableZipFile, collectedDataZipName);
            String message = String.format("Zipped statistics and user feedback in %s", formatElapsedTime(System.currentTimeMillis()-zipStart));
            mProgressListener.log(message);
            message = String.format("TB-Loader completed in %s", formatElapsedTime(System.currentTimeMillis()-startTime));
            mProgressListener.log(message);
            mProgressListener.extraStep("Finished");
        } catch (IOException e) {
            e.printStackTrace();
            mProgressListener.log(getStackTrace(e));
            mProgressListener.log("Exception zipping stats");
            opLog.put("zipException", e);
        }
        opLog.finish();
    }

    private DeploymentInfo getUpdateDeploymentInfo(OperationLog.Operation opLog,
                                                   TBDeviceInfo tbDeviceInfo,
                                                   String collectionTimestamp, String todaysDate,
                                                   File collectedDataDirectory,
                                                   File contentUpdateDirectory) {
        Config config = TBLoaderAppContext.getInstance().getConfig();
        // Find the image with the community's language and/or group (such as a/b test group).
        String imageName = TBLoaderUtils.getImageForCommunity(contentUpdateDirectory, mCommunity.getName());

        // What firmware comes with this content update?
        String firmwareRevision = TBLoaderUtils.getFirmwareVersionNumbers(contentUpdateDirectory);

        String deviceSerialNumber = tbDeviceInfo.getSerialNumber();
        if (tbDeviceInfo.needNewSerialNumber()) {
            deviceSerialNumber = getNewDeviceSerialNumber();
        }

        DeploymentInfo.DeploymentInfoBuilder builder = new DeploymentInfo.DeploymentInfoBuilder()
                .withSerialNumber(deviceSerialNumber)
                .withProjectName(mProject)
                .withDeploymentName(contentUpdateDirectory.getName())
                .withPackageName(imageName)
                .withUpdateDirectory(collectedDataDirectory.getName())
                .withUpdateTimestamp(todaysDate) // TODO: this should be the "deployment date", the first date the new content is deployed.
                .withFirmwareRevision(firmwareRevision)
                .withCommunity(mCommunity.getName());
        DeploymentInfo newDeploymentInfo = builder.build();

        opLog.put("project", mProject)
            .put("deployment", contentUpdateDirectory.getName())
            .put("package", imageName)
            .put("community", mCommunity.getName())
            .put("sn", deviceSerialNumber)
            .put("tbloaderId", config.getTbcdid())
            .put("username", config.getUsername())
            .put("timestamp", collectionTimestamp);
        return newDeploymentInfo;
    }

    @SuppressLint("DefaultLocale")
    private String formatElapsedTime(Long millis) {
        if (millis < 1000) {
            // Less than one second
            return String.format("%d ms", millis);
        } else if (millis < 60000) {
            // Less than one minute. Format like '1.25 s' or '25.3 s' (3 digits).
            String time = String.format("%f", millis / 1000.0);
            return time.substring(0, 4) + " s";
        } else {
            long minutes = millis / 60000;
            long seconds = (millis % 60000) / 1000;
            return String.format("%d:%02d", minutes, seconds);
        }
    }

    /**
     * Allocates a new serial number, and builds the string from prefix, tb-loader device id, and the serial number.
     * NOTE: To avoid collisions with the PC assigned serial numbers, this sets the high bit of the tb-loader device id.
     * @return The serial number string.
     */
    private String getNewDeviceSerialNumber() {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        int tbcdid = Integer.parseInt(TBLoaderAppContext.getInstance().getConfig().getTbcdid(), 16);
        tbcdid |= 0x8000;

        int newSrn = TBLoaderAppContext.getInstance().getConfig().allocateDeviceSerialNumber();

        String newSerialNumber = String.format("%s%04x%04x", mSrnPrefix, tbcdid, newSrn).toUpperCase();
        OperationLog.log("AllocateSerialNumber").put("srn", newSerialNumber).finish();
        return newSerialNumber;
    }

}
