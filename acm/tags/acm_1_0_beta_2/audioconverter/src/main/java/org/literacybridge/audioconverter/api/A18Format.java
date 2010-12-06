package org.literacybridge.audioconverter.api;

// import javax.sound.sampled.AudioFormat;

public class A18Format extends AudioConversionFormat{

	public String usedAlgo;
	public String usedHeader;
	
	public enum AlgorithmList {
		A1600,
		A1800,
		A3600
	}
	
	public enum useHeaderChoice {
		Yes,
		No
	}
		
	
	public A18Format (int BitDepth, float SampleRate, int Channels, AlgorithmList usedAlgo, useHeaderChoice usedHeader){
		
		super(BitDepth, SampleRate, Channels);
		
		this.usedAlgo = String.valueOf(usedAlgo);
		this.usedHeader = String.valueOf(usedHeader);
		
		this.setFileEnding("A18");	
	}
	

}
