package org.literacybridge.acm.metadata;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

public class TestMetadataSerialization extends TestCase {
	public void testSerialization() throws Exception {
		Metadata metadata = new Metadata();
		
		// add some metadata
		metadata.addMetadataField(MetadataSpecification.DC_TITLE, new MetadataValue<String>("test"));
		
		
		// validate
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(baos);
		LBMetadataSerializer serializer = new LBMetadataSerializer();
		serializer.serialize(metadata, out);
		out.close();
		
		byte[] buffer = baos.toByteArray();
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(buffer));
		Metadata deserialized = serializer.deserialize(in);
		
		assertEquals("Mismatch in deserialized metadata.", metadata, deserialized);
	}
	
	public static void assertEquals(String message, Metadata expected, Metadata actual) {
		Iterator<MetadataField<?>> it = expected.getFieldsIterator();
		while (it.hasNext()) {
			MetadataField<?> field = it.next();
			assertEquals(message, field, expected, actual);
		}
	}

	public static <T> void assertEquals(String message, MetadataField<T> field, Metadata expected, Metadata actual) {
		List<MetadataValue<T>> valuesExpected = expected.getMetadataValues(field);
		List<MetadataValue<T>> valuesActual   = actual.getMetadataValues(field);
		
		assertEquals(message, valuesExpected.size(), valuesActual.size());
		
		for (int i = 0; i < valuesExpected.size(); i++) {
			assertEquals(message, valuesExpected.get(i), valuesActual.get(i));
		}
		
	}
}
