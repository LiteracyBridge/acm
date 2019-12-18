package org.literacybridge.androidtbloader.tbloader;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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
import android.widget.Toast;
import org.literacybridge.androidtbloader.R;
import org.literacybridge.androidtbloader.TBLoaderAppContext;
import org.literacybridge.androidtbloader.checkin.LocationProvider;
import org.literacybridge.androidtbloader.recipient.RecipientChooserActivity;
import org.literacybridge.androidtbloader.talkingbook.TalkingBookConnectionManager;
import org.literacybridge.androidtbloader.util.Config;
import org.literacybridge.androidtbloader.util.Constants;
import org.literacybridge.androidtbloader.util.PathsProvider;
import org.literacybridge.androidtbloader.util.Util;
import org.literacybridge.core.fs.FsFile;
import org.literacybridge.core.fs.OperationLog;
import org.literacybridge.core.fs.TbFile;
import org.literacybridge.core.fs.ZipUnzip;
import org.literacybridge.core.spec.Recipient;
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
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

import static android.app.Activity.RESULT_OK;
import static android.text.TextUtils.TruncateAt.MARQUEE;
import static org.literacybridge.androidtbloader.util.Constants.ISO8601;
import static org.literacybridge.androidtbloader.util.Constants.UTC;
import static org.literacybridge.androidtbloader.util.PathsProvider.getLocalDeploymentDirectory;
import static org.literacybridge.core.tbloader.TBLoaderConstants.COLLECTED_DATA_SUBDIR_NAME;

/**
 * This implements the UI of the Loader portion of the application.
 */
public class TbLoaderFragment extends Fragment {
    private static final String TAG = "TBL!:" + TbLoaderFragment.class.getSimpleName();

    private final int REQUEST_CODE_SELECT_RECIPIENT = 101;

    private TBLoaderAppContext mAppContext;

    private TalkingBookConnectionManager mTalkingBookConnectionManager;
    private TalkingBookConnectionManager.TalkingBook mConnectedDevice;
    private TBDeviceInfo mConnectedDeviceInfo;
    private String mProject;
    private boolean mStatsOnly;
    private ArrayList<String> mPreselectedRecipients;
    private Recipient mRecipient;
    private String mCommunityDirectory;

    private String mLocation;
    private String mCoordinates;
    private String mSrnPrefix = "b-";

    private boolean mTestingDeployment;

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
    private boolean mCollapseCommunityName = false;
    private TextView mProjectNameTextView;
    private String mUserName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAppContext = (TBLoaderAppContext) getActivity().getApplicationContext();
        Intent intent = getActivity().getIntent();
        mProject = mAppContext.getProject();
        mStatsOnly = intent.getBooleanExtra("statsonly", false);
        mLocation = intent.getStringExtra("location");
        mTestingDeployment = intent.getBooleanExtra(Constants.TESTING_DEPLOYMENT, false);
        Location currentCoordinates = LocationProvider.getLatestLocation();
        if (currentCoordinates != null) {
            mCoordinates = String.format("%+03.5f %+03.5f", currentCoordinates.getLatitude(), currentCoordinates.getLongitude());
        }
        mUserName = intent.getStringExtra("username");
        if (!mStatsOnly) {
            if (intent.hasExtra(Constants.PRESELECTED_RECIPIENTS)) {
                mPreselectedRecipients = intent.getStringArrayListExtra(Constants.PRESELECTED_RECIPIENTS);
            } else {
                mPreselectedRecipients = new ArrayList<>();
            }
            // Will be null, if there is no recipient at the path 'mPreselectedRecipients'.
            mRecipient = mAppContext.getProgramSpec().getRecipients().getRecipient(mPreselectedRecipients);
        }
        mTalkingBookConnectionManager = mAppContext.getTalkingBookConnectionManager();

        ////////////////////////////////////////////////////////////////////////////////
        // Debug code
        SharedPreferences userPrefs = PreferenceManager.getDefaultSharedPreferences(mAppContext);
        boolean isDebug = mAppContext.isDebug();
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
        // Uncomment to force some allocations whenever the page loads. For testing the SRN management.
//        if (isDebug) {
//            TBLoaderAppContext.getInstance().getConfig().allocateDeviceSerialNumber((srn)-> {
//                    Log.d(TAG, "new serial number is " + srn);
//                },
//                (ex)->{
//                    Log.d(TAG, "Could not allocate serial number", ex);
//                });
//
//        }
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
        Toolbar toolbar = view.findViewById(R.id.main_toolbar);
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
        if (mStatsOnly) {
            ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(
                            R.string.updater_collect_stats_title);
        }
        // The toolbar also *contains* a TextView with an id of main_toolbar_title.
        TextView main_title = view.findViewById(R.id.main_toolbar_title);
        main_title.setText("");

