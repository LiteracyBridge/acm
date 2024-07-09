package org.literacybridge.androidtbloader.util

import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import org.literacybridge.core.fs.OperationLog
import org.literacybridge.core.spec.ProgramSpec
import org.literacybridge.androidtbloader.App

/**
 * Configuration for the current user.
 */
class Config(private val mAppContext: App) {
    private val mUserPrefs: SharedPreferences

    // Program info is stored in these three fields.
    // {"programid" : "role,role"}
    private var mUserProgramsAndRoles: Map<String, String?>? = null
    private var mS3RepositoryList: List<String>? = null
    private var mProgramNames: Map<String, String>? = null

    interface Listener {
        fun onSuccess()
        fun onError()
    }

    //    private TbSrnHelper mTbSrnHelper;
    private val mUserPrograms: Set<String>? = null
    val tbcdid: String?
        get() = mUserPrefs.getString(TBCDID_PROP, null)
    val isAdvanced: Boolean
        get() = email!!.endsWith("@amplio.org")
    val cognitoUserID: String?
        get() = mUserPrefs.getString(USERNAME_PROP, null)
    val email: String?
        get() = mUserPrefs.getString(EMAIL_PROP, null)
    val name: String?
        get() = mUserPrefs.getString(NAME_PROP, "")

    var g_programSpec: ProgramSpec? = null

    fun updateName(newName: String?) {
        val prefsEditor = mUserPrefs.edit()
        prefsEditor.putString(NAME_PROP, newName)
        prefsEditor.apply()
    }

    init {
        mUserPrefs = PreferenceManager.getDefaultSharedPreferences(mAppContext)
    }

    /**
     * This should be called to clear any user-specific cached settings.
     */
    //    public void onSignOut() {
    //        SharedPreferences userPrefs = PreferenceManager.getDefaultSharedPreferences(
    //            TBLoaderAppContext.getInstance());
    //        SharedPreferences.Editor editor = userPrefs.edit();
    //        editor.clear().apply();
    //        mTbSrnHelper = null;
    //        mUserPrograms = null;
    //    }
    fun applyUserDetails(details: Map<String?, String?>?) {
        val prevEmail = email
        if (details != null && details.size > 0) {
            val opLog = OperationLog.startOperation("GetUserDetails")
            val prefsEditor = mUserPrefs.edit()
            for ((key, value) in details) {
                prefsEditor.putString(key, value)
                opLog.put(key, value)
                Log.d(TAG, String.format("Config: %s => %s", key, value))
            }
            val name = details["name"]
            //            if (StringUtils.isBlank(name)) {
//                name = details.get("custom:greeting");
//            }
            prefsEditor.putString("greeting", name)

            // If the email has changed, then we need to clear the stale TBCD value. It will be re-filled
            // in prepareForSerialNumberAllocation
            if (!mUserPrefs.getString(EMAIL_PROP, "").equals(prevEmail, ignoreCase = true)) {
                prefsEditor.remove(TBCDID_PROP)
            }
            prefsEditor.commit()
            opLog.finish()
        }
    }

    fun haveCachedConfig(): Boolean {
        return mUserPrefs.getString(EMAIL_PROP, null) != null
    }

    /**
     * Determines whether the current user is assigned to a given project.
     *
     * @param programid The project of interest.
     * @return true if the current user should see the project, false otherwise.
     */
    fun isProgramIdForUser(programid: String?): Boolean {
        // TODO: Remove this function
//        if (mUserProgramsAndRoles != null) {
//            String rolesStr = mUserProgramsAndRoles.get(programid);
//            if (StringUtils.isNotBlank(rolesStr)) {
//                Set<String> roles = new HashSet<>(Arrays.asList(rolesStr.split(",")));
//                roles.retainAll(TBLOADER_ROLES);
//                return roles.size() > 0;
//            }
//        }
        return true
    }

    val programIdsForUser: List<String>
        /**
         * Gets the list of program ids for this user.
         * @return a list of program ids.
         */
        get() {
            val result: MutableList<String> = ArrayList()
            if (mUserProgramsAndRoles != null) {
                for (programid in mUserProgramsAndRoles!!.keys) {
                    if (isProgramIdForUser(programid)) result.add(programid)
                }
            }
            return result
        }

