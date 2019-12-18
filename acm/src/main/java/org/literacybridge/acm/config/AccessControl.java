package org.literacybridge.acm.config;

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.literacybridge.acm.Constants;
import org.literacybridge.core.fs.ZipUnzip;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

// TESTING: required for AWS check-out platform

public class AccessControl {
    /**
     * These are the status values that can be returned by init()
     */
    public enum AccessStatus {
        none,                           // No status yet
        lockError,                      // Can't lock; ACM already open
        processError,                   // Can't lock, don't know why
        previouslyCheckedOutError,      // Already open this user, can't open with sandbox
        noNetworkNoDbError,             // Can't get to server, and have no local database
        noDbError,                      // No local database. Dropbox problem?
        checkedOut,                     // Already checked out. Just open it.
        newDatabase,                    // New database (on server). Just open it.
        noServer(true),                 // Can open, with sandbox
        outdatedDb(true),               // Can open, with sandbox
        notAvailable(true, true),       // Can open with sandbox, may be outdated
        userReadOnly(true, true),       // Can open with sandbox, may be outdated
        available;                      // Can open it

        private final boolean okWithSandbox;
        private final boolean mayBeOutdated;

        AccessStatus() {
            this(false, false);
        }

        AccessStatus(boolean okWithSandbox) {
            this(okWithSandbox, false);
        }

        AccessStatus(boolean okWithSandbox, boolean mayBeOutdated) {
            this.okWithSandbox = okWithSandbox;
            this.mayBeOutdated = mayBeOutdated;
        }

        public boolean isOkWithSandbox() {
            return okWithSandbox;
        }

        public boolean isMayBeOutdated() {
            return mayBeOutdated;
        }
    }

    /**
     * THese are the status values that can be returned by open() and initDb().
     */
    public enum OpenStatus {
        none,                           // No status yet.
        serverError,                    // Error accessing server.
        notAvailableError,              // Not available to checkout.
        // Open states below here
        opened,                         // Opened for read-write.
        reopened,                       // Opened for read-write; it was already open.
        newDatabase,                    // Brand new database. read-write.
        openedSandboxed;                // Open, but sandboxed.

        public boolean isOpen() {
            return this.ordinal() >= opened.ordinal();
        }
    }

    /**
     * These are the values that can be returned by commitDbChanges().
     */
    public enum UpdateDbStatus {
        ok,                             // Saved locally, checked in status on server.
        denied,                         // Server denied checkin.
        networkError,                   // Can't access server to checkin.
        zipError                        // Error zipping the database (metadata).
    }

    public enum CloseDisposition {
        save,
        discard
    }

    private static final Logger LOG = Logger.getLogger(AccessControl.class.getName());
    private static final int NUM_ZIP_FILES_TO_KEEP = 4;
    // First db zip name is "db1.zip"
    private final static String DB_ZIP_FILENAME_PREFIX = Constants.DBHomeDir;
    private final static String DB_ZIP_FILENAME_INITIAL = DB_ZIP_FILENAME_PREFIX + "1.zip";
    private final static String DB_ZIP_FILENAME_FORMAT = DB_ZIP_FILENAME_PREFIX + "%d.zip";
    // The php checkout app returns the string "NULL" if no checkin file was found
    private final static String DB_DOES_NOT_EXIST = "NULL";
    private final static String DB_KEY_OVERRIDE = "force";

    protected final DBConfiguration dbConfiguration;

    AccessStatus accessStatus = AccessStatus.none;
    OpenStatus openStatus = OpenStatus.none;
    private Map<String,String> possessor;
    private DBInfo dbInfo;

    AccessControl(DBConfiguration dbConfiguration) {
        this.dbConfiguration = dbConfiguration;
    }

    private void setDBKey(String key) {
        dbInfo.setDbKey(key);
    }

    private String getDBKey() {
        return dbInfo.getDbKey();
    }

    private void setAWSKey(String key) {
        dbInfo.setAWSKey(key);
    }

    private String getAWSKey() {
        return dbInfo.getAWSKey();
    }

    private void setPossessor(Map<String,String>  name) {
        possessor = name;
    }

    Map<String,String>  getPosessor() {
        return possessor;
    }

    AccessStatus getAccessStatus() {
        return accessStatus;
    }
    OpenStatus getOpenStatus() {
        return openStatus;
    }

