package org.literacybridge.acm.store;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;

import static org.literacybridge.acm.store.MetadataSpecification.*;

public class LBMetadataIDs {
  public static final int CATEGORY_FIELD_ID = 0;

  // TODO: this should be defined in a separate (online, xml?) spec
  // Really TODO: Where do these numbers come from? They look pretty, um, magical...
  public static final ImmutableBiMap<MetadataField<?>, Integer> FieldToIDMap = new ImmutableBiMap.Builder<MetadataField<?>, Integer>()
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
          .put(LB_CORRELATION_ID, 34)
          .build();
  public static final ImmutableBiMap<MetadataField<?>, String> FieldToNameMap = new ImmutableBiMap.Builder<MetadataField<?>, String>()
          .put(DC_TITLE, DC_TITLE.getName())
          .put(DC_PUBLISHER, DC_PUBLISHER.getName())
          .put(DC_IDENTIFIER, DC_IDENTIFIER.getName())
          .put(DC_SOURCE, DC_SOURCE.getName())
          .put(DC_LANGUAGE, DC_LANGUAGE.getName())
          .put(DC_RELATION, DC_RELATION.getName())
          .put(DTB_REVISION, DTB_REVISION.getName())
          .put(LB_DURATION, LB_DURATION.getName())
          .put(LB_MESSAGE_FORMAT, LB_MESSAGE_FORMAT.getName())
          .put(LB_TARGET_AUDIENCE, LB_TARGET_AUDIENCE.getName())
          .put(LB_DATE_RECORDED, LB_DATE_RECORDED.getName())
          .put(LB_KEYWORDS, LB_KEYWORDS.getName())
          .put(LB_TIMING, LB_TIMING.getName())
          .put(LB_PRIMARY_SPEAKER, LB_PRIMARY_SPEAKER.getName())
          .put(LB_GOAL, LB_GOAL.getName())
          .put(LB_ENGLISH_TRANSCRIPTION, LB_ENGLISH_TRANSCRIPTION.getName())
          .put(LB_NOTES, LB_NOTES.getName())
          .put(LB_BENEFICIARY, LB_BENEFICIARY.getName())
          .put(LB_STATUS, LB_STATUS.getName())
          .put(LB_CORRELATION_ID, LB_CORRELATION_ID.getName())
          .build();

  public static final ImmutableBiMap<String, MetadataField<?>> NameToFieldMap = FieldToNameMap.inverse();

  public static final ImmutableMap<MetadataField<?>, Class> FieldToValueClassMap = new ImmutableMap.Builder<MetadataField<?>, Class>()
          .put(DC_TITLE, String.class)
          .put(DC_PUBLISHER, String.class)
          .put(DC_IDENTIFIER, String.class)
          .put(DC_SOURCE, String.class)
          .put(DC_LANGUAGE, RFC3066LanguageCode.class)
          .put(DC_RELATION, String.class)
          .put(DTB_REVISION, String.class)
          .put(LB_DURATION, String.class)
          .put(LB_MESSAGE_FORMAT, String.class)
          .put(LB_TARGET_AUDIENCE, String.class)
          .put(LB_DATE_RECORDED, String.class)
          .put(LB_KEYWORDS, String.class)
          .put(LB_TIMING, String.class)
          .put(LB_PRIMARY_SPEAKER, String.class)
          .put(LB_GOAL, String.class)
          .put(LB_ENGLISH_TRANSCRIPTION, String.class)
          .put(LB_NOTES, String.class)
          .put(LB_BENEFICIARY, String.class)
          .put(LB_STATUS, Integer.class)
          .put(LB_CORRELATION_ID, Integer.class)
          .build();

}
