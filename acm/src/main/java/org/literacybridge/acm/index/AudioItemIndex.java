package org.literacybridge.acm.index;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
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
	public static final String TAGS_FIELD = "tags";
	public static final String LOCALES_FIELD = "locales";

	private Directory dir = new RAMDirectory();
	AudioItemDocumentFactory factory = new AudioItemDocumentFactory();
	private IndexWriter writer;
	private SearcherManager searchmanager;

	public AudioItemIndex() throws IOException {
		List<AudioItem> items = AudioItem.getFromDatabase();
		IndexWriterConfig config = new IndexWriterConfig(Version.LATEST, new AudioItemDocumentFactory.PrefixAnalyzer());
		writer = new IndexWriter(dir, config);
		searchmanager = new SearcherManager(writer, true, new SearcherFactory());

		for (AudioItem item : items) {
			updateAudioItem(item);
		}

		writer.commit();
	}

	public void updateAudioItem(AudioItem audioItem) throws IOException {
		writer.updateDocument(new Term(UID_FIELD, audioItem.getUuid()),
				factory.createLuceneDocument(audioItem));
		searchmanager.maybeRefresh();
	}

	public List<AudioItem> search(String filterString, PersistentTag selectedTag) throws IOException {
		BooleanQuery q = new BooleanQuery();
		q.add(new TermQuery(new Term(TEXT_FIELD, filterString)), Occur.MUST);
		if (selectedTag != null) {
			q.add(new TermQuery(new Term(TAGS_FIELD, selectedTag.getName())), Occur.MUST);
		}
		return search(q);
	}

	public List<AudioItem> search(String filterString, List<PersistentCategory> filterCategories,
			List<PersistentLocale> locales) throws IOException {
		BooleanQuery q = new BooleanQuery();
		q.add(new TermQuery(new Term(TEXT_FIELD, filterString)), Occur.MUST);

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

		return search(q);
	}

	private List<AudioItem> search(Query query) throws IOException {
		List<AudioItem> results = Lists.newArrayList();
		IndexSearcher searcher = searchmanager.acquire();
		TopDocs topDocs = searcher.search(query, 100000);
		for (ScoreDoc hit : topDocs.scoreDocs) {
			Document doc = searcher.doc(hit.doc);
			String uuid = doc.get(UID_FIELD);

			results.add(AudioItem.getFromDatabase(uuid));
		}
		return results;
	}

}
