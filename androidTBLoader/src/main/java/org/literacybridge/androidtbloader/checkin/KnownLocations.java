package org.literacybridge.androidtbloader.checkin;

import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;
import android.util.Log;

import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import org.literacybridge.androidtbloader.TBLoaderAppContext;
import org.literacybridge.androidtbloader.content.ContentInfo;
import org.literacybridge.androidtbloader.content.ContentManager;
import org.literacybridge.androidtbloader.signin.UserHelper;
import org.literacybridge.androidtbloader.util.Config;
import org.literacybridge.androidtbloader.util.Constants;
import org.literacybridge.androidtbloader.util.PathsProvider;
import org.literacybridge.androidtbloader.util.S3Helper;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.Context.MODE_PRIVATE;
import static org.literacybridge.androidtbloader.util.Constants.LOCATION_FILE_EXTENSION;

/**
 * Manages the community locations that we know.
 */

class KnownLocations {
    private static final String TAG = KnownLocations.class.getSimpleName();

    private static Map<String, List<CommunityInfo>> allCommunities = new HashMap<String, List<CommunityInfo>>();

    private List<String> projects = new ArrayList<>();
    private Map<String, List<CommunityInfo>> communities = new HashMap<String, List<CommunityInfo>>();
    public KnownLocations(List<String> projects) {
        for (String p : projects) {
            this.projects.add(p.toUpperCase());
        }
        loadLocationsForProjects(this.projects);
        for (String p : this.projects) {
            this.communities.put(p, allCommunities.get(p));
        }
    }

    /**
     * Finds the communities "near" a location, where near currently means 1/2 km.
     * @param location The location.
     * @return List of nearby communities, sorted by ascending distance.
     */
    public List<CommunityInfo> findCommunitiesNear(Location location) {
        List<SR> near = findByDistance(location, 0, 50);
        Collections.sort(near, new Comparator<SR>() {
            @Override
            public int compare(SR lhs, SR rhs) {
                if (lhs.distance < rhs.distance) return -1;
                if (lhs.distance > rhs.distance) return 1;
                return lhs.community.name.compareToIgnoreCase(rhs.community.name);
            }
        });
        List<CommunityInfo> result = new ArrayList<>();
        for (SR sr : near) {
            result.add(sr.community);
        }
        return result;
    }

    private List<SR> findByDistance(Location target, float minDist, float maxDist) {
        List<SR> result = new ArrayList<>();
        for (String project : projects) {
            for (CommunityInfo community : communities.get(project)) {
                float distance = community.location.distanceTo(target);
                if (distance >= minDist && distance < maxDist) {
                    result.add(new SR(distance, community));
                }
            }
        }
        return result;
    }

    public void setLocationInfoFor(Location mGpsLocation, String community, String project) {
        // LOG, so we capture this
        List<CommunityInfo> projCommunities = allCommunities.get(project);
        if (projCommunities == null) {
            projCommunities = new ArrayList<>();
            allCommunities.put(project, projCommunities);
        }
        CommunityInfo info = null;
        for (CommunityInfo ci : projCommunities) {
            if (ci.name.equals(community)) {
                info = ci;
                break;
            }
        }
        if (info == null) {
            info = new CommunityInfo(community, project, null);
            projCommunities.add(info);
        }
        info.location = mGpsLocation;
    }

    private static class SR {
        double distance;
        CommunityInfo community;

        public SR(double distance, CommunityInfo community) {
            this.distance = distance;
            this.community = community;
        }
    }


    public static void loadLocationsForProjects(List<String> projects) {
        for (String proj : projects) {
            String project = proj.toUpperCase();
            if (!allCommunities.containsKey(project)) {
                File locFile = new File(PathsProvider.getLocationsCacheDirectory(),
                        project + LOCATION_FILE_EXTENSION);
                if (locFile.exists()) {
                    LocationsCsvFile csv = new LocationsCsvFile(locFile);
                    allCommunities.put(project, csv.read());
                }
            }
        }
    }


}
