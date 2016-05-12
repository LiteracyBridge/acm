package org.literacybridge.acm.tbloader;

import org.junit.Test;
import org.junit.Assert;
import static org.junit.Assert.*;

/**
 * Created by bill on 5/11/16.
 */
public class TBLoaderTest {

    @Test
    public void testSerialIs10Chars() {
        String goodBSerialNo = "b-12345678";
        String badSerialNoShort = "b-1234567";
        String badSerialNoLong = "b-123456789";
        TBLoader tbloader = new TBLoader(null, "b-");

        boolean isGoodSerialNo = tbloader.isSerialNumberFormatGood(goodBSerialNo);
        assertTrue("Expect 'format good' when length is 10", isGoodSerialNo);

        isGoodSerialNo = tbloader.isSerialNumberFormatGood(badSerialNoShort);
        assertFalse("Expect 'format bad' when length is < 10", isGoodSerialNo);

        isGoodSerialNo = tbloader.isSerialNumberFormatGood(badSerialNoLong);
        assertFalse("Expect 'format bad' when length is > 10", isGoodSerialNo);
    }

    @Test
    public void testASerialIsNotOkByDefault() {
        String goodBSerialNo = "a-01230124";
        TBLoader tbloader = new TBLoader(null, null);

        boolean isGoodSerialNo = tbloader.isSerialNumberFormatGood(goodBSerialNo);
        assertFalse(isGoodSerialNo);
    }

    @Test
    public void testBSerialIsOkByDefault() {
        String goodBSerialNo = "b-01230124";
        TBLoader tbloader = new TBLoader(null, null);

        boolean isGoodSerialNo = tbloader.isSerialNumberFormatGood(goodBSerialNo);
        assertTrue(isGoodSerialNo);
    }

    @Test
    public void testOldStyleSerialIsNotOk2() {
        String oldBSerialNo = "b-00ff0000";
        TBLoader tbloader = new TBLoader(null, null);

        boolean isGoodSerialNo = tbloader.isSerialNumberFormatGood(oldBSerialNo);
        assertTrue("Expect b-00010124 is OK in isGoodSerialNo", isGoodSerialNo);

        boolean isGood2SerialNo = tbloader.isSerialNumberFormatGood2(oldBSerialNo);
        assertFalse("Expect b-00010124 is NOT OK in isGoodSerialNo2", isGood2SerialNo);
    }
}
