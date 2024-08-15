package org.literacybridge.archived_androidtbloader.checkin;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import org.literacybridge.archived_androidtbloader.R;
import org.literacybridge.archived_androidtbloader.TBLoaderAppContext;
import org.literacybridge.archived_androidtbloader.checkin.LocationProvider.MyLocationListener;
import org.literacybridge.archived_androidtbloader.community.CommunityInfo;
import org.literacybridge.archived_androidtbloader.content.ContentManager;
import org.literacybridge.archived_androidtbloader.recipient.RecipientChooserActivity;
import org.literacybridge.archived_androidtbloader.util.Constants;
import org.literacybridge.core.spec.ProgramSpec;
import org.literacybridge.core.spec.RecipientList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static android.app.Activity.RESULT_OK;

/**
 * Implements the checkin screen. Listens for GPS location, and tries to find nearby communities, which
 * are then marked for update..
 * <p>
 * The user can manually select other communities for update.
 * <p>
 * THe user can also mark communities as being at this GPS location.
 */

public class CheckinFragment extends Fragment {
    private static final String TAG = "TBL!:" + CheckinFragment.class.getSimpleName();

    private final static int REQUEST_CODE_PRESELECT_RECIPIENTS = 101;

    private String mChosenProject;
    private List<String> mProjectList;
    private List<String> mPreselectedRecipients = new ArrayList<>();

    private KnownLocations mKnownLocations;

    private TextView mProjectLabelTextView;
    private TextView mProjectNameTextView;
    private TextView mGpsCoordinatesTextView;
    private TextView mGpsLocationTimeTextView;

    private TextView mRecipientTextView;

    private CheckBox mTestDeploymentCheckBox;
    private Button mCheckinButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getActivity().getIntent();
        if (intent.hasExtra(Constants.PRESELECTED_RECIPIENTS)) {
            mPreselectedRecipients = intent.getStringArrayListExtra(Constants.PRESELECTED_RECIPIENTS);
        }

        ContentManager mContentManager = ((TBLoaderAppContext) getActivity().getApplicationContext())
            .getContentManager();

        mProjectList = new ArrayList<>(mContentManager.getProjectNames(ContentManager.Flags.Local));
        mKnownLocations = new KnownLocations(mProjectList);

