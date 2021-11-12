package org.literacybridge.androidtbloader.community;

import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.literacybridge.androidtbloader.BuildConfig;
import org.literacybridge.androidtbloader.R;
import org.literacybridge.androidtbloader.TBLoaderAppContext;
import org.literacybridge.androidtbloader.checkin.LocationProvider;
import org.literacybridge.androidtbloader.content.ContentManager;
import org.literacybridge.androidtbloader.util.Constants;
import org.literacybridge.androidtbloader.util.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.app.Activity.RESULT_OK;
import static android.view.View.GONE;
import static java.lang.Math.min;

/**
 * Choose a community from a list.
 *
 * A number of options are provided, depending on the needs of the caller.
 * - communities: extra data; if provided is the list of communities to show.
 * - projects: extra data; if provided let user choose a project from the list to
 *   filter the communities shown. If only one project, don't show it as a choice.
 */

public class ChooseCommunityFragment extends Fragment {
    private static final String TAG = "TBL!:" + ChooseCommunityFragment.class.getSimpleName();

    private ContentManager mContentManager;
    private List<String> mProjectList;
    private String mProject;

    private List<CommunityInfo> mOriginalList;
    private List<CommunityInfo> mSortedList;
    private List<CommunityInfo> mFilteredList;
    private String mFilter;
    private Pattern mFilterPattern;

    private TextView mProjectTextView;
    private CheckBox mSortByDistanceCheckBox;
    private EditText mFilterText;

    private CommunityInfoAdapter mAdapter;

    private boolean mSortByDistance;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContentManager = ((TBLoaderAppContext) getActivity().getApplicationContext()).getContentManager();
        mSortedList = new ArrayList<>();
        mFilteredList = new ArrayList<>();

        // Get the arguments; a project or projects, or a list of communities.
        Intent intent = getActivity().getIntent();
        List<String> projectsList = intent.getStringArrayListExtra("projects");
        mProject = intent.getStringExtra(Constants.PROJECT);


        if (projectsList != null && projectsList.size() > 0) {
            Log.d(TAG, "Got a list of projects");
            mProjectList = new ArrayList<>(projectsList);
        } else if (mProject != null){
            Log.d(TAG, String.format("Using '%s' to build project list", mProject));
            mProjectList = Collections.singletonList(mProject);
        }

        // If there is no project given, but there is a list, take the first item in the list as the project
        if (mProject == null) {
            if (mProjectList != null && mProjectList.size() > 0) {
                Log.d(TAG, "Using first project from list as initial project");
                mProject = mProjectList.get(0);
            }
        } else {
            // There is a project given. If there's also a list, make sure the project is in the list.
            boolean found = false;
            for (String proj : mProjectList) {
                if (proj.equals(mProject)) {
                    found = true;
                    break;
                }
            }
            if (!found) throw new IllegalStateException("Given project not in given list of projects");
        }

        // If there is a project, populate the original list from it.
        if (mProject != null) {
            setProject(mProject);
        } else {
            // No project(s) passed, we need to have communities passed.
            List<String> communities = intent.getStringArrayListExtra(Constants.COMMUNITIES);
            if (communities != null) {
                mOriginalList = CommunityInfo.parseExtra(communities);
            }
        }

        // Ensure we have communities to choose from.
        if (mOriginalList == null || mOriginalList.size() == 0) {
            throw new IllegalStateException("No communities from which to choose");
        }

        if (BuildConfig.DEBUG) {
            String orgList = Util.join(mOriginalList.subList(0, min(mOriginalList.size(), 3)), ", ");
            List<String> excluded = intent.getStringArrayListExtra("excluded");
            if (excluded == null) {
                excluded = Collections.singletonList("-none-");
            }
            String exclList = Util.join(excluded.subList(0, min(excluded.size(), 3)), ", ");
            Log.d(TAG, String.format("Get community for %s, list: %s, excl: %s", mProject, orgList, exclList));
        }

        // Caller can exclude some communities from the display, if desired.
        List<String> excluded = intent.getStringArrayListExtra("excluded");
        if (excluded != null && excluded.size() > 0) {
            List<CommunityInfo> excludedCommunities = CommunityInfo.parseExtra(excluded);
            for (CommunityInfo info : excludedCommunities) {
                if (mOriginalList.contains(info)) mOriginalList.remove(info);
            }
        }

