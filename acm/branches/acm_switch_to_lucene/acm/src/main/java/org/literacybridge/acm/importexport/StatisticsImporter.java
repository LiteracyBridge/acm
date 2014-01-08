package org.literacybridge.acm.importexport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.literacybridge.acm.db.PersistentAudioItem;
import org.literacybridge.acm.db.PersistentMetadata;
import org.literacybridge.acm.metadata.MetadataSpecification;

public class StatisticsImporter {
	public void importStatsFolder(File folder) throws IOException {
		File[] files = folder.listFiles(new FilenameFilter() {			
			@Override public boolean accept(File dir, String name) {
				if (name.toLowerCase().endsWith(".csv")) {
					return true;
				}
				
				return false;
			}
		});
		
		for (File file : files) {
			importStatsFile(file);
		}
	}
	
	public void importStatsFile(File file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		try {
			List<String> tokens = tokenizeCSV(reader.readLine());
			String deviceId = tokens.get(0);
			int bootCycleNumber = Integer.parseInt(tokens.get(1));
			
			while(reader.ready()) {
				tokens = tokenizeCSV(reader.readLine());
				String audioItemID = tokens.get(0); 
				PersistentAudioItem audioItem = PersistentAudioItem.getFromDatabase(audioItemID);
				if (audioItem != null) {
					PersistentMetadata metadata = audioItem.getPersistentMetadata();
					
					metadata.setStatistic(MetadataSpecification.LB_OPEN_COUNT, deviceId, 
							bootCycleNumber, Integer.parseInt(tokens.get(1)));
					metadata.setStatistic(MetadataSpecification.LB_COMPLETION_COUNT, deviceId, 
							bootCycleNumber, Integer.parseInt(tokens.get(2)));
					metadata.setStatistic(MetadataSpecification.LB_COPY_COUNT, deviceId, 
							bootCycleNumber, Integer.parseInt(tokens.get(3)));
					metadata.setStatistic(MetadataSpecification.LB_SURVEY1_COUNT, deviceId, 
							bootCycleNumber, Integer.parseInt(tokens.get(4)));
					metadata.setStatistic(MetadataSpecification.LB_APPLY_COUNT, deviceId, 
							bootCycleNumber, Integer.parseInt(tokens.get(5)));
					metadata.setStatistic(MetadataSpecification.LB_NOHELP_COUNT, deviceId, 
							bootCycleNumber, Integer.parseInt(tokens.get(6)));
					
					metadata.commit();
				}
			}
		} finally {
			reader.close();
		}
	}
	
	private final List<String> tokenizeCSV(String line) {
		final StringTokenizer tokenizer = new StringTokenizer(line, " ,");
		List<String> list = new ArrayList<String>();
		while (tokenizer.hasMoreTokens()) {
			list.add(tokenizer.nextToken());
		}
		return list;
	}
}
