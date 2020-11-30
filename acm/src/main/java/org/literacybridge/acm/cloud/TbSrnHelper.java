package org.literacybridge.acm.cloud;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.core.tbloader.TbSrnAllocationInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.literacybridge.core.tbloader.TbSrnAllocationInfo.*;

public class TbSrnHelper {
    private static final String TBL_INFO_NAME = "tbsrnstore.info";
    private static final File tblInfoFile = new File(ACMConfiguration.getInstance()
        .getApplicationHomeDirectory(), TBL_INFO_NAME);
    private static final File tblInfoFileNew = new File(ACMConfiguration.getInstance()
        .getApplicationHomeDirectory(), TBL_INFO_NAME+".new");

    private final Authenticator authInstance = Authenticator.getInstance();

    private final String usersEmail;
    private String currentEmail;
    private Properties tbSrnStore;
    private TbSrnAllocationInfo tbSrnAllocationInfo;

    private int tbSrnAllocationSize;

    /**
     * Creates an SRN helper, for the given user, otherwise for user with the "best" SRN
     * allocation, where "best" means most available SRNs.
     * @param usersEmail for whom to use saved SRN allocations.
     */
    TbSrnHelper(String usersEmail) {
        this.usersEmail = usersEmail;
        currentEmail = usersEmail;
        tbSrnStore = loadPropertiesFile();
        tbSrnAllocationInfo = loadSrnInfoForEmail(currentEmail);
        tbSrnAllocationSize = ACMConfiguration.getInstance().getTbSrnAllocationSize();
    }

    private static class AllocTracker {
        int primaryBegin = -1;
        int primaryEnd = -1;
        int backupBegin = -1;
        int backupEnd = -1;
        int nextSrn = -1;
        AllocTracker add(String value, int n) {
            switch (value) {
                case "primarybegin":
                    primaryBegin = n;
                    break;
                case "primaryend":
                    primaryEnd = n;
                    break;
                case "backupbegin":
                    backupBegin = n;
                    break;
                case "backupend":
                    backupEnd = n;
                    break;
                case "nextsrn":
                    nextSrn = n;
                    break;
            }
            return this;
        }
        boolean isComplete() {
            return primaryBegin > 0 &&
                nextSrn >= primaryBegin &&
                primaryEnd > nextSrn &&
                backupBegin >= primaryEnd &&
                backupEnd > backupBegin;
        }
        int size() {
            if (!isComplete()) return 0;
            return primaryEnd - nextSrn - 1 + backupEnd - backupBegin - 1;
        }
    }

    /**
     * Finds the email address, if any, of the user with the largest available SRN allocation.
     * @return the email address, or null if no email address has available allocations.
     */
    private String findBestSrnAllocationEmail() {
        // me@example.com=123
        Pattern emailPattern = Pattern.compile("([\\w.-]+@[\\w.-]+)");
        // 123.primarybegin=234
        Pattern allocPattern = Pattern.compile("(\\d+)\\.(primarybegin|primaryend|backupbegin|backupend|nextsrn)");
        if (tbSrnStore == null) return null;
        Map<Integer, String> emails = new HashMap<>();  // note email and their ids here
        Map<Integer, AllocTracker> sizes = new HashMap<>(); // accumulate counts for ids here
        // Look at all of the properties.
        tbSrnStore.forEach((k, v) -> {
            String key = k.toString();
            String value = v.toString();
            // If this is (begin|end)(begin|end), count it.
            Matcher matcher = allocPattern.matcher(key);
            if (matcher.matches()) {
                int id = Integer.parseInt(matcher.group(1));
                AllocTracker tracker = sizes.getOrDefault(id, new AllocTracker())
                    .add(matcher.group(2), Integer.parseInt(value));
                sizes.putIfAbsent(id, tracker);
            } else {
                // If this is email=id, remember it.
                matcher = emailPattern.matcher(key);
                if (matcher.matches()) {
                    emails.put(Integer.parseInt(value), key);
                }
            }
        });
        // Find the id with the largest allocation
        int maxSize = 0;
        int idOfMax = 0;
        for (Map.Entry<Integer, AllocTracker> entry : sizes.entrySet()) {
            int size = entry.getValue().size();
            if (size > maxSize) {
                maxSize = size;
                idOfMax = entry.getKey();
            }
        }
        if (idOfMax > 0) {
            return emails.get(idOfMax);
        }
        return null;
    }

    /**
     * Tells when the srns are from some other email address.
     * @return true if being used by a different email address.
     */
    public boolean isBorrowedId() {
        return !usersEmail.equals(currentEmail);
    }

