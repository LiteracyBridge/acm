package core.tbloader;

import org.junit.Test;
import org.literacybridge.core.fs.FsFile;
import org.literacybridge.core.fs.TbFile;
import org.literacybridge.core.tbloader.TbFlashData;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class TbFlashDataTest {
  @Test
  public void testLoadingStatsFile() throws Exception {
    String testFileName = "flashData.bin.test";
    File testFile = new File(TbFlashData.class.getClassLoader().getResource(testFileName).getFile());
    TbFile flashData = new FsFile(testFile);
    TbFlashData info = new TbFlashData(flashData);

    assertEquals("B-000C02CA", info.getSerialNumber());
    assertEquals("DEMO-SEATTLE", info.getCommunity());
  }
}
