package org.literacybridge.core.tbloader;

import org.literacybridge.core.tbdevice.TbDeviceInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static org.literacybridge.core.tbloader.TBLoaderConstants.DEFAULT_GROUP_LABEL;
import static org.literacybridge.core.tbloader.TBLoaderConstants.GROUP_FILE_EXTENSION;
import static org.literacybridge.core.tbloader.TBLoaderConstants.IMAGES_SUBDIR_OLD;
import static org.literacybridge.core.tbloader.TBLoaderConstants.IMAGES_SUBDIR_V1;
import static org.literacybridge.core.tbloader.TBLoaderConstants.IMAGES_SUBDIR_V2;
import static org.literacybridge.core.tbloader.TBLoaderConstants.RECIPIENTID_PROPERTY;

public class TBLoaderUtils {
    private static final Logger LOG = Logger.getLogger(TBLoaderUtils.class.getName());
    public static final Pattern GOOD_SRN_PATTERN = Pattern.compile("(?i)^([abc]-([0-9a-f]{4})([0-9a-f]{4}))$");
    // After several duplicate B- serial numbers were assigned, we updated all B- to C- with new numbers.
    public static final Pattern UPDATED_SRN_PATTERN = Pattern.compile("(?i)^([ac]-([0-9a-f]{4})([0-9a-f]{4}))$");

    public static String getDateTime() {
        return getDateTime(new Date());
    }
    public static String getDateTime(Date date) {
        //noinspection SuspiciousDateFormat
        SimpleDateFormat sdfDate = new SimpleDateFormat(
                "yyyy'y'MM'm'dd'd'HH'h'mm'm'ss's'", Locale.US);
        return sdfDate.format(date);
    }

