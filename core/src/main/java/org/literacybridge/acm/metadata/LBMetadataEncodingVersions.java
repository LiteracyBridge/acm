package org.literacybridge.acm.metadata;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.google.common.collect.ImmutableSet;

public class LBMetadataEncodingVersions {
	public abstract static class Version {
		private final Collection<MetadataField<?>> fieldIDs;
		
	    Version() {
			fieldIDs = fillFields();
		}
		
		public final Collection<MetadataField<?>> getFields() {
			return this.fieldIDs;
		}
		
		
		abstract Collection<MetadataField<?>> fillFields();
	}
	
	public final static Version VERSION_1 = new Version() {
		@Override
		public Collection<MetadataField<?>> fillFields() {
			return new ImmutableSet.Builder<MetadataField<?>>()
			.add(MetadataSpecification.DC_TITLE)
			.add(MetadataSpecification.DC_CREATOR)
			.add(MetadataSpecification.DC_SUBJECT)
			.add(MetadataSpecification.DC_DESCRIPTION)
			.add(MetadataSpecification.DC_PUBLISHER)
			.add(MetadataSpecification.DC_CONTRIBUTOR)
			.add(MetadataSpecification.DC_DATE)
			.add(MetadataSpecification.DC_TYPE)
			.add(MetadataSpecification.DC_FORMAT)
			.add(MetadataSpecification.DC_IDENTIFIER)
			.add(MetadataSpecification.DC_SOURCE)
			.add(MetadataSpecification.DC_LANGUAGE)
			.add(MetadataSpecification.DC_RELATION)
			.add(MetadataSpecification.DC_COVERAGE)
			.add(MetadataSpecification.DC_RIGHTS)
			.add(MetadataSpecification.DTB_REVISION)
			.add(MetadataSpecification.DTB_REVISION_DATE)
			.add(MetadataSpecification.DTB_REVISION_DESCRIPTION)
			.build();
		}
	};
	
	public static Version getVersion(int version) {
		switch (version) {
		case 1: return VERSION_1;
		}
		
		return VERSION_1;
	}
	
	public static final Version CURRENT_VERSION = VERSION_1;

}
