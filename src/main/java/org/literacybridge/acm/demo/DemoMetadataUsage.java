package org.literacybridge.acm.demo;

import java.util.List;

import org.literacybridge.acm.metadata.Metadata;
import org.literacybridge.acm.metadata.MetadataSpecification;
import org.literacybridge.acm.metadata.MetadataValue;

public class DemoMetadataUsage {
	public static void main(String[] args) {
		Metadata metadata = new Metadata();
		
		MetadataValue<String> foo1 = new MetadataValue<String>("foo1");
		metadata.addMetadataField(MetadataSpecification.DC_TITLE, foo1);
		try {
			foo1.setAttributeValue(MetadataSpecification.DC_CREATOR_ROLE, "bar1");
		} catch (IllegalArgumentException e) {
			System.out.println("Expected exception: " + e);
		}

		
		MetadataValue<String> foo2 = new MetadataValue<String>("foo2");
		metadata.addMetadataField(MetadataSpecification.DC_CREATOR, foo2);
		foo2.setAttributeValue(MetadataSpecification.DC_CREATOR_ROLE, "bar2");
		
		List<MetadataValue<String>> titles = metadata.getMetadataValues(MetadataSpecification.DC_TITLE);
		for (MetadataValue<String> title : titles) {
			String role = title.getAttribute(MetadataSpecification.DC_CREATOR_ROLE);
		
			System.out.println(title.getValue());
			System.out.println(role);
		}

		List<MetadataValue<String>> creators = metadata.getMetadataValues(MetadataSpecification.DC_CREATOR);
		for (MetadataValue<String> creator : creators) {
			String role = creator.getAttribute(MetadataSpecification.DC_CREATOR_ROLE);
		
			System.out.println(creator.getValue());
			System.out.println(role);
		}
	}
}
