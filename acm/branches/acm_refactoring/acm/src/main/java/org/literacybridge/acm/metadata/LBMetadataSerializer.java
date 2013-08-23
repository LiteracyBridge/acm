package org.literacybridge.acm.metadata;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.literacybridge.acm.categories.Taxonomy.Category;
import org.literacybridge.acm.db.PersistentCategory;
import org.literacybridge.acm.utils.IOUtils;

/**
 * Literacy bridge uses a backwards- and forwards-compatible metadata serialization
 * format. Besides compatibility the primary goal of this format is decoding efficiency
 * to minimize computational costs on talkingbook devices.
 * Note that to achieve this efficiency goal it is also important that the individual
 * {@link MetadataField} subclasses use efficient serialization encodings.
 * 
 * The backwards-compatibility is achieved by storing a Metadata revision in the
 * Header. A Decoder must read this revision first and evaluate if it "knows" the revision,
 * i.e. if decoder_version >= encoder_version. If it does, then it knows the exact format
 * and can skip the remainder of the header by making use of the the numberOfFields value, 
 * and decode the data portion directly.
 * 
 * If the revision is not known by the decoder, then it was written by a newer version of
 * the software than the one that is decoding. In that case the decoding is done in forwards-
 * compatibility mode: the FieldInfos section of the Header is decoded first to determine
 * which individual fields are known by the decoder. Fields that were introduced in a later
 * software version than the decoder will not be known and hence skipped by the decoder by
 * making use of the length information that is stored for every field.
 * 
 * All known fields are decoded individually.
 * 
 */
public class LBMetadataSerializer extends MetadataSerializer {
	public static final int METADATA_VERSION_1 = 1;
	
	public static final int METADATA_VERSION_CURRENT = METADATA_VERSION_1;
	
	@Override
	public void deserialize(Metadata metadata, Collection<Category> categories, DataInput in) throws IOException {
		// first read the metadata version and number of field infos in the header
		int serializedVersion = IOUtils.readLittleEndian32(in);
		int numberOfFields = IOUtils.readLittleEndian32(in);
		
		for (int i = 0; i < numberOfFields; i++) {
			int fieldID = IOUtils.readLittleEndian16(in);
			int fieldLength = IOUtils.readLittleEndian32(in);

			if (fieldID == LBMetadataIDs.CATEGORY_FIELD_ID) {
				deserializeCategories(categories, in);
			} else {
				MetadataField<?> field = LBMetadataIDs.FieldToIDMap.inverse().get(fieldID);
				if (field == null) {
					// unknown field - for forward-compatibility we must skip it
					in.skipBytes(fieldLength);
				} else {
					deserializeField(metadata, field, in);
				}
			}
		}
	}

	private final <T> void deserializeField(Metadata metadata, MetadataField<T> field, DataInput in) throws IOException {
		int numValues = (in.readByte() & 0xff);
		for (int i = 0; i < numValues; i++) {
			MetadataValue<T> value = field.deserialize(in);
			metadata.setMetadataField(field, value);
		}
	}
	
	@Override
	public void serialize(Collection<Category> categories, Metadata metadata, DataOutput headerOut) throws IOException {
		IOUtils.writeLittleEndian32(headerOut, METADATA_VERSION_CURRENT);
		IOUtils.writeLittleEndian32(headerOut, 1 + metadata.getNumberOfFields());
		
		// first encode categories
		IOUtils.writeLittleEndian16(headerOut, LBMetadataIDs.CATEGORY_FIELD_ID);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream serializedDataPortion = new DataOutputStream(baos);
		serializeCategories(categories, serializedDataPortion);
		IOUtils.writeLittleEndian32(headerOut, baos.size());
		serializedDataPortion.flush();
		headerOut.write(baos.toByteArray());
		serializedDataPortion.close();
		
		final Iterator<MetadataField<?>> it = metadata.getFieldsIterator();
		while (it.hasNext()) {
			MetadataField<?> field = it.next();
			if (LBMetadataIDs.FieldToIDMap.containsKey(field)) {
				baos = new ByteArrayOutputStream();
				serializedDataPortion = new DataOutputStream(baos);
	
				serializeField(metadata, field, serializedDataPortion);
				// encode field id
				IOUtils.writeLittleEndian16(headerOut, LBMetadataIDs.FieldToIDMap.get(field));
				// encode field length
				IOUtils.writeLittleEndian32(headerOut, baos.size());
				serializedDataPortion.flush();
				headerOut.write(baos.toByteArray());
				serializedDataPortion.close();
			}
		}
		
	}
	
	private final <T> void serializeField(Metadata metadata, MetadataField<T> field, DataOutput out) throws IOException {
		List<MetadataValue<T>> values = metadata.getMetadataValues(field);
		if (values != null) {
			out.writeByte((byte) values.size());
			for (MetadataValue<T> value : values) {
				field.serialize(out, value);
			}
		} else {
			out.write((byte) 0);
		}
			
	}

	private final void serializeCategories(Collection<Category> categories, DataOutput out) throws IOException {
		if (categories == null) {
			out.writeByte((byte) 0);
		} else {
			out.writeByte((byte) categories.size());
			for (Category c : categories) {
				IOUtils.writeAsUTF8(out, c.getUuid());
			}
		}
	}
	
	private final void deserializeCategories(Collection<Category> categories, DataInput in) throws IOException {
		int numValues = (in.readByte() & 0xff);
		for (int i = 0; i < numValues; i++) {
			String catID = IOUtils.readUTF8(in);
			PersistentCategory mCat = PersistentCategory.getFromDatabase(catID);
			if (mCat != null) {
				Category cat = new Category(mCat);
				categories.add(cat);
			}
		}
	}
}
