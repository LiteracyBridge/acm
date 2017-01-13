package org.literacybridge.androidtbloader.checkin;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.literacybridge.androidtbloader.R;
import org.literacybridge.androidtbloader.TBLoaderAppContext;
import org.literacybridge.androidtbloader.community.ChooseCommunityActivity;
import org.literacybridge.androidtbloader.content.ContentManager;

import java.util.ArrayList;
import java.util.Arrays;
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
    private static final String TAG = CheckinFragment.class.getSimpleName();

    private final int REQUEST_CODE_ADD_GPS_TO_COMMUNITY = 102;
    private final int REQUEST_CODE_UPDATE_TODAY_COMMUNITY = 103;

    private ContentManager mContentManager;
    private String mUser;
    private String mChosenProject;
    private List<String> mTodayProjectList;
    private List<String> mProjectList;
    private List<String> mNearbyCommunitiesList;
    private List<String> mUpdateTodayCommunitiesList;
    private ArrayAdapter<String> mNearbyCommunitiesAdapter;
    private ArrayAdapter<String> mUpdateTodayCommunitiesAdapter;

    private Location mGpsLocation;
    private KnownLocations mKnownLocations;

    private TextView mProjectLabelTextView;
    private TextView mProjectNameTextView;
    private TextView mGpsCoordinatesTextView;
    private TextView mGpsLocationTimeTextView;

    private ImageButton mGoButton;
    private Button mGoButton2;

    private TextView mNearbyCommunitiesTextView;
    private ListView mNearbyCommunitiesListView;
    private Button mAddGpsToCommunityButton;
    private TextView mUpdateTodayTextView;
    private ListView mUpdateTodayListView;
    private Button mUpdateTodayAddButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getActivity().getIntent();
        mUser = intent.getStringExtra("user");
        mChosenProject = intent.getStringExtra("project");
        mContentManager = ((TBLoaderAppContext) getActivity().getApplicationContext()).getContentManager();

        mProjectList = new ArrayList<>(mContentManager.getProjectNames(ContentManager.Flags.Local));
        mKnownLocations = new KnownLocations(mProjectList);

        mNearbyCommunitiesList = new ArrayList<>();
        mUpdateTodayCommunitiesList = new ArrayList<>();

        setupLocation();
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

        // We want a "back" button (sometimes called "up"), but we don't want back navigation, but
        // to simply end this activity without setting project or community.
        toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
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
        mProjectNameTextView.setOnClickListener(mProjectListener);
        if (mChosenProject != null) {
            mProjectNameTextView.setText(mChosenProject);
        }

        // Nearby communities
        mNearbyCommunitiesTextView = (TextView) view.findViewById(R.id.checkin_nearby_label);
        mNearbyCommunitiesListView = (ListView) view.findViewById(R.id.checkin_nearby_community_groups);
        mNearbyCommunitiesAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_list_item_1,
                mNearbyCommunitiesList);
        mNearbyCommunitiesListView.setAdapter(mNearbyCommunitiesAdapter);
        mAddGpsToCommunityButton = (Button) view.findViewById(R.id.checkin_add_gps_to_community);
        mAddGpsToCommunityButton.setOnClickListener(mAddGpsToCommunityListener);
        mAddGpsToCommunityButton.setEnabled(false);

        // Other communities to update
        mUpdateTodayTextView = (TextView) view.findViewById(R.id.checkin_update_today_label);
        mUpdateTodayListView = (ListView) view.findViewById(R.id.checkin_update_today_communities);
        mUpdateTodayCommunitiesAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_list_item_1,
                mUpdateTodayCommunitiesList);
        mUpdateTodayListView.setAdapter(mUpdateTodayCommunitiesAdapter);
        mUpdateTodayAddButton = (Button) view.findViewById(R.id.checkin_update_today_add_community);
        mUpdateTodayAddButton.setOnClickListener(mUpdateTodayAddListener);

        mGoButton = (ImageButton) view.findViewById(R.id.checkin_button);
        mGoButton.setOnClickListener(mCheckinListener);
        mGoButton2 = (Button) view.findViewById(R.id.checkin_button_2);
        mGoButton2.setOnClickListener(mCheckinListener);

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
                String community = data.getStringExtra("selected");
                Log.d(TAG,
                        String.format("Community '%s' reported at %5.2f %5.2f; now have %d here",
                                community,
                                mGpsLocation.getLongitude(),
                                mGpsLocation.getLatitude(),
                                mNearbyCommunitiesList.size()+1));
                // TODO: Which project community did they choose?
                mKnownLocations.setLocationInfoFor(mGpsLocation, community, mChosenProject);
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
                String community = data.getStringExtra("selected");
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
    private void addNewToList(String community, List<String> list, final ArrayAdapter adapter) {
        boolean newLocation = true;
        for (String c : list) {
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
    private OnClickListener mAddGpsToCommunityListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // List of ALL communities to set GPS locations.
            ArrayList<String> list = new ArrayList<>();
            for (Set<String> communities : mContentManager.getCommunitiesInProjects().values()) {
                list.addAll(communities);
            }
            Intent intent = new Intent(getActivity(), ChooseCommunityActivity.class);
            intent.putExtra("list", list);
            startActivityForResult(intent, REQUEST_CODE_ADD_GPS_TO_COMMUNITY);
        }
    };

    /**
     * Listener on the "Add to update today list" button. Starts the "Choose Community" activity.
     */
    private OnClickListener mUpdateTodayAddListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // List of project communities, to add to update today list.
            List<String> projects = mChosenProject != null ? Arrays.asList(mChosenProject) : mProjectList;
            List<String> list = mContentManager.getCommunitiesInProjects(projects);
            Intent intent = new Intent(getActivity(), ChooseCommunityActivity.class);
            intent.putExtra("list", new ArrayList<>(list));
            startActivityForResult(intent, REQUEST_CODE_UPDATE_TODAY_COMMUNITY);
        }
    };

    /**
     * Listener on the main button. Return information to caller.
     */
    private OnClickListener mCheckinListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent();
            intent.putExtra("location", "Other");
            intent.putExtra("project", mChosenProject);
            intent.putStringArrayListExtra("communities", (ArrayList<String>)mUpdateTodayCommunitiesList);
            getActivity().setResult(RESULT_OK, intent);
            getActivity().finish();
        }
    };

    /**
     * Enables and disables buttons based on state; information retrieved so far.
     */
    private void setButtonState() {
        if (mChosenProject != null) {
            mProjectLabelTextView.setText("Project");
            mProjectNameTextView.setText(mChosenProject);
        } else {
            mProjectLabelTextView.setText(String.format("%d projects",
                    mProjectList.size()));
            mProjectNameTextView.setText("Tap to choose project.");
        }
        mUpdateTodayAddButton.setEnabled(mChosenProject != null);
        mGoButton.setEnabled(mChosenProject != null);
        mGoButton2.setEnabled(mChosenProject != null);
    }

    /**
     * Listener for clicks on project. Lets user select a project from the ones that have been
     * downloaded. Uses the simple "pick from list" dialog.
     */
    private OnClickListener mProjectListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final String[] projects = mProjectList.toArray(new String[mProjectList.size()]);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.pick_project_dialog_title)
                    .setItems(projects, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, final int which) {
                            mChosenProject = projects[which];
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
        mAddGpsToCommunityButton.setEnabled(true);
        List<CommunityInfo> communities = mKnownLocations.findCommunitiesNear(location);
        Log.d(TAG, String.format("%d communities at this location: %s", communities.size(), communities.toString()));
        if (communities.size() > 0) {
            List<String> communityNames = new ArrayList<>();
            Set<String> projects = new HashSet<>();
            for (CommunityInfo c : communities) {
                communityNames.add(c.name);
                projects.add(c.projectName);
            }
            mNearbyCommunitiesList.addAll(communityNames);
            mUpdateTodayCommunitiesList.addAll(communityNames);
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

    private final LocationProvider.MyLocationListener mLocationListener = new LocationProvider.MyLocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            Log.d(TAG, String.format("Location changed: %s", location.toString()));
            @SuppressLint("DefaultLocale")
            String locstr = String.format("%4.4f⦿%4.4f, %5.2f, %4.0f☼",
                    location.getLongitude(), location.getLatitude(),
                    location.getAltitude(), location.getBearing());
            mGpsLocation = location;
            if (mGpsCoordinatesTextView != null) {
                mGpsCoordinatesTextView.setText(locstr);
            }

            onGotLocation(location);
        }

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

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.d(TAG,
                    String.format("Location provider status changed: %s, %d, %s",
                            provider,
                            status,
                            extras.toString()));
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.d(TAG, String.format("Location provider enabled: %s", provider));
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.d(TAG, String.format("Location provider disabled: %s", provider));
        }
    };


    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;

    @Override
    public void onRequestPermissionsResult(int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Accepted
                    LocationProvider.getLocation(mLocationListener);
                } else {
                    // Denied
                    Toast.makeText(getActivity(), "LOCATION Denied", Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    private void setupLocation() {
        Log.v(TAG, "handlePermissionsAndGetLocation");
        int hasWriteContactsPermission;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            hasWriteContactsPermission = getActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_CODE_ASK_PERMISSIONS);
                return;
            }
        }
        LocationProvider.getLocation(mLocationListener);//if already has permission
    }

}
