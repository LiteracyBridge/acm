package core.tbloader;

import org.junit.Test;
import org.literacybridge.core.tbloader.TBLoaderUtils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by bill on 5/11/16.
 */
public class TBLoaderTest {

//    @Test
//    public void testAlwaysFails() {
//        assertTrue("Should fail", false);
//    }

    @Test
    public void testSerialIs10Chars() {
        String prefix = "b-";
        String goodBSerialNo = "b-12345678";
        String badSerialNoShort = "b-1234567";
        String badSerialNoLong = "b-123456789";

        boolean isGoodSerialNo = TBLoaderUtils.isSerialNumberFormatGood(prefix, goodBSerialNo);
        assertTrue("Expect 'format good' when length is 10", isGoodSerialNo);

        isGoodSerialNo = TBLoaderUtils.isSerialNumberFormatGood(prefix, badSerialNoShort);
        assertFalse("Expect 'format bad' when length is < 10", isGoodSerialNo);

        isGoodSerialNo = TBLoaderUtils.isSerialNumberFormatGood(prefix, badSerialNoLong);
        assertFalse("Expect 'format bad' when length is > 10", isGoodSerialNo);
    }

    @Test
    public void testASerialIsNotOkByDefault() {
        String prefix = "b-";
        String goodBSerialNo = "a-01230124";

        boolean isGoodSerialNo = TBLoaderUtils.isSerialNumberFormatGood(prefix, goodBSerialNo);
        assertFalse(isGoodSerialNo);
    }

    @Test
    public void testBSerialIsOkByDefault() {
        String prefix = "b-";
        String goodBSerialNo = "b-01230124";

        boolean isGoodSerialNo = TBLoaderUtils.isSerialNumberFormatGood(prefix, goodBSerialNo);
        assertTrue(isGoodSerialNo);
    }

    @Test
    public void testOldStyleSerialIsNotOk2() {
        String prefix = "b-";
        String oldBSerialNo = "b-00ff0000";

        boolean isGoodSerialNo = TBLoaderUtils.isSerialNumberFormatGood(prefix, oldBSerialNo);
        assertTrue("Expect b-00010124 is OK in isGoodSerialNo", isGoodSerialNo);

        boolean isGood2SerialNo = TBLoaderUtils.isSerialNumberFormatGood2(oldBSerialNo);
        assertFalse("Expect b-00010124 is NOT OK in isGoodSerialNo2", isGood2SerialNo);
    }

}
