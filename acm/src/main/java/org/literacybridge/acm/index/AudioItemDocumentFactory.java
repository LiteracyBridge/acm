package org.literacybridge.acm.index;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.literacybridge.acm.categories.Taxonomy.Category;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.db.PersistentTag;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.CommandLineParams;
import org.literacybridge.acm.metadata.Metadata;
import org.literacybridge.acm.metadata.MetadataField;
import org.literacybridge.acm.metadata.MetadataSpecification;
import org.literacybridge.acm.metadata.MetadataValue;
import org.literacybridge.acm.metadata.RFC3066LanguageCode;

import com.google.common.collect.ImmutableList;

public class AudioItemDocumentFactory {
	private static final List<MetadataField<String>> COLUMNS_TO_INDEX = ImmutableList.<MetadataField<String>>builder()
			.add(MetadataSpecification.LB_KEYWORDS)
			.add(MetadataSpecification.DC_TITLE)
			.add(MetadataSpecification.DC_IDENTIFIER)
			.add(MetadataSpecification.DC_SOURCE)
			.build();

	public Document createLuceneDocument(AudioItem audioItem) throws IOException {
		Document doc = new Document();
		Metadata metadata = audioItem.getMetadata();
		for (MetadataField<String> field : COLUMNS_TO_INDEX) {
			List<MetadataValue<String>> values = metadata.getMetadataValues(field);
			if (values != null) {
				for (MetadataValue<String> value : values) {
					doc.add(new TextField(AudioItemIndex.TEXT_FIELD, new StringReader(value.getValue())));
				}
			}
		}

		doc.add(new StringField(AudioItemIndex.UID_FIELD, audioItem.getUuid(), Store.YES));
		for (Category category : audioItem.getCategoryList()) {
			doc.add(new StringField(AudioItemIndex.CATEGORIES_FIELD, category.getUuid(), Store.YES));
		}
		for (PersistentTag tag : audioItem.getPlaylists()) {
			doc.add(new StringField(AudioItemIndex.TAGS_FIELD, tag.getName(), Store.YES));
		}
		for (MetadataValue<RFC3066LanguageCode> code : metadata.getMetadataValues(MetadataSpecification.DC_LANGUAGE)) {
			doc.add(new StringField(AudioItemIndex.LOCALES_FIELD, code.toString(), Store.YES));
		}

		return doc;
	}

	public static class PrefixAnalyzer extends Analyzer {
		@Override
		protected TokenStreamComponents createComponents(String field, Reader reader) {
			WhitespaceTokenizer tokenizer = new WhitespaceTokenizer(reader);
			TokenFilter filter = new LowerCaseFilter(tokenizer);
			filter = new PrefixTokenFilter(filter);

			return new TokenStreamComponents(tokenizer, filter);
		}
	}

	private static class PrefixTokenFilter extends TokenFilter {
		private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
		private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);

		private int currentInputTokenLength = 0;
		private int currentOutputTokenLength = 0;
		private boolean first = true;

		protected PrefixTokenFilter(TokenStream input) {
			super(input);
		}

		@Override
		public boolean incrementToken() throws IOException {
			currentOutputTokenLength++;
			if (currentInputTokenLength <= currentOutputTokenLength) {
				if (!input.incrementToken()) {
					return false;
				}
				currentInputTokenLength = termAtt.length();
				currentOutputTokenLength = 1;
				posIncrAtt.setPositionIncrement(1);
			}

			if (first) {
				first = false;
			} else {
				posIncrAtt.setPositionIncrement(0);
			}
			termAtt.setLength(currentOutputTokenLength);
			return true;
		}

		@Override
		public void reset() throws IOException {
			super.reset();
			currentInputTokenLength = 0;
			currentOutputTokenLength = 0;
			first = true;
		}
	}

	public static void main(String[] args) throws Exception {
		CommandLineParams params = new CommandLineParams();
		params.disableUI = true;
		params.readonly = true;
		params.sandbox = true;
		params.sharedACM = "ACM-UWR";
		Application.startUp(params);
		long start = System.currentTimeMillis();
		AudioItemIndex index = new AudioItemIndex();
		long end = System.currentTimeMillis();
		System.out.println((end - start) + "ms.");
	}
}
