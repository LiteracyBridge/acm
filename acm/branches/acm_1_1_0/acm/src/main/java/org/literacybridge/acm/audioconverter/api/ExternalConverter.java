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
	
	Map<String, String> parameters = new LinkedHashMap<String, String>();
	
	public ExternalConverter()  
	{
		try {
			A18ToMP3Conv = new A18ToMP3Converter();
			A18ToWAVConv = new A18ToWavConverter();
			AnyToA18Conv = new AnyToA18Converter();
			FFMpegConv = new FFMpegConverter();
		} catch (AudioConverterInitializationException e) {
			
			e.printStackTrace();
		}

	}
	
	public void convert(File sourceFile, File targetFile, AudioConversionFormat targetFormat) 
		throws ConversionException {
		
		// Default: Don't overwrite
		
		this.convert(sourceFile, targetFile, targetFormat, false);
		
	}
	
	
	public String convert(File sourceFile, File targetFile, AudioConversionFormat targetFormat, boolean overwrite) 
		throws ConversionException {
		String result = null;
		
		if (targetFormat.getFileEnding()== "A18")
		{
				SetParameters(targetFormat);
				result = AnyToA18Conv.convertFile(sourceFile, targetFile, overwrite, this.parameters);
		}
		if (targetFormat.getFileEnding() == "WAV" || targetFormat.getFileEnding() == "MP3")
		{
			if (getFileExtension(sourceFile).equalsIgnoreCase(".a18")) {
				SetParameters(targetFormat);
				result = A18ToWAVConv.convertFile(sourceFile, targetFile, overwrite, this.parameters);
			} else {
				SetParameters(targetFormat);
				result = FFMpegConv.convertFile(sourceFile, targetFile, overwrite, this.parameters, "." + targetFormat.getFileEnding());
			}
		}
		return result;
	}
	
	public static String getFileExtension(File file) {
		String name = file.getName();
		return name.substring(name.length() - 4, name.length()).toLowerCase();
	}
	
	private void SetParameters(AudioConversionFormat paramFormat) {
		
		parameters.clear();
		
		parameters.put(BaseAudioConverter.BIT_RATE, String.valueOf(paramFormat.getBitRateString()));
		parameters.put(BaseAudioConverter.SAMPLE_RATE, String.valueOf(paramFormat.getSampleRateString()));
		
		if (paramFormat.getFileEnding() == "A18")
		{

			parameters.put(AnyToA18Converter.USE_HEADER, ((A18Format)paramFormat).usedHeader);
			parameters.put(AnyToA18Converter.ALGORITHM, ((A18Format)paramFormat).usedAlgo);
		}
	}
	
}

