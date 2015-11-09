package org.literacybridge.acm.index;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetField;
import org.apache.lucene.util.Version;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.LBMetadataSerializer;
import org.literacybridge.acm.store.Metadata;
import org.literacybridge.acm.store.MetadataField;
import org.literacybridge.acm.store.MetadataSpecification;
import org.literacybridge.acm.store.MetadataValue;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.acm.store.RFC3066LanguageCode;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

public class AudioItemDocumentFactory {
    private static final Set<MetadataField<String>> PREFIX_SEARCH_COLUMNS = ImmutableSet.<MetadataField<String>>builder()
            .add(MetadataSpecification.LB_KEYWORDS)
            .add(MetadataSpecification.DC_TITLE)
            .add(MetadataSpecification.DC_IDENTIFIER)
            .add(MetadataSpecification.DC_SOURCE)
            .add(MetadataSpecification.LB_MESSAGE_FORMAT)
            .add(MetadataSpecification.LB_TARGET_AUDIENCE)
            .add(MetadataSpecification.LB_PRIMARY_SPEAKER)
            .add(MetadataSpecification.LB_NOTES)
            .add(MetadataSpecification.LB_GOAL)
            .add(MetadataSpecification.LB_TIMING)
            .add(MetadataSpecification.LB_BENEFICIARY)
            .build();

    public Document createLuceneDocument(AudioItem audioItem) throws IOException {
        Document doc = new Document();
        Metadata metadata = audioItem.getMetadata();
        for (MetadataField<String> field : PREFIX_SEARCH_COLUMNS) {
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
            doc.add(new SortedSetDocValuesFacetField(AudioItemIndex.CATEGORIES_FACET_FIELD, category.getUuid()));
        }

        for (Playlist tag : audioItem.getPlaylists()) {
            doc.add(new StringField(AudioItemIndex.TAGS_FIELD, tag.getName(), Store.YES));
        }
        for (MetadataValue<RFC3066LanguageCode> code : metadata.getMetadataValues(MetadataSpecification.DC_LANGUAGE)) {
            doc.add(new StringField(AudioItemIndex.LOCALES_FIELD, code.getValue().getLocale().getLanguage().toLowerCase(), Store.YES));
            doc.add(new SortedSetDocValuesFacetField(AudioItemIndex.LOCALES_FACET_FIELD, code.toString()));
        }

        doc.add(new StringField(AudioItemIndex.REVISION_FIELD,
                metadata.getMetadataValues(MetadataSpecification.DTB_REVISION).get(0).getValue(), Store.YES));

        LBMetadataSerializer serializer = new LBMetadataSerializer();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        serializer.serialize(Lists.newArrayList(audioItem.getCategoryList()), metadata, out);
        out.flush();
        doc.add(new StoredField(AudioItemIndex.RAW_METADATA_FIELD, baos.toByteArray()));

        return doc;
    }

    public static class PrefixAnalyzer extends Analyzer {
        @Override
        protected TokenStreamComponents createComponents(String field, Reader reader) {
            WhitespaceTokenizer tokenizer = new WhitespaceTokenizer(Version.LUCENE_47, reader);
            TokenFilter filter = new LowerCaseFilter(Version.LUCENE_47, tokenizer);

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
            if (currentInputTokenLength < currentOutputTokenLength) {
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
}
