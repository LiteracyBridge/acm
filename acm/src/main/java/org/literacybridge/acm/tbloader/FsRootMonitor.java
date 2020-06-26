package org.literacybridge.acm.tbloader;

import org.literacybridge.acm.gui.util.UIUtils;
import org.literacybridge.acm.utils.OsUtils;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Monitors the file system roots for something that seems to be a Talking Book.
 */
class FsRootMonitor extends Thread {
    private static final Logger LOG = Logger.getLogger(FsRootMonitor.class.getName());

    private final FileSystemView fsView = FileSystemView.getFileSystemView();
    private boolean enabled = true;
    Set<String> oldList = new HashSet<>();

    private final Predicate<File> filter;
    private final Consumer<List<File>> rootsHandler;

    public FsRootMonitor(Consumer<List<File>> rootsHandler) {
        this.filterParams = new FilterParams().minimum(2).maximum(16).forbidding("Network Drive");
        if (OsUtils.WINDOWS) {
            filterParams.allowing("USB Drive");
        }
        this.filter = this::rootsFilter;
        this.rootsHandler = rootsHandler;
        this.setDaemon(true);
    }

    public FilterParams getFilterParams() {
        return filterParams;
    }

    public void setFilterParams(FilterParams filterParams) {
        this.filterParams = filterParams;
        refresh();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public synchronized void refresh() {
        oldList.clear();
        updateRoots(true);
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
     *
     * @param force If true, always notifies. If false, notifies only if changes.
     */
    private synchronized void updateRoots(boolean force) {
        List<File> roots = getRoots();

        boolean needRefresh = force || oldList.size() != roots.size();
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
                    //ex.printStackTrace();
                }
            });
            oldList.clear();
            oldList.addAll(roots.stream().map(File::getAbsolutePath).collect(Collectors.toList()));
        }
    }

    @Override
    public void run() {
        boolean firstPass = true;
        //noinspection InfiniteLoopStatement
        while (true) {
            if (enabled) {
                updateRoots(firstPass);
                firstPass = false;
            }
            //noinspection BusyWait
            try {
                sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private FilterParams filterParams;

    private boolean rootsFilter(File root) {
        try {
            String label = fsView.getSystemDisplayName(root);
            String type = fsView.getSystemTypeDescription(root);
            String description = type != null ? type : label;
            if (label.trim().equals("CD Drive") || label.startsWith("DVD") || label.contains(
                "Macintosh")) {
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