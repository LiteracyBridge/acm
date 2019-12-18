package org.literacybridge.androidtbloader.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import org.literacybridge.androidtbloader.TBLoaderAppContext;
import org.literacybridge.androidtbloader.signin.UserHelper;
import org.literacybridge.core.tbloader.TbSrnAllocationInfo;

import java.util.function.Consumer;

import static org.literacybridge.core.tbloader.TbSrnAllocationInfo.TB_SRN_BACKUP_BEGIN_NAME;
import static org.literacybridge.core.tbloader.TbSrnAllocationInfo.TB_SRN_BACKUP_END_NAME;
import static org.literacybridge.core.tbloader.TbSrnAllocationInfo.TB_SRN_HEXID_NAME;
import static org.literacybridge.core.tbloader.TbSrnAllocationInfo.TB_SRN_ID_NAME;
import static org.literacybridge.core.tbloader.TbSrnAllocationInfo.TB_SRN_NEXTSRN_NAME;
import static org.literacybridge.core.tbloader.TbSrnAllocationInfo.TB_SRN_PRIMARY_BEGIN_NAME;
import static org.literacybridge.core.tbloader.TbSrnAllocationInfo.TB_SRN_PRIMARY_END_NAME;

public class TbSrnHelper {
    private static final String TAG = "TBL!:" + TbSrnHelper.class.getSimpleName();

    private static final String SRN_PREFS = "TB-Serial-Numbers";
    private final TBLoaderAppContext mAppContext;
    private TbSrnAllocationInfo mTbSrnAllocationInfo;
    private SharedPreferences mSrnPreferences;
    private String mEmail;

    public TbSrnHelper(TBLoaderAppContext appContext) {
        mAppContext = appContext;
    }

    public String getTbLoaderIdHex() { return mTbSrnAllocationInfo.getTbloaderidHex();}
    /**
     * Prepare to allocate SRNs by filling the TbSrnAllocationInfo from local storage. Try to allocate
     * more from server if we have none, or if the backup range is empty.
     * @param listener Callback for success or failure.
     */
    public void prepareForAllocation(String email, Config.Listener listener) {
        mEmail = email;
        // Lazy get the prefs object in which we store the SRN allocations.
        if (mSrnPreferences == null) {
            mSrnPreferences = mAppContext.getSharedPreferences(SRN_PREFS, Context.MODE_PRIVATE);
        }
        // Lazy read or initialize a TbSrnAllocationInfo object.
        if (mTbSrnAllocationInfo == null) {
            int id = mSrnPreferences.getInt(mEmail, -1);
            if (id >= 0) {
                mTbSrnAllocationInfo = fromPrefs(mSrnPreferences, id);
            }
        }
        if (mTbSrnAllocationInfo == null) {
            mTbSrnAllocationInfo = new TbSrnAllocationInfo();
        }

        // If we need a block of SRNs, try to get them now.
        if (mTbSrnAllocationInfo.getPrimaryBegin() == 0 || mTbSrnAllocationInfo.getBackupBegin() == 0) {
            getSrnBlock(listener::onSuccess, (ex) -> {
                Log.e(TAG, "Failed to allocate a SRN block", ex);
                listener.onError();
            });
        } else {
            // Didn't need to get any new SRNs.
            listener.onSuccess();
        }
    }

    void allocateNextSrn(Consumer<String> success, Consumer<Exception> failure) {
        if (mTbSrnAllocationInfo.hasNext()) {
            int next = mTbSrnAllocationInfo.allocateNext();
            toPrefs(mSrnPreferences, mEmail, mTbSrnAllocationInfo);
            success.accept(mTbSrnAllocationInfo.formatSrn(next));
        } else {
            // Out of SRNs. Try to allocate more.
            // Couldn't get a SRN.
            getSrnBlock(()->{
                    // Got some, so the TbSrnAllocationInfo was filled in. Allocate one and return it.
                    int next = mTbSrnAllocationInfo.allocateNext();
                    toPrefs(mSrnPreferences, mEmail, mTbSrnAllocationInfo);
                success.accept(mTbSrnAllocationInfo.formatSrn(next));
                }, failure
            );
        }
    }

    void getSrnBlock(Runnable success, Consumer<Exception> failure) {
        // If no SRNs, allocate two blocks worth, otherwise just refill the backup if it is empty.
        int nBlocks = mTbSrnAllocationInfo == null || mTbSrnAllocationInfo.getPrimaryBegin() == 0 ? 2 : 1;
        int nSRNs = nBlocks * blocksize();
        // Make the call
        allocateTbSrnBlock(nSRNs, (id,hexid,begin,end)->{
            // Success. Fill the AllocationInfo from the reservation, and persist the info.
            mTbSrnAllocationInfo.applyReservation(id, hexid, begin, end);
            toPrefs(mSrnPreferences, mEmail, mTbSrnAllocationInfo);
            success.run();
        }, ex->{
            // Couldn't get any SRNs. But, if we still have any to allocate, call it success.
            if (mTbSrnAllocationInfo.hasNext()) {
                success.run();
            } else {
                // Don't have any SRNs to allocate.
                failure.accept(ex);
            }
        });
    }

