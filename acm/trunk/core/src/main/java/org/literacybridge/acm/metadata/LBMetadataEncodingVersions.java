package org.literacybridge.acm.metadata;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class LBMetadataEncodingVersions {
	public abstract static class Version {
		private List<Integer> fieldIDs = new LinkedList<Integer>();
		
	    Version() {
			fillFieldIDs();
			fieldIDs = Collections.unmodifiableList(fieldIDs);
		}
		
		public final List<Integer> getFieldIDs() {
			return this.fieldIDs;
		}
		
		final void addField(MetadataField<?> field) {
			LBMetadataIDs.FieldToIDMap.get(field);	
		}
		
		abstract void fillFieldIDs();
	}
	
	public final static Version VERSION_1 = new Version() {
		@Override
		public void fillFieldIDs() {
			addField(MetadataSpecification.DC_TITLE);
			addField(MetadataSpecification.DC_CREATOR);
		}
	};
	
	public static Version getVersion(int version) {
		switch (version) {
		case 1: return VERSION_1;
		}
		
		return VERSION_1;
	}
}