    /**
     * Given a dbNN.zip file name (from Dropbox), determine the NN+1 filename, and store both
     * for later use. If the NN can't be parsed, or there is no filename (ie, null), then
     * store (null,null) for the file names.
     * Due to a quirk of the php checkout processor, if there is no known .zip file name, as with
     * a brand new ACM database, the file name will be the string "NULL". We use this as a flag
     * to mean "brand new database, start with 1".
     * <p>
     * Note that in some circumstances, this method is called with a filename that is not
     * necessarily the latest filename, when we are unable to access the server.
     *
     * @param currentFilename a string like "dbNN.zip", or "NULL"
     */
    private void setZipFilenames(String currentFilename) {
        String nextFilename = null;

        if (currentFilename != null) {
            if (currentFilename.equalsIgnoreCase(DB_DOES_NOT_EXIST)) {
                // ACM does not yet exist, so create name for newly created zip to use on
                // updateDB()
                nextFilename = DB_ZIP_FILENAME_INITIAL;
            } else {
                // Extract NN from dbNN.zip
                String currentFileNumber = currentFilename.substring(
                        DB_ZIP_FILENAME_PREFIX.length(), currentFilename.lastIndexOf('.'));
                try {
                    int nextFileNumber = Integer.parseInt(currentFileNumber) + 1;
                    nextFilename = String.format(DB_ZIP_FILENAME_FORMAT, nextFileNumber);
                } catch (NumberFormatException e) {
                    // there's some strange .zip -- probably a "(conflicted copy)" or
                    // something else weird -- don't use it!
                    LOG.log(Level.WARNING, "Unable to parse filename " + currentFilename);
                    currentFilename = null;
                }
            }
        }
        dbInfo.setFilenames(currentFilename, nextFilename);
    }

    int getCurrentDbVersion() {
        try {
            String currentFilename = getCurrentZipFilename();
            String currentFileNumber = currentFilename.substring(
                DB_ZIP_FILENAME_PREFIX.length(), currentFilename.lastIndexOf('.'));
            return Integer.parseInt(currentFileNumber);
        } catch (Exception e) {
            return -1;
        }
    }

    public String getCurrentZipFilename() {
        return dbInfo.getCurrentFilename();
    }

    private String getNextZipFilename() {
        return dbInfo.getNextFilename();
    }

    private boolean isSandboxed() {
        return dbConfiguration.isSandboxed();
    }

    private void setSandboxed(boolean isSandboxed) {
        dbConfiguration.setSandboxed(isSandboxed);
    }

    /**
     * Cleans up the temp directory for this ACM
     */
    private void deleteLocalDB() {
        try {
            // deleting old local DB so that next startup knows everything shutdown
            // normally
            // Like ~/LiteracyBridge/ACM/temp/ACM-CARE
            File oldDB = new File(dbConfiguration.getTempACMsDirectory(),
                                  dbConfiguration.getSharedACMname());
            FileUtils.deleteDirectory(oldDB);
        } catch (IOException e) {
            e.printStackTrace();
            // TODO: Notify the user so they have some slim chance of getting around the problem.
        }
    }
    
    /**
     * Do we seem to actually have network connectivity?
     * @return True if we can reach literacybridge.org.
     */
    public static boolean isOnline() {
        boolean result = false;
        try {
//            long startTime = System.nanoTime();
            URLConnection connection = new URL("http://amplio.org").openConnection();
            connection.connect();
//            long validatedTime = System.nanoTime();
//            System.out.printf("Online test in %.2f msec\n", (validatedTime-startTime)/1000000.0);
            result = true;
        } catch (MalformedURLException e) {
            // this should not ever happen (if the URL above is good)
            e.printStackTrace();
        } catch (IOException e) {
            // Ignore this exception; means we're not online.
        }
        return result;
    }

    /**
     * Non-interactive version of initDb. Either works or not, with current setting of
     * isForceSandbox() config item.
     */
    public void initDb() {
        boolean useSandbox = ACMConfiguration.getInstance().isForceSandbox();
        accessStatus = init();
        if (accessStatus == AccessStatus.available ||
                accessStatus == AccessStatus.checkedOut ||
                accessStatus == AccessStatus.newDatabase ||
                accessStatus.isOkWithSandbox() && useSandbox) {
            openStatus = open(useSandbox);
            openStatus.isOpen();
        }
    }

