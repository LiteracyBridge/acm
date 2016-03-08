package org.literacybridge.acm.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.literacybridge.acm.api.IDataRequestResult;

import com.google.common.collect.Lists;

public class LuceneMetadataStoreTest {
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void testAudioItems() throws Exception {
        LuceneMetadataStore store = newStore();

        AudioItem a1 = store.newAudioItem("1");
        AudioItem a2 = store.newAudioItem("2");
        AudioItem a3 = store.newAudioItem("3");

        a1.getMetadata().setMetadataField(MetadataSpecification.DC_TITLE, MetadataValue.newValue("Lorem"));
        a2.getMetadata().setMetadataField(MetadataSpecification.DC_TITLE, MetadataValue.newValue("Lorem ipsum"));
        a3.getMetadata().setMetadataField(MetadataSpecification.DC_TITLE, MetadataValue.newValue("Lorem ipsum dolor"));

        assertNumItems(store.getAudioItems(), 0);
        store.commit(a1, a2, a3);
        assertNumItems(store.getAudioItems(), 3);

        assertNumSearchResults(store, "lorem", 3);

        a2.getMetadata().setMetadataField(MetadataSpecification.DC_TITLE, MetadataValue.newValue("Lrem ipsum"));
        store.commit(a2);
        assertNumItems(store.getAudioItems(), 3);

        assertNumSearchResults(store, "lorem", 2);

        assertNotNull(store.getAudioItem("3"));
        store.deleteAudioItem("3");
        assertNumSearchResults(store, "lorem", 1);
        assertNull(store.getAudioItem("3"));
        assertNumItems(store.getAudioItems(), 2);

        Exception expectedException = null;
        try {
            store.commit(a3);
        } catch (IllegalStateException e) {
            expectedException = e;
        }
        assertNotNull(expectedException);

        assertNumSearchResults(store, "lorem", 1);
        assertNull(store.getAudioItem("3"));
        assertNumItems(store.getAudioItems(), 2);
    }

    @Test
    public void testAudioPlaylists() throws Exception {
        LuceneMetadataStore store = newStore();

        AudioItem a1 = store.newAudioItem("1");
        AudioItem a2 = store.newAudioItem("2");
        AudioItem a3 = store.newAudioItem("3");

        a1.getMetadata().setMetadataField(MetadataSpecification.DC_TITLE, MetadataValue.newValue("Lorem"));
        a2.getMetadata().setMetadataField(MetadataSpecification.DC_TITLE, MetadataValue.newValue("Lorem ipsum"));
        a3.getMetadata().setMetadataField(MetadataSpecification.DC_TITLE, MetadataValue.newValue("Lorem ipsum dolor"));

        store.commit(a1, a2, a3);

        Playlist p1 = store.newPlaylist("1");
        Playlist p2 = store.newPlaylist("2");
        Playlist p3 = store.newPlaylist("3");

        p1.addAudioItem(a1);
        p1.addAudioItem(a2);
        p1.addAudioItem(a3);

        p2.addAudioItem(a1);
        p2.addAudioItem(a2);

        p3.addAudioItem(a1);

        store.commit(p1, p2, p3);

        assertNumSearchResults(store, "lorem", p1, 3);
        assertNumSearchResults(store, "lorem", p2, 2);
        assertNumSearchResults(store, "lorem", p3, 1);

        store.deletePlaylist(p2.getUuid());
        Exception expectedException = null;
        try {
            store.commit(p1, p2, p3);
        } catch (IllegalStateException e) {
            expectedException = e;
        }
        assertNotNull(expectedException);
        assertNumSearchResults(store, "lorem", p2, 0);
    }

