package org.literacybridge.androidtbloader.community;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import org.literacybridge.androidtbloader.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.app.Activity.RESULT_OK;

/**
 * Created by bill on 12/22/16.
 */

public class ChooseCommunityFragment extends Fragment {
    private static final String TAG = ChooseCommunityFragment.class.getSimpleName();

    private List<String> mOriginalList;
    private List<String> mFilteredList;
    private String mFilter;
    private Pattern mFilterPattern;

    private ArrayAdapter mAdapter;

    private EditText mFilterText;
    private ListView mListView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getActivity().getIntent();
        mOriginalList = new ArrayList<>(intent.getStringArrayListExtra("list"));
        mFilteredList = new ArrayList<>(mOriginalList);
        Collections.sort(mOriginalList, new Comparator<String>() {
            @Override
            public int compare(String lhs, String rhs) {
                return lhs.compareToIgnoreCase(rhs);
            }
        });
        mFilter = intent.getStringExtra("filter");

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_choose_community, container, false);

        mFilterText = (EditText)view.findViewById(R.id.filtered_chooser_filter_text);
        mListView = (ListView)view.findViewById(R.id.filtered_chooser_list);

        mFilterText.addTextChangedListener(filterTextListener);

        mListView.setOnTouchListener(listViewTouchListener);
        mListView.setOnItemClickListener(listViewItemClickListener);

        mFilter = "";
        mAdapter = new ArrayAdapter(getActivity(), android.R.layout.simple_list_item_1, mFilteredList);
        mListView.setAdapter(mAdapter);

        updateListView();

        return view;
    }

    /**
     * This is to hide the keyboard when the user scrolls the list. Shows more list.
     */
    OnTouchListener listViewTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getActivity().getWindow().getCurrentFocus().getWindowToken(), 0);
            return false;
        }
    };

    /**
     * Handles the actual choosing of an item.
     */
    OnItemClickListener listViewItemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String selected = (String)mListView.getItemAtPosition(position);
            Intent intent = new Intent();
            intent.putExtra("selected", selected);
            getActivity().setResult(RESULT_OK, intent);
            getActivity().finish();
        }
    };

    /**
     * Watches for changes to the filter TextEdit, and updates the filtering appropriately.
     */
    TextWatcher filterTextListener = new TextWatcher() {
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

    /**
     * Updates the filtered list with the current filter.
     */
    private void updateListView() {
        mFilteredList.clear();
        if (mFilter == null || mFilter.length() == 0) {
            mFilteredList.addAll(mOriginalList);
        } else {
            for (String s : mOriginalList) {
                Matcher m = mFilterPattern.matcher(s);
                if (m.find()) {
                    mFilteredList.add(s);
                }
            }
        }
        mAdapter.notifyDataSetChanged();
    }


}
