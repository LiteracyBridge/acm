package org.literacybridge.acm.config;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.literacybridge.acm.cloud.Authenticator;
import org.literacybridge.acm.sandbox.Sandbox;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Created by bill on 3/6/17.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ AccessControlTest.class, AccessControl.class, ACMConfiguration.class, PathsProvider.class })
public class AccessControlTest {
    @Rule
    TemporaryFolder folder = new TemporaryFolder();

    // These are created fresh for every test. Populate as needed.
    private File home;  // Like ~/Literacybridge
    private File temp;  // Like ~/Literacybridge/temp
    private File dbx;   // Like ~/Dropbox
    private File acmDir; // Like ~/Dropbox/ACM-NADA
    private File sandboxDir;

    private DBConfiguration getMockDbConfig() throws IOException {
        home = folder.newFolder("home");
        temp = new File(home, "temp");
        temp.mkdirs();
        dbx = folder.newFolder("dbx");
        acmDir = new File(dbx, "ACM-NADA");
        acmDir.mkdirs();
        sandboxDir = folder.newFolder("sandbox");

        AmplioHome testInstance = new AmplioHome() {
            @Override
            protected File getTempDirectory() {
                return temp;
            }
        };
        AmplioHome.getInstance(testInstance);

        PathsProvider ppConfig = new PathsProvider("ACM-NADA", true) {
        };
        Sandbox sandbox = new Sandbox(acmDir, sandboxDir);

        ACMConfiguration mockConfig = PowerMockito.mock(ACMConfiguration.class);
        PowerMockito.mockStatic(ACMConfiguration.class);
        PowerMockito.when(ACMConfiguration.getInstance()).thenReturn(mockConfig);
        PowerMockito.when(mockConfig.getPathProvider("ACM-NADA")).thenReturn(ppConfig);

        DBConfiguration dbConfig = PowerMockito.mock(DBConfiguration.class);
        when(dbConfig.getSandbox()).thenReturn(sandbox);
        when(dbConfig.getPathProvider()).thenReturn(ppConfig);
        when(dbConfig.getProgramHomeDirName()).thenReturn("ACM-NADA");
        when(dbConfig.getProgramHomeDir()).thenReturn(acmDir);
        return dbConfig;
    }

    private void populateAcm() throws IOException {
        File zip = new File(acmDir, "db1.zip");
        new FileOutputStream(zip).close();
    }

    private void mockOffline() {
        PowerMockito.mockStatic(AccessControl.class);
        when(AccessControl.isOnline()).thenReturn(false);
    }

    private void mockOnline() {
        PowerMockito.mockStatic(AccessControl.class);
        when(AccessControl.isOnline()).thenReturn(true);
    }

    @Test
    public void testOfflineNoDb() throws IOException {
        // Offline, and no database .zip file.
        mockOffline();

        DBConfiguration dbConfig = getMockDbConfig();
        AccessControl ac = new AccessControl(dbConfig);

        AccessControlResolver.AccessStatus status = ac.determineAccessStatus();

        assertEquals(AccessControlResolver.AccessStatus.noNetworkNoDbError, status);
    }

    @Test
    public void testOfflineHaveDb() throws IOException {
        // Offline, but with a db1.zip file.
        mockOffline();

        DBConfiguration dbConfig = getMockDbConfig();
        populateAcm();
        AccessControl ac = new AccessControl(dbConfig);

        AccessControlResolver.AccessStatus status = ac.determineAccessStatus();

        assertEquals(AccessControlResolver.AccessStatus.noServer, status);
    }


    @Test
    public void testOnlineNoDb() throws IOException {
        // Online, but no .zip file.
        mockOnline();

        DBConfiguration dbConfig = getMockDbConfig();

        AccessControl ac = new AccessControl(dbConfig);
        AccessControl spy = spy(ac);
        doReturn(true).when(spy).isDbAvailableToCheckout("ACM-NADA");
        // No db*.zip exists; we don't have any database at all.
        doReturn("db2.zip").when(spy).getCurrentZipFilename();

        AccessControlResolver.AccessStatus status = spy.determineAccessStatus();

        assertEquals(AccessControlResolver.AccessStatus.noDbError, status);
    }


