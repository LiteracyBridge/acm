package org.literacybridge.androidtbloader.checkin;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;
import org.literacybridge.androidtbloader.R;
import org.literacybridge.androidtbloader.TBLoaderAppContext;
import org.literacybridge.androidtbloader.checkin.LocationProvider.MyLocationListener;
import org.literacybridge.androidtbloader.community.ChooseCommunityActivity;
import org.literacybridge.androidtbloader.community.CommunityInfo;
import org.literacybridge.androidtbloader.community.CommunityInfoAdapter;
import org.literacybridge.androidtbloader.content.ContentManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static android.app.Activity.RESULT_OK;

/**
 * Implements the checkin screen. Listens for GPS location, and tries to find nearby communities, which
 * are then marked for update..
 *
 * The user can manually select other communities for update.
 *
 * THe user can also mark communities as being at this GPS location.
 */

public class CheckinFragment extends Fragment {
    private static final String TAG = "TBL!:" + CheckinFragment.class.getSimpleName();

    private final static int REQUEST_CODE_ADD_GPS_TO_COMMUNITY = 102;
    private final static int REQUEST_CODE_UPDATE_TODAY_COMMUNITY = 103;

    private final static String NEARBY_TAB_TAG = "nearby";
    private final static String TODAY_TAB_TAG = "today";

    private String mChosenProject;
    private List<String> mProjectList;
    private List<CommunityInfo> mNearbyCommunitiesList;
    private List<CommunityInfo> mUpdateTodayCommunitiesList;
    private CommunityInfoAdapter mNearbyCommunitiesAdapter;
    private CommunityInfoAdapter mUpdateTodayCommunitiesAdapter;

    private Location mGpsLocation;
    private KnownLocations mKnownLocations;

    private TextView mProjectLabelTextView;
    private TextView mProjectNameTextView;
    private TextView mGpsCoordinatesTextView;
    private TextView mGpsLocationTimeTextView;

    private Button mCheckinButton;

    private Button mAddCommunityToButton;
    private TabHost mTabHost;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getActivity().getIntent();
        mChosenProject = intent.getStringExtra("project");
        ContentManager mContentManager = ((TBLoaderAppContext) getActivity().getApplicationContext())
                .getContentManager();

        mProjectList = new ArrayList<>(mContentManager.getProjectNames(ContentManager.Flags.Local));
        mKnownLocations = new KnownLocations(mProjectList);

        mNearbyCommunitiesList = new ArrayList<>();
        mUpdateTodayCommunitiesList = new ArrayList<>();

