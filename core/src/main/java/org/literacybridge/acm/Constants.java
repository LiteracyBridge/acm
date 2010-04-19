package org.literacybridge.acm;

import java.io.File;

public class Constants {
    public final static File USER_HOME_DIR = new File(System.getProperty("user.home", "."));
    public final static File LB_SYSTEM_DIR = new File(USER_HOME_DIR, ".literacybridge");
    public final static File DATABASE_DIR = new File(LB_SYSTEM_DIR, ".db");
    public final static File REPOSITORY_DIR = new File(LB_SYSTEM_DIR, "content");
}
