package org.literacybridge.acm.store;

import java.io.IOException;

public abstract class Persistable {
    // if true, this object is in an undefined state due to a failed rollback attempt, and must be discarded.
    private boolean rollbackFailed;

    public Persistable() {
        this.rollbackFailed = false;
    }

    /**
     * If true, this object is in an undefined state due to a failed rollback attempt, and must be discarded.
     */
    public final boolean rollbackFailed() {
        return rollbackFailed;
    }

    /**
     * Commits this object to the index.
     */
    public abstract void commit(Transaction t) throws IOException;

    /**
     * Resets the internal state of this object to the version stored in the index.
     */
    public abstract void rollback(Transaction t) throws IOException;
}
