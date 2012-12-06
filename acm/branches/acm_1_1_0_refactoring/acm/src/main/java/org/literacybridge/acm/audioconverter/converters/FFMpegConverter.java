package org.literacybridge.acm.audioconverter.converters;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;

public class FFMpegConverter extends BaseAudioConverter {
	final static String TARGET_EXTENSION = ".wav";
	
	public FFMpegConverter() {
		super(TARGET_EXTENSION);
	}

	@Override
	public ConversionResult doConvertFile(File audioFile, File targetDir, File targetFile, Map<String, String> parameters)
			throws ConversionException {
		String cmd = getConverterEXEPath()
						+ " -i \"" + audioFile.getAbsolutePath() + "\""
						+ " -ab 16"
						+ " -ar 16000"   // 16000 sampling rate
						+ " -ac 1"       // 1 channel = mono
						+ " -y"          // overwrite output file
						+ " \"" + targetFile.getAbsolutePath() + "\"";

		ConversionResult result = new ConversionResult();
		result.outputFile = targetFile;
		result.response = BaseAudioConverter.executeConversionCommand(cmd,
																	  true,  // important! ffmpeg prints to stderr, not stdout
																	  audioFile.getName());
		
		return result;
	}

	public void validateConverter() throws AudioConverterInitializationException {
		BaseAudioConverter.validateConverterExecutable(getConverterEXEPath(), 
													   true,   // important! ffmpeg prints to stderr, not stdout
													   "FFmpeg version");
		boolean success = false;
		try {
			File formatsFile = new File(System.getProperty("user.dir") + "/converters/ffmpeg/formats.txt");
			
			FileInputStream fis = new FileInputStream(formatsFile);
			InputStreamReader isr = new InputStreamReader(fis);
			BufferedReader br = new BufferedReader(isr);
			String line = null;

			// parse 'ffmpeg -formats output', it starts like this:
			
			// File formats:
			//   E 3g2             3GP2 format
			//   E 3gp             3GP format
			//  D  4xm             4X Technologies format
			//  D  IFF             IFF format
			//  D  MTV             MTV format
			//  DE RoQ             id RoQ format
			
			line = br.readLine();
			// first line should be: "File Formats:"
			if (!line.trim().equals("File formats:")) {
				success = false;
				return;
			}
			
			Pattern pattern = Pattern.compile("^(.)(.)(.)\\s*(\\S*).*$");
			
			// pick every file extensions that has a 'D', which indicates decoding ability
			while ((line = br.readLine()) != null) {
				Matcher matcher = pattern.matcher(line);
				if (!matcher.matches()) {
					break;
				}
				if (matcher.group(2).equals("D")) {
					String extension = matcher.group(4).toLowerCase();
					StringTokenizer tokenizer = new StringTokenizer(extension, ",");
					while (tokenizer.hasMoreTokens()) {
						EXTENSIONS.add(tokenizer.nextToken());
					}
				}
			}
			
			success = true;
		} catch (Exception e) {
			// nothing to do - finally block will throw exception
		} finally {
			if (!success) {
				throw new AudioConverterInitializationException("Converter executable could not be executed.");
			}
		}

	}
	
	public String getConverterEXEPath() {
		// as we assume our converter directory located in the same
		// directory as we are, get out location
		return System.getProperty("user.dir") + "/converters/ffmpeg/ffmpeg.exe";
	}

	@Override
	public String getShortDescription() {
		return "Convert any audio file to .wav";
	}

	private static final Set<String> EXTENSIONS = new HashSet<String>();
	
	@Override
	public Set<String> getSourceFileExtensions() {
		return EXTENSIONS;
	}
}