    @Test
    public void testOnlineNotAvailable() throws IOException {
        // Online, but database already checked out.
        mockOnline();

        DBConfiguration dbConfig = getMockDbConfig();
        populateAcm();

        AccessControl ac = new AccessControl(dbConfig);
        AccessControl spy = spy(ac);
        doReturn(false).when(spy).isDbAvailableToCheckout("ACM-NADA");

        AccessControlResolver.AccessStatus status = spy.determineAccessStatus();

        assertEquals(AccessControlResolver.AccessStatus.notAvailable, status);
    }

    @Test
    public void testOnlineServerError() throws IOException {
        // Were online, but got error accessing server.
        mockOnline();

        DBConfiguration dbConfig = getMockDbConfig();
        populateAcm();

        AccessControl ac = new AccessControl(dbConfig);
        AccessControl spy = spy(ac);
        doThrow(new IOException("boo!")).when(spy).isDbAvailableToCheckout("ACM-NADA");

        AccessControlResolver.AccessStatus status = spy.determineAccessStatus();

        assertEquals(AccessControlResolver.AccessStatus.noServer, status);
    }

//    @Test
//    public void testOnlineNewDatabase() throws IOException {
//        // Online, brand new database (per the server).
//        mockOnline();
//
//        DBConfiguration dbConfig = getMockDbConfig();
//        populateAcm();
//
//        AccessControl ac = new AccessControl(dbConfig);
//        AccessControl spy = spy(ac);
//        doReturn(true).when(spy).checkOutDB("ACM-NADA", "statusCheck");
//        doReturn("null").when(spy).getCurrentZipFilename();
//        doReturn(false).when(spy).isNewCheckoutRecord();
//
//        AccessControlResolver.AccessStatus status = spy.init();
//
//        assertEquals(status, AccessControlResolver.AccessStatus.newDatabase);
//    }

    @Test
    public void testOnlineOldDatabase() throws IOException {
        // Online, but don't have the latest database downloaded.
        mockOnline();

        DBConfiguration dbConfig = getMockDbConfig();
        populateAcm();

        AccessControl ac = new AccessControl(dbConfig);
        AccessControl spy = spy(ac);
        doReturn(true).when(spy).isDbAvailableToCheckout("ACM-NADA");
        // Only db1.zip exists; we don't seem to have the latest.
        doReturn("db2.zip").when(spy).getCurrentZipFilename();

        AccessControlResolver.AccessStatus status = spy.determineAccessStatus();

        assertEquals(AccessControlResolver.AccessStatus.outdatedDb, status);
    }

    @Test
    public void testOnlineReadOnlyAccess() throws IOException {
        // Online, have up to date database, but read-only user.
        mockOnline();

        DBConfiguration dbConfig = getMockDbConfig();
        populateAcm();
        when(dbConfig.userIsReadOnly()).thenReturn(true);
        Authenticator.setDebugInstance(new Authenticator(){
            @Override
            public boolean hasUpdatingRole() { return false; }
        });

        AccessControl ac = new AccessControl(dbConfig);
        AccessControl spy = spy(ac);
        doReturn(true).when(spy).isDbAvailableToCheckout("ACM-NADA");
        doReturn("db1.zip").when(spy).getCurrentZipFilename();

        AccessControlResolver.AccessStatus status = spy.determineAccessStatus();

        assertEquals(AccessControlResolver.AccessStatus.userReadOnly, status);
    }

    @Test
    public void testOnlineAvailable() throws IOException {
        // Online, have latest database, read-write user.
        mockOnline();

        DBConfiguration dbConfig = getMockDbConfig();
        populateAcm();
        when(dbConfig.userIsReadOnly()).thenReturn(false);
        Authenticator.setDebugInstance(new Authenticator(){
            @Override
            public boolean hasUpdatingRole() { return true; }
        });

        AccessControl ac = new AccessControl(dbConfig);
        AccessControl spy = spy(ac);
        doReturn(true).when(spy).isDbAvailableToCheckout("ACM-NADA");
        doReturn("db1.zip").when(spy).getCurrentZipFilename();

        AccessControlResolver.AccessStatus status = spy.determineAccessStatus();

        assertEquals(AccessControlResolver.AccessStatus.available, status);
    }
}
