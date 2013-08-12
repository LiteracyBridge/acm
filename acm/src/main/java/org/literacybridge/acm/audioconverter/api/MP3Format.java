package org.literacybridge.acm.audioconverter.api;

public class MP3Format extends AudioConversionFormat {

		public MP3Format (int BitDepth, float SampleRate, int Channels){
			
			super(BitDepth, SampleRate, Channels);
					
			this.setFileEnding("MP3");
					
		}
		
		
	}

	

