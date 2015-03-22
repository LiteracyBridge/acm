package org.literacybridge.acm.index;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.tokenattributes.TermToBytesRefAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.sortedset.DefaultSortedSetDocValuesReaderState;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetCounts;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
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
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.db.PersistentCategory;
import org.literacybridge.acm.db.PersistentLocale;
import org.literacybridge.acm.db.PersistentTag;

import com.google.common.collect.Lists;

public class AudioItemIndex {
	public static final String TEXT_FIELD = "text";
	public static final String UID_FIELD = "uid";
	public static final String CATEGORIES_FIELD = "categories";
	public static final String CATEGORIES_FACET_FIELD = "categories_facet";
	public static final String TAGS_FIELD = "tags";
	public static final String LOCALES_FIELD = "locales";
	public static final String LOCALES_FACET_FIELD = "locales_facet";

	private Directory dir = new RAMDirectory();
	AudioItemDocumentFactory factory = new AudioItemDocumentFactory();
	private IndexWriter writer;
	private SearcherManager searchmanager;

	private final FacetsConfig facetsConfig;
	private final QueryAnalyzer queryAnalyzer;

	public AudioItemIndex() throws IOException {
		facetsConfig = new FacetsConfig();
		facetsConfig.setMultiValued(AudioItemIndex.CATEGORIES_FACET_FIELD, true);
		facetsConfig.setMultiValued(AudioItemIndex.LOCALES_FACET_FIELD, true);

		queryAnalyzer = new QueryAnalyzer();

		List<AudioItem> items = AudioItem.getFromDatabase();
		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_47, new AudioItemDocumentFactory.PrefixAnalyzer());
		writer = new IndexWriter(dir, config);
		searchmanager = new SearcherManager(writer, true, new SearcherFactory());

		for (AudioItem item : items) {
			updateAudioItem(item);
		}

		writer.commit();
	}

	public void updateAudioItem(AudioItem audioItem) throws IOException {
		Document doc = factory.createLuceneDocument(audioItem);
		writer.updateDocument(new Term(UID_FIELD, audioItem.getUuid()),
				facetsConfig.build(doc));
		searchmanager.maybeRefresh();
	}

	public List<String> search(String filterString, PersistentTag selectedTag) throws IOException {
		BooleanQuery q = new BooleanQuery();
		q.add(new TermQuery(new Term(TEXT_FIELD, filterString)), Occur.MUST);
		if (selectedTag != null) {
			q.add(new TermQuery(new Term(TAGS_FIELD, selectedTag.getName())), Occur.MUST);
		}
		return search(q);
	}

	public List<String> search(String filterString, List<PersistentCategory> filterCategories,
			List<PersistentLocale> locales) throws IOException {
		BooleanQuery q = new BooleanQuery();
		if (filterString == null || filterString.isEmpty()) {
			q.add(new MatchAllDocsQuery(), Occur.MUST);
		} else {
			TokenStream ts = queryAnalyzer.tokenStream(TEXT_FIELD, filterString);
			TermToBytesRefAttribute termAtt = ts.addAttribute(TermToBytesRefAttribute.class);
			ts.reset();

			while (ts.incrementToken()) {
				termAtt.fillBytesRef();
				q.add(new TermQuery(new Term(TEXT_FIELD, BytesRef.deepCopyOf(termAtt.getBytesRef()))), Occur.MUST);
			}
			ts.close();
		}

		if (filterCategories != null && !filterCategories.isEmpty()) {
			BooleanQuery categoriesQuery = new BooleanQuery();
			for (PersistentCategory category : filterCategories) {
				categoriesQuery.add(new TermQuery(new Term(CATEGORIES_FIELD, category.getUuid())), Occur.SHOULD);
			}
			q.add(categoriesQuery, Occur.MUST);
		}

		if (locales != null && !locales.isEmpty()) {
//		BooleanQuery localesQuery = new BooleanQuery();
//		for (PersistentLocale locale : locales) {
//			localesQuery.add(new TermQuery(new Term(LOCALES_FIELD, locale.getLanguage())), Occur.SHOULD);
//		}
//		q.add(localesQuery, Occur.MUST);
		}

		System.out.println(q);
		return search(q);
	}

	private List<String> search(Query query) throws IOException {
		final FacetsCollector facetsCollector = new FacetsCollector();
		final List<String> results = Lists.newArrayList();
		final IndexSearcher searcher = searchmanager.acquire();

		Collector collector = MultiCollector.wrap(facetsCollector, new Collector() {
			@Override public void setScorer(Scorer arg0) throws IOException {}
			@Override public void setNextReader(AtomicReaderContext arg0) throws IOException {}

			@Override
			public void collect(int docId) throws IOException {
				Document doc = searcher.doc(docId);
				results.add(doc.get(UID_FIELD));
			}

			@Override public boolean acceptsDocsOutOfOrder() {
				return true;
			}
		});

		searcher.search(query, collector);
		SortedSetDocValuesFacetCounts facetCounts =
				new SortedSetDocValuesFacetCounts(new DefaultSortedSetDocValuesReaderState(searcher.getIndexReader()), facetsCollector);
		System.out.println(facetCounts.getAllDims(100));

		return results;
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
