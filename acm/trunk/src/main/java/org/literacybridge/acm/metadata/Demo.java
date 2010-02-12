package org.literacybridge.acm.metadata;

import java.util.List;

public class Demo {
	public static void main(String[] args) {
		MetadataSet metadata = new MetadataSet();
		
		MetadataValue<String> foo1 = new MetadataValue<String>("foo1");
		foo1.addAttribute(MetadataSpecification.DC_CREATOR_ROLE, "bar1");
		metadata.addMetadataField(MetadataSpecification.DC_TITLE, foo1);

		MetadataValue<String> foo2 = new MetadataValue<String>("foo2");
		foo2.addAttribute(MetadataSpecification.DC_CREATOR_ROLE, "bar2");
		metadata.addMetadataField(MetadataSpecification.DC_TITLE, foo2);

		List<MetadataValue<String>> titles = metadata.getMetadataValues(MetadataSpecification.DC_TITLE);
		for (MetadataValue<String> title : titles) {
			String role = title.getAttribute(MetadataSpecification.DC_CREATOR_ROLE);
		
			System.out.println(title.getValue());
			System.out.println(role);
		}
	}
}
