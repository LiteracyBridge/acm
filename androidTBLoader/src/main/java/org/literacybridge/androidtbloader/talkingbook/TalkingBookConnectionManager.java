package org.literacybridge.androidtbloader.talkingbook;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.storage.StorageManager;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import org.literacybridge.androidtbloader.DeploymentPackage;
import org.literacybridge.androidtbloader.TBLoaderAppContext;
import org.literacybridge.androidtbloader.db.TalkingBookDBHelper;
import org.literacybridge.androidtbloader.db.TalkingBookDBSchema.KnownTalkingBooksTable;
import org.literacybridge.androidtbloader.dropbox.DropboxFileSystem;
import org.literacybridge.androidtbloader.dropbox.IOHandler;
import org.literacybridge.core.ProgressListener;
import org.literacybridge.core.fs.DefaultTBFileSystem;
import org.literacybridge.core.fs.RelativePath;
import org.literacybridge.core.fs.TBFileSystem;
import org.literacybridge.core.tbloader.DeploymentInfo;
import org.literacybridge.core.tbloader.TBDeviceInfo;
import org.literacybridge.core.tbloader.TBLoaderConfig;
import org.literacybridge.core.tbloader.TBLoaderConstants;
import org.literacybridge.core.tbloader.TBLoaderCore;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class TalkingBookConnectionManager {
    private static final String TAG = TalkingBookConnectionManager.class.getSimpleName();
    private static final String TB_FS_DEFAULT_SERIAL_NUMBER = "9285-F050";

    private final TBLoaderAppContext mAppContext;
    private final UsbManager mUsbManagermanager;
    private final StorageManager mStorageManager;

    private final SQLiteDatabase mDatabase;
    private TalkingBookConnectionEventListener mTalkingBookConnectionEventListener;

    private volatile boolean mIsMounted;
    private volatile TalkingBook mConnectedTalkingBook;

    private volatile boolean mUsbWatcherDisabled;

    private final LocationManager mLocationManager;

    public TalkingBookConnectionManager(TBLoaderAppContext context) {
        mAppContext = context;
        mUsbManagermanager = (UsbManager) mAppContext.getSystemService(Context.USB_SERVICE);
        mStorageManager = (StorageManager) mAppContext.getSystemService(Context.STORAGE_SERVICE);
        mLocationManager = (LocationManager) mAppContext.getSystemService(Context.LOCATION_SERVICE);
        mDatabase = new TalkingBookDBHelper(context).getWritableDatabase();

        checkMountedDevices();

        registerReceiver();
    }

    public boolean isDeviceConnected() {
        Map<String, UsbDevice> map = mUsbManagermanager.getDeviceList();
        for (Map.Entry<String, UsbDevice> entry : map.entrySet()) {
            final UsbDevice device = entry.getValue();
            if (device.getVendorId() == 6975 && device.getProductId() == 8194) {
                return true;
            }
        }
        return false;
    }

    public boolean isDeviceMounted() {
        return mIsMounted;
    }

    public void setUsbWatcherDisabled(boolean disabled) {
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

    public void addPermission(Uri deviceBasePath) throws IllegalStateException {
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

    public boolean hasPermission() {
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
                            TBFileSystem fs = new AndroidTBFileSystem(mAppContext.getContentResolver(), root);
                            mConnectedTalkingBook = new TalkingBook(fs, TBLoaderCore.getSerialNumberFromSystem(fs), device.getValue().mLabel);
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

        public MountedDevice(String label, String uuid, String path) {
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

    public void onUSBEvent() {
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

    public class TalkingBook {
        private final TBFileSystem mTalkingBookFileSystem;
        private final String mSerialNumber;
        private final String mDeviceLabel;

        private TalkingBook(TBFileSystem talkingBookFileSystem, String serialNumber, String deviceLabel) {
            mTalkingBookFileSystem = talkingBookFileSystem;
            mSerialNumber = serialNumber;
            mDeviceLabel = deviceLabel;
        }

        public TBFileSystem getTalkingBookFileSystem() {
            return mTalkingBookFileSystem;
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

    public void installDeploymentPackage(DeploymentPackage deploymentPackage,
                                         String community,
                                         ProgressListener progressListener) throws IOException {
        final TalkingBook talkingBook = mConnectedTalkingBook;

        File f = new File(mAppContext.getExternalFilesDir(IOHandler.EXTERNAL_FILES_TYPE), deploymentPackage.getLocalUnzipPath().asString());
        f = new File(f, "content");
        Log.d(TAG, "opening dirc " + f);
        // TODO: make TBLoaderCore accept RelativePath here
        TBFileSystem imageSource = DefaultTBFileSystem.open(f);
        Log.d(TAG, "opening " + imageSource);
        String deploymentName = null;
        for (String file : imageSource.list(RelativePath.EMPTY)) {
            deploymentName = file;
            break;
        }

        imageSource = DefaultTBFileSystem.open(new File(f, deploymentName));
        Log.d(TAG, "opening " + imageSource);

        String imageName = TBLoaderCore.getImageFromCommunity(imageSource, community);
        Log.d(TAG, "imageName " + imageName);

        String myDeviceId = PreferenceManager.getDefaultSharedPreferences(mAppContext)
                .getString("pref_tbloader_device_id", null);

        TBFileSystem dropboxFileSystem = new DropboxFileSystem(mAppContext.getDropboxConnecton().getApi(), "/");

        try {
            RelativePath collectedDataFile = RelativePath.parse(
                    TBLoaderConstants.COLLECTED_DATA_DROPBOXDIR_PREFIX + myDeviceId);
            TBLoaderConfig tbLoaderConfig = new TBLoaderConfig.Builder()
                    .withDeviceID(myDeviceId)
                    .withProject(deploymentPackage.getProjectName())
                    .withSrnPrefix("b-")
                    .withDropbox(dropboxFileSystem, collectedDataFile)
                    .withTempFileSystem(DefaultTBFileSystem.open(mAppContext.getExternalCacheDir()))
                    .build();
            TBDeviceInfo deviceInfo = new TBDeviceInfo(talkingBook.getTalkingBookFileSystem(), talkingBook.getDeviceLabel(), myDeviceId);
            TBLoaderCore core = new TBLoaderCore(tbLoaderConfig, deviceInfo);
            DeploymentInfo oldDeploymentInfo = core.loadDeploymentInfoFromDevice();
            String deviceSerialNumber = deviceInfo.getSerialNumber();
            if (deviceSerialNumber.equals(TBLoaderConstants.NEED_SERIAL_NUMBER)) {
                deviceSerialNumber = (tbLoaderConfig.getSrnPrefix() + tbLoaderConfig.getDeviceID()
                        + getNewDeviceSerialNumber()).toUpperCase();
            }

            DeploymentInfo newDeploymentInfo = new DeploymentInfo(deviceSerialNumber, imageName, deploymentName, null,
                    TBLoaderCore.getFirmwareRevisionNumbers(imageSource), community);

            Log.d(TAG, "UPDATE: \n\toldDeploymentInfo: " + oldDeploymentInfo.toString() + "\n\tnewDeploymentInfo: " + newDeploymentInfo.toString());
            core.update(oldDeploymentInfo, newDeploymentInfo, getCurrentLocation(), false, false, imageSource, progressListener);
        } catch (IOException e) {
            Log.d("tbloader", "Unable to read TBInfo", e);
        }
    }

    private String getNewDeviceSerialNumber() {
        final String prefName = "device_serial_number_counter";
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mAppContext);
        int intSrn = sharedPreferences.getInt(prefName, 0);

        final SharedPreferences.Editor sharedPreferencesEditor = PreferenceManager.getDefaultSharedPreferences(mAppContext).edit();
        sharedPreferencesEditor.putInt(prefName, intSrn + 1);

        return String.format("%04x", intSrn);
    }

    private String getCurrentLocation() {
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(Criteria.NO_REQUIREMENT);
        final String provider = mLocationManager.getBestProvider(criteria, true);

        String locationString = "UNKNOWN";
        if (isLocationEnabled(mLocationManager) && (ActivityCompat.checkSelfPermission(mAppContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(mAppContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            Location location = mLocationManager.getLastKnownLocation(provider);
            if (location != null) {
                locationString = "\"(" + location.getLatitude() + ", " + location.getLongitude() + ")\"";
            }
        }

        return locationString;
    }

    private static boolean isLocationEnabled(LocationManager locationManager) {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }
}
