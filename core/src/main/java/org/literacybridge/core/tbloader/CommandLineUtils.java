package org.literacybridge.core.tbloader;

import org.literacybridge.core.OSChecker;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandLineUtils {
  private static final Logger LOG = Logger.getLogger(CommandLineUtils.class.getName());

  private static File windowsUtilsDirectory = new File(".");

  private static final Pattern PCT_COMPLETE = Pattern.compile("^(\\d{1,2}) percent completed\\.+$");

  static void setUtilsDirectory(File windowsUtilsDirectory) {
    CommandLineUtils.windowsUtilsDirectory = windowsUtilsDirectory;
  }

  /**
   * Runs the given command in a DOS command shell.
   * @param cmd The command, in a string.
   * @return An ad-hoc reinterpretation of the command output.
   * @throws IOException
   */
  public static String execute(String cmd) throws IOException {
    String line;
    String errorLine = null;
    LOG.log(Level.INFO, "TBL!: Executing:" + cmd);
    if (!cmd.startsWith("cmd /C ")) cmd = "cmd /C " + cmd;
    Process proc = Runtime.getRuntime().exec(cmd);

    // Stdout is called the "InputStream". Hopefully, someone at Sun was fired for that...
    BufferedReader br1 = new BufferedReader(new InputStreamReader(proc.getInputStream()));
    BufferedReader br2 = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

    // Stdout
    errorLine = processCommandOutput(errorLine, br1);
    // Stderr
    errorLine = processCommandOutput(errorLine, br2);

    try {
      proc.waitFor();
    } catch (InterruptedException e) {
      throw new IOException(e);
    }
    return errorLine;
  }

  /**
   * processes either stdout or stderr from a command.
   * @param errorLine Any errorLine detected so far
   * @param br A buffered reader with the output
   * @return The errorLine at the end of processing this outout
   * @throws IOException if the output file can't be read
   */
  private static String processCommandOutput(String errorLine, BufferedReader br) throws IOException {
    StringBuilder outBuf = new StringBuilder();
    String line;
    do {
      line = br.readLine();
      if (line != null) {
        line = line.trim();
        Matcher matcher = PCT_COMPLETE.matcher(line);
        if (!matcher.matches()) {
          outBuf.append(line).append("\n");
        }
      }
      if (line != null && errorLine == null) {
        errorLine = dosErrorCheck(line);
      }
    } while (line != null);
    LOG.log(Level.INFO, outBuf.toString());
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
            || line.startsWith("The system cannot find the file")
            || line.startsWith("Windows found errors on the disk")) {
      // checkdisk shows corruption
      errorMsg = "File system corrupted";
    } else if (line.startsWith("The system cannot find the path specified.")) {
      errorMsg = "TB not found.  Unplug/replug USB and try again.";
    }
    if (errorMsg != null) {
      LOG.log(Level.SEVERE, line);
      LOG.log(Level.SEVERE, errorMsg);
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
    String cmd = String.format("%s %s",new File(windowsUtilsDirectory, "RemoveDrive.exe") ,drive);
    String errorLine = CommandLineUtils.execute(cmd);
    return errorLine == null;
  }
}