        // We want a "back" button (sometimes called "up"), but we don't want back navigation, but
        // to simply end this activity without setting project or community.
        ((AppCompatActivity)getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(view1 -> {
            if (shouldDoBackPressed()) {
                getActivity().finish();
            }
        });

        // Project name and initial value.
        mProjectNameTextView = view.findViewById(R.id.update_project_name);
        mProjectNameTextView.setText(mProject);

        if (mStatsOnly) {
            view.findViewById(R.id.loader_deployment).setVisibility(View.GONE);
        } else {
            TextView mDeploymentNameTextView = view.findViewById(
                R.id.content_update_name);
            File deploymentDirectory = getLocalDeploymentDirectory(mProject);
            mDeploymentNameTextView.setText(deploymentDirectory.getName());
        }

        // Talking Book ID, aka serial number, and initial value.
        mTalkingBookIdTextView = view.findViewById(R.id.talking_book_id);

        // Field for any warning text.
        mTalkingBookWarningsTextView = view.findViewById(R.id.talking_book_warnings);
        mTalkingBookWarningsTextView.setText("");

        if (mStatsOnly) {
            mCommunityNameTextView = view.findViewById(R.id.content_update_display_community);
            // Community selection is irrelevant for stats only; for that we just want to display the name
            // found on the Talking Book.
            view.findViewById(R.id.loader_community).setVisibility(View.GONE);
            view.findViewById(R.id.loader_display_community).setVisibility(View.VISIBLE);
        } else {
            // Community. Show the values if we already know them.
            mCommunityNameTextView = view.findViewById(R.id.community_name);
            LinearLayout mCommunityGroup = view.findViewById(R.id.loader_community);
            mCommunityNameTextView.setSelected(true);
            mCommunityGroup.setOnClickListener(setCommunityListener);
        }
        mCommunityNameTextView.setEllipsize(MARQUEE);
        mCommunityNameTextView.setSingleLine(false);
        mCommunityNameTextView.setMarqueeRepeatLimit(-1);

        mRefreshFirmwareCheckBox = view.findViewById(R.id.refresh_firmware);
        if (mStatsOnly) {
            mRefreshFirmwareCheckBox.setVisibility(View.GONE);
            ((TextView)view.findViewById(R.id.update_step_label)).setText(getString(
                R.string.statistics_step_label));
        } else if (mTestingDeployment) {
            view.findViewById(R.id.test_deployment).setVisibility(View.VISIBLE);
        }

        // ProgressListener display
        mUpdateStepTextView = view.findViewById(R.id.update_step);
        mUpdateDetailTextView = view.findViewById(R.id.update_detail);
        mUpdateLogTextView = view.findViewById(R.id.update_log);

        mSpinner = view.findViewById(R.id.progressBar1);
        mSpinner.setVisibility(View.GONE);
        mGoButton = view.findViewById(R.id.button_go);
        mGoButton.setOnClickListener(goClickListener);

        if (mRecipient != null) {
            setCommunityName(mRecipient);
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
        if (requestCode == REQUEST_CODE_SELECT_RECIPIENT) {
            onGotCommunity(resultCode, data);
        }
    }

    private void onGotCommunity(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (data != null && data.hasExtra(Constants.SELECTED_RECIPIENT)) {
                ArrayList<String> selectedRecipient = data.getStringArrayListExtra(Constants.SELECTED_RECIPIENT);
                final Recipient recipient = mAppContext.getProgramSpec().getRecipients().getRecipient(selectedRecipient);
                getActivity().runOnUiThread(() -> setCommunityName(recipient));
            }
        }
    }

