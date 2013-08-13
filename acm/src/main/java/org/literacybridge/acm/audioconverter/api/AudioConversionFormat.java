package org.literacybridge.acm.audioconverter.api;

import javax.sound.sampled.AudioFormat;

public abstract class AudioConversionFormat extends AudioFormat {

	private float mBitRate;
	
	private String mFileEnding;

	public AudioConversionFormat (int BitDepth, float SampleRate, int Channels){
	
		super(SampleRate, BitDepth, Channels, false, false);
		
		// super.frameRate = 0;
		// super.frameSize = 0;
		
	}
	
	public String getFileEnding(){
		return mFileEnding;
	}

	public void setFileEnding(String passFileEnding){
		mFileEnding = passFileEnding;
	}

	public float getBitRate(){
		
		// Calculate BitRate 
		CalcBitRate();
		
		return  mBitRate;
	}
	
	public String getBitRateString(){
		
		// Calculate BitRate 
		CalcBitRate();
		
		return  RemoveDecimals(mBitRate);
		
	}
	
	public float getBitDepth()
	{
		return super.sampleSizeInBits;
	}
	
	
	private void CalcBitRate(){
		mBitRate = sampleSizeInBits * channels * sampleRate;
	}
	
	private String RemoveDecimals(float Number) {

		// Convert to String format without decimals
		String returnString = String.valueOf(Number);
		returnString = returnString.substring(0, returnString.indexOf("."));
		
		return  returnString;
	}
	
	public String getSampleRateString()
	{
		return RemoveDecimals(sampleRate);
	}
	
}