    /**
     * Gets the friendly name for the given programid. If the friendly name can't be determined,
     * simply returns the programid.
     * @param programid for which friendly name is desired.
     * @return the friendly name, or the programid if the friendly name isn't available.
     */
    fun getFriendlyName(programid: String): String {
        return if (mProgramNames == null) programid else mProgramNames!![programid] ?: programid
    }
    /**
     * Attempt to allocate the next Talking Book serial number for this TBLoader. If there are
     * no serial numbers available locally, will attempt to call the server to allocate a new
     * block.
     *
     * @param success  A Consumer that accepts an integer of the next serial number.
     * @param failure  A Consumer that accepts an Exception from a failure.
     */
    //    public void allocateDeviceSerialNumber(Consumer<String> success, Consumer<Exception> failure) {
    //        mTbSrnHelper.allocateNextSrn(success, failure);
    //    }
    /**
     * Get the user config from server. If no connectivity, and have a cached config use that.
     *
     * @param listener A Listener to accept the results.
     */
    fun prepareForSerialNumberAllocation(listener: Listener?) {
//        mTbSrnHelper = new TbSrnHelper(mAppContext);
//        String email = getEmail();
//        mTbSrnHelper.prepareForAllocation(email, new Listener() {
//            @Override
//            public void onSuccess() {
//                String tbcd = mTbSrnHelper.getTbLoaderIdHex();
//                if (!mUserPrefs.getString(TBCDID_PROP, "").equalsIgnoreCase(tbcd)) {
//                    SharedPreferences.Editor prefsEditor = mUserPrefs.edit();
//                    prefsEditor.putString(TBCDID_PROP, tbcd);
//                    prefsEditor.apply();
//                }
//                listener.onSuccess();
//            }
//
//            @Override
//            public void onError() {
//                listener.onError();
//            }
//        });
    }

    fun saveProgramsInfoJSON(programsInfo: JSONObject) {
        val resultString = programsInfo.toString()
        if (mUserPrefs.getString(
                PROGRAMS_INFO_PROP,
                ""
            ) != resultString
        ) {
            val prefsEditor = mUserPrefs.edit()
            prefsEditor.putString(PROGRAMS_INFO_PROP, resultString)
            prefsEditor.apply()
        }
    }

    //    public void loadProgramsInfo() {
    //        String jsonString = mUserPrefs.getString(PROGRAMS_INFO_PROP, null);
    //        if (StringUtils.isNotBlank(jsonString)) {
    //            try {
    //                JSONObject jsonObject = new JSONObject(jsonString);
    //                parseProgramsInfoJSON(jsonObject);
    //            } catch (JSONException e) {
    //                e.printStackTrace();
    //            }
    //        }
    //    }
    //
    fun parseProgramsInfoJSON(programsInfo: JSONObject) {
        // TODO: this is not really needed since all programs are now on s3
        val programs_info: MutableMap<String, Map<String, String>> = HashMap()
        val implicit_repository = programsInfo.optString("implicit_repository", "dbx")
        try {
            // Covert the lame-ass android json object to something usable.
            val programs = programsInfo["programs"] as JSONObject
            val it = programs.keys()
            while (it.hasNext()) {
                val programId = it.next()
                try {
                    val programInfoJson = programs.getJSONObject(programId)
                    // Convert inner object to something usable.
                    val programInfo: MutableMap<String, String> = HashMap()
                    val it2 = programInfoJson.keys()
                    while (it2.hasNext()) {
                        val programInfoPropertyName = it2.next()
                        val programInfoProperty = programInfoJson.getString(programInfoPropertyName)
                        programInfo[programInfoPropertyName] = programInfoProperty
                    }
                    programs_info[programId] = programInfo
                } catch (e: JSONException) {
                    // Ignore the key. Should not happen IRL.
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        // Build map of {programid: "role,role"}
//        mUserProgramsAndRoles = programs_info.entries.stream()
//            .collect(
//                Collectors.toMap<Map.Entry<String, Map<String, String>>, String, String?>(
//                    Function<Map.Entry<String, Map<String, String>>, String> { (key, value) -> java.util.Map.Entry.key },
//                    Function<Map.Entry<String, Map<String, String>>, String?> { (_, value): Map.Entry<String, Map<String, String>> -> value["roles"] })
//            )
        // Build a list of [programid] of those programs with s3 as repository.
//        mS3RepositoryList = programs_info.entries.stream()
//            .filter { (_, value): Map.Entry<String, Map<String, String>> ->
//                (value["repository"] ?: implicit_repository).equals("s3", ignoreCase = true)
//            }
//            .map<String>(Function<Map.Entry<String, Map<String, String>>, String> { (key, value) -> java.util.Map.Entry.key })
//            .collect(Collectors.toList<String>())
        // Extract the friendly names as {programid: "name"}
//        mProgramNames = programs_info.entries.stream()
//            .collect(
//                Collectors.toMap<Map.Entry<String, Map<String, String>>, String, String>(
//                    Function<Map.Entry<String, Map<String, String>>, String> { (key, value) -> java.util.Map.Entry.key },
//                    Function<Map.Entry<String, Map<String, String>>, String> { (key, value): Map.Entry<String, Map<String, String>> ->
//                        value["name"] ?: key
//                    })
//            )
    }

    companion object {
        private val TAG = "TBL!:" + Config::class.java.simpleName

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
        private val TBLOADER_ROLES: Set<String> =
            HashSet(mutableListOf("*", "AD", "PM", "CO", "FO"))
        private const val USERNAME_PROP = "cognito:username"
        private const val EMAIL_PROP = "email"
        private const val NAME_PROP = "name" // "custom:greeting";

        // Programs info from "getprograms" API call is stored under this key.
        private const val PROGRAMS_INFO_PROP = "programs_info"
        private const val TBCDID_PROP = "tbcd"
    }
}
