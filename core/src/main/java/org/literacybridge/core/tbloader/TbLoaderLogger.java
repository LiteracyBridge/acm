package org.literacybridge.core.tbloader;

import org.literacybridge.core.fs.OperationLog;
import org.literacybridge.core.fs.TbFile;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static org.literacybridge.core.fs.TbFile.Flags.append;
import static org.literacybridge.core.fs.TbFile.Flags.nil;

class TbLoaderLogger {
    public static final String VERSION_TBDATA = "v03";
    private final TBLoaderCore tbLoaderCore;

    public TbLoaderLogger(TBLoaderCore tbLoaderCore) {
        this.tbLoaderCore = tbLoaderCore;
    }

    /**
     * Helper to append the contents of an OperationLog.Operation to a file.
     *
     * @param logData The Operation to be appended.
     * @param logFile The file to which the Operation is to be appended.
     * @throws IOException If there is an error writing to the file.
     */
    private void writeLogDataToFile(OperationLog.Operation logData, TbFile logFile) throws IOException {
        // Copy the k=v .log file next to the .csv file.
        InputStream logContent = new ByteArrayInputStream(logData.formatLog().getBytes());
        boolean isNewLogFile = !logFile.exists();
        TbFile.Flags logFlag = isNewLogFile ? nil : append;
        logFile.createNew(logContent, logFlag);
        logContent.close();
    }

