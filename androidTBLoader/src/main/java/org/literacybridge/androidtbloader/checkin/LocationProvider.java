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
import org.literacybridge.androidtbloader.community.CommunityInfo;
import org.literacybridge.core.fs.OperationLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bill on 1/3/17.
 */
@SuppressWarnings("MissingPermission")
public class LocationProvider {
    private static final String TAG = LocationProvider.class.getSimpleName();
    private static final float UNKNOWN_DISTANCE = (float) 1e12; // a bit past the orbit of Jupiter, in meteres.
    private static Map<CommunityInfo, Float> sCachedDistances = new HashMap<>();
    private static Location sLatestLocation = null;

    /**
     * Returns the most recently acquired GPS location.
     * @return
     */
    public static Location getLatestLocation() {
        return sLatestLocation;
    }

    /**
     * Given a community, how far away is it from the current location?
     * @param community The community.
     * @return The distance, in meters.
     */
    public static float distanceTo(CommunityInfo community) {
        if (sCachedDistances.containsKey(community)) {
            return sCachedDistances.get(community);
        }
        float distance = UNKNOWN_DISTANCE;
        if (community.getLocation() != null && LocationProvider.getLatestLocation() != null) {
            distance = LocationProvider.getLatestLocation().distanceTo(community.getLocation());
        }
        sCachedDistances.put(community, distance);
        return distance;
    }

    /**
     * Given a community, get the distance in a nice readable format.
     */
    public static String getDistanceString(CommunityInfo community) {
        float distance = distanceTo(community);
        if (distance == UNKNOWN_DISTANCE) {
            return "--";
        }
        if (distance < 1000) {
            return String.format("%.0f m", distance);
        }
        distance = distance / 1000.0f; // convert to km
        if (distance < 100) {
            return String.format("%.1f km", distance);
        }
        return String.format("%.0f km", distance);
    }

    private static void gotNewLocation(Location newLocation) {
        float delta = 0;
        // If the location changed, invalidate any distances < 10x the change, on the premise
        // that longer distances didn't change much, as a percentage.
        if (sLatestLocation != null) {
            delta = sLatestLocation.distanceTo(newLocation);
            delta *= 10.0f;
            List<CommunityInfo> toRemove = new ArrayList<>();
            for (Map.Entry<CommunityInfo, Float> entry : sCachedDistances.entrySet()) {
                if (entry.getValue() < delta || entry.getValue() == UNKNOWN_DISTANCE) {
                    toRemove.add(entry.getKey());
                }
            }
            for (CommunityInfo entry : toRemove) {
                sCachedDistances.remove(entry);
            }
        }
        sLatestLocation = newLocation;
    }


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
                gotNewLocation(location);
                callback.onNewLocationAvailable(location,
                        location.getProvider(),
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
