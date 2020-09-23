package org.literacybridge.acm.tools;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.AccessControl;
import org.literacybridge.acm.gui.CommandLineParams;
import org.literacybridge.acm.repository.AudioItemRepositoryImpl;
import org.literacybridge.acm.utils.LogHelper;

public class AcmCleaner {


    public static void main(String[] args) throws Exception {
        new LogHelper().withName("AcmCleaner.log").initialize();
        int argCount = args.length;
        if (argCount != 1) {
            printUsage();
            System.exit(1);
        }

        String dbName = args[0];

        CommandLineParams params = new CommandLineParams();
        params.disableUI = true;
        params.sandbox = false;

        ACMConfiguration.initialize(params);
        try {
            long timer = -System.currentTimeMillis();
            // We want exclusive write access to the ACM because we may be deleting files. If someone has
            // just added a file, but not saved their work, it'll look like it's abandoned, when it's
            // actually just not committed yet.
            if (!ACMConfiguration.getInstance().setCurrentDB(dbName)) {
                AccessControl.AccessStatus status = ACMConfiguration.getInstance().getCurrentDB().getDbAccessStatus();
                System.err.printf("Can't open db '%s': %s.\n", dbName, status);
                return;
            }
            // Do the work.
            AudioItemRepositoryImpl.CleanResult result = ACMConfiguration.getInstance().getCurrentDB().cleanUnreferencedFiles();
            // This closes the db without saving the database. We only deleted un-referenced files, so we don't need
            // a new database.
            ACMConfiguration.getInstance().getCurrentDB().closeDb();

            timer += System.currentTimeMillis();
            System.out.printf("Cleaned %d bytes in %d files in db '%s', %d ms.\n",
                result.bytes, result.files, dbName, timer);
        } catch (IllegalArgumentException ignored) {
            System.err.printf("Can't clean db '%s'.\n", dbName);
        }
    }

    private static void printUsage() {
        System.err.println("Usage: AcmCleaner {ACM-name}.");
        System.err.println("  Deletes abandoned old audio files in an ACM database.");
    }
}
