package core;

import static org.junit.Assert.*;

import org.junit.Test;
import org.literacybridge.core.tbloader.TBInfo;

public class TBInfoTest {
  @Test
  public void testLoadingStatsFile() throws Exception {
    String testFile = TBInfo.class.getClassLoader().getResource("flashData.bin.test").getFile();
    TBInfo info = new TBInfo(testFile);

    assertEquals("B-000C02CA", info.getSerialNumber());
    assertEquals("DEMO-SEATTLE", info.getLocation());
  }
}
