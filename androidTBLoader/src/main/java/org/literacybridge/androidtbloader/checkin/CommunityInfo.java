package org.literacybridge.androidtbloader.checkin;

import android.location.Location;

/**
 * Information about a community in a project.
 */

public class CommunityInfo {
    public String name;
    public String projectName;
    public Location location;

    public CommunityInfo(String name, String projectName, Location location) {
        this.name = name;
        this.projectName = projectName;
        this.location = location;
    }

    public CommunityInfo(String name, String projectName, double latitude, double longitude) {
        Location l = new Location("cached");
        l.setLatitude(latitude);
        l.setLongitude(longitude);
        this.name = name;
        this.projectName = projectName;
        this.location = l;
    }
}
