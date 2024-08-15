package org.literacybridge.archived_androidtbloader;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import org.literacybridge.archived_androidtbloader.talkingbook.TalkingBookConnectionManager;

public class SettingsFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    private SharedPreferences mSharedPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
        mSharedPreferences = getPreferenceManager().getSharedPreferences();

        for (String key : mSharedPreferences.getAll().keySet()) {
            onSharedPreferenceChanged(mSharedPreferences, key);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);

        updateSummaries();
    }

    @Override
    public void onPause() {
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference pref = findPreference(key);
        if (pref != null && pref instanceof EditTextPreference) {
            updateTextSummary((EditTextPreference) pref);
        } else {
            updateSummaries();
        }
    }

    private void updateTextSummary(EditTextPreference preference) {
        // set the EditTextPreference's summary value to its current text
        preference.setSummary(preference.getText());
    }

    private void updateSummaries() {

        TalkingBookConnectionManager talkingBookConnectionManager =
                ((TBLoaderAppContext) getActivity().getApplicationContext()).getTalkingBookConnectionManager();

        Preference preference = findPreference("pref_prefer_network_location");
        preference.setSummary(getString(R.string.pref_summary_location_preference));

        preference = findPreference("pref_tb_access");
        if (talkingBookConnectionManager.hasDefaultPermission()) {
            preference.setSummary(getString(R.string.pref_summary_tb_access_granted));
        } else {
            preference.setSummary(getString(R.string.pref_summary_tb_access_not_granted));
        }

        if (!TBLoaderAppContext.getInstance().getConfig().isAdvanced()) {
            // TODO: it should be possible to avoid even loading the advanced settings.
            Preference pc = findPreference("pref_key_advanced_settings");
            if (pc != null) {
                getPreferenceScreen().removePreference(pc);
            }
        } else {
            preference = findPreference("pref_show_unpublished");
            if (mSharedPreferences.getBoolean("pref_show_unpublished", false)) {
                preference.setSummary(getString(R.string.pref_summary_show_unpublished));
            } else {
                preference.setSummary(getString(R.string.pref_summary_show_published_only));
            }

            ////////////////////////////////////////////////////////////////////////////////
            // Debug code
            preference = findPreference(("pref_simulate_device"));
            if (mSharedPreferences.getBoolean(preference.getKey(), false)) {
                preference.setSummary(R.string.pref_summary_simulate_device);
            } else {
                preference.setSummary(R.string.pref_summary_physical_device);
            }

            preference = findPreference(("pref_disable_uploads"));
            if (mSharedPreferences.getBoolean(preference.getKey(), false)) {
                preference.setSummary(R.string.pref_summary_uploads_disabled);
            } else {
                preference.setSummary(R.string.pref_summary_uploads_enabled);
            }
            // Debug code
            ////////////////////////////////////////////////////////////////////////////////

            preference = findPreference(("pref_allow_install_outdated"));
            if (mSharedPreferences.getBoolean(preference.getKey(), false)) {
                preference.setSummary(R.string.pref_summary_allow_install_outdated_allowed);
            } else {
                preference.setSummary(R.string.pref_summary_allow_install_outdated_not_allowed);
            }
        }
    }
}
