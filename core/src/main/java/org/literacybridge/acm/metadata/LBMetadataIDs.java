package org.literacybridge.acm.metadata;

import com.google.common.collect.ImmutableBiMap;

public class LBMetadataIDs {
	public static final ImmutableBiMap<MetadataField<?>, Integer> FieldToIDMap =
	       new ImmutableBiMap.Builder<MetadataField<?>, Integer>()
	           .put(MetadataSpecification.DC_TITLE, 1)
	           .put(MetadataSpecification.DC_CREATOR, 2)
	           .build();
}
