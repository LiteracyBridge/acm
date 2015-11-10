package org.literacybridge.acm.index;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
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
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Collector;
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
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.acm.store.Taxonomy;

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

    AudioItemDocumentFactory factory = new AudioItemDocumentFactory();
    private IndexWriter writer;
    private SearcherManager searchmanager;

    private final FacetsConfig facetsConfig;
    private final QueryAnalyzer queryAnalyzer;

    private AudioItemIndex(IndexWriter writer) throws IOException {
        this.writer = writer;
        facetsConfig = new FacetsConfig();
        facetsConfig.setMultiValued(AudioItemIndex.CATEGORIES_FACET_FIELD, true);
        facetsConfig.setMultiValued(AudioItemIndex.LOCALES_FACET_FIELD, true);

        queryAnalyzer = new QueryAnalyzer();

        searchmanager = new SearcherManager(writer, true, new SearcherFactory());
    }

    public static AudioItemIndex loadOrBuild(File path) throws IOException {
        if (!path.exists()) {
            path.mkdirs();
        }

        if (!path.isDirectory()) {
            throw new IOException("Path must be a directory.");
        }

        Directory dir = FSDirectory.open(path);
        boolean indexFromDB = !DirectoryReader.indexExists(dir);

        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_47, new AudioItemDocumentFactory.PrefixAnalyzer());
        config.setOpenMode(OpenMode.CREATE_OR_APPEND);
        IndexWriter writer = new IndexWriter(dir, config);

        AudioItemIndex index = new AudioItemIndex(writer);

        if (indexFromDB) {
            Iterable<AudioItem> items = ACMConfiguration.getCurrentDB().getMetadataStore().getAudioItems();

            for (AudioItem item : items) {
                index.addAudioItem(item);
            }

            index.writer.forceMerge(1);
            index.writer.commit();
        }
        index.searchmanager.maybeRefreshBlocking();
        return index;
    }

    public void closeAndFlush() throws IOException {
        writer.forceMerge(1);
        writer.close();
    }

    public void updateAudioItem(AudioItem audioItem) throws IOException {
        Document doc = factory.createLuceneDocument(audioItem);
        writer.updateDocument(new Term(UID_FIELD, audioItem.getUuid()),
                facetsConfig.build(doc));
        searchmanager.maybeRefreshBlocking();
    }

    private void addAudioItem(AudioItem audioItem) throws IOException {
        Document doc = factory.createLuceneDocument(audioItem);
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

    public IDataRequestResult search(String filterString, Playlist selectedTag) throws IOException {
        BooleanQuery q = new BooleanQuery();
        addTextQuery(q, filterString);
        if (selectedTag != null) {
            q.add(new TermQuery(new Term(TAGS_FIELD, selectedTag.getName())), Occur.MUST);
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
        final IndexSearcher searcher = searchmanager.acquire();

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

            MetadataStore store = ACMConfiguration.getCurrentDB().getMetadataStore();
            SortedSetDocValuesFacetCounts facetCounts =
                    new SortedSetDocValuesFacetCounts(new DefaultSortedSetDocValuesReaderState(searcher.getIndexReader()), facetsCollector);
            List<FacetResult> facetResults = facetCounts.getAllDims(1000);
            Map<String, Integer> categoryFacets = Maps.newHashMap();
            Map<String, Integer> localeFacets = Maps.newHashMap();
            for (FacetResult r : facetResults) {
                if (r.dim.equals(CATEGORIES_FACET_FIELD)) {
                    for (LabelAndValue lv : r.labelValues) {
                        categoryFacets.put(store.getCategory(lv.label).getUuid(), lv.value.intValue());
                    }
                }
                if (r.dim.equals(LOCALES_FACET_FIELD)) {
                    for (LabelAndValue lv : r.labelValues) {
                        localeFacets.put(lv.label, lv.value.intValue());
                    }

                }
            }

            DataRequestResult result = new DataRequestResult(store.getTaxonomy().getRootCategory(), categoryFacets, localeFacets, Lists.newArrayList(results),
                    store.getPlaylists());

            return result;
        } finally {
            searchmanager.release(searcher);
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
