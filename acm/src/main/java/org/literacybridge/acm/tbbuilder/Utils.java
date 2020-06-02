package org.literacybridge.acm.tbbuilder;

import org.apache.commons.io.FilenameUtils;
import org.literacybridge.acm.config.ACMConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class Utils {
    private final TBBuilder tbBuilder;
    private final TBBuilder.BuilderContext builderContext;

    static private final Pattern deplPattern = Pattern.compile("(.*)-(\\d+)-(\\d+)");

    Utils(TBBuilder tbBuilder, TBBuilder.BuilderContext builderContext) {
        this.tbBuilder = tbBuilder;
        this.builderContext = builderContext;
    }

    /**
     * Gets a list of all of the firmware versions in the program. While only the latest will be
     * *installed*, any of the ones present are considered acceptable.
     * @return A list, possibly empty, of the firmware versions in the program.
     */
    public List<String> allFirmwareImageVersions() {
        File[] firmwareVersions = new File(builderContext.sourceTbOptionsDir, "firmware")
            .listFiles((dir, name) -> name.endsWith(".img"));
        if (firmwareVersions != null) {
            return Arrays.stream(firmwareVersions)
                .map(File::getName)
                .map(FilenameUtils::getBaseName)
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    /**
     * Finds the latest firmware image for the program. Looks in the
     * ~/CloudDir/ACM-{program}/TB-Loaders/TB_Options/firmware directory.
     * @return The highest numbered firmware found.
     */
    File latestFirmwareImage() {
        // Find the lexically greatest filename of firmware. Works because we'll never exceed 4 digits.
        File latestFirmware = null;
        File[] firmwareVersions = new File(builderContext.sourceTbOptionsDir, "firmware")
            .listFiles();
        for (File f : firmwareVersions) {
            if (latestFirmware == null) {
                latestFirmware = f;
            } else if (latestFirmware.getName().compareToIgnoreCase(f.getName()) < 0) {
                latestFirmware = f;
            }
        }
        return latestFirmware;
    }

    /**
     * Is the given deployment a good name for a deployment in this program?
     * @param deployment name to be excamined
     * @param acmName name of the program
     * @param deploymentNumberStr deployment number
     * @return true if the name looks OK, false otherwise.
     */
    static boolean isAcceptableNameForDeployment(String deployment, String acmName, String deploymentNumberStr) {
        int deploymentNum = Integer.parseInt(deploymentNumberStr);
        Calendar rightNow = Calendar.getInstance();
        int year = rightNow.get(Calendar.YEAR);
        int shortYear = year % 100;
        Matcher matcher = deplPattern.matcher(deployment);
        if (matcher.matches() && matcher.groupCount() == 3) {
            // Our deployment?
            if (Integer.parseInt(matcher.group(3)) != deploymentNum) return false;
            // Our ACM?
            if (!ACMConfiguration.cannonicalProjectName(acmName).equalsIgnoreCase(matcher.group(1))) return false;
            // This year? Last year? Next year?
            int deplYear = Integer.parseInt(matcher.group(2));
            return Math.abs(deplYear - year) <= 1 || Math.abs(deplYear - shortYear) <= 1;
        }
        return false;
    }

    /**
     * Deletes all the *.rev files from the given directory.
     *
     * @param dir from which to remove .rev files.
     */
    static void deleteRevFiles(File dir) {
        File[] files = dir.listFiles((dir1, name) -> name.toLowerCase().endsWith(".rev"));
        if (files != null) {
            for (File revisionFile : files) {
                revisionFile.delete();
            }
        }
    }
}
