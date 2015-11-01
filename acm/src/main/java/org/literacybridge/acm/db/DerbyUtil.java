package org.literacybridge.acm.db;

import java.io.File;

/**
 * @deprecated: We're removing Derby DB from the ACM and are switching to a Lucene index
 *              for storing and searching all metadata.
 */
@Deprecated
public class DerbyUtil {

    // use this file as identification property for the Derby DB root directory
    public final String SERVICE_PROPERTIES = "service.properties";


    public static boolean IsDatabaseStructureValid(String databaseRootDirPath) {

        File databaseRootDir = new File(databaseRootDirPath);
        if (databaseRootDir.exists()) {
            File lbDatabase = new File(databaseRootDirPath + File.separator + "");

        }



        return false;
    }

}
