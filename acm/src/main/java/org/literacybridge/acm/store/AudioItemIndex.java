package org.literacybridge.acm.store;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.payloads.PayloadHelper;
import org.apache.lucene.analysis.tokenattributes.TermToBytesRefAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.LabelAndValue;
import org.apache.lucene.facet.sortedset.DefaultSortedSetDocValuesReaderState;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetCounts;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.SegmentInfos;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MultiCollector;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class AudioItemIndex {
  private static final Logger LOG = Logger
      .getLogger(AudioItemIndex.class.getName());

  public static final String TEXT_FIELD = "text";
  public static final String UID_FIELD = "uid";
  public static final String CATEGORIES_FIELD = "categories";
  public static final String CATEGORIES_FACET_FIELD = "categories_facet";
  public static final String PLAYLISTS_FIELD = "playlists";
  public static final String PLAYLISTS_FACET_FIELD = "playlists_facet";
  public static final String LOCALES_FIELD = "locales";
  public static final String LOCALES_FACET_FIELD = "locales_facet";
  public static final String REVISION_FIELD = "rev";
  public static final String RAW_METADATA_FIELD = "raw_data";

  public static final String PLAYLIST_NAMES_COMMIT_DATA = "playlist_names";
  public static final String MAX_PLAYLIST_UID_COMMIT_DATA = "max_playlist_uuid";

  private AudioItemDocumentFactory factory = new AudioItemDocumentFactory();
  private final Directory dir;
  private SearcherManager searcherManager;

  private final FacetsConfig facetsConfig;
  private final QueryAnalyzer queryAnalyzer;
  private final Taxonomy taxonomy;

  private int currentMaxPlaylistUuid;

  private AudioItemIndex(Directory dir, Taxonomy taxonomy) throws IOException {
    this.dir = dir;
    this.taxonomy = taxonomy;

    facetsConfig = new FacetsConfig();
    facetsConfig.setMultiValued(AudioItemIndex.CATEGORIES_FACET_FIELD, true);
    facetsConfig.setMultiValued(AudioItemIndex.LOCALES_FACET_FIELD, true);
    facetsConfig.setMultiValued(AudioItemIndex.PLAYLISTS_FACET_FIELD, true);

    queryAnalyzer = new QueryAnalyzer();
    searcherManager = new SearcherManager(dir, new SearcherFactory());
    readPlaylistNames();
  }

  private final String generateNewPlaylistUuid() {
    return Integer.toString(currentMaxPlaylistUuid++);
  }

  private final IndexWriter newWriter(OpenMode openMode) throws IOException {
    IndexWriterConfig config = new IndexWriterConfig(
        new AudioItemDocumentFactory.PrefixAnalyzer());
    config.setOpenMode(openMode);
    return new IndexWriter(dir, config) {
      @Override
      public void close() throws IOException {
        super.close();
        if (searcherManager != null) {
          searcherManager.maybeRefreshBlocking();
        }
      }
    };
  }

  public Transaction newTransaction(MetadataStore store) throws IOException {
    return new Transaction(store, this, newWriter(OpenMode.APPEND));
  }

  public static boolean indexExists(File path) throws IOException {
    if (!path.isDirectory()) {
      return false;
    }

    return DirectoryReader.indexExists(FSDirectory.open(path.toPath()));
  }

  public static AudioItemIndex newIndex(File path, Taxonomy taxonomy)
      throws IOException {
    if (!path.exists()) {
      boolean success = path.mkdirs();
      if (!success) {
        throw new IOException("Unable to create index directory " + path);
      }
    }

    if (indexExists(path)) {
      throw new IOException("Index already exists in " + path);
    }

    IndexWriterConfig config = new IndexWriterConfig(
        new AudioItemDocumentFactory.PrefixAnalyzer());
    config.setOpenMode(OpenMode.CREATE);
    Directory dir = FSDirectory.open(path.toPath());
    // create empty index
    new IndexWriter(dir, config).close();

    return new AudioItemIndex(dir, taxonomy);
  }

  public static AudioItemIndex load(File path, Taxonomy taxonomy)
      throws IOException {
    if (!indexExists(path)) {
      throw new IOException("Index does not exist in " + path);
    }

    return new AudioItemIndex(FSDirectory.open(path.toPath()), taxonomy);
  }

  public boolean updateAudioItem(AudioItem audioItem, IndexWriter writer)
      throws IOException {
    Document oldDoc = getDocument(audioItem.getUuid());

    if (oldDoc == null) {
      addAudioItem(audioItem, writer);
      return true;
    } else {
      Document doc = factory.createLuceneDocument(audioItem);
      writer.updateDocument(new Term(UID_FIELD, audioItem.getUuid()),
          facetsConfig.build(doc));
      return false;
    }
  }

  private void addAudioItem(AudioItem audioItem, IndexWriter writer)
      throws IOException {
    Document doc = factory.createLuceneDocument(audioItem);
    writer.addDocument(facetsConfig.build(doc));
  }

  private void addTextQuery(BooleanQuery.Builder bq, String filterString)
      throws IOException {
    if (filterString == null || filterString.isEmpty()) {
      bq.add(new MatchAllDocsQuery(), Occur.MUST);
    } else {
      TokenStream ts = queryAnalyzer.tokenStream(TEXT_FIELD, filterString);
      TermToBytesRefAttribute termAtt = ts
          .addAttribute(TermToBytesRefAttribute.class);
      ts.reset();

      while (ts.incrementToken()) {
        bq.add(new TermQuery(
            new Term(TEXT_FIELD, BytesRef.deepCopyOf(termAtt.getBytesRef()))),
            Occur.MUST);
      }
      ts.close();
    }
  }

  private Map<String, Playlist.Builder> readPlaylistNames() throws IOException {
    Map<String, Playlist.Builder> playlists = Maps.newHashMap();
    SegmentInfos infos = SegmentInfos.readLatestCommit(dir);
    Map<String, String> commitData = infos.getUserData();
    if (!commitData.isEmpty()) {
      String playlistNames = commitData.get(PLAYLIST_NAMES_COMMIT_DATA);
      this.currentMaxPlaylistUuid = Math.max(currentMaxPlaylistUuid,
          Integer.parseInt(commitData.get(MAX_PLAYLIST_UID_COMMIT_DATA)));
      StringTokenizer tokenizer = new StringTokenizer(playlistNames, ",");
      while (tokenizer.hasMoreTokens()) {
        String[] pair = tokenizer.nextToken().split(":");
        String uuid = pair[0];
        String name = pair[1];
        playlists.put(uuid, Playlist.builder().withUuid(uuid).withName(name));
      }
    }
    return playlists;
  }

  private void storePlaylistNames(Iterable<Playlist> playlists,
      IndexWriter writer) throws IOException {
    StringBuilder builder = new StringBuilder();
    for (Playlist playlist : playlists) {
      builder.append(playlist.getUuid());
      builder.append(':');
      builder.append(playlist.getName());
      builder.append(',');
    }
    if (builder.length() > 0) {
      // remove trailing ','
      builder.setLength(builder.length() - 1);
    }
    Map<String, String> commitData = Maps.newHashMap();
    commitData.put(PLAYLIST_NAMES_COMMIT_DATA, builder.toString());
    commitData.put(MAX_PLAYLIST_UID_COMMIT_DATA,
        Integer.toString(currentMaxPlaylistUuid));
    writer.setCommitData(commitData);
  }

  public void deletePlaylist(String uuid, Transaction t) throws IOException {
    Map<String, Playlist.Builder> playlists = readPlaylistNames();
    playlists.remove(uuid);
    storePlaylistNames(Iterables.transform(playlists.values(),
        new Function<Playlist.Builder, Playlist>() {
          @Override
          public Playlist apply(Playlist.Builder builder) {
            return builder.build();
          }
        }), t.getWriter());
  }

  public Playlist newPlaylist(String name) {
    Playlist playlist = new Playlist(generateNewPlaylistUuid());
    playlist.setName(name);
    return playlist;
  }

  public boolean updatePlaylistName(Playlist playlist, Transaction t)
      throws IOException {
    Map<String, Playlist.Builder> playlists = readPlaylistNames();
    Playlist.Builder removed = playlists.remove(playlist.getUuid());
    List<Playlist> updatedPlaylists = Lists.newLinkedList();
    for (Playlist.Builder p : playlists.values()) {
      updatedPlaylists.add(p.build());
    }
    updatedPlaylists.add(playlist);
    storePlaylistNames(updatedPlaylists, t.getWriter());
    return removed == null;
  }

  public Iterable<Playlist> getPlaylists() throws IOException {
    final Map<String, Playlist.Builder> playlists = readPlaylistNames();
    final IndexSearcher searcher = searcherManager.acquire();
    try {
      IndexReader reader = searcher.getIndexReader();
      TermsEnum termsEnum = null;
      for (LeafReaderContext leaf : reader.leaves()) {
        LeafReader leafReader = leaf.reader();
        Terms terms = leafReader.terms(PLAYLISTS_FIELD);
        if (terms != null) {
          termsEnum = terms.iterator();
          BytesRef term = null;
          while ((term = termsEnum.next()) != null) {
            String uuid = term.utf8ToString();
            Playlist.Builder playlist = playlists.get(uuid);
            if (playlist != null) {
              loadPlaylistFromPostingList(leafReader, uuid, playlist);
            }
          }
        }
      }

      List<Playlist> result = Lists.newArrayListWithCapacity(playlists.size());
      for (Playlist.Builder builder : playlists.values()) {
        result.add(builder.build());
      }
      return result;
    } finally {
      searcherManager.release(searcher);
    }
  }

  public void refresh(Playlist playlist) throws IOException {
    getPlaylist(playlist.getUuid(), playlist);
  }

  public Playlist getPlaylist(String uuid) throws IOException {
    return getPlaylist(uuid, null);
  }

  private Playlist getPlaylist(final String uuid, final Playlist playlist)
      throws IOException {
    final IndexSearcher searcher = searcherManager.acquire();
    try {
      final Playlist.Builder builder = Playlist.builder();
      builder.withUuid(uuid);
      builder.withName(uuid);
      if (playlist != null) {
        builder.withPlaylistPrototype(playlist);
      }

      IndexReader reader = searcher.getIndexReader();
      for (LeafReaderContext leaf : reader.leaves()) {
        loadPlaylistFromPostingList(leaf.reader(), uuid, builder);
      }

      return builder.build();
    } finally {
      searcherManager.release(searcher);
    }
  }

  private void loadPlaylistFromPostingList(LeafReader leafReader,
      String playlistUuid, Playlist.Builder playlistBuilder)
      throws IOException {
    // Iterate over the posting list that contains a posting for each AudioItem
    // belonging to the given Playlist
    PostingsEnum postingsEnum = leafReader.postings(
        new Term(PLAYLISTS_FIELD, playlistUuid), PostingsEnum.PAYLOADS);
    if (postingsEnum != null) {
      while (postingsEnum.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
        // important: Lucene applies deletes of documents to posting lists
        // lazily when it
        // performs a segment merge, so it is necessary here to check if this
        // audioItem is deleted
        if (leafReader.getLiveDocs() == null
            || leafReader.getLiveDocs().get(postingsEnum.docID())) {
          postingsEnum.nextPosition();
          BytesRef payload = postingsEnum.getPayload();
          int playlistPos = PayloadHelper.decodeInt(payload.bytes,
              payload.offset);
          AudioItem audioItem = loadAudioItem(
              leafReader.document(postingsEnum.docID()));
          playlistBuilder.addAudioItem(audioItem.getUuid(), playlistPos);
        }
      }
    }
  }

  public AudioItem getAudioItem(final String uuid) throws IOException {
    Document doc = getDocument(uuid);
    return doc != null ? loadAudioItem(doc) : null;
  }

  public Iterable<AudioItem> getAudioItems() throws IOException {
    final List<AudioItem> results = Lists.newArrayList();
    final IndexSearcher searcher = searcherManager.acquire();
    try {
      searcher.search(new MatchAllDocsQuery(), new Collector() {
        @Override
        public LeafCollector getLeafCollector(final LeafReaderContext context)
            throws IOException {
          return new LeafCollector() {
            @Override
            public void setScorer(Scorer scorer) throws IOException {
            }

            @Override
            public void collect(int docId) throws IOException {
              results.add(loadAudioItem(context.reader().document(docId)));
            }
          };
        }

        @Override
        public boolean needsScores() {
          return false;
        }
      });

      return results;
    } finally {
      searcherManager.release(searcher);
    }
  }

  public void deleteAudioItem(final String uuid, Transaction t)
      throws IOException {
    t.getWriter().deleteDocuments(new Term(UID_FIELD, uuid));
  }

  private Document getDocument(final String uuid) throws IOException {
    final AtomicReference<Document> result = new AtomicReference<Document>();

    final IndexSearcher searcher = searcherManager.acquire();
    try {
      searcher.search(new TermQuery(new Term(UID_FIELD, uuid)),
          new Collector() {
            @Override
            public LeafCollector getLeafCollector(
                final LeafReaderContext context) throws IOException {
              return new LeafCollector() {
                @Override
                public void setScorer(Scorer scorer) throws IOException {
                }

                @Override
                public void collect(int docId) throws IOException {
                  result.set(context.reader().document(docId));
                }
              };
            }

            @Override
            public boolean needsScores() {
              // TODO Auto-generated method stub
              return false;
            }
          });
    } finally {
      searcherManager.release(searcher);
    }

    return result.get();
  }

  public void refresh(AudioItem audioItem) throws IOException {
    Document doc = getDocument(audioItem.getUuid());
    if (doc == null) {
      throw new IOException("AudioItem not found.");
    }

    loadAudioItem(doc, audioItem);
  }

  private AudioItem loadAudioItem(Document doc) throws IOException {
    AudioItem audioItem = new AudioItem(doc.get(UID_FIELD));
    loadAudioItem(doc, audioItem);
    return audioItem;
  }

  private void loadAudioItem(Document doc, AudioItem audioItem)
      throws IOException {
    LBMetadataSerializer deserializer = new LBMetadataSerializer();
    Set<Category> categories = new HashSet<Category>();
    BytesRef ref = doc.getBinaryValue(RAW_METADATA_FIELD);
    deserializer.deserialize(audioItem.getMetadata(), taxonomy, categories,
        new DataInputStream(
            new ByteArrayInputStream(ref.bytes, ref.offset, ref.length)));

    for (Category category : categories) {
      audioItem.addCategory(category);
    }
  }

  public SearchResult search(String filterString, Playlist selectedTag)
      throws IOException {
    BooleanQuery.Builder bq = new BooleanQuery.Builder();
    addTextQuery(bq, filterString);
    if (selectedTag != null) {
      bq.add(new TermQuery(new Term(PLAYLISTS_FIELD, selectedTag.getUuid())),
          Occur.MUST);
    }
    return search(bq.build());
  }

  public SearchResult search(String filterString,
      List<Category> filterCategories, List<Locale> locales)
      throws IOException {
    BooleanQuery.Builder bq = new BooleanQuery.Builder();
    addTextQuery(bq, filterString);

    if (filterCategories != null && !filterCategories.isEmpty()) {
      BooleanQuery.Builder categoriesQuery = new BooleanQuery.Builder();
      for (Category category : filterCategories) {
        categoriesQuery.add(
            new TermQuery(new Term(CATEGORIES_FIELD, category.getUuid())),
            Occur.SHOULD);
      }
      bq.add(categoriesQuery.build(), Occur.MUST);
    }

    if (locales != null && !locales.isEmpty()) {
      BooleanQuery.Builder localesQuery = new BooleanQuery.Builder();
      for (Locale locale : locales) {
        localesQuery.add(
            new TermQuery(
                new Term(LOCALES_FIELD, locale.getLanguage().toLowerCase())),
            Occur.SHOULD);
      }
      bq.add(localesQuery.build(), Occur.MUST);
    }

    return search(bq.build());
  }

  private SearchResult search(Query query) throws IOException {
    final FacetsCollector facetsCollector = new FacetsCollector();
    final Set<String> results = Sets.newHashSet();
    final IndexSearcher searcher = searcherManager.acquire();
    try {
      Collector collector = MultiCollector.wrap(facetsCollector,
          new Collector() {
            @Override
            public LeafCollector getLeafCollector(
                final LeafReaderContext context) throws IOException {
              return new LeafCollector() {
                @Override
                public void setScorer(Scorer scorer) throws IOException {
                }

                @Override
                public void collect(int docId) throws IOException {
                  Document doc = context.reader().document(docId);
                  results.add(doc.get(UID_FIELD));
                }
              };
            }

            @Override
            public boolean needsScores() {
              return false;
            }
          });

      searcher.search(query, collector);

      Map<String, Integer> categoryFacets = Maps.newHashMap();
      Map<String, Integer> localeFacets = Maps.newHashMap();
      Map<String, Integer> playlistFacets = Maps.newHashMap();

      try {
        SortedSetDocValuesFacetCounts facetCounts = new SortedSetDocValuesFacetCounts(
            new DefaultSortedSetDocValuesReaderState(searcher.getIndexReader()),
            facetsCollector);
        List<FacetResult> facetResults = facetCounts.getAllDims(1000);
        for (FacetResult r : facetResults) {
          if (r.dim.equals(CATEGORIES_FACET_FIELD)) {
            for (LabelAndValue lv : r.labelValues) {
              categoryFacets.put(lv.label, lv.value.intValue());
            }
          }
          if (r.dim.equals(LOCALES_FACET_FIELD)) {
            for (LabelAndValue lv : r.labelValues) {
              localeFacets.put(lv.label, lv.value.intValue());
            }

          }
          if (r.dim.equals(PLAYLISTS_FACET_FIELD)) {
            for (LabelAndValue lv : r.labelValues) {
              playlistFacets.put(lv.label, lv.value.intValue());
            }

          }
        }
      } catch (IllegalArgumentException e) {
        // With empty indexes it can happen that Lucene throws an exception here
        // due to missing facet data
      }

      SearchResult result = new SearchResult(
          searcher.getIndexReader().numDocs(), categoryFacets, localeFacets,
          playlistFacets, Lists.newArrayList(results));

      return result;
    } finally {
      searcherManager.release(searcher);
    }
  }

  public static class QueryAnalyzer extends Analyzer {
    @Override
    protected TokenStreamComponents createComponents(String field) {
      WhitespaceTokenizer tokenizer = new WhitespaceTokenizer();
      TokenFilter filter = new LowerCaseFilter(tokenizer);
      return new TokenStreamComponents(tokenizer, filter);
    }
  }
}
