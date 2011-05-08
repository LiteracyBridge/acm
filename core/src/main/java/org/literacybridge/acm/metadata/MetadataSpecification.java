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
	
	/** dc:Creator 
		Content: Names of primary author or creator of the intellectual content of the publication.
		Occurrence: Optional (not all documents have known creators) - recommended.
		Added attributes:
			* role -- (optional) The function performed by the creator (e.g., author, editor). See Publication Structure for details on normative list of values.
			* file-as -- (optional) A normalized form of the contents suitable for machine processing.
	 */
	public final static Attribute<String> DC_CREATOR_ROLE = new Attribute<String>();
	public final static Attribute<String> DC_CREATOR_FILE_AS = new Attribute<String>();
	public final static MetadataField<String> DC_CREATOR = new MetadataStringField("DC_CREATOR", DC_CREATOR_ROLE, DC_CREATOR_FILE_AS);


	/** dc:Subject 
		Content: The topic of the content of the publication.
		Occurrence: Optional - recommended.
	 */ 
	public final static MetadataField<String> DC_SUBJECT = new MetadataStringField("DC_SUBJECT");
	

	/** dc:Description 
		Content: Plain text describing the publication's content.
		Occurrence: Optional
	 */
	public final static MetadataField<String> DC_DESCRIPTION = new MetadataStringField("DC_DESCRIPTION");

	/** dc:Publisher 
		Content: The agency responsible for making the DTB available. (Compare dtb:sourcePublisher and dtb:producer.)
		Occurrence: Required
	 */
	public final static MetadataField<String> DC_PUBLISHER = new MetadataStringField("DC_PUBLISHER");

	/** dc:Contributor 
		Content: A party whose contribution to the publication is secondary to those named in dc:Creator.
		Occurrence: Optional
		Added attributes:
			* role -- (optional) The function performed by the contributor (e.g., translator, compiler). See Publication Structure for details on normative list of values.
			* file-as -- (optional) A normalized form of the contents suitable for machine processing.
	*/
	public final static Attribute<String> DC_CONTRIBUTOR_ROLE = new Attribute<String>();
	public final static Attribute<String> DC_CONTRIBUTOR_FILE_AS  = new Attribute<String>();
	public final static MetadataField<String> DC_CONTRIBUTOR = new MetadataStringField("DC_CONTRIBUTOR", DC_CONTRIBUTOR_ROLE, DC_CONTRIBUTOR_FILE_AS);


	/** dc:Date 
		Content: Date of publication of the DTB. (Compare dtb:sourceDate and dtb:producedDate.) In format from [ISO8601]; the syntax is YYYY[-MM[-DD]] with a mandatory 
	         	 4-digit year, an optional 2-digit month, and, if the month is present, an optional 2-digit day of month.
		Occurrence: Required
		Added attributes:
	 		* event -- (optional) Significant occurrence related to publication of the DTB. Allows repetition of dc:Date to describe, for example, multiple revisions. 
			   		   Best practice is to use dtb:revision and dtb:revisionDate instead. 
	*/
	public final static Attribute<String> DC_DATE_EVENT = new Attribute<String>();
	public final static MetadataField<ISO8601Date> DC_DATE = new MetadataField<ISO8601Date>("DC_DATE", DC_DATE_EVENT) {
		@Override
	    void validateValue(ISO8601Date value) throws InvalidMetadataException {
			// TODO: implement
		}

		@Override
		protected MetadataValue<ISO8601Date> deserialize(DataInput in)
				throws IOException {
			return ISO8601Date.deserialize(in);
		}

		@Override
		protected void serialize(DataOutput out,
				MetadataValue<ISO8601Date> value) throws IOException {
			ISO8601Date.serialize(out, value);
		};
	};

	/** dc:Type 
		Content: The nature of the content of the DTB (i.e., sound, text, image). Best practice is to draw from the Dublin Core's enumerated list [DC-Type].
		Occurrence: Optional
	 */ 
	public final static MetadataField<String> DC_TYPE = new MetadataStringField("DC_TYPE");

	/** dc:Format 
		Content: The standard or specification to which the DTB was produced. Values of dc:Format in a DTB conforming to this standard are valid only if they read "ANSI/NISO Z39.86-2005".
		Occurrence: Required
	 */
	public final static MetadataField<String> DC_FORMAT = new MetadataStringField("DC_FORMAT");

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

	/** dc:Coverage 
		Content: The extent or scope of the content of the resource. Not expected to be used for DTBs.
		Occurrence: Optional
	 */ 
	public final static MetadataField<String> DC_COVERAGE = new MetadataStringField("DC_COVERAGE");

	/** dc:Rights 
		Content: Information about rights held in and over the DTB. (Compare dtb:sourceRights.)
		Occurrence: Optional
	 */ 
	public final static MetadataField<String> DC_RIGHTS  = new MetadataStringField("DC_RIGHTS");

	//============================================================================================================
	// Additional Daisy dtb metadata fields
	//============================================================================================================

    /** dtb:revision 
    	Content: Non-negative integer value of the specific version of the DTB. Incremented each time the DTB is revised.
    	Occurrence: Optional. Not repeatable.
     */
	public final static MetadataField<String> DTB_REVISION = new MetadataStringField("DTB_REVISION");
	
    /** dtb:revisionDate 
    	Content: Date associated with the specific dtb:revision. In format from [ISO8601]; the syntax is YYYY[-MM[-DD]] with a mandatory 4-digit year, an optional 2-digit month, and, if the month is present, an optional 2-digit day of month.
    	Occurrence: Optional. Not repeatable.
	 */
	public final static MetadataField<String> DTB_REVISION_DATE = new MetadataStringField("DTB_REVISION_DATE");
	
    /** dtb:revisionDescription 
    	Content: A string describing the changes introduced in a specific dtb:revision.
    	Occurrence: Optional. Not repeatable.
	 */ 
	public final static MetadataField<String> DTB_REVISION_DESCRIPTION = new MetadataStringField("DTB_REVISION_DESCRIPTION");
	

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
	
	// currently unused
	public final static MetadataField<Integer> LB_RATING = new MetadataIntegerField("LB_RATING");


	//============================================================================================================
	
	/** Convenience collection to iterate over all metadata fields */
	// TODO: reflection?
	public Collection<MetadataField<?>> ALL_METADATA_FIELDS = new ImmutableSet.Builder<MetadataField<?>>()
																		.add(DC_TITLE)
																		.add(DC_CREATOR)
																		.add(DC_SUBJECT)
																		.add(DC_DESCRIPTION)
																		.add(DC_PUBLISHER)
																		.add(DC_CONTRIBUTOR)
																		.add(DC_DATE)
																		.add(DC_TYPE)
																		.add(DC_FORMAT)
																		.add(DC_IDENTIFIER)
																		.add(DC_SOURCE)
																		.add(DC_LANGUAGE)
																		.add(DC_RELATION)
																		.add(DC_COVERAGE)
																		.add(DC_RIGHTS)
																		.add(DTB_REVISION)
																		.add(DTB_REVISION_DATE)
																		.add(DTB_REVISION_DESCRIPTION)
																		.add(LB_COPY_COUNT)
																		.add(LB_OPEN_COUNT)
																		.add(LB_COMPLETION_COUNT)
																		.add(LB_SURVEY1_COUNT)
																		.add(LB_APPLY_COUNT)
																		.add(LB_NOHELP_COUNT)
																		.add(LB_RATING)
																		.build();
}
	