package org.literacybridge.acm.store;

import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.index.IndexWriter;

import com.google.common.collect.Sets;

public class Transaction {
    private static final Logger LOG = Logger.getLogger(Transaction.class.getName());

    public Set<Committable> objects;
    public AudioItemIndex index;
    public IndexWriter writer;

    public Transaction(AudioItemIndex index, IndexWriter writer) throws IOException {
        this.index = index;
        this.writer = writer;
        this.objects = Sets.newLinkedHashSet();
    }

    public final void commit() throws IOException {
        boolean success = false;
        try {
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