    /**
     * Check the status of the database, to see if it can be opened. Based on the result,
     * the caller may be able to open the database, but may need to accept sandbox mode.
     * Or, if the database is already opened for writing, may need to forgo sandbox mode.
     *
     * Remembers the access status.
     *
     * @return An enum giving the status.
     */
    public AccessStatus init() {
        try {
            LockACM.lockDb(dbConfiguration);
        } catch (LockACM.MultipleInstanceException e) {
            String msg = "Can't open ACM";
            if (e.getMessage() != null && e.getMessage().length() > 0) {
                msg = msg + ": " + e.getMessage();
            }
            System.out.println(msg);
            return AccessStatus.lockError;
        } catch (Exception e) {
            String msg = "Can't open ACM";
            if (e.getMessage() != null && e.getMessage().length() > 0) {
                msg = msg + ": " + e.getMessage();
            }
            System.out.println(msg);
            return AccessStatus.processError;
        }

        if (dbInfo == null) {
            dbInfo = new DBInfo(dbConfiguration);
        }
        // Is the db *already* checked out here? (Implication is can't be already in sandbox mode.)
        if (dbInfo.isCheckedOut()) {
            if (ACMConfiguration.getInstance().isForceSandbox()) {
                return AccessStatus.previouslyCheckedOutError;
            }
            accessStatus = AccessStatus.checkedOut;
        } else {
            deleteLocalDB();
            accessStatus = determineRWStatus();
        }

        return accessStatus;
    }

    /**
     * Attempts to open the database.
     *
     * @param useSandbox If true, changes will not be saved.
     * @return The OpenStatus.
     */
    OpenStatus open(boolean useSandbox) {
        OpenStatus openStatus;
        if (!LockACM.isLocked() || dbInfo == null) {
            throw new IllegalStateException("Call to open() without call to init()");
        }

        // Validate that we can open the database.
        switch (accessStatus) {
        // These are just errors -- should not have been called.
        case none:
        case lockError:
        case processError:
        case noNetworkNoDbError:
        case noDbError:
            throw new IllegalStateException("Illegal call to open()");

            // These are OK, provided useSandbox is false
        case previouslyCheckedOutError:
        case checkedOut:
            if (useSandbox) {
                throw new IllegalArgumentException("'useSandbox' mut be false");
            }
            break;

        // These are OK, provided useSandbox is true
        case noServer:
        case outdatedDb:
        case notAvailable:
        case userReadOnly:
            if (!useSandbox) {
                throw new IllegalArgumentException("'useSandbox' mut be true");
            }
            break;

        // Good to go...
        case newDatabase:
            // Sets the key to "force" to force creation of the new record.
            setDBKey(DB_KEY_OVERRIDE);
            break;
        case available:
            break;
        }

        if (dbInfo.isCheckedOut()) {
            openStatus = OpenStatus.reopened;
        } else if (useSandbox) {
            openStatus = OpenStatus.openedSandboxed;
        } else if (accessStatus == AccessStatus.newDatabase) {
            openStatus = OpenStatus.newDatabase;
        } else {
            // Try to check out on server.
            boolean dbAvailable;
            try {
                dbAvailable = checkOutDB(dbConfiguration.getSharedACMname(), "checkout");
                openStatus = dbAvailable ? OpenStatus.opened : OpenStatus.notAvailableError;
            } catch (IOException e) {
                openStatus = OpenStatus.serverError;
            }
        }
        setSandboxed(useSandbox);

        // If we're able to open the database, create mirror if necessary, set up the repository.
        if (openStatus.isOpen()) {
            // If newly checked out, create the db mirror.
            if (openStatus != OpenStatus.reopened) {
                // If we successfully called checkOutDB, the zip file name has been set. If we didn't
                // make the call (reopened, openedSandboxed, newDatabase), or if the call failed
                // (notAvailableError, serverError), then the name has not been set. Only if the
                // status is (opened) will the name have been set. So, if needed, set it now from
                // the latest timestamp.
                if (openStatus != OpenStatus.opened) {
                    assert getCurrentZipFilename() == null : "Expected no zip file name.";
                    setNewestModifiedZipFileAsCurrent();
                }
                createDBMirror();
                // Is this newly created, as far as server knows?
                if (!useSandbox && getCurrentZipFilename().equalsIgnoreCase(DB_DOES_NOT_EXIST)) {
                    setDBKey(DB_KEY_OVERRIDE);
                }
            }
        }
        return openStatus;
    }

