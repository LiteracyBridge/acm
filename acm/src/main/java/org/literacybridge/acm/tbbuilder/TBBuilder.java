package org.literacybridge.acm.tbbuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.DBConfiguration;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.CommandLineParams;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.utils.IOUtils;
import org.literacybridge.acm.utils.ZipUnzip;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public class TBBuilder {
	private static final String[] CSV_COLUMNS = {"project", "contentpackage", "contentid", "categoryid", "order"};
	private static final String CONTENT_IN_PACKAGES_CSV_FILE_NAME = "contentinpackages.csv";

	public static String firstMessageListName = "1";
	public static String IntroMessageID = "0-5";
	private static String IntroMessageListFilename = IntroMessageID + ".txt";
	public static final String ACM_PREFIX = "ACM-";
	private final static int MAX_DEPLOYMENTS = 5;
	private File dropboxTbLoadersDir;
	private File targetDeploymentDir;
	private CSVWriter contentInPackageCSVWriter;
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
				exportList(packageName, list,targetWelcomeMessageDir,"intro.a18", false);
				list.delete();
				hasIntro = true;
			} else if (!list.getName().equalsIgnoreCase("_activeLists.txt")) {
				exportList(packageName, list, targetAudioDir, true);
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
		System.out.println("Done with adding image for " + packageName + " and " + languageCode + ".");


	}

	public void createDeployment(String deployment) throws Exception {
		CommandLineParams params = new CommandLineParams();
		params.disableUI = true;
		params.readonly = true;
		params.sandbox = true;
		params.sharedACM = ACM_PREFIX + ACMname;
		Application.startUp(params);
		deploymentNumber = deployment;
		targetDeploymentDir = new File(targetTempDir, "content/" + deploymentNumber);
		File targetMetadataDir = new File(targetTempDir, "metadata/" + deploymentNumber);

		sourceTbOptionsDir = new File(dropboxTbLoadersDir, "TB_Options");

		// use LB Home Dir to create folder, then zip to Dropbox and delete the folder
		IOUtils.deleteRecursive(targetDeploymentDir);
		targetDeploymentDir.mkdirs();
		IOUtils.deleteRecursive(targetMetadataDir);
		targetMetadataDir.mkdirs();

		contentInPackageCSVWriter = new CSVWriter(new FileWriter(
				new File(targetMetadataDir, CONTENT_IN_PACKAGES_CSV_FILE_NAME)), ',');

		// write column headers
		contentInPackageCSVWriter.writeNext(CSV_COLUMNS);

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

		deleteRevFiles(targetTempDir);
		char revision;
		revision = getLatestDeploymentVersion(dropboxTbLoadersDir,deploymentNumber,false);
		File newRev = new File(targetTempDir,deploymentNumber + "-" + revision + ".rev");
		newRev.createNewFile();

		System.out.println("\nDone with deployment of software and basic/community content.");
	}

	public TBBuilder (String sharedACM) throws Exception {
		dropboxTbLoadersDir = ACMConfiguration.dirACM(sharedACM);  //.getCurrentDB().getTBLoadersDirectory();
		ACMname = sharedACM.substring(ACM_PREFIX.length());
		File localTbLoadersDir = new File(DBConfiguration.getLiteracyBridgeHomeDir(), Constants.TBLoadersHomeDir);
		targetTempDir = new File(localTbLoadersDir,ACMname);
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

	public void publish(String[] deployments) throws Exception {
		// e.g. 'ACM-UWR/TB-Loaders/published/'
		final File publishBaseDir = new File(dropboxTbLoadersDir, "published");
		publishBaseDir.mkdirs();
		char revision = getLatestDeploymentVersion(publishBaseDir, deployments[0], true);

		// e.g. '2015-6-c'
		final String publishDeploymentName = deployments[0] + "-" + revision;

		// e.g. 'ACM-UWR/TB-Loaders/published/2015-6-c'
		final File publishDeploymentDir = new File(publishBaseDir, publishDeploymentName);
		publishDeploymentDir.mkdirs();

		String zipSuffix = deployments[0] + "-" + revision + ".zip";
		File localContent = new File(targetTempDir,"content");
		ZipUnzip.zip(localContent, new File(publishDeploymentDir,"content-" + zipSuffix), true, deployments);

		// merge csv files
		File localMetadata = new File(targetTempDir,"metadata");
		final List<File> inputCSVFiles = new LinkedList<File>();
		File[] deploymentDirs = localMetadata.listFiles(new FileFilter() {
			@Override public boolean accept(File f) {
				return f.isDirectory();
			}
		});
		for (File deploymentDir : deploymentDirs) {
			deploymentDir.listFiles(new FileFilter() {
				@Override public boolean accept(File f) {
					if (f.getName().endsWith(".csv")) {
						inputCSVFiles.add(f);
						return true;
					}
					return false;
				}
			});
		}
		File mergedCSVFile = new File(publishDeploymentDir, CONTENT_IN_PACKAGES_CSV_FILE_NAME);
		mergeCSVFiles(inputCSVFiles, mergedCSVFile);

		deleteRevFiles(targetTempDir);
		ZipUnzip.zip(new File(publishDeploymentDir,"software"), new File(publishDeploymentDir,"software-" + zipSuffix), true);
	}


	private static void deleteRevFiles(File dir) {
		File[] files = dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".rev") /*&&  name.toLowerCase().startsWith(deploymentNumber)*/;
			}
		});
		for (File revisionFile:files) {
			revisionFile.delete();
		}
	}

	public static void main(String[] args) throws Exception {
		TBBuilder tbb;
		if (args.length == 0){
			printUsage();
			System.exit(1);
		} else if (args[0].equalsIgnoreCase("CREATE")) {
			tbb = new TBBuilder(args[1]);
			tbb.createDeployment(args[2]);
			if (args.length == 5) {
				tbb.addImage(args[3],args[4], "default");
			} else if (args.length == 6 || args.length == 9) {
				tbb.addImage(args[3],args[4], args[5]);
				if (args.length == 9) {
					tbb.addImage(args[6],args[7], args[8]);
				}
			} else {
				printUsage();
				System.exit(1);
			}
			tbb.contentInPackageCSVWriter.close();
		} else if (args[0].equalsIgnoreCase("PUBLISH")) {
			if (args.length < 3) {
				printUsage();
				System.exit(1);
			}
			tbb = new TBBuilder(args[1]);
			int deploymentCount = args.length - 2;
			if (deploymentCount>MAX_DEPLOYMENTS)
				deploymentCount=MAX_DEPLOYMENTS;
			String[] deployments = new String[deploymentCount];
			for (int i=0;i<deploymentCount;i++) {
				deployments[i]=args[i+2];
			}
			tbb.publish(deployments);
		} else {
			printUsage();
			System.exit(1);
		}
	}

	private static void printUsage() {
		System.out.println("Usage: java -cp acm.jar:lib/* org.literacybridge.acm.tbbuilder.TBBuilder CREATE <acm_name> <deployment> <package_name> <language> (<group>) (<package_name2>) (<language2>) (<group2>)");
		System.out.println("OR   : java -cp acm.jar:lib/* org.literacybridge.acm.tbbuilder.TBBuilder PUBLISH <acm_name> <default_deployment> (<deployment2>...) ");
	}

	private void exportList(String contentPackage, File list, File targetDirectory, boolean writeToCSV) throws Exception {
		exportList(contentPackage, list,targetDirectory,null, writeToCSV);
	}

	private void exportList(String contentPackage, File list, File targetDirectory,
			String filename, boolean writeToCSV) throws Exception {
		System.out.println("  Exporting list " + list);
		String[] csvColumns = new String[5];
		csvColumns[0] = ACMname;
		csvColumns[1] = contentPackage;
		csvColumns[3] = list.getName().substring(0, list.getName().length() - 4); // strip .txt

		AudioItemRepository repository = ACMConfiguration.getCurrentDB().getRepository();
		BufferedReader reader = new BufferedReader(new FileReader(list));

		int order = 1;
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

				if (writeToCSV) {
					csvColumns[2] = uuid;
					csvColumns[4] = Integer.toString(order);
					contentInPackageCSVWriter.writeNext(csvColumns);
				}

				order++;
			}
		} finally {
			reader.close();
		}
	}

	private static void mergeCSVFiles(Iterable<File> inputFiles, File output) throws IOException {
		CSVWriter writer = new CSVWriter(new FileWriter(output), ',');

		try {
			writer.writeNext(CSV_COLUMNS);

			for (File input : inputFiles) {
				CSVReader reader = new CSVReader(new FileReader(input), ',');
				// skip header
				reader.readNext();
				writer.writeAll(reader.readAll());
				reader.close();
			}
		} finally {
			writer.close();
		}
	}
}
