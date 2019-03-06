package org.literacybridge.core.tbloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.literacybridge.core.tbloader.TBLoaderConstants.DEFAULT_GROUP_LABEL;
import static org.literacybridge.core.tbloader.TBLoaderConstants.GROUP_FILE_EXTENSION;
import static org.literacybridge.core.tbloader.TBLoaderConstants.IMAGES_SUBDIR;
import static org.literacybridge.core.tbloader.TBLoaderConstants.RECIPIENTID_PROPERTY;
import static org.literacybridge.core.tbloader.TBLoaderConstants.STARTING_SERIALNUMBER;

public class TBLoaderUtils {
    private static final Logger LOG = Logger.getLogger(TBLoaderUtils.class.getName());

    public static String getDateTime() {
        return getDateTime(new Date());
    }
    public static String getDateTime(Date date) {
        SimpleDateFormat sdfDate = new SimpleDateFormat(
                "yyyy'y'MM'm'dd'd'HH'h'mm'm'ss's'", Locale.US);
        return sdfDate.format(date);
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
        final int maxTbId = 0x4f;
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
                // The number below needs to be greater than the highest assigned "tbDeviceInfo" (TB Laptop). As of 6-March-19,
                // that highest assigned tbDeviceInfo number is 0x21. Opening up the range eases maintenance, but lets more
                // corrupted srns get through. 0x4f is somewhere in the middle.
                if ((highBytes < maxTbId) || (highBytes > 0x8000 && highBytes < (0x8000 | maxTbId))) {
                    int lowBytes = Integer.parseInt(srn.substring(6), 0x10);
                    // A batch of TBs was shipped from the factory with "B-000C036A". Re-allocate those.
                    if (highBytes == 12 && lowBytes == 874) {
                        isGood = false;
                    } else if (lowBytes >= STARTING_SERIALNUMBER) {
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
     * Given a Deployment directory with one or more images (packages), and a community, find
     * the package that matches the community.
     *
     * @param deploymentDirectory The Deployment directory, with one or more images, like
     *                            ~/LiteracyBridge/TB-Loaders/{project}/content/{deployment}
     * @param community           The community name for which the image name is desired. Like "vingving - jirapa"
     * @return The name of the image that matches, like "demo-2017-3-dga".
     */
    public static String getImageForCommunity(final File deploymentDirectory, String community) {
        if (community == null) {
            return null;
        }
        String imageName = "";
        String defaultImageName = "";
        String groupName;
        File[] images;

        // ~/LiteracyBridge/TB-Loaders/{project}/content/{deployment}/images
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

            if (communityDir.exists() && communityDir.isDirectory()) {
                // get groups in community; ie get list of '*.grp' marker files
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
                        // ~/LiteracyBridge/TB-Loaders/{project}/content/{deployment}/images/{image}/system
                        File systemDir = new File(image, "system");
                        // ~/LiteracyBridge/TB-Loaders/{project}/content/{deployment}/images/{image}/system/*.grp
                        String[] imageGroups = systemDir.list(new FilenameFilter() {
                            @Override
                            public boolean accept(File dir, String name) {
                                String lowercase = name.toLowerCase();
                                return lowercase.endsWith(GROUP_FILE_EXTENSION);
                            }
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
            if (imageName.length() == 0) {
                // no match of groups between community and multiple packages
                // Only hope is that there is a default package. If there were multiples,
                // we'll pick one at random (last one wins).
                imageName = defaultImageName;
            }
        }
        if (imageName.length() == 0) {
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
            String[] files = new File(deploymentDirectory, TBLoaderConstants.CONTENT_BASIC_SUBDIR)
                .list(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        String lowercase = name.toLowerCase();
                        return lowercase.endsWith(".img");
                    }
                });
            if (files.length > 1) {
                version = "(Multiple Firmwares!)";
            } else if (files.length == 1) {
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
        // ~/LiteracyBridge/TB-Loaders/{project}/content/{deployment}/communities/{communitydir}
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
        UUID result = UUID.nameUUIDFromBytes(bytes);
        return result;
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
        Long high = byteBuffer.getLong();
        Long low = byteBuffer.getLong();

        return new UUID(high, low);
    }
}
