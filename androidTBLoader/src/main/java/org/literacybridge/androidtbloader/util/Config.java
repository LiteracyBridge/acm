package org.literacybridge.androidtbloader.util;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.literacybridge.androidtbloader.TBLoaderAppContext;
import org.literacybridge.core.fs.OperationLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Configuration for the current user.
 */

public class Config {
    private static final String TAG = "TBL!:" + Config.class.getSimpleName();

    // These are the values that we get from the Cognito signin. As of 2019-12-24
    // [sub, exp, iat, token_use, event_id, aud, iss, phone_number_verified, auth_time # Cognito values 
    // custom:greeting # what the user asked to be called.
    // email_verified # will always be true if they're here; means they responded to their email. 
    // edit # regex of projects the user can edit. 
    // admin # if true, user is an admin in their projects. 
    // cognito:username # sign-in user name. These are UUIDs, assigned by the Cognito system.
    // view # regex of projects the user can view. 
    // phone_number # user's phone number, if they provided one. 
    // email # user's email. We use this for unique identity.
    // ]

    private static final Set<String> TBLOADER_ROLES = new HashSet<>(Arrays.asList("*", "AD", "PM","CO", "FO"));

    private static final String USERNAME_PROP = "cognito:username";
    private static final String EMAIL_PROP = "email";
    private static final String NAME_PROP = "name"; // "custom:greeting";

    // Programs info from "getprograms" API call is stored under this key.
    private static final String PROGRAMS_INFO_PROP = "programs_info";

    private static final String TBCDID_PROP = "tbcd";

    private final SharedPreferences mUserPrefs;
    private final TBLoaderAppContext mAppContext;

    // Program info is stored in these three fields.
    // {"programid" : "role,role"}
    private Map<String, String> mUserProgramsAndRoles;
    private List<String> mS3RepositoryList;
    private Map<String, String> mProgramNames;

    public interface Listener {
        void onSuccess();

        void onError();
    }

    private TbSrnHelper mTbSrnHelper;

    private Set<String> mUserPrograms;

    public String getTbcdid() {
        return mUserPrefs.getString(TBCDID_PROP, null);
    }

    public boolean isAdvanced() {
        return getEmail().endsWith("@amplio.org");
    }

    public String getCognitoUserID() {
        return mUserPrefs.getString(USERNAME_PROP, null);
    }

    public String getEmail() {
        return mUserPrefs.getString(EMAIL_PROP, null);
    }
    public String getName() { return mUserPrefs.getString(NAME_PROP, ""); }
    public void updateName(String newName) {
        SharedPreferences.Editor prefsEditor = mUserPrefs.edit();
        prefsEditor.putString(NAME_PROP, newName);
        prefsEditor.apply();
    }

    public Config(TBLoaderAppContext appContext) {
        mAppContext = appContext;
        mUserPrefs = PreferenceManager.getDefaultSharedPreferences(mAppContext);
    }

    /**
     * This should be called to clear any user-specific cached settings.
     */
    public void onSignOut() {
        SharedPreferences userPrefs = PreferenceManager.getDefaultSharedPreferences(
            TBLoaderAppContext.getInstance());
        SharedPreferences.Editor editor = userPrefs.edit();
        editor.clear().apply();
        mTbSrnHelper = null;
        mUserPrograms = null;
    }

    @SuppressLint("ApplySharedPref")
    public void applyUserDetails(Map<String, String> details) {
        String prevEmail = getEmail();
        if (details != null && details.size() > 0) {
            OperationLog.Operation opLog = OperationLog.startOperation("GetUserDetails");
            SharedPreferences.Editor prefsEditor = mUserPrefs.edit();

            for (Map.Entry<String, String> e : details.entrySet()) {
                prefsEditor.putString(e.getKey(), e.getValue());
                opLog.put(e.getKey(), e.getValue());
                Log.d(TAG, String.format("Config: %s => %s", e.getKey(), e.getValue()));
            }
            String name = details.get("name");
            if (StringUtils.isBlank(name)) {
                name = details.get("custom:greeting");
            }
            prefsEditor.putString("greeting", name);

            // If the email has changed, then we need to clear the stale TBCD value. It will be re-filled
            // in prepareForSerialNumberAllocation
            //noinspection ConstantConditions
            if (!mUserPrefs.getString(EMAIL_PROP, "").equalsIgnoreCase(prevEmail)) {
                prefsEditor.remove(TBCDID_PROP);
            }

            prefsEditor.commit();
            opLog.finish();
        }
    }

    public boolean haveCachedConfig() {
        return mUserPrefs.getString(EMAIL_PROP, null) != null;
    }

