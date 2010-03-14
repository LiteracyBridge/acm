package org.literacybridge.acm.metadata;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.literacybridge.acm.metadata.LBMetadataEncodingVersions.Version;

/**
 * Literacy bridge uses a backwards- and forwards-compatible metadata serialization
 * format. Besides compatibility the primary goal of this format is decoding efficiency
 * to minimize computational costs on talkingbook devices.
 * Note that to achieve this efficiency goal it is also important that the individual
 * {@link MetadataField} subclasses use efficient serialization encodings.
 * 
 * The backwards-compatibility is achieved by storing a Metadata revision in the
 * Header. A Decoder must read this revision first and evaluate if it"knows" the revision,
 * i.e. if decoder_version >= encoder_version. If it does, then it knows the exact format
 * and can skip the remainder of the header by making use the the numberOfFields value, and
 * decode the data portion directly.
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
		int serializedVersion = in.readInt();
		int numberOfFields = in.readInt();
		
		Iterator<FieldInfo> fieldsToDecode;
		
		if (serializedVersion <= METADATA_VERSION_CURRENT) {
			// decode efficiently by skipping the remainder of the header
			int bytesToSkip = NUM_BYTES_PER_FIELD_INFO * numberOfFields;
			int actuallySkipped = in.skipBytes(bytesToSkip);
			if (actuallySkipped != bytesToSkip) {
				throw new IOException("Unable to data section. Metadata corrupt.");
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
				int fieldID = in.readShort();
				int fieldLength = in.readInt();
				
				fields.add(new FieldInfo(fieldID, fieldLength));
			}
			
			fieldsToDecode = fields.iterator();
		}
		
		Metadata metadata = new Metadata();
		
		// no deserialize the metadata fields
		// TODO: implement
		
		return metadata;
	}

	@Override
	public void serialize(Metadata metadata, DataOutput out) throws IOException {
		// TODO Auto-generated method stub
		
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
