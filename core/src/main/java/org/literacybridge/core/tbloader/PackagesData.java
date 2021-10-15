package org.literacybridge.core.tbloader;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This describes one or more packages for a TBv2 device.
 */
@SuppressWarnings("UnusedReturnValue")
public class PackagesData implements Serializable {
    private static final int PACKAGES_DATA_VERSION = 1;
    private static final int PACKAGES_DATA_MAX_LINE = 200;
    public static final String PACKAGES_DATA_TXT = "packages_data.txt";

    private final String deploymentName;
    private final List<PackageData> packages = new ArrayList<>();

    public PackagesData(String deploymentName) {
        this.deploymentName = deploymentName;
    }

    public PackageData addPackage(String packageName) {
        PackageData newPackage = new PackageData(packageName);
        packages.add(newPackage);
        return newPackage;
    }

    /**
     * Export the package_data.txt file to the given file.
     * @param packageDataStream to which the package_data.txt should be written.
     * @param packageNames to be included in the exported data. Generally one
     */
    public void exportPackageDataFile(OutputStream packageDataStream, String... packageNames) {
        if (packageNames == null || packageNames.length == 0) {
            packageNames = getPackages().stream()
                .map(p -> p.name)
                .toArray(String[]::new);
        }
        new PackagesDataExporter(deploymentName, packageDataStream, packageNames).export();
    }

    /**
     * Adds the packages from the other packagesData to this one.
     * @param other to be added/merged into this one.
     */
    public void addPackagesData(PackagesData other) {
        packages.addAll(other.packages);
    }

    /**
     * Returns the list of packages in this PackagesData.
     * @return the list.
     */
    public List<PackageData> getPackages() {
        return packages;
    }

    public static class PackageData {
        public final String name;
        private final List<Path> paths = new ArrayList<>();
        private final List<Path> promptsPath = new ArrayList<>();
        private final List<PlaylistData> playlists = new ArrayList<>();
        private Path announcement = null;

        public PackageData(String name) {
            this.name = name;
        }

        public Collection<Path> getAllPaths() {
            Set<Path> result = new HashSet<>(paths);
            for (PlaylistData pld : playlists) {
                result.addAll(pld.getAllPaths());
            }
            return result;
        }

        public PackageData withPromptPath(Path path) {
            if (!paths.contains(path)) {
                paths.add(path);
            }
            if (!promptsPath.contains(path)) {
                promptsPath.add(path);
            }
            return this;
        }

        public PackageData withAnnouncement(Path path) {
            this.announcement = path;
            if (path != null && !paths.contains(path.getParent())) {
                paths.add(path.getParent());
            }
            return this;
        }

        public PlaylistData addPlaylist(String playlistName) {
            PlaylistData newPlaylist = new PlaylistData(playlistName);
            playlists.add(newPlaylist);
            return newPlaylist;
        }

        public static class PlaylistData {
            public final String name;
            private AudioData shortPrompt;
            private AudioData longPrompt;
            private final List<AudioData> messages = new ArrayList<>();

            public PlaylistData(String name) {
                this.name = name;
            }

            public Collection<Path> getAllPaths() {
                Set<Path> result = new HashSet<>();
                result.add(shortPrompt.path.getParent());
                result.add(longPrompt.path.getParent());
                for (AudioData ad : messages) {
                    result.add(ad.path.getParent());
                }
                return result;
            }

            public PlaylistData withShortPrompt(String title, Path path) {
                this.shortPrompt = new AudioData(title, path);
                return this;
            }

            public PlaylistData withLongPrompt(String title, Path path) {
                this.longPrompt = new AudioData(title, path);
                return this;
            }

            public void addMessage(String title, Path path) {
                messages.add(new AudioData(title, path));
            }
        }
    }

    /**
     * An audio item, with the name/title and path to the audio file.
     */
    public static class AudioData {
        public final String name;
        public final Path path;

        public AudioData(String name, Path path) {
            this.name = name;
            this.path = path;
        }
    }

    /**
     * Helper class to export package_data.txt.
     */
    private class PackagesDataExporter {
        private final static int MIN_COMMENT_START = 20;

