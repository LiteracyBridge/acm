package org.literacybridge.audioconverter;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.literacybridge.audioconverter.api.A18Format;
import org.literacybridge.audioconverter.api.AudioConversionFormat;
import org.literacybridge.audioconverter.api.ExternalConverter;
import org.literacybridge.audioconverter.api.MP3Format;
import org.literacybridge.audioconverter.api.WAVFormat;
import org.literacybridge.audioconverter.converters.BaseAudioConverter.ConversionException;

/**
 * This set contains a list of black box tests that evaluate the proper conversion of
 * different file types into A18.
 */
public class BlackBoxTestSuite {

	// Define external converter
	static	ExternalConverter testConverter;
	static AudioConversionFormat testFormatA18;
	static AudioConversionFormat testFormatWAV;
	static AudioConversionFormat testFormatMP3;
	
	static File MP3File;
	static File WAVFile;
	static File A18File;
	static File targetDir;
	static File outputFile;
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		
		testConverter = new ExternalConverter();
		
		// Prepare converter settings
		float SampleRate = 8000;
		int Channels = 1;
		int BitDepth = 4;		
		A18Format.useHeaderChoice headerC = A18Format.useHeaderChoice.No;
		A18Format.AlgorithmList algoL = A18Format.AlgorithmList.A1800;
		
		// Set source file for test		
		String testDir = System.getProperty("user.dir") + "/junit/";
		MP3File = new File(testDir + "source_mp3.mp3");
		A18File = new File(testDir + "source_a18.a18");
		targetDir = new File(testDir);
		
		// Generate test format for MP3->A18 conversion
		testFormatA18 = new A18Format(BitDepth, SampleRate, Channels, algoL, headerC);

		// Generate test format for A18->WAV conversion
		testFormatWAV = new WAVFormat(BitDepth, SampleRate, Channels);

		// Generate test format for A18->MP3 conversion
		testFormatMP3 = new MP3Format(BitDepth, SampleRate, Channels);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		
		// Delete a18 output file
		try
		{
			outputFile.delete();
		}
		catch(Exception e)
		{
			System.out.println("Exception " + e.getMessage() + " was thrown when deleting output files." +
					"Please delete files manually. ");
		}

	}
	
	/**
	 * @throws ConversionException 
	 * @throws java.lang.Exception
	 */
	
	@Test
	public void convertMP3toA18() throws ConversionException {
			
		// Convert MP3 file to A18 and wait for exceptions
		testConverter.convert(MP3File, targetDir, testFormatA18);
		
		// Verify output file exists
		
		outputFile = new File(targetDir + "\\source_mp3.a18"); 
		
		assertTrue("Verify if target file " + outputFile + " is created.", outputFile.exists());
		
	}
	
	@Test
	public void convertA18toWAV() throws ConversionException {
		
		// Convert A18 file to WAV and wait for exceptions
		testConverter.convert(A18File, targetDir, testFormatWAV);
		
		// Verify output file exists
		
		outputFile = new File(targetDir + "\\source_a18.wav"); 
		
		assertTrue("Verify if target file " + outputFile + " is created.", outputFile.exists());
		
	}
	
	@Test
	public void convertA18toMP3() throws ConversionException {
		
		// Convert A18 file to WAV and wait for exceptions
		testConverter.convert(A18File, targetDir, testFormatMP3);
		
		// Verify output file exists
		
		outputFile = new File(targetDir + "\\source_a18.mp3"); 
		
		assertTrue("Verify if target file " + outputFile + " is created.", outputFile.exists());
		
	}

}
