package org.literacybridge.acm.index;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicReference;

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
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.SegmentInfos;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
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
import org.apache.lucene.util.Version;
import org.literacybridge.acm.api.IDataRequestResult;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.core.DataRequestResult;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.LBMetadataSerializer;
import org.literacybridge.acm.store.MetadataStore.Transaction;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.acm.store.Playlist.Builder;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class AudioItemIndex {
    public static final String TEXT_FIELD = "text";
    public static final String UID_FIELD = "uid";
    public static final String CATEGORIES_FIELD = "categories";
    public static final String CATEGORIES_FACET_FIELD = "categories_facet";
    public static final String TAGS_FIELD = "tags";
    public static final String LOCALES_FIELD = "locales";
    public static final String LOCALES_FACET_FIELD = "locales_facet";
    public static final String REVISION_FIELD = "rev";
    public static final String RAW_METADATA_FIELD = "raw_data";
    public static final String IMPORT_ORDER_ID_FIELD = "import_order";

    public static final String PLAYLIST_NAMES_COMMIT_DATA = "playlist_names";
    public static final String MAX_PLAYLIST_UID_COMMIT_DATA = "max_playlist_uuid";

    private AudioItemDocumentFactory factory = new AudioItemDocumentFactory();
    private final Directory dir;
    private SearcherManager searcherManager;

    private final FacetsConfig facetsConfig;
    private final QueryAnalyzer queryAnalyzer;

    private int currentMaxPlaylistUuid;

    private AudioItemIndex(Directory dir, Iterable<AudioItem> audioItems) throws IOException {
        this.dir = dir;

        facetsConfig = new FacetsConfig();
        facetsConfig.setMultiValued(AudioItemIndex.CATEGORIES_FACET_FIELD, true);
        facetsConfig.setMultiValued(AudioItemIndex.LOCALES_FACET_FIELD, true);

        if (audioItems != null) {
            migrateFromDB(audioItems);
        }

        queryAnalyzer = new QueryAnalyzer();
        searcherManager = new SearcherManager(dir, new SearcherFactory());
        readPlaylistNames();
    }

    @Deprecated
    private final void migrateFromDB(Iterable<AudioItem> audioItems) throws IOException {
        Map<String, Playlist> playlists = Maps.newHashMap();
        IndexWriter writer = newWriter();
        for (AudioItem item : audioItems) {
            for (Playlist playlist : item.getPlaylists()) {
                // important to use getName() here, because in the old DB we didn't use uuids for playlists;
                // in the new Lucene index we do use uuids, which we generate here in the migration step
                if (!playlists.containsKey(playlist.getName())) {
                    playlist.setUuid(generateNewPlaylistUuid());
                    playlists.put(playlist.getName(), playlist);
                }
                playlist.setUuid(playlists.get(playlist.getName()).getUuid());
            }
            addAudioItem(item, writer);
        }

        storePlaylistNames(playlists.values(), writer);

        writer.forceMerge(1);
        writer.commit();
        writer.close();
    }

    private final String generateNewPlaylistUuid() {
        return Integer.toString(currentMaxPlaylistUuid++);
    }

    public IndexWriter newWriter() throws IOException {
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_47, new AudioItemDocumentFactory.PrefixAnalyzer());
        config.setOpenMode(OpenMode.CREATE_OR_APPEND);
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

    public static boolean indexExists(File path) throws IOException {
        if (!path.isDirectory()) {
            return false;
        }

        return DirectoryReader.indexExists(FSDirectory.open(path));
    }

    public static AudioItemIndex migrateFromDB(File path, Iterable<AudioItem> audioItems) throws IOException {
        if (indexExists(path)) {
            throw new IOException("Index already exists in " + path);
        }

        if (!path.exists()) {
            path.mkdirs();
        }

        return new AudioItemIndex(FSDirectory.open(path), audioItems);
    }

    public static AudioItemIndex load(File path) throws IOException {
        if (!indexExists(path)) {
            throw new IOException("Index does not exist in " + path);
        }

        return new AudioItemIndex(FSDirectory.open(path), null);
    }

    public void updateAudioItem(AudioItem audioItem, IndexWriter writer) throws IOException {
        Document oldDoc = getDocument(audioItem.getUuid());

        if (oldDoc == null) {
            addAudioItem(audioItem, writer);
        } else {
            Document doc = factory.createLuceneDocument(audioItem, Long.parseLong(oldDoc.get(IMPORT_ORDER_ID_FIELD)));
            writer.updateDocument(new Term(UID_FIELD, audioItem.getUuid()),
                    facetsConfig.build(doc));
        }
        ACMConfiguration.getCurrentDB().getAudioItemCache().invalidate(audioItem.getUuid());
    }

    private void addAudioItem(AudioItem audioItem, IndexWriter writer) throws IOException {
        Document doc = factory.createLuceneDocument(audioItem, System.currentTimeMillis());
        writer.addDocument(facetsConfig.build(doc));
    }

    private void addTextQuery(BooleanQuery bq, String filterString) throws IOException {
        if (filterString == null || filterString.isEmpty()) {
            bq.add(new MatchAllDocsQuery(), Occur.MUST);
        } else {
            TokenStream ts = queryAnalyzer.tokenStream(TEXT_FIELD, filterString);
            TermToBytesRefAttribute termAtt = ts.addAttribute(TermToBytesRefAttribute.class);
            ts.reset();

            while (ts.incrementToken()) {
                termAtt.fillBytesRef();
                bq.add(new TermQuery(new Term(TEXT_FIELD, BytesRef.deepCopyOf(termAtt.getBytesRef()))), Occur.MUST);
            }
            ts.close();
        }
    }

    private Map<String, Playlist.Builder> readPlaylistNames() throws IOException {
        Map<String, Playlist.Builder> playlists = Maps.newHashMap();
        SegmentInfos infos = new SegmentInfos();
        infos.read(dir);
        Map<String, String> commitData = infos.getUserData();
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
        return playlists;
    }

    private void storePlaylistNames(Iterable<Playlist> playlists, IndexWriter writer) throws IOException {
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
        commitData.put(MAX_PLAYLIST_UID_COMMIT_DATA, Integer.toString(currentMaxPlaylistUuid));
        writer.setCommitData(commitData);
    }

    public void deletePlaylist(String uuid, Transaction t) throws IOException {
        Map<String, Playlist.Builder> playlists = readPlaylistNames();
        playlists.remove(uuid);
        storePlaylistNames(Iterables.transform(playlists.values(), new Function<Playlist.Builder, Playlist>() {
            @Override public Playlist apply(Builder builder) {
                return builder.build();
            }
        }), t.getWriter());
    }

    public Playlist addPlaylist(String name, Transaction t) throws IOException {
        Playlist playlist = new Playlist(generateNewPlaylistUuid());
        playlist.setName(name);
        storePlaylistNames(Iterables.concat(Iterables.transform(readPlaylistNames().values(), new Function<Playlist.Builder, Playlist>() {
            @Override public Playlist apply(Builder builder) {
                return builder.build();
            }
        }), Lists.newArrayList(playlist)), t.getWriter());
        return playlist;
    }


    public Iterable<Playlist> getPlaylists() throws IOException {
        final Map<String, Playlist.Builder> playlists = readPlaylistNames();
        final IndexSearcher searcher = searcherManager.acquire();
        try {
            IndexReader reader = searcher.getIndexReader();
            TermsEnum termsEnum = null;
            for (AtomicReaderContext leaf : reader.leaves()) {
                AtomicReader leafReader = leaf.reader();
                Terms terms = leafReader.terms(TAGS_FIELD);
                if (terms != null) {
                    termsEnum = terms.iterator(termsEnum);
                    BytesRef term = null;
                    while ((term = termsEnum.next()) != null) {
                        String uuid = term.utf8ToString();
                        Playlist.Builder playlist = playlists.get(uuid);
                        if (playlist != null) {
                            DocsAndPositionsEnum tp = leafReader.termPositionsEnum(new Term(TAGS_FIELD, term));
                            while (tp.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
                                // TODO: we could also use the termPosition instead of a payload to store the playlist position
                                tp.nextPosition();
                                BytesRef payload = tp.getPayload();
                                int playlistPos = PayloadHelper.decodeInt(payload.bytes, payload.offset);
                                AudioItem audioItem = loadAudioItem(leafReader.document(tp.docID()));
                                playlist.addAudioItem(audioItem.getUuid(), playlistPos);
                            }
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

    public Playlist getPlaylist(String uuid) throws IOException {
        final IndexSearcher searcher = searcherManager.acquire();
        try {
            final Playlist.Builder builder = Playlist.builder();
            builder.withUuid(uuid);
            builder.withName(uuid);

            IndexReader reader = searcher.getIndexReader();
            for (AtomicReaderContext leaf : reader.leaves()) {
                AtomicReader leafReader = leaf.reader();
                DocsAndPositionsEnum tp = leafReader.termPositionsEnum(new Term(TAGS_FIELD, uuid));
                if (tp != null) {
                    while (tp.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
                        // TODO: we could also use the termPosition instead of a payload to store the playlist position
                        tp.nextPosition();
                        BytesRef payload = tp.getPayload();
                        int playlistPos = PayloadHelper.decodeInt(payload.bytes, payload.offset);
                        AudioItem audioItem = loadAudioItem(leafReader.document(tp.docID()));
                        builder.addAudioItem(audioItem.getUuid(), playlistPos);
                    }
                }
            }

            return builder.build();
        } finally {
            searcherManager.release(searcher);
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
                private AtomicReader atomicReader;

                @Override public void setScorer(Scorer arg0) throws IOException {}
                @Override public void setNextReader(AtomicReaderContext context) throws IOException {
                    this.atomicReader = context.reader();
                }

                @Override
                public void collect(int docId) throws IOException {
                    results.add(loadAudioItem(atomicReader.document(docId)));
                }

                @Override public boolean acceptsDocsOutOfOrder() {
                    return true;
                }
            });

            return results;
        } finally {
            searcherManager.release(searcher);
        }
    }

    public void deleteAudioItem(final String uuid) throws IOException {
        IndexWriter writer = newWriter();
        try {
            writer.deleteDocuments(new Term(UID_FIELD, uuid));
        } finally {
            writer.close();
        }
    }

    private Document getDocument(final String uuid) throws IOException {
        final AtomicReference<Document> result = new AtomicReference<Document>();

        final IndexSearcher searcher = searcherManager.acquire();
        try {
            searcher.search(new TermQuery(new Term(UID_FIELD, uuid)), new Collector() {
                private AtomicReader atomicReader;

                @Override public void setScorer(Scorer arg0) throws IOException {}
                @Override public void setNextReader(AtomicReaderContext context) throws IOException {
                    this.atomicReader = context.reader();
                }

                @Override
                public void collect(int docId) throws IOException {
                    result.set(atomicReader.document(docId));
                }

                @Override public boolean acceptsDocsOutOfOrder() {
                    return true;
                }
            });
        } finally {
            searcherManager.release(searcher);
        }

        return result.get();
    }

    private AudioItem loadAudioItem(Document doc) throws IOException {
        AudioItem audioItem = new AudioItem(doc.get(UID_FIELD));

        LBMetadataSerializer deserializer = new LBMetadataSerializer();
        Set<Category> categories = new HashSet<Category>();
        BytesRef ref = doc.getBinaryValue(RAW_METADATA_FIELD);
        deserializer.deserialize(audioItem.getMetadata(), categories,
                new DataInputStream(new ByteArrayInputStream(ref.bytes, ref.offset, ref.length)));

        for (Category category : categories) {
            audioItem.addCategory(category);
        }

        return audioItem;
    }

    public IDataRequestResult search(String filterString, Playlist selectedTag) throws IOException {
        BooleanQuery q = new BooleanQuery();
        addTextQuery(q, filterString);
        if (selectedTag != null) {
            q.add(new TermQuery(new Term(TAGS_FIELD, selectedTag.getUuid())), Occur.MUST);
        }
        return search(q);
    }

    public IDataRequestResult search(String filterString, List<Category> filterCategories,
            List<Locale> locales) throws IOException {
        BooleanQuery q = new BooleanQuery();
        addTextQuery(q, filterString);

        if (filterCategories != null && !filterCategories.isEmpty()) {
            BooleanQuery categoriesQuery = new BooleanQuery();
            for (Category category : filterCategories) {
                categoriesQuery.add(new TermQuery(new Term(CATEGORIES_FIELD, category.getUuid())), Occur.SHOULD);
            }
            q.add(categoriesQuery, Occur.MUST);
        }

        if (locales != null && !locales.isEmpty()) {
            BooleanQuery localesQuery = new BooleanQuery();
            for (Locale locale : locales) {
                localesQuery.add(new TermQuery(new Term(LOCALES_FIELD, locale.getLanguage().toLowerCase())), Occur.SHOULD);
            }
            q.add(localesQuery, Occur.MUST);
        }

        return search(q);
    }

    private IDataRequestResult search(Query query) throws IOException {
        final FacetsCollector facetsCollector = new FacetsCollector();
        final Set<String> results = Sets.newHashSet();
        final IndexSearcher searcher = searcherManager.acquire();
        try {
            Collector collector = MultiCollector.wrap(facetsCollector, new Collector() {
                private AtomicReader atomicReader;

                @Override public void setScorer(Scorer arg0) throws IOException {}
                @Override public void setNextReader(AtomicReaderContext context) throws IOException {
                    this.atomicReader = context.reader();
                }

                @Override
                public void collect(int docId) throws IOException {
                    Document doc = atomicReader.document(docId);
                    results.add(doc.get(UID_FIELD));
                }

                @Override public boolean acceptsDocsOutOfOrder() {
                    return true;
                }
            });

            searcher.search(query, collector);

            SortedSetDocValuesFacetCounts facetCounts =
                    new SortedSetDocValuesFacetCounts(new DefaultSortedSetDocValuesReaderState(searcher.getIndexReader()), facetsCollector);
            List<FacetResult> facetResults = facetCounts.getAllDims(1000);
            Map<String, Integer> categoryFacets = Maps.newHashMap();
            Map<String, Integer> localeFacets = Maps.newHashMap();
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
            }

            DataRequestResult result = new DataRequestResult(categoryFacets, localeFacets, Lists.newArrayList(results),
                    getPlaylists());

            return result;
        } finally {
            searcherManager.release(searcher);
        }
    }

    public static class QueryAnalyzer extends Analyzer {
        @Override
        protected TokenStreamComponents createComponents(String field, Reader reader) {
            WhitespaceTokenizer tokenizer = new WhitespaceTokenizer(Version.LUCENE_47, reader);
            TokenFilter filter = new LowerCaseFilter(Version.LUCENE_47, tokenizer);
            return new TokenStreamComponents(tokenizer, filter);
        }
    }
}
