package org.literacybridge.androidtbloader.checkin;

import android.location.Location;

import org.literacybridge.androidtbloader.community.CommunityInfo;
import org.literacybridge.androidtbloader.util.PathsProvider;
import org.literacybridge.core.fs.OperationLog;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.literacybridge.androidtbloader.util.Constants.LOCATION_FILE_EXTENSION;

/**
 * Manages the community locations that we know.
 */

public class KnownLocations {
    //private static final String TAG = KnownLocations.class.getSimpleName();

    private static Map<String, Map<String, CommunityInfo>> allProjects = new HashMap<>();

    /**
     * Tries to find the project / community combination in all projects. Returns null if not found.
     * @param community Community of interest.
     * @param project Project of interest.
     * @return The CommunityInfo, if found, otherwise null.
     */
    public static CommunityInfo findCommunity(String community, String project) {
        Map<String, CommunityInfo> communities = allProjects.get(project);
        if (communities != null) {
            return communities.get(community);
        }
        return null;
    }

    private List<String> projects = new ArrayList<>();
    private Map<String, Map<String, CommunityInfo>> communities = new HashMap<>();
    KnownLocations(List<String> projects) {
        for (String p : projects) {
            this.projects.add(p.toUpperCase());
        }
        loadLocationsForProjects(this.projects);
        for (String p : this.projects) {
            this.communities.put(p, allProjects.get(p));
        }
    }

    /**
     * Finds the communities "near" a location, where near currently means 1/2 km.
     * @param location The location.
     * @return List of nearby communities, sorted by ascending distance.
     */
    List<CommunityInfo> findCommunitiesNear(Location location) {
        List<SR> near = findByDistance(location, 0, 500);
        Collections.sort(near, new Comparator<SR>() {
            @Override
            public int compare(SR lhs, SR rhs) {
                if (lhs.distance < rhs.distance) return -1;
                if (lhs.distance > rhs.distance) return 1;
                return lhs.community.getName().compareToIgnoreCase(rhs.community.getName());
            }
        });
        List<CommunityInfo> result = new ArrayList<>();
        for (SR sr : near) {
            result.add(sr.community);
        }
        return result;
    }

    /**
     * Helper to find communities with known locations within a given distance.
     * @param target The location for which the list of nearby communities is desired.
     * @param minDist The closest desired distance.
     * @param maxDist The farthest desired distance.
     * @return A list of objects with communities within the desired range.
     */
    private List<SR> findByDistance(Location target, float minDist, float maxDist) {
        List<SR> result = new ArrayList<>();
        for (String project : projects) {
            for (CommunityInfo community : communities.get(project).values()) {
                float distance = community.getLocation().distanceTo(target);
                if (distance >= minDist && distance < maxDist) {
                    result.add(new SR(distance, community));
                }
            }
        }
        return result;
    }

    /**
     * Given a gps location and a community, mark the community as at that location.
     * @param mGpsLocation The GPS coordinates.
     * @param community A community at those coordinates.
     */
    static void setLocationInfoFor(Location mGpsLocation, CommunityInfo community) {
        // We can use this location to improve our locations. Send to the server; let them handle it.
        Map<String,String> logInfo = new HashMap<>();
        logInfo.put("community", community.getName());
        logInfo.put("project", community.getProject());
        logInfo.put("longitude", Double.toString(mGpsLocation.getLongitude()));
        logInfo.put("latitude", Double.toString(mGpsLocation.getLatitude()));
        OperationLog.logEvent("setlocation", logInfo);

        // Also record it locally.
        // Find the collection of communities associated with the given community's project.
        Map<String, CommunityInfo> projCommunities = allProjects.get(community.getProject());
        // If there is no such collection, create a new empty one.
        if (projCommunities == null) {
            projCommunities = new HashMap<>();
            allProjects.put(community.getProject(), projCommunities);
        }
        // Find the entry for this community in the collection of communities.
        CommunityInfo info = projCommunities.get(community.getName());
        // If there is no such entity, create a new one.
        if (info == null) {
            info = new CommunityInfo(community.getName(), community.getProject());
            projCommunities.put(community.getName(), info);
        }
        info.setLocation(mGpsLocation);
    }

    /**
     * A helper object to hold the distance from the "current" location to a given project.
     */
    private static class SR {
        double distance;
        CommunityInfo community;

        SR(double distance, CommunityInfo community) {
            this.distance = distance;
            this.community = community;
        }
    }

    /**
     * Given a list of project names, load the locations files for those projects. Cache
     * the location files, so subsequent calls don't need to read a file.
     * @param projects A list of projects of interest.
     */
    public static void loadLocationsForProjects(List<String> projects) {
        for (String proj : projects) {
            String project = proj.toUpperCase();
            if (!allProjects.containsKey(project)) {
                File locFile = new File(PathsProvider.getLocationsCacheDirectory(),
                        project + LOCATION_FILE_EXTENSION);
                if (locFile.exists()) {
                    LocationsCsvFile csv = new LocationsCsvFile(locFile);
                    allProjects.put(project, csv.read());
                }
            }
        }
    }


}
