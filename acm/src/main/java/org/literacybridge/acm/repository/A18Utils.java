package org.literacybridge.acm.repository;

import java.io.*;
import java.util.Collection;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.repository.AudioItemRepository.AudioFormat;
import org.literacybridge.acm.store.*;
import org.literacybridge.acm.utils.IOUtils;

public class A18Utils {
  public static void updateDuration(AudioItem audioItem) throws IOException {
      AudioItemRepository repository = ACMConfiguration.getInstance().getCurrentDB().getRepository();
      File f = null;
      if (repository.hasAudioFileWithFormat(audioItem, AudioFormat.A18)) {
          f = repository.findFileWithFormat(audioItem, AudioFormat.A18);
      }

    if (f != null) {
      DataInputStream in = new DataInputStream(
          new BufferedInputStream(new FileInputStream(f)));
      in.skipBytes(4);
      int bps = IOUtils.readLittleEndian16(in);
      in.close();
      long sec = (f.length() * 8 + bps / 2) / bps;
      int min = (int) (sec / 60L);
      // String test = String.valueOf(sec) + "/" + String.valueOf(bps) + "/" +
      // String.valueOf(f.length());
      sec -= min * 60;
      String sMin = String.valueOf(min);
      String sSec = String.valueOf(sec);
      if (sMin.length() == 1)
        sMin = "0" + sMin;
      if (sSec.length() == 1)
        sSec = "0" + sSec;
      String duration = sMin + ":" + sSec + ((bps == 16000) ? "  l" : "  h");
      // duration += test;
      audioItem.getMetadata().putMetadataField(
          MetadataSpecification.LB_DURATION,
          new MetadataValue<String>(duration));
      ACMConfiguration.getInstance().getCurrentDB().getMetadataStore()
          .commit(audioItem);
    }
  }

  public static void appendMetadataToA18(AudioItem audioItem, File a18File) throws IOException {
      Metadata metadata = audioItem.getMetadata();
      Collection<Category> categories = audioItem.getCategoryList();

      appendMetadataToA18(metadata, categories, a18File);
  }

  public static void appendMetadataToA18(Metadata metadata, Collection<Category> categories, File a18File) throws IOException {
      try (FileOutputStream fos = new FileOutputStream(a18File,true);
           BufferedOutputStream bos = new BufferedOutputStream(fos);
           DataOutputStream out = new DataOutputStream(bos)) {
          LBMetadataSerializer serializer = new LBMetadataSerializer();
          serializer.serialize(categories, metadata, out);
      }
  }

    public static void copyA18WithoutMetadata(File fromFile, File toFile) throws IOException {
        DataInputStream in = new DataInputStream(new FileInputStream(fromFile));
        int numBytes = IOUtils.readLittleEndian32(in);
        in.close();
        IOUtils.copy(fromFile, toFile, numBytes + 4);
    }
}