    // Give the dev very small blocks!
    private int _blocksize = -1;
    private int blocksize() {
        if (_blocksize < 0) {
            _blocksize = (UserHelper.getAuthenticationPayload("email").equals("bill@amplio.org")) ? 2 : 20;
        }
        return _blocksize;
    }

    @SuppressLint("ApplySharedPref")
    private void toPrefs(SharedPreferences prefs, String email, TbSrnAllocationInfo allocationInfo) {
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putInt(email, allocationInfo.getTbloaderid());
        String prefix = allocationInfo.getTbloaderid() + ".";

        prefsEditor.putInt(prefix + TB_SRN_ID_NAME, allocationInfo.getTbloaderid());
        prefsEditor.putString(prefix + TB_SRN_HEXID_NAME, allocationInfo.getTbloaderidHex());
        prefsEditor.putInt(prefix + TB_SRN_NEXTSRN_NAME, allocationInfo.getNextSrn());
        prefsEditor.putInt(prefix + TB_SRN_PRIMARY_BEGIN_NAME, allocationInfo.getPrimaryBegin());
        prefsEditor.putInt(prefix + TB_SRN_PRIMARY_END_NAME, allocationInfo.getPrimaryEnd());
        prefsEditor.putInt(prefix + TB_SRN_BACKUP_BEGIN_NAME, allocationInfo.getBackupBegin());
        prefsEditor.putInt(prefix + TB_SRN_BACKUP_END_NAME, allocationInfo.getBackupEnd());

        // Commit, not apply, because we're willing to wait to be sure it is persisted.
        prefsEditor.commit();
    }

    /**
     * Helper to instantiate a TbSrnAllocationInfo from the prefs object where they're stored.
     * @param prefs from which to load.
     * @param id of the prefs to load.
     * @return the TbSrnAllocationInfo object.
     */
    private TbSrnAllocationInfo fromPrefs(SharedPreferences prefs, int id) {
        TbSrnAllocationInfo result = null;
        String prefix = id + ".";
        int tbloaderid = prefs.getInt(prefix + TB_SRN_ID_NAME, -1);
        String tbloaderidHex = prefs.getString(prefix + TB_SRN_HEXID_NAME, "");
        int nextSrn = prefs.getInt(prefix + TB_SRN_NEXTSRN_NAME, -1);
        int primaryBase = prefs.getInt(prefix + TB_SRN_PRIMARY_BEGIN_NAME, 0);
        int primaryEnd = prefs.getInt(prefix + TB_SRN_PRIMARY_END_NAME, 0);
        int backupBase = prefs.getInt(prefix + TB_SRN_BACKUP_BEGIN_NAME, 0);
        int backupEnd = prefs.getInt(prefix + TB_SRN_BACKUP_END_NAME, 0);
        if (tbloaderid == id && ( (nextSrn >= primaryBase && nextSrn < primaryEnd)
                                 || (nextSrn == 0 && primaryBase == 0 && primaryEnd == 0 && backupBase == 0 && backupEnd == 0) )) {
            result = new TbSrnAllocationInfo(tbloaderid,
                    tbloaderidHex,
                    nextSrn,
                    primaryBase,
                    primaryEnd,
                    backupBase,
                    backupEnd);

        }
        return result;
    }


  /**
     * Callbacks for SRN block allocation.
     */
    private interface AllocateTbSrnBlockSuccess {
        void onSuccess(int id, String hexid, int begin, int end);
    }
    /**
     * Makes a REST call to a Lambda function to allocate a block of TB serial numbers.
     * @param n the number of SRNs desired.
     * @param successListener callback for success.
     * @param failureListener callback for failure.
     */
    private void allocateTbSrnBlock(int n, final AllocateTbSrnBlockSuccess successListener, Consumer<Exception> failureListener) {
        String baseURL = "https://lj82ei7mce.execute-api.us-west-2.amazonaws.com/Prod";
        String requestURL = baseURL + "/reserve";
        if (n > 0) requestURL += "?n=" + n;

        HttpHelper.authenticatedRestCall(requestURL,
                response -> {
                    JSONObject result;
                    try {
                        result = response.getJSONObject("result");
                        if (result != null && result.getString("status").equals("ok")) {
                            int begin = result.getInt("begin");
                            int end = result.getInt("end");
                            int id = result.getInt("id");
                            String hexid = result.getString("hexid");
                            successListener.onSuccess(id, hexid, begin, end);
                        } else {
                            failureListener.accept(new Exception("Could not get SRN block."));
                        }
                    } catch (JSONException e) {
                        failureListener.accept(e);
                    }
                },
                failureListener::accept);
    }


}
