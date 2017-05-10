package org.literacybridge.acm.audioconverter.api;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.literacybridge.acm.audioconverter.converters.A18ToMP3Converter;
import org.literacybridge.acm.audioconverter.converters.A18ToWavConverter;
import org.literacybridge.acm.audioconverter.converters.AnyToA18Converter;
import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter;
import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter.AudioConverterInitializationException;
import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter.ConversionException;
import org.literacybridge.acm.audioconverter.converters.FFMpegConverter;

public class ExternalConverter {

  private A18ToMP3Converter A18ToMP3Conv;
  private A18ToWavConverter A18ToWAVConv;
  private AnyToA18Converter AnyToA18Conv;
  private FFMpegConverter FFMpegConv;

  public ExternalConverter() {
    try {
      A18ToMP3Conv = new A18ToMP3Converter();
      A18ToWAVConv = new A18ToWavConverter();
      AnyToA18Conv = new AnyToA18Converter();
      FFMpegConv = new FFMpegConverter();
    } catch (AudioConverterInitializationException e) {

      e.printStackTrace();
    }

  }

  public void convert(File sourceFile, File targetFile,
      AudioConversionFormat targetFormat) throws ConversionException {

    // Default: Don't overwrite

    this.convert(sourceFile, targetFile, targetFormat, false);

  }

  public String convert(File sourceFile, File targetFile,
      AudioConversionFormat targetFormat, boolean overwrite)
      throws ConversionException {
    return this.convert(sourceFile, targetFile, targetFile.getParentFile(),
        targetFormat, false);
  }

  public String convert(File sourceFile, File targetDir, File tmpDir,
      AudioConversionFormat targetFormat, boolean overwrite)
      throws ConversionException {
    String result = null;
    Map<String, String> parameters = null;

    if (targetFormat.getFileEnding().equals("A18")) {
      parameters = getParameters(targetFormat);
      result = AnyToA18Conv.convertFile(sourceFile, targetDir, tmpDir,
          overwrite, parameters);
    }
    if (targetFormat.getFileEnding().equals("WAV") || targetFormat.getFileEnding().equals("MP3")) {
      if (getFileExtension(sourceFile).equalsIgnoreCase(".a18")) {
        parameters = getParameters(targetFormat);
        if (targetFormat.getFileEnding().equals("WAV")) {
          result = A18ToWAVConv.convertFile(sourceFile, targetDir, tmpDir, overwrite, parameters);
        } else {
          result = A18ToMP3Conv.convertFile(sourceFile, targetDir, tmpDir, overwrite, parameters);
        }
      } else {
        parameters = getParameters(targetFormat);
        result = FFMpegConv.convertFile(sourceFile, targetDir, tmpDir, overwrite, parameters, "." + targetFormat.getFileEnding());
      }
    }
    return result;
  }

  public static String getFileExtension(File file) {
    String name = file.getName();
    return name.substring(name.length() - 4, name.length()).toLowerCase();
  }

  private Map<String, String> getParameters(AudioConversionFormat targetFormat) {

    Map<String, String> parameters = new LinkedHashMap<String, String>();

    parameters.put(BaseAudioConverter.BIT_RATE,
        String.valueOf(targetFormat.getBitRateString()));
    parameters.put(BaseAudioConverter.SAMPLE_RATE,
        String.valueOf(targetFormat.getSampleRateString()));

    if (targetFormat.getFileEnding().equals("A18")) {

      parameters.put(AnyToA18Converter.USE_HEADER,
          ((A18Format) targetFormat).usedHeader);
      parameters.put(AnyToA18Converter.ALGORITHM,
          ((A18Format) targetFormat).usedAlgo);
    }
    return parameters;
  }

}
