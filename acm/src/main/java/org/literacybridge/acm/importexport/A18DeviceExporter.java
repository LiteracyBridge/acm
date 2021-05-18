package org.literacybridge.acm.importexport;

import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter.ConversionException;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.device.DeviceInfo;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.repository.AudioItemRepositoryImpl;
import org.literacybridge.acm.store.AudioItem;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class A18DeviceExporter {
  private static final Logger LOG = Logger
      .getLogger(A18DeviceExporter.class.getName());

  private static final String INBOX_SUB_DIR = "inbox/messages";

    /**
     * Determine if any audio item can be saved to the device.
     * @param device The path to the device in the file system.
     * @return True if an item may be saved to the device, false if the device is not properly configured.
     */
  public static boolean canExportToDevice(DeviceInfo device) {
      File deviceLocation = device.getPathToDevice();
      deviceLocation = new File(deviceLocation, INBOX_SUB_DIR);

      return deviceLocation.exists() && deviceLocation.isDirectory();
  }

  /**
   * Exports an audio item to a connected Talking Book device.
   * @param item The audio item to be exported.
   * @param device The path to the device in the file system.
   * @return true if the item was successfully exported, false otherwise
   * @throws IOException If there was an error writing to the device.
   */
  public static boolean exportToDevice(AudioItem item, DeviceInfo device)
      throws IOException {
    File deviceLocation = device.getPathToDevice();
    deviceLocation = new File(deviceLocation, INBOX_SUB_DIR);

    if (!deviceLocation.exists() || !deviceLocation.isDirectory()) {
      return false;
    }

    try {
      AudioItemRepositoryImpl repository = (AudioItemRepositoryImpl) ACMConfiguration.getInstance()
              .getCurrentDB()
              .getRepository();
      repository.exportAudioFileWithFormat(item,
              new File(deviceLocation, repository.getAudioFilename(item, AudioItemRepository.AudioFormat.A18)),
              AudioItemRepository.AudioFormat.A18);
    } catch (ConversionException | AudioItemRepository.UnsupportedFormatException e) {
      LOG.log(Level.WARNING, "Error converting audio file.", e);
      return false;
    }

    // success
    return true;
  }
}
