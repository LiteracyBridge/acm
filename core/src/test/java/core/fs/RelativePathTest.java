package core.fs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.literacybridge.core.fs.RelativePath;

import java.io.File;

public class RelativePathTest {
    private final static String FS = File.separator;

  @Test
  public void testParse() {
    String pathStr = "var"+FS+"log"+FS+"daily.out";
    RelativePath path = RelativePath.parse(pathStr);
    assertEquals("daily.out", path.getLastSegment());
    assertEquals(pathStr, path.asString());
  }

  @Test
  public void testAppendComponents() {
    String pathStr = "var"+FS+"log"+FS+"daily.out";
    RelativePath path = new RelativePath();
    assertEquals("", path.asString());
    path = new RelativePath(path, "var");
    assertEquals(1, path.getSegmentCount());
    assertEquals("var", path.getLastSegment());
    path = new RelativePath(path, "log", "daily.out");
    assertEquals("daily.out", path.getLastSegment());
    assertEquals(3, path.getSegmentCount());
    assertEquals(pathStr, path.asString());
  }

  @Test
  public void testParent() {
    String pathStr = "var"+FS+"log"+FS+"daily.out";
    RelativePath path = RelativePath.parse(pathStr);
    assertEquals("daily.out", path.getLastSegment());
    assertEquals(pathStr, path.asString());

    RelativePath parent = path.getParent();
    assertEquals(2, parent.getSegmentCount());
    assertEquals("var"+FS+"log", parent.asString());
    assertEquals("log", parent.getLastSegment());
  }

  @Test
  public void testEquals() {
    String pathStr = "var"+FS+"log"+FS+"daily.out";
    RelativePath path = RelativePath.parse(pathStr);
    RelativePath path1 = new RelativePath(path);
    RelativePath path2 = RelativePath.parse(path.asString());
    assertEquals(path, path1);
    assertEquals(path, path2);
    assertEquals(path.hashCode(), path1.hashCode());
    assertEquals(path.hashCode(), path2.hashCode());
  }

}
