package org.literacybridge.acm.store;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Committable {
    private static final Logger LOG = Logger.getLogger(Committable.class.getName());

    // if true, this object is in an undefined state due to a failed rollback attempt, and must be discarded.
    private boolean rollbackFailed;

    public Committable() {
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
    public final void commit(Transaction t) {
        boolean success = false;
        try {
            doCommit(t);
            success = true;
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "IOException while committing Committable: " + this.toString(), e);
        } finally {
            if (!success) {
                // if rollback() is properly called later and succeeds, this will be set back to false
                rollbackFailed = true;
            }
        }
    }

    public abstract void doCommit(Transaction t) throws IOException;

    /**
     * Resets the internal state of this object to the version stored in the index.
     */
    public final void rollback(Transaction t) {
        boolean success = false;
        try {
            doRollback(t);
            success = true;
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "IOException while rolling back Committable: " + this.toString(), e);
        } finally {
            rollbackFailed = !success;
        }
    }

    public abstract void doRollback(Transaction t) throws IOException;
}