    /**
     * Check various status conditions to see if the user can check out the database,
     * and whether they must use sandbox mode to do so.
     *
     * @return A value from AccessStatus enum.
     */
    private AccessStatus determineRWStatus() {

        if (!isOnline()) {
            if (findNewestModifiedZipFile() == null) {
                // Offline, no database available. This is a hard failure.
                return AccessStatus.noNetworkNoDbError;
            } else {
                // Offline. This can still be successful, in sandbox mode.
                return AccessStatus.noServer;
            }
        }

        try {
            boolean dbAvailable = checkOutDB(dbConfiguration.getSharedACMname(), "statusCheck");
            if (!dbAvailable) {
                return AccessStatus.notAvailable;
            }
        } catch (IOException e) {
            // No server. This can still be successful, in sandbox mode.
            return AccessStatus.noServer;
        }

        if (getCurrentZipFilename().equalsIgnoreCase(DB_DOES_NOT_EXIST)) {
            // The database doesn't exist yet. We will create a new database.
            return AccessStatus.newDatabase;
        }

        if (findNewestModifiedZipFile() == null) {
            // No zip file at all -- Dropbox problems? Hard error.
            return AccessStatus.noDbError;
        }
        if (!haveLatestDB()) {
            // Out of date .zip file. This can still be successful, in sandbox mode.
            return AccessStatus.outdatedDb;
        }
        if (!dbConfiguration.userHasWriteAccess(ACMConfiguration.getInstance().getUserName())) {
            // User has RO access. This can still be successful, in sandbox mode.
            return AccessStatus.userReadOnly;
        }

        return AccessStatus.available;
    }

    public void updateDb() {
        throw new IllegalStateException("Only valid in sub-classes");
    }

    UpdateDbStatus commitDbChanges() {
        return commitOrDiscard(CloseDisposition.save);
    }

    UpdateDbStatus discardDbChanges() {
        return commitOrDiscard(CloseDisposition.discard);
    }

    /**
     * This should be called "writeBackDb" or "commitChanges" or something like that.
     * <p>
     * Writes the current temporary database back to a .zip file in the global shared
     * directory, ie, Dropbox.
     *
     * @param disposition 'save' or 'discard'
     * @return UpdateDbStatus.ok if the checkin was performed (or discard was successful),
     * .denied if there was a key mismatch on the server,
     * .networkError if we could not reach the server,
     * .zipError if there was an error zipping up the database
     */
    private UpdateDbStatus commitOrDiscard(CloseDisposition disposition) {
        boolean saveWork = disposition == CloseDisposition.save;
        String filename = null;

        if (saveWork) {
            filename = saveDbFromMirror();
            if (filename == null) {
                return UpdateDbStatus.zipError;
            }
        }

        boolean checkedInOk;
        try {
            checkedInOk = checkInDB(dbConfiguration.getSharedACMname(), filename);
        } catch (IOException ex) {
            return UpdateDbStatus.networkError;
        }

        if (!checkedInOk) {
            return UpdateDbStatus.denied;
        }

        if (saveWork) {
            // We saved the .zip OK, and updated server status OK. Safe to clean up.
            deleteOldZipFiles(NUM_ZIP_FILES_TO_KEEP);
        }

        dbInfo.deleteCheckoutFile();
        accessStatus = AccessStatus.none;
        return UpdateDbStatus.ok;
    }

