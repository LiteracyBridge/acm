package org.literacybridge.acm.audioconverter.converters;

import org.apache.commons.io.FilenameUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Set;

public abstract class BaseAudioConverter {

  public static final String SAMPLE_RATE = "sampleRate";
  public static final String BIT_RATE = "bitRate";

  protected final String targetFormatExtension;

  public BaseAudioConverter(String targetFormatExtension) {
    this.targetFormatExtension = targetFormatExtension;
  }

  public File targetFile(File inputFile, File targetDir) {
    return targetFile(inputFile, targetDir, targetFormatExtension);
  }

    /**
     * Given a file with the desired base name, and a directory, and a desired extension, return a File of
     * the given base name, with the desired extension, in the desired directory.
     * @param inputFile File with the base name of the desire dresult.
     * @param targetDir Directory in which the file is desired.
     * @param alternateExtension Desired extension of the file.
     * @return A File with the desired extension in the desired directory.
     */
  static public File targetFile(File inputFile, File targetDir, String alternateExtension) {
    if (alternateExtension.charAt(0)!='.') alternateExtension = "." + alternateExtension;
    return new File(targetDir, FilenameUtils.removeExtension(inputFile.getName()) + alternateExtension);
  }

  public String convertFile(File inputFile,
      File outputFile,
      File tmpDir,
      boolean overwrite,
      Map<String, String> parameters) throws ConversionException
  {
    if (outputFile.exists()) {
      if (!overwrite) {
        return null;
      } else {
        if (!outputFile.delete()) {
          throw new ConversionException(String.format("Unable to overwrite output file (%s).",
              outputFile));
        }
      }
    }

    ConversionResult result = doConvertFile(inputFile, outputFile.getParentFile(), outputFile, tmpDir, parameters);

    if (!result.outputFile.getAbsolutePath().equals(outputFile.getAbsolutePath())) {
      if (!result.outputFile.equals(outputFile) && !result.outputFile.renameTo(outputFile)) {
        throw new ConversionException(String.format("Unable to rename output file (%s as %s).",
            result.outputFile,
            outputFile));
      }
    }
    return result.response;
  }

  public abstract ConversionResult doConvertFile(File inputFile, File targetDir,
      File targetFile, File tmpDir, Map<String, String> parameters)
      throws ConversionException;

  // @return short description for the format comboBox, e.g. ".a18 to .wav
  // format"
  public abstract String getShortDescription();

  // @return get source file extension, e.g. a18
  public abstract Set<String> getSourceFileExtensions();

  public abstract void validateConverter()
      throws AudioConverterInitializationException;

 static String executeConversionCommand(String cmd, boolean listenToStdErr,
      String inputFileName) throws ConversionException {
    StringBuilder responseBuilder = new StringBuilder();
    boolean success = false;
    try {
      Process proc = Runtime.getRuntime().exec(cmd);

      InputStream stderr = listenToStdErr ? proc.getErrorStream()
          : proc.getInputStream();
      InputStreamReader isr = new InputStreamReader(stderr);
      BufferedReader br = new BufferedReader(isr);
      String line;

      while ((line = br.readLine()) != null) {
        responseBuilder.append(line);
        responseBuilder.append('\n');
      }

      // check for converter error
      success = (proc.waitFor() == 0);
    } catch (Exception ignored) {
    } finally {
      if (!success) {
        //noinspection ThrowFromFinallyBlock
        throw new ConversionException(
            "Converter: Internal error while converting file: '" + inputFileName
                + "'");
      }
    }

    return responseBuilder.toString();
  }

  static void consumeProcessOutput(Process proc, boolean listenToStdErr)
      throws IOException {
    InputStream stderr = listenToStdErr ? proc.getErrorStream()
        : proc.getInputStream();
    InputStreamReader isr = new InputStreamReader(stderr);
    BufferedReader br = new BufferedReader(isr);

    //noinspection StatementWithEmptyBody
    while (br.readLine() != null)
      ;
  }

  static void validateConverterExecutable(String exePath,
      boolean listenToStdErr, String outputPrefix)
      throws AudioConverterInitializationException {
    File exeFile = new File(exePath);
    if (!exeFile.exists()) {
      throw new AudioConverterInitializationException(
          "Converter executable not found.");
    }

    boolean success = false;
    try {

      Process proc = Runtime.getRuntime().exec(exePath);
      InputStream stderr = listenToStdErr ? proc.getErrorStream()
          : proc.getInputStream();
      InputStreamReader isr = new InputStreamReader(stderr);
      BufferedReader br = new BufferedReader(isr);
      String line;

      while ((line = br.readLine()) != null) {
        if (line.startsWith(outputPrefix)) {
          success = true;
        }
      }
      proc.waitFor();
      success = true;
    } catch (Exception e) {
      // nothing to do - finally block will throw exception
    } finally {
      if (!success) {
        //noinspection ThrowFromFinallyBlock
        throw new AudioConverterInitializationException(
            "Converter executable could not be executed.");
      }
    }
  }

  public static class AudioConverterException extends Exception {
    private static final long serialVersionUID = 1L;

    public AudioConverterException(String msg) {
      super(msg);
    }
  }

  public static class AudioConverterInitializationException
      extends AudioConverterException {
    private static final long serialVersionUID = 1L;

    public AudioConverterInitializationException(String msg) {
      super(msg);
    }
  }

  public static class ConversionException extends AudioConverterException {
    private static final long serialVersionUID = 1L;

    public ConversionException(String msg) {
      super(msg);
    }
  }

  public static class ConversionSourceMissingException extends ConversionException {
    private final String filename;

    public ConversionSourceMissingException(String msg, String filename) {
      super(msg);
      this.filename = filename;
    }

  }

  public static class ConversionResult {
    public String response;
    public File outputFile;
  }
}