        mFilter = intent.getStringExtra("filter");
        if (mFilter == null) mFilter = "";

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_choose_community, container, false);

        // The actionbar has a "title" property that is set from the activity's "label=" property
        // from the AndroidManifest file. Here, we make the toolbar work like an action bar.
        Toolbar toolbar = (Toolbar) view.findViewById(R.id.main_toolbar);
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
        // The toolbar also *contains* a TextView with an id of main_toolbar_title.
        TextView main_title = (TextView) view.findViewById(R.id.main_toolbar_title);
        main_title.setText("");

        // We want a "back" button (sometimes called "up"), but we don't want back navigation, rather
        // to simply end this activity without setting project or community.
        ((AppCompatActivity)getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View view){
                getActivity().finish();
            }
        });

        mFilterText = (EditText)view.findViewById(R.id.filtered_chooser_filter_text);
        mFilterText.addTextChangedListener(filterTextListener);
        mSortByDistanceCheckBox = (CheckBox) view.findViewById(R.id.filtered_chooser_sort_distance_checkBox);
        mSortByDistanceCheckBox.setOnClickListener(sortByDistanceClickListener);
        LinearLayout mProjectGroup = (LinearLayout) view.findViewById(
                R.id.filtered_chooser_project_group);
        mProjectGroup.setOnClickListener(projectClickListener);
        mProjectTextView = (TextView)view.findViewById(R.id.filtered_chooser_project);
        TextView mProjectLabelTextView = (TextView) view.findViewById(
                R.id.filtered_chooser_project_label);

        RecyclerView mCommunityInfoRecyclerView = (RecyclerView) view.findViewById(
                R.id.filtered_chooser_recycler);
        mCommunityInfoRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mCommunityInfoRecyclerView.setOnTouchListener(listViewTouchListener);

        mAdapter = new CommunityInfoAdapter(getActivity(), mFilteredList, mCommunitySelectedListener
        );
        mCommunityInfoRecyclerView.setAdapter(mAdapter);

        if (mProject == null) {
            mProjectGroup.setVisibility(GONE);
        } else {
            mProjectTextView.setText(mProject);
            if (mProjectList == null || mProjectList.size() == 1) {
                mProjectLabelTextView.setText(R.string.filtered_chooser_project_label);
            }
        }

        // Don't show the keyboard until user taps in filter.
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        sortList();
        updateListView();

        return view;
    }

    @NonNull
    private OnClickListener sortByDistanceClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                mSortByDistance = mSortByDistanceCheckBox.isChecked();
                sortList();
                filterList();
                mAdapter.notifyDataSetChanged();
            }
        };


    /**
     * This is to hide the keyboard when the user scrolls the list. Shows more list.
     */
    private OnTouchListener listViewTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getActivity().getWindow().getCurrentFocus().getWindowToken(), 0);
            return false;
        }
    };

    private OnClickListener projectClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            final String[] projects = mProjectList.toArray(new String[mProjectList.size()]);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.pick_project_dialog_title)
                    .setItems(projects, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, final int which) {
                            setProject(projects[which]);
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mProjectTextView.setText(mProject);
                                    mAdapter.notifyDataSetChanged();
                                }
                            });
                        }
                    });
            builder.create().show();
        }
    };

    /**
     * Watches for changes to the filter TextEdit, and updates the filtering appropriately.
     */
    private TextWatcher filterTextListener = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { }
        @Override
        public void afterTextChanged(Editable s) {
            String newFilter = mFilterText.getText().toString();
            if (!newFilter.equalsIgnoreCase(mFilter)) {
                mFilter = newFilter;
                mFilterPattern = Pattern.compile("(?i)"+mFilter);
                updateListView();
            }
        }
    };

    private CommunityInfoAdapter.CommunityInfoAdapterListener mCommunitySelectedListener = new CommunityInfoAdapter.CommunityInfoAdapterListener() {
        @Override
        public void onCommunityClicked(CommunityInfo community) {
            Intent intent = new Intent();
            intent.putExtra("selected", community.makeExtra());
            getActivity().setResult(RESULT_OK, intent);
            getActivity().finish();
        }
    };

    private void setProject(String project) {
        Map<String, Map<String, CommunityInfo>> projects = mContentManager.getCommunitiesForProjects(
            Collections.singletonList(project));
        if (projects.containsKey(project)) {
            mProject = project;
            mOriginalList = new ArrayList<>(projects.get(mProject).values());
            sortList();
            filterList();
        }
    }

    /**
     * Sorts the original list to the sorted list, based on current criteria.
     */
    private void sortList() {
        mSortedList.clear();
        mSortedList.addAll(mOriginalList);
        Collections.sort(mSortedList, new Comparator<CommunityInfo>() {
            @Override
            public int compare(CommunityInfo lhs, CommunityInfo rhs) {
                if (mSortByDistance) {
                    Float dlhs = LocationProvider.distanceTo(lhs);
                    Float drhs = LocationProvider.distanceTo(rhs);
                    if (dlhs < drhs) return -1;
                    if (dlhs > drhs) return 1;
                }
                int cmp = lhs.getProject().compareToIgnoreCase(rhs.getProject());
                if (cmp != 0) {
                    return cmp;
                }
                return lhs.getName().compareToIgnoreCase(rhs.getName());
            }
        });
    }

    /**
     * Filters the sorted list to the filtered list, based on current criteria.
     */
    private void filterList() {
        mFilteredList.clear();
        if (mFilter == null || mFilter.length() == 0) {
            mFilteredList.addAll(mSortedList);
        } else {
            for (CommunityInfo info : mSortedList) {
                Matcher m = mFilterPattern.matcher(info.getName());
                if (m.find()) {
                    mFilteredList.add(info);
                }
            }
        }
    }

    /**
     * Updates the filtered list with the current filter.
     */
    private void updateListView() {
        filterList();
        mAdapter.notifyDataSetChanged();
    }


}
