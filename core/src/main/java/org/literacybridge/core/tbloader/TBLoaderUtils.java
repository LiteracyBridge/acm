package org.literacybridge.core.tbloader;

import org.literacybridge.core.fs.TbFile;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.literacybridge.core.tbloader.TBLoaderConstants.DEFAULT_GROUP_LABEL;
import static org.literacybridge.core.tbloader.TBLoaderConstants.GROUP_FILE_EXTENSION;
import static org.literacybridge.core.tbloader.TBLoaderConstants.IMAGES_SUBDIR;
import static org.literacybridge.core.tbloader.TBLoaderConstants.STARTING_SERIALNUMBER;

public class TBLoaderUtils {
    private static final Logger LOG = Logger.getLogger(TBLoaderUtils.class.getName());

    public static String getDateTime() {
        SimpleDateFormat sdfDate = new SimpleDateFormat(
                "yyyy'y'MM'm'dd'd'HH'h'mm'm'ss's'");
        String dateTime = sdfDate.format(new Date());
        return dateTime;
    }

    /**
     * Tests whether a string is a valid "serial number" for this TB-Loader (sensitive to whether the TB-Loader
     * is being run in "old TB mode" or "new TB mode").
     *
     * @param srn - the string to check
     * @return TRUE if the string could be a serial number, FALSE if not.
     */
    public static boolean isSerialNumberFormatGood(String prefix, String srn) {
        boolean isGood;
        if (srn == null)
            isGood = false;
        else if (srn.toLowerCase().startsWith(prefix.toLowerCase())
                && srn.length() == 10)
            isGood = true;
        else {
            isGood = false;
            LOG.log(Level.INFO, "***Incorrect Serial Number Format:" + srn + "***");
        }
        return isGood;
    }

    /**
     * Tests whether a string is a valid new-style "serial number".
     *
     * @param srn - the string to check
     * @return TRUE if the string is a well-formed serial number, FALSE if not.
     */
    public static boolean isSerialNumberFormatGood2(String srn) {
        final int maxTbId = 0x2f;
        boolean isGood = false;
        try {
            if (srn != null && srn.length() == 10 && srn.substring(1, 2).equals("-")
                    && (srn.substring(0, 1).equalsIgnoreCase("A") || srn.substring(0, 1)
                    .equalsIgnoreCase("B"))) {
                int highBytes = Integer.parseInt(srn.substring(2, 6), 0x10);
                // TODO: What was this trying to do? It appears to try to guess that if the "tbDeviceInfo number" part of a srn is
                // in the range 0-N, then the SRN must be > X, where N was "0x10" and X was "0x200" (it was the case that if
                // a number was assigned, the tbDeviceInfo number would have been smaller than 0x10 for most devices, and the
                // serial numbers were arbitrarily started at 0x200 for each tbDeviceInfo). I (Bill) *think* that this just
                // missed getting updated as the tbDeviceInfo number(s) increased; an ordinary bug. Does show the value of
                // burned-in serial numbers.
                //
                // The number below needs to be greater than the highest assigned "tbDeviceInfo" (TB Laptop). As of 23-May-16,
                // that highest assigned tbDeviceInfo number is 0x14. Opening up the range eases maintenance, but lets more
                // corrupted srns get through. 0x2f is somewhere in the middle.
                if ((highBytes < maxTbId) || (highBytes > 0x8000 && highBytes < (0x8000 | maxTbId))) {
                    int lowBytes = Integer.parseInt(srn.substring(6), 0x10);
                    if (lowBytes >= STARTING_SERIALNUMBER) {
                        isGood = true;
                    }
                }
            }
        } catch (NumberFormatException e) {
            isGood = false;
        }
        return isGood;
    }

    /**
     * Given a Content Update directory with one or more images (packages), and a community, find
     * the package that matches the community.
     *
     * @param deploymentDirectory The Content Update directory, with one or more images.
     * @param community           The community name for which the image name is desired. Like "vingving - jirapa"
     * @return The name of the image that matches, like "demo-2017-3-dga".
     */
    public static String getImageForCommunity(final File deploymentDirectory, String community) {
        if (community == null) {
            return null;
        }
        String imageName = "";
        String defaultImageName = "";
        String groupName = "";
        File[] images;

        File imagesDir = new File(deploymentDirectory, IMAGES_SUBDIR.asString());
        images = imagesDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });
        if (images != null && images.length == 1) {
            // Take the only image package
            imageName = images[0].getName();
        } else if (images != null) {
            // Multiple images, so look for a match
            File communityDir = new File(deploymentDirectory,
                    "communities" + File.separator + community + File.separator + "system");

            if (communityDir.exists()) {
                // get groups in community
                String[] groups = communityDir.list(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        String lowercase = name.toLowerCase();
                        return lowercase.endsWith(GROUP_FILE_EXTENSION);
                    }
                });
                // For each .grp file in the community...
                groupsLoop:
                for (String group : groups) {
                    // look for a match in each of images's group listing
                    groupName = group.substring(0, group.length() - 4);
                    for (File image : images) {
                        File systemDir = new File(image, "system");
                        String[] imageGroups = systemDir.list(new FilenameFilter() {
                            @Override
                            public boolean accept(File dir, String name) {
                                String lowercase = name.toLowerCase();
                                return lowercase.endsWith(GROUP_FILE_EXTENSION);
                            }
                        });
                        for (String imageGroup : imageGroups) {
                            String imageGroupName = imageGroup.substring(0,
                                    imageGroup.length() - 4);
                            if (imageGroupName.equalsIgnoreCase(groupName)) {
                                imageName = image.getName();
                                break groupsLoop;
                            } else if (imageGroupName.equalsIgnoreCase(DEFAULT_GROUP_LABEL)) {
                                defaultImageName = image.getName();
                            }
                        }
                    }
                }
            }
            if (imageName.length() == 0) {
                // no match of groups between community and multiple packages
                // Only hope is that there is a default package. If there were multiples,
                // we'll pick one at random (last one wins).
                imageName = defaultImageName;
            }
        }
        if (imageName.length() == 0) {
            imageName = "ERROR!  MISSING CONTENT IMAGE!";
        }
        return imageName;
    }

    /**
     * Find the firmware version number or numbers in a Content Update.
     *
     * @param deploymentDirectory The Content Update directory.
     * @return A string with "(No firmware)", a version like "r1216", or "(Multiple Firmwares!)".
     */
    public static String getFirmwareVersionNumbers(TbFile deploymentDirectory) {
        String version = "(No firmware)";

        try {
            // get Package
            String[] files = deploymentDirectory.open(TBLoaderConstants.CONTENT_BASIC_SUBDIR)
                    .list(new TbFile.FilenameFilter() {
                        @Override
                        public boolean accept(TbFile parent, String name) {
                            String lowercase = name.toLowerCase();
                            return lowercase.endsWith(".img");
                        }
                    });
            if (files.length > 1) {
                version = "(Multiple Firmwares!)";
            } else if (files.length == 1) {
                version = files[0].substring(0, files[0].length() - 4);
            }
        } catch (Exception ignore) {
            LOG.log(Level.WARNING, "exception - ignore and keep going with default string", ignore);
        }
        return version;
    }
}
