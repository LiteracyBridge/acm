package org.literacybridge.archived_androidtbloader.recipient;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import org.apache.commons.lang3.StringUtils;
import org.literacybridge.archived_androidtbloader.R;
import org.literacybridge.archived_androidtbloader.TBLoaderAppContext;
import org.literacybridge.archived_androidtbloader.util.Constants;
import org.literacybridge.core.spec.ProgramSpec;
import org.literacybridge.core.spec.RecipientList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static android.app.Activity.RESULT_OK;

public class RecipientChooserFragment extends Fragment {
    private static final String TAG = "TBL!:" + RecipientChooserFragment.class.getSimpleName();

    private TBLoaderAppContext mAppContext;
    private ProgramSpec mProgramSpec;
    private RecipientList mRecipientList;

    private ViewGroup mSpinnerGroup;
    private Button mSelectButton;

    private List<String> mPreselectedRecipients;
    private boolean mIsPreselection;

    private List<String> selections = new ArrayList<>();
    private List<HierarchyHandler> handlers = new ArrayList<>();
    private int attentionLevel;
    private int selectionLevel;

    private int selectedColor;
    private int unavailableColor;
    private int attentionColor;
    private int autoselectedColor;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAppContext = (TBLoaderAppContext) getActivity().getApplicationContext();
        mProgramSpec = mAppContext.getProgramSpec();
        mRecipientList = mProgramSpec.getRecipients();

        // Get the arguments; a project or projects, or a list of communities.
        Intent intent = getActivity().getIntent();
        if (intent.hasExtra(Constants.PRESELECTED_RECIPIENTS)) {
            mPreselectedRecipients = intent.getStringArrayListExtra(Constants.PRESELECTED_RECIPIENTS);
        } else {
            mPreselectedRecipients = new ArrayList<>();
        }

