package org.literacybridge.acm.tbbuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;

import org.literacybridge.acm.Constants;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.DBConfiguration;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.CommandLineParams;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.utils.IOUtils;
import org.literacybridge.acm.utils.ZipUnzip;
import org.apache.commons.io.FileUtils;

public class TBBuilder {
	public static String firstMessageListName = "1";
	public static String IntroMessageID = "0-5";
	private static String IntroMessageListFilename = IntroMessageID + ".txt";
	private static boolean hasIntro = false;
	private static int groupCount = 0;
	
	public static void main(String[] args) throws Exception {
		if (args.length < 4) {
			printUsage();
			System.exit(1);
		}
		
		CommandLineParams params = new CommandLineParams();
		params.disableUI = true;
		params.readonly = true;
		params.sandbox = true;
		params.sharedACM = args[0];
		String deploymentNumber = args[1];
		final String packageName = args[2];
		String languageCode = args[3];
		String[] groups = new String[5];
		groupCount = args.length - 4;
		for (int i=0;i<groupCount && i<5;i++) {
			groups[i]=args[i+4];
		}
				
		Application.startUp(params);
		
		
		System.out.println("\n\nExporting package " + packageName);
		
		File dropboxTbLoadersDir = ACMConfiguration.getCurrentDB().getTBLoadersDirectory();
		File sourcePackageDir = new File(dropboxTbLoadersDir,"packages/" + packageName);
		File sourceMessagesDir = new File(sourcePackageDir,"messages");
		File sourceListsDir = new File(sourceMessagesDir,"lists/" + TBBuilder.firstMessageListName);
		
		// use LB Home Dir to create folder, then zip to Dropbox and delete the folder
		File localTbLoadersDir = new File(DBConfiguration.getLiteracyBridgeHomeDir(), Constants.TBLoadersHomeDir);
		File targetTempDir = new File(localTbLoadersDir,"temp");
		File targetDeploymentDir = new File(targetTempDir, "content/" + deploymentNumber);
		File targetImagesDir = new File(targetDeploymentDir,"images");
		File targetImageDir = new File(targetImagesDir,packageName);

		IOUtils.deleteRecursive(targetImageDir);
		targetImageDir.mkdirs();

		if (!sourceListsDir.exists()) {
			System.err.println("Directory not found: " + sourceListsDir);
			System.exit(1);
		} else if (sourceListsDir.listFiles().length == 0) {
			System.err.println("No lists found in " + sourceListsDir);
			System.exit(1);
		}

		
		File targetMessagesDir = new File(targetImageDir,"messages");
		FileUtils.copyDirectory(sourceMessagesDir, targetMessagesDir);
		
		File targetAudioDir = new File(targetMessagesDir, "audio");
		File targetListsDir = new File(targetMessagesDir,"lists/" + TBBuilder.firstMessageListName);
		File targetLanguagesDir = new File(targetImageDir,"languages");
		File targetLanguageDir = new File(targetLanguagesDir, languageCode);
		File targetWelcomeMessageDir = targetLanguageDir;

		if (!targetAudioDir.exists() && !targetAudioDir.mkdirs()) {
			System.err.println("Unable to create directory: " + targetAudioDir);
			System.exit(1);				
		}
		
		if (!targetLanguageDir.exists() && !targetLanguageDir.mkdirs()) {
			System.err.println("Unable to create directory: " + targetLanguageDir);
			System.exit(1);			
		}

		if (!targetWelcomeMessageDir.exists() && !targetWelcomeMessageDir.mkdirs()) {
			System.err.println("Unable to create directory: " + targetWelcomeMessageDir);
			System.exit(1);			
		}
		
		File[] lists = targetListsDir.listFiles();
		for (File list : lists) {
			if (list.getName().equals(TBBuilder.IntroMessageListFilename)) {
				exportList(list,targetWelcomeMessageDir,"intro.a18");
				list.delete();
				hasIntro = true;
			} else if (!list.getName().equalsIgnoreCase("_activeLists.txt")) {
				exportList(list, targetAudioDir);				
			}
		}

		File sourceTbOptionsDir = new File(dropboxTbLoadersDir, "TB_Options");
		File sourceBasic = new File(sourceTbOptionsDir,"basic");
		FileUtils.copyDirectory(sourceBasic, targetImageDir);		
		
		File sourceConfigFile = new File(sourceTbOptionsDir,"config_files/config.txt");
		File targetSystemDir = new File(targetImageDir, "system");
		FileUtils.copyFileToDirectory(sourceConfigFile, targetSystemDir);
		
		File sourceLanguage = new File(sourceTbOptionsDir,"languages/" + languageCode);
		FileUtils.copyDirectory(sourceLanguage, targetLanguageDir);
		
		File sourceControlFile;
		if (hasIntro) {
			sourceControlFile = new File(sourceTbOptionsDir,"system_menus/control-with_intro.txt");
		} else {
			sourceControlFile = new File(sourceTbOptionsDir,"system_menus/control-no_intro.txt");
		}
		FileUtils.copyFile(sourceControlFile, new File(targetLanguageDir,"control.txt"));
	
		File sourceFirmware = null;
		File[] firmwareOptions = new File(sourceTbOptionsDir,"firmware").listFiles();
		for (File f:firmwareOptions) {
			if (sourceFirmware==null) {
				sourceFirmware = f;
			} else if (sourceFirmware.getName().compareToIgnoreCase(f.getName()) < 0) {
				sourceFirmware = f;
			}
		}
		File targetBasicDir = new File(targetDeploymentDir,"basic");
		FileUtils.copyFileToDirectory(sourceFirmware, targetBasicDir);

		//create profile.txt
		String profileString = packageName + "," + languageCode + "," + TBBuilder.firstMessageListName + ",menu\n";		
		File profileFile = new File(targetSystemDir,"profiles.txt");
        BufferedWriter out = new BufferedWriter(new FileWriter(profileFile));
		out.write(profileString);
		out.close();
		
		for (int i=0;i<groupCount;i++) {
			File f= new File(targetSystemDir,groups[i] + ".grp");
			f.createNewFile();
		}
		
		File f = new File(targetSystemDir,packageName + ".pkg");
		f.createNewFile();
		
		File sourceCommunitiesDir = new File(dropboxTbLoadersDir,"communities");
		File targetCommunitiesDir = new File(targetDeploymentDir,"communities");
		FileUtils.copyDirectory(sourceCommunitiesDir, targetCommunitiesDir);
		
		File[] files = dropboxTbLoadersDir.listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".rev") &&  name.toLowerCase().startsWith(packageName);
			}
		});
		
		
		char version ='a';
		if (files.length > 1)
			throw new Exception("Too many *rev files.  There can only be one.");
		else if (files.length > 0) {
			version = files[0].getName().subSequence(packageName.length()+1,packageName.length()+2).charAt(0);
			version++; 
		}

		files[0].delete();
		File newRev = new File(dropboxTbLoadersDir,packageName + "-" + version + ".rev");
		newRev.createNewFile();
		
		String zipSuffix = packageName + "-" + version + ".zip";
		
		ZipUnzip.zip(targetTempDir, new File(dropboxTbLoadersDir,"content-" + zipSuffix));
		FileUtils.deleteDirectory(targetTempDir);	

		ZipUnzip.zip(new File(dropboxTbLoadersDir,"software"), new File(dropboxTbLoadersDir,"software-" + zipSuffix), true);

		System.out.println("\nDone.");
	}
	
	private static void printUsage() {
		System.out.println("Usage: java -cp acm.jar:lib/* org.literacybridge.acm.tbbuilder.TBBuilder <acm_name> <deployment> <package_name> <language> (<group>)*");
	}

	private static void exportList(File list, File targetDirectory) throws Exception {
		exportList(list,targetDirectory,null);
	}
	
	private static void exportList(File list, File targetDirectory, String filename) throws Exception {
		System.out.println("  Exporting list " + list);
		AudioItemRepository repository = ACMConfiguration.getCurrentDB().getRepository();
		BufferedReader reader = new BufferedReader(new FileReader(list));
		
		try {
			while (reader.ready()) {
				String uuid = reader.readLine();
				AudioItem audioItem = AudioItem.getFromDatabase(uuid);
				System.out.println("    Exporting audioitem " + uuid + " to " + targetDirectory);
				if (filename == null) {
					repository.exportA18WithMetadata(audioItem, targetDirectory);
				} else {
					repository.exportA18WithMetadataToFile(audioItem, new File(targetDirectory,filename));
				}
			}
		} finally {
			reader.close();
		}
	}
}