    /**
     * Prepare to allocate TB SRNs. Things to check:
     * - if we have no local tbLoaderId, we have no local block(s) of allocated SRNs, and we
     *   need to try to get two blocks.
     * - if we have a local tbLoaderId, get the associated tblInfo. If there is no primary
     *   block, try to promote the backup to primary.
     * - if we have no primary or backup block, try to allocate whichever is missing.
     * - if we changed anything, persist the properties file
     *
     * @return the number of available tb srns.
     */
    @SuppressWarnings("UnusedReturnValue")
    public int prepareForAllocation() {
        if (tbSrnAllocationInfo == null || tbSrnAllocationInfo.getPrimaryBegin() == 0 || tbSrnAllocationInfo.getBackupBegin() == 0) {
            int nBlocks = (tbSrnAllocationInfo ==null || (
                tbSrnAllocationInfo.getPrimaryBegin() ==0 && tbSrnAllocationInfo.getBackupBegin() ==0)) ? 2 : 1;
            TbSrnAllocationInfo newTbSrnAllocationInfo = new TbSrnAllocationInfo(tbSrnAllocationInfo);
            // We need at least one block of numbers.
            if (authInstance.isAuthenticated() && authInstance.isOnline()) {
                Map<String,Object> srnAllocation = allocateTbSrnBlock(tbSrnAllocationSize * nBlocks);
                if (applyReservation(newTbSrnAllocationInfo, srnAllocation)) {
                    // We've successfully allocated a new block of SRNs. Try to persist it.
                    Properties newTbSrnStore = saveSrnInfo(newTbSrnAllocationInfo);
                    if (storePropertiesFile(newTbSrnStore)) {
                        // Successfully persisted, so publish to the rest of the app.
                        this.tbSrnAllocationInfo = newTbSrnAllocationInfo;
                        this.tbSrnStore = newTbSrnStore;
                    }
                } // begin > 0 && end > begin
            } else {
                // The user doesn't have allocations, and we're not authenticated online. The only
                // chance for allocations is if there is another user who has an allocation block
                // on this computer.
                currentEmail = findBestSrnAllocationEmail();
                tbSrnAllocationInfo = loadSrnInfoForEmail(currentEmail);
            }
        }
        if (tbSrnAllocationInfo == null) {
            System.out.println("No TB SRN allocation possible.");
        } else {
            System.out.printf("TB SRN info, id: %04x, avail: %d, next: %04x(%d), primary: %d-%d, backup: %d-%d\n",
                tbSrnAllocationInfo.getTbloaderid(), tbSrnAllocationInfo.available(),
                tbSrnAllocationInfo.getNextSrn(), tbSrnAllocationInfo.getNextSrn(),
                tbSrnAllocationInfo.getPrimaryBegin(), tbSrnAllocationInfo.getPrimaryEnd(),
                tbSrnAllocationInfo.getBackupBegin(), tbSrnAllocationInfo.getBackupEnd());
        }

        return tbSrnAllocationInfo != null ? tbSrnAllocationInfo.available() : 0;
    }

    /**
     * Query whether an SRN can be allocated.
     * @return true if there is any available SRN.
     */
    @SuppressWarnings("unused")
    public boolean hasAvailableSrn() {
        return tbSrnAllocationInfo != null && tbSrnAllocationInfo.hasNext();
    }

    /**
     * Allocate the next SRN, if possible. The TbSrnInfo must be successfully written
     * to disk first.
     * @return the next SRN, or 0 if none is available.
     */
    public int allocateNextSrn() {
        int allocated = 0;
        if (tbSrnAllocationInfo != null && tbSrnAllocationInfo.hasNext()) {
            TbSrnAllocationInfo newTbSrnAllocationInfo = new TbSrnAllocationInfo(tbSrnAllocationInfo);
            int next = newTbSrnAllocationInfo.allocateNext();
            // If we don't have a backup block, and we're authenticated & online, try to get one now.
            if (!newTbSrnAllocationInfo.hasBackup() && authInstance.isAuthenticated()
                    && authInstance.isOnline() && !isBorrowedId()) {
                Map<String, Object> srnAllocation = allocateTbSrnBlock(tbSrnAllocationSize);
                applyReservation(newTbSrnAllocationInfo, srnAllocation);
            }
            // Persist to disk before we return to caller.
            Properties newTbLoaderInfo = saveSrnInfo(newTbSrnAllocationInfo);
            if (storePropertiesFile(newTbLoaderInfo)) {
                System.out.printf("Allocated TB SRN, id: %04x, avail: %d, srn: %04x, next: %04x, primary: %d-%d, backup: %d-%d\n",
                    newTbSrnAllocationInfo.getTbloaderid(), newTbSrnAllocationInfo.available(),
                    next, newTbSrnAllocationInfo.getNextSrn(),
                    newTbSrnAllocationInfo.getPrimaryBegin(), newTbSrnAllocationInfo.getPrimaryEnd(),
                    newTbSrnAllocationInfo.getBackupBegin(), newTbSrnAllocationInfo.getBackupEnd());
                tbSrnAllocationInfo = newTbSrnAllocationInfo;
                tbSrnStore = newTbLoaderInfo;
                allocated = next;
            }
        }
        return allocated;
    }