        mIsPreselection = intent.getBooleanExtra("preselect", false);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
        @Nullable ViewGroup container,
        Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.activity_recipient_chooser, container, false);

        this.selectedColor = ContextCompat.getColor(mAppContext, R.color.text_normal);
        this.unavailableColor = ContextCompat.getColor(mAppContext, R.color.text_disabled);
        this.attentionColor = ContextCompat.getColor(mAppContext, R.color.text_attention);
        this.autoselectedColor = ContextCompat.getColor(mAppContext, R.color.text_autoselect);

        // The actionbar has a "title" property that is set from the activity's "label=" property
        // from the AndroidManifest file. Here, we make the toolbar work like an action bar.
        Toolbar toolbar = view.findViewById(R.id.main_toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        // The toolbar also *contains* a TextView with an id of main_toolbar_title.
        TextView main_title = view.findViewById(R.id.main_toolbar_title);
        main_title.setText("");

        // We want a "back" button (sometimes called "up"), but we don't want back navigation, rather
        // to simply end this activity without setting project or community.
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(view1 -> getActivity().finish());

        TextView mProjectTextView = view.findViewById(R.id.recipient_chooser_project);
        mProjectTextView.setText(mAppContext.getProject());

        mSpinnerGroup = (LinearLayout) view.findViewById(R.id.recipient_chooser_spinners);
        mSelectButton = view.findViewById(R.id.recipient_chooser_select);
        Button mPreselectButton = view.findViewById(R.id.recipient_chooser_preselect);

        if (mIsPreselection) {
            mSelectButton.setVisibility(View.GONE);
            mPreselectButton.setOnClickListener(preSelectListener);
        } else {
            mPreselectButton.setVisibility(View.GONE);
            view.findViewById(R.id.recipient_chooser_preselection_prompt).setVisibility(View.GONE);
            mSelectButton.setOnClickListener(selectListener);
        }

        setupSelectionHandlers();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mProgramSpec = mAppContext.getProgramSpec();
        mRecipientList = mProgramSpec.getRecipients();
    }

    @Override
    public void onPause() {
        super.onPause();
        mProgramSpec = null;
        mRecipientList = null;
    }

    private OnClickListener preSelectListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent();
            ArrayList<String> subselection = new ArrayList<>(selections.subList(0, selectionLevel+1));
            intent.putStringArrayListExtra(Constants.PRESELECTED_RECIPIENTS, subselection);
            getActivity().setResult(RESULT_OK, intent);
            getActivity().finish();
        }
    };

    private OnClickListener selectListener = v -> {
        Intent intent = new Intent();
        intent.putStringArrayListExtra(Constants.SELECTED_RECIPIENT, (ArrayList<String>)selections);
        getActivity().setResult(RESULT_OK, intent);
        getActivity().finish();
    };

    /**
     * Create the handlers for choosing the recipient levels; add handlers to their container.
     */
    private void setupSelectionHandlers() {
        LayoutInflater inflater = LayoutInflater.from(mAppContext);
        for (int level = 0; level <= mRecipientList.getMaxLevel(); level++) {
            Spinner spinner = (Spinner) inflater.inflate(R.layout.recipient_chooser_spinner,
                mSpinnerGroup,
                false);
            mSpinnerGroup.addView(spinner);
            HierarchyHandler handler = new HierarchyHandler(level, spinner);

            // Apply the adapter to the spinner
            spinner.setAdapter(handler);
            spinner.setOnItemSelectedListener(handler);
            spinner.setOnTouchListener(handler);

            // Placeholder for a selection
            selections.add("");

            handlers.add(handler);
        }
        selectionLevel = -1;

        fillHandlers(0);
        mPreselectedRecipients.clear();
        enableButtons();
    }

    /**
     * Add the choices to the chooser at the given level of the hierarchy.
     * - If the given level has only one value to choose from, auto-select it.
     * - If we were given a pre-selection list, and there is a pre-selection for
     *   this level, and the pre-selection actually exists, auto-select it.
     * If an auto-selection is done, fill the next level, otherwise clear the
     * remaining (more specific) levels.
     * @param level of chooser to fill.
     */
    private void fillHandlers(int level) {
        List<String> values = getValuesForLevel(level);
        HierarchyHandler handler = handlers.get(level);
        Spinner spinner = handler.spinner; // (Spinner) mView.findViewById(IDs[level]);
        handler.fillData(values);
        // Check for a pre-selection.
        int preSelectIx = -1;
        if (values.size()==1) {
            preSelectIx = 1;
        } else if (level< mPreselectedRecipients.size()) {
            String preSelected = mPreselectedRecipients.get(level);
            // There are pre-selections. Do we have a match here?
            if (values.contains(preSelected)) {
                preSelectIx = values.indexOf(preSelected) + 1;
            } else {
                mPreselectedRecipients.clear(); // done with them.
            }
        }

        if (preSelectIx > 0) {
            // There is only one value; autoselect it, and don't let user try to change it.
            // Or there was a pre-selection from invoker.
            spinner.setEnabled(values.size() > 1);
            spinner.setSelection(preSelectIx);
            gotSelection(level, values.get(preSelectIx-1));
            if (level < mRecipientList.getMaxLevel()) {
                fillHandlers(level + 1);
            }
        } else {
            spinner.setEnabled(true);
            spinner.setSelection(0); // The prompt
            handler.setAttention(true);
            clearHandlers(level + 1);
        }
    }

    private void clearHandlers(int level) {
        for (; level <= mRecipientList.getMaxLevel(); level++) {
            HierarchyHandler handler = handlers.get(level);
            Spinner spinner = handler.spinner; // (Spinner) mView.findViewById(IDs[level]);
            handler.clearData();
            spinner.setEnabled(false);
        }
    }

    private void gotSelection(int level, String value) {
        selections.set(level, value);
        selectionLevel = level;
        handlers.get(level).setAttention(false);
        enableButtons();
    }

    private void enableButtons() {
        if (!mIsPreselection) {
            boolean enable = selectionLevel == mRecipientList.getMaxLevel();
            // Set alpha, because Android doesn't distinguish disabled buttons. (TODO: really????)
            mSelectButton.setAlpha(enable ? 1 : 0.5f);
            mSelectButton.setEnabled(enable);
        }
    }

    private List<String> getValuesForLevel(int level) {
        return new ArrayList<>(mRecipientList.getChildrenOfPath(selections.subList(0, level)));
    }

    /**
     * This is a combined handler for the selection spinners.
     */
    private class HierarchyHandler extends ArrayAdapter<CharSequence>
        implements AdapterView.OnItemSelectedListener, View.OnTouchListener {

        Spinner spinner;
        int level;
        // Hack so we can ignore "ItemSelected" that's not due to a touch.
        boolean touched = false;

        String label;

        private List<String> mObjects = new ArrayList<>();

        HierarchyHandler(int level, Spinner spinner)
        {
            super(mAppContext, R.layout.recipient_chooser_item);
            this.level = level;
            this.label = mRecipientList.getNameOfLevel(level);
            this.spinner = spinner;
        }

        /**
         * Called to set a level as the "attention" level, or not. The "attention" level is
         * highlighted in a different color, to draw attention to that fact that it is the
         * next level that needs attention from the user.
         * @param attention true if the level should be the "attention level".
         */
        void setAttention(boolean attention) {
            if (attention && attentionLevel != level) {
                // This level is becoming the "attention" level.
                if (attentionLevel>=0 && attentionLevel<handlers.size()) {
                    // Tell the old level that it's no longer center of attention.
                    handlers.get(attentionLevel).setAttention(false);
                }
                attentionLevel = level;
                super.notifyDataSetChanged();
            } else if (!attention && attentionLevel == level) {
                // This level is becoming "no longer the attention level"
                attentionLevel = -1;
                super.notifyDataSetChanged();
            }
        }


        private void clearData() {
            mObjects.clear();
            super.notifyDataSetChanged();
        }

        private void fillData(Collection<String> newData) {
            mObjects.clear();
            mObjects.addAll(newData);
            super.notifyDataSetChanged();
        }

        // AdapterView.OnItemSelectedListener
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (!touched) return;
            touched = false;
            int itemIx = position - 1;
            if (itemIx < 0) return;
            String item = mObjects.get(itemIx);
            Log.d(TAG, String.format("%s selected %s @ %d", label, item, position));
            gotSelection(level, item);
            if (level < mRecipientList.getMaxLevel()) {
                fillHandlers(level + 1);
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            touched = true;
            return false;
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }

        // ArrayAdapter<CharSequence>
        @Override
        public int getCount() {
            return mObjects.size() + 1;
        }

        @Nullable
        @Override
        public CharSequence getItem(int position) {
            if (position == 0) {
                if (mObjects.size() == 0) {
                    return String.format("-- No %s --", label);
                } else {
                    return String.format("Select %s...", label);
                }
            } else {
                return mObjects.get(position - 1);
            }
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(mAppContext);
                convertView = inflater.inflate(R.layout.recipient_chooser_item, parent, false);
                TextView label = convertView.findViewById(R.id.hierarchy_spinner_label);
                label.setText(this.label);
            }
            TextView choice = convertView.findViewById(R.id.hierarchy_spinner_choice_text);
            TextView msg = convertView.findViewById(R.id.hierarchy_spinner_choice_message);

            int color;
            if (!spinner.isEnabled() && mObjects.size() != 1) {
                // Disabled and not a single choice. Means not auto-selected; simply unavailable.
                color = unavailableColor;
            } else if (level == attentionLevel) {
                // Draw the user's eye to this level; next level that needs a selection.
                color = attentionColor;
            } else if (mObjects.size() == 1) {
                // If there's a single value, we're going to auto-select it, whether it is
                // earlier or later in the selection stack.
                color = autoselectedColor;
            } else {
                // It's been selected. Can be changed if the user needs to, but otherwise, just done.
                color = selectedColor;
            }

            TextView view = layoutItem(position, choice, msg);
            view.setTextColor(color);

            return convertView;
        }

        /**
         * This returns a view to draw an item in the dropdown list.
         * @param position The index of the item in the backing virtual array.
         * @param convertView Maybe an old instance of the view, maybe null.
         * @param parent The container of the new view.
         * @return A new view, or the old one recycled.
         */
        @Override
        public View getDropDownView(int position,
            @Nullable View convertView,
            @NonNull ViewGroup parent)
        {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(mAppContext);
                convertView = inflater.inflate(R.layout.recipient_chooser_dropdown_item, parent, false);
            }
            TextView choice = convertView.findViewById(R.id.hierarchy_spinner_choice_text);
            TextView msg = convertView.findViewById(R.id.hierarchy_spinner_choice_message);

            layoutItem(position, choice, msg);

            return convertView;
        }

        /**
         * How many TBs are in the given entity?
         * @param entity for which to get the number.
         * @return the number, as a string.
         */
        String getNumTbsLabel(String entity) {
            if (level < 0) return "";
            List<String> path = new ArrayList<>();
            path.addAll(selections.subList(0, level));
            path.add(entity);
            int n = mRecipientList.getNumTbs(path);
            return String.format(" (%d TB%s)", n, n==1?"":"s");
        }

        private TextView layoutItem(int position, TextView choice, TextView msg) {
            boolean isMsg = false;
            String text;
            TextView view;
            if (position == 0 && mObjects.size() != 0) {
                text = String.format("Select %s...", label);
                isMsg = true;
            } else if (position == 0 && mObjects.size() == 0) {
                // There are none of these to choose from.
                text = String.format("-- No %s --", label);
                isMsg = true;
            } else if (position != 0 && StringUtils.isBlank(mObjects.get(position - 1))) {
                // There are one or more items in the level, but THIS one is empty. That's different
                // from "there are none to choose from" (which is a "msg", not a "choice").
                text = String.format("-- No %s --", label) + getNumTbsLabel("");
            } else {
                text = mObjects.get(position - 1);
                text = text + getNumTbsLabel(text);
            }

            if (isMsg) {
                choice.setVisibility(View.GONE);
                msg.setVisibility(View.VISIBLE);
                msg.setText(text);
                view = msg;
            } else {
                choice.setVisibility(View.VISIBLE);
                msg.setVisibility(View.GONE);
                choice.setText(text);
                view = choice;
            }
            return view;
        }

        /**
         * We disable the first item, which we use as a prompt. So, not all items are enabled.
         * @return false
         */
        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        /**
         * We disable the first item, which we use as a prompt. So, item[0] is disabled,
         * and everything else is enabled.
         * @param position for which the enabled status is desired.
         * @return true if enabled (position > 0), false if not (position == 0)
         */
        @Override
        public boolean isEnabled(int position) {
            return position > 0;
        }

    }

}
