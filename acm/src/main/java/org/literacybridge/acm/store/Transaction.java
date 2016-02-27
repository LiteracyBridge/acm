package org.literacybridge.acm.store;

import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.index.IndexWriter;
import org.literacybridge.acm.index.AudioItemIndex;

import com.google.common.collect.Sets;

public class Transaction {
    private static final Logger LOG = Logger.getLogger(Transaction.class.getName());

    public Set<Persistable> objects;
    public AudioItemIndex index;
    public IndexWriter writer;

    public Transaction(AudioItemIndex index, IndexWriter writer) throws IOException {
        this.index = index;
        this.writer = writer;
        this.objects = Sets.newLinkedHashSet();
    }

    public final void commit() {
        boolean success = false;
        try {
            for (Persistable o : objects) {
                o.commit(this);
            }
            success = true;
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "IOException while commiting a transaction.", e);
        } finally {
            if (success) {
                boolean success2 = false;
                try {
                    writer.close();
                    success2 = true;
                } catch (IOException e) {
                    LOG.log(Level.SEVERE, "IOException while commiting a transaction.", e);
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

    public final void rollback() {
        try {
            writer.rollback();
            for (Persistable o : objects) {
                try {
                    o.rollback(this);
                } catch (IOException e) {
                    LOG.log(Level.SEVERE, "IOException while rolling back PersistableObject: " + o, e);
                }
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "IOException while rolling back a transaction.", e);
        }
    }

    public void add(Persistable object) {
        objects.add(object);
    }

    public AudioItemIndex getIndex() {
        return index;
    }

    public IndexWriter getWriter() {
        return writer;
    }
}