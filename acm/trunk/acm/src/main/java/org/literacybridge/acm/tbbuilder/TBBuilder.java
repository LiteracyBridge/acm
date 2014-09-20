package org.literacybridge.acm.tbbuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;

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
	public static final String ACM_PREFIX = "ACM-";
	private final static int MAX_GROUPS = 5;
	private File dropboxTbLoadersDir;
	private File targetDeploymentDir;
	private File sourceTbOptionsDir;
	private File targetTempDir;
	private String deploymentNumber;
	public String ACMname;
	
	public void addImage(String packageName, String languageCode, String group) throws Exception {
		String groups[] = new String[1];
		groups[0] = group;
		addImage(packageName, languageCode, groups);
	}

	public void addImage(String packageName, String languageCode, String[] groups) throws Exception {
		boolean hasIntro = false;
		int groupCount = groups.length;
		System.out.println("\n\nExporting package " + packageName);

		File sourcePackageDir = new File(dropboxTbLoadersDir,"packages/" + packageName);		
		File sourceMessagesDir = new File(sourcePackageDir,"messages");
		File sourceListsDir = new File(sourceMessagesDir,"lists/" + TBBuilder.firstMessageListName);
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
		
		
		
	}

	public void createDeployment(String deployment) throws Exception {
		deploymentNumber = deployment;
		dropboxTbLoadersDir = ACMConfiguration.getCurrentDB().getTBLoadersDirectory();
		sourceTbOptionsDir = new File(dropboxTbLoadersDir, "TB_Options");
		
		// use LB Home Dir to create folder, then zip to Dropbox and delete the folder
		File localTbLoadersDir = new File(DBConfiguration.getLiteracyBridgeHomeDir(), Constants.TBLoadersHomeDir);
		targetTempDir = new File(localTbLoadersDir,ACMname);
		targetDeploymentDir = new File(targetTempDir, "content/" + deploymentNumber);
		IOUtils.deleteRecursive(targetDeploymentDir);
		targetDeploymentDir.mkdirs();
		
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

		File sourceCommunitiesDir = new File(dropboxTbLoadersDir,"communities");
		File targetCommunitiesDir = new File(targetDeploymentDir,"communities");
		FileUtils.copyDirectory(sourceCommunitiesDir, targetCommunitiesDir);

		File localSoftware = new File(targetTempDir,"software");
		FileUtils.deleteDirectory(localSoftware);
		FileUtils.copyDirectory(new File(dropboxTbLoadersDir, "software"),localSoftware);
		
		File[] files = targetTempDir.listFiles(new FilenameFilter() {			
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".rev") &&  name.toLowerCase().startsWith(deploymentNumber);
			}
		});
		for (File revisionFile:files) {
			revisionFile.delete();
		}
		char revision;
		revision = getLatestDeploymentVersion(dropboxTbLoadersDir,deploymentNumber,false);
		File newRev = new File(targetTempDir,deploymentNumber + "-" + revision + ".rev");
		newRev.createNewFile();

		System.out.println("\nDone.");				
	}

	public TBBuilder (String sharedACM) throws Exception {
		CommandLineParams params = new CommandLineParams();
		params.disableUI = true;
		params.readonly = true;
		params.sandbox = true;
		params.sharedACM = sharedACM;
		Application.startUp(params);	
		ACMname = params.sharedACM.substring(ACM_PREFIX.length());
	}

	private static char getLatestDeploymentVersion(File publishTbLoadersDir, final String deploymentNumber, boolean updateRevision) throws Exception{
		char revision ='a';
		File[] files = publishTbLoadersDir.listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".rev") &&  name.toLowerCase().startsWith(deploymentNumber);
			}
		});
		if (files.length > 1)
			throw new Exception("Too many *rev files.  There can only be one.");
		else if (files.length == 1) {
			revision = files[0].getName().subSequence(deploymentNumber.length()+1,deploymentNumber.length()+2).charAt(0);
			revision++; 
		}
		if (updateRevision) {
			// search files again, but this time for all *.rev files, not just the one with the same deployment number so we can delete them all
			files = publishTbLoadersDir.listFiles(new FilenameFilter() {				
				@Override
				public boolean accept(File dir, String name) {
					return name.toLowerCase().endsWith(".rev");
				}
			});
			int fileCount = files.length;
			for(int i=0;i<fileCount;i++) {
				files[i].delete();
			}
			File newRev = new File(publishTbLoadersDir,deploymentNumber + "-" + revision + ".rev");
			newRev.createNewFile();
		}
		return revision;
	}

	public void publish() throws Exception {
		char revision;

		revision = getLatestDeploymentVersion(dropboxTbLoadersDir,deploymentNumber,true);
		String zipSuffix = deploymentNumber + "-" + revision + ".zip";		
		File localContent = new File(targetTempDir,"content");
		ZipUnzip.zip(localContent, new File(dropboxTbLoadersDir,"content-" + zipSuffix), true);
		File[] files = targetTempDir.listFiles(new FilenameFilter() {			
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".rev") /*&&  name.toLowerCase().startsWith(deploymentNumber)*/;
			}
		});
		int fileCount = files.length;
		for (int i=0;i< fileCount;i++) {
			files[i].delete();
		}
		ZipUnzip.zip(new File(dropboxTbLoadersDir,"software"), new File(dropboxTbLoadersDir,"software-" + zipSuffix), true);
	}
	
	public static void main(String[] args) throws Exception {
		TBBuilder tbb;
/*		int groupCount = args.length - 4;
		if (groupCount>MAX_GROUPS)
			groupCount=MAX_GROUPS;
		String[] groups = new String[groupCount];
		for (int i=0;i<groupCount;i++) {
			groups[i]=args[i+4];
		}
*/		if (args.length != 5 && args.length != 8) {
			printUsage();
			System.exit(1);
		} else {
			tbb = new TBBuilder(args[0]);
			tbb.createDeployment(args[1]);
			tbb.addImage(args[2],args[3], args[4]);
			if (args.length == 8) {
				tbb.addImage(args[5],args[6], args[7]);
			}
			tbb.publish();
		}
	}
	
	private static void printUsage() {
		System.out.println("Usage: java -cp acm.jar:lib/* org.literacybridge.acm.tbbuilder.TBBuilder <acm_name> <deployment> <package_name> <language> <group> (<package_name2>) (<language2>) (<group2>)");
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
