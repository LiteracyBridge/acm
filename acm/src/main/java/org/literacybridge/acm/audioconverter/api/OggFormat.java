package org.literacybridge.acm.audioconverter.api;

public class OggFormat extends AudioConversionFormat {

  public OggFormat(int BitDepth, float SampleRate, int Channels) {

    super(BitDepth, SampleRate, Channels);

    this.setFileEnding("OGG");

  }

}