    @SuppressWarnings("unchecked")
    boolean checkOutDB(String db, String action) throws IOException {
        String computerName;
        boolean status_aws = true;
        String filename_aws = null, key_aws = null, possessor_aws = null;

        // Code for testing. This is 'true' when dropbox is overridden by environment variable,
        // that is, not a real ACM in dropbox.
        if (ACMConfiguration.getInstance().isNoDbCheckout()) {
            if (!setNewestModifiedZipFileAsCurrent()) {
                setZipFilenames(DB_DOES_NOT_EXIST);
            }
            return true;
        }

        try {
            computerName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e1) {
            computerName = "UNKNOWN";
        }

        // send POST request to AWS API gateway to invoke acmCheckOut lambda function
        String requestURL = "https://7z4pu4vzqk.execute-api.us-west-2.amazonaws.com/prod";
        JSONObject request = new JSONObject();

        if (action.equals("checkout")) {
            action = "checkOut";
        }

        request.put("db", db);
        request.put("action", action);
        request.put("name", ACMConfiguration.getInstance().getUserName());
        request.put("contact", ACMConfiguration.getInstance().getUserContact());
        request.put("version", Constants.ACM_VERSION);
        request.put("computername", computerName);
        //request.put("comment", comment);   for possible future use, allows any string input

        HttpUtility httpUtility = new HttpUtility();
        JSONObject jsonResponse;
        try {
            httpUtility.sendPostRequest(requestURL, request);
            jsonResponse = httpUtility.readJSONObject();
            LOG.info(String.format("%s: %s\n          %s\n", action, request.toString(), jsonResponse.toString()));
        } catch (IOException ex) {
            ex.printStackTrace();
            throw ex;
        }
        httpUtility.disconnect();

        // parse response
        Map<String,String> posessor = new HashMap<>();
        Object o = jsonResponse.get("status");
        if (o instanceof String) {
            String str = (String)o;
            status_aws = str.equalsIgnoreCase("ok");
        }
        o = jsonResponse.get("key");
        if (o instanceof String) {
            key_aws = (String)o;
        }
        o = jsonResponse.get("filename");
        if (o instanceof String) {
            filename_aws = (String)o;
        }
        o = jsonResponse.get("openby");
        if (o instanceof String) {
            possessor_aws = (String)o;
            posessor.put("openby", (String)o);
        }
        o = jsonResponse.get("opendate");
        if (o instanceof String) {
            posessor.put("opendate", (String)o);
        }
        o = jsonResponse.get("computername");
        if (o instanceof String) {
            posessor.put("computername", (String)o);
        }

        if (status_aws) {
            if (key_aws != null) {
                setAWSKey(key_aws);
            }
        }

        if (filename_aws != null)
            setZipFilenames(filename_aws);
        if (status_aws) {
            if (key_aws != null) {
                setDBKey(key_aws);
                dbInfo.setCheckedOut(true);
            }
        } else if (posessor.size() != 0) {
            setPossessor(posessor);
        }
        return status_aws;

    }

    /**
     * Attempts to check in the database filename, on server. The server will match the
     * provided key against the server's saved version of the key for the ACM. A null filename
     * means that the checkout is simply being discarded.
     *
     * @param db       The ACM name, like "ACM-DEMO".
     * @param filename The name of the file, dbNN.zip, or null to discard checkout
     * @return true if
     * @throws IOException if server is inaccessible
     */
    @SuppressWarnings("unchecked")
    private boolean checkInDB(String db, String filename) throws IOException {
        String computerName;
        String action;
        String key = getDBKey();

        if (ACMConfiguration.getInstance().isNoDbCheckout()) {
            return true;
        }

        // for AWS parallel integration tests
        boolean status_aws = false;

        if (filename == null) {
            action = "discard";
            filename = "";
        } else {
            action = "checkin";
        }

        try {
            computerName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e1) {
            computerName = "UNKNOWN";
        }

        // AWS check-in, configured in AWS Application Gateway
        String requestURL = "https://7z4pu4vzqk.execute-api.us-west-2.amazonaws.com/prod";
        JSONObject request = new JSONObject();

        if (key != null && key.equals("force")) {
            setAWSKey("new");
        }

        if (action.equals("checkin")) {
            action = "checkIn";
        }

        request.put("db", db);
        request.put("action", action);
        request.put("key", getAWSKey());
        request.put("filename", filename);
        request.put("name", ACMConfiguration.getInstance().getUserName());
        request.put("contact", ACMConfiguration.getInstance().getUserContact());
        request.put("version", Constants.ACM_VERSION);
        request.put("computername", computerName);
        //request.put("comment", comment); for possible future use, allows string input
        
        HttpUtility httpUtility = new HttpUtility();
        JSONObject jsonResponse = null;
        try {
            httpUtility.sendPostRequest(requestURL, request);
            jsonResponse = httpUtility.readJSONObject();
            LOG.info(String.format("%s: %s\n          %s\n", action, request.toString(), jsonResponse.toString()));
        } catch (IOException ex) {
            ex.printStackTrace();
            // Rethrow the exception, so caller knows we can't get to server.
            throw ex;
        }
        httpUtility.disconnect();

        if (jsonResponse != null) {
            Object o = jsonResponse.getOrDefault("status", "");
            if (o instanceof String) {
                String str = (String) o;
                status_aws = str.equalsIgnoreCase("ok");
            }
        }

        return status_aws;
    }

