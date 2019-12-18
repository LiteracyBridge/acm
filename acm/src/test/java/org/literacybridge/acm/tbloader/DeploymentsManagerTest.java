package org.literacybridge.acm.tbloader;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.core.tbloader.TBLoaderConstants;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ DeploymentsManager.class, ACMConfiguration.class })
public class DeploymentsManagerTest {
    @Rule
    TemporaryFolder folder = new TemporaryFolder();

    private static final String PROJECT_NAME = "NADA";
    private static final String ACM_NAME = "ACM-" + PROJECT_NAME;


    // These are created fresh for every test. Populate as needed.
    private File localProjectDir;
    private File localContentDir;
    private File globalPublishedDir;

    private void mockAcmConfig() throws IOException {
        // Like ~/Literacybridge/TB-Loaders/{proj}
        File home = folder.newFolder("home");
        File localTbLoaders = new File(home, "TB-Loaders");
        localProjectDir = new File(localTbLoaders, PROJECT_NAME);
        localContentDir = new File(localProjectDir, "content");
        localContentDir.mkdirs();

        // Like ~/Dropbox/ACM-{proj}/TB-Loaders/published
        File dbx = folder.newFolder("dbx");
        File acmDir = new File(dbx, ACM_NAME);
        File tbLoaderDir = new File(acmDir, "TB-Loaders");
        globalPublishedDir = new File(tbLoaderDir, "published");
        globalPublishedDir.mkdirs();

        ACMConfiguration acmConfig = PowerMockito.mock(ACMConfiguration.class);
        when(acmConfig.getLocalTbLoaderDirFor(PROJECT_NAME)).thenReturn(localProjectDir);
        when(acmConfig.getTbLoaderDirFor(ACM_NAME)).thenReturn(tbLoaderDir);

        PowerMockito.mockStatic(ACMConfiguration.class);
        PowerMockito.when(ACMConfiguration.cannonicalAcmDirectoryName(PROJECT_NAME)).thenReturn(ACM_NAME);
        PowerMockito.when(ACMConfiguration.getInstance()).thenReturn(acmConfig);
    }

    /**
     * Helper class to make it easier to create mock deployments on disk.
     */
    private class MockDeployment {
        String deploymentName;
        String publishedRevision;
        private String timeStr;
        private boolean unpublished;
        private boolean oldUnpublishedNames;

        private MockDeployment(String deploymentName) {
            this.deploymentName = deploymentName;
            this.unpublished = true;
            DateFormat ISO8601time = new SimpleDateFormat("HHmmss.SSS'Z'", Locale.US); // Quoted "Z" to indicate UTC, no timezone offset
            ISO8601time.setTimeZone(TBLoaderConstants.UTC);
            this.timeStr = ISO8601time.format(new Date());
        }
        private MockDeployment(String deploymentName, String publishedRevision) {
            this.deploymentName = deploymentName;
            this.publishedRevision = publishedRevision;
        }
        private MockDeployment withOldUnpublishedName() {
            this.oldUnpublishedNames = true;
            return this;
        }
        private MockDeployment withGlobalRev() throws IOException {
            if (unpublished) throw new IllegalStateException("Can't have global rev with unpublished content.");
            String revFileName = String.format("%s-%s.rev", deploymentName, publishedRevision);
            File revFile = new File(globalPublishedDir, revFileName);
            revFile.createNewFile();
            return this;
        }
        private MockDeployment withGlobalContent() throws IOException {
            if (unpublished) throw new IllegalStateException("Can't have global content with unpublished content.");
            File deploymentDir = new File(globalPublishedDir, String.format("%s-%s", deploymentName,
                publishedRevision));
            deploymentDir.mkdir();
            String zipName = String.format("content-%s-%s.zip", deploymentName, publishedRevision);
            File zipFile = new File(deploymentDir, zipName);
            zipFile.createNewFile();
            return this;
        }
        private MockDeployment withLocalRev() throws IOException {
            File revFile = new File(localProjectDir, revFileName());
            revFile.createNewFile();
            return this;
        }
        private MockDeployment withLocalContent() {
            File deploymentDir = new File(localContentDir, deploymentName);
            deploymentDir.mkdir();
            return this;
        }

        private String revFileName() {
            String revFileName;
            if (unpublished) {
                if (oldUnpublishedNames) {
                    revFileName = String.format(TBLoaderConstants.UNPUBLISHED_REV + "_%s.rev", timeStr);
                } else {
                    revFileName = String.format(TBLoaderConstants.UNPUBLISHED_REVISION_FORMAT, timeStr, deploymentName);
                }
            } else {
                revFileName = String.format("%s-%s.rev", deploymentName, publishedRevision);
            }
            return revFileName;
        }
        private String revision() {
            return publishedRevision;
        }

