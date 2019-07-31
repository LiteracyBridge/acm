package org.literacybridge.acm.store;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.payloads.PayloadHelper;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetField;
import org.apache.lucene.util.BytesRef;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

public class AudioItemDocumentFactory {
  private static final Set<MetadataField<String>> PREFIX_SEARCH_COLUMNS = ImmutableSet
      .<MetadataField<String>> builder().add(MetadataSpecification.LB_KEYWORDS)
      .add(MetadataSpecification.DC_TITLE)
      .add(MetadataSpecification.DC_IDENTIFIER)
      .add(MetadataSpecification.DC_SOURCE)
      .add(MetadataSpecification.LB_MESSAGE_FORMAT)
      .add(MetadataSpecification.LB_TARGET_AUDIENCE)
      .add(MetadataSpecification.LB_PRIMARY_SPEAKER)
      .add(MetadataSpecification.LB_NOTES).add(MetadataSpecification.LB_GOAL)
      .add(MetadataSpecification.LB_TIMING)
      .add(MetadataSpecification.LB_BENEFICIARY).build();

  public Document createLuceneDocument(AudioItem audioItem) throws IOException {
    Document doc = new Document();
    Metadata metadata = audioItem.getMetadata();
    for (MetadataField<String> field : PREFIX_SEARCH_COLUMNS) {
      if (metadata.containsField(field)) {
        MetadataValue<String> value = metadata.getMetadataValue(field);
        doc.add(new TextField(AudioItemIndex.TEXT_FIELD,
            new StringReader(value.getValue())));
      }
    }

    doc.add(new StringField(AudioItemIndex.UID_FIELD, audioItem.getId(),
        Store.YES));
    for (Category category : audioItem.getCategoryList()) {
      doc.add(new StringField(AudioItemIndex.CATEGORIES_FIELD,
          category.getId(), Store.YES));
      doc.add(new SortedSetDocValuesFacetField(
          AudioItemIndex.CATEGORIES_FACET_FIELD, category.getId()));
    }

    doc.add(new Field(AudioItemIndex.PLAYLISTS_FIELD,
        new PlaylistTokenStream(audioItem), TextField.TYPE_NOT_STORED));
    for (Playlist playlist : audioItem.getPlaylists()) {
      doc.add(new SortedSetDocValuesFacetField(
          AudioItemIndex.PLAYLISTS_FACET_FIELD, playlist.getId()));
    }

    if (metadata.containsField(MetadataSpecification.DC_LANGUAGE)) {
      MetadataValue<RFC3066LanguageCode> code = metadata
          .getMetadataValue(MetadataSpecification.DC_LANGUAGE);
      doc.add(new StringField(AudioItemIndex.LOCALES_FIELD,
          code.getValue().getLocale().getLanguage().toLowerCase(), Store.YES));
      doc.add(new SortedSetDocValuesFacetField(
          AudioItemIndex.LOCALES_FACET_FIELD, code.toString()));

    }

    if (metadata.containsField(MetadataSpecification.DTB_REVISION)) {
      doc.add(new StringField(
          AudioItemIndex.REVISION_FIELD, metadata
              .getMetadataValue(MetadataSpecification.DTB_REVISION).getValue(),
          Store.YES));
    }

    LBMetadataSerializer serializer = new LBMetadataSerializer();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(baos);
    serializer.serialize(Lists.newArrayList(audioItem.getCategoryList()),
        metadata, out);
    out.flush();
    doc.add(
        new StoredField(AudioItemIndex.RAW_METADATA_FIELD, baos.toByteArray()));

    return doc;
  }

  public static class PrefixAnalyzer extends Analyzer {
    @Override
    protected TokenStreamComponents createComponents(String field) {
      WhitespaceTokenizer tokenizer = new WhitespaceTokenizer();
      TokenFilter filter = new LowerCaseFilter(tokenizer);

      filter = new PrefixTokenFilter(filter);

      return new TokenStreamComponents(tokenizer, filter);
    }
  }

  private static class PrefixTokenFilter extends TokenFilter {
    private final CharTermAttribute termAtt = addAttribute(
        CharTermAttribute.class);
    private final PositionIncrementAttribute posIncrAtt = addAttribute(
        PositionIncrementAttribute.class);

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

  private static class PlaylistTokenStream extends TokenStream {
    private final CharTermAttribute termAtt = addAttribute(
        CharTermAttribute.class);
    private final PayloadAttribute payloadAtt = addAttribute(
        PayloadAttribute.class);

    private final AudioItem audioItem;
    private final Iterator<Playlist> playlistIterator;

    private PlaylistTokenStream(AudioItem audioItem) {
      this.audioItem = audioItem;
      this.playlistIterator = audioItem.getPlaylists().iterator();
    }

    @Override
    public boolean incrementToken() throws IOException {
      if (!playlistIterator.hasNext()) {
        return false;
      }

      Playlist playlist = playlistIterator.next();
      termAtt.setEmpty();
      termAtt.append(playlist.getId());
      int value = playlist.getAudioItemPosition(audioItem.getId());
      BytesRef payload = new BytesRef(PayloadHelper.encodeInt(value));
      payloadAtt.setPayload(payload);
      return true;
    }
  }
}