    /**
     * Determines whether the current user is assigned to a given project.
     *
     * @param programid The project of interest.
     * @return true if the current user should see the project, false otherwise.
     */
    public boolean isProgramIdForUser(String programid) {
        if (mUserProgramsAndRoles != null) {
            String rolesStr = mUserProgramsAndRoles.get(programid);
            if (StringUtils.isNotBlank(rolesStr)) {
                Set<String> roles = new HashSet<>(Arrays.asList(rolesStr.split(",")));
                roles.retainAll(TBLOADER_ROLES);
                return roles.size() > 0;
            }
        }
        return false;
    }

    /**
     * Gets the list of program ids for this user.
     * @return a list of program ids.
     */
    public List<String> getProgramIdsForUser() {
        List<String> result = new ArrayList<>();
        if (mUserProgramsAndRoles != null) {
            for (String programid : mUserProgramsAndRoles.keySet()) {
                if (isProgramIdForUser(programid))
                    result.add(programid);
            }
        }
        return result;
    }

    /**
     * Gets the friendly name for the given programid. If the friendly name can't be determined,
     * simply returns the programid.
     * @param programid for which friendly name is desired.
     * @return the friendly name, or the programid if the friendly name isn't available.
     */
    public String getFriendlyName(String programid) {
        if (mProgramNames == null) return programid;
        return mProgramNames.getOrDefault(programid, programid);
    }

    /**
     * Attempt to allocate the next Talking Book serial number for this TBLoader. If there are
     * no serial numbers available locally, will attempt to call the server to allocate a new
     * block.
     *
     * @param success  A Consumer that accepts an integer of the next serial number.
     * @param failure  A Consumer that accepts an Exception from a failure.
     */
    public void allocateDeviceSerialNumber(Consumer<String> success, Consumer<Exception> failure) {
        mTbSrnHelper.allocateNextSrn(success, failure);
    }

    /**
     * Get the user config from server. If no connectivity, and have a cached config use that.
     *
     * @param listener A Listener to accept the results.
     */
    public void prepareForSerialNumberAllocation(final Listener listener) {
        mTbSrnHelper = new TbSrnHelper(mAppContext);
        String email = getEmail();
        mTbSrnHelper.prepareForAllocation(email, new Listener() {
            @Override
            public void onSuccess() {
                String tbcd = mTbSrnHelper.getTbLoaderIdHex();
                if (!mUserPrefs.getString(TBCDID_PROP, "").equalsIgnoreCase(tbcd)) {
                    SharedPreferences.Editor prefsEditor = mUserPrefs.edit();
                    prefsEditor.putString(TBCDID_PROP, tbcd);
                    prefsEditor.apply();
                }
                listener.onSuccess();
            }

            @Override
            public void onError() {
                listener.onError();
            }
        });
    }

    public void saveProgramsInfoJSON(JSONObject programsInfo) {
        String resultString = programsInfo.toString();
        if (!mUserPrefs.getString(PROGRAMS_INFO_PROP, "").equals(resultString)) {
            SharedPreferences.Editor prefsEditor = mUserPrefs.edit();
            prefsEditor.putString(PROGRAMS_INFO_PROP, resultString);
            prefsEditor.apply();
        }
    }
    public void loadProgramsInfo() {
        String jsonString = mUserPrefs.getString(PROGRAMS_INFO_PROP, null);
        if (StringUtils.isNotBlank(jsonString)) {
            try {
                JSONObject jsonObject = new JSONObject(jsonString);
                parseProgramsInfoJSON(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
    public void parseProgramsInfoJSON(JSONObject programsInfo) {
        Map<String,Map<String,String>> programs_info = new HashMap<>();
        String implicit_repository = programsInfo.optString("implicit_repository", "dbx");

        try {
            // Covert the lame-ass android json object to something usable.
            JSONObject programs = (JSONObject) programsInfo.get("programs");
            for (Iterator<String> it = programs.keys(); it.hasNext(); ) {
                String programId = it.next();
                try {
                    JSONObject programInfoJson = programs.getJSONObject(programId);
                    // Convert inner object to something usable.
                    Map<String,String> programInfo = new HashMap<>();
                    for (Iterator<String> it2 = programInfoJson.keys(); it2.hasNext();) {
                        String programInfoPropertyName = it2.next();
                        String programInfoProperty = programInfoJson.getString(programInfoPropertyName);
                        programInfo.put(programInfoPropertyName, programInfoProperty);
                    }
                    programs_info.put(programId, programInfo);
                } catch (JSONException e) {
                    // Ignore the key. Should not happen IRL.
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Build map of {programid: "role,role"}
        mUserProgramsAndRoles = programs_info.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e->e.getValue().get("roles")));
        // Build a list of [programid] of those programs with s3 as repository.
        mS3RepositoryList = programs_info.entrySet().stream()
            .filter(e->e.getValue().getOrDefault("repository", implicit_repository).equalsIgnoreCase("s3"))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        // Extract the friendly names as {programid: "name"}
        mProgramNames = programs_info.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e->e.getValue().getOrDefault("name", e.getKey())));
    }

}
