package org.literacybridge.acm.metadata;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;

import org.literacybridge.acm.metadata.types.MetadataIntegerField;
import org.literacybridge.acm.metadata.types.MetadataStatisticsField;
import org.literacybridge.acm.metadata.types.MetadataStringField;

import com.google.common.collect.ImmutableSet;

public interface MetadataSpecification {
	//============================================================================================================
	// Dublin core fields
	//============================================================================================================
	
	/** dc:Title 
		Content: The title of the DTB, including any subtitles.
		Occurrence: Required
	*/
	public final static MetadataField<String> DC_TITLE = new MetadataStringField("DC_TITLE");
	
	/** dc:Publisher 
		Content: The agency responsible for making the DTB available. (Compare dtb:sourcePublisher and dtb:producer.)
		Occurrence: Required
	 */
	public final static MetadataField<String> DC_PUBLISHER = new MetadataStringField("DC_PUBLISHER");


	/** dc:Identifier 
		Content: A string or number identifying the DTB. One instance of this element, that which is referenced from the packageunique-identifier attribute, must include an id.
		Occurrence: Required
		Added attributes:
	 		* scheme -- (optional) The name of the system or authority that generated or assigned the identifier. For example, "DOI", "ISBN", or "DTB".
	 */  
	public final static Attribute<String> DC_IDENTIFIER_SCHEME = new Attribute<String>();
	public final static MetadataField<String> DC_IDENTIFIER = new MetadataStringField("DC_IDENTIFIER", DC_IDENTIFIER_SCHEME);

	/** dc:Source 
		Content: A reference to a resource (e.g., a print original, ebook, etc.) from which the DTB is derived. Best practice is to use the ISBN when available.
		Occurrence: Optional - recommended.
	 */ 
	public final static MetadataField<String> DC_SOURCE = new MetadataStringField("DC_SOURCE");

	/** dc:Language 
		Content: Language of the content of the publication. An [RFC 3066] language code. For Sweden: "sv" or "sv-SE"; for UK: "en" or "en-GB"; for US: "en" or "en-US"; etc.
		Occurrence: Required
	 */ 
	public final static MetadataField<RFC3066LanguageCode> DC_LANGUAGE = new MetadataField<RFC3066LanguageCode>("DC_LANGUAGE") {
		@Override
		void validateValue(RFC3066LanguageCode value) throws InvalidMetadataException {
			RFC3066LanguageCode.validate(value); 
		}

		@Override
		protected MetadataValue<RFC3066LanguageCode> deserialize(DataInput in)
				throws IOException {
			return RFC3066LanguageCode.deserialize(in);
		}

		@Override
		protected void serialize(DataOutput out,
				MetadataValue<RFC3066LanguageCode> value) throws IOException {
			RFC3066LanguageCode.serialize(out, value);
		};
	};

	/** dc:Relation 
		Content: A reference to a related resource.
		Occurrence: Optional
	 */ 
	public final static MetadataField<String> DC_RELATION = new MetadataStringField("DC_RELATION");

	//============================================================================================================
	// Additional Daisy dtb metadata fields
	//============================================================================================================

    /** dtb:revision 
    	Content: Non-negative integer value of the specific version of the DTB. Incremented each time the DTB is revised.
    	Occurrence: Optional. Not repeatable.
     */
	public final static MetadataField<String> DTB_REVISION = new MetadataStringField("DTB_REVISION");
	
	

	//============================================================================================================
	// Additional Literacy Bridge fields
	//============================================================================================================
	
	public final static MetadataStatisticsField LB_COPY_COUNT = new MetadataStatisticsField("LB_COPY_COUNT");
	
	public final static MetadataStatisticsField LB_OPEN_COUNT = new MetadataStatisticsField("LB_OPEN_COUNT");
	
	public final static MetadataStatisticsField LB_COMPLETION_COUNT = new MetadataStatisticsField("LB_COMPLETION_COUNT");
	
	// survey feature statistics - survey question 1 is if the audio item is useless or knowledge could be applied 
	public final static MetadataStatisticsField LB_SURVEY1_COUNT = new MetadataStatisticsField("LB_SURVEY1_COUNT");
	public final static MetadataStatisticsField LB_APPLY_COUNT = new MetadataStatisticsField("LB_APPLY_COUNT");
	public final static MetadataStatisticsField LB_NOHELP_COUNT = new MetadataStatisticsField("LB_USELESS_COUNT");
	
	
	public final static MetadataField<String> LB_DURATION = new MetadataStringField("LB_DURATION");
	public final static MetadataField<String> LB_MESSAGE_FORMAT = new MetadataStringField("LB_MESSAGE_FORMAT");
	public final static MetadataField<String> LB_TARGET_AUDIENCE = new MetadataStringField("LB_TARGET_AUDIENCE");
	public final static MetadataField<String> LB_DATE_RECORDED = new MetadataStringField("LB_DATE_RECORDED");	public final static MetadataField<String> LB_KEYWORDS = new MetadataStringField("LB_KEYWORDS");
	public final static MetadataField<String> LB_TIMING = new MetadataStringField("LB_TIMING");
	public final static MetadataField<String> LB_PRIMARY_SPEAKER = new MetadataStringField("LB_PRIMARY_SPEAKER");
	public final static MetadataField<String> LB_GOAL = new MetadataStringField("LB_GOAL");
	public final static MetadataField<String> LB_ENGLISH_TRANSCRIPTION = new MetadataStringField("LB_ENGLISH_TRANSCRIPTION");
	public final static MetadataField<String> LB_NOTES = new MetadataStringField("LB_NOTES");
	public final static MetadataField<String> LB_BENEFICIARY = new MetadataStringField("LB_BENEFICIARY");
	public final static MetadataField<Integer> LB_STATUS = new MetadataIntegerField("LB_STATUS");

	
	//============================================================================================================
	
	/** Convenience collection to iterate over all metadata fields */
	// TODO: reflection?
	public Collection<MetadataField<?>> ALL_METADATA_FIELDS = new ImmutableSet.Builder<MetadataField<?>>()
																		.add(DC_TITLE)
																		.add(DC_PUBLISHER)
																		.add(DC_IDENTIFIER)
																		.add(DC_SOURCE)
																		.add(DC_LANGUAGE)
																		.add(DC_RELATION)
																		.add(DTB_REVISION)
																		.add(LB_COPY_COUNT)
																		.add(LB_OPEN_COUNT)
																		.add(LB_COMPLETION_COUNT)
																		.add(LB_SURVEY1_COUNT)
																		.add(LB_APPLY_COUNT)
																		.add(LB_NOHELP_COUNT)
																		.add(LB_DURATION)
																		.add(LB_MESSAGE_FORMAT)
																		.add(LB_TARGET_AUDIENCE)
																		.add(LB_DATE_RECORDED)
																		.add(LB_KEYWORDS)
																		.add(LB_TIMING)
																		.add(LB_PRIMARY_SPEAKER)
																		.add(LB_GOAL)
																		.add(LB_ENGLISH_TRANSCRIPTION)
																		.add(LB_NOTES)
																		.add(LB_BENEFICIARY)
																		.add(LB_STATUS)
																		.build();
}
	