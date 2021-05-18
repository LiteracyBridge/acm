package org.literacybridge.acm.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.collect.Lists;

public class LuceneMetadataStoreTest {
  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  private final static Committable newFailingCommittable() {
    return new Committable() {
      @Override
      public boolean doCommit(Transaction t) throws IOException {
        throw new RuntimeException("Trigger rollback");
      }

      @Override
      public void doRollback(Transaction t) throws IOException {
      }
    };
  }

  private final static Committable newFailingRollback() {
    return new Committable() {
      @Override
      public boolean doCommit(Transaction t) throws IOException {
        return false;
      }

      @Override
      public void doRollback(Transaction t) throws IOException {
        throw new RuntimeException("Rollback failed");
      }
    };
  }

  @Test
  public void testAudioItems() throws Exception {
    LuceneMetadataStore store = newStore();

    AudioItem a1 = store.newAudioItem("1");
    AudioItem a2 = store.newAudioItem("2");
    AudioItem a3 = store.newAudioItem("3");

    a1.getMetadata().putMetadataField(MetadataSpecification.DC_TITLE,
        MetadataValue.newValue("Lorem"));
    a2.getMetadata().putMetadataField(MetadataSpecification.DC_TITLE,
        MetadataValue.newValue("Lorem ipsum"));
    a3.getMetadata().putMetadataField(MetadataSpecification.DC_TITLE,
        MetadataValue.newValue("Lorem ipsum dolor"));

    assertNumItems(store.getAudioItems(), 0);
    store.commit(a1, a2, a3);
    assertNumItems(store.getAudioItems(), 3);

    assertNumSearchResults(store, "lorem", 3);

    a2.getMetadata().putMetadataField(MetadataSpecification.DC_TITLE,
        MetadataValue.newValue("Lrem ipsum"));
    store.commit(a2);
    assertNumItems(store.getAudioItems(), 3);

    assertNumSearchResults(store, "lorem", 2);

    assertNotNull(store.getAudioItem("3"));
    store.deleteAudioItem("3");
    store.commit(a3);
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
  public void testPlaylists() throws Exception {
    LuceneMetadataStore store = newStore();

    AudioItem a1 = store.newAudioItem("1");
    AudioItem a2 = store.newAudioItem("2");
    AudioItem a3 = store.newAudioItem("3");

    a1.getMetadata().putMetadataField(MetadataSpecification.DC_TITLE,
        MetadataValue.newValue("Lorem"));
    a2.getMetadata().putMetadataField(MetadataSpecification.DC_TITLE,
        MetadataValue.newValue("Lorem ipsum"));
    a3.getMetadata().putMetadataField(MetadataSpecification.DC_TITLE,
        MetadataValue.newValue("Lorem ipsum dolor"));

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

    store.deletePlaylist(p2.getId());
    store.commit(p2);
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

    a1.getMetadata().putMetadataField(MetadataSpecification.DC_TITLE,
        MetadataValue.newValue("Lorem"));
    a2.getMetadata().putMetadataField(MetadataSpecification.DC_TITLE,
        MetadataValue.newValue("Lorem ipsum"));
    a3.getMetadata().putMetadataField(MetadataSpecification.DC_TITLE,
        MetadataValue.newValue("Lorem ipsum dolor"));

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

    Iterator<Category> it = store.getTaxonomy().getRootCategory().getChildren()
        .iterator();
    Category cat1 = it.next();
    Category cat1_1 = cat1.getChildren().iterator().next();
    Category cat2 = it.next();
    Category cat3 = it.next();
    Category cat3_1 = cat3.getChildren().iterator().next();

    a1.addCategory(cat3); // this will also add the first child leaf of cat 3
    a2.addCategory(cat1_1); // this should also automatically add the parent
                            // cat1 to this audioitem
    a1.addCategory(cat2);
    a2.addCategory(cat2);
    a3.addCategory(cat2);
    store.commit(a1, a2, a3);

    assertNumSearchResults(store, "lor", Lists.newArrayList(cat3_1), null, 1);
    assertNumSearchResults(store, "lor", Lists.newArrayList(cat2), null, 3);
    assertNumSearchResults(store, "lor", Lists.newArrayList(cat1), null, 1);
    assertNumSearchResults(store, "lor", Lists.newArrayList(cat1_1), null, 1);

    a1.getMetadata().putMetadataField(MetadataSpecification.DC_LANGUAGE,
        MetadataValue.newValue(new RFC3066LanguageCode("en-us")));
    a2.getMetadata().putMetadataField(MetadataSpecification.DC_LANGUAGE,
        MetadataValue.newValue(new RFC3066LanguageCode("en-us")));
    a3.getMetadata().putMetadataField(MetadataSpecification.DC_LANGUAGE,
        MetadataValue.newValue(new RFC3066LanguageCode("de-de")));
    store.commit(a1, a2, a3);

    assertNumSearchResults(store, "lor", Lists.newArrayList(cat2),
        Lists.newArrayList(Locale.ENGLISH, Locale.GERMAN), 3);
    assertNumSearchResults(store, "lor", Lists.newArrayList(cat2),
        Lists.newArrayList(Locale.ENGLISH), 2);
    assertNumSearchResults(store, "lor", Lists.newArrayList(cat2),
        Lists.newArrayList(Locale.GERMAN), 1);
    assertNumSearchResults(store, "lor do", Lists.newArrayList(cat2),
        Lists.newArrayList(Locale.GERMAN), 1);
    assertNumSearchResults(store, "lor do", Lists.newArrayList(cat2),
        Lists.newArrayList(Locale.ENGLISH), 0);
  }

  @Test
  public void testTransactions() throws Exception {
    LuceneMetadataStore store = newStore();

    AudioItem a1 = store.newAudioItem("1");
    AudioItem a2 = store.newAudioItem("2");
    AudioItem a3 = store.newAudioItem("3");

    a1.getMetadata().putMetadataField(MetadataSpecification.DC_TITLE,
        MetadataValue.newValue("Lorem"));
    a2.getMetadata().putMetadataField(MetadataSpecification.DC_TITLE,
        MetadataValue.newValue("Lorem ipsum"));
    a3.getMetadata().putMetadataField(MetadataSpecification.DC_TITLE,
        MetadataValue.newValue("Lorem ipsum dolor"));

    store.commit(a1, a2, a3);
    assertNumSearchResults(store, "lor", 3);

    // test rollback
    a1.getMetadata().putMetadataField(MetadataSpecification.DC_TITLE,
        MetadataValue.newValue("Lorem 123"));
    a2.getMetadata().putMetadataField(MetadataSpecification.DC_TITLE,
        MetadataValue.newValue("Lxyzorem ipsum"));
    Exception expectedException = null;
    try {
      store.commit(a1, a2, newFailingCommittable());
    } catch (Exception e) {
      expectedException = e;
    }
    assertNotNull(expectedException);

    // the changes should not have made it to the index
    assertNumSearchResults(store, "lor", 3);
    assertNumSearchResults(store, "123", 0);

    store.commit(a1, a2);
    // this commit should have been a noop, assuming the previous rollback
    // correctly reset the state of a1 and a2
    assertNumSearchResults(store, "lor", 3);
    assertNumSearchResults(store, "123", 0);

    // try making the same changes again, this time without an exception
    a1.getMetadata().putMetadataField(MetadataSpecification.DC_TITLE,
        MetadataValue.newValue("Lorem 123"));
    a2.getMetadata().putMetadataField(MetadataSpecification.DC_TITLE,
        MetadataValue.newValue("Lxyzorem ipsum"));

    store.commit(a1, a2);
    assertNumSearchResults(store, "lor", 2);
    assertNumSearchResults(store, "123", 1);

    // try rollback of a delete attempt
    store.deleteAudioItem(a1.getId());
    // before the commit a2 should still be returned
    assertEquals(a1, store.getAudioItem(a1.getId()));

    a2.getMetadata().putMetadataField(MetadataSpecification.DC_TITLE,
        MetadataValue.newValue("Lorem ipsum"));

    try {
      store.commit(a1, a2, newFailingCommittable());
    } catch (Exception e) {
      expectedException = e;
    }
    assertNotNull(expectedException);

    // a1 should not be deleted, and the title of a2 should not have been
    // altered
    assertNumSearchResults(store, "lor", 2);
    assertNumSearchResults(store, "123", 1);
    assertEquals(a1, store.getAudioItem(a1.getId()));

    // this is a no-op, but we want to check that a1 is still committable after
    // a successful rollback
    store.commit(a1);

    // Now try this scenario again, but also with a Committable that causes the
    // rollback to fail

    // try rollback of a delete attempt
    store.deleteAudioItem(a1.getId());
    // before the commit a2 should still be returned
    assertEquals(a1, store.getAudioItem(a1.getId()));

    a2.getMetadata().putMetadataField(MetadataSpecification.DC_TITLE,
        MetadataValue.newValue("Lorem ipsum"));

    expectedException = null;
    try {
      store.commit(a1, a2, newFailingCommittable(), newFailingRollback());
    } catch (Exception e) {
      expectedException = e;
    }
    assertNotNull(expectedException);

    // a1 should not be deleted, and the title of a2 should not have been
    // altered
    assertNumSearchResults(store, "lor", 2);
    assertNumSearchResults(store, "123", 1);
    assertEquals(a1, store.getAudioItem(a1.getId()));

    expectedException = null;
    try {
      // now a1 should not be committable anymore - we expect an
      // IllegalStateException
      store.commit(a1);
    } catch (IllegalStateException e) {
      expectedException = e;
    }
    assertNotNull(expectedException);

    // try one more scenario with a combination of AudioItems, Playlists and a
    // failing Committable
    store = newStore();

    a1 = store.newAudioItem("1");
    a2 = store.newAudioItem("2");
    a3 = store.newAudioItem("3");

    a1.getMetadata().putMetadataField(MetadataSpecification.DC_TITLE,
        MetadataValue.newValue("Lorem"));
    a2.getMetadata().putMetadataField(MetadataSpecification.DC_TITLE,
        MetadataValue.newValue("Lorem ipsum"));
    a3.getMetadata().putMetadataField(MetadataSpecification.DC_TITLE,
        MetadataValue.newValue("Lorem ipsum dolor"));

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

    store.deletePlaylist(p2.getId());
    a2.getMetadata().putMetadataField(MetadataSpecification.DC_TITLE,
        MetadataValue.newValue("Lxyzorem ipsum"));
    expectedException = null;
    try {
      store.commit(a1, a2, newFailingCommittable());
    } catch (Exception e) {
      expectedException = e;
    }
    assertNotNull(expectedException);

    assertNumSearchResults(store, "lorem", p1, 3);
    assertNumSearchResults(store, "lorem", p2, 2);
    assertNumSearchResults(store, "lorem", p3, 1);
  }

  @Test
  public void testNoNestedTransactions() throws Exception {
    LuceneMetadataStore store = newStore();
    Transaction t1 = store.newTransaction();

    // this call should yield null, because t1 was not committed/rolled back yet
    Transaction t2 = store.newTransaction();

    assertNotNull(t1);
    assertNull(t2);

    t1.commit();
    assertFalse(t1.isActive());
    t2 = store.newTransaction();
    assertNotNull(t2);
    t2.rollback();
    assertFalse(t2.isActive());
    assertNotNull(store.newTransaction());
  }

  @Test
  public void testNoNestedTransactionsMultiThreaded() throws Exception {
    final LuceneMetadataStore store = newStore();
    final Transaction t1 = store.newTransaction();
    t1.add(new Committable() {
      @Override
      public void doRollback(Transaction t) throws IOException {
      }

      @Override
      public boolean doCommit(Transaction t) throws IOException {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        return false;
      }
    });

    Thread thread1 = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          t1.commit();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    });

