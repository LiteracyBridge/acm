package org.literacybridge.acm.audioconverter.api;


import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.store.AudioItem;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

public class AudioVolumeModifier {

    public void modifyAudio(AudioItem audioItem, String newVolume) throws BaseAudioConverter.ConversionException, AudioItemRepository.UnsupportedFormatException, IOException {
        File f = ACMConfiguration.getInstance().getCurrentDB().getRepository()
                .getAudioFile(audioItem, AudioItemRepository.AudioFormat.WAV);

        String outputFilePath = f.getAbsolutePath();
        try {
            // Open the input audio file from the file system
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(f);
            AudioFormat audioFormat = audioInputStream.getFormat();

            // Read the audio data into a byte array
            byte[] audioData = new byte[(int) (audioInputStream.getFrameLength() * audioFormat.getFrameSize())];
            audioInputStream.read(audioData);

            // Modify the volume of the audio data
            float volume = Float.parseFloat(newVolume);
            modifyVolume(audioData, volume);

            // Create a new AudioInputStream with the modified audio data
            AudioInputStream modifiedAudioInputStream = new AudioInputStream(
                    new ByteArrayInputStream(audioData), audioFormat, audioData.length / audioFormat.getFrameSize());

            // Save the modified audio to a new file
            AudioSystem.write(modifiedAudioInputStream, AudioFileFormat.Type.WAVE, new File(outputFilePath));

            // Close the input stream
            audioInputStream.close();
            modifiedAudioInputStream.close();

            System.out.println("Volume modification completed. Output saved to " + outputFilePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void modifyVolume(byte[] audioData, float volume) {
        for (int i = 0; i < audioData.length; i += 2) {
            // Assuming 16-bit audio, modify the left and right channels separately
            short sample = (short) ((audioData[i + 1] << 8) | (audioData[i] & 0xFF));
            sample = (short) (sample * volume);
            audioData[i] = (byte) sample;
            audioData[i + 1] = (byte) (sample >> 8);
        }
    }
}

