package org.literacybridge.acm.audioconverter.converters;

import java.io.File;
import java.util.Map;
import java.util.regex.Pattern;

public class WavToA18Converter extends A18BaseConverter {
  public WavToA18Converter() {
    super(".a18");

  }

  @Override
  protected String getCommand(File audioFile, File targetFile,
      Map<String, String> parameters) {
    StringBuilder command = new StringBuilder();
    command.append(getConverterEXEPath());

    for (String key : parameters.keySet()) {
      if (key.equals(AnyToA18Converter.ALGORITHM)) {
        command.append(" -e ").append(parameters.get(key));
      } else if (key.equals(AnyToA18Converter.USE_HEADER)) {
        command.append(" -h ").append(parameters.get(key));
      }
    }
    command.append(" -b 16000 -o \"").append(targetFile.getAbsolutePath()).append("\"");
    command.append(" \"").append(audioFile.getAbsolutePath()).append("\"");

    return command.toString();
  }

  @Override
  protected Pattern getPattern() {
    return Pattern.compile("^Encode\\s'([^']*)'.*");
  }
}