        LocationProvider.getCurrentLocation(mLocationListener);//if already has permission
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_checkin, container, false);

        // The actionbar has a "title" property that is set from the activity's "label=" property
        // from the AndroidManifest file. Here, we make the toolbar work like an action bar.
        Toolbar toolbar = (Toolbar) view.findViewById(R.id.main_toolbar);
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
        // The toolbar also *contains* a TextView with an id of main_toolbar_title.
        TextView main_title = (TextView) view.findViewById(R.id.main_toolbar_title);
        main_title.setText("");

        // We want an "up" button (that is, one that points "back"), but we don't want back navigation, rather
        // to simply end this activity without setting project or community.
        ((AppCompatActivity)getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                getActivity().finish();
            }
        });

        // GPS Coordinates.
        mGpsCoordinatesTextView = (TextView) view.findViewById(R.id.checkin_gps_coordinates);
        mGpsLocationTimeTextView = (TextView) view.findViewById(R.id.checkin_gps_elapsed_time);

        // Project name and initial value.
        mProjectLabelTextView = (TextView) view.findViewById(R.id.checkin_project_label);
        mProjectNameTextView = (TextView) view.findViewById(R.id.checkin_project_name);
        mProjectNameTextView.setOnClickListener(mSelectProjectListener);
        if (mChosenProject != null) {
            mProjectNameTextView.setText(mChosenProject);
        }

        // Add community to... button.
        mAddCommunityToButton = (Button) view.findViewById(R.id.checkin_add_community);
        mAddCommunityToButton.setOnClickListener(mAddCommunityToListListener);

        // Nearby communities
        RecyclerView mNearbyCommunitiesRecyclerView = (RecyclerView) view.findViewById(
                R.id.checkin_nearby_community_groups);
        mNearbyCommunitiesRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mNearbyCommunitiesAdapter = new CommunityInfoAdapter(getActivity(), mNearbyCommunitiesList);
        mNearbyCommunitiesRecyclerView.setAdapter(mNearbyCommunitiesAdapter);

        // Other communities to update
        RecyclerView mUpdateTodayRecyclerView = (RecyclerView) view.findViewById(
                R.id.checkin_update_today_communities);
        mUpdateTodayRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mUpdateTodayCommunitiesAdapter = new CommunityInfoAdapter(getActivity(),
                mUpdateTodayCommunitiesList);
        mUpdateTodayRecyclerView.setAdapter(mUpdateTodayCommunitiesAdapter);

        mCheckinButton = (Button) view.findViewById(R.id.checkin_button);
        mCheckinButton.setOnClickListener(mCheckinListener);

        // Set up tabs between "add gps" and "today" views.
        mTabHost = (TabHost)view.findViewById(R.id.checkin_tabHost);
        mTabHost.setup();
        mTabHost.setOnTabChangedListener(mOnTabChangeListener);

        // "Communities to update today" tab. This will be the default tab.
        TabHost.TabSpec spec = mTabHost.newTabSpec(TODAY_TAB_TAG);
        spec.setContent(R.id.checkin_tab_update_today);
        spec.setIndicator("Updating Today");
        mTabHost.addTab(spec);

        // "Set GPS coordinates for community" tab.
        spec = mTabHost.newTabSpec(NEARBY_TAB_TAG);
        spec.setContent(R.id.checkin_tab_nearby);
        spec.setIndicator("At This Location");
        mTabHost.addTab(spec);

        setButtonState();

        return view;
    }

    /**
     * Handle the results of activities that we start.
     *
     * @param requestCode The requestCode that was passed to startActivityForResult.
     * @param resultCode The resultCode passed to setResult() in the other activity.
     * @param data The extra data passed to setResult() in the other activity.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_ADD_GPS_TO_COMMUNITY:
                gotCommunityForLocation(resultCode, data);
                break;
            case REQUEST_CODE_UPDATE_TODAY_COMMUNITY:
                gotCommunityForUpdateToday(resultCode, data);
                break;
        }
    }

    /**
     * Handles the results of "Choose Community" from the "Set this GPS location on communities" button.
     * @param resultCode The resultCode passed to setResult() in the "Choose Community" activity.
     * @param data The extra data passed to setResult() in the "Choose Community" activity.
     */
    private void gotCommunityForLocation(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (data.hasExtra("selected")) {
                CommunityInfo community = CommunityInfo.parseExtra(data.getStringExtra("selected"));
                Log.d(TAG,
                        String.format("Community '%s' reported at %5.2f %5.2f; now have %d here",
                                community,
                                mGpsLocation.getLatitude(),
                                mGpsLocation.getLongitude(),
                                mNearbyCommunitiesList.size()+1));
                KnownLocations.setLocationInfoFor(mGpsLocation, community);
                addNewToList(community, mNearbyCommunitiesList, mNearbyCommunitiesAdapter);
            }
        }
    }

    /**
     * Handles the results of "Choose Community" from the "Add to update today list" button.
     * @param resultCode The resultCode passed to setResult() in the "Choose Community" activity.
     * @param data The extra data passed to setResult() in the "Choose Community" activity.
     */
    private void gotCommunityForUpdateToday(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (data.hasExtra("selected")) {
                CommunityInfo community = CommunityInfo.parseExtra(data.getStringExtra("selected"));
                Log.d(TAG, String.format("Community '%s' to be updated, now have %d", community, mUpdateTodayCommunitiesList.size()+1));
                addNewToList(community,
                        mUpdateTodayCommunitiesList,
                        mUpdateTodayCommunitiesAdapter);
            }
        }
    }

    /**
     * Common logic to add a community to one of the two lists.
     * @param community The community to be added to a list.
     * @param list The list to which it should be added.
     * @param adapter The list's adapter to be notified of a data set change.
     */
    private void addNewToList(CommunityInfo community, List<CommunityInfo> list, final CommunityInfoAdapter adapter) {
        boolean newLocation = true;
        for (CommunityInfo c : list) {
            if (c.equals(community)) {
                newLocation = false;
                break;
            }
        }
        if (newLocation) {
            list.add(community);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.notifyDataSetChanged();
                    setButtonState();
                }
            });
        }
    }

    /**
     * Listener on the "Set this GPS location on communities" button. Starts the "Choose Community" activity.
     */
    private OnClickListener mAddCommunityToListListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mTabHost.getCurrentTabTag().equals(NEARBY_TAB_TAG)) {
                // List of ALL communities to set GPS locations.
                Intent intent = new Intent(getActivity(), ChooseCommunityActivity.class);
                intent.putStringArrayListExtra("projects", new ArrayList<>(mProjectList));
                // Don't offer communities already in the list.
                intent.putStringArrayListExtra("excluded", CommunityInfo.makeExtra(mNearbyCommunitiesList));
                startActivityForResult(intent, REQUEST_CODE_ADD_GPS_TO_COMMUNITY);
            } else if (mTabHost.getCurrentTabTag().equals(TODAY_TAB_TAG)) {
                // List of project communities, to add to update today list.
                List<String> projects = mChosenProject == null ? mProjectList :
                        Collections.singletonList(mChosenProject);
                Intent intent = new Intent(getActivity(), ChooseCommunityActivity.class);
                intent.putStringArrayListExtra("projects", new ArrayList<>(projects));
                // Don't offer communities already in the list.
                intent.putStringArrayListExtra("excluded", CommunityInfo.makeExtra(mUpdateTodayCommunitiesList));
                startActivityForResult(intent, REQUEST_CODE_UPDATE_TODAY_COMMUNITY);
            }
        }
    };

    private OnTabChangeListener mOnTabChangeListener = new OnTabChangeListener() {
        @Override
        public void onTabChanged(String tabId) {
            if (tabId.equals(NEARBY_TAB_TAG)){
                mAddCommunityToButton.setText(R.string.checkin_set_gps_location_button_label);
            } else if (tabId.equals(TODAY_TAB_TAG)) {
                mAddCommunityToButton.setText(R.string.checkin_updating_community_today_button_label);
            }
            setButtonState();
        }
    };

    /**
     * Listener on the "Check In" button. Return information to caller.
     */
    private OnClickListener mCheckinListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent();
            intent.putExtra("location", "Other");
            intent.putExtra("project", mChosenProject);
            intent.putStringArrayListExtra("communities", CommunityInfo.makeExtra(mUpdateTodayCommunitiesList));
            getActivity().setResult(RESULT_OK, intent);
            getActivity().finish();
        }
    };

    /**
     * Enables and disables buttons based on state; information retrieved so far.
     */
    private void setButtonState() {
        if (mChosenProject != null) {
            mProjectLabelTextView.setText(R.string.checkin_project_label);
            mProjectNameTextView.setText(mChosenProject);
        } else {
            mProjectLabelTextView.setText(String.format(getString(R.string.checkin_n_projects_format),
                    mProjectList.size()));
            mProjectNameTextView.setText(R.string.checkin_tap_to_choose_project_label);
        }
        // Enable the "Add Community To..." button if...
        boolean enabled = false;
        if (mTabHost.getCurrentTabTag().equals(NEARBY_TAB_TAG) && mGpsLocation != null) {
            // ...GPS tab, and we have GPS location.
            enabled = true;
        } else if (mTabHost.getCurrentTabTag().equals(TODAY_TAB_TAG) && mChosenProject != null){
            // ...Today tab, and we have chosen a project
            enabled = true;
        }
        mAddCommunityToButton.setEnabled(enabled);

        //mUpdateTodayAddButton.setEnabled(mChosenProject != null);
        // Enable the "Checkin" button if we have a project and one or more communities.
        enabled = mChosenProject != null && mUpdateTodayCommunitiesList.size() > 0;
        mCheckinButton.setEnabled(enabled);
    }

    /**
     * Listener for clicks on project. Lets user select a project from the ones that have been
     * downloaded. Uses the simple "pick from list" dialog.
     */
    private OnClickListener mSelectProjectListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final String[] projects = mProjectList.toArray(new String[mProjectList.size()]);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.pick_project_dialog_title)
                    .setItems(projects, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, final int which) {
                            mChosenProject = projects[which];
                            List<CommunityInfo> keepers = new ArrayList<>();
                            for (CommunityInfo info : mUpdateTodayCommunitiesList) {
                                if (mChosenProject.equalsIgnoreCase(info.getProject())) {
                                    keepers.add(info);
                                }
                            }
                            if (keepers.size() < mUpdateTodayCommunitiesList.size()) {
                                mUpdateTodayCommunitiesList.clear();
                                mUpdateTodayCommunitiesList.addAll(keepers);
                                mUpdateTodayCommunitiesAdapter.notifyDataSetChanged();
                            }
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mProjectNameTextView.setText(mChosenProject);
                                    setButtonState();
                                }
                            });
                        }
                    });
            builder.create().show();

        }
    };

    /**
     * Handles a new location from GPS. Finds communities "close" to the current location, and the
     * projects those communities are in. If only one project, pre-choose it for the user.
     * @param location The location from the GPS.
     */
    private void onGotLocation(Location location) {
        List<CommunityInfo> communities = mKnownLocations.findCommunitiesNear(location);
        Log.d(TAG, String.format("%d communities at this location: %s", communities.size(), communities.toString()));
        if (communities.size() > 0) {
            // Find all of the projects in these communities.
            Set<String> projects = new HashSet<>();
            for (CommunityInfo c : communities) {
                projects.add(c.getProject());
            }
            mNearbyCommunitiesList.addAll(communities);
            mUpdateTodayCommunitiesList.addAll(communities);
            mNearbyCommunitiesAdapter.notifyDataSetChanged();
            mUpdateTodayCommunitiesAdapter.notifyDataSetChanged();

            Log.d(TAG, String.format("Communities encompass %d projects: %s", projects.size(), projects.toString()));
            if (projects.size() == 1) {
                // Exactly one, so we know what we're getting here.
                mChosenProject = projects.iterator().next();
                setButtonState();
            }
        }
    }

    private final MyLocationListener mLocationListener = new MyLocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            Log.d(TAG, String.format("Location changed: %s", location.toString()));
            @SuppressLint("DefaultLocale")
            String locstr = String.format("%4.4f %4.4f, %5.2f, %4.0f☼",
                    location.getLatitude(), location.getLongitude(),
                    location.getAltitude(), location.getBearing());
            mGpsLocation = location;
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

}
