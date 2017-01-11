package org.literacybridge.androidtbloader.checkin;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import org.literacybridge.androidtbloader.TBLoaderAppContext;
import org.literacybridge.core.fs.OperationLog;

/**
 * Created by bill on 1/3/17.
 */
@SuppressWarnings("MissingPermission")
public class LocationProvider {
    private static final String TAG = LocationProvider.class.getSimpleName();

    interface LocationCallback {
        void onNewLocationAvailable(Location location, String provider, long nanos);
    }

    static void getLocation(final MyLocationListener locationListener) {
        // Uncomment these lines to get location updates.
//        int LOCATION_REFRESH_TIME = 1000; // ms
//        int LOCATION_REFRESH_DISTANCE = 5; // m
//        LocationManager mLocationManager = (LocationManager) (getActivity().getSystemService(
//                LOCATION_SERVICE));
        //noinspection MissingPermission
//        mLocationManager.requestLocationUpdates(GPS_PROVIDER, LOCATION_REFRESH_TIME,
//                LOCATION_REFRESH_DISTANCE, mLocationListener);

        final OperationLog.Operation op = OperationLog.startOperation("getlocation");
        requestSingleUpdate(TBLoaderAppContext.getInstance(),
                new LocationCallback() {
                    @Override
                    public void onNewLocationAvailable(Location location,
                            String provider,
                            long nanos) {
                        op.put("provider", provider);
                        op.put("latitude", location.getLatitude());
                        op.put("longitude", location.getLongitude());
                        op.end();
                        locationListener.onLocationChanged(location, provider, nanos);
                    }
                });
    }

    // calls back to calling thread, note this is for low granularity: if you want higher precision, swap the
    // contents of the else and if. Also be sure to check gps permission/settings are allowed.
    // call usually takes <10ms
    static void requestSingleUpdate(final Context context,
            final LocationCallback callback) {
        final LocationManager locationManager = (LocationManager) context.getSystemService(
                Context.LOCATION_SERVICE);
        boolean preferNetwork = PreferenceManager.getDefaultSharedPreferences(TBLoaderAppContext.getInstance()).getBoolean("pref_prefer_network_location", false);
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        final long startTime = System.nanoTime();
        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                callback.onNewLocationAvailable(location,
                        LocationManager.GPS_PROVIDER,
                        System.nanoTime() - startTime);
            }
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) { }
            @Override
            public void onProviderEnabled(String provider) { }
            @Override
            public void onProviderDisabled(String provider) { }
        };

        if (isGPSEnabled && !(isNetworkEnabled && preferNetwork)) {
            Log.d(TAG, "Attempting to get location from GPS");
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            locationManager.requestSingleUpdate(criteria, listener, null);
        } else if (isNetworkEnabled) {
            Log.d(TAG, "Attempting to get location from network");
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_COARSE);
            locationManager.requestSingleUpdate(criteria, listener, null);
        }
    }

    interface MyLocationListener extends LocationListener {
        void onLocationChanged(Location location, String provider, long nanos);
    }
}
