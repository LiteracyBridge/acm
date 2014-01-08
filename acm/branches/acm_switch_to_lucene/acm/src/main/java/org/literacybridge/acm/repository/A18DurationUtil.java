package org.literacybridge.acm.repository;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.metadata.MetadataSpecification;
import org.literacybridge.acm.metadata.MetadataValue;
import org.literacybridge.acm.repository.AudioItemRepository.AudioFormat;
import org.literacybridge.acm.utils.IOUtils;

public class A18DurationUtil {
	public static void updateDuration(AudioItem audioItem) throws IOException {		
    	File f = ACMConfiguration.getCurrentDB().getRepository().getAudioFile(audioItem, AudioFormat.A18);
    	if (f != null) {
	    	DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(f)));
	    	in.skipBytes(4);
	    	int bps = IOUtils.readLittleEndian16(in);
	    	in.close();
	    	long sec = (f.length() * 8 + bps/2) / bps;
	    	int min = (int)(sec / 60L);
	    	//String test = String.valueOf(sec) + "/" + String.valueOf(bps) + "/" + String.valueOf(f.length());
	    	sec -= min * 60;
	    	String sMin = String.valueOf(min);
	    	String sSec = String.valueOf(sec);
	    	if (sMin.length()==1)
	    		sMin = "0" + sMin;
	    	if (sSec.length()==1)
	    		sSec = "0" + sSec;
	    	String duration = sMin + ":" + sSec + ((bps==16000)?"  l":"  h");
	    	//duration += test;
	    	audioItem.getMetadata().setMetadataField(MetadataSpecification.LB_DURATION, new MetadataValue<String>(duration));
	    	audioItem.commit();
    	}
	}
}