    public TbSrnAllocationInfo getTbSrnAllocationInfo() {
        return new TbSrnAllocationInfo(this.tbSrnAllocationInfo);
    }

    /**
     * Extract, for the current user, the TbSrnInfo from the TbSrnStore (ie, from the
     * properties file).
     * @param email for which to get SrnInfo.
     * @return the TbSrnInfo for the current user, or null if there is none or it can't be read.
     */
    public TbSrnAllocationInfo loadSrnInfoForEmail(String email) {
        TbSrnAllocationInfo tbSrnAllocationInfo;
        if (tbSrnStore == null) return null;
        int tbLoaderId = getTbLoaderIdForEmail(email);
        if (tbLoaderId <= 0) return null;
        String prefix = String.format("%d.", tbLoaderId);

        int tbloaderid = Integer.parseInt(tbSrnStore.getProperty(prefix + TB_SRN_ID_NAME));
        String tbloaderidHex = tbSrnStore.getProperty(prefix + TB_SRN_HEXID_NAME);
        int nextSrn = Integer.parseInt(tbSrnStore.getProperty(prefix + TB_SRN_NEXTSRN_NAME));
        int primaryBase = Integer.parseInt(tbSrnStore.getProperty(prefix + TB_SRN_PRIMARY_BEGIN_NAME));
        int primaryEnd = Integer.parseInt(tbSrnStore.getProperty(prefix + TB_SRN_PRIMARY_END_NAME));
        int backupBase = Integer.parseInt(tbSrnStore.getProperty(prefix + TB_SRN_BACKUP_BEGIN_NAME));
        int backupEnd = Integer.parseInt(tbSrnStore.getProperty(prefix + TB_SRN_BACKUP_END_NAME));

        assert(String.format("%04x", tbloaderid).equalsIgnoreCase(tbloaderidHex));
        assert(nextSrn >= primaryBase && nextSrn < primaryEnd);
        assert((backupBase < backupEnd) || (backupBase == 0 && backupEnd == 0));

        tbSrnAllocationInfo = new TbSrnAllocationInfo(tbloaderid,
            tbloaderidHex,
            nextSrn,
            primaryBase,
            primaryEnd,
            backupBase,
            backupEnd);
        return tbSrnAllocationInfo;
    }

    /**
     * Saves the given TbSrnInfo into a clone of the TbSrnStore. This lets us save the updated
     * properties to disk before exposing them to the application.
     * @param tbSrnAllocationInfo to be saved.
     * @return a new Properties that is a clone of the existing TbSrnStore updated with the TbSrnInfo.
     */
    private Properties saveSrnInfo(TbSrnAllocationInfo tbSrnAllocationInfo) {
        Properties newTbLoaderInfo = this.tbSrnStore == null ? new Properties() : (Properties)this.tbSrnStore
            .clone();
        
        int tbLoaderId = tbSrnAllocationInfo.getTbloaderid();
        String prefix = String.format("%d.", tbLoaderId);

        // Associate the email address with the tbloader id
        newTbLoaderInfo.setProperty(usersEmail, String.valueOf(tbLoaderId));

        newTbLoaderInfo.setProperty(prefix + TB_SRN_ID_NAME, String.valueOf(tbSrnAllocationInfo.getTbloaderid()));
        newTbLoaderInfo.setProperty(prefix + TB_SRN_HEXID_NAME, tbSrnAllocationInfo.getTbloaderidHex());
        newTbLoaderInfo.setProperty(prefix + TB_SRN_NEXTSRN_NAME, String.valueOf(tbSrnAllocationInfo.getNextSrn()));
        newTbLoaderInfo.setProperty(prefix + TB_SRN_PRIMARY_BEGIN_NAME, String.valueOf(tbSrnAllocationInfo.getPrimaryBegin()));
        newTbLoaderInfo.setProperty(prefix + TB_SRN_PRIMARY_END_NAME, String.valueOf(tbSrnAllocationInfo.getPrimaryEnd()));
        newTbLoaderInfo.setProperty(prefix + TB_SRN_BACKUP_BEGIN_NAME, String.valueOf(tbSrnAllocationInfo.getBackupBegin()));
        newTbLoaderInfo.setProperty(prefix + TB_SRN_BACKUP_END_NAME, String.valueOf(tbSrnAllocationInfo.getBackupEnd()));
        
        return newTbLoaderInfo;
    }

