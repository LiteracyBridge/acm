package org.literacybridge.acm.audioconverter.converters;

import java.io.File;
import java.util.regex.Pattern;
import java.util.Map;

public class A18ToWavConverter extends A18BaseConverter {
	public A18ToWavConverter() throws AudioConverterInitializationException {
		super(".wav");
	}

	@Override
	protected Pattern getPattern() {
		return Pattern.compile("^Decode\\s'([^']*)'.*");
	}

	@Override
	protected String getCommand(File audioFile, File targetFile, Map<String, String> parameters) {
		StringBuffer command = new StringBuffer();
		command.append(getConverterEXEPath());
		command.append(" -d ");
                
		for (String key : parameters.keySet()) {
			if (key.equals(A18BaseConverter.BIT_RATE)) {
				// The -b option is only used on encoding wav->a18, not the decode option of a18->wav 
				// command.append(" -b " + parameters.get(key));
			} else if (key.equals(A18BaseConverter.SAMPLE_RATE)) {
				command.append(" -s " + parameters.get(key));
			}
		}		
		command.append(" -o \"" + targetFile.getAbsolutePath()+ "\"");
		command.append(" \"" + audioFile.getAbsolutePath() + "\"");		
		
		return command.toString();
	}
}
