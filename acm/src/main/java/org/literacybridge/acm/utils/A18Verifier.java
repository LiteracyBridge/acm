package org.literacybridge.acm.utils;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import org.literacybridge.acm.categories.Taxonomy;
import org.literacybridge.acm.categories.Taxonomy.Category;
import org.literacybridge.acm.db.Persistence;
import org.literacybridge.acm.metadata.LBMetadataSerializer;
import org.literacybridge.acm.metadata.Metadata;

public class A18Verifier {
	public static void main(String[] args) throws Exception {
		Persistence.initialize();
		Taxonomy.getTaxonomy();
		boolean verbose = false;
		if (args.length == 0) {
			printUsage();
		}
		
		int index = 0;
		
		if (args.length > 1) {
			if (args[0].equalsIgnoreCase("-v")) {
				verbose = true;
				index++;
			} else {
				printUsage();
			}
		}
		
		File f = new File(args[index]);
		if (!f.exists()) {
			System.out.println("File or directory '" + args[index] + "' does not exist.\n");
			printUsage();
		}
		
		if (f.isDirectory()) {
			processDir(f, verbose);
		} else {
			processFile(f, verbose);
		}
	}
	
	private static void processDir(File dir, boolean verbose) throws IOException {
		File[] files = dir.listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".a18");
			}
		});
		
		boolean success = true;
		for (File f : files) {
			if (!processFile(f, verbose)) {
				success = false;
			}
		}
		if (success) {
			System.out.println("\nAll files successfully verified.");
		} else {
			System.out.println("\nERRORS occurred.");
			if (!verbose) {
				System.out.println("Run in verbose mode to see further details.");
			}
		}
	}
	
	private static boolean processFile(File file, boolean verbose) throws IOException {
		System.out.print("Verifying file " + file.getName());
		try {
			DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
			int bytesToSkip = IOUtils.readLittleEndian32(in);
			if (verbose) {
				System.out.print(" [bytesToSkip=" + bytesToSkip + ", file.length=" + file.length() + "]");
			}
			System.out.print(" ... ");
			
			final Metadata metadata = new Metadata();
			final Set<Category> categories = new HashSet<Category>();
			if (bytesToSkip + 4 < file.length()) { 
				in.skipBytes(bytesToSkip);
				LBMetadataSerializer serializer = new LBMetadataSerializer();
				serializer.deserialize(metadata, categories, in);
				System.out.println("OK");
			} else {
				System.out.println("\tWARNING: No metadata section found.");
			}
			if (verbose) {
				System.out.println(metadata.toString());
				System.out.print("Categories: ");
				Iterator<Category> it = categories.iterator();
				while (it.hasNext()) {
					System.out.print(it.next().getCategoryName(Locale.ENGLISH));
					if (it.hasNext()) {
						System.out.print(", ");
					} else {
						System.out.println();
					}
				}
			}
			in.close();
			return true;
		} catch (Exception e) {
			System.out.println("ERROR.");
			if (verbose) {
				e.printStackTrace(System.err);
			}
			return false;
		}

	}
	
	private static void printUsage() {
		System.out.print("Usage: java -jar acm-core-a18-verifier.jar");
		System.out.println(" [-v] <path>");
		System.out.println("\n\t-v\tOptional: verbose mode");
		System.out.println("\t<path>\tpath to a18 file or directory containing multiple a18 files");
		System.exit(1);
	}
}
