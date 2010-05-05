package org.literacybridge.acm.metadata;

import com.google.common.collect.ImmutableBiMap;
import static org.literacybridge.acm.metadata.MetadataSpecification.*;

public class LBMetadataIDs {
	public static final int CATEGORY_FIELD_ID = 0;
	
	// TODO: this should be defined in a separate (online, xml?) spec
	public static final ImmutableBiMap<MetadataField<?>, Integer> FieldToIDMap =
	       new ImmutableBiMap.Builder<MetadataField<?>, Integer>()
				.put(DC_TITLE, 1)
				.put(DC_CREATOR, 2)
				.put(DC_SUBJECT, 3)
				.put(DC_DESCRIPTION, 4)
				.put(DC_PUBLISHER, 5)
				.put(DC_CONTRIBUTOR, 6)
				.put(DC_DATE, 7)
				.put(DC_TYPE, 8)
				.put(DC_FORMAT, 9)
				.put(DC_IDENTIFIER, 10)
				.put(DC_SOURCE, 11)
				.put(DC_LANGUAGE, 12)
				.put(DC_RELATION, 13)
				.put(DC_COVERAGE, 14)
				.put(DC_RIGHTS, 15)
				.put(DTB_REVISION, 16)
				.put(DTB_REVISION_DATE, 17)
				.put(DTB_REVISION_DESCRIPTION, 18)
				.put(LB_COPY_COUNT, 19)
				.put(LB_PLAY_COUNT, 20)
				.put(LB_RATING, 21)
				.build();
}
