package org.literacybridge.acm.tools;

import java.io.File;
import java.io.IOException;

import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.content.LocalizedAudioItem;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.CommandLineParams;
import org.literacybridge.acm.importexport.CSVExporter;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

public class CSVDatabaseExporter {
	private CSVDatabaseExporter(String acmName) throws Exception {
		CommandLineParams params = new CommandLineParams();
		params.disableUI = true;
		params.readonly = true;
		params.sandbox = true;
		params.disableIndex = true;
		params.sharedACM = acmName;
		Application.startUp(params);
	}

	private void export(File csvFile) throws IOException {
		CSVExporter.export(Lists.transform(AudioItem.getFromDatabase(), new Function<AudioItem, LocalizedAudioItem>() {
			@Override public LocalizedAudioItem apply(AudioItem item) {
				return item.getLocalizedAudioItem(null);
			}
		}), csvFile);
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			printUsage();
			System.exit(1);
		}

		CSVDatabaseExporter exporter = new CSVDatabaseExporter(args[0]);
		exporter.export(new File(args[1]));
	}

	private static void printUsage() {
		System.out.println("Usage: java -cp acm.jar:lib/* org.literacybridge.acm.tools.CSVDatabaseExporter <acm_name> <csv_file>");
	}


}