    final AtomicReference<Transaction> active = new AtomicReference<Transaction>(
        t1);

    Thread thread2 = new Thread(new Runnable() {
      @Override
      public void run() {
        System.out.println("Creating new transaction");
        active.set(store.newTransaction());
      }
    });

    thread1.start();
    thread2.start();
    thread1.join();
    thread2.join();

    assertNull(active.get());
  }

  private LuceneMetadataStore newStore() throws Exception {
    Taxonomy taxonomy = Taxonomy.createTaxonomy(null,null);
    return new LuceneMetadataStore(taxonomy, tmp.newFolder());
  }

  private static void assertNumSearchResults(MetadataStore store, String query,
      int expectedNumResults) throws Exception {
    assertNumSearchResults(store, query, null, expectedNumResults);
  }

  private static void assertNumSearchResults(MetadataStore store, String query,
      Playlist playlist, int expectedNumResults) throws Exception {
    SearchResult result = store.search(query, playlist);
    assertEquals(expectedNumResults, result.getAudioItems().size());
  }

  private static void assertNumSearchResults(MetadataStore store, String query,
      List<Category> categories, List<Locale> locales, int expectedNumResults)
      throws Exception {
    SearchResult result = store.search(query, categories, locales);
    assertEquals(expectedNumResults, result.getAudioItems().size());
  }

  private static void assertNumItems(Iterable<?> list, int expectedNumItems) {
    int actual = 0;
    for (Iterator<?> it = list.iterator(); it.hasNext()
        && it.next() != null; actual++)
      ;
    assertEquals(expectedNumItems, actual);
  }
}