    /**
     * Writes the log files that the statistics processing will use to analyze deployments and usage.
     *
     * @param action          The TB-Loader action, "update", "update-fw", "stats-only", etc.
     * @param durationSeconds How long it took to collect any stats and perform any Deployment.
     * @throws IOException if there is an error writing any of the log files.
     */
    void logTBData(
        String action,
        int durationSeconds) throws IOException {

        OperationLog.Operation opLog = OperationLog.startOperation("LogTbData");
        OperationLog.Operation statsLog = OperationLog.startOperation("statsdata");
        OperationLog.Operation deploymentLog = OperationLog.startOperation("deployment");
        OperationLog.Info operationInfo = new OperationLog.Info();
        OperationLog.Info statsInfo = new OperationLog.Info();


        BufferedWriter bw;

        // like /Users/alice/Dropbox/tbcd000c/{PROJECT}/OperationalData/{TBCDID}/tbdata-v03-{YYYYyMMmDDd}-{TBCDID}.csv
        TbFile csvFile = tbLoaderCore.getCollectedOpDataDir().open(tbLoaderCore.getOpCsvFilename());

        try {
            csvFile.getParent().mkdirs();
            boolean isNewFile = !csvFile.exists();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bw = new BufferedWriter(new OutputStreamWriter(baos));
            if (isNewFile) {
                bw.write("PROJECT,UPDATE_DATE_TIME,OUT_SYNCH_DIR,LOCATION,ACTION,DURATION_SEC,");
                bw.write("OUT-SN,OUT-DEPLOYMENT,OUT-IMAGE,OUT-FW-REV,OUT-COMMUNITY,OUT-ROTATION-DATE,");
                bw.write(
                    "IN-SN,IN-DEPLOYMENT,IN-IMAGE,IN-FW-REV,IN-COMMUNITY,IN-LAST-UPDATED,IN-SYNCH-DIR,IN-DISK-LABEL,CHKDSK CORRUPTION?,");
                bw.write("FLASH-SN,FLASH-REFLASHES,");
                bw.write(
                    "FLASH-DEPLOYMENT,FLASH-IMAGE,FLASH-COMMUNITY,FLASH-LAST-UPDATED,FLASH-CUM-DAYS,FLASH-CORRUPTION-DAY,FLASH-VOLT,FLASH-POWERUPS,FLASH-PERIODS,FLASH-ROTATIONS,");
                bw.write(
                    "FLASH-MSGS,FLASH-MINUTES,FLASH-STARTS,FLASH-PARTIAL,FLASH-HALF,FLASH-MOST,FLASH-ALL,FLASH-APPLIED,FLASH-USELESS");
                for (int i = 0; i < 5; i++) {
                    bw.write(",FLASH-ROTATION,FLASH-MINUTES-R" + i + ",FLASH-PERIOD-R" + i
                        + ",FLASH-HRS-POST-UPDATE-R" + i + ",FLASH-VOLT-R" + i);
                }
                bw.write("\n");
            }
            // If the TB is moving between projects, the old project and new project are different.
            // This single value can then be correct for stats, or for deployment. Go for stats;
            // the deployment is tracked more easily in the deploymentLog.
            bw.write(tbLoaderCore.mOldDeploymentInfo.getProjectName().toUpperCase() + ",");
            bw.write(tbLoaderCore.mLegacyFormatUpdateTimestamp.toUpperCase() + ",");
            bw.write(tbLoaderCore.mLegacyFormatUpdateTimestamp.toUpperCase() + "-" + tbLoaderCore.mTbLoaderConfig.getTbLoaderId()
                .toUpperCase() + ",");
            bw.write(tbLoaderCore.mLocation.toUpperCase() + ",");
            bw.write(action + ",");
            bw.write(durationSeconds + ",");
            bw.write(tbLoaderCore.mTbDeviceInfo.getSerialNumber().toUpperCase() + ",");
            bw.write(tbLoaderCore.mStatsOnly
                     ? ","
                     : tbLoaderCore.mNewDeploymentInfo.getDeploymentName().toUpperCase() + ",");
            bw.write(tbLoaderCore.mStatsOnly
                     ? ","
                     : String.join(",", tbLoaderCore.mNewDeploymentInfo.getPackageNames()).toUpperCase() + ",");
            bw.write(tbLoaderCore.mStatsOnly ? "," : tbLoaderCore.mNewDeploymentInfo.getFirmwareRevision() + ",");
            bw.write(tbLoaderCore.mStatsOnly
                     ? ","
                     : tbLoaderCore.mNewDeploymentInfo.getCommunity().toUpperCase() + ",");
            bw.write(tbLoaderCore.mStatsOnly ? "," : tbLoaderCore.mNewDeploymentInfo.getUpdateTimestamp() + ",");
            bw.write(tbLoaderCore.mOldDeploymentInfo.getSerialNumber().toUpperCase() + ",");
            bw.write(tbLoaderCore.mOldDeploymentInfo.getDeploymentName().toUpperCase() + ",");
            bw.write(String.join(",", String.join(",", tbLoaderCore.mOldDeploymentInfo.getPackageNames()))
                .toUpperCase() + ",");
            bw.write(tbLoaderCore.mOldDeploymentInfo.getFirmwareRevision() + ",");
            bw.write(tbLoaderCore.mOldDeploymentInfo.getCommunity().toUpperCase() + ",");
            bw.write(tbLoaderCore.mOldDeploymentInfo.getUpdateTimestamp() + ",");
            String lastSynchDir = tbLoaderCore.mOldDeploymentInfo.getUpdateDirectory();
            bw.write((lastSynchDir != null ? lastSynchDir.toUpperCase() : "") + ",");
            bw.write(tbLoaderCore.mTbDeviceInfo.getLabel() + ",");
            bw.write(tbLoaderCore.mTbDeviceInfo.isCorrupted() + ",");

            // With one exception, the stats are ordered the same as the csv values. Not necessary,
            // of course, but can help matching them up.
            // The exception is that, here, the action is first, not fifth.
            operationInfo
                .put("action", action)
                .put("tbcdid", tbLoaderCore.mTbLoaderConfig.getTbLoaderId())
                .put("username", tbLoaderCore.mTbLoaderConfig.getUserEmail())
                .put("useremail", tbLoaderCore.mTbLoaderConfig.getUserEmail())
                .put("project", tbLoaderCore.mOldDeploymentInfo.getProjectName().toUpperCase())
                .put("update_date_time", tbLoaderCore.mLegacyFormatUpdateTimestamp.toUpperCase())
                .put("out_synch_dir",
                    tbLoaderCore.mLegacyFormatUpdateTimestamp.toUpperCase() + "-" + tbLoaderCore.mTbLoaderConfig.getTbLoaderId()
                        .toUpperCase())
                .put("location", tbLoaderCore.mLocation.toUpperCase());
            if (tbLoaderCore.mCoordinates != null && tbLoaderCore.mCoordinates.length() > 0) {
                operationInfo.put("coordinates", tbLoaderCore.mCoordinates);
            }
            operationInfo
                .put("duration_sec", Integer.toString(durationSeconds));
            opLog.put(operationInfo);
            statsLog.put(operationInfo);

            if (!tbLoaderCore.mStatsOnly) {
                opLog
                    .put("out_sn", tbLoaderCore.mTbDeviceInfo.getSerialNumber().toUpperCase())
                    .put("out_deployment", tbLoaderCore.mNewDeploymentInfo.getDeploymentName().toUpperCase())
                    .put("out_package",
                        String.join(",", tbLoaderCore.mNewDeploymentInfo.getPackageNames()).toUpperCase())
                    .put("out_firmware", tbLoaderCore.mNewDeploymentInfo.getFirmwareRevision())
                    .put("out_community", tbLoaderCore.mNewDeploymentInfo.getCommunity().toUpperCase())
                    .put("out_rotation", tbLoaderCore.mNewDeploymentInfo.getUpdateTimestamp())
                    .put("out_project", tbLoaderCore.mNewDeploymentInfo.getProjectName())
                    .put("out_testing", tbLoaderCore.mNewDeploymentInfo.isTestDeployment());

                // Everything there is to know about a deployment to a Talking Book should be here.
                deploymentLog
                    .put("action", action)
                    .put("tbcdid", tbLoaderCore.mTbLoaderConfig.getTbLoaderId())
                    .put("username", tbLoaderCore.mTbLoaderConfig.getUserEmail())
                    .put("useremail", tbLoaderCore.mTbLoaderConfig.getUserEmail())
                    .put("sn", tbLoaderCore.mTbDeviceInfo.getSerialNumber().toUpperCase())
                    .put("newsn", tbLoaderCore.mNewDeploymentInfo.isNewSerialNumber())
                    .put("project", tbLoaderCore.mNewDeploymentInfo.getProjectName().toUpperCase())
                    .put("deployment", tbLoaderCore.mNewDeploymentInfo.getDeploymentName().toUpperCase())
                    .put("package", String.join(",", tbLoaderCore.mNewDeploymentInfo.getPackageNames()).toUpperCase())
                    .put("community", tbLoaderCore.mNewDeploymentInfo.getCommunity().toUpperCase())
                    .put("firmware", tbLoaderCore.mNewDeploymentInfo.getFirmwareRevision())
                    .put("location", tbLoaderCore.mLocation.toUpperCase())
                    .put("timestamp", tbLoaderCore.mUpdateTimestampISO)
                    .put("duration", Integer.toString(durationSeconds))
                    .put("testing", tbLoaderCore.mNewDeploymentInfo.isTestDeployment());
                if (tbLoaderCore.mNewDeploymentInfo.getDeploymentNumber() > 0) {
                    deploymentLog.put("deploymentnumber", tbLoaderCore.mNewDeploymentInfo.getDeploymentNumber());
                }
                if (tbLoaderCore.mNewDeploymentInfo.getRecipientid() != null) {
                    opLog.put("out_recipientid", tbLoaderCore.mNewDeploymentInfo.getRecipientid());
                    deploymentLog.put("recipientid", tbLoaderCore.mNewDeploymentInfo.getRecipientid());
                }
                if (tbLoaderCore.mCoordinates != null && tbLoaderCore.mCoordinates.length() > 0) {
                    deploymentLog.put("coordinates", tbLoaderCore.mCoordinates);
                }

            }

            statsInfo
                .put("in_sn", tbLoaderCore.mOldDeploymentInfo.getSerialNumber().toUpperCase())
                .put("in_deployment", tbLoaderCore.mOldDeploymentInfo.getDeploymentName().toUpperCase())
                .put("in_package", String.join(",", tbLoaderCore.mOldDeploymentInfo.getPackageNames()).toUpperCase())
                .put("in_firmware", tbLoaderCore.mOldDeploymentInfo.getFirmwareRevision())
                .put("in_community", tbLoaderCore.mOldDeploymentInfo.getCommunity().toUpperCase())
                .put("in_project", tbLoaderCore.mOldDeploymentInfo.getProjectName())
                .put("in_update_timestamp", tbLoaderCore.mOldDeploymentInfo.getUpdateTimestamp())
                .put("in_synchdir", (lastSynchDir != null ? lastSynchDir.toUpperCase() : ""))
                .put("in_disk_label", tbLoaderCore.mTbDeviceInfo.getLabel())
                .put("disk_corrupted", tbLoaderCore.mTbDeviceInfo.isCorrupted());
            if (tbLoaderCore.mOldDeploymentInfo.getRecipientid() != null) {
                statsInfo.put("in_recipientid", tbLoaderCore.mOldDeploymentInfo.getRecipientid());
                opLog.put("in_recipientid", tbLoaderCore.mOldDeploymentInfo.getRecipientid());
            }

            if (tbLoaderCore.mTtbFlashData != null) {
                bw.write(tbLoaderCore.mTtbFlashData.getSerialNumber().toUpperCase() + ",");
                bw.write(tbLoaderCore.mTtbFlashData.getCountReflashes() + ",");
                bw.write(tbLoaderCore.mTtbFlashData.getDeploymentNumber().toUpperCase() + ",");
                bw.write(tbLoaderCore.mTtbFlashData.getImageName().toUpperCase() + ",");
                bw.write(tbLoaderCore.mTtbFlashData.getCommunity().toUpperCase() + ",");
                bw.write(tbLoaderCore.mTtbFlashData.getUpdateYear() + "/" + tbLoaderCore.mTtbFlashData.getUpdateMonth() + "/"
                    + tbLoaderCore.mTtbFlashData.getUpdateDate() + ",");
                bw.write(tbLoaderCore.mTtbFlashData.getCumulativeDays() + ",");
                bw.write(tbLoaderCore.mTtbFlashData.getCorruptionDay() + ",");
                bw.write(tbLoaderCore.mTtbFlashData.getLastInitVoltage() + ",");
                bw.write(tbLoaderCore.mTtbFlashData.getPowerups() + ",");
                bw.write(tbLoaderCore.mTtbFlashData.getPeriods() + ",");
                bw.write(tbLoaderCore.mTtbFlashData.getProfileTotalRotations() + ",");
                bw.write(tbLoaderCore.mTtbFlashData.getTotalMessages() + ",");

                int totalSecondsPlayed = 0, countStarted = 0, countQuarter = 0, countHalf = 0, countThreequarters = 0, countCompleted = 0, countApplied = 0, countUseless = 0;
                int numRotations = Math.max(5, tbLoaderCore.mTtbFlashData.getProfileTotalRotations());
                for (int m = 0; m < tbLoaderCore.mTtbFlashData.getTotalMessages(); m++) {
                    for (int r = 0; r < numRotations; r++) {
                        totalSecondsPlayed += tbLoaderCore.mTtbFlashData.getStats()[m][r].getTotalSecondsPlayed();
                        countStarted += tbLoaderCore.mTtbFlashData.getStats()[m][r].getCountStarted();
                        countQuarter += tbLoaderCore.mTtbFlashData.getStats()[m][r].getCountQuarter();
                        countHalf += tbLoaderCore.mTtbFlashData.getStats()[m][r].getCountHalf();
                        countThreequarters += tbLoaderCore.mTtbFlashData.getStats()[m][r].getCountThreequarters();
                        countCompleted += tbLoaderCore.mTtbFlashData.getStats()[m][r].getCountCompleted();
                        countApplied += tbLoaderCore.mTtbFlashData.getStats()[m][r].getCountApplied();
                        countUseless += tbLoaderCore.mTtbFlashData.getStats()[m][r].getCountUseless();
                    }
                }
                bw.write(totalSecondsPlayed / 60 + ",");
                bw.write(countStarted + ",");
                bw.write(countQuarter + ",");
                bw.write(countHalf + ",");
                bw.write(countThreequarters + ",");
                bw.write(countCompleted + ",");
                bw.write(countApplied + ",");
                bw.write(String.valueOf(countUseless));
                for (int r = 0; r < numRotations; r++) {
                    bw.write("," + r + "," + tbLoaderCore.mTtbFlashData.totalPlayedSecondsPerRotation(r) / 60
                        + "," + tbLoaderCore.mTtbFlashData.getRotations()[r].getStartingPeriod() + ",");
                    bw.write(tbLoaderCore.mTtbFlashData.getRotations()[r].getHoursAfterLastUpdate() + ","
                        + tbLoaderCore.mTtbFlashData.getRotations()[r].getInitVoltage());
                }

                statsInfo
                    .put("flash_sn", tbLoaderCore.mTtbFlashData.getSerialNumber().toUpperCase())
                    .put("flash_reflashes", tbLoaderCore.mTtbFlashData.getCountReflashes())
                    .put("flash_deployment", tbLoaderCore.mTtbFlashData.getDeploymentNumber().toUpperCase())
                    .put("flash_package", tbLoaderCore.mTtbFlashData.getImageName().toUpperCase())
                    .put("flash_community", tbLoaderCore.mTtbFlashData.getCommunity().toUpperCase())
                    .put("flash_last_updated",
                        tbLoaderCore.mTtbFlashData.getUpdateYear() + "/" + tbLoaderCore.mTtbFlashData.getUpdateMonth() + "/"
                            + tbLoaderCore.mTtbFlashData.getUpdateDate())
                    .put("flash_cumulative_days", tbLoaderCore.mTtbFlashData.getCumulativeDays())
                    .put("flash_corruption_day", tbLoaderCore.mTtbFlashData.getCorruptionDay())
                    .put("flash_last_initial_v", tbLoaderCore.mTtbFlashData.getLastInitVoltage())
                    .put("flash_powerups", tbLoaderCore.mTtbFlashData.getPowerups())
                    .put("flash_periods", tbLoaderCore.mTtbFlashData.getPeriods())
                    .put("flash_rotations", tbLoaderCore.mTtbFlashData.getProfileTotalRotations())
                    .put("flash_num_messages", tbLoaderCore.mTtbFlashData.getTotalMessages());

                statsInfo
                    .put("flash_total_seconds", totalSecondsPlayed)
                    .put("flash_started", countStarted)
                    .put("flash_one_quarter", countQuarter)
                    .put("flash_half", countHalf)
                    .put("flash_three_quarters", countThreequarters)
                    .put("flash_completed", countCompleted)
                    .put("flash_applied", countApplied)
                    .put("flash_useless", countUseless);

                final String[] N = {"0", "1", "2", "3", "4"};
                for (int r = 0; r < numRotations; r++) {
                    statsInfo
                        .put("flash_seconds_" + N[r], tbLoaderCore.mTtbFlashData.totalPlayedSecondsPerRotation(r))
                        .put("flash_period_" + N[r], tbLoaderCore.mTtbFlashData.getRotations()[r].getStartingPeriod())
                        .put("flash_hours_post_update_" + N[r],
                            tbLoaderCore.mTtbFlashData.getRotations()[r].getHoursAfterLastUpdate())
                        .put("flash_initial_v_" + N[r], tbLoaderCore.mTtbFlashData.getRotations()[r].getInitVoltage());
                }

            }
            statsInfo.put("in_testing", tbLoaderCore.mOldDeploymentInfo.isTestDeployment());

            opLog.put(statsInfo);
            statsLog.put(statsInfo);
            statsLog.put("statsonly", tbLoaderCore.mStatsOnly);

            String inDeploymentUUID = tbLoaderCore.mTbDeviceInfo.getDeploymentUUID();
            if (inDeploymentUUID != null) {
                statsLog.put("deployment_uuid", inDeploymentUUID);
                opLog.put("in_deployment_uuid", inDeploymentUUID);
                deploymentLog.put("prev_deployment_uuid", inDeploymentUUID);
            }
            if (!tbLoaderCore.mStatsOnly) {
                opLog.put("out_deployment_uuid", tbLoaderCore.mDeploymentUUID);
                deploymentLog.put("deployment_uuid", tbLoaderCore.mDeploymentUUID);
            }
            statsLog.put("stats_uuid", tbLoaderCore.mStatsCollectedUUID);
            opLog.put("stats_uuid", tbLoaderCore.mStatsCollectedUUID);

            bw.write("\n");
            bw.flush();
            bw.close();

            InputStream content = new ByteArrayInputStream(baos.toByteArray());
            TbFile.Flags flag = isNewFile ? nil : append;
            opLog.put("append", !isNewFile);

            csvFile.createNew(content, flag);
            content.close();
            baos.close();

            // Copy the k=v .log files next to the .csv file.
            String logSuffix = tbLoaderCore.getOpLogSuffix();
            writeLogDataToFile(opLog, tbLoaderCore.getCollectedOpDataDir().open("tbData" + logSuffix));
            writeLogDataToFile(statsLog, tbLoaderCore.getCollectedOpDataDir().open("statsData" + logSuffix));
            if (!tbLoaderCore.mStatsOnly) {
                writeLogDataToFile(deploymentLog, tbLoaderCore.getCollectedOpDataDir().open("deployments" + logSuffix));
            }

        } catch (Exception e) {
            opLog.put("exception", e);
            e.printStackTrace();
            throw e;
        } finally {
            opLog.finish();
            statsLog.finish();
            if (!tbLoaderCore.mStatsOnly) {
                deploymentLog.finish();
            }
        }
    }
}