    private void setCommunityName(Recipient recipient) {
        mRecipient = recipient;
        Map<String,String> recipientsMap = mAppContext.getProgramSpec().getRecipientsMap();
        if (recipientsMap != null) {
            mCommunityDirectory = recipientsMap.get(recipient.recipientid);
        }
        mCollapseCommunityName = false;
        mCommunityNameTextView.setText(mRecipient.getName());
        mCommunityNameTextView.setEllipsize(MARQUEE);
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
            mCollapseCommunityName = true;
            setButtonState();

            TBDeviceInfo tbDeviceInfo = new TBDeviceInfo(mConnectedDevice.getTalkingBookRoot(),
                mConnectedDevice.getDeviceLabel(),
                mSrnPrefix);

            String deviceSerialNumber = tbDeviceInfo.getSerialNumber();
            if (!mStatsOnly && tbDeviceInfo.needNewSerialNumber()) {
                TBLoaderAppContext.getInstance().getConfig().allocateDeviceSerialNumber((srn)-> doPerformUpdate(tbDeviceInfo, srn),
                    (ex)->{
                        mTalkingBookWarningsTextView.setText(getString(R.string.CANT_ALLOCATE_SRN));
                        mSpinner.setVisibility(View.INVISIBLE);
                        mUpdateInProgress = false;
                        setButtonState();
                    });
            } else {
                doPerformUpdate(tbDeviceInfo, deviceSerialNumber);
            }
        }

