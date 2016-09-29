package org.literacybridge.acm.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Created by bill on 2/24/16.
 */
public class DropboxFinder {
  private static class BadEnvironmentException extends RuntimeException {
    BadEnvironmentException(String envVar) {
      super("Bad environment: missing variable " + envVar);
    }
  }

  private static final String infoFileName = "info.json";

  public static void main(String[] args) {
    String dropboxPath = getDropboxPath();
    System.out.println(dropboxPath);
  }

  public static String getDropboxPath() {
    DropboxFinder dbFinder = new DropboxFinder();
    File infoJson = dbFinder.getInfoFile();
    return dbFinder.getDropboxPathFromInfoFile(infoJson);
  }

  /**
   * Determine if running on Windows.
   *
   * This is implemented as a static function so that it can be mocked, for
   * testing. All we really want is the value of OsUtils.WINDOWS.
   *
   * @return True if running on Windows. False otherwise.
   */
  protected static boolean onWindows() {
    return OsUtils.WINDOWS;
  }

  /**
   * Reads the "path" property from the info.json file.
   * 
   * @param infoJson
   *          A File that may point to an info.json file (it may not exist).
   * @return The path to the Dropbox data, as a String.
   */
  private String getDropboxPathFromInfoFile(File infoJson) {
    try {
      InputStream is = new FileInputStream(infoJson);
      return getDropboxPathFromInputStream(is);
    } catch (Exception e) {
      return "";
    }
  }

  /**
   * Parse the Dropbox info.json data, and return the path to the Dropbox.
   * 
   * @param is
   *          An input stream that will supply the characters of the JSON.
   * @return The 'path' property from the JSON file, or empty if none.
   */
  protected String getDropboxPathFromInputStream(InputStream is) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode root = mapper.readTree(is);
      JsonNode node = root.get("business");
      if (node != null) {
        return node.get("path").getTextValue();
      }
      // No business, look for "personal"
      node = root.get("personal");
      if (node != null) {
        return node.get("path").getTextValue();
      }
    } catch (Exception ignored) {
    }
    return "";
  }

  /**
   * Look for Dropbox info.json file. The location is system dependent.
   * 
   * @return File with path to Dropbox info.json. The file may not exist.
   */
  protected File getInfoFile() {
    File infoJson;
    if (DropboxFinder.onWindows()) {
      infoJson = getInfoFileFromWindows();
    } else {
      infoJson = getInfoFileFromNix();
    }
    return infoJson;
  }

  /**
   * Look for Dropbox info.json on Windows system.
   * 
   * @return File with path to Dropbox info.json.
   */
  private File getInfoFileFromWindows() {
    File jsonFile;
    String jsonPath = System.getenv("APPDATA");
    if (jsonPath == null) {
      throw new BadEnvironmentException("APPDATA");
    }
    jsonPath = jsonPath + File.separator + "Dropbox";
    jsonFile = new File(jsonPath, infoFileName);
    if (!jsonFile.exists()) {
      jsonPath = System.getenv("LOCALAPPDATA");
      if (jsonPath == null) {
        throw new BadEnvironmentException("LOCALAPPDATA");
      }
      jsonPath = jsonPath + File.separator + "Dropbox";
      jsonFile = new File(jsonPath, infoFileName);
    }
    return jsonFile;
  }

  /**
   * Look for the Dropbox info.json on *nix system (OS/X or Linux).
   * 
   * @return File with path to Dropbox info.json file.
   */
  private File getInfoFileFromNix() {
    File jsonFile;
    String jsonPath = System.getProperty("user.home");
    jsonPath = jsonPath + File.separator + ".dropbox";
    jsonFile = new File(jsonPath, infoFileName);
    return jsonFile;
  }
}
