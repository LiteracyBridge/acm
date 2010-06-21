package org.literacybridge.audioconverter.api;

public class WAVFormat extends AudioConversionFormat {

	public WAVFormat (int BitDepth, float SampleRate, int Channels){
		
		super(BitDepth, SampleRate, Channels);
				
		this.setFileEnding("WAV");
				
	}
}
