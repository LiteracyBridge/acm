package org.literacybridge.androidtbloader.checkin;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import org.literacybridge.androidtbloader.TBLoaderAppContext;

/**
 * Created by bill on 1/3/17.
 */
@SuppressWarnings("MissingPermission")
public class LocationProvider {

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

        requestSingleUpdate(TBLoaderAppContext.getInstance(),
                new LocationCallback() {
                    @Override
                    public void onNewLocationAvailable(Location location,
                            String provider,
                            long nanos) {
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
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (isGPSEnabled) {
            final long startTime = System.nanoTime();
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            locationManager.requestSingleUpdate(criteria, new LocationListener() {
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
            }, null);
        } else {
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if (isNetworkEnabled) {
                final long startTime = System.nanoTime();
                Criteria criteria = new Criteria();
                criteria.setAccuracy(Criteria.ACCURACY_COARSE);
                locationManager.requestSingleUpdate(criteria, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        callback.onNewLocationAvailable(location,
                                LocationManager.NETWORK_PROVIDER,
                                System.nanoTime() - startTime);
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) { }

                    @Override
                    public void onProviderEnabled(String provider) { }

                    @Override
                    public void onProviderDisabled(String provider) { }
                }, null);
            }
        }
    }

    interface MyLocationListener extends LocationListener {
        void onLocationChanged(Location location, String provider, long nanos);
    }
}
