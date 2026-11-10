package org.literacybridge.acm.gui.fx.dashboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One row of the Modification table: either a playlist, or one track inside a playlist.
 * Read by TreeItemPropertyValueFactory, so the getters must stay public.
 */
public class ModificationRow {
    private final String name;
    private final String duration;
    private final List<ModificationRow> trackRows;
    private final boolean track;

    private ModificationRow(String name, String duration, List<ModificationRow> trackRows, boolean track) {
        this.name = name;
        this.duration = duration;
        this.trackRows = trackRows;
        this.track = track;
    }

    public static ModificationRow playlist(String name, String duration, List<ModificationRow> trackRows) {
        return new ModificationRow(name, duration, new ArrayList<>(trackRows), false);
    }

    public static ModificationRow track(String name, String duration) {
        return new ModificationRow(name, duration, Collections.emptyList(), true);
    }

    public String getName() {
        return name;
    }

    public String getDuration() {
        return duration;
    }

    // Track rows show "--" in the Tracks column, as in the design.
    public String getTracks() {
        return track ? "--" : Integer.toString(trackRows.size());
    }

    public List<ModificationRow> getTrackRows() {
        return Collections.unmodifiableList(trackRows);
    }

    public boolean isTrack() {
        return track;
    }
}
