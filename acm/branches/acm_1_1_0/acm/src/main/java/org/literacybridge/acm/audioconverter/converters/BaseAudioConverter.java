package org.literacybridge.acm.audioconverter.converters;

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
	
	public String convertFile(File inputFile, File targetDir, File tmpDir, boolean overwrite, Map<String, String> parameters) throws ConversionException {
		return convertFile(inputFile, targetDir, tmpDir, overwrite, parameters, targetFormatExtension);
	}
	
	public String convertFile(File inputFile, File targetDir, File tmpDir, boolean overwrite, Map<String, String> parameters, String targetExtension) throws ConversionException {
		if (tmpDir == null) {
			tmpDir = targetDir;
		}
		File outputFile = new File(targetDir, getFileNameWithoutExtension(inputFile.getName()) + targetExtension);
		if (outputFile.exists())
			if (!overwrite) {
				return null;
			} else {
				if (!outputFile.delete()) {
					// TODO: we should probably have some retry logic here, and fail after 5 attempts or so
					throw new ConversionException("Unable to overwrite output file.");
				}
		}
		ConversionResult result = doConvertFile(inputFile, targetDir, outputFile, tmpDir, parameters);
		if (!result.outputFile.getAbsolutePath().equals(outputFile.getAbsolutePath())) {
			// TODO: we should probably have some retry logic here, and fail after 5 attempts or so
			if (!result.outputFile.renameTo(outputFile)) {
				throw new ConversionException("Unable to rename output file.");
			}
		}
		return result.response;
	}
	
	public abstract ConversionResult doConvertFile(File inputFile, File targetDir, File targetFile, File tmpDir, Map<String, String> parameters) throws ConversionException;
	
	// @return short description for the format comboBox, e.g. ".a18 to .wav format"
	public abstract String getShortDescription();
	
	// @return get source file extension, e.g. a18
	public abstract Set<String> getSourceFileExtensions();
	
	public abstract void validateConverter() throws AudioConverterInitializationException;
	
	static String getFileNameWithoutExtension(String fullName) {
		return fullName.substring(0, fullName.lastIndexOf('.'));
	}
	
	static String executeConversionCommand(String cmd, boolean listenToStdErr, String inputFileName) throws ConversionException {
		StringBuilder responseBuilder = new StringBuilder();
		boolean success = false;
		try {
			Process proc = Runtime.getRuntime().exec(cmd);
	
			InputStream stderr = listenToStdErr ? proc.getErrorStream() : proc.getInputStream(); 
			InputStreamReader isr = new InputStreamReader(stderr);
			BufferedReader br = new BufferedReader(isr);
			String line = null;
		
			while ((line = br.readLine()) != null) {
				responseBuilder.append(line);
				responseBuilder.append('\n');
			}
			
			// check for converter error
			success = (proc.waitFor() == 0);
		} catch (Exception e) {
			success = false;
		} finally {
			if (!success) {
				throw new ConversionException("Converter: Internal error while converting file: '" + inputFileName + "'");
			}
		}
			
		return responseBuilder.toString();
	}
	
	static void consumeProcessOutput(Process proc, boolean listenToStdErr) throws IOException {
		InputStream stderr = listenToStdErr ? proc.getErrorStream() : proc.getInputStream(); 
		InputStreamReader isr = new InputStreamReader(stderr);
		BufferedReader br = new BufferedReader(isr);
		String line = null;
	
		while ((line = br.readLine()) != null);
	}

	static void validateConverterExecutable(String exePath, boolean listenToStdErr, String outputPrefix) throws AudioConverterInitializationException {
		File exeFile = new File(exePath);
		if (!exeFile.exists()) {
			throw new AudioConverterInitializationException("Converter executable not found.");
		}

		boolean success = false;
		try {
			String cmd = exePath;

			Process proc = Runtime.getRuntime().exec(cmd);
			InputStream stderr = listenToStdErr ? proc.getErrorStream() : proc.getInputStream();
			InputStreamReader isr = new InputStreamReader(stderr);
			BufferedReader br = new BufferedReader(isr);
			String line = null;

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
				throw new AudioConverterInitializationException("Converter executable could not be executed.");
			}
		}
	}
	
	public static class AudioConverterException extends Exception {
		private static final long serialVersionUID = 1L;

		public AudioConverterException(String msg) {
			super(msg);
		}
	}

	
	public static class AudioConverterInitializationException extends AudioConverterException {
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
	
	static class ConversionResult {
		public String response;
		public File outputFile;
	}
	
}
