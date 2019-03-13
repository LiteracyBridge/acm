package org.literacybridge.acm.tbbuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class BuildSpec {
    String deploymentName;
    List<ContentPackage> contentPackages = new ArrayList<>();

    public BuildSpec(String deploymentName, ContentPackage... contentPackages) {
        this.deploymentName = deploymentName;
        this.contentPackages.addAll(Arrays.asList(contentPackages));
    }

    public void addPackage(ContentPackage contentPackage) {
        this.contentPackages.add(contentPackage);
    }

    public class PackageInfo {
        String name;
        String languagecode;
        boolean hasUserfeedback;
        boolean hasIntro;
        boolean hasInstructions;
        List<String> groups = new ArrayList<>();

        public PackageInfo(String name, String languagecode, String... groups) {
            this.name = name;
            this.languagecode = languagecode;
            this.groups.addAll(Arrays.asList(groups));
        }
    }

    // Package is a keyword. And it would not be possible to tell from context, right?
    public class ContentPackage {
        PackageInfo packageInfo;
        List<Playlist> playlists = new ArrayList<>();

        public ContentPackage(PackageInfo packageInfo, Playlist... playlists) {
            this.packageInfo = packageInfo;
            this.playlists.addAll(Arrays.asList(playlists));
        }

        public void addPlaylist(Playlist playlist) {
            this.playlists.add(playlist);
        }
    }

    public class Playlist {
        String name;
        File shortPrompt;
        File longPrompt;
        List<String> contentIds = new ArrayList<>();

        public Playlist(String name, File shortPrompt, File longPrompt, Collection<String> contentIds) {
            this.name = name;
            this.shortPrompt = shortPrompt;
            this.longPrompt = longPrompt;
            this.contentIds.addAll(contentIds);
        }

        public void addContent(String contentId) {
            this.contentIds.add(contentId);
        }
    }
}