    /**
     * Tests whether a string is a valid "serial number" for this TB-Loader (sensitive to whether the TB-Loader
     * is being run in "old TB mode" or "new TB mode").
     * @param prefix The serial number "series" of interest, like "A-", "B-", or "C-"
     * @param srn - the string to check
     * @return TRUE if the string could be a serial number, FALSE if not.
     */
    public static boolean isSerialNumberFormatGood(String prefix, String srn) {
        // matcher looks for A-, B- or C-0123ABCD
        boolean isGood = (srn != null && GOOD_SRN_PATTERN.matcher(srn).matches() && srn.toLowerCase().startsWith(prefix.toLowerCase()));
        if (!isGood) {
            LOG.log(Level.FINE, "TBL!: ***Incorrect Serial Number Format:" + srn + "***");
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
        // matcher looks for A-, B-, or C-0123ABCD
        return (srn != null && GOOD_SRN_PATTERN.matcher(srn).matches());
    }

    /**
     * Tests whether a given string is a valid serial number in the given series. This will test whether
     * the given string is a well-formed serial nubmer, and also whether it is part of the deprecated "B-" series.
     * @param series The serial number series of interest, "A-", "B-", or "C-".
     * @param srn The string to test.
     * @return True if a serial number is needed to repalce the given string.
     */
    public static boolean newSerialNumberNeeded(String series, String srn) {
        return srn == null || !UPDATED_SRN_PATTERN.matcher(srn).matches() || !srn.toLowerCase().startsWith(series.toLowerCase());
    }

    private static File findImagesDir(File deploymentDirectory, TbDeviceInfo.DEVICE_VERSION device_version) {
        if (device_version == TbDeviceInfo.DEVICE_VERSION.TBv2) {
            return new File(deploymentDirectory, IMAGES_SUBDIR_V2.asString());
        }
        // Not v2, so assume v1.
        File imagesDir = new File(deploymentDirectory, IMAGES_SUBDIR_V1.asString());
        if (!imagesDir.isDirectory()) {
            imagesDir = new File(deploymentDirectory, IMAGES_SUBDIR_OLD.toString());
        }
        return imagesDir;
    }

    /**
     * Helper to find an images directory. This is only useful for callers that are not concerned
     * with differences between TBv1 and TBv2, such as finding the names of the packages.
     *
     * This assumes that the same packages exist in both TBv1 and TBv2 deployments.
     *
     * @param deploymentDirectory The directory with the whole deployment.
     * @return A File object for images.v2, images.v1, or images.
     */
    private static File findImagesDir(File deploymentDirectory) {
        File imagesDir = new File(deploymentDirectory, IMAGES_SUBDIR_V2.asString());
        if (!imagesDir.isDirectory()) {
            imagesDir = new File(deploymentDirectory, IMAGES_SUBDIR_V1.toString());
            if (!imagesDir.isDirectory()) {
                imagesDir = new File(deploymentDirectory, IMAGES_SUBDIR_OLD.toString());
            }
        }
        return imagesDir;
    }

    /**
     * Gets the list of packages in a Deployment.
     * @param deploymentDirectory with the deployment.
     * @return a String[] of the packages in the deployment. Empty array if no packages found.
     */
    public static String[] getPackagesInDeployment(File deploymentDirectory) {
        // ~/Amplio/TB-Loaders/{project}/content/{deployment}/images
        File imagesDir = findImagesDir(deploymentDirectory);
        String[] packages = imagesDir.list( (dir, name) -> new File(dir, name).isDirectory() );
        if (packages == null) {
            packages = new String[0];
        }
        return packages;
    }
    public static String[] getPackagesInDeployment(File deploymentDirectory, TbDeviceInfo.DEVICE_VERSION device_version) {
        // ~/Amplio/TB-Loaders/{project}/content/{deployment}/images
        File imagesDir = findImagesDir(deploymentDirectory, device_version);
        String[] packages = imagesDir.list( (dir, name) -> new File(dir, name).isDirectory() );
        if (packages == null) {
            packages = new String[0];
        }
        return packages;
    }

    /**
     * Given a Deployment directory with one or more images (packages), and a community, find
     * the package that matches the community.
     *
     * @param deploymentDirectory The Deployment directory, with one or more images, like
     *                            ~/Amplio/TB-Loaders/{project}/content/{deployment}
     * @param community           The community name for which the image name is desired. Like "vingving - jirapa"
     * @return The name of the image that matches, like "demo-2017-3-dga".
     */
    @Deprecated
    public static String getImageForCommunity(final File deploymentDirectory, String community) {
        return getPackageForCommunity(deploymentDirectory, community);
    }

    public static String getPackageForCommunity(final File deploymentDirectory, String community) {
        if (community == null) {
            return null;
        }
        String imageName = "";
        String defaultImageName = "";
        String groupName;
        File[] images;

        // ~/Amplio/TB-Loaders/{project}/content/{deployment}/images
        File imagesDir = findImagesDir(deploymentDirectory);
        images = imagesDir.listFiles(File::isDirectory);
        if (images != null && images.length == 1) {
            // Take the only image package
            imageName = images[0].getName();
        } else if (images != null) {
            // Multiple images, so look for a match
            File communityDir = new File(deploymentDirectory,
                    "communities" + File.separator + community + File.separator + "system");

            if (communityDir.exists() && communityDir.isDirectory()) {
                // get groups in community; ie get list of '*.grp' marker files
                String[] groups = communityDir.list((dir, name) -> {
                    String lowercase = name.toLowerCase();
                    return lowercase.endsWith(GROUP_FILE_EXTENSION);
                });
                // For each .grp file in the community...
                groupsLoop:
                for (String group : groups != null ? groups : new String[0]) {
                    // look for a match in each of images's group listing
                    groupName = group.substring(0, group.length() - 4);
                    for (File image : images) {
                        // ~/Amplio/TB-Loaders/{project}/content/{deployment}/images/{image}/system
                        File systemDir = new File(image, "system");
                        // ~/Amplio/TB-Loaders/{project}/content/{deployment}/images/{image}/system/*.grp
                        String[] imageGroups = systemDir.list((dir, name) -> {
                            String lowercase = name.toLowerCase();
                            return lowercase.endsWith(GROUP_FILE_EXTENSION);
                        });
                        if (imageGroups == null) {
                            continue;
                        }
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
            if (imageName.isEmpty()) {
                // no match of groups between community and multiple packages
                // Only hope is that there is a default package. If there were multiples,
                // we'll pick one at random (last one wins).
                imageName = defaultImageName;
            }
        }
        if (imageName.isEmpty()) {
            imageName = TBLoaderConstants.MISSING_PACKAGE;
        }
        return imageName;
    }

    /**
     * Find the firmware version number or numbers in a Deployment.
     *
     * @param deploymentDirectory The Deployment directory.
     * @return A string with "(No firmware)", a version like "r1216", or "(Multiple Firmwares!)".
     */
    public static String getFirmwareVersionNumbers(File deploymentDirectory) {
        String version = "(No firmware)";

        try {
            // get Package
            File firmwareDir = new File(deploymentDirectory, TBLoaderConstants.CONTENT_BASIC_SUBDIR);
            if (!firmwareDir.isDirectory()) new File(deploymentDirectory, "firmware.v1");
            if (!firmwareDir.isDirectory()) new File(deploymentDirectory, "firmware");
            String[] files = firmwareDir
                .list((dir, name) -> {
                    String lowercase = name.toLowerCase();
                    return lowercase.endsWith(".img");
                });
            if (files != null && files.length > 1) {
                version = "(Multiple Firmwares!)";
            } else if (files != null && files.length == 1) {
                version = files[0].substring(0, files[0].length() - 4);
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "TBL!: exception - ignore and keep going with default string", ex);
        }
        return version;
    }

    /**
     *
     * @param deploymentDirectory The directory with images. {project}/content/{Deployment}
     * @param communityName The community (directory) name.
     * @return The recipient id, if it can be determined, otherwise null.
     */
    public static String getRecipientIdForCommunity(File deploymentDirectory, String communityName) {
        // ~/Amplio/TB-Loaders/{project}/content/{deployment}/communities/{communitydir}
        File communitiesDir = new File(deploymentDirectory, "communities");
        File communityDir = new File(communitiesDir, communityName);

        return getRecipientProperty(communityDir, RECIPIENTID_PROPERTY);
    }

    public static String getRecipientProperty(File communityDir, String propertyName) {
        File recipientidFile = new File(communityDir, "recipient.id");

        if (recipientidFile.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(recipientidFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    String[] parts = line.split("=", 2);
                    if (parts[0].equalsIgnoreCase(propertyName)) {
                        return parts[1].trim();
                    }
                }
            } catch (IOException e) {
                // Ignore
            }
        }
        return null;
    }

    /**
     * Converts number of bytes into proper scale.
     *
     * @param bytes number of bytes to be converted.
     * @return A string that represents the bytes in a proper scale.
     */
    //@SuppressLint("DefaultLocale")
    public static String getBytesString(long bytes) {
        String[] quantifiers = new String[] {
                "KiB", "MiB", "GiB", "TiB"
        };
        double sizeNum = bytes;
        for (int i = 0;; i++) {
            if (i >= quantifiers.length) {
                return "Too Much";
            }
            sizeNum /= 1024;
            if (sizeNum <= 999) {
                return String.format("%.2f %s", sizeNum, quantifiers[i]);
            }
        }
    }

    public static UUID uuidFromName(UUID namespace, byte[] nameBytes) {
        byte[] namespaceBytes = getBytesFromUUID(namespace);
        byte[] bytes = new byte[namespaceBytes.length + nameBytes.length];
        System.arraycopy(namespaceBytes, 0, bytes, 0, namespaceBytes.length);
        System.arraycopy(nameBytes, 0, bytes, namespaceBytes.length, nameBytes.length);
        return UUID.nameUUIDFromBytes(bytes);
    }
    public static UUID uuidFromName(UUID namespace, String name) {
        return uuidFromName(namespace, name.getBytes());
    }

    private static byte[] getBytesFromUUID(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());

        return bb.array();
    }

    private static UUID getUUIDFromBytes(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        long high = byteBuffer.getLong();
        long low = byteBuffer.getLong();

        return new UUID(high, low);
    }
}
