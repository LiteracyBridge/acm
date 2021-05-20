package org.literacybridge.acm.config;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.cloud.Authenticator;
import org.literacybridge.acm.config.AccessControlResolver.AccessStatus;
import org.literacybridge.acm.config.AccessControlResolver.ACCESS_CHOICE;
import org.literacybridge.acm.config.AccessControlResolver.OpenStatus;
import org.literacybridge.core.fs.ZipUnzip;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// TESTING: required for AWS check-out platform

public class AccessControl {

    private static final Logger LOG = Logger.getLogger(AccessControl.class.getName());
    private static final int NUM_ZIP_FILES_TO_KEEP = 4;
    // First db zip name is "db1.zip"
    private final static String DB_ZIP_FILENAME_PREFIX = Constants.DBHomeDir;
    private final static String DB_ZIP_FILENAME_INITIAL = DB_ZIP_FILENAME_PREFIX + "1.zip";
    private final static String DB_ZIP_FILENAME_FORMAT = DB_ZIP_FILENAME_PREFIX + "%d.zip";
    private final static Pattern DB_ZIP_MATCHER = Pattern.compile("(?i)^db([0-9]+)\\.zip$");
    // The php checkout app returns the string "NULL" if no checkin file was found
    private final static String DB_DOES_NOT_EXIST = "NULL";
    private final static String DB_KEY_OVERRIDE = "force";

    protected final DBConfiguration dbConfiguration;
    private final AccessControlResolver resolver;

    AccessStatus accessStatus = AccessStatus.none;
    OpenStatus openStatus = OpenStatus.none;

    private Map<String,String> possessor;
    private DBInfo dbInfo;

    AccessControl(DBConfiguration dbConfiguration) {
        this.dbConfiguration = dbConfiguration;
        this.resolver = AccessControlResolver.getDefault();
    }
    AccessControl(DBConfiguration dbConfiguration, AccessControlResolver resolver) {
        this.dbConfiguration = dbConfiguration;
        this.resolver = resolver;
    }

    private void setPossessor(Map<String,String>  name) {
        possessor = name;
    }

