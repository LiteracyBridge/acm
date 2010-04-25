package org.literacybridge.acm.importexport;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.literacybridge.acm.categories.Taxonomy.Category;
import org.literacybridge.acm.metadata.LBMetadataSerializer;
import org.literacybridge.acm.metadata.Metadata;
import org.literacybridge.acm.utils.IOUtils;

public class A18Importer extends FileImporter {

	public A18Importer() {
		super(FileImporter.getFileExtensionFilter(".a18"));	
	}

	
	@Override
	protected void importSingleFile(Category category, File file)
			throws IOException {
		DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
		int bytesToSkip = IOUtils.readLittleEndianInt(in);
		final Metadata metadata;
		if (bytesToSkip + 4 < file.length()) { 
			in.skipBytes(bytesToSkip);
			LBMetadataSerializer serializer = new LBMetadataSerializer();
			metadata = serializer.deserialize(in);
		} else {
			metadata = new Metadata();
		}
		System.out.println(metadata.toString());
		in.close();
	}

	public static void main(String[] args) throws Exception {
		A18Importer importer = new A18Importer();
		importer.importSingleFile(null, new File("/Users/michael/lb-workspace/maven.1271816866778/audioconverter/trunk/src/test/resources/source_a18.a18"));
	}
}
