package org.literacybridge.core.tbloader;

import org.literacybridge.core.OSChecker;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CommandLineUtils {
  private static final Logger LOG = Logger.getLogger(CommandLineUtils.class.getName());

  /**
   * Runs the given command in a DOS command shell.
   * @param cmd The command, in a string.
   * @return An ad-hoc reinterpretation of the command output.
   * @throws IOException
     */
  public static String execute(String cmd) throws IOException {
    String line;
    String errorLine = null;
    LOG.log(Level.INFO, "Executing:" + cmd);
    if (!cmd.startsWith("cmd /C ")) cmd = "cmd /C " + cmd;
    Process proc = Runtime.getRuntime().exec(cmd);

    BufferedReader br1 = new BufferedReader(
        new InputStreamReader(proc.getInputStream()));
    BufferedReader br2 = new BufferedReader(
        new InputStreamReader(proc.getErrorStream()));

    do {
      line = br1.readLine();
      LOG.log(Level.INFO, line);
      if (line != null && errorLine == null) {
        errorLine = dosErrorCheck(line);
      }
    } while (line != null);

    do {
      line = br2.readLine();
      LOG.log(Level.INFO, line);
      if (line != null && errorLine == null) {
        errorLine = dosErrorCheck(line);
      }
    } while (line != null);

    try {
      proc.waitFor();
    } catch (InterruptedException e) {
      throw new IOException(e);
    }
    return errorLine;
  }

  private static String dosErrorCheck(String line) {
    String errorMsg = null;

    if (line.contains("New")) {
      //  file copy validation failed (some files missing on target)
      errorMsg = line;//.substring(line.length()-30);
    } else if (line.contains("Invalid media or Track 0 bad - disk unusable")) {
      // formatting error
      errorMsg = "Bad memory card.  Please discard and replace it.";
    } else if (line.contains("Specified drive does not exist.")
        || line.startsWith(
        "The volume does not contain a recognized file system.")) {
      errorMsg = "Either bad memory card or USB connection problem.  Try again.";
    } else if (line.contains("Windows found problems with the file system") /* || line.startsWith("File Not Found") */
        || line.startsWith("The system cannot find the file")) {
      // checkdisk shows corruption
      errorMsg = "File system corrupted";
    } else if (line.startsWith("The system cannot find the path specified.")) {
      errorMsg = "TB not found.  Unplug/replug USB and try again.";
    }
    return errorMsg;
  }

  public static boolean formatDisk(String drive, String newLabel) throws IOException {
    if (!OSChecker.WINDOWS) {
      throw new IllegalStateException("formatDisk operation is only supported on Windows");
    }
    if (drive.length() > 2) drive = drive.substring(0,2);
    String errorLine = CommandLineUtils.execute(String.format("format %s /FS:FAT32 /v:%s /Y /Q", drive, newLabel));
    return errorLine == null;
  }

  public static boolean checkDisk(String drive) throws IOException {
    if (!OSChecker.WINDOWS) {
      throw new IllegalStateException("checkDisk operation is only supported on Windows");
    }
    if (drive.length() > 2) drive = drive.substring(0,2);
    String errorLine = CommandLineUtils.execute(String.format("echo n|chkdsk %s", drive));
    return errorLine == null;
  }

  public static boolean checkDisk(String drive, String saveOutputFile) throws IOException {
    if (!OSChecker.WINDOWS) {
      throw new IllegalStateException("checkDisk operation is only supported on Windows");
    }
    if (drive.length() > 2) drive = drive.substring(0,2);
    File output = new File(saveOutputFile);
    if (!output.getParentFile().exists()) {
      output.getParentFile().mkdirs();
    }
    String errorLine = CommandLineUtils.execute(String.format("echo n|chkdsk %s > %s", drive, output.getAbsolutePath()));
    return errorLine == null;
  }

  public static boolean relabel(String drive, String newLabel) throws IOException {
    if (!OSChecker.WINDOWS) {
      throw new IllegalStateException("relabel operation is only supported on Windows");
    }
    if (drive.length() > 2) drive = drive.substring(0,2);
    String errorLine = CommandLineUtils.execute(String.format("label %s %s", drive, newLabel));
    return errorLine == null;
  }

  public static boolean disconnectDrive(String drive) throws IOException {
    String errorLine = CommandLineUtils.execute(String.format(new File("software/RemoveDrive.exe").toString() +" %s", drive));
    return errorLine == null;
  }
}
