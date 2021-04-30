package core.tbloader;

import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.literacybridge.core.fs.FsFile;
import org.literacybridge.core.fs.TbFile;
import org.literacybridge.core.tbloader.DeploymentInfo;
import org.literacybridge.core.tbloader.ProgressListener;
import org.literacybridge.core.tbloader.TBDeviceInfo;
import org.literacybridge.core.tbloader.TBLoaderConfig;
import org.literacybridge.core.tbloader.TBLoaderConstants;
import org.literacybridge.core.tbloader.TBLoaderCore;
import org.literacybridge.core.tbloader.TBLoaderUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.literacybridge.core.tbloader.TBLoaderUtils.isSerialNumberFormatGood;
import static org.literacybridge.core.tbloader.TBLoaderUtils.isSerialNumberFormatGood2;

/**
 * Created by bill on 5/11/16.
 */
public class TBLoaderTest {
    private static final String location = "Other";
    private static final String testUser = "tester";
    private static final String tbcdId = "1234";
    private static final String newSrn = "B-12341234";
    private static final String newProject = "PROJ2";
    private static final String newDepl = "PROJ2-18-2";
    private static final String newPkg = "DEMO-2016-2-EN";
    private static final String newTimestamp = "Tue Feb 27 00:00:00 HST 2018";
    private static final String newFirmware = "r1999";
    private static final String newCommunity = "demo-Seattle";
    private static final String recipientid = "123456789abc";
    private static final String srnPrefix = "B-";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

//    @Test
//    public void testAlwaysFails() {
//        assertTrue("Should fail", false);
//    }

    @Test
    public void testSerialIs10Chars() {
        String prefix = TBLoaderConstants.NEW_TB_SRN_PREFIX;
        String goodBSerialNo = prefix + "12345678";
        String badSerialNoShort = prefix + "1234567";
        String badSerialNoLong = prefix + "123456789";

        boolean isGoodSerialNo = TBLoaderUtils.isSerialNumberFormatGood(prefix, goodBSerialNo);
        assertTrue("Expect 'format good' when length is 10", isGoodSerialNo);

        isGoodSerialNo = TBLoaderUtils.isSerialNumberFormatGood(prefix, badSerialNoShort);
        assertFalse("Expect 'format bad' when length is < 10", isGoodSerialNo);

        isGoodSerialNo = TBLoaderUtils.isSerialNumberFormatGood(prefix, badSerialNoLong);
        assertFalse("Expect 'format bad' when length is > 10", isGoodSerialNo);
    }

    @Test
    public void testASerialIsNotOkByDefault() {
        String prefix = TBLoaderConstants.NEW_TB_SRN_PREFIX;
        String goodBSerialNo = "a-01230124";

        boolean isGoodSerialNo = TBLoaderUtils.isSerialNumberFormatGood(prefix, goodBSerialNo);
        assertFalse(isGoodSerialNo);
    }

    @Test
    public void testBSerialIsOkByDefault() {
        String prefix = TBLoaderConstants.NEW_TB_SRN_PREFIX;
        String goodBSerialNo = prefix + "01230124";

        boolean isGoodSerialNo = TBLoaderUtils.isSerialNumberFormatGood(prefix, goodBSerialNo);
        assertTrue(isGoodSerialNo);
    }

    @Test
    public void testOldStyleSerialIsNotOk2() {
        String prefix = TBLoaderConstants.OLD_TB_SRN_PREFIX;
        String oldBSerialNo = prefix + "00ff0000";

        boolean isGoodSerialNo = TBLoaderUtils.isSerialNumberFormatGood(prefix, oldBSerialNo);
        assertTrue("Expect "+oldBSerialNo+" is OK in isGoodSerialNo", isGoodSerialNo);

        boolean isGood2SerialNo = TBLoaderUtils.isSerialNumberFormatGood2(oldBSerialNo);
        assertTrue("Expect b-00010124 is OK in isGoodSerialNo2", isGood2SerialNo);
    }

    @Test
    public void testUpdateBtoCseries() {
        String prefix = "B-";
        String goodBSerialNo = prefix + "01230124";

        boolean isGoodSerialNo = TBLoaderUtils.isSerialNumberFormatGood(prefix, goodBSerialNo);
        assertTrue("Expect "+goodBSerialNo+" to be OK in isGoodSerialNo.", isGoodSerialNo);

        boolean isGoodSerialNo2 = TBLoaderUtils.isSerialNumberFormatGood2(goodBSerialNo);
        assertTrue("Expect "+goodBSerialNo+" to be OK in isGoodSerialNo2.", isGoodSerialNo2);

        boolean isNeedNewSrn = TBLoaderUtils.newSerialNumberNeeded(prefix, goodBSerialNo);
        assertTrue("Expect "+goodBSerialNo+" to be newSerialNumberNeeded.", isNeedNewSrn);
    }

}
