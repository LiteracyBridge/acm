package org.literacybridge.acm.tbloader;

import org.literacybridge.acm.gui.util.UIUtils;
import org.literacybridge.acm.utils.OsUtils;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Monitors the file system roots for something that seems to be a Talking Book.
 */
class FsRootMonitor extends Thread {
    private static final Logger LOG = Logger.getLogger(FsRootMonitor.class.getName());
    public static final int MILLIS_BETWEEN_SCANS = 500;

    private final FileSystemView fsView = FileSystemView.getFileSystemView();
    private boolean enabled = true;
    Set<String> oldList = new HashSet<>();

    private final Predicate<File> filter;
    private final Consumer<List<File>> rootsHandler;

    AtomicBoolean refreshRequested = new AtomicBoolean(true);

    public FsRootMonitor(Consumer<List<File>> rootsHandler) {
        this.filterParams = new FilterParams().minimum(1).maximum(16).forbidding("Network Drive");
        if (OsUtils.WINDOWS) {
            filterParams.allowing("USB Drive", "Lecteur USB");
        }
        this.filter = this::rootsFilter;
        this.rootsHandler = rootsHandler;
        this.setDaemon(true);
    }

    public FilterParams getFilterParams() {
        return new FilterParams(filterParams);
    }

    public void setFilterParams(FilterParams filterParams) {
        this.filterParams = filterParams;
        refresh();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void refresh() {
        refreshRequested.set(true);
    }

    /**
     * Gets the file system objects that might be TalkingBooks. On Windows, a Talking Book will be
     * a file system root. On MacOS, it will be a subdirectory of /Volumes.
     *
     * @return The potential Talking Book objects.
     */
    private List<File> getRoots() {
        List<File> roots = new ArrayList<>();
        if (OsUtils.WINDOWS) {
            for (File root : File.listRoots()) {
                if (root.getAbsoluteFile().toString().compareTo("D:") >= 0
                    && root.listFiles() != null && (filter == null || filter.test(root))) {
                    roots.add(root);
                }
            }
        } else if (OsUtils.MAC_OS) {
            File[] macDrives = new File("/Volumes").listFiles();
            if (macDrives != null) {
                for (File r : macDrives) {
                    if (r.listFiles() != null && (filter == null || filter.test(r))) {
                        roots.add(r);
                    }
                }
            }
        }
        return roots;
    }

    /**
     * Does the work of geting the list of roots, looking for changes, and notifying the owner
     * of any changes.
     */
    private synchronized void updateRoots() throws InterruptedException, InvocationTargetException {
        List<File> roots = getRoots();
        boolean needRefresh = refreshRequested.getAndSet(false);
        if (needRefresh) {
            oldList.clear();
        }

        needRefresh = needRefresh || oldList.size() != roots.size();
        if (!needRefresh) {
            for (File root : roots) {
                if (!oldList.contains(root.getAbsolutePath())) {
                    needRefresh = true;
                    break;
                }
            }
        }

        if (needRefresh) {
            LOG.log(Level.INFO, "deviceMonitor sees added/removed/changed drive");
            UIUtils.invokeAndWait(() -> {
                try {
                    rootsHandler.accept(roots);
                } catch (Exception ex) {
                    if (!(ex.getCause() instanceof InterruptedException)) {
                        System.out.println(ex);
                        ex.printStackTrace();
                    } else {
                        System.out.println("FS Monitor thread terminated");
                    }
                }
            });
            oldList.clear();
            oldList.addAll(roots.stream().map(File::getAbsolutePath).collect(Collectors.toList()));
        }
    }

    @Override
    public void run() {
        //noinspection InfiniteLoopStatement
        while (true) {
            try {
                if (enabled) {
                    updateRoots();
                }
                //noinspection BusyWait
                sleep(MILLIS_BETWEEN_SCANS);
            } catch (InterruptedException | InvocationTargetException e) {
//                e.printStackTrace();
            }
        }
    }

    private final Pattern parallelsWinDrive = Pattern.compile("(?i)\\[[A-Z]] Windows.*");
    private FilterParams filterParams;

    private boolean rootsFilter(File root) {
        try {
            String label = fsView.getSystemDisplayName(root);
            String type = fsView.getSystemTypeDescription(root);
            String description = type != null ? type : label;
            if (label.trim().equals("CD Drive") || label.startsWith("DVD") || label.contains(
                "Macintosh") || parallelsWinDrive.matcher(label).matches()) {
                return false;
            }
            // Ignore network drives. Includes host drives shared by Parallels.
            String typeDescr = fsView.getSystemTypeDescription(root);
            if (typeDescr != null && typeDescr.equalsIgnoreCase("network drive")) {
                return false;
            }
            long size = root.getTotalSpace();

            if (size < filterParams.minSize || size > filterParams.maxSize) return false;
            if (filterParams.allowedLabels.size() > 0) {
                if (filterParams.allowedLabels.stream()
                                              .noneMatch(l -> l.equalsIgnoreCase(description))) {
                    return false;
                }
            }
            return filterParams.forbiddenLabels.stream().noneMatch(description::startsWith);
        } catch (Exception ex) {
            return false;
        }
    }

    @SuppressWarnings({ "SameParameterValue", "UnusedReturnValue" })
    public static class FilterParams {
        private static final long MiB = 1024 * 1024;
        private static final long GB = 1000 * 1000 * 1000;
        private static final long GiB = 1024 * 1024 * 1024L;
        long minSize = 0L;
        long maxSize = Long.MAX_VALUE;
        List<String> allowedLabels = new ArrayList<>();
        List<String> forbiddenLabels = new ArrayList<>();

        FilterParams() { }

        FilterParams(FilterParams other) {
            this.minSize = other.minSize;
            this.maxSize = other.maxSize;
            this.allowedLabels.addAll(other.allowedLabels);
            this.forbiddenLabels.addAll(other.forbiddenLabels);
        }

        FilterParams minimum(long minSize) {
            if (minSize > 1000) {
                this.minSize = minSize;
            } else {
                this.minSize = minSize * GB;
                this.minSize = (this.minSize < MiB) ? 0 : this.minSize - MiB;
            }
            return this;
        }

        FilterParams maximum(long maxSize) {
            if (maxSize > 1000) {
                this.maxSize = maxSize;
            } else {
                this.maxSize = maxSize * GiB;
                this.maxSize = (this.maxSize > Long.MAX_VALUE - MiB) ?
                               Long.MAX_VALUE :
                               this.maxSize + MiB;
            }
            return this;
        }

        FilterParams allowing(String... allowed) {
            Collections.addAll(allowedLabels, allowed);
            return this;
        }

        FilterParams forbidding(String... forbidden) {
            Collections.addAll(forbiddenLabels, forbidden);
            return this;
        }
    }

}
