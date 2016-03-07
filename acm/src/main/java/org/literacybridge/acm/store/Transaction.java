package org.literacybridge.acm.store;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.lucene.index.IndexWriter;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class Transaction {
    private static final Logger LOG = Logger.getLogger(Transaction.class.getName());

    private final Set<Committable> objects;
    private final AudioItemIndex index;
    private final IndexWriter writer;
    private final MetadataStore store;

    public Transaction(MetadataStore store, AudioItemIndex index, IndexWriter writer) throws IOException {
        this.store = store;
        this.index = index;
        this.writer = writer;
        this.objects = Sets.newLinkedHashSet();
    }

    private final void prepareCommit(Transaction t, MetadataStore store, Iterable<Committable> objects) {
        List<Committable> additionalObjects = Lists.newArrayList();
        for (Committable o : objects) {
            o.prepareCommit(store, additionalObjects);
        }

        if (!additionalObjects.isEmpty()) {
            t.addAll(additionalObjects);
            prepareCommit(t, store, additionalObjects);
        }
    }

    public final void commit() throws IOException {
        boolean success = false;
        try {
            prepareCommit(this, store, objects);

            for (Committable o : objects) {
                o.commit(this);
            }
            success = true;
        } finally {
            if (success) {
                boolean success2 = false;
                try {
                    writer.close();
                    success2 = true;
                } finally {
                    if (!success2) {
                        rollback();
                    }
                }
            } else {
                rollback();
            }
        }
    }

    public final void rollback() throws IOException {
        try {
            writer.rollback();
        } finally {
            for (Committable o : objects) {
                o.rollback(this);
            }
        }
    }

    public void add(Committable committable) {
        objects.add(committable);
    }

    public void addAll(List<Committable> committables) {
        for (Committable c : committables) {
            add(c);
        }
    }

    public void addAll(Committable... committables) {
        for (Committable c : committables) {
            add(c);
        }
    }

    public AudioItemIndex getIndex() {
        return index;
    }

    public IndexWriter getWriter() {
        return writer;
    }
}