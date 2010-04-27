package org.literacybridge.acm.metadata;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.literacybridge.acm.metadata.LBMetadataEncodingVersions.Version;
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
	
	private static final int NUM_BYTES_PER_FIELD_INFO = 6;

	
	@Override
	public Metadata deserialize(DataInput in) throws IOException {
		// first read the metadata version and number of field infos in the header
		int serializedVersion = IOUtils.readLittleEndian32(in);
		int numberOfFields = IOUtils.readLittleEndian32(in);
		
		Iterator<FieldInfo> fieldsToDecode;
		
		if (serializedVersion <= METADATA_VERSION_CURRENT) {
			// decode efficiently by skipping the remainder of the header
			int bytesToSkip = NUM_BYTES_PER_FIELD_INFO * numberOfFields;
			int actuallySkipped = in.skipBytes(bytesToSkip);
			if (actuallySkipped != bytesToSkip) {
				throw new IOException("Unable to seek to data section. Metadata corrupt.");
			}
			
			// lookup which fields need to be decoded for the known format
			Version version = LBMetadataEncodingVersions.getVersion(serializedVersion);
			final Iterator<Integer> fieldIDsIterator = version.getFieldIDs().iterator();
			
			fieldsToDecode = new Iterator<FieldInfo>() {

				@Override
				public boolean hasNext() {
					return fieldIDsIterator.hasNext();
				}

				@Override
				public FieldInfo next() {
					return new FieldInfo(fieldIDsIterator.next(), -1);
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException("remove not supported.");
				}
			};
			
		} else {
			// decode header to build up list of fields to read
			List<FieldInfo> fields = new LinkedList<FieldInfo>();
			
			for (int i = 0; i < numberOfFields; i++) {
				int fieldID = IOUtils.readLittleEndian16(in);
				int fieldLength = IOUtils.readLittleEndian32(in);
				
				fields.add(new FieldInfo(fieldID, fieldLength));
			}
			
			fieldsToDecode = fields.iterator();
		}
		
		Metadata metadata = new Metadata();
		
		// now deserialize the metadata fields
		while (fieldsToDecode.hasNext()) {
			FieldInfo fieldInfo = fieldsToDecode.next();
			MetadataField<?> field = LBMetadataIDs.FieldToIDMap.inverse().get(fieldInfo.fieldID);
			if (field == null) {
				// unknown field - for forward-compatibility we must skip it
				in.skipBytes(fieldInfo.fieldLength);
			} else {
				deserializeField(metadata, field, in);
			}
		}
		
		return metadata;
	}

	private final <T> void deserializeField(Metadata metadata, MetadataField<T> field, DataInput in) throws IOException {
		int numValues = (in.readByte() & 0xff);
		for (int i = 0; i < numValues; i++) {
			MetadataValue<T> value = field.deserialize(in);
			metadata.addMetadataField(field, value);
		}
	}
	
	@Override
	public void serialize(Metadata metadata, DataOutput headerOut) throws IOException {
		IOUtils.writeLittleEndian32(headerOut, METADATA_VERSION_CURRENT);
		IOUtils.writeLittleEndian32(headerOut, metadata.getNumberOfValues());
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream serializedDataPortion = new DataOutputStream(baos);
		Iterator<MetadataField<?>> it = metadata.getFieldsIterator();
		int lastSize = 0;
		while (it.hasNext()) {
			MetadataField<?> field = it.next();
			serializeField(metadata, field, serializedDataPortion);
			int size = baos.size();
			// encode field id
			IOUtils.writeLittleEndian16(headerOut, LBMetadataIDs.FieldToIDMap.get(field));
			// encode field length
			IOUtils.writeLittleEndian32(headerOut, size - lastSize);
			lastSize = size;
		}
		
		// now the header is complete - copy over the data portion now
		serializedDataPortion.flush();
		headerOut.write(baos.toByteArray());
		serializedDataPortion.close();
	}
	
	private final <T> void serializeField(Metadata metadata, MetadataField<T> field, DataOutput out) throws IOException {
		List<MetadataValue<T>> values = metadata.getMetadataValues(field);
		out.writeByte((byte) values.size());
		for (MetadataValue<T> value : values) {
			field.serialize(out, value);
		}
	}
	
	private static final class FieldInfo {
		int fieldID;
		int fieldLength;
		
		public FieldInfo(int fieldID, int fieldLength) {
			this.fieldID = fieldID;
			this.fieldLength = fieldLength;
		}
	}
	
}
