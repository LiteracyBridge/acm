//package org.literacybridge.androidtbloader;
//
//import android.Manifest;
//import android.app.Activity;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.content.IntentFilter;
//import android.content.pm.PackageManager;
//import android.hardware.usb.UsbConstants;
//import android.hardware.usb.UsbDevice;
//import android.hardware.usb.UsbDeviceConnection;
//import android.hardware.usb.UsbEndpoint;
//import android.hardware.usb.UsbInterface;
//import android.hardware.usb.UsbManager;
//import android.location.Criteria;
//import android.location.Location;
//import android.location.LocationManager;
//import android.net.Uri;
//import android.os.storage.StorageManager;
//import android.support.annotation.Nullable;
//import android.support.v4.app.ActivityCompat;
//import android.support.v4.app.Fragment;
//import android.os.Bundle;
//import android.support.v4.provider.DocumentFile;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.Button;
//import android.widget.TextView;
//
//import org.literacybridge.androidtbloader.dropbox.ACMDatabaseInfo;
//import org.literacybridge.androidtbloader.dropbox.DropboxAccountPickerActivity;
//import org.literacybridge.androidtbloader.dropbox.DropboxFileSystem;
//import org.literacybridge.androidtbloader.dropbox.IOHandler;
//import org.literacybridge.androidtbloader.talkingbook.AndroidTBFileSystem;
//import org.literacybridge.core.fs.DefaultTBFileSystem;
//import org.literacybridge.core.fs.RelativePath;
//import org.literacybridge.core.fs.TBFileSystem;
//import org.literacybridge.core.tbloader.DeploymentInfo;
//import org.literacybridge.core.tbloader.TBDeviceInfo;
//import org.literacybridge.core.tbloader.TBLoaderConfig;
//import org.literacybridge.core.tbloader.TBLoaderConstants;
//import org.literacybridge.core.tbloader.TBLoaderCore;
//
//import java.io.IOException;
//import java.lang.reflect.InvocationTargetException;
//import java.lang.reflect.Method;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.Executors;
//
///**
// * A placeholder fragment containing a simple view.
// */
//public class MainActivityFragment extends Fragment {
//    private static final int READ_REQUEST_CODE = 42;
//    private static final int DROPBOX_CODE = 43;
//    private TextView textView;
//    private Button openFileButton;
//    private Button getPermissionButton;
//    private Button dropboxButton;
//    private Button readStatsButton;
//    private Button unmountButton;
//    private LocationManager mLocationManager;
//    private Uri baseURI = Uri.parse("content://com.android.externalstorage.documents/tree/9285-F050%3A");
//
//    public MainActivityFragment() {
//    }
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        return inflater.inflate(R.layout.fragment_main, container, false);
//    }
//
//    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
//        textView = (TextView) view.findViewById(R.id.mainText);
//        openFileButton = (Button) view.findViewById(R.id.openFileButton);
//        getPermissionButton = (Button) view.findViewById(R.id.getPermissionButton);
//        dropboxButton = (Button) view.findViewById(R.id.dropboxButton);
//        readStatsButton = (Button) view.findViewById(R.id.readStatsButton);
//        unmountButton = (Button) view.findViewById(R.id.unmountButton);
//
//        mLocationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
//        Criteria criteria = new Criteria();
//        criteria.setAccuracy(Criteria.ACCURACY_FINE);
//        criteria.setAltitudeRequired(false);
//        criteria.setBearingRequired(false);
//        criteria.setCostAllowed(true);
//        criteria.setPowerRequirement(Criteria.NO_REQUIREMENT);
//        final String provider = mLocationManager.getBestProvider(criteria, true);
//
//
//        String locationString = "UNKNOWN";
//        if (isLocationEnabled() && (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
//                || ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
//            Location location = mLocationManager.getLastKnownLocation(provider);
//            if (location != null) {
//                locationString = "(" + location.getLatitude() + ", " + location.getLongitude() + ")";
//            }
//        }
//
//
//        getPermissionButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
////                // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
////                // browser.
////                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
////
////                startActivityForResult(intent, READ_REQUEST_CODE);
//
//                Intent intent = new Intent(getActivity(), SettingsActivity.class);
//                startActivity(intent);
//            }
//        });
//
//        final String loc = locationString;
//        openFileButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                textView.setText(loc);
//            }
//        });
//
//        dropboxButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(getActivity(), DropboxAccountPickerActivity.class);
//                startActivityForResult(intent, DROPBOX_CODE);
//            }
//        });
//
//        readStatsButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Executors.newSingleThreadExecutor().submit(new Runnable() {
//                    @Override
//                    public void run() {
//                        DocumentFile root = DocumentFile.fromTreeUri(getContext(), baseURI);
//                        try {
//                            RelativePath collectedDataFile = RelativePath.parse(
//                                    TBLoaderConstants.COLLECTED_DATA_DROPBOXDIR_PREFIX + "000C");
//                            TBFileSystem fs = new AndroidTBFileSystem(
//                                    getContext().getContentResolver(), root);
//                            TBLoaderConfig tbLoaderConfig = new TBLoaderConfig.Builder()
//                                    .withDeviceID("000C")
//                                    .withProject("ACM-UWR")
//                                    .withSrnPrefix("b-")
//                                    .withHomePath(getContext().getApplicationInfo().dataDir)
//                                    .withDropbox(new DropboxFileSystem(IOHandler.getInstance().getAPI(), ""), collectedDataFile)
//                                    .withTempFileSystem(DefaultTBFileSystem.open(getContext().getExternalCacheDir()))
//                                    .build();
//                            TBDeviceInfo deviceInfo = new TBDeviceInfo(fs, "B-000C02CA", "000C");
//                            TBLoaderCore core = new TBLoaderCore(tbLoaderConfig, deviceInfo);
//                            DeploymentInfo oldDeploymentInfo = core.loadDeploymentInfoFromDevice();
//                            DeploymentInfo newDeploymentInfo = new DeploymentInfo("B-000C02CA", "", "org", null, "", "Non-specific");
//                            core.update(oldDeploymentInfo, newDeploymentInfo, loc, true, false, new TBLoaderCore.ProgressListener() {
//                                @Override
//                                public void updateProgress(final String progressUpdate) {
//                                    getActivity().runOnUiThread(new Runnable() {
//                                        public void run() {
//                                            textView.setText(textView.getText() + progressUpdate);
//                                        }
//                                    });
//                                }
//                            });
//                        } catch (IOException e) {
//                            Log.d("tbloader", "Unable to read TBInfo", e);
//                        }
//                    }
//                });
//            }
//        });
//
//        unmountButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Executors.newSingleThreadExecutor().submit(new Runnable() {
//                    @Override
//                    public void run() {
//
//                        UsbManager manager = (UsbManager) getContext().getSystemService(Context.USB_SERVICE);
//                        Map<String, UsbDevice> map = manager.getDeviceList();
//                        for (Map.Entry<String, UsbDevice> entry : map.entrySet()) {
//                            final UsbDevice device = entry.getValue();
//
//                            getActivity().runOnUiThread(new Runnable() {
//                                public void run() {
//                                    textView.setText(textView.getText() + "device: " + device + "\n");
//                                }
//                            });
//
//                            final UsbInterface usbInt = findUsableInterface(device);
//
//                            getActivity().runOnUiThread(new Runnable() {
//                                public void run() {
//                                    textView.setText(textView.getText() + "usbInt: " + usbInt + "\n");
//                                }
//                            });
//
//                            final UsbDeviceConnection connection = manager.openDevice(entry.getValue());
//
//                            getActivity().runOnUiThread(new Runnable() {
//                                public void run() {
//                                    textView.setText(textView.getText() + "connection: " + connection + "\n");
//                                }
//                            });
//
//                            final UsbEndpoint out = findUsableEndpoints(usbInt, UsbConstants.USB_DIR_OUT);
//
//                            getActivity().runOnUiThread(new Runnable() {
//                                public void run() {
//                                    textView.setText(textView.getText() + "endpoint: " + out + "\n");
//                                }
//                            });
//
//                            final boolean claimed = connection.claimInterface(usbInt, true);
//
//                            getActivity().runOnUiThread(new Runnable() {
//                                public void run() {
//                                    textView.setText(textView.getText() + "claimed: " + claimed + "\n");
//                                }
//                            });
//
//
//                            final int TIMEOUT = 20000;
//                            byte[] buffer = new byte[6];
//                            buffer[0] = (byte) 0x1b;
//                            buffer[4] = (byte) 0xa2;
//                            connection.bulkTransfer(out, buffer, buffer.length, TIMEOUT);
//
//
//                            connection.releaseInterface(usbInt);
//
//                        }
//                    }
//                });
//            }
//        });
//
////        File[] files = getContext().getExternalFilesDirs(null);
////        if (files != null) {
////            for (File f : files) {
////                textView.setText(textView.getText() + f.getAbsolutePath() + "\n");
////            }
////        }
//
//        IntentFilter filter = new IntentFilter();
//        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
//        filter.addDataScheme("file");
//        filter.setPriority(999);
//
//        getContext().registerReceiver(new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                checkPermissions();
//            }
//        }, filter);
//        checkPermissions();
//    }
//
//    private boolean isLocationEnabled() {
//        return mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
//                mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
//    }
//
//    private static UsbInterface findUsableInterface(UsbDevice device) {
//        for (int i = 0; i < device.getInterfaceCount(); i++) {
//            UsbInterface interfaceProbe = device.getInterface(i);
//            if (interfaceProbe.getInterfaceClass() == UsbConstants.USB_CLASS_MASS_STORAGE) {
//                if (interfaceProbe.getInterfaceSubclass() == 0x6) {
//                    if (interfaceProbe.getInterfaceProtocol() == 0x50) {
//                        return device.getInterface(i);
//                    }
//                }
//            }
//        }
//
//        return null;
//    }
//
//    private static UsbEndpoint findUsableEndpoints(UsbInterface usbInterface, int direction) {
//        for (int i = 0; i < usbInterface.getEndpointCount(); i++) {
//            UsbEndpoint endpointProbe = usbInterface.getEndpoint(i);
//            if (endpointProbe.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
//                if (endpointProbe.getDirection() == direction) {
//                    return endpointProbe;
//                }
//            }
//        }
//
//        return null;
//    }
//
//    public void checkPermissions() {
//        try {
//            for (Map.Entry<String, String> e : getSecondaryMountedVolumesMap(this.getContext()).entrySet()) {
//                textView.setText(textView.getText() + e.getKey() + ", " + e.getValue() + "\n");
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    public static Map<String, String> getSecondaryMountedVolumesMap(Context context) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
//        Object[] volumes;
//
//        StorageManager sm = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
//
//
//        Method getVolumeListMethod = sm.getClass().getMethod("getVolumeList");
//        for (Method m : sm.getClass().getMethods()) {
//            Log.d("method", m.toString());
//        }
//
//        volumes = (Object[])getVolumeListMethod.invoke(sm);
//
//        Map<String, String> volumesMap = new HashMap<>();
//        for (Object volume : volumes) {
//            Log.d("volumes", "Found: " + volume);
//            Method getStateMethod = volume.getClass().getMethod("getState");
//            String mState = (String) getStateMethod.invoke(volume);
//
//            Method isPrimaryMethod = volume.getClass().getMethod("isPrimary");
//            boolean mPrimary = (Boolean) isPrimaryMethod.invoke(volume);
//
//            if (!mPrimary && mState.equals("mounted")) {
//                Method getPathMethod = volume.getClass().getMethod("getPath");
//                String mPath = (String) getPathMethod.invoke(volume);
//                Log.d("umount", ", path: " + mPath);
//                Method disableUsbMassStorageMethod = sm.getClass().getMethod("disableUsbMassStorage");
//                //disableUsbMassStorageMethod.invoke(sm);
//
//          //      unmountMethod.invoke(volId);
//                Log.d("umount", "done");
//
//                Method getUuidMethod = volume.getClass().getMethod("getUuid");
//                String mUuid = (String) getUuidMethod.invoke(volume);
//
//                if (mUuid != null && mPath != null)
//                    volumesMap.put(mUuid, mPath);
//            }
//        }
//        return volumesMap;
//    }
//
//    @Override
//    public void onActivityResult(int requestCode, int resultCode,
//                                 Intent resultData) {
//        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
//            baseURI = resultData.getData();
//            int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
//            getContext().getContentResolver().takePersistableUriPermission(baseURI, flags);
//            textView.setText("Got permission for URI: " + baseURI.toString());
//        }
//
//        if (requestCode == DROPBOX_CODE && resultCode == Activity.RESULT_OK) {
//            textView.setText(textView.getText() + "\n\nDropbox:\n");
//            Executors.newSingleThreadExecutor().submit(new Runnable() {
//                @Override
//                public void run() {
//                    IOHandler.getInstance().refresh();
//                    List<ACMDatabaseInfo> dbs = IOHandler.getInstance().getDatabaseInfos();
//                    Log.d("Dropbox", "Size: " + dbs.size());
//                    for (final ACMDatabaseInfo info : dbs) {
//                        getActivity().runOnUiThread(new Runnable() {
//                            public void run() {
//                                textView.setText(textView.getText() + info.getName() + "\n");
//                                Log.d("Dropbox", info.getTBLoadersDropBoxPath());
//                            }
//                        });
//                    }
//                }
//            });
//
//        }
//    }
//}