    /**
     * Expand the latest .zip file into the temporary database directory.
     */
    private void createDBMirror() {
        String zipFileName = getCurrentZipFilename();
        if (zipFileName.equals(AccessControl.DB_DOES_NOT_EXIST)) {
            // Nothing to mirror.
            return;
        }
        try {
            File outDirectory = dbConfiguration.getTempDatabaseDirectory();
            File inZipFile = new File(dbConfiguration.getSharedACMDirectory(), zipFileName);
            Calendar cal = Calendar.getInstance();
            LOG.info(String.format("Started DB Mirror: %2d:%02d.%03d\n",
                    cal.get(Calendar.MINUTE),
                    cal.get(Calendar.SECOND),
                    cal.get(Calendar.MILLISECOND)));
            ZipUnzip.unzip(inZipFile, outDirectory);
            cal = Calendar.getInstance();
            LOG.info(String.format("Completed DB Mirror: %2d:%02d.%03d\n",
                cal.get(Calendar.MINUTE),
                cal.get(Calendar.SECOND),
                cal.get(Calendar.MILLISECOND)));
        } catch (Exception e) {
            // Gee, I wonder if it worked? Oh, well, whatever...
            // TODO: this is probably a fatal error.
            e.printStackTrace();
        }
    }

    /**
     * Zip the current contents of the temporary database directory into the
     * previously determined "next" zip file name.
     *
     * @return The name of the new .zip file, if it was created OK, null if any error.
     */
    private String saveDbFromMirror() {
        String filename = null;
        try {
            // The name previously decided for the next zip file name.
            filename = getNextZipFilename();
            File outZipFile = new File(dbConfiguration.getSharedACMDirectory(), filename);
            File inDirectory = dbConfiguration.getTempDatabaseDirectory();
            ZipUnzip.zip(inDirectory, outZipFile);
        } catch (IOException ex) {
            return null;
        }
        return filename;
    }

    /**
     * Helper to delete old .zip files from the ACM- directory.
     *
     * @param numFilesToKeep Number of files to leave in ACM- directory.
     */
    private void deleteOldZipFiles(int numFilesToKeep) {
        List<File> files = Lists.newArrayList(
                dbConfiguration.getSharedACMDirectory().listFiles((dir, name) -> {
                    String lowercase = name.toLowerCase();
                    return lowercase.endsWith(".zip");
                }));

        // sort files from old to new
        files.sort((o1, o2) -> Long.compare(o1.lastModified(), o2.lastModified()));

        int numToDelete = files.size() - numFilesToKeep;
        for (int i = 0; i < numToDelete; i++) {
            files.get(i).delete();
        }
    }

    /**
     * Searches the ACM-XYZ directory for the latest modified .zip file. If one is found
     * that is set as the current file.
     *
     * @return The File with the newest lastModified() property.
     */
    private File findNewestModifiedZipFile() {
        File[] files;
        File latestModifiedFile = null;
        files = dbConfiguration.getSharedACMDirectory().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                String lowercase = name.toLowerCase();
                return lowercase.endsWith(".zip");
            }
        });
        for (File file : files) {
            if (latestModifiedFile == null
                    || file.lastModified() > latestModifiedFile.lastModified()) {
                latestModifiedFile = file;
            }
        }
        return latestModifiedFile;
    }

    /**
     * Searches the ACM-XYZ directory for the latest modified .zip file. If one is found
     * that is set as the current file.
     *
     */
    private boolean setNewestModifiedZipFileAsCurrent() {
        File lastModifiedFile = findNewestModifiedZipFile();

        if (lastModifiedFile != null) {
            setZipFilenames(lastModifiedFile.getName());
            return true;
        }
        return false;
    }

    /**
     * Do we have the latest db .zip file locally?
     *
     * @return True if we have it, false if we don't or don't know.
     */
    private boolean haveLatestDB() {
        String filenameShouldHave = getCurrentZipFilename();

        if (filenameShouldHave == null) {
            return false;
        }

        if (filenameShouldHave.equalsIgnoreCase(AccessControl.DB_DOES_NOT_EXIST))
            return true; // if the ACM is new, you have the latest there is (nothing)

        File fileShouldHave = new File(dbConfiguration.getSharedACMDirectory(), filenameShouldHave);
        return fileShouldHave.exists();
    }

}
