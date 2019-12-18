package org.literacybridge.androidtbloader.main;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserAttributes;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserCodeDeliveryDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.UpdateAttributesHandler;
import org.apache.commons.lang3.StringUtils;
import org.literacybridge.androidtbloader.BuildConfig;
import org.literacybridge.androidtbloader.R;
import org.literacybridge.androidtbloader.SettingsActivity;
import org.literacybridge.androidtbloader.TBLoaderAppContext;
import org.literacybridge.androidtbloader.checkin.CheckinActivity;
import org.literacybridge.androidtbloader.checkin.KnownLocations;
import org.literacybridge.androidtbloader.content.ContentManager;
import org.literacybridge.androidtbloader.content.ManageContentActivity;
import org.literacybridge.androidtbloader.signin.AboutApp;
import org.literacybridge.androidtbloader.signin.ChangePasswordActivity;
import org.literacybridge.androidtbloader.signin.SigninActivity;
import org.literacybridge.androidtbloader.signin.UserHelper;
import org.literacybridge.androidtbloader.talkingbook.TalkingBookConnectionManager;
import org.literacybridge.androidtbloader.tbloader.TbLoaderActivity;
import org.literacybridge.androidtbloader.uploader.UploadService;
import org.literacybridge.androidtbloader.uploader.UploadStatusActivity;
import org.literacybridge.androidtbloader.util.Config;
import org.literacybridge.androidtbloader.util.Constants;
import org.literacybridge.androidtbloader.util.Errors;
import org.literacybridge.core.fs.OperationLog;
import org.literacybridge.core.tbloader.TBDeviceInfo;
import org.literacybridge.core.tbloader.TBLoaderUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static android.app.Activity.RESULT_OK;

/**
 * Implements the main screen. From here, the signed-in user can choose what they want to do.
 */

public class MainFragment extends Fragment {
    private static final String TAG = "TBL!:" + MainFragment.class.getSimpleName();

    private static final String CANT_LAUNCH_TITLE = "Can't Start TB-Loader";
    private static final String NO_CONFIG_ERROR = "TB-Loader can not load a configuration file from server, and has no cached configuration. " +
        "This is probably a network or connectivity error. Sadly, TB-Loader must exit. ";
    private ContentManager mContentManager;

    private String configErrorMessage(Errors error) {
        return NO_CONFIG_ERROR + String.format(" Please note this error code: [%d].", error.errorNo);
    }

    private static final int REQUEST_CODE_MANAGE_CONTENT = 101;
    private static final int REQUEST_CODE_CHECKIN = 102;
    private static final int REQUEST_CODE_UPDATE_TBS = 103;
    private static final int REQUEST_CODE_UPDATE_UPLOAD_STATUS = 104;

    private TBLoaderAppContext mApplicationContext;
    private Config mConfig;

    private DrawerLayout mDrawer;

    private TextView mGreetingName;
    private TextView mGreetingEmail;

    private ViewGroup mManageGroup;
    private ViewGroup mCheckinGroup;
    private ViewGroup mUpdateGroup;
    private ViewGroup mGetStatsGroup;

    private boolean mHaveConfig = false;
    private String mUserName;
    private String mUserid;

    private String mCheckinLocation;
    private ArrayList<String> mPreselectedRecipients = new ArrayList<>();

    private TextView mUploadCountTextView;
    private TextView mUploadSizeTextView;
    private TextView mUploadNextTextView;

    private ViewGroup mTbGroup;
    private TextView mTbStatus;
    private TextView mTbId;
    private TextView mTbContent;

    private AlertDialog userDialog;
    private ProgressDialog waitDialog;

    // We also wait until mHaveConfig is true.
    private boolean awaitingLocationPermission = true;

    private boolean mTestingDeployment = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mApplicationContext = (TBLoaderAppContext)getActivity().getApplicationContext();
        mContentManager = mApplicationContext.getContentManager();
        mConfig = mApplicationContext.getConfig();
        mUserid = UserHelper.getUserId();
        mUserName = UserHelper.getUsername();
        mApplicationContext.getTalkingBookConnectionManager().canAccessConnectedDevice();

