package org.literacybridge.tbloaderandroid.util.device_manager
//
//import android.content.BroadcastReceiver
//import android.content.ContentValues
//import android.content.Context
//import android.content.Intent
//import android.content.IntentFilter
//import android.database.sqlite.SQLiteDatabase
//import android.hardware.usb.UsbDevice
//import android.hardware.usb.UsbManager
//import android.net.Uri
//import android.os.storage.StorageManager
//import android.util.Log
//import androidx.documentfile.provider.DocumentFile
//import androidx.localbroadcastmanager.content.LocalBroadcastManager
////import org.literacybridge.androidtbloader.TBLoaderAppContext
//import org.literacybridge.androidtbloader.db.TalkingBookDbHelper
//import org.literacybridge.androidtbloader.db.TalkingBookDbSchema.KnownTalkingBooksTable
//import org.literacybridge.core.fs.TbFile
//import org.literacybridge.core.tbdevice.TbDeviceInfo
//import org.literacybridge.androidtbloader.App
//import java.util.Arrays
//import kotlin.concurrent.Volatile
//
//
//class ConnectionManager(context: App) {
//    private val mAppContext: App
//    private val mUsbManagermanager: UsbManager
//    private val mStorageManager: StorageManager
//    private val mDatabase: SQLiteDatabase
//    private var mTalkingBookConnectionEventListener: TalkingBookConnectionEventListener? = null
//
//    @Volatile
//    var isDeviceMounted = false
//        private set
//
//    @Volatile
//    private var mConnectedTalkingBook: TalkingBook? = null
//
//    @Volatile
//    private var mSimulatedTalkingBook: TalkingBook? = null
//
//    @Volatile
//    private var mUsbWatcherDisabled = false
//
//    init {
//        mAppContext = context
//        mUsbManagermanager = mAppContext.getSystemService(Context.USB_SERVICE)
//        mStorageManager = mAppContext.getSystemService(Context.STORAGE_SERVICE)
//        mDatabase = TalkingBookDbHelper(context).getWritableDatabase()
//        checkMountedDevices()
//        registerReceiver()
//    }
//
//    val isDeviceConnected: Boolean
//        /**
//         * Is there a device with vendor id 0x1b3f (GeneralPlus) and
//         * product id 0x2002 (web camera) attached to the USB? (That's
//         * how the Talking Book shows up.)
//         * @return True if such a device is found, False otherwise
//         */
//        get() {
//            val map: Map<String?, UsbDevice> = mUsbManagermanager.deviceList
//            for ((_, device) in map) {
//                if (GEN_PLUS_VENDOR_IDS.contains(device.vendorId) && GEN_PLUS_PRODUCT_IDS.contains(
//                        device.productId
//                    )
//                ) {
//                    return true
//                }
//            }
//            return false
//        }
//
//    fun setUsbWatcherDisabled(disabled: Boolean) {
//        mUsbWatcherDisabled = disabled
//    }
//
//    fun hasDefaultPermission(): Boolean {
//        for (uriPermission in mAppContext.getContentResolver().getPersistedUriPermissions()) {
//            if (uriPermission.uri.path!!.contains(TB_FS_DEFAULT_SERIAL_NUMBER)) {
//                return true
//            }
//        }
//        return false
//    }
//
//    @Throws(IllegalStateException::class)
//    fun addPermission(deviceBasePath: Uri) {
//        val volumesMap =
//            secondaryMountedVolumesMap
//        check(volumesMap.size == 1) { "More than one USB devices connected." }
//        for (key in volumesMap.keys) {
//            val values = getContentValues(key, deviceBasePath)
//            mDatabase.insert(KnownTalkingBooksTable.NAME, null, values)
//            break
//        }
//    }
//
//    fun hasPermission(): Boolean {
//        return if (mConnectedTalkingBook != null) {
//            true
//        } else {
//            try {
//                val volumesMap =
//                    secondaryMountedVolumesMap
//                Log.d(TAG, "getSecondaryMountedVolumesMap: " + secondaryMountedVolumesMap.size)
//                for ((key): Map.Entry<String, MountedDevice> in volumesMap)  {
//                    val deviceBaseUri = getDeviceUri(key)
//                    if (deviceBaseUri != null) {
//                        return true
//                    }
//                }
//            } catch (e: Exception) {
//                Log.d(TAG, "Unable to connect to device", e)
//            }
//            false
//        }
//    }
//
//    val connectedTalkingBook: TalkingBook?
//        get() = if (mSimulatedTalkingBook != null) mSimulatedTalkingBook else mConnectedTalkingBook
//
//    ////////////////////////////////////////////////////////////////////////////////
//    // Debug code
//    fun setSimulatedTalkingBook(simulatedTalkingBook: TalkingBook?) {
//        mSimulatedTalkingBook = simulatedTalkingBook
//    }
//
//    // Debug code
//    ////////////////////////////////////////////////////////////////////////////////
//    fun canAccessConnectedDevice(): TalkingBook? {
//        if (mSimulatedTalkingBook != null) {
//            return mSimulatedTalkingBook
//        }
//        try {
//            val volumesMap =
//                secondaryMountedVolumesMap
//            Log.d(TAG, "getSecondaryMountedVolumesMap: " + secondaryMountedVolumesMap.size)
//            for ((key, value) in volumesMap) {
//                val deviceBaseUri = getDeviceUri(key)
//                Log.d(
//                    TAG,
//                    "key: $key  deviceBaseUri: $deviceBaseUri"
//                )
//                if (deviceBaseUri != null) {
//                    val root = DocumentFile.fromTreeUri(mAppContext, deviceBaseUri)
//                    if (root != null && root.exists()) {
//                        if (mConnectedTalkingBook == null) {
//                            val fs: TbFile = AndroidDocFile(root, mAppContext.getContentResolver())
//                            mConnectedTalkingBook = TalkingBook(
//                                fs,
//                                TbDeviceInfo.getSerialNumberFromFileSystem(fs),
//                                value.mLabel, value.mPath
//                            )
//                            if (mTalkingBookConnectionEventListener != null) {
//                                Log.d(TAG, "Sending Talking Book connection event")
//                                mTalkingBookConnectionEventListener!!.onTalkingBookConnectEvent(
//                                    mConnectedTalkingBook
//                                )
//                            } else {
//                                Log.d(
//                                    TAG,
//                                    "Not sending Talking Book connection event; no listener"
//                                )
//                            }
//                            val intent = Intent(TB_CONNECTION_STATUS)
//                            LocalBroadcastManager.getInstance(TBLoaderAppContext.getInstance())
//                                .sendBroadcast(intent)
//                        }
//                        Log.d(TAG, "root exists ")
//                        return mConnectedTalkingBook
//                    }
//                    Log.d(TAG, "root does not exist")
//                }
//            }
//        } catch (e: Exception) {
//            Log.d(TAG, "Unable to connect to device", e)
//        }
//        return null
//    }
//
//    private fun getDeviceUri(usbUid: String): Uri? {
//        val cursor = mDatabase.query(
//            KnownTalkingBooksTable.NAME,
//            null,
//            KnownTalkingBooksTable.Cols.USB_UUID + " = ?", arrayOf<String>(usbUid),
//            null,
//            null,
//            null
//        )
//        return try {
//            if (cursor.count == 0) {
//                return null
//            }
//            cursor.moveToFirst()
//            Uri.parse(cursor.getString(cursor.getColumnIndex(KnownTalkingBooksTable.Cols.BASE_URI)))
//        } finally {
//            cursor.close()
//        }
//    }
//
//    private fun checkMountedDevices() {
//        isDeviceMounted = !secondaryMountedVolumesMap.isEmpty()
//    }
//
//    /**
//     * We need to be able to reliably unmount the Talking Book, but that is not possible.
//     * Unfortunately, Google has apparently stopped supporting USB drives in applications.
//     * @param talkingBook The TalkingBook to be unmounted.
//     * @return True if unmounted successfully, false otherwise. So, false always.
//     */
//    fun unMount(talkingBook: TalkingBook): Boolean {
//        // If no path, it's not a real USB, so nothing to do.
//        return if (talkingBook.mPath == null) true else false
//        // TODO: If we ever learn how to do this, implement it. This fails with a missing permission
//        // MOUNT_UNMOUNT_FILESYSTEMS (we DO declare that permission in the manifest, but Google
//        // ignores that, too.)
////        try {
////            // mMountService is a private field on the StorageManager.  Get it.
////            Field mMountService = mStorageManager.getClass().getDeclaredField("mMountService");
////            mMountService.setAccessible(true);
////            Object mountService = mMountService.get(mStorageManager);
////            // unmountVolume(String mountPoint, boolean force, boolean removeEncryption) is a method of IMountService
////            Method unmountVolume = mountService.getClass().getMethod("unmountVolume", String.class, boolean.class, boolean.class);
////            unmountVolume.invoke(mountService, talkingBook.mPath, false, false);
////            success = true;
////            // TODO: I think we need to wait for a callback that the USB has disconnected.
////        } catch (NoSuchFieldException e) {
////            e.printStackTrace();
////        } catch (IllegalAccessException e) {
////            e.printStackTrace();
////        } catch (NoSuchMethodException e) {
////            e.printStackTrace();
////        } catch (InvocationTargetException e) {
////            e.printStackTrace();
////        }
//    }
//
//    private val secondaryMountedVolumesMap: Map<String, MountedDevice>
//        private get() {
//            val volumesMap: MutableMap<String, MountedDevice> = HashMap()
//            try {
//                val volumes: Array<Any>
//                val getVolumeListMethod = mStorageManager.javaClass.getMethod("getVolumeList")
//                volumes = getVolumeListMethod.invoke(mStorageManager) as Array<Any>
//                for (volume in volumes) {
//                    val getStateMethod = volume.javaClass.getMethod("getState")
//                    val mState = getStateMethod.invoke(volume) as String
//                    val isPrimaryMethod = volume.javaClass.getMethod("isPrimary")
//                    val mPrimary = isPrimaryMethod.invoke(volume) as Boolean
//                    if (!mPrimary && mState == "mounted") {
//                        val getPathMethod = volume.javaClass.getMethod("getPath")
//                        val path = getPathMethod.invoke(volume) as String
//                        val getUuidMethod = volume.javaClass.getMethod("getUuid")
//                        val uuid = getUuidMethod.invoke(volume) as String
//                        val getUserLabelMethod = volume.javaClass.getMethod("getUserLabel")
//                        val userLabel = getUserLabelMethod.invoke(volume) as String
//                        Log.d(
//                            TAG,
//                            "Found one mounted device: $uuid -> $volume, label=$userLabel"
//                        )
//                        if (uuid != null && path != null) {
//                            volumesMap[uuid] = MountedDevice(userLabel, uuid, path)
//                        }
//                    }
//                }
//            } catch (e: Exception) {
//                Log.d(TAG, "Unable to load list of mounted secondary storage devices")
//            }
//            return volumesMap
//        }
//
//    private class MountedDevice internal constructor(
//        private val mLabel: String,
//        private val mUuid: String,
//        private val mPath: String
//    )
//
//    private fun registerReceiver() {
//        val filter = IntentFilter()
//        filter.addAction(Intent.ACTION_MEDIA_MOUNTED)
//        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED)
//        filter.addAction(Intent.ACTION_MEDIA_REMOVED)
//        filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL)
//        filter.addDataScheme("file")
//        filter.priority = 999
//        mAppContext.registerReceiver(object : BroadcastReceiver() {
//            override fun onReceive(context: Context, intent: Intent) {
//                checkMountedDevices()
//            }
//        }, filter)
//    }
//
//    private fun onUSBEvent() {
//        if (isDeviceConnected) {
//            if (!mUsbWatcherDisabled) {
//                val setupIntent: Intent = TalkingBookConnectionSetupActivity.newIntent(mAppContext)
//                setupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                mAppContext.startActivity(setupIntent)
//            }
//        } else {
//            if (mConnectedTalkingBook != null && mTalkingBookConnectionEventListener != null) {
//                mTalkingBookConnectionEventListener!!.onTalkingBookDisConnectEvent()
//            }
//            mConnectedTalkingBook = null
//            val intent = Intent(TB_CONNECTION_STATUS)
//            LocalBroadcastManager.getInstance(TBLoaderAppContext.getInstance())
//                .sendBroadcast(intent)
//        }
//    }
//
//    fun setTalkingBookConnectionEventListener(talkingBookConnectionEventListener: TalkingBookConnectionEventListener?) {
//        mTalkingBookConnectionEventListener = talkingBookConnectionEventListener
//    }
//
//    class USBReceiver : BroadcastReceiver() {
//        override fun onReceive(context: Context, intent: Intent) {
//            (context.applicationContext as TBLoaderAppContext)
//                .getTalkingBookConnectionManager().onUSBEvent()
//        }
//    }
//
//    class TalkingBook(
//        val talkingBookRoot: TbFile,
//        val serialNumber: String,
//        val deviceLabel: String,
//        private val mPath: String?
//    )
//
//    interface TalkingBookConnectionEventListener {
//        fun onTalkingBookConnectEvent(connectedDevice: TalkingBook?)
//        fun onTalkingBookDisConnectEvent()
//    }
//
//    companion object {
//        private val TAG = "TBL!:" + ConnectionManager::class.java.simpleName
//        private const val TB_FS_DEFAULT_SERIAL_NUMBER = "9285-F050"
//        const val TB_CONNECTION_STATUS = "tb_connection_status"
//
//        // Genplus, sdAdapter, armKeil
//        private val GEN_PLUS_VENDOR_ID_LIST = arrayOf(6975, 5325, 1003)
//        private val GEN_PLUS_PRODUCT_ID_LIST = arrayOf(8194, 4636, 9252)
//        private val GEN_PLUS_VENDOR_IDS: Set<Int> = HashSet(Arrays.asList(*GEN_PLUS_VENDOR_ID_LIST))
//        private val GEN_PLUS_PRODUCT_IDS: Set<Int> =
//            HashSet(Arrays.asList(*GEN_PLUS_PRODUCT_ID_LIST))
//
//        private fun getContentValues(usbUid: String, deviceBaseUri: Uri): ContentValues {
//            val values = ContentValues()
//            values.put(KnownTalkingBooksTable.Cols.USB_UUID, usbUid)
//            values.put(KnownTalkingBooksTable.Cols.BASE_URI, deviceBaseUri.toString())
//            return values
//        }
//    }
//}
//