    /**
     * Gets the TB-Loader ID for the current user (by email).
     * @param email for which to get SrnInfo.
     * @return the TB-Loader id, or -1 if none or can't be read.
     */
    private int getTbLoaderIdForEmail(String email) {
        int id = -1;
        if (tbSrnStore != null && StringUtils.isNotEmpty(email)) {
            String tbloaderId = tbSrnStore.getProperty(email, "-1");
            try {
                id = Integer.parseInt(tbloaderId);
            } catch (NumberFormatException ignored) {
                // Ignore and keep -1
            }
        }
        return id;
    }

    /**
     * Loads the TbSrnStore from the properties file.
     * @return the Properties object, or null if none or it can't be read.
     */
    private Properties loadPropertiesFile() {
        Properties tbLoaderInfo = null;
        if (tblInfoFile.exists()) {
            Properties newTbLoaderInfo = new Properties();
            try (FileInputStream fis = new FileInputStream(tblInfoFile);
                InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
                newTbLoaderInfo.load(isr);
                tbLoaderInfo = newTbLoaderInfo;
            } catch (Exception ignored) {
                // ignore and return null
            }
        }
        return tbLoaderInfo;
    }

    /**
     * Store the given properties file to the TBL_INFO_NAME file (known internally as TbSrnStore).
     * @param tbLoaderInfo the Properties to write.
     * @return true if it was successfully saved, false otherwise.
     */
    private boolean storePropertiesFile(Properties tbLoaderInfo) {
        boolean ok = false;
        if (tblInfoFileNew.exists()) {
            //noinspection ResultOfMethodCallIgnored
            tblInfoFileNew.delete();
        }
        try (FileOutputStream fos = new FileOutputStream(tblInfoFileNew);
            OutputStreamWriter osw = new OutputStreamWriter(fos)) {
            tbLoaderInfo.store(osw, null);
            osw.flush();
            osw.close();
            //noinspection ResultOfMethodCallIgnored
            tblInfoFile.delete();
            ok = tblInfoFileNew.renameTo(tblInfoFile);
        } catch (IOException ignored) {
            // Ignore and keep "false"
        }
        return ok;
    }

    /**
     * Make a call to reserve a block of serial numbers.
     * @param n Number of SRNs to request.
     * @return a Map of the returned result.
     */
    private Map<String, Object> allocateTbSrnBlock(int n) {
        Map<String,Object> result = new HashMap<>();

        String baseURL = Authenticator.TBL_HELPER_API;
        String requestURL = baseURL + "/reserve";
        if (n > 0) requestURL += "?n=" + n;

        JSONObject jsonResponse = authInstance.getAwsInterface().authenticatedGetCall(requestURL);

        if (jsonResponse != null) {
            Object o = jsonResponse.get("result");
            if (o instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String,Object> tblMap = (Map<String,Object>) o;
                for (Map.Entry<String,Object> e : tblMap.entrySet())
                    result.put(e.getKey(), e.getValue());
            }
        }

        return result;
    }

    /**
     * Helper function to translate from our local srn allocation object to the actual
     * function implemented by TbSrnAllocationInfo
     * @param tbSrnAllocationInfo the TbSrnAllocationInfo to be updated.
     * @param srnAllocation the allocation with which to update it.
     * @return true if the allocation was well formed, false otherwise.
     */
    public boolean applyReservation(TbSrnAllocationInfo tbSrnAllocationInfo, Map<String,Object> srnAllocation) {
        long begin_l = (Long) srnAllocation.getOrDefault("begin", -1);
        int begin = (int) begin_l;
        long end_l = (Long) srnAllocation.getOrDefault("end", -1);
        int end = (int) end_l;
        long id_l = (Long) srnAllocation.getOrDefault("id", -1);
        int id = (int) id_l;
        String hexid = (String) srnAllocation.getOrDefault("hexid", "");
        System.out.printf("TB SRN allocation, begin: %d, end: %d, id: %04x\n", begin, end, id);
        return tbSrnAllocationInfo.applyReservation(id, hexid, begin, end);
    }


}
