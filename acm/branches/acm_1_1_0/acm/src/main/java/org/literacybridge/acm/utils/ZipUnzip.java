package org.literacybridge.acm.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ZipUnzip {
	static File baseInDir;
	
	private static void addDirectory(ZipOutputStream zout, File fileSource) throws IOException {
		if (fileSource != baseInDir) {
			String relativeDirName = baseInDir.toURI().relativize(fileSource.toURI()).getPath();
			System.out.println("RelDirName:" + relativeDirName);
			zout.putNextEntry(new ZipEntry(relativeDirName));
		}
		File[] files = fileSource.listFiles();	
		System.out.println("Adding directory " + fileSource.getName());
		for(int i=0; i < files.length; i++) {
			if(files[i].isDirectory()) {
				addDirectory(zout,files[i]);
				continue;
			}
			try {
				String relativeFileName = baseInDir.toURI().relativize(files[i].toURI()).getPath();
				System.out.println("Adding file " + relativeFileName);
				byte[] buffer = new byte[1024];
				FileInputStream fin = new FileInputStream(files[i]);
				zout.putNextEntry(new ZipEntry(relativeFileName));
				int length;
				while((length = fin.read(buffer)) > 0) {
					zout.write(buffer, 0, length);
				}
				zout.closeEntry();
				fin.close();
			}
			catch(IOException ioe) {
				System.out.println("IOException :" + ioe); 
			}
		}
	}

	public static void zip (File inDir, File outFile) throws IOException {
		baseInDir = inDir;
		ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(outFile));
		addDirectory(zout,inDir);
		zout.close();
		System.out.println("Zip file has been created!");		
	}

	public static void unzip (File inFile, File outDir) throws IOException {
		String parentDirName = inFile.getName().substring(0,inFile.getName().lastIndexOf(".zip"));
		File parentDir = new File(outDir, parentDirName);
		ZipFile zfile = new ZipFile(inFile);
		Enumeration <? extends ZipEntry> entries = zfile.entries();
	    while (entries.hasMoreElements()) {
	    	ZipEntry entry = entries.nextElement();
	    	File file = new File(parentDir, entry.getName());
	    	if (entry.isDirectory()) {
	    		file.mkdirs();
	    	} else {
		        file.getParentFile().mkdirs();
		        InputStream in = zfile.getInputStream(entry);
		        try {
		        	OutputStream out = new FileOutputStream(file);
		        	byte[] buffer = new byte[1024];
		        	while (true) {
		        		int readCount = in.read(buffer);
		        		if (readCount < 0) {
		        			break;
		        		}
		        		out.write(buffer, 0, readCount);
		        	}
		        } finally {
		          in.close();
		        }
	    	}
	    }
	}

}
