package org.literacybridge.acm.audioconverter.converters;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.util.UIUtils;
import org.literacybridge.acm.utils.IOUtils;

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
	public ConversionResult doConvertFile(File audioFile, File targetDir, File targetFile, File tmpDir, Map<String, String> parameters) throws ConversionException {
		File tmpSourceFile = null;
		File ultimateDestDir = null;
		
		try {
			if (tmpDir != null && !tmpDir.equals(audioFile.getParentFile())) {
				tmpSourceFile = new File(tmpDir, audioFile.getName());
				try {
					IOUtils.copy(audioFile, tmpSourceFile);
					// the a18 converter stores intermediate files in the source directory, which is often undesired (e.g. when using dropbox)
					// copy the source file to a temp directory first
					audioFile = tmpSourceFile;
				} catch (IOException e) {
					// ignore and use original source file
				}
			}
			
			if (tmpDir != null && !tmpDir.equals(targetDir) && targetDir.getAbsolutePath().startsWith(ACMConfiguration.getCurrentDB().getSharedACMDirectory().getAbsolutePath())) {
				// targetDir should never be in Dropbox because of some strange interaction with Dropbox that has caused bad A18 output 
				ultimateDestDir = targetDir;
				targetDir = tmpDir;
			}
			
			String cmd = getCommand(audioFile, targetDir, parameters);
			ConversionResult result = new ConversionResult();
			result.outputFile = new File(targetDir, audioFile.getName() + targetFormatExtension);
			if (ultimateDestDir != null && result.outputFile.exists()) {
				result.outputFile.delete(); 
			}
			result.response = BaseAudioConverter.executeConversionCommand(cmd,
															  false,  // important! a18 converter prints to stdout, not stderr
															  audioFile.getName());
			
			if (ultimateDestDir != null) {
				if (result.outputFile.renameTo(targetFile)) {
					result.outputFile = targetFile;				
				}
			}
			Matcher matcher = pattern.matcher(result.response);
			if (matcher.matches()) {
				if (!result.response.endsWith("Succeeded")) {
					throw new ConversionException("Converter: Internal error while converting file: '" + audioFile.getName() + "'");
				}
			}
			return result;
		} finally {
			if (tmpSourceFile != null && tmpSourceFile.exists()) {
				tmpSourceFile.delete();
			}
			if (ultimateDestDir != null) {
				File tmpDest = new File (tmpDir,audioFile.getName() + targetFormatExtension);
				if (tmpDest.exists()) {
					tmpDest.delete();
				}
			}
		}
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
