package org.literacybridge.tbloaderandroid.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.literacybridge.core.tbloader.TbSrnAllocationInfo
import org.literacybridge.tbloaderandroid.App
import org.literacybridge.tbloaderandroid.api_services.NetworkModule
import org.literacybridge.tbloaderandroid.util.Constants.Companion.LOG_TAG
import java.util.function.Consumer

class TBSerialNumberHelper {
    private val mAppContext: Context = App.context
    private var mTbSrnAllocationInfo: TbSrnAllocationInfo? = null
    private var mSrnPreferences: SharedPreferences? = null
    private var mEmail: String? = null
    val tbLoaderIdHex: String
        get() = mTbSrnAllocationInfo!!.tbloaderidHex

    /**
     * Prepare to allocate SRNs by filling the TbSrnAllocationInfo from local storage. Try to allocate
     * more from server if we have none, or if the backup range is empty.
     * @param listener Callback for success or failure.
     */
    suspend fun prepareForAllocation(email: String?): String? {
        mEmail = email
        // Lazy get the prefs object in which we store the SRN allocations.
        if (mSrnPreferences == null) {
            mSrnPreferences = mAppContext.getSharedPreferences(SRN_PREFS, Context.MODE_PRIVATE)
        }
        // Lazy read or initialize a TbSrnAllocationInfo object.
        if (mTbSrnAllocationInfo == null) {
            val id = mSrnPreferences!!.getInt(mEmail, -1)
            if (id >= 0) {
                mTbSrnAllocationInfo = fromPrefs(mSrnPreferences!!, id)
            }
        }
        if (mTbSrnAllocationInfo == null) {
            mTbSrnAllocationInfo = TbSrnAllocationInfo()
        }

        // If we need a block of SRNs, try to get them now.
        if (mTbSrnAllocationInfo!!.primaryBegin == 0 || mTbSrnAllocationInfo!!.backupBegin == 0) {
            val result = getSrnBlock()
            if (result != null) {
                val next = mTbSrnAllocationInfo!!.allocateNext()
                return mTbSrnAllocationInfo!!.formatSrn(next)
            }
            Log.e(
                TAG,
                "Failed to allocate a SRN block",
            )
            return null
//            getSrnBlock(
//                { listener.onSuccess() }
//            ) { ex: Exception? ->
//                Log.e(
//                    TAG,
//                    "Failed to allocate a SRN block",
//                    ex
//                )
//                listener.onError()
//            }
        }
        // Didn't need to get any new SRNs.
        return null
//            listener.onSuccess()
    }

    suspend fun allocateNextSrn(
        success: Consumer<String?>,
        failure: Consumer<Exception?>
    ): String? {
        if (mTbSrnAllocationInfo!!.hasNext()) {
            val next = mTbSrnAllocationInfo!!.allocateNext()
            return mTbSrnAllocationInfo!!.formatSrn(next)
//            toPrefs(mSrnPreferences, mEmail, mTbSrnAllocationInfo)
//            success.accept(mTbSrnAllocationInfo!!.formatSrn(next))
        } else {
            // Out of SRNs. Try to allocate more.
            // Couldn't get a SRN.
            val result = getSrnBlock()
            if (result != null) {
                val next = mTbSrnAllocationInfo!!.allocateNext()
                return mTbSrnAllocationInfo!!.formatSrn(next)
            }
        }
        return null
    }

    suspend fun getSrnBlock(): TbSrnAllocationInfo? {
        // If no SRNs, allocate two blocks worth, otherwise just refill the backup if it is empty.
        val nBlocks =
            if (mTbSrnAllocationInfo == null || mTbSrnAllocationInfo!!.primaryBegin == 0) 2 else 1
        val nSRNs = nBlocks * blocksize()
        try {
            val response = NetworkModule().instance().reserveSerialNumber(nSRNs)
            val result = response.data[0]
            if (result.status == "ok") {
                // Success. Fill the AllocationInfo from the reservation, and persist the info.
                mTbSrnAllocationInfo!!.applyReservation(
                    result.id!!,
                    result.hexid,
                    result.begin!!,
                    result.end!!
                )
                // TODO: add serial number to datastore
//                toPrefs(mSrnPreferences, mEmail, mTbSrnAllocationInfo)
//                success.run()
                return mTbSrnAllocationInfo
            }
            // Couldn't get any SRNs. But, if we still have any to allocate, call it success.
            if (mTbSrnAllocationInfo!!.hasNext()) {
                return mTbSrnAllocationInfo
            }
            return null
        } catch (e: Exception) {
            // Couldn't get any SRNs. But, if we still have any to allocate, call it success.
            if (mTbSrnAllocationInfo!!.hasNext()) {
                return mTbSrnAllocationInfo
            }
            // Don't have any SRNs to allocate.
            // TODO: log error operationsLog
            throw e
        }

    }

    // Give the dev very small blocks!
    private var _blocksize = -1

    private fun blocksize(): Int {
        if (_blocksize < 0) {
            _blocksize = if (dataStoreManager.currentUser!!.email.equals("bill@amplio.org")
            ) 2 else 512
        }
        return _blocksize
    }