        OperationLog.Operation opLog = OperationLog.log("MainFragment.onCreate")
            .put("externalStorageAvailable",
                 TBLoaderUtils.getBytesString(new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath()).getAvailableBytes()))
            .put("internalStorageAvailable",
                 TBLoaderUtils.getBytesString(new StatFs(Environment.getDataDirectory().getAbsolutePath()).getAvailableBytes()))
            .put("tempStorageAvailable",
                 TBLoaderUtils.getBytesString(new StatFs(Environment.getDownloadCacheDirectory().getAbsolutePath()).getAvailableBytes()));
        opLog.put("phonemake", Build.MANUFACTURER)
            .put("phonemodel", Build.MODEL)
            .put("android", Build.VERSION.RELEASE)
            .put("tbloader", BuildConfig.VERSION_NAME);
        opLog.finish();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_main, container, false);

        // No user id. Go back to SigninActivity.
        if (mUserid == null || mUserid.length() == 0) {
            Log.d(TAG, "No user id, activating signin.");
            backToSignin();
            return null;
        }

        mGreetingName = view.findViewById(R.id.main_greeting_label);
        mGreetingEmail = view.findViewById(R.id.main_greeting_email);

        mManageGroup = view.findViewById(R.id.main_manage_content_group);
        mCheckinGroup = view.findViewById(R.id.main_checkin_group);
        mUpdateGroup = view.findViewById(R.id.main_update_talking_books_group);
        mGetStatsGroup = view.findViewById(R.id.main_get_stats_talking_books_group);

        mManageGroup.setOnClickListener(manageListener);
        mCheckinGroup.setOnClickListener(checkinListener);
        mUpdateGroup.setOnClickListener(updateListener);
        mGetStatsGroup.setOnClickListener(getStatsListener);

        mTbGroup = view.findViewById(R.id.main_tb_status_group);
        mTbStatus = view.findViewById(R.id.main_tb_status);
        mTbId = view.findViewById(R.id.main_tb_id);
        mTbContent = view.findViewById(R.id.main_tb_content);

        mUploadCountTextView = view.findViewById(R.id.main_count_uploads);
        mUploadSizeTextView = view.findViewById(R.id.main_size_uploads);
        mUploadNextTextView = view.findViewById(R.id.main_next_upload);

        mUploadCountTextView.setOnClickListener(uploadListener);

        // Set toolbar for this screen. By default, has a title from the application manifest
        // application.label property.
        Toolbar toolbar = view.findViewById(R.id.main_toolbar);
        // This title is independent of the default title, defined in the main_tool_bar.xml file
        TextView main_title = view.findViewById(R.id.main_toolbar_title);
        main_title.setText("");
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);

        // Set navigation drawer for this screen.  Note that R.id.main_drawer_layout is in the
        // $.layout.activity_main (.xml) file.
        mDrawer = view.findViewById(R.id.main_drawer_layout);
        ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(getActivity(), mDrawer,
                toolbar, R.string.nav_drawer_open, R.string.nav_drawer_close);
        mDrawer.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();
        NavigationView nDrawer = view.findViewById(R.id.nav_view);
        nDrawer.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Log.d(TAG, String.format("Perform action for %s", item.toString()));
                performAction(item);
                return true;
            }
        });

        setButtonState();
        showWaitDialog("Loading...");
        getUserDetails();

        fillUploadValues();
        showGreeting();

        mContentManager.fetchContentList();

        KnownLocations.refreshCommunityLocations(new Config.Listener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Got location config");
            }

            @Override
            public void onError() {
                Log.d(TAG, "No location config");
            }
        });

        getLocationPermission();

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.activity_main_drawer, menu);
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(mApplicationContext).unregisterReceiver(mMessageReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        fillUploadValues();
        IntentFilter filter = new IntentFilter();
        filter.addAction(UploadService.UPLOADER_STATUS_EVENT);
        filter.addAction(ContentManager.CONTENT_LIST_CHANGED_EVENT);
        filter.addAction(TalkingBookConnectionManager.TB_CONNECTION_STATUS);
        LocalBroadcastManager.getInstance(mApplicationContext).registerReceiver(
                mMessageReceiver, filter);
        updateTBStatus();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
        case REQUEST_CODE_MANAGE_CONTENT:
            if (resultCode == RESULT_OK) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setButtonState();
                    }
                });
            }
            break;
        case REQUEST_CODE_CHECKIN:
            if (resultCode == RESULT_OK) {
                if (data.hasExtra(Constants.LOCATION)) {
                    mCheckinLocation = data.getStringExtra(Constants.LOCATION);
                }
                if (data != null && data.hasExtra(Constants.PRESELECTED_RECIPIENTS)) {
                    mPreselectedRecipients = data.getStringArrayListExtra(Constants.PRESELECTED_RECIPIENTS);
                } else {
                    mPreselectedRecipients = new ArrayList<>();
                }
                if (data.hasExtra(Constants.TESTING_DEPLOYMENT)) {
                    mTestingDeployment = data.getBooleanExtra(Constants.TESTING_DEPLOYMENT, false);
                }
                getActivity().runOnUiThread(this::setButtonState);
            }
            break;
        case REQUEST_CODE_UPDATE_TBS:
            fillUploadValues();
            break;
        case REQUEST_CODE_UPDATE_UPLOAD_STATUS:
            break;
        default:
            break;
        }
    }

    private void checkStartupInformationComplete() {
        if (!mHaveConfig || awaitingLocationPermission) {
            return;
        }
        closeWaitDialog();
        // The UI is all set up, so now let the upload service start.
        UploadService.startUploadService();
    }

    @SuppressLint("SetTextI18n")
    private void showGreeting() {
        String greeting = mConfig.getGreeting();
        if (StringUtils.isEmpty(greeting)) {
            greeting = getString(R.string.main_greeting_default);
        }
        String email = mConfig.getEmail();
        mGreetingName.setText(greeting);
        mGreetingEmail.setText(email);
    }

    /**
     * Perform the action for the selected navigation / menu item. Note that some of these
     * are repeated from the signin screen.
     * @param item The Menu item selected.
     */
    private void performAction(MenuItem item) {
        // Close the navigation drawer
        mDrawer.closeDrawers();

        // Find which item was selected
        switch(item.getItemId()) {
            case R.id.nav_user_sign_out:
                UserHelper.getPool().getUser(mUserid).signOut();
                UserHelper.getCredentialsProvider(getActivity().getApplicationContext()).clear();
                mConfig.onSignOut();
                Intent intent = new Intent();
                intent.putExtra(Constants.SIGNOUT, true);
                getActivity().setResult(RESULT_OK, intent);
                getActivity().finish();
                break;

            case R.id.nav_show_upload_status:
                Intent userActivity = new Intent(getActivity(), UploadStatusActivity.class);
                userActivity.putExtra(Constants.USERID, mUserid);
                startActivityForResult(userActivity, REQUEST_CODE_UPDATE_UPLOAD_STATUS);
                break;

            case R.id.nav_user_change_password:
                // Change password
                Intent changePssActivity = new Intent(getActivity(), ChangePasswordActivity.class);
                startActivity(changePssActivity);
                break;

            case R.id.nav_user_settings:
                // Change password
                Intent settingsActivity = new Intent(getActivity(), SettingsActivity.class);
                startActivity(settingsActivity);
                break;

            case R.id.nav_main_about:
                // For the inquisitive
                Intent aboutAppActivity = new Intent(getActivity(), AboutApp.class);
                startActivity(aboutAppActivity);
                break;

            case R.id.nav_user_edit_greeting:
                editPreferredGreeting(mConfig.getGreeting());
                break;


        }
    }

    private void backToSignin() {
        Intent startNewIntent = new Intent(mApplicationContext, SigninActivity.class);
        startNewIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startNewIntent);
        getActivity().finish();
    }

    /**
     * Get the Cognito user details (username, greeting, email address).
     */
    private void getUserDetails() {
        final OperationLog.Operation opLog = OperationLog.startOperation("GetUserDetails");

        Map<String,String> mUserDetails = UserHelper.getAuthenticationPayload();
        mConfig.applyUserDetails(mUserDetails);

        if (mUserDetails.size() > 0) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showGreeting();
                    setButtonState();
                }
            });
        }

        checkStartupInformationComplete();

        mConfig.prepareForSerialNumberAllocation(new Config.Listener() {
            @Override
            public void onSuccess() {
                mHaveConfig = true;
                opLog.finish(mUserDetails);
                checkStartupInformationComplete();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setButtonState();
                    }
                });
            }

            @Override
            public void onError() {
                // This is a fatal error.
                opLog.put("failed", true).finish();
                showDialogMessage(CANT_LAUNCH_TITLE, configErrorMessage(Errors.NoConfig), true);
            }
        });
    }

    private OnClickListener manageListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            doManage();
        }
    };

    private void doManage() {
        Intent userActivity = new Intent(getActivity(), ManageContentActivity.class);
        userActivity.putExtra(Constants.NAME, mUserid);
        startActivityForResult(userActivity, REQUEST_CODE_MANAGE_CONTENT);
    }

    private OnClickListener checkinListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent userActivity = new Intent(getActivity(), CheckinActivity.class);
            userActivity.putStringArrayListExtra(Constants.PRESELECTED_RECIPIENTS, (ArrayList<String>) mPreselectedRecipients);
            userActivity.putExtra(Constants.NAME, mUserid);
            startActivityForResult(userActivity, REQUEST_CODE_CHECKIN);
        }
    };

    private OnClickListener uploadListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent userActivity = new Intent(getActivity(), UploadStatusActivity.class);
            userActivity.putExtra(Constants.USERID, mUserid);
            startActivityForResult(userActivity, REQUEST_CODE_UPDATE_UPLOAD_STATUS);
        }
    };

    private OnClickListener updateListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            doUpdate(false);
        }
    };

    private OnClickListener getStatsListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            doUpdate(true);
        }
    };

    private void doUpdate(boolean statsOnly) {
        Intent userActivity = new Intent(getActivity(), TbLoaderActivity.class);
        userActivity.putExtra(Constants.USERNAME, mUserName);
        userActivity.putExtra(Constants.STATSONLY, statsOnly);
        userActivity.putExtra(Constants.TESTING_DEPLOYMENT, mTestingDeployment);
        if (statsOnly) {
            userActivity.putExtra(Constants.LOCATION, "other");
        } else {
            userActivity.putStringArrayListExtra(Constants.PRESELECTED_RECIPIENTS, (ArrayList<String>) mPreselectedRecipients);
            userActivity.putExtra(Constants.LOCATION, mCheckinLocation);
        }
        startActivityForResult(userActivity, REQUEST_CODE_UPDATE_TBS);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(UploadService.UPLOADER_STATUS_EVENT)) {
                // Get extra data included in the Intent
                String name = intent.getStringExtra(Constants.NAME);
                int count = intent.getIntExtra("count", 0);
                long size = intent.getLongExtra("size", 0);
                Log.d(TAG,
                    String.format("got updateProgress, count:%d, size:%d, name:%s", count, size,
                        name));
                fillUploadValues(count, size);
            } else if (intent.getAction().equals(ContentManager.CONTENT_LIST_CHANGED_EVENT)) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setButtonState();
                    }
                });
            } else if (intent.getAction().equals(TalkingBookConnectionManager.TB_CONNECTION_STATUS)) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateTBStatus();
                    }
                });
            }
        }
    };

    private void fillUploadValues() {
        UploadService uploadService = mApplicationContext.getUploadService();
        int count = uploadService.getUploadCount();
        long size = uploadService.getUploadSize();
        fillUploadValues(count, size);
    }

    private void fillUploadValues(int count, long size) {
        if (count > 0) {
            mUploadCountTextView.setText(String.format(getString(R.string.main_n_stats_files_to_upload), TBLoaderUtils
                .getBytesString(size), count));
            //mUploadSizeTextView.setText(String.format(getString(R.string.main_n_bytes_to_upload), Util.getBytesString(size)));
            mUploadSizeTextView.setVisibility(View.GONE);
            //mUploadNextTextView.setText(name);
            mUploadNextTextView.setVisibility(View.GONE);
        } else {
            mUploadCountTextView.setText(getString(R.string.main_no_stats_files_to_upload));
            mUploadSizeTextView.setVisibility(View.GONE);
            mUploadNextTextView.setVisibility(View.GONE);
        }
    }

    private void setButtonState() {
        boolean canManage = mHaveConfig;
        boolean canCheckin = mHaveConfig && mContentManager.haveContentInfo();
        boolean canUpdate = canCheckin && StringUtils.isNotEmpty(mApplicationContext.getProject());

        mManageGroup.setAlpha(canManage ? 1.0f : 0.33f);
        mManageGroup.setEnabled(canManage);

        mCheckinGroup.setAlpha(canCheckin ? 1.0f : 0.33f);
        mCheckinGroup.setEnabled(canCheckin);

        mUpdateGroup.setAlpha(canUpdate ? 1.0f : 0.33f);
        mUpdateGroup.setEnabled(canUpdate);

        mGetStatsGroup.setAlpha(canCheckin ? 1.0f : 0.33f);
        mGetStatsGroup.setEnabled(canCheckin);
    }

    private void updateTBStatus() {
        TalkingBookConnectionManager.TalkingBook talkingBook = mApplicationContext.getTalkingBookConnectionManager().getConnectedTalkingBook();
        if (talkingBook == null) {
            mTbStatus.setText(getString(R.string.no_talking_book_connected));
            mTbId.setVisibility(View.GONE);
            mTbContent.setVisibility(View.GONE);
            mTbGroup.setBackgroundColor(getResources().getColor(R.color.white));
        } else {
            TBDeviceInfo deviceInfo = new TBDeviceInfo(talkingBook.getTalkingBookRoot(),
                talkingBook.getDeviceLabel(),
                "b-");
            String recipientidStr = "";
            if (deviceInfo.getRecipientid()!=null) {
                recipientidStr = String.format("(id: %s)", deviceInfo.getRecipientid());
            }
            mTbStatus.setText(String.format("%s connected, currently:", talkingBook.getSerialNumber()));
            mTbId.setText(String.format(" Community: %s %s", deviceInfo.getCommunityName(), recipientidStr));
            mTbContent.setText(String.format(" Deployment: %s / %s", deviceInfo.getProjectName(), deviceInfo.getDeploymentName()));
            mTbId.setVisibility(View.VISIBLE);
            mTbContent.setVisibility(View.VISIBLE);
            mTbGroup.setBackgroundColor(getResources().getColor(R.color.TB_CONNECTED_FIELD));
        }
    }

    private void editPreferredGreeting(final String attributeValue) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Preferred Greeting");
        final EditText input = new EditText(getActivity());
        input.setText(attributeValue);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);

        input.setLayoutParams(lp);
        input.requestFocus();
        builder.setView(input);

        builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    String newValue = input.getText().toString();
                    if(!newValue.equals(attributeValue)) {
                        showWaitDialog("Updating...");
                        updateAttribute(UserHelper.getSignUpFieldsC2O().get("Preferred Greeting"), newValue);
                    }
                    userDialog.dismiss();
                } catch (Exception e) {
                    // Log failure
                }
            }
        });
        builder.setNegativeButton("CANCEL",
                                  new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int which) {}});
        userDialog = builder.create();
        userDialog.show();
    }

    // Update attributes
    private void updateAttribute(String attributeType, String attributeValue) {
        UpdateAttributesHandler updateHandler = new UpdateAttributesHandler() {
            @Override
            public void onSuccess(List<CognitoUserCodeDeliveryDetails> attributesVerificationList) {
                // Update successful
                closeWaitDialog();
                if(attributesVerificationList.size() > 0) {
                    showDialogMessage("Updated", "The updated attributes has to be verified",  false);
                }
                if (attributeType.equals(UserHelper.getSignUpFieldsC2O().get("Preferred Greeting"))) {
                    mApplicationContext.getConfig().updateGreeting(attributeValue);
                }
                showGreeting();
            }

            @Override
            public void onFailure(Exception exception) {
                // Update failed
                closeWaitDialog();
                showDialogMessage("Update failed", UserHelper.formatException(exception), false);
            }
        };

        if(attributeType == null || attributeType.length() < 1) {
            closeWaitDialog();
            return;
        }
        CognitoUserAttributes updatedUserAttributes = new CognitoUserAttributes();
        updatedUserAttributes.addAttribute(attributeType, attributeValue);
        showWaitDialog("Updating...");
        UserHelper.getPool().getUser(UserHelper.getUserId()).updateAttributesInBackground(updatedUserAttributes, updateHandler);
    }

    private void closeUserDialog() {
        if (userDialog != null) {
            userDialog.dismiss();
        }
        userDialog = null;
    }

    private void showDialogMessage(String title, String body, final Runnable okCallback, final Runnable cancelCallback) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title)
            .setMessage(body)
            .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        closeUserDialog();
                    } catch (Exception e) {
                        // Log failure
                        Log.e(TAG,"Dialog dismiss failed");
                    } finally {
                        okCallback.run();
                    }
                }
            });
        if (cancelCallback != null) {
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        closeUserDialog();
                    } catch (Exception e) {
                        // Log failure
                        Log.e(TAG,"Dialog dismiss failed");
                    } finally {
                        cancelCallback.run();
                    }
                }
            });
        }
        closeWaitDialog();
        userDialog = builder.create();
        userDialog.show();
    }

    private void showDialogMessage(String title, String body, Runnable okCallback) {
        showDialogMessage(title, body, okCallback, null);
    }

    private void showDialogMessage(String title, String body, final boolean exit) {
        showDialogMessage(title, body, new Runnable(){
            @Override
            public void run() {
                if (exit) {
                    Intent intent = new Intent();
                    intent.putExtra(Constants.EXIT_APPLICATION, true);
                    getActivity().setResult(RESULT_OK, intent);
                    getActivity().finish();
                }
            }
        });
    }

    private void closeWaitDialog() {
        try {
            if (waitDialog != null)
                waitDialog.dismiss();
        }
        catch (Exception e) {
            //
        }
        waitDialog = null;
    }
    private void showWaitDialog(String message) {
        closeWaitDialog();
        waitDialog = new ProgressDialog(getActivity());
        waitDialog.setTitle(message);
        waitDialog.show();
    }

    final private int REQUEST_CODE_ASK_LOCATION_PERMISSIONS = 123;

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
        case REQUEST_CODE_ASK_LOCATION_PERMISSIONS:
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Accepted
                awaitingLocationPermission = false;
                checkStartupInformationComplete();
            } else {
                // Denied
                showDialogMessage("Location Permission Required", "The TB-Loader requires Location permission to run.", true);
            }
            break;
        default:
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    private void getLocationPermission() {
        Log.v(TAG, "handlePermissionsAndGetLocation");
        int hasWriteContactsPermission;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            hasWriteContactsPermission = getActivity().checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION);
            if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                   REQUEST_CODE_ASK_LOCATION_PERMISSIONS);
                return;
            }
        }
        awaitingLocationPermission = false;
        checkStartupInformationComplete();
    }

}
