package org.literacybridge.acm.metadata;

import com.google.common.collect.ImmutableBiMap;
import static org.literacybridge.acm.metadata.MetadataSpecification.*;

public class LBMetadataIDs {
	public static final int CATEGORY_FIELD_ID = 0;
	
	// TODO: this should be defined in a separate (online, xml?) spec
	public static final ImmutableBiMap<MetadataField<?>, Integer> FieldToIDMap =
	       new ImmutableBiMap.Builder<MetadataField<?>, Integer>()
				.put(DC_TITLE, 1)
				.put(DC_PUBLISHER, 5)
				.put(DC_IDENTIFIER, 10)
				.put(DC_SOURCE, 11)
				.put(DC_LANGUAGE, 12)
				.put(DC_RELATION, 13)
				.put(DTB_REVISION, 16)
				.put(LB_DURATION, 22)
				.put(LB_MESSAGE_FORMAT, 23)
				.put(LB_TARGET_AUDIENCE, 24)
				.put(LB_DATE_RECORDED, 25)
				.put(LB_KEYWORDS, 26)
				.put(LB_TIMING, 27)
				.put(LB_PRIMARY_SPEAKER, 28)
				.put(LB_GOAL, 29)
				.put(LB_ENGLISH_TRANSCRIPTION, 30)
				.put(LB_NOTES, 31)
				.put(LB_BENEFICIARY, 32)
				.put(LB_STATUS, 33)
				.build();
}
