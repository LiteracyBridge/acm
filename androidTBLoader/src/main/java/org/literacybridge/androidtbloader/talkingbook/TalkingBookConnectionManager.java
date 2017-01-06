package org.literacybridge.androidtbloader.talkingbook;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.UriPermission;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.storage.StorageManager;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import org.literacybridge.androidtbloader.TBLoaderAppContext;
import org.literacybridge.androidtbloader.db.TalkingBookDbHelper;
import org.literacybridge.androidtbloader.db.TalkingBookDbSchema.KnownTalkingBooksTable;
import org.literacybridge.core.fs.TbFile;
import org.literacybridge.core.tbloader.TBDeviceInfo;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TalkingBookConnectionManager {
    private static final String TAG = TalkingBookConnectionManager.class.getSimpleName();
    private static final String TB_FS_DEFAULT_SERIAL_NUMBER = "9285-F050";
    private static Integer [] GEN_PLUS_VENDOR_ID_LIST = {6975, 5325};
    private static Integer [] GEN_PLUS_PRODUCT_ID_LIST = {8194, 4636};
    private static Set<Integer> GEN_PLUS_VENDOR_IDS = new HashSet<>(Arrays.asList(GEN_PLUS_VENDOR_ID_LIST));
    private static Set<Integer> GEN_PLUS_PRODUCT_IDS = new HashSet<>(Arrays.asList(GEN_PLUS_PRODUCT_ID_LIST));

    private final TBLoaderAppContext mAppContext;
    private final UsbManager mUsbManagermanager;
    private final StorageManager mStorageManager;

    private final SQLiteDatabase mDatabase;
    private TalkingBookConnectionEventListener mTalkingBookConnectionEventListener;

    private volatile boolean mIsMounted;
    private volatile TalkingBook mConnectedTalkingBook;

    private volatile boolean mUsbWatcherDisabled;


    public TalkingBookConnectionManager(TBLoaderAppContext context) {
        mAppContext = context;
        mUsbManagermanager = (UsbManager) mAppContext.getSystemService(Context.USB_SERVICE);
        mStorageManager = (StorageManager) mAppContext.getSystemService(Context.STORAGE_SERVICE);
        mDatabase = new TalkingBookDbHelper(context).getWritableDatabase();

        checkMountedDevices();

        registerReceiver();
    }

    /**
     * Is there a device with vendor id 0x1b3f (GeneralPlus) and
     * product id 0x2002 (web camera) attached to the USB? (That's
     * how the Talking Book shows up.)
     * @return True if such a device is found, False otherwise
     */
    boolean isDeviceConnected() {
        Map<String, UsbDevice> map = mUsbManagermanager.getDeviceList();
        for (Map.Entry<String, UsbDevice> entry : map.entrySet()) {
            final UsbDevice device = entry.getValue();
            if (GEN_PLUS_VENDOR_IDS.contains(device.getVendorId()) && GEN_PLUS_PRODUCT_IDS.contains(device.getProductId())) {
                return true;
            }
        }
        return false;
    }

    boolean isDeviceMounted() {
        return mIsMounted;
    }

    void setUsbWatcherDisabled(boolean disabled) {
        mUsbWatcherDisabled = disabled;
    }

    public boolean hasDefaultPermission() {
        for (UriPermission uriPermission : mAppContext.getContentResolver().getPersistedUriPermissions()) {
            if (uriPermission.getUri().getPath().contains(TB_FS_DEFAULT_SERIAL_NUMBER)) {
                return true;
            }
        }

        return false;

    }

    void addPermission(Uri deviceBasePath) throws IllegalStateException {
        Map<String, MountedDevice> volumesMap = getSecondaryMountedVolumesMap();
        if (volumesMap.size() != 1) {
            throw new IllegalStateException("More than one USB devices connected.");
        }

        for (String key : volumesMap.keySet()) {
            ContentValues values = getContentValues(key, deviceBasePath);
            mDatabase.insert(KnownTalkingBooksTable.NAME, null, values);
            break;
        }
    }

    boolean hasPermission() {
        if (mConnectedTalkingBook != null) {
            return true;
        } else {
            try {
                Map<String, MountedDevice> volumesMap = getSecondaryMountedVolumesMap();
                Log.d(TAG, "getSecondaryMountedVolumesMap: " + getSecondaryMountedVolumesMap().size());
                for (Map.Entry<String, MountedDevice> device : volumesMap.entrySet()) {
                    Uri deviceBaseUri = getDeviceUri(device.getKey());
                    if (deviceBaseUri != null) {
                        return true;
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "Unable to connect to device", e);
            }

            return false;
        }
    }

    public TalkingBook getConnectedTalkingBook() {
        return mConnectedTalkingBook;
    }
    ////////////////////////////////////////////////////////////////////////////////
    // Debug code
    public void setSimulatedTalkingBook(TalkingBook simulatedTalkingBook) {
        mConnectedTalkingBook = simulatedTalkingBook;
    }
    // Debug code
    ////////////////////////////////////////////////////////////////////////////////

    public TalkingBook canAccessConnectedDevice() {
        if (mConnectedTalkingBook != null) {
            return mConnectedTalkingBook;
        }

        try {
            Map<String, MountedDevice> volumesMap = getSecondaryMountedVolumesMap();
            Log.d(TAG, "getSecondaryMountedVolumesMap: " + getSecondaryMountedVolumesMap().size());
            for (Map.Entry<String, MountedDevice> device : volumesMap.entrySet()) {
                Uri deviceBaseUri = getDeviceUri(device.getKey());
                Log.d(TAG, "key: " + device.getKey() + "  deviceBaseUri: " + deviceBaseUri);
                if (deviceBaseUri != null) {
                    DocumentFile root = DocumentFile.fromTreeUri(mAppContext, deviceBaseUri);
                    if (root != null && root.exists()) {
                        if (mConnectedTalkingBook == null) {
                            TbFile fs = new AndroidDocFile(root, mAppContext.getContentResolver());
                            mConnectedTalkingBook = new TalkingBook(fs, TBDeviceInfo.getSerialNumberFromFileSystem(fs), device.getValue().mLabel);
                            if (mTalkingBookConnectionEventListener != null) {
                                mTalkingBookConnectionEventListener.onTalkingBookConnectEvent(mConnectedTalkingBook);
                            }
                        }

                        Log.d(TAG, "root exists ");
                        return mConnectedTalkingBook;
                    }
                    Log.d(TAG, "root does not exist");
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Unable to connect to device", e);
        }

        return null;
    }

    private Uri getDeviceUri(String usbUid) {
        Cursor cursor = mDatabase.query(
                KnownTalkingBooksTable.NAME,
                null,
                KnownTalkingBooksTable.Cols.USB_UUID + " = ?",
                new String[] {usbUid},
                null,
                null,
                null
        );

        try {
            if (cursor.getCount() == 0) {
                return null;
            }

            cursor.moveToFirst();
            return Uri.parse(cursor.getString(cursor.getColumnIndex(KnownTalkingBooksTable.Cols.BASE_URI)));
        } finally {
            cursor.close();
        }
    }

    private void checkMountedDevices() {
        mIsMounted = !getSecondaryMountedVolumesMap().isEmpty();
    }

    private Map<String, MountedDevice> getSecondaryMountedVolumesMap() {
        Map<String, MountedDevice> volumesMap = new HashMap<>();

        try {
            Object[] volumes;

            Method getVolumeListMethod = mStorageManager.getClass().getMethod("getVolumeList");
            volumes = (Object[]) getVolumeListMethod.invoke(mStorageManager);

            for (Object volume : volumes) {
                Method getStateMethod = volume.getClass().getMethod("getState");
                String mState = (String) getStateMethod.invoke(volume);

                Method isPrimaryMethod = volume.getClass().getMethod("isPrimary");
                boolean mPrimary = (Boolean) isPrimaryMethod.invoke(volume);

                if (!mPrimary && mState.equals("mounted")) {
                    Method getPathMethod = volume.getClass().getMethod("getPath");
                    String path = (String) getPathMethod.invoke(volume);

                    Method getUuidMethod = volume.getClass().getMethod("getUuid");
                    String uuid = (String) getUuidMethod.invoke(volume);

                    Method getUserLabelMethod = volume.getClass().getMethod("getUserLabel");
                    String userLabel = (String) getUserLabelMethod.invoke(volume);
                    Log.d(TAG, "Found one mounted device: " + uuid + " -> " + volume + ", label=" + userLabel);

                    if (uuid != null && path != null) {
                        volumesMap.put(uuid, new MountedDevice(userLabel, uuid, path));
                    }
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Unable to load list of mounted secondary storage devices");
        }
        return volumesMap;
    }

    private static final class MountedDevice {
        private final String mLabel;
        private final String mUuid;
        private final String mPath;

        MountedDevice(String label, String uuid, String path) {
            mLabel = label;
            mUuid = uuid;
            mPath = path;
        }
    }


    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_REMOVED);
        filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        filter.addDataScheme("file");
        filter.setPriority(999);

        mAppContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                checkMountedDevices();
            }
        }, filter);
    }

    private static ContentValues getContentValues(String usbUid, Uri deviceBaseUri) {
        ContentValues values = new ContentValues();
        values.put(KnownTalkingBooksTable.Cols.USB_UUID, usbUid);
        values.put(KnownTalkingBooksTable.Cols.BASE_URI, deviceBaseUri.toString());

        return values;
    }

    private void onUSBEvent() {
        if (isDeviceConnected()) {
            if (!mUsbWatcherDisabled) {
                Intent setupIntent = TalkingBookConnectionSetupActivity.newIntent(mAppContext, false);
                setupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mAppContext.startActivity(setupIntent);
            }
        } else {
            if (mConnectedTalkingBook != null && mTalkingBookConnectionEventListener != null) {
                mTalkingBookConnectionEventListener.onTalkingBookDisConnectEvent();
            }
            mConnectedTalkingBook = null;
        }
    }

    public void setTalkingBookConnectionEventListener(TalkingBookConnectionEventListener talkingBookConnectionEventListener) {
        mTalkingBookConnectionEventListener = talkingBookConnectionEventListener;
    }

    public static class USBReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ((TBLoaderAppContext) context.getApplicationContext())
                    .getTalkingBookConnectionManager().onUSBEvent();
        }
    }

    public static class TalkingBook {
        private final TbFile mTalkingBookRoot;
        private final String mSerialNumber;
        private final String mDeviceLabel;

        public TalkingBook(TbFile talkingBookRoot, String serialNumber, String deviceLabel) {
            mTalkingBookRoot = talkingBookRoot;
            mSerialNumber = serialNumber;
            mDeviceLabel = deviceLabel;
        }

        public TbFile getTalkingBookRoot() {
            return mTalkingBookRoot;
        }

        public String getSerialNumber() {
            return mSerialNumber;
        }

        public String getDeviceLabel() {
            return mDeviceLabel;
        }
    }

    public interface TalkingBookConnectionEventListener {
        void onTalkingBookConnectEvent(TalkingBook connectedDevice);
        void onTalkingBookDisConnectEvent();
    }

 }
