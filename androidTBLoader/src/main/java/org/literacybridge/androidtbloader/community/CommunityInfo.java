package org.literacybridge.androidtbloader.community;

import android.location.Location;

import org.literacybridge.androidtbloader.checkin.KnownLocations;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Information about a community in a project.
 */

public class CommunityInfo {
    private final static String DELIMITER = "\u205c"; // ⁜, dotted cross


    private final String name;
    private final String project;
    private Location location;

    public String getName() {
        return name;
    }

    public String getProject() {
        return project;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public CommunityInfo(String name, String project) {
        this.name = name;
        this.project = project;
        this.location = null;
    }

    public CommunityInfo(String name, String project, Location location) {
        this.name = name;
        this.project = project;
        this.location = location;
    }

    public CommunityInfo(String name, String project, double latitude, double longitude) {
        Location l = new Location("cached");
        l.setLatitude(latitude);
        l.setLongitude(longitude);
        this.name = name;
        this.project = project;
        this.location = l;
    }

    /**
     * Make a string of project and community names that can be passed through "extra" data.
     * @return The string, suitable for extra data.
     */
    public String makeExtra() {
        return this.project + DELIMITER + this.name;
    }

    /**
     * Given a string from extra data, return a CommunityInfo. If location is known for the
     * community, it will be present in the CommunityInfo.
     * @param delimited The string from "makeExtra"
     * @return The corresponding CommunityInfo.
     */
    public static CommunityInfo parseExtra(String delimited) {
        CommunityInfo result = null;
        String [] parts = delimited.split(DELIMITER);
        if (parts.length == 2) {
            result = KnownLocations.findCommunity(parts[1], parts[0]);
            if (result == null) {
                result = new CommunityInfo(parts[0], parts[1]);
            }
        }
        return result;
    }

    public static ArrayList<String> makeExtra(Collection<CommunityInfo> infos) {
        ArrayList<String> result = new ArrayList<>();
        for (CommunityInfo info : infos) {
            result.add(info.makeExtra());
        }
        return result;
    }
    public static ArrayList<CommunityInfo> parseExtra(List<String> list) {
        ArrayList<CommunityInfo> result = new ArrayList<>();
        for (String extra : list) {
            result.add(parseExtra(extra));
        }
        return result;
    }

   @Override
    public int hashCode() {
        return name.toUpperCase().hashCode() ^ project.toUpperCase().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CommunityInfo)) return false;
        boolean eql = name.equalsIgnoreCase(((CommunityInfo) o).name);
        if (eql) {
            eql = project.equalsIgnoreCase(((CommunityInfo) o).name);
        }
        return eql;
    }

    @Override
    public String toString() {
        return name;
    }
}
