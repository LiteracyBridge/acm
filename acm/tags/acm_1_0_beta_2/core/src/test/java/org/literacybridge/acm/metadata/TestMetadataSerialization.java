package org.literacybridge.acm.metadata;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.literacybridge.acm.categories.Taxonomy;
import org.literacybridge.acm.categories.Taxonomy.Category;
import org.literacybridge.acm.db.Persistence;
import org.literacybridge.acm.db.PersistentCategory;

public class TestMetadataSerialization extends TestCase {
	@Override
	public void setUp() throws Exception {
		//Persistence.initialize();
	}
	
	public void testSerialization() throws Exception {
/*		Metadata metadata = new Metadata();
		Set<Category> categories = new HashSet<Category>();
		
		Taxonomy.getTaxonomy();
		categories.add(new Category(PersistentCategory.getFromDatabase("1")));
		
		// add some metadata
		metadata.addMetadataField(MetadataSpecification.DC_TITLE, new MetadataValue<String>("test"));
		metadata.addMetadataField(MetadataSpecification.DC_TITLE, new MetadataValue<String>("test1"));
		
		metadata.addMetadataField(MetadataSpecification.DC_CREATOR, new MetadataValue<String>("test2"));
		
		
		// validate
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(baos);
		LBMetadataSerializer serializer = new LBMetadataSerializer();
		serializer.serialize(categories, metadata, out);
		out.close();
		
		Set<Category> readCategories = new HashSet<Category>();
		byte[] buffer = baos.toByteArray();
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(buffer));
		Metadata deserialized = serializer.deserialize(readCategories, in);
		
		assertEquals("Mismatch in deserialized metadata.", metadata, deserialized);*/
	}
	
	public static void assertEquals(String message, Metadata expected, Metadata actual) throws Exception {
		Iterator<MetadataField<?>> it = expected.getFieldsIterator();
		while (it.hasNext()) {
			MetadataField<?> field = it.next();
			assertEquals(message, field, expected, actual);
		}
	}

	public static <T> void assertEquals(String message, MetadataField<T> field, Metadata expected, Metadata actual) 
			throws Exception {
		List<MetadataValue<T>> valuesExpected = expected.getMetadataValues(field);
		List<MetadataValue<T>> valuesActual   = actual.getMetadataValues(field);
		
		assertEquals(message, valuesExpected.size(), valuesActual.size());
		
		for (int i = 0; i < valuesExpected.size(); i++) {
			assertEquals(message, valuesExpected.get(i), valuesActual.get(i));
		}
		
	}
}
