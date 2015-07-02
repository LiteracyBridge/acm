package org.literacybridge.acm.tools;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.literacybridge.acm.categories.Taxonomy;
import org.literacybridge.acm.categories.Taxonomy.Category;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.LockACM;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.content.LocalizedAudioItem;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.CommandLineParams;
import org.literacybridge.acm.gui.util.language.LanguageUtil;
import org.literacybridge.acm.importexport.CSVExporter;
import org.literacybridge.acm.tbbuilder.TBBuilder;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import au.com.bytecode.opencsv.CSVWriter;

public class DBExporter {
	private String project;
	private File exportDirectory;
	private static boolean hasACMStarted = false;
	
	private DBExporter(String dbName,File exportDir) throws Exception {
		exportDirectory = exportDir;

		if (!DBExporter.hasACMStarted) {
			CommandLineParams params = new CommandLineParams();
			params.sharedACM = dbName;
			params.disableUI = true;
			params.readonly = true;
			params.sandbox = true;
			params.disableIndex = true;
			Application.startUp(params);
			DBExporter.hasACMStarted = true;
		} else
			ACMConfiguration.setCurrentDB(dbName, false);
		project = dbName.substring(TBBuilder.ACM_PREFIX.length());
	}
	
	public void export() {
		categoriesExporter();
		languagesExporter();
		metadataExporter();
	}
	
	private void categoriesExporter() {
		String[] header = {"ID","Name","Project"};
		File exportFile = new File(exportDirectory,project + "-categories.csv");
		try {
			CSVWriter writer = new CSVWriter(new FileWriter(exportFile), ',');
			writer.writeNext(header);
			Category leaf = Taxonomy.getTaxonomy().getRootCategory();
			getChildren(writer,leaf);
			writer.close();	
		} catch (IOException e) {
			System.out.println("==========>Could not export categories!");
			e.printStackTrace();
		}
	}
	private void languagesExporter() {
		String[] header = {"ID","Name","Project"};
		File exportFile = new File(exportDirectory,project + "-languages.csv");
		try {
			CSVWriter writer = new CSVWriter(new FileWriter(exportFile), ',');
		//String[] header = {"ID","Name","Project"};
			writer.writeNext(header);
			List <Locale> languages = ACMConfiguration.getCurrentDB().getAudioLanguages();
			for (Locale l:languages) {
				String languageCode = l.getLanguage();
				String languageLabel = LanguageUtil.getLocalizedLanguageName(l);
				String[] values = {languageCode,languageLabel,project};
				writer.writeNext(values);
			}
			writer.close();	
		} catch (IOException e) {
			System.out.println("==========>Could not export languages!");
			e.printStackTrace();
		}
	}
	
	private void metadataExporter() {
		File exportFile = new File(exportDirectory,project + "-metadata.csv");
		try {
			CSVExporter.export(Lists.transform(AudioItem.getFromDatabase(), new Function<AudioItem, LocalizedAudioItem>() {
				@Override public LocalizedAudioItem apply(AudioItem item) {
					return item.getLocalizedAudioItem(null);
				}
			}), exportFile);
		} catch (IOException e) {
			System.out.println("==========>Could not export metadata!");
			e.printStackTrace();
		}
	}


	private void getChildren(CSVWriter writer, Category cat) {
		List<Category> children = cat.getSortedChildren();
		for (Category child:children) {
			String[] values = new String[3];
			values[0] = child.getUuid();
			values[1] = child.getCategoryName(new Locale("en")).toString();
			values[2] = project;
			writer.writeNext(values);
			if (child.hasChildren()) {
				getChildren(writer, child);
			}
		}
	}

	private static void printUsage() {
		System.out.println("Usage: java -cp acm.jar:lib/* org.literacybridge.acm.tools.DBExporter <export directory> <acm_name>+");
	}

	public static void main(String[] args) throws Exception {
		int argCount = args.length;
		if (argCount < 2) { //TODO: when IllegalThreadStateException below is addressed, we can allow more than 2 args
			printUsage();
			System.exit(1);
		}
		File exportDir = new File(args[0]);
		if (!exportDir.isDirectory()) {
			throw new Exception("Export directory doesn't exist.\n"+ exportDir.getAbsolutePath());  //"and cannot be created"
		}
		// TODO:This currently causes an IllegalThreadStateException with more than one ACM
		for (int i=1;i<argCount;i++) {
			DBExporter exporter = new DBExporter(args[i],exportDir);
			exporter.export();
		}
	}
}
