package org.literacybridge.acm.audioconverter.converters;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;

public class A18ToMP3Converter extends BaseAudioConverter {
	private FFMpegConverter ffmpegConverter = new FFMpegConverter();
	private A18ToWavConverter a18ToWavConverter = new A18ToWavConverter();
	
	public A18ToMP3Converter() throws AudioConverterInitializationException {
		super(".mp3");
	}

	@Override
	public ConversionResult doConvertFile(File inputFile, File targetDir,
			File targetFile, Map<String, String> parameters) throws ConversionException {
		File tmp = null;
		try {
			ConversionResult r1 = a18ToWavConverter.doConvertFile(inputFile, targetDir, targetFile, parameters);
			tmp = r1.outputFile;
			ConversionResult r2 = ffmpegConverter.doConvertFile(r1.outputFile, targetDir, targetFile, parameters);
			
			r2.response = r1.response + "\n" + r2.response;
			return r2;
		} finally {
			if (tmp != null) {
				tmp.delete();
			}
		}
	}

	@Override
	public String getShortDescription() {
		return "Convert *.a18 audio file to .mp3";
	}

	private static final Set<String> EXTENSIONS = new HashSet<String>();
	static {
		EXTENSIONS.add("a18");
	}
	
	@Override
	public Set<String> getSourceFileExtensions() {
		return EXTENSIONS;
	}

	@Override
	public void validateConverter()
			throws AudioConverterInitializationException {
		ffmpegConverter.validateConverter();
		a18ToWavConverter.validateConverter();
	}

}
