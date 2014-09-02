package org.literacybridge.acm.tbbuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;

import org.literacybridge.acm.Constants;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.DBConfiguration;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.CommandLineParams;
import org.literacybridge.acm.metadata.MetadataSpecification;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.utils.IOUtils;

public class TBBuilder {
	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			printUsage();
			System.exit(1);
		}
		
		CommandLineParams params = new CommandLineParams();
		params.disableUI = true;
		params.readonly = true;
		params.sandbox = true;
		params.sharedACM = args[0];
		Application.startUp(params);
		
		String packageName = args[1];
		
		System.out.println("\n\nExporting package " + packageName);
		
		
		File sourceDir = new File(ACMConfiguration.getCurrentDB().getTBLoadersDirectory(), "active/"
				+ packageName);

		if (!sourceDir.exists()) {
			System.err.println("Directory not found: " + sourceDir);
			System.exit(1);
		}
		File[] lists = sourceDir.listFiles();

		if (lists.length == 0) {
			System.err.println("No lists found in " + sourceDir);
			System.exit(1);
		}
		
		File tbLoadersDir = new File(DBConfiguration.getLiteracyBridgeHomeDir(), Constants.TBLoadersHomeDir);
		File packageDir = new File(tbLoadersDir, packageName);
		File audioTargetDir = new File(packageDir, "basic/messages/audio");
		File listsTargetDir = new File(packageDir, "basic/messages/lists");
		File welcomeMessageTargetDir = new File(packageDir, "basic/languages/dga");

		if (!audioTargetDir.exists() && !audioTargetDir.mkdirs()) {
			System.err.println("Unable to create directory: " + audioTargetDir);
			System.exit(1);			
		}
		
		if (!listsTargetDir.exists() && !listsTargetDir.mkdirs()) {
			System.err.println("Unable to create directory: " + listsTargetDir);
			System.exit(1);			
		}

		if (!welcomeMessageTargetDir.exists() && !welcomeMessageTargetDir.mkdirs()) {
			System.err.println("Unable to create directory: " + welcomeMessageTargetDir);
			System.exit(1);			
		}
		
		for (File list : lists) {
			IOUtils.copy(list, new File(listsTargetDir, list.getName()));
			if (!list.getName().equalsIgnoreCase("_activeLists.txt")) {
				exportList(list, audioTargetDir);				
			}
		}
		
		/*
          _______________________________________ 
          / TERRIBLE HACK EXPLICITLY REQUESTED BY \
          \ CLIFF:                                /
           --------------------------------------- 
                  \   ^__^
                   \  (oo)\_______
                      (__)\       )\/\
                          ||----w |
                          ||     ||
                          
		 * Full text search for the first audio item whose title matches the package name.  
		 */

		/* COMMENTING OUT THE TERRIBLE HACK: Instead, any playlist can be exported to the Intro Message Category */
//		List<AudioItem> items = AudioItem.getFromDatabaseBySearch(packageName, null, null);
//		for (AudioItem item : items) {
//			String title = item.getLocalizedAudioItem(null).getMetadata().getMetadataValues(MetadataSpecification.DC_TITLE).get(0).getValue();
//			if (title.trim().equals(packageName.trim())) {
//				System.out.println("\nExporting audioitem " + item.getUuid() + " (" + title + ") to " + welcomeMessageTargetDir);
//				ACMConfiguration.getCurrentDB().getRepository().exportA18WithMetadata(item, welcomeMessageTargetDir);
//			}
//		}
		
		System.out.println("\nDone.");
	}
	
	private static void printUsage() {
		System.out.println("Usage: java -cp acm.jar:lib/* org.literacybridge.acm.tbbuilder.TBBuilder <acm_name> <package_name>");
	}
	
	private static void exportList(File list, File targetDirectory) throws Exception {
		System.out.println("  Exporting list " + list);
		AudioItemRepository repository = ACMConfiguration.getCurrentDB().getRepository();
		BufferedReader reader = new BufferedReader(new FileReader(list));
		
		try {
			while (reader.ready()) {
				String uuid = reader.readLine();
				AudioItem audioItem = AudioItem.getFromDatabase(uuid);
				System.out.println("    Exporting audioitem " + uuid + " to " + targetDirectory);
				repository.exportA18WithMetadata(audioItem, targetDirectory);
			}
		} finally {
			reader.close();
		}
	}
}
