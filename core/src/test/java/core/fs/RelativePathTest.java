package core.fs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.literacybridge.core.fs.RelativePath;

public class RelativePathTest {
  @Test
  public void testParse() {
    String pathStr = "var/log/daily.out";
    RelativePath path = RelativePath.parse(pathStr);
    assertEquals("daily.out", path.getLastSegment());
    assertEquals(pathStr, path.asString());
  }

  @Test
  public void testAppendComponents() {
    String pathStr = "var/log/daily.out";
    RelativePath path = new RelativePath();
    assertEquals("", path.asString());
    path = new RelativePath(path, "var");
    assertEquals(1, path.getSegments().size());
    assertEquals("var", path.getLastSegment());
    path = new RelativePath(path, "log", "daily.out");
    assertEquals("daily.out", path.getLastSegment());
    assertEquals(3, path.getSegments().size());
    assertEquals(pathStr, path.asString());
  }

  @Test
  public void testParent() {
    String pathStr = "var/log/daily.out";
    RelativePath path = RelativePath.parse(pathStr);
    assertEquals("daily.out", path.getLastSegment());
    assertEquals(pathStr, path.asString());

    RelativePath parent = path.getParent();
    assertEquals(2, parent.getSegments().size());
    assertEquals("var/log", parent.asString());
    assertEquals("log", parent.getLastSegment());
  }

  @Test
  public void testEquals() {
    String pathStr = "var/log/daily.out";
    RelativePath path = RelativePath.parse(pathStr);
    RelativePath path1 = new RelativePath(path.getSegments());
    RelativePath path2 = RelativePath.parse(path.asString());
    assertEquals(path, path1);
    assertEquals(path, path2);
    assertEquals(path.hashCode(), path1.hashCode());
    assertEquals(path.hashCode(), path2.hashCode());
  }

  @Test
  public void testGetRelativePath() {
    String root = "var/";
    String absolutePath = "var/log/daily.out";

    RelativePath relativePath = RelativePath.getRelativePath(root, "daily.out");
    assertNull(relativePath);

    relativePath = RelativePath.getRelativePath(root, absolutePath);
    assertNotNull(relativePath);

    assertEquals("daily.out", relativePath.getLastSegment());
    assertEquals("log/daily.out", relativePath.asString());
  }
}
