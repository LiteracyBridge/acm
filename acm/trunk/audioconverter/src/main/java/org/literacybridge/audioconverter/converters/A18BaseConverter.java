package org.literacybridge.audioconverter.converters;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;

public abstract class A18BaseConverter extends BaseAudioConverter {

	// extracted conversion result
	private Pattern pattern = getPattern();
	
	public A18BaseConverter(String targetFormatExtension) throws AudioConverterInitializationException {
		super(targetFormatExtension);
	}
	
	protected abstract Pattern getPattern();
	
	public String getConverterEXEPath() {
		// as we assume our converter directory located in the same
		// directory as we are, get out location
		return System.getProperty("user.dir") + "/converters/a18/AudioBatchConverter.exe";
	}
	
	public void validateConverter() throws AudioConverterInitializationException {
		BaseAudioConverter.validateConverterExecutable(getConverterEXEPath(), false, "Generalplus Audio Batch Converter Tool");
	}
	
	protected abstract String getCommand(File audioFile, File targetFile, Map<String, String> parameters);
	
	@Override
	public ConversionResult doConvertFile(File audioFile, File targetDir, File targetFile, Map<String, String> parameters) throws ConversionException {
	
		String cmd = getCommand(audioFile, targetDir, parameters);
		ConversionResult result = new ConversionResult();
		result.outputFile = new File(targetDir, audioFile.getName() + targetFormatExtension);
		result.response = BaseAudioConverter.executeConversionCommand(cmd,
														  false,  // important! a18 converter prints to stdout, not stderr
														  audioFile.getName());
	
		Matcher matcher = pattern.matcher(result.response);
		if (matcher.matches()) {
			if (!result.response.endsWith("Succeeded")) {
				throw new ConversionException("Converter: Internal error while converting file: '" + audioFile.getName() + "'");
			}
		}
		return result;
	}

	@Override
	public String getShortDescription() {
		return "Convert *.a18 to WAV audio files";
	}

	private static final Set<String> EXTENSIONS = new HashSet<String>();
	static {
		EXTENSIONS.add("a18");
	}
	
	@Override
	public Set<String> getSourceFileExtensions() {
		return EXTENSIONS;
	}
}