    public Map<String,String>  getPosessor() {
        return new HashMap<>(possessor);
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

    public boolean isSandboxed() {
        return dbConfiguration.isSandboxed();
    }

    public void setSandboxed(boolean isSandboxed) {
        dbConfiguration.setSandboxed(isSandboxed);
    }

    private boolean isSyncFailure() {
        return dbConfiguration.isSyncFailure();
    }

    /**
     * Cleans up the temp directory for this ACM
     */
    private void deleteLocalDB() {
        try {
            // deleting old local DB so that next startup knows everything shutdown
            // normally
            // Like ~/LiteracyBridge/ACM/temp/ACM-CARE
            FileUtils.deleteDirectory(dbConfiguration.getPathProvider().getLocalProgramTempDir());
        } catch (Exception e) {
            System.err.print("Caught exception deleting local db.\n");
            e.printStackTrace();
            // TODO: Notify the user so they have some slim chance of getting around the problem.
        }
    }
    
    /**
     * Do we seem to actually have network connectivity?
     * @return True if we can reach amplio.org.
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
        accessStatus = determineAccessStatus();

        ACCESS_CHOICE choice = resolver.resolveAccessStatus(this, accessStatus);
        if (choice == ACCESS_CHOICE.USE_READONLY) { useSandbox = true; }
        // If a fatal error and interative, terminate.
        if ((accessStatus.isFatal() || (accessStatus.isOkWithSandbox() && !useSandbox) )
                && !ACMConfiguration.getInstance().isDisableUI()) {
            stackTraceExit(accessStatus);
        }

        if (accessStatus.isAlwaysOk() || accessStatus.isOkWithSandbox() && useSandbox) {
            openStatus = open(useSandbox);
            resolver.resolveOpenStatus(this, openStatus);
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
    public AccessStatus determineAccessStatus() {
        AccessStatus status;
        try {
            AcmLocker.lockDb(dbConfiguration);
        } catch (AcmLocker.MultipleInstanceException e) {
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
            status = AccessStatus.checkedOut;
        } else {
            deleteLocalDB();
            status = determineRWStatus();
        }

        return status;
    }

    /**
     * Attempts to open the database.
     *
     * @param useSandbox If true, changes will not be saved.
     * @return The OpenStatus.
     */
    OpenStatus open(boolean useSandbox) {
        OpenStatus status;
        if (!AcmLocker.isLocked() || dbInfo == null) {
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
        case syncFailure:
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
            dbInfo.setCheckoutKey(DB_KEY_OVERRIDE);
            dbInfo.setNewCheckoutRecord();
            break;
        case available:
            break;
        }

        if (dbInfo.isCheckedOut()) {
            status = OpenStatus.reopened;
        } else if (useSandbox) {
            status = OpenStatus.openedSandboxed;
        } else if (accessStatus == AccessStatus.newDatabase) {
            status = OpenStatus.newDatabase;
        } else {
            // Try to check out on server.
            boolean dbAvailable;
            try {
                dbAvailable = checkOutDB(dbConfiguration.getProgramHomeDirName());
                status = dbAvailable ? OpenStatus.opened : OpenStatus.notAvailableError;
            } catch (IOException e) {
                status = OpenStatus.serverError;
            }
        }
        setSandboxed(useSandbox);

        // If we're able to open the database, create mirror if necessary, set up the repository.
        if (status.isOpen()) {
            // If newly checked out, create the db mirror.
            if (status != OpenStatus.reopened) {
                // If we successfully called checkOutDB, the zip file name has been set. If we didn't
                // make the call (reopened, openedSandboxed, newDatabase), or if the call failed
                // (notAvailableError, serverError), then the name has not been set. Only if the
                // status is (opened) will the name have been set. So, if needed, set it now from
                // the latest timestamp.
                if (status != OpenStatus.opened) {
                    assert getCurrentZipFilename() == null : "Expected no zip file name.";
                    setNewestModifiedZipFileAsCurrent();
                }
                createDBMirror();
                // Is this newly created, as far as server knows?
                if (!useSandbox && (getCurrentZipFilename()==null || getCurrentZipFilename().equalsIgnoreCase(DB_DOES_NOT_EXIST))) {
                    dbInfo.setCheckoutKey(DB_KEY_OVERRIDE);
                    dbInfo.setNewCheckoutRecord();
                }
            }
        }
        return status;
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
        } else if (isSyncFailure()) {
            return AccessStatus.syncFailure;
        }

        try {
            boolean dbAvailable = isDbAvailableToCheckout(dbConfiguration.getProgramHomeDirName());
            if (!dbAvailable) {
                return AccessStatus.notAvailable;
            }
        } catch (IOException e) {
            // No server. This can still be successful, in sandbox mode.
            return AccessStatus.noServer;
        }

        if (dbInfo.isNewCheckoutRecord()) {
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
        if (dbConfiguration.userIsReadOnly()) {
            // User has RO access. This can still be successful, in sandbox mode.
            return AccessStatus.userReadOnly;
        }

        return AccessStatus.available;
    }

    AccessControlResolver.UpdateDbStatus commitDbChanges() {
        AccessControlResolver.UpdateDbStatus status = AccessControlResolver.UpdateDbStatus.ok;
        String dbName = dbConfiguration.getProgramHomeDirName();
        String filename = null;

        filename = saveDbFromMirror();
        if (filename == null) {
            // If we couldn't save the file, ask the user whether to keep (and try later) or discard changes.
            status = AccessControlResolver.UpdateDbStatus.zipError;
            AccessControlResolver.UPDATE_CHOICE choice = resolver.resolveUpdateStatus(this, status);
            if (choice == AccessControlResolver.UPDATE_CHOICE.DELETE) {
                return discardDbChanges();
            }
        }
        if (status == AccessControlResolver.UpdateDbStatus.ok) {
            try {
                if (!checkInDB(dbName, filename))
                    status = AccessControlResolver.UpdateDbStatus.denied;
            } catch (IOException ex) {
                status = AccessControlResolver.UpdateDbStatus.networkError;
            }

            AccessControlResolver.UPDATE_CHOICE choice = resolver.resolveUpdateStatus(this, status);

            if (status == AccessControlResolver.UpdateDbStatus.ok) {
                // We saved the .zip OK, and updated server status OK. Safe to clean up.
                deleteOldZipFiles(NUM_ZIP_FILES_TO_KEEP);
                dbInfo.deleteCheckoutFile();
                deleteLocalDB();
                accessStatus = AccessStatus.none;
                openStatus = OpenStatus.none;
            }
        }
        return status;
    }

    AccessControlResolver.UpdateDbStatus discardDbChanges() {
        AccessControlResolver.UpdateDbStatus status = AccessControlResolver.UpdateDbStatus.ok;
        String dbName = dbConfiguration.getProgramHomeDirName();

        boolean checkedInOk;
        try {
            if (!discardCheckout(dbName))
                status = AccessControlResolver.UpdateDbStatus.denied;
        } catch (IOException ex) {
            status = AccessControlResolver.UpdateDbStatus.networkError;
        }

        AccessControlResolver.UPDATE_CHOICE choice = resolver.resolveUpdateStatus(this, status);

        if (status == AccessControlResolver.UpdateDbStatus.ok) {
            dbInfo.deleteCheckoutFile();
            deleteLocalDB();
            accessStatus = AccessStatus.none;
            openStatus = OpenStatus.none;
        }
        return status;
    }

    /**
     * Checks the server to see if the given db is available to check out.
     * @param db the name of the database, "ACM-LBG-COVID-19" or "UNICEF-GH_CHPS". Has an ACM- prefix if the
     *           program directory has an ACM- prefix. (Or, has an ACM- prefix if the database is in Dropbox,
     *           and not if the database is in S3.)
     * @return True if the database is available, false otherwise.
     * @throws IOException if there is a network error.
     */
    boolean isDbAvailableToCheckout(String db) throws IOException {
        return checkOutDbHelper(db, "statusCheck");
    }

    /**
     * Attempts to check out the given database.
     * @param db the name of the database, "ACM-LBG-COVID-19" or "UNICEF-GH_CHPS". Has an ACM- prefix if the
     *           program directory has an ACM- prefix. (Or, has an ACM- prefix if the database is in Dropbox,
     *           and not if the database is in S3.)
     * @return True if the database was successfully checked out, false otherwise.
     * @throws IOException if there is a network error.
     */
    boolean checkOutDB(String db) throws IOException {
        return checkOutDbHelper(db, "checkout");
    }

    private boolean checkOutDbHelper(String db, String action) throws IOException {
        Authenticator authenticator = Authenticator.getInstance();
        Authenticator.AwsInterface awsInterface = authenticator.getAwsInterface();
        String computerName;
        boolean statusOk = false;
        boolean nodb = false;
        String currentZipFilename = null, checkoutKey = null;

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

        StringBuilder requestUrl = new StringBuilder(Authenticator.ACCESS_CONTROL_API);
        requestUrl.append("/acm");
        requestUrl.append('/').append(action);
        requestUrl.append('/').append(db);
        requestUrl.append("?version=").append(Constants.ACM_VERSION);
        requestUrl.append("&name=").append(authenticator.getUserEmail());
        requestUrl.append("&contact=").append(authenticator.getUserProperty("phone_number", ""));
        requestUrl.append("&computername=").append(computerName);

        JSONObject jsonResponse = awsInterface.authenticatedGetCall(requestUrl.toString());
        if (jsonResponse == null) {
            throw new IOException("Can't reach network");
        }
        LOG.info(String.format("%s: %s\n          %s\n", action, requestUrl.toString(), jsonResponse.toString()));

        // parse response
        Map<String,String> posessor = new HashMap<>();
        Object o = jsonResponse.get("status");
        if (o instanceof String) {
            String str = (String)o;
            if (str.equalsIgnoreCase("ok")) {
                statusOk = true;
            } else if (str.equalsIgnoreCase("nodb")) {
                statusOk = true;
                nodb = true;
            }
        }
        o = jsonResponse.get("state");
        if (o instanceof JSONObject) {
            JSONObject state = (JSONObject)o;
            //        acm_comment String:	Created ACM
            //        acm_name String:	ACM-LBG-COVID19
            //        acm_state String:	CHECKED_OUT
            //        last_in_comment Null:	true
            //        last_in_contact String:	425-830-4327
            //        last_in_date String:	2020-08-19 16:24:24.837178
            //        last_in_file_name String:	db57.zip
            //        last_in_name String:	bill
            //        last_in_version String:	c202002160
            //        now_out_comment Null:	true
            //        now_out_computername String:	DESKTOP-0NGHQ8K
            //        now_out_contact String:	0203839826
            //        now_out_date String:	2020-11-17 08:36:03.208980
            //        now_out_key String:	1190441
            //        now_out_name String:	Fidelis
            //        now_out_version String:   r2011111
            o = state.get("last_in_file_name");
            if (o instanceof String) {
                currentZipFilename = (String)o;
            }
            o = state.get("now_out_name");
            if (o instanceof String) {
                posessor.put("openby", (String)o);
            }
            o = state.get("now_out_date");
            if (o instanceof String) {
                posessor.put("opendate", (String)o);
            }
            o = state.get("now_out_computername");
            if (o instanceof String) {
                posessor.put("computername", (String)o);
            }
        }
        
        o = jsonResponse.get("key");
        if (o instanceof String) {
            checkoutKey = (String)o;
        }
        o = jsonResponse.get("filename");
        if (o instanceof String) {
            currentZipFilename = (String)o;
        }

        o = jsonResponse.get("openby");
        if (o instanceof String) {
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

        if (currentZipFilename != null)
            setZipFilenames(currentZipFilename);
        if (nodb) {
            dbInfo.setNewCheckoutRecord();
            // This hack is because the php implementation used to return the text "null" when there was no checkout
            // for the database.
            setZipFilenames(DB_DOES_NOT_EXIST);
        }
        if (statusOk) {
            if (checkoutKey != null) {
                dbInfo.setCheckoutKey(checkoutKey);
                dbInfo.setCheckedOut();
            }
        } else if (posessor.size() != 0) {
            setPossessor(posessor);
        }
        return statusOk;

    }

    /**
     * Discards a checkout. The checkout record is released on the server.
     * @param acmName the name of the database, "ACM-LBG-COVID-19" or "UNICEF-GH_CHPS". Has an ACM- prefix if the
     *           program directory has an ACM- prefix. (Or, has an ACM- prefix if the database is in Dropbox,
     *           and not if the database is in S3.)
     * @return True if the checkin was released OK, false otherwise.
     * @throws IOException if the server can't be reached.
     */
    private boolean discardCheckout(String acmName) throws IOException {
        return checkInDB(acmName, null);
    }

    /**
     * Attempts to check in the database filename, on server. The server will match the
     * provided key against the server's saved version of the key for the ACM. A null filename
     * means that the checkout is simply being discarded.
     *
     * @param acmName       The ACM name, like "ACM-DEMO".
     * @param filename The name of the file, dbNN.zip, or null to discard checkout
     * @return true if
     * @throws IOException if server is inaccessible
     */
    private boolean checkInDB(String acmName, String filename) throws IOException {
        Authenticator authenticator = Authenticator.getInstance();
        Authenticator.AwsInterface awsInterface = authenticator.getAwsInterface();
        String computerName;
        String action;
        String key = dbInfo.getCheckoutKey();

        if (ACMConfiguration.getInstance().isNoDbCheckout()) {
            return true;
        }

        // for AWS parallel integration tests
        boolean status_aws = false;

        if (dbInfo.isNewCheckoutRecord()) {
            action = "create";
        } else if (filename == null) {
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

        StringBuilder requestUrl = new StringBuilder(Authenticator.ACCESS_CONTROL_API);
        requestUrl.append("/acm");
        requestUrl.append('/').append(action);
        requestUrl.append('/').append(acmName);
        requestUrl.append("?version=").append(Constants.ACM_VERSION);
        requestUrl.append("&filename=").append(filename);
        requestUrl.append("&key=").append(dbInfo.getCheckoutKey());
        requestUrl.append("&name=").append(authenticator.getUserEmail());
        requestUrl.append("&contact=").append(authenticator.getUserProperty("phone_number", ""));
        requestUrl.append("&computername=").append(computerName);

        JSONObject jsonResponse = awsInterface.authenticatedGetCall(requestUrl.toString());
        if (jsonResponse == null) {
            throw new IOException("Can't reach server");
        }
        LOG.info(String.format("%s: %s\n          %s\n", action, requestUrl.toString(), jsonResponse.toString()));

        Object o = jsonResponse.get("status");
        if (o instanceof String) {
            String str = (String) o;
            status_aws = str.equalsIgnoreCase("ok");
        }

        return status_aws;
    }

    /**
     * Expand the latest .zip file into the temporary database directory.
     */
    private void createDBMirror() {
        String zipFileName = getCurrentZipFilename();
        if (zipFileName == null || zipFileName.equals(AccessControl.DB_DOES_NOT_EXIST)) {
            // Nothing to mirror.
            return;
        }
        try {
            File outDirectory = dbConfiguration.getLocalTempDbDir();
            Path inZipPath = new File(dbConfiguration.getProgramHomeDir(), zipFileName).toPath();
            File inSbFile = dbConfiguration.getSandbox().inputFile(inZipPath);
            Calendar cal = Calendar.getInstance();
            LOG.info(String.format("Started DB Mirror: %2d:%02d.%03d\n",
                    cal.get(Calendar.MINUTE),
                    cal.get(Calendar.SECOND),
                    cal.get(Calendar.MILLISECOND)));
            ZipUnzip.unzip(inSbFile, outDirectory);
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
            Path outZipPath = new File(dbConfiguration.getProgramHomeDir(), filename).toPath();
            File outSbFile = dbConfiguration.getSandbox().outputFile(outZipPath);
            File inDirectory = dbConfiguration.getLocalTempDbDir();
            ZipUnzip.zip(inDirectory, outSbFile);
        } catch (IOException ex) {
            return null;
        }
        return filename;
    }

    private List<File> findZipFiles() {
        Collection<Path> homeDirPaths = dbConfiguration.getSandbox().listPaths(dbConfiguration.getProgramHomeDir().toPath());
        List<File> homeDirFiles = homeDirPaths.stream()
            .map(Path::toFile)
            .collect(Collectors.toList());
        List<File> zipFiles = homeDirFiles.stream()
            .filter(f -> DB_ZIP_MATCHER.matcher(f.getName().toLowerCase()).matches())
            .collect(Collectors.toList());
        if (zipFiles.size() == 0) {
            for (File file : homeDirFiles) {
                if (DB_ZIP_MATCHER.matcher(file.getName().toLowerCase()).matches()) {
                    zipFiles.add(file);
                }
            }
            if (zipFiles.size() != 0) {
                System.err.println("Getting list of zip files via filter failed; fall back to ordinary loop.");
            }
        }
        return zipFiles;
    }

    /**
     * Helper to delete old .zip files from the ACM- directory.
     *
     * @param numFilesToKeep Number of files to leave in ACM- directory.
     */
    @SuppressWarnings("SameParameterValue")
    private void deleteOldZipFiles(int numFilesToKeep) {
        List<File> zipFiles = findZipFiles();

        // sort files from old to new
        zipFiles.sort(Comparator.comparingInt(file -> {
                Matcher m = DB_ZIP_MATCHER.matcher(file.getName());
                if (m.matches()) {
                    String dbNumber = m.group(1);
                    return Integer.parseInt(dbNumber);
                }
                return -1;
            }
        ));

        int numToDelete = zipFiles.size() - numFilesToKeep;
        for (int i = 0; i < numToDelete; i++) {
            dbConfiguration.getSandbox().delete(zipFiles.get(i));
        }
    }

    /**
     * Searches the ACM-XYZ directory for the latest modified .zip file. If one is found
     * that is set as the current file.
     *
     * @return The File with the newest lastModified() property.
     */
    private File findNewestModifiedZipFile() {
        List<File> zipFiles = findZipFiles();
        return zipFiles.stream().max(Comparator.comparingLong(File::lastModified)).orElse(null);
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

        File fileShouldHave = new File(dbConfiguration.getProgramHomeDir(), filenameShouldHave);
        return dbConfiguration.getSandbox().exists(fileShouldHave.toPath());
    }

    /**
     * Helper to print a stack trace and exit.
     *
     * @param rc the return code.
     */
    private void stackTraceExit(AccessStatus rc) {
        System.err.printf("AccessStatus: %s\n", rc.toString());
        new Throwable().printStackTrace();
        System.exit(1);
    }
}
