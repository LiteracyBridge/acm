package org.literacybridge.acm.metadata;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

public class LBMetadataIDs {
	private static final Map<MetadataField<?>, Integer> internalFieldToIDMap = new IdentityHashMap<MetadataField<?>, Integer>();
	public static final Map<MetadataField<?>, Integer> FieldToIDMap = Collections.unmodifiableMap(internalFieldToIDMap);

	
	static {
		internalFieldToIDMap.put(MetadataSpecification.DC_TITLE, 1);
		internalFieldToIDMap.put(MetadataSpecification.DC_CREATOR, 2);
	}
}