        private final String deploymentName;
        private final OutputStream packageDataStream;
        private final Set<String> packageNames;
        private final List<PackageData> packagesToExport;

        private PrintStream ps;
        private List<Path> paths;

        public PackagesDataExporter(String deploymentName, OutputStream packageDataStream, String[] packageNames) {
            this.deploymentName = deploymentName;
            this.packageDataStream = packageDataStream;
            this.packageNames = Arrays.stream(packageNames).collect(Collectors.toSet());
            // Gets the packages whose names were provided.
            this.packagesToExport = PackagesData.this.packages.stream()
                .filter(p -> this.packageNames.contains(p.name))
                .collect(Collectors.toList());
        }

        /**
         * Exports the package data to the given file.
         */
        private void export() {
            try (PrintStream ps = new PrintStream(packageDataStream)) {
                this.ps = ps;
                // Header
                DateFormat ISO8601dateTime = new SimpleDateFormat("yyyy/MM/dd @ HH:mm:ss z", Locale.US); // Quoted "Z" to indicate UTC, no timezone offset
//                ISO8601dateTime.setTimeZone(TBLoaderConstants.UTC);
                heading(0, "Created on "+ISO8601dateTime.format(new Date()));

                print(PACKAGES_DATA_VERSION, "format version");
                print(deploymentName, "deployment name");

                // Audio paths
                getExportedPaths();
                heading(0, "paths");
                print(paths.size(), "number of paths");
                paths.forEach(p -> {
                    String path = p.toString();
                    // The TB wants proper path separators, so undo any windows damage.
                    path = path.replace("\\", "/");
                    if (!path.startsWith("/")) path = "/" + path;
                    if (!path.endsWith("/")) path = path + "/";
                    print(path, "");
                });

                // Packages
                print(packagesToExport.size(), "number of packages");
                for (PackageData pkg : this.packagesToExport) {
                    exportPackage(pkg);
                }
            } finally {
                this.ps = null;
                this.paths = null;
            }
        }

        /**
         * Creates the package_data for a single package.
         * @param pkg The package to be exported.
         */
        private void exportPackage(PackageData pkg) {
            heading(0, "package: "+pkg.name);
            print(pkg.name, "");//"package name");
            exportAudio(1, pkg.announcement, "package announcement");
            String promptsPaths = pkg.promptsPath.stream()
                .map(p -> Integer.toString(paths.indexOf(p)))
                .collect(Collectors.joining(";"));
//            heading(1, "paths to prompts");
            print(1, promptsPaths, "path(s) to prompts");

            // Playlists
            print(1, pkg.playlists.size(), "number of playlists");
            for (PackageData.PlaylistData pl : pkg.playlists) {
                heading(2, "playlist: "+pl.name);
                print(2, pl.name, "");//"playlist name");
                exportAudio(2, pl.shortPrompt.path, "playlist announcement");
                exportAudio(2, pl.longPrompt.path, "playlist invitation");

                // Messages
                print(2, pl.messages.size(), "number of messages");
                pl.messages.forEach(msg -> exportAudio(3, msg.path, msg.name));
            }
        }

        /**
         * Writes the line for one audio file.
         * @param level of indentation.
         * @param audio Path to the audio file.
         * @param description Description of the audio file, for a comment in package data.
         */
        private void exportAudio(int level, Path audio, String description) {
            print(level, String.format("%d  %s ", paths.indexOf(audio.getParent()), audio.getFileName()), description);
        }

        private void heading(int level, String data) { print(level, "#------- "+data+" --------", null); }
        private void print(int number, String comment) {
            print(0, number, comment);
        }
        private void print(String data, String comment) {
            print(0, data, comment);
        }

        private void print(int level, int number, String comment) {
            print(level, Integer.toString(number), comment);
        }
        private void print(int level, String data, String comment) {
            String line = StringUtils.repeat("  ", level) + data;
            if (line.length() > PACKAGES_DATA_MAX_LINE) {
                // Drop the indentation and see if that's enough.
                line = data;
                if (line.length() > PACKAGES_DATA_MAX_LINE) {
                    throw new IllegalArgumentException("Line too long: " + line);
                }
            }
            if (StringUtils.isNotBlank(comment)) {
                if (line.length() < MIN_COMMENT_START) {
                    line += StringUtils.repeat(" ", MIN_COMMENT_START - line.length());
                }
                line += " # " + comment;
                if (line.length() > PACKAGES_DATA_MAX_LINE) {
                    // It was OK before the comment was added, so simply truncate the comment.
                    line = line.substring(0, PACKAGES_DATA_MAX_LINE);
                }
            }
            ps.println(line);
        }