        private String deploymentName() {
            return (deploymentName != null) ?
                   deploymentName : publishedRevision.substring(0, publishedRevision.length()-2);
        }
    }

    @Test
    public void nullTest() {
        // Ok.
    }

//    @Test
//    public void testNoData() throws IOException {
//        mockAcmConfig();
//
//        // No deployments in DBX, none local.
//
//        DeploymentsManager dm = new DeploymentsManager(PROJECT_NAME);
//        DeploymentsManager.LocalDeployment ld = dm.getLocalDeployment();
//
//        assertNull("Should be no local revision.", ld.localRevision);
//        assertNull("Should be no local content.", ld.localContent);
//        assertFalse("Should not be unpublished.", ld.isUnpublished);
//
//        DeploymentsManager.AvailableDeployments ad = dm.getAvailableDeployments();
//
//        assertNull("Should be no global deployment name.", ad.getCurrentDeployment());
//        assertTrue("Should be missing latest.", ad.isMissingLatest());
//        assertEquals("Should be no deployments", 0, ad.getDeploymentDescriptions().size());
//
//        DeploymentsManager.State state = dm.getState();
//        assertEquals("Should be missing latest.", DeploymentsManager.State.Missing_Latest, state);
//    }
//
//    @Test
//    public void testNoLatest() throws IOException {
//        mockAcmConfig();
//
//        // Global .rev file, w/o corresponding .zip file. (Not synced from Dropbox?)
//
//        new MockDeployment("NADA-2018-4", "a")
//            .withGlobalRev();
//
//        DeploymentsManager dm = new DeploymentsManager(PROJECT_NAME);
//        DeploymentsManager.LocalDeployment ld = dm.getLocalDeployment();
//
//        assertNull("Should be no local revision.", ld.localRevision);
//        assertNull("Should be no local content.", ld.localContent);
//        assertFalse("Should not be unpublished.", ld.isUnpublished);
//
//        DeploymentsManager.AvailableDeployments ad = dm.getAvailableDeployments();
//
//        assertNotNull("Should be a global deployment name.", ad.getCurrentDeployment());
//        assertTrue("Should be missing latest.", ad.isMissingLatest());
//        assertEquals("Should be one deployment", 0, ad.getDeploymentDescriptions().size());
//
//        DeploymentsManager.State state = dm.getState();
//        assertEquals("Should be missing latest.", DeploymentsManager.State.Missing_Latest, state);
//    }
//
//    @Test
//    public void testNoLatest2() throws IOException {
//        mockAcmConfig();
//
//        // Global .zip file, w/o corresponding .rev file. (Not synced from Dropbox?)
//
//        new MockDeployment("NADA-2018-4", "a")
//            .withGlobalContent();
//
//        DeploymentsManager dm = new DeploymentsManager(PROJECT_NAME);
//        DeploymentsManager.LocalDeployment ld = dm.getLocalDeployment();
//
//        assertNull("Should be no local revision.", ld.localRevision);
//        assertNull("Should be no global deployment name.", ld.localContent);
//        assertFalse("Should not be unpublished.", ld.isUnpublished);
//
//        DeploymentsManager.AvailableDeployments ad = dm.getAvailableDeployments();
//
//        assertNull("Should be no global deployment name.", ad.getCurrentDeployment());
//        assertTrue("Should be missing latest.", ad.isMissingLatest());
//        assertEquals("Should be one deployment", 1, ad.getDeploymentDescriptions().size());
//
//        DeploymentsManager.State state = dm.getState();
//        assertEquals("Should be missing latest.", DeploymentsManager.State.Missing_Latest, state);
//    }
//
//    @Test
//    public void testNoLatest3() throws IOException {
//        mockAcmConfig();
//
//        // Global .rev file, w/o corresponding .zip file. (Not synced from Dropbox?)
//        // Local rev and content up-to-date. Should still be "Missing_Latest".
//
//        MockDeployment latest = new MockDeployment("NADA-2018-4", "a")
//            .withGlobalRev()
//            .withLocalRev()
//            .withLocalContent();
//
//        DeploymentsManager dm = new DeploymentsManager(PROJECT_NAME);
//        DeploymentsManager.LocalDeployment ld = dm.getLocalDeployment();
//
//        assertEquals("Should be a local revision.", latest.revision(), ld.localRevision);
//        assertEquals("Should be some local content.", latest.deploymentName(), ld.localContent.getName());
//        assertFalse("Should not be unpublished.", ld.isUnpublished);
//
//        DeploymentsManager.AvailableDeployments ad = dm.getAvailableDeployments();
//
//        assertEquals("Should be a global deployment name.", latest.deploymentName(), ad.getCurrentDeployment());
//        assertTrue("Should be missing latest.", ad.isMissingLatest());
//        assertEquals("Should be no deployment", 0, ad.getDeploymentDescriptions().size());
//
//        DeploymentsManager.State state = dm.getState();
//        assertEquals("Should be missing latest.", DeploymentsManager.State.Missing_Latest, state);
//    }
//
//    @Test
//    public void testNoLatest4() throws IOException {
//        mockAcmConfig();
//
//        // Two global .rev and .zip files. Which is right? Can't tell; should still be "Missing_Latest".
//
//        new MockDeployment("NADA-2018-4", "a")
//            .withGlobalRev()
//            .withGlobalContent();
//        new MockDeployment("NADA-2018-3", "b")
//            .withGlobalRev()
//            .withGlobalContent();
//
//        DeploymentsManager dm = new DeploymentsManager(PROJECT_NAME);
//        DeploymentsManager.LocalDeployment ld = dm.getLocalDeployment();
//
//        assertNull("Should be no local revision.", ld.localRevision);
//        assertNull("Should be no local content.", ld.localContent);
//        assertFalse("Should not be unpublished.", ld.isUnpublished);
//
//        DeploymentsManager.AvailableDeployments ad = dm.getAvailableDeployments();
//
//        assertNull("Should be a global deployment name.", ad.getCurrentDeployment());
//        assertTrue("Should be missing latest.", ad.isMissingLatest());
//        assertEquals("Should be no deployment", 2, ad.getDeploymentDescriptions().size());
//
//        DeploymentsManager.State state = dm.getState();
//        assertEquals("Should be missing latest.", DeploymentsManager.State.Missing_Latest, state);
//    }
//
//    @Test
//    public void testNoLocal() throws IOException {
//        mockAcmConfig();
//
//        // Three revisions of two deployments in dbx, none local.
//
//        new MockDeployment("NADA-2018-3", "a")
//            .withGlobalContent();
//        new MockDeployment("NADA-2018-4", "a")
//            .withGlobalContent();
//        MockDeployment latest = new MockDeployment("NADA-2018-4", "b")
//            .withGlobalContent()
//            .withGlobalRev();
//
//        DeploymentsManager dm = new DeploymentsManager(PROJECT_NAME);
//        DeploymentsManager.LocalDeployment ld = dm.getLocalDeployment();
//
//        assertNull("Should be no local revision.", ld.localRevision);
//        assertNull("Should be no local content.", ld.localContent);
//        assertFalse("Should not be unpublished.", ld.isUnpublished);
//
//        DeploymentsManager.AvailableDeployments ad = dm.getAvailableDeployments();
//
//        assertEquals("Should be a global deployment name.", latest.deploymentName(), ad.getCurrentDeployment());
//        assertFalse("Should not be missing latest.", ad.isMissingLatest());
//        assertEquals("Should be two deployments", 2, ad.getDeploymentDescriptions().size());
//
//        DeploymentsManager.State state = dm.getState();
//        assertEquals("Should be 'no deployment.", DeploymentsManager.State.No_Deployment, state);
//    }
//
//    @Test
//    public void testLatestLocal() throws IOException {
//        mockAcmConfig();
//
//        // Three revisions of two deployments; latest also local.
//
//        new MockDeployment("NADA-2018-3", "a")
//            .withGlobalContent();
//        new MockDeployment("NADA-2018-4", "a")
//            .withGlobalContent();
//        MockDeployment latest = new MockDeployment("NADA-2018-4", "b")
//            .withGlobalContent()
//            .withGlobalRev()
//            .withLocalContent()
//            .withLocalRev();
//
//        DeploymentsManager dm = new DeploymentsManager(PROJECT_NAME);
//        DeploymentsManager.LocalDeployment ld = dm.getLocalDeployment();
//
//        assertEquals("Should be a local revision.", latest.revision(), ld.localRevision);
//        assertEquals("Should be some local content.", latest.deploymentName(), ld.localContent.getName());
//        assertFalse("Should not be unpublished.", ld.isUnpublished);
//
//        DeploymentsManager.State state = dm.getState();
//        assertEquals("Should be 'ok latest.", DeploymentsManager.State.OK_Latest, state);
//    }
//
//    @Test
//    public void testNotLatestLocal() throws IOException {
//        mockAcmConfig();
//
//        // Three revisions of two deployments; local version is not latest.
//
//        new MockDeployment("NADA-2018-3", "a")
//            .withGlobalContent();
//        MockDeployment notLatest = new MockDeployment("NADA-2018-4", "a")
//            .withGlobalContent()
//            .withLocalContent()
//            .withLocalRev();
//        new MockDeployment("NADA-2018-4", "b").withGlobalContent().withGlobalRev();
//
//        DeploymentsManager dm = new DeploymentsManager(PROJECT_NAME);
//        DeploymentsManager.LocalDeployment ld = dm.getLocalDeployment();
//
//        assertEquals("Should be a local revision.", notLatest.revision(), ld.localRevision);
//        assertEquals("Should be some local content.", notLatest.deploymentName(), ld.localContent.getName());
//        assertFalse("Should not be unpublished.", ld.isUnpublished);
//
//        DeploymentsManager.State state = dm.getState();
//        assertEquals("Should be 'not latest.", DeploymentsManager.State.Not_Latest, state);
//    }
//
//    @Test
//    public void testBadLocal() throws IOException {
//        mockAcmConfig();
//
//        // A good published revision. Only rev local.
//
//        new MockDeployment("NADA-2018-4", "b")
//            .withGlobalContent()
//            .withGlobalRev()
//            .withLocalRev();
//
//        DeploymentsManager dm = new DeploymentsManager(PROJECT_NAME);
//        DeploymentsManager.LocalDeployment ld = dm.getLocalDeployment();
//
//        assertNull("Should be no local revision.", ld.localRevision);
//        assertNull("Should be no local content.", ld.localContent);
//        assertFalse("Should not be unpublished.", ld.isUnpublished);
//
//        DeploymentsManager.State state = dm.getState();
//
//        assertEquals("Should be 'no deployment.", DeploymentsManager.State.No_Deployment, state);
//    }
//
//    @Test
//    public void testBadLocal2() throws IOException {
//        mockAcmConfig();
//
//        // A good published revision. Only content local.
//
//        new MockDeployment("NADA-2018-4", "b")
//            .withGlobalContent()
//            .withGlobalRev()
//            .withLocalContent();
//
//        DeploymentsManager dm = new DeploymentsManager(PROJECT_NAME);
//        DeploymentsManager.LocalDeployment ld = dm.getLocalDeployment();
//
//        assertNull("Should be no local revision.", ld.localRevision);
//        assertNull("Should be no local content.", ld.localContent);
//        assertFalse("Should not be unpublished.", ld.isUnpublished);
//
//        DeploymentsManager.State state = dm.getState();
//        assertEquals("Should be 'no deployment.", DeploymentsManager.State.No_Deployment, state);
//    }
//
//    @Test
//    public void testBadLocal3() throws IOException {
//        mockAcmConfig();
//
//        // A good published revision. Local rev and content mismatch.
//
//        new MockDeployment("NADA-2018-4", "b")
//            .withGlobalContent()
//            .withGlobalRev()
//            .withLocalRev();
//
//        new MockDeployment("NADA-2018-3", "a")
//            .withLocalContent();
//
//        DeploymentsManager dm = new DeploymentsManager(PROJECT_NAME);
//        DeploymentsManager.LocalDeployment ld = dm.getLocalDeployment();
//
//        assertNotNull("Expect an error message.", ld.errorMessage);
//        assertFalse("Should not be unpublished.", ld.isUnpublished);
//
//        DeploymentsManager.State state = dm.getState();
//        assertEquals("Should be 'bad local.", DeploymentsManager.State.Bad_Local, state);
//    }
//
//    @Test
//    public void testBadLocal4() throws IOException {
//        mockAcmConfig();
//
//        // A good published revision, also local. Plus an extra local content.
//
//        new MockDeployment("NADA-2018-4", "b")
//            .withGlobalContent()
//            .withGlobalRev()
//            .withLocalRev()
//            .withLocalContent();
//
//        new MockDeployment("NADA-2018-3", "a")
//            .withLocalContent();
//
//        DeploymentsManager dm = new DeploymentsManager(PROJECT_NAME);
//        DeploymentsManager.LocalDeployment ld = dm.getLocalDeployment();
//
//        assertNotNull("Should be a local revision.", ld.localRevision);
//        assertNotNull("Should be local content.", ld.localContent);
//        assertFalse("Should not be unpublished.", ld.isUnpublished);
//
//        DeploymentsManager.State state = dm.getState();
//        assertEquals("Should be 'OK Latest.", DeploymentsManager.State.OK_Latest, state);
//    }
//
//    @Test
//    public void testBadLocal5() throws IOException {
//        mockAcmConfig();
//
//        // A good published revision, also local. Plus an extra local rev marker.
//
//        new MockDeployment("NADA-2018-4", "b")
//            .withGlobalContent()
//            .withGlobalRev()
//            .withLocalRev()
//            .withLocalContent();
//
//        new MockDeployment("NADA-2018-3", "a")
//            .withLocalRev();
//
//        DeploymentsManager dm = new DeploymentsManager(PROJECT_NAME);
//        DeploymentsManager.LocalDeployment ld = dm.getLocalDeployment();
//
//        assertNotNull("Expect an error message.", ld.errorMessage);
//        assertFalse("Should not be unpublished.", ld.isUnpublished);
//
//        DeploymentsManager.State state = dm.getState();
//        assertEquals("Should be 'bad local.", DeploymentsManager.State.Bad_Local, state);
//    }
//
//    @Test
//    public void testUnpublishedLocal() throws IOException {
//        mockAcmConfig();
//
//        // Three revisions of two deployments; local version is unpublished.
//
//        new MockDeployment("NADA-2018-3", "a")
//            .withGlobalContent();
//        new MockDeployment("NADA-2018-4", "a").withGlobalContent();
//        new MockDeployment("NADA-2018-4", "b")
//            .withGlobalContent()
//            .withGlobalRev();
//        MockDeployment unpublished = new MockDeployment("NADA-2018-4")
//            .withOldUnpublishedName()
//            .withLocalRev()
//            .withLocalContent();
//
//        DeploymentsManager dm = new DeploymentsManager(PROJECT_NAME);
//        DeploymentsManager.LocalDeployment ld = dm.getLocalDeployment();
//
//        assertEquals("Should be a local revision.", "UNPUBLISHED", ld.localRevision);
//        assertEquals("Should be some local content.", unpublished.deploymentName(), ld.localContent.getName());
//        assertTrue("Should be unpublished.", ld.isUnpublished);
//
//        DeploymentsManager.State state = dm.getState();
//        assertEquals("Should be 'unpublished.", DeploymentsManager.State.OK_Unpublished, state);
//    }
//
//    @Test
//    public void testUnpublishedLocal2() throws IOException {
//        mockAcmConfig();
//
//        // Three revisions of two deployments; local version is unpublished.
//
//        new MockDeployment("NADA-2018-2", "b")
//            .withGlobalContent()
//            .withLocalContent();
//
//        new MockDeployment("NADA-2018-3", "a")
//            .withGlobalContent();
//        new MockDeployment("NADA-2018-4", "a")
//            .withGlobalContent();
//        new MockDeployment("NADA-2018-4", "b")
//            .withGlobalContent()
//            .withGlobalRev();
//        new MockDeployment("NADA-2018-4").withOldUnpublishedName()
//            .withLocalRev()
//            .withLocalContent();
//
//        DeploymentsManager dm = new DeploymentsManager(PROJECT_NAME);
//        DeploymentsManager.LocalDeployment ld = dm.getLocalDeployment();
//
//        assertNotNull("Expect an error message.", ld.errorMessage);
//        assertNull("Should be no local revision.", ld.localRevision);
//        assertNull("Should be no local content.", ld.localContent);
//        assertFalse("Should not be unpublished.", ld.isUnpublished);
//
//        DeploymentsManager.State state = dm.getState();
//        assertEquals("Should be 'Bad Local.", DeploymentsManager.State.Bad_Local, state);
//    }
//
//    @Test
//    public void testUnpublishedLocal3() throws IOException {
//        mockAcmConfig();
//
//        // Three revisions of two deployments; local version is unpublished.
//
//        new MockDeployment("NADA-2017-2", "b")
//            .withGlobalContent()
//            .withLocalContent();
//
//        new MockDeployment("NADA-2018-3", "a")
//            .withGlobalContent();
//        new MockDeployment("NADA-2018-4", "a")
//            .withGlobalContent();
//        new MockDeployment("NADA-2018-4", "b")
//            .withGlobalContent()
//            .withGlobalRev();
//        MockDeployment unpublished = new MockDeployment("NADA-2018-4")
//            .withLocalRev()
//            .withLocalContent();
//
//        DeploymentsManager dm = new DeploymentsManager(PROJECT_NAME);
//        DeploymentsManager.LocalDeployment ld = dm.getLocalDeployment();
//
//        assertEquals("Should be a local revision.", "UNPUBLISHED", ld.localRevision);
//        assertEquals("Should be some local content.", unpublished.deploymentName(), ld.localContent.getName());
//        assertTrue("Should be unpublished.", ld.isUnpublished);
//
//        DeploymentsManager.State state = dm.getState();
//        assertEquals("Should be 'unpublished.", DeploymentsManager.State.OK_Unpublished, state);
//    }

}
