package main.java.org.literacybridge.acm.metadata;

public interface MetadataSpecification {
	/** dc:Title 
		Content: The title of the DTB, including any subtitles.
		Occurrence: Required
	*/
	public final static MetadataField<String> DC_TITLE = new MetadataField<String>() {};
	
	/** dc:Creator 
		Content: Names of primary author or creator of the intellectual content of the publication.
		Occurrence: Optional (not all documents have known creators) - recommended.
		Added attributes:
			* role -- (optional) The function performed by the creator (e.g., author, editor). See Publication Structure for details on normative list of values.
			* file-as -- (optional) A normalized form of the contents suitable for machine processing.
	 */
	public final static Attribute<String> DC_CREATOR_ROLE = new Attribute<String>() {};
	public final static Attribute<String> DC_CREATOR_FILE_AS = new Attribute<String>() {};
	public final static MetadataField<String> DC_CREATOR = new MetadataField<String>(DC_CREATOR_ROLE, DC_CREATOR_FILE_AS) {};


	/** dc:Subject 
		Content: The topic of the content of the publication.
		Occurrence: Optional - recommended.
	 */ 
	public final static MetadataField<String> DC_SUBJECT = new MetadataField<String>() {};
	

	/** dc:Description 
		Content: Plain text describing the publication's content.
		Occurrence: Optional
	 */
	public final static MetadataField<String> DC_DESCRIPTION = new MetadataField<String>() {};

	/** dc:Publisher 
		Content: The agency responsible for making the DTB available. (Compare dtb:sourcePublisher and dtb:producer.)
		Occurrence: Required
	 */
	public final static MetadataField<String> DC_PUBLISHER = new MetadataField<String>() {};

	/** dc:Contributor 
		Content: A party whose contribution to the publication is secondary to those named in dc:Creator.
		Occurrence: Optional
		Added attributes:
			* role -- (optional) The function performed by the contributor (e.g., translator, compiler). See Publication Structure for details on normative list of values.
			* file-as -- (optional) A normalized form of the contents suitable for machine processing.
	*/
	public final static Attribute<String> DC_CONTRIBUTOR_ROLE = new Attribute<String>() {};
	public final static Attribute<String> DC_CONTRIBUTOR_FILE_AS  = new Attribute<String>() {};
	public final static MetadataField<String> DC_CONTRIBUTOR = new MetadataField<String>(DC_CONTRIBUTOR_ROLE, DC_CONTRIBUTOR_FILE_AS) {};


	/** dc:Date 
		Content: Date of publication of the DTB. (Compare dtb:sourceDate and dtb:producedDate.) In format from [ISO8601]; the syntax is YYYY[-MM[-DD]] with a mandatory 
	         	 4-digit year, an optional 2-digit month, and, if the month is present, an optional 2-digit day of month.
		Occurrence: Required
		Added attributes:
	 		* event -- (optional) Significant occurrence related to publication of the DTB. Allows repetition of dc:Date to describe, for example, multiple revisions. 
			   		   Best practice is to use dtb:revision and dtb:revisionDate instead. 
	*/
	public final static Attribute<String> DC_DATE_EVENT = new Attribute<String>() {};
	public final static MetadataField<String> DC_DATE = new MetadataField<String>(DC_DATE_EVENT) {};

	/** dc:Type 
		Content: The nature of the content of the DTB (i.e., sound, text, image). Best practice is to draw from the Dublin Core's enumerated list [DC-Type].
		Occurrence: Optional
	 */ 
	public final static MetadataField<String> DC_Type = new MetadataField<String>() {};

	/** dc:Format 
		Content: The standard or specification to which the DTB was produced. Values of dc:Format in a DTB conforming to this standard are valid only if they read "ANSI/NISO Z39.86-2005".
		Occurrence: Required
	 */
	public final static MetadataField<String> DC_FORMAT = new MetadataField<String>() {};

	/** dc:Identifier 
		Content: A string or number identifying the DTB. One instance of this element, that which is referenced from the packageunique-identifier attribute, must include an id.
		Occurrence: Required
		Added attributes:
	 		* scheme -- (optional) The name of the system or authority that generated or assigned the identifier. For example, "DOI", "ISBN", or "DTB".
	 */  
	public final static Attribute<String> DC_IDENTIFIER_SCHEME = new Attribute<String>() {};
	public final static MetadataField<String> DC_IDENTIFIER = new MetadataField<String>(DC_IDENTIFIER_SCHEME) {};

	/** dc:Source 
		Content: A reference to a resource (e.g., a print original, ebook, etc.) from which the DTB is derived. Best practice is to use the ISBN when available.
		Occurrence: Optional - recommended.
	 */ 
	public final static MetadataField<String> DC_SOURCE = new MetadataField<String>() {};

	/** dc:Language 
		Content: Language of the content of the publication. An [RFC 3066] language code. For Sweden: "sv" or "sv-SE"; for UK: "en" or "en-GB"; for US: "en" or "en-US"; etc.
		Occurrence: Required
	 */ 
	public final static MetadataField<String> DC_LANGUAGE = new MetadataField<String>() {};

	/** dc:Relation 
		Content: A reference to a related resource.
		Occurrence: Optional
	 */ 
	public final static MetadataField<String> DC_RELATION = new MetadataField<String>() {};

	/** dc:Coverage 
		Content: The extent or scope of the content of the resource. Not expected to be used for DTBs.
		Occurrence: Optional
	 */ 
	public final static MetadataField<String> DC_COVERAGE = new MetadataField<String>() {};

	/** dc:Rights 
		Content: Information about rights held in and over the DTB. (Compare dtb:sourceRights.)
		Occurrence: Optional
	 */ 
	public final static MetadataField<String> DC_RIGHTS  = new MetadataField<String>() {};


}