        private void doPerformUpdate(TBDeviceInfo tbDeviceInfo, String deviceSerialNumber) {
            Executors.newSingleThreadExecutor().submit(() -> {
                try {
                    performOperation(tbDeviceInfo, deviceSerialNumber);
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
                    getActivity().runOnUiThread(() -> {
                        mSpinner.setVisibility(View.INVISIBLE);
                        mUpdateInProgress = false;
                        mTalkingBookIdTextView.setText(srn);
                        setButtonState();
                        setMessages();
                        Toast.makeText(getActivity(), "It is now safe to disconnect the Talking Book.", Toast.LENGTH_SHORT).show();
                    });
                    unmount();
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
            Intent intent = new Intent(getActivity(), RecipientChooserActivity.class);
            intent.putStringArrayListExtra(Constants.PRESELECTED_RECIPIENTS, mPreselectedRecipients);
            startActivityForResult(intent, REQUEST_CODE_SELECT_RECIPIENT);
        }
    };

    /**
     * Set the Go button to enabled or disabled, depending on the application state.
     */
    private void setButtonState() {
        // Enable the Go button if we have a TB connected, have a location & a community,
        // and aren't already updating.
        boolean goEnabled = (mStatsOnly || mRecipient != null) &&
                (mConnectedDevice != null) &&
                !mUpdateInProgress;
        mGoButton.setAlpha(goEnabled ? 1 : 0.5f);
        mGoButton.setEnabled(goEnabled);
        mCommunityNameTextView.setSingleLine(mCollapseCommunityName);

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
                    mRecipient != null && !deviceCommunity.equalsIgnoreCase(mRecipient.getName()) &&
                        !deviceCommunity.equalsIgnoreCase("UNKNOWN");
                if (projectMismatch || communityMismatch) {
                    message = "Warning: This Talking Book was previously part of";
                    if (projectMismatch)
                        message += " project " + deviceProject;
                    if (mRecipient != null) {
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
                getActivity().runOnUiThread(() -> {
                    updateTbConnectionStatus(connectedDevice);
                    // New TB connected, so clear any results from updating a previous TB.
                    mProgressListener.clear();
                    setButtonState();
                });
            }

            @Override
            public void onTalkingBookDisConnectEvent() {
                Log.d(TAG, "Disconnected Talking Book");
                getActivity().runOnUiThread(() -> {
                    updateTbConnectionStatus(null);
                    setButtonState();
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
            Log.d(TAG, String.format("Now connected to %s", mConnectedDeviceInfo.getDescription()));
        } else {
            Log.d(TAG, "Now disconnected from device");
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
    abstract static class MyProgressListener extends ProgressListener {
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
            activity().runOnUiThread(() -> {
                mUpdateStepTextView.setText(mStep);
                mUpdateDetailTextView.setText(mDetail);
            });
        }

        @Override
        public void step(final Steps step) {
            mStep = step.description();
            mDetail = "";
            activity().runOnUiThread(() -> {
                mUpdateStepTextView.setText(mStep);
                mUpdateDetailTextView.setText(mDetail);
            });
        }

        @Override
        public void detail(final String detail) {
            mDetail = detail;
            activity().runOnUiThread(() -> mUpdateDetailTextView.setText(mDetail));
        }

        @Override
        public void log(final String line) {
            mLog = line + "\n" + mLog;
            activity().runOnUiThread(() -> mUpdateLogTextView.setText(mLog));
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
            activity().runOnUiThread(() -> mUpdateLogTextView.setText(mLog));
        }
    };

    /**
     * Updates the Talking Book
     */
    private void performOperation(TBDeviceInfo tbDeviceInfo, String deviceSerialNumber) {
        OperationLog.Operation opLog = OperationLog.startOperation(
            mStatsOnly ? "CollectStatistics" : "UpdateTalkingBook");
        Config config = TBLoaderAppContext.getInstance().getConfig();

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
            .withCollectedDataDirectory(collectedDataTbFile)
            .withTempDirectory(tempTbFile)
            .withUserName(mUserName)
            .build();

        DeploymentInfo oldDeploymentInfo = tbDeviceInfo.createDeploymentInfo(mProject);

        getActivity().runOnUiThread(() -> mTalkingBookWarningsTextView.setText(getString(R.string.do_not_disconnect)));

        TBLoaderCore.Builder builder = new TBLoaderCore.Builder()
            .withTbLoaderConfig(tbLoaderConfig)
            .withTbDeviceInfo(tbDeviceInfo)
            .withOldDeploymentInfo(oldDeploymentInfo)
            .withLocation(mLocation)
            .withCoordinates(mCoordinates) // May be null; ok because it's optional anyway.
            .withRefreshFirmware(mRefreshFirmwareCheckBox.isChecked())
            .withProgressListener(mProgressListener)
            .withStatsOnly(mStatsOnly)
            .withPostUpdateDelay(Constants.androidPostUpdateSleepTime);

        TBLoaderCore.Result result;
        if (mStatsOnly) {
            result = builder.build().collectStatistics();
        } else {
            // The directory with images. {project}/content/{Deployment}
            File deploymentDirectory = getLocalDeploymentDirectory(mProject);
            DeploymentInfo newDeploymentInfo = getUpdateDeploymentInfo(opLog, tbDeviceInfo,
                                                        deviceSerialNumber,
                                                        collectionTimestamp, todaysDate,
                                                        collectedDataDirectory,
                                                        deploymentDirectory
                                                        );
            // Add in the update specific data, then go!
            result = builder
                .withDeploymentDirectory(new FsFile(deploymentDirectory))
                .withNewDeploymentInfo(newDeploymentInfo)
                .build().update();
        }
        opLog.put("gotstatistics", result.gotStatistics);

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

            mAppContext.getUploadService().uploadFileAsName(uploadableZipFile, collectedDataZipName);
            String message = String.format("Zipped statistics and user feedback in %s", Util.formatElapsedTime(System.currentTimeMillis()-zipStart));
            mProgressListener.log(message);
            message = String.format("TB-Loader completed in %s", Util.formatElapsedTime(System.currentTimeMillis()-startTime));
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
                                                   String deviceSerialNumber,
                                                   String collectionTimestamp, String todaysDate,
                                                   File collectedDataDirectory,
                                                   File deploymentDirectory) {
        Config config = TBLoaderAppContext.getInstance().getConfig();
        // Find the image with the community's language and/or group (such as a/b test group).
        String imageName = TBLoaderUtils.getImageForCommunity(deploymentDirectory, mCommunityDirectory);

        // What firmware comes with this Deployment?
        String firmwareRevision = TBLoaderUtils.getFirmwareVersionNumbers(deploymentDirectory);

        String recipientid = mRecipient.recipientid;

        DeploymentInfo.DeploymentInfoBuilder builder = new DeploymentInfo.DeploymentInfoBuilder()
                .withSerialNumber(deviceSerialNumber)
                .withNewSerialNumber(tbDeviceInfo.needNewSerialNumber())
                .withProjectName(mProject)
                .withDeploymentName(deploymentDirectory.getName())
                .withPackageName(imageName)
                .withUpdateDirectory(collectedDataDirectory.getName())
                .withUpdateTimestamp(todaysDate) // TODO: this should be the "Deployment date", the first date the new content is deployed.
                .withFirmwareRevision(firmwareRevision)
                .withCommunity(mCommunityDirectory)
                .withRecipientid(recipientid)
                .asTestDeployment(mTestingDeployment);
        DeploymentInfo newDeploymentInfo = builder.build();

        opLog.put("project", mProject)
            .put("deployment", deploymentDirectory.getName())
            .put("package", imageName)
            .put("recipientid", mRecipient.recipientid)
            .put("community", mCommunityDirectory)
            .put("sn", deviceSerialNumber)
            .put("tbloaderId", config.getTbcdid())
            .put("username", config.getUsername())
            .put("timestamp", collectionTimestamp);
        return newDeploymentInfo;
    }

}
