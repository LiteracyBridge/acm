package org.literacybridge.acm;

import java.io.File;

public class Constants {
	public final static String TBDefinitionsHomeDirName		= "TB-definitions";


    public final static File USER_HOME_DIR = new File(System.getProperty("user.home", "."));
    public final static long CACHE_SIZE_IN_BYTES			= 2L * 1024L * 1024L * 1024L; // 2GB
 }
