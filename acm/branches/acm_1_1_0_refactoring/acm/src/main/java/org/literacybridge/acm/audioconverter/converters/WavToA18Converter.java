package org.literacybridge.acm.audioconverter.converters;

import java.io.File;
import java.util.regex.Pattern;
import java.util.Map;

public class WavToA18Converter extends A18BaseConverter {
	public WavToA18Converter() throws AudioConverterInitializationException {
		super(".a18");

	}
	
	@Override
	protected String getCommand(File audioFile, File targetFile, Map<String, String> parameters) {
		StringBuffer command = new StringBuffer();
		command.append(getConverterEXEPath());
		
		for (String key : parameters.keySet()) {
			if (key.equals(AnyToA18Converter.ALGORITHM)) {
				command.append(" -e " + parameters.get(key));
			} else if (key.equals(AnyToA18Converter.USE_HEADER)) {
				command.append(" -h " + parameters.get(key));
			} else if (key.equals(AnyToA18Converter.BIT_RATE)) {
				//command.append(" -b " + parameters.get(key));
			}// else if (key.equals(AnyToA18Converter.SAMPLE_RATE)) {
			//	command.append(" -s " + parameters.get(key));
			//}
		}		
		command.append(" -b 16000 -o \"" + targetFile.getAbsolutePath()+ "\"");
		command.append(" \"" + audioFile.getAbsolutePath() + "\"");

		return command.toString();
	}

	@Override
	protected Pattern getPattern() {
		return Pattern.compile("^Encode\\s'([^']*)'.*");
	}
}
