package org.literacybridge.acm.metadata;

import java.util.StringTokenizer;

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
}
