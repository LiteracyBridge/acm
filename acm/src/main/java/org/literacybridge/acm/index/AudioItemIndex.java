package org.literacybridge.acm.index;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.literacybridge.acm.content.AudioItem;

public class AudioItemIndex {
	public static final String TEXT_FIELD = "text";
	public static final String UID_FIELD = "uid";
	public static final String CATEGORIES_FIELD = "categories";
	public static final String TAGS_FIELD = "tags";
	public static final String LOCALES_FIELD = "locales";

	private Directory dir = new RAMDirectory();
	AudioItemDocumentFactory factory = new AudioItemDocumentFactory();
	private IndexWriter writer;

	public AudioItemIndex() throws IOException {
		List<AudioItem> items = AudioItem.getFromDatabase();
		IndexWriterConfig config = new IndexWriterConfig(Version.LATEST, new AudioItemDocumentFactory.PrefixAnalyzer());
		writer = new IndexWriter(dir, config);

		for (AudioItem item : items) {
			updateAudioItem(item);
		}
		writer.close();

		IndexReader reader = DirectoryReader.open(dir);
		IndexSearcher searcher = new IndexSearcher(reader);
		System.out.println(searcher.search(new TermQuery(new Term(TEXT_FIELD, "a")), 100).totalHits);

	}

	public void updateAudioItem(AudioItem audioItem) throws IOException {
		writer.updateDocument(new Term(UID_FIELD, audioItem.getUuid()),
				factory.createLuceneDocument(audioItem));
	}

}
