package core.tbloader;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;
import org.literacybridge.core.fs.DefaultTBFileSystem;
import org.literacybridge.core.fs.RelativePath;
import org.literacybridge.core.fs.TBFileSystem;
import org.literacybridge.core.tbloader.TBInfo;

public class TBInfoTest {
  @Test
  public void testLoadingStatsFile() throws Exception {
    String testFileName = "flashData.bin.test";
    File testFile = new File(TBInfo.class.getClassLoader().getResource(testFileName).getFile());
    TBFileSystem fs = DefaultTBFileSystem.open(testFile.getParentFile());
    TBInfo info = new TBInfo(fs, new RelativePath(testFileName));

    assertEquals("B-000C02CA", info.getSerialNumber());
    assertEquals("DEMO-SEATTLE", info.getLocation());
  }
}