        LocationProvider.getCurrentLocation(mLocationListener);//if already has permission
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
        ViewGroup container,
        Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.activity_checkin, container, false);

        // The actionbar has a "title" property that is set from the activity's "label=" property
        // from the AndroidManifest file. Here, we make the toolbar work like an action bar.
        Toolbar toolbar = view.findViewById(R.id.main_toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        // The toolbar also *contains* a TextView with an id of main_toolbar_title.
        TextView main_title = view.findViewById(R.id.main_toolbar_title);
        main_title.setText("");

        // We want an "up" button (that is, one that points "back"), but we don't want back navigation, rather
        // to simply end this activity without setting project or community.
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(view1 -> getActivity().finish());

        // GPS Coordinates.
        mGpsCoordinatesTextView = view.findViewById(R.id.checkin_gps_coordinates);
        mGpsLocationTimeTextView = view.findViewById(R.id.checkin_gps_elapsed_time);

        // Project name and initial value.
        mProjectLabelTextView = view.findViewById(R.id.checkin_project_label);
        mProjectNameTextView = view.findViewById(R.id.checkin_project_name);
        mProjectNameTextView.setOnClickListener(mSelectProjectListener);
        if (getChosenProject() != null) {
            mProjectNameTextView.setText(getChosenProject());
        }

        // Recipient display
        ViewGroup mRecipientGroup = view.findViewById(R.id.checkin_recipient_group);
        mRecipientTextView = view.findViewById(R.id.checkin_recipient_display);
        mRecipientGroup.setOnClickListener(mPreselectRecipientsListener);

        mTestDeploymentCheckBox = view.findViewById(R.id.deploying_for_testing);

        mCheckinButton = view.findViewById(R.id.checkin_button);
        mCheckinButton.setOnClickListener(mCheckinListener);

        setButtonState();

        return view;
    }

    /**
     * Handle the results of activities that we start.
     *
     * @param requestCode The requestCode that was passed to startActivityForResult.
     * @param resultCode  The resultCode passed to setResult() in the other activity.
     * @param data        The extra data passed to setResult() in the other activity.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
        case REQUEST_CODE_PRESELECT_RECIPIENTS:
            if (data != null && data.hasExtra(Constants.PRESELECTED_RECIPIENTS)) {
                mPreselectedRecipients = data.getStringArrayListExtra(Constants.PRESELECTED_RECIPIENTS);
                fillPreselectionDisplay();
            }
            break;
        }
    }

    private void getPreselectedRecipients() {
        if (this.mChosenProject != null) {
            Intent intent = new Intent(getActivity(), RecipientChooserActivity.class);
            intent.putExtra("preselect", true);
            intent.putStringArrayListExtra(Constants.PRESELECTED_RECIPIENTS, (ArrayList<String>) mPreselectedRecipients);
            startActivityForResult(intent, REQUEST_CODE_PRESELECT_RECIPIENTS);
        }
    }

    private RecipientList getRecipientList() {
        ProgramSpec programSpec = ((TBLoaderAppContext) getActivity().getApplicationContext()).getProgramSpec();
        return programSpec.getRecipients();
    }

    private void fillPreselectionDisplay() {
        int level = 0;
        StringBuilder result = new StringBuilder();
        StringBuilder gap = new StringBuilder("\n  ");
        for (String name : mPreselectedRecipients) {
            if (name.length() == 0) {
                StringBuilder noName = new StringBuilder();
                RecipientList recipients = getRecipientList();
                // There is no name at this level. Is that because there was nothing to choose
                // from, or because user chose "null value"?
                List<String> path = mPreselectedRecipients.subList(0, level);
                Collection<String> values = recipients.getChildrenOfPath(path);
                if (values.size() > 1) {
                    // There are values at this level, but user has chosen an empty one.
                    noName.append("-- no associated ")
                        .append(recipients.getNameOfLevel(level))
                        .append(" --");
                } else {
                    // There are either no values at all, or the only one is empty.
                    noName.append("-- has no ").append(recipients.getPluralOfLevel(level));
                }
                name = noName.toString();
            }
            result.append(name).append(gap);
            gap.append("  ");
            level++;
        } for (; level <= 6; level++)
            result.append(gap);
        mRecipientTextView.setText(result.toString());
    }

    /**
     * Listener on the "Check In" button. Return information to caller.
     */
    private OnClickListener mCheckinListener = v -> doCheckin();

    private void doCheckin() {
        Intent intent = new Intent();
        intent.putExtra("location", "Other");
        intent.putExtra(Constants.PROJECT, getChosenProject());
        intent.putExtra(Constants.TESTING_DEPLOYMENT, mTestDeploymentCheckBox.isChecked());
        intent.putStringArrayListExtra(Constants.PRESELECTED_RECIPIENTS, (ArrayList<String>) mPreselectedRecipients);
        getActivity().setResult(RESULT_OK, intent);
        getActivity().finish();
    }

    /**
     * Enables and disables buttons based on state; information retrieved so far.
     */
    private void setButtonState() {
        if (getChosenProject() != null) {
            mProjectLabelTextView.setText(R.string.checkin_project_label);
            mProjectNameTextView.setText(getChosenProject());
        } else {
            mProjectLabelTextView.setText(String.format(getString(R.string.checkin_n_projects_format),
                mProjectList.size()));
            mProjectNameTextView.setText(R.string.checkin_tap_to_choose_project_label);
        }

        //mUpdateTodayAddButton.setEnabled(mChosenProject != null);
        // Enable the "Checkin" button if we have a project and one or more communities.
        boolean enabled = getChosenProject() != null;
        mCheckinButton.setEnabled(enabled);
    }

    private OnClickListener mPreselectRecipientsListener = v -> getPreselectedRecipients();

    /**
     * Listener for clicks on project. Lets user select a project from the ones that have been
     * downloaded. Uses the simple "pick from list" dialog.
     */
    private OnClickListener mSelectProjectListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final String[] projects = mProjectList.toArray(new String[0]);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.pick_project_dialog_title)
                .setItems(projects, (dialog, which) -> {
                    setChosenProject(projects[which]);

                    getActivity().runOnUiThread(() -> {
                        mProjectNameTextView.setText(getChosenProject());
                        setButtonState();
                    });
                });
            builder.create().show();

        }
    };

    /**
     * Handles a new location from GPS. Finds communities "close" to the current location, and the
     * projects those communities are in. If only one project, pre-choose it for the user.
     *
     * @param location The location from the GPS.
     */
    private void onGotLocation(Location location) {
        List<CommunityInfo> communities = mKnownLocations.findCommunitiesNear(location);
        Log.d(TAG,
            String.format("%d communities at this location: %s",
                communities.size(),
                communities.toString()));
        if (communities.size() > 0) {
            // Find all of the projects in these communities.
            Set<String> projects = new HashSet<>();
            for (CommunityInfo c : communities) {
                projects.add(c.getProject());
            }

            Log.d(TAG,
                String.format("Communities encompass %d projects: %s",
                    projects.size(),
                    projects.toString()));
            if (projects.size() == 1) {
                // Exactly one, so we know what we're getting here.
                setChosenProject(projects.iterator().next());
                setButtonState();
            }
        }
    }

    private final MyLocationListener mLocationListener = new MyLocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            Log.d(TAG, String.format("Location changed: %s", location.toString()));
            @SuppressLint("DefaultLocale")
            String locstr = String.format("%+3.5f %+3.5f, %5.2f, %4.0f☼",
                location.getLatitude(),
                location.getLongitude(),
                location.getAltitude(),
                location.getBearing());
            if (mGpsCoordinatesTextView != null) {
                mGpsCoordinatesTextView.setText(locstr);
            }

            onGotLocation(location);
        }

        @SuppressLint("DefaultLocale")
        @Override
        public void onLocationChanged(Location location, String provider, long nanos) {
            onLocationChanged(location);
            String strTime;
            if (nanos > 1000000) {
                strTime = String.format("%s: %.1f ms", provider, nanos / 1e6);
            } else {
                strTime = String.format("%s: %.1f μs", provider, nanos / 1e3);
            }
            mGpsLocationTimeTextView.setText(strTime);
        }
    };

    private String getChosenProject() {
        return mChosenProject;
    }

    private void setChosenProject(String chosenProject) {
        this.mChosenProject = chosenProject;
//        this.mPreselectedRecipients.clear();
        ((TBLoaderAppContext) getActivity().getApplicationContext()).setProject(chosenProject);
        if (chosenProject != null) {
            getPreselectedRecipients();
        }
    }
}
