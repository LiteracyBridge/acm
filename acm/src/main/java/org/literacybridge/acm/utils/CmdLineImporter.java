package org.literacybridge.acm.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.CommandLineParams;
import org.literacybridge.acm.importexport.FileImporter;

import com.google.common.collect.Sets;

public class CmdLineImporter {
	static String successDir = "success";

	public static void main(String[] args) throws Exception {
		Params params = new Params();
		CmdLineParser parser = new CmdLineParser(params);

		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			printUsage(parser);
			System.exit(1);
		}
		
		if (StringUtils.isEmpty(params.acmName) || params.dirs.isEmpty()) {
			printUsage(parser);
			System.exit(1);			
		}

		Set<File> inputDirs = Sets.newHashSet();
		for (String dirName : params.dirs) {
			File dir = new File(dirName);
			if (!dir.exists()) {
				System.err.println("Directory does not exist: " + dirName);
				System.exit(1);
			}
			inputDirs.add(dir);
		}
		
		Set<File> filesToImport = Sets.newHashSet();
		gatherFiles(inputDirs, filesToImport, params.recursive);

		
		// start ACM and acquire write access
		try {
			CommandLineParams acmParams = new CommandLineParams();
			acmParams.disableUI = true;
			//acmParams.readonly = false;
			//acmParams.sandbox = false;
			acmParams.sharedACM = params.acmName;
			Application.startUp(acmParams);	
		} catch (Exception e) {
			System.err.println("Unable to start ACM.");
			System.exit(2);			
		}
		
		if (ACMConfiguration.getCurrentDB().getControlAccess().isSandbox()) {
			System.err.println("Unable to acquire writer access.");
			System.exit(3);
		}

		
		boolean success = importFiles(filesToImport);
		if (success) {
			System.out.println("All files imported without an exception.");
		} else {
			System.out.println("At least one file could not be imported.");
		}
		
		ACMConfiguration.getCurrentDB().getControlAccess().updateDB();
		System.exit(success ? 0 : 4);
	}
	
	private static final class Params {
		@Option(name="-r",usage="Traverse directories recursively to discover .a18 files.")
		public boolean recursive = false;

		@Option(name="-acm",usage="Name of the ACM database")
		public String acmName;
		
		@Argument
		public List<String> dirs;	
	}
	
	private static void printUsage(CmdLineParser parser) {
		System.err.println("java -cp acm.jar:lib/* org.literacybridge.acm.utils [-r] -acm <acm_name> <dir1> [<dir2> ...]");
		parser.printUsage(System.err);
	}
	
	private static void gatherFiles(Set<File> inputDirs, Set<File> filesToImport, boolean recursive) throws IOException {
		for (File dir : inputDirs) {
			if (recursive) {
				File[] subdirs = dir.listFiles(new FileFilter() {
					@Override public boolean accept(File file) {
						return file.isDirectory() && !file.getName().equals(successDir);
					}
				});
				
				gatherFiles(Sets.newHashSet(subdirs), filesToImport, recursive);
			}
			
			File[] a18Files = dir.listFiles(new FilenameFilter() {
				@Override public boolean accept(File file, String fileName) {
					return fileName.toLowerCase().endsWith("a18");
				}
			});
			
			for (File a18 : a18Files) {
				filesToImport.add(a18);
			}
		}
	}
	
	private static boolean importFiles(Set<File> filesToImport) throws Exception {
		boolean success = true;
		boolean needSuccessFolder = true;
		FileImporter importer = FileImporter.getInstance();
		long count = 0; 
		String total = String.valueOf(filesToImport.size());
		
		for (File file : filesToImport) {
			try {
				System.out.println("Importing file " + String.valueOf(++count) + " of " + total + ": " + file);
				importer.importFile(null, file);
				FileUtils.moveToDirectory(file, new File(file.getParentFile(),successDir), true);
			} catch (Exception e) {
				success = false;
				logError(file, e);
			}
		}
		
		return success;
	}
	
	private static void logError(File a18File, Exception exception) {
		File errorFile = new File(a18File.getParentFile(), a18File.getName() + ".error.txt");
		PrintStream out = null;
		try {
			out = new PrintStream(errorFile);
			exception.printStackTrace(out);
		} catch (IOException e) {
			// ignore
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}
}