        /**
         * Builds the list of exported paths.
         */
        private void getExportedPaths() {
            Set<Path> exportedPaths = new HashSet<>();
            packagesToExport.forEach(p -> exportedPaths.addAll(p.getAllPaths()));
            paths = new ArrayList<>(exportedPaths);
        }

    }

    public static class PackagesDataImporter {
        private final InputStream packageDataStream;
        private BufferedReader bufferedReader;
        private final List<String> paths = new ArrayList<>();
        private PackagesData packagesData;
        private String deploymentName;

        public PackagesDataImporter(InputStream packageDataStream) {
            this.packageDataStream = packageDataStream;
        }

        public PackagesData do_import() throws IOException {
            bufferedReader = new BufferedReader(new InputStreamReader(packageDataStream));
            try {
                parse();
            } finally {
                bufferedReader.close();
            }
            return packagesData;
        }

        public String getDeploymentName() {
            return deploymentName;
        }

        private void parse() throws IOException {
            int version = Integer.parseInt(nextLine());
            deploymentName = nextLine();
            packagesData = new PackagesData(deploymentName);
            int numPaths = Integer.parseInt(nextLine());
            for (int i = 0; i<numPaths; i++) {
                paths.add(nextLine());
            }
            int numPackages = Integer.parseInt(nextLine());
            for (int i = 0; i< numPackages; i++) {
                parsePackage();
            }
        }

        private void parsePackage() throws IOException {
            String packageName = nextLine();
            PackageData packageData = packagesData.addPackage(packageName);
            String[] parts = nextLine().split("\\s+", 2);
            if (parts.length > 1) {
                String path = paths.get(Integer.parseInt(parts[0])) + parts[1];
                packageData.withAnnouncement(new File(path).toPath());
            }
            parts = nextLine().split("[\\s;]*");
            // We only expect and can handle one; the file format allows several
            packageData.withPromptPath(new File(paths.get(Integer.parseInt(parts[0]))).toPath());
            int numPlaylists = Integer.parseInt(nextLine());
            for (int i=0; i<numPlaylists; i++) {
                parsePlaylist(packageData);
            }
        }

        private void parsePlaylist(PackageData packageData) throws IOException {
            String playlistName = nextLine();
            PackageData.PlaylistData playlistData = packageData.addPlaylist(playlistName);
            Path announcement = nextPath();
            Path invitation = nextPath();
            playlistData.withShortPrompt("playlist announcement", announcement);
            playlistData.withLongPrompt("playlist invitation", invitation);
            int numMessages = Integer.parseInt(nextLine());
            for (int i=0; i<numMessages; i++) {
                parseMessage(playlistData);
            }
        }

        private void parseMessage(PackageData.PlaylistData playlistData) throws IOException {
            String line = nextLine(true);
            String[] parts = line.split("\\s+", 3);
            String pathString = paths.get(Integer.parseInt(parts[0])) + parts[1];
            Path path = new File(pathString).toPath();
            playlistData.addMessage(parts[2], path);
        }

        private Path nextPath() throws IOException {
            String[] parts = nextLine().split("\\s+");
            String path = paths.get(Integer.parseInt(parts[0])) + parts[1];
            return new File(path).toPath();
        }

        private String nextLine() throws IOException {
            return nextLine(false);
        }
        private String nextLine(boolean keepComment) throws IOException {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] parts = line.split("#", 2);
                if (parts.length<1) continue;
                line = parts[0];
                if (keepComment) {
                    List<String> commentParts = Arrays.asList(parts).subList(1,parts.length);
                    line += ' ' + String.join("  ", commentParts);
                }
                line = line.trim();
                if (line.length() < 1) continue;
                return line;
            }
            return null;
        }
    }

}
