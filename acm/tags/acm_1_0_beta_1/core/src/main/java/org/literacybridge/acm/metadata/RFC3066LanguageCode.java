package org.literacybridge.acm.metadata;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.StringTokenizer;

import org.literacybridge.acm.utils.IOUtils;

public class RFC3066LanguageCode {
	private String[] codes;
	
	public RFC3066LanguageCode(String code) {
		// parse the string
		StringTokenizer tokenizer = new StringTokenizer(code, "-");
		int numTokens = tokenizer.countTokens();
		codes = new String[numTokens];
		
		int i = 0;
		while (tokenizer.hasMoreTokens()) {
			codes[i++] = tokenizer.nextToken();
		}
	}
	
	public static boolean validate(RFC3066LanguageCode code) {
		if (code.codes == null || code.codes.length == 0) {
			return false;
		}
		
		// TODO: validate language and country codes
		return true;
	}
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append(codes[0]);
		for (int i = 1; i < codes.length; i++) {
			b.append("-");
			b.append(codes[i]);
		}
		return b.toString();
	}
	
	public static MetadataValue<RFC3066LanguageCode> deserialize(DataInput in) throws IOException {
		String value = IOUtils.readUTF8(in);
		return new MetadataValue<RFC3066LanguageCode>(new RFC3066LanguageCode(value));
	}
	
	public static void serialize(DataOutput out, MetadataValue<RFC3066LanguageCode> value) throws IOException {
		IOUtils.writeAsUTF8(out, value.toString());
	}
}