    @Test
    public void testSearch() throws Exception {
        LuceneMetadataStore store = newStore();

        AudioItem a1 = store.newAudioItem("1");
        AudioItem a2 = store.newAudioItem("2");
        AudioItem a3 = store.newAudioItem("3");

        a1.getMetadata().setMetadataField(MetadataSpecification.DC_TITLE, MetadataValue.newValue("Lorem"));
        a2.getMetadata().setMetadataField(MetadataSpecification.DC_TITLE, MetadataValue.newValue("Lorem ipsum"));
        a3.getMetadata().setMetadataField(MetadataSpecification.DC_TITLE, MetadataValue.newValue("Lorem ipsum dolor"));

        assertNumSearchResults(store, "lor", 0);
        store.commit(a1, a2, a3);

        assertNumSearchResults(store, "lor", 3);
        assertNumSearchResults(store, "lor ip ", 2);
        assertNumSearchResults(store, "lor ip DO", 1);

        Playlist p1 = store.newPlaylist("1");
        Playlist p2 = store.newPlaylist("2");
        Playlist p3 = store.newPlaylist("3");

        p1.addAudioItem(a1);
        p1.addAudioItem(a2);
        p1.addAudioItem(a3);

        p2.addAudioItem(a1);
        p2.addAudioItem(a2);

        p3.addAudioItem(a1);

        store.commit(p1, p2, p3);

        assertNumSearchResults(store, "lor ip do", p1, 1);
        assertNumSearchResults(store, "lor ip do", p2, 0);
        assertNumSearchResults(store, "lor ip do", p3, 0);
        assertNumSearchResults(store, "lor", p2, 2);
        assertNumSearchResults(store, "lor", p1, 3);

        Iterator<Category> it = store.getTaxonomy().getRootCategory().getChildren().iterator();
        Category cat1 = it.next();
        Category cat1_1 = cat1.getChildren().iterator().next();
        Category cat2 = it.next();
        Category cat3 = it.next();
        Category cat3_1 = cat3.getChildren().iterator().next();

        a1.addCategory(cat3);   // this will also add the first child leaf of cat 3
        a2.addCategory(cat1_1); // this should also automatically add the parent cat1 to this audioitem
        a1.addCategory(cat2);
        a2.addCategory(cat2);
        a3.addCategory(cat2);
        store.commit(a1, a2, a3);

        assertNumSearchResults(store, "lor", Lists.newArrayList(cat3_1), null, 1);
        assertNumSearchResults(store, "lor", Lists.newArrayList(cat2), null, 3);
        assertNumSearchResults(store, "lor", Lists.newArrayList(cat1), null, 1);
        assertNumSearchResults(store, "lor", Lists.newArrayList(cat1_1), null, 1);

        a1.getMetadata().setMetadataField(MetadataSpecification.DC_LANGUAGE, MetadataValue.newValue(new RFC3066LanguageCode("en-us")));
        a2.getMetadata().setMetadataField(MetadataSpecification.DC_LANGUAGE, MetadataValue.newValue(new RFC3066LanguageCode("en-us")));
        a3.getMetadata().setMetadataField(MetadataSpecification.DC_LANGUAGE, MetadataValue.newValue(new RFC3066LanguageCode("de-de")));
        store.commit(a1, a2, a3);

        assertNumSearchResults(store, "lor", Lists.newArrayList(cat2), Lists.newArrayList(Locale.ENGLISH, Locale.GERMAN), 3);
        assertNumSearchResults(store, "lor", Lists.newArrayList(cat2), Lists.newArrayList(Locale.ENGLISH), 2);
        assertNumSearchResults(store, "lor", Lists.newArrayList(cat2), Lists.newArrayList(Locale.GERMAN), 1);
        assertNumSearchResults(store, "lor do", Lists.newArrayList(cat2), Lists.newArrayList(Locale.GERMAN), 1);
        assertNumSearchResults(store, "lor do", Lists.newArrayList(cat2), Lists.newArrayList(Locale.ENGLISH), 0);
    }

    private LuceneMetadataStore newStore() throws Exception {
        AudioItemIndex index = AudioItemIndex.newIndex(tmp.newFolder());
        Taxonomy taxonomy = Taxonomy.createTaxonomy(null);
        return new LuceneMetadataStore(taxonomy, index);
    }

    private static void assertNumSearchResults(MetadataStore store, String query, int expectedNumResults) throws Exception {
        assertNumSearchResults(store, query, null, expectedNumResults);
    }

    private static void assertNumSearchResults(MetadataStore store, String query,
            Playlist playlist, int expectedNumResults) throws Exception {
        IDataRequestResult result = store.search(query, playlist);
        assertEquals(expectedNumResults, result.getAudioItems().size());
    }

    private static void assertNumSearchResults(MetadataStore store, String query,
            List<Category> categories, List<Locale> locales, int expectedNumResults) throws Exception {
        IDataRequestResult result = store.search(query, categories, locales);
        assertEquals(expectedNumResults, result.getAudioItems().size());
    }

    private static void assertNumItems(Iterable<?> list, int expectedNumItems) {
        int actual = 0;
        for (Iterator<?> it = list.iterator(); it.hasNext() && it.next() != null; actual++);
        assertEquals(expectedNumItems, actual);
    }
}