    @SuppressLint("ApplySharedPref")
    private fun toPrefs(
        prefs: SharedPreferences?,
        email: String?,
        allocationInfo: TbSrnAllocationInfo?
    ) {
        val prefsEditor = prefs!!.edit()
        prefsEditor.putInt(email, allocationInfo!!.tbloaderid)
        val prefix = allocationInfo.tbloaderid.toString() + "."
        prefsEditor.putInt(prefix + TbSrnAllocationInfo.TB_SRN_ID_NAME, allocationInfo.tbloaderid)
        prefsEditor.putString(
            prefix + TbSrnAllocationInfo.TB_SRN_HEXID_NAME,
            allocationInfo.tbloaderidHex
        )
        prefsEditor.putInt(prefix + TbSrnAllocationInfo.TB_SRN_NEXTSRN_NAME, allocationInfo.nextSrn)
        prefsEditor.putInt(
            prefix + TbSrnAllocationInfo.TB_SRN_PRIMARY_BEGIN_NAME,
            allocationInfo.primaryBegin
        )
        prefsEditor.putInt(
            prefix + TbSrnAllocationInfo.TB_SRN_PRIMARY_END_NAME,
            allocationInfo.primaryEnd
        )
        prefsEditor.putInt(
            prefix + TbSrnAllocationInfo.TB_SRN_BACKUP_BEGIN_NAME,
            allocationInfo.backupBegin
        )
        prefsEditor.putInt(
            prefix + TbSrnAllocationInfo.TB_SRN_BACKUP_END_NAME,
            allocationInfo.backupEnd
        )

        // Commit, not apply, because we're willing to wait to be sure it is persisted.
        prefsEditor.commit()
    }

    /**
     * Helper to instantiate a TbSrnAllocationInfo from the prefs object where they're stored.
     * @param prefs from which to load.
     * @param id of the prefs to load.
     * @return the TbSrnAllocationInfo object.
     */
    private fun fromPrefs(prefs: SharedPreferences, id: Int): TbSrnAllocationInfo? {
        var result: TbSrnAllocationInfo? = null
        val prefix = "$id."
        val tbloaderid = prefs.getInt(prefix + TbSrnAllocationInfo.TB_SRN_ID_NAME, -1)
        val tbloaderidHex = prefs.getString(prefix + TbSrnAllocationInfo.TB_SRN_HEXID_NAME, "")
        val nextSrn = prefs.getInt(prefix + TbSrnAllocationInfo.TB_SRN_NEXTSRN_NAME, -1)
        val primaryBase = prefs.getInt(prefix + TbSrnAllocationInfo.TB_SRN_PRIMARY_BEGIN_NAME, 0)
        val primaryEnd = prefs.getInt(prefix + TbSrnAllocationInfo.TB_SRN_PRIMARY_END_NAME, 0)
        val backupBase = prefs.getInt(prefix + TbSrnAllocationInfo.TB_SRN_BACKUP_BEGIN_NAME, 0)
        val backupEnd = prefs.getInt(prefix + TbSrnAllocationInfo.TB_SRN_BACKUP_END_NAME, 0)
        if (tbloaderid == id && (nextSrn >= primaryBase && nextSrn < primaryEnd || nextSrn == 0 && primaryBase == 0 && primaryEnd == 0 && backupBase == 0 && backupEnd == 0)) {
            result = TbSrnAllocationInfo(
                tbloaderid,
                tbloaderidHex,
                nextSrn,
                primaryBase,
                primaryEnd,
                backupBase,
                backupEnd
            )
        }
        return result
    }

    /**
     * Callbacks for SRN block allocation.
     */
    private fun interface AllocateTbSrnBlockSuccess {
        fun onSuccess(id: Int, hexid: String?, begin: Int, end: Int)
    }

    /**
     * Makes a REST call to a Lambda function to allocate a block of TB serial numbers.
     * @param n the number of SRNs desired.
     * @param successListener callback for success.
     * @param failureListener callback for failure.
     */
//    private suspend fun allocateTbSrnBlock(
//        n: Int,
//        successListener: AllocateTbSrnBlockSuccess,
//        failureListener: Consumer<Exception>
//    ) {
//        var requestURL = "$SRN_HELPER_URL/reserve"
////        lateinit var response: ApiResponseModel<TalkingBookSerial>
//
////        if (n > 0) requestURL += "?n=$n"
//        val response = NetworkModule().instance().reserveSerialNumber(n)
//
//        HttpHelper.authenticatedRestCall(requestURL,
//            { response ->
//                val result: JSONObject
//                try {
//                    result = response.getJSONObject("result")
//                    if (result != null && result.getString("status") == "ok") {
//                        val begin = result.getInt("begin")
//                        val end = result.getInt("end")
//                        val id = result.getInt("id")
//                        val hexid = result.getString("hexid")
//                        successListener.onSuccess(id, hexid, begin, end)
//                    } else {
//                        failureListener.accept(Exception("Could not get SRN block."))
//                    }
//                } catch (e: JSONException) {
//                    failureListener.accept(e)
//                }
//            }) { failureListener.accept(it) }
//    }

    companion object {
        private val TAG = "$LOG_TAG Talking Book Serial Number:"
        private const val SRN_PREFS = "TB-Serial-Numbers"
    }
}

