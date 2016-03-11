package org.literacybridge.acm.store;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Committable {
    private static enum CommitState {
        VALID,                     // object is in a healthy state and in sync with the store
        COMMIT_FAILED,             // a commit failed
        ROLLBACK_FAILED            // a rollback attempt failed - this object is now in an invalid state
    }

    private static enum DeleteState {
        NOT_DELETED,               // object was not deleted
        DELETED,                   // object was deleted
        DELETE_REQUESTED           // a delete of this object was requested, but not committed yet
    }

    private static final Logger LOG = Logger.getLogger(Committable.class.getName());

    private CommitState commitState;
    private DeleteState deleteState;

    private CommitListener listener;

    public Committable() {
        this.commitState = CommitState.VALID;
        this.deleteState = DeleteState.NOT_DELETED;
    }

    public void setCommitListener(CommitListener listener) {
        this.listener = listener;
    }

    public final void ensureIsCommittable() {
        if (deleteState == DeleteState.DELETED) {
            throw new IllegalStateException("This object was deleted and must not be used anymore.");
        }

        if (commitState == CommitState.ROLLBACK_FAILED) {
            throw new IllegalStateException("This object is in an undefined state due to a failed rollback attempt and must be discarded");
        }
    }

    public final void delete() {
        if (deleteState != DeleteState.DELETED) {
            deleteState = DeleteState.DELETE_REQUESTED;
        }
    }

    protected final boolean isDeleteRequested() {
        return deleteState == DeleteState.DELETE_REQUESTED;
    }

    final void setRollbackFailed() {
        this.commitState = CommitState.ROLLBACK_FAILED;
    }

    /**
     * Commits this object to the index.
     */
    public void prepareCommit(MetadataStore store, List<Committable> additionalObjects) {
    }

    /**
     * Commits this object to the index.
     */
    public final void commit(Transaction t) {
        ensureIsCommittable();

        boolean success = false;
        try {
            doCommit(t);
            success = true;
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "IOException while committing Committable: " + this.toString(), e);
        } finally {
            if (!success) {
                commitState = CommitState.COMMIT_FAILED;
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
            if (!success) {
                commitState = CommitState.ROLLBACK_FAILED;
            }
        }
    }

    final void afterCommit() {
        if (listener != null) {
            listener.afterCommit();
        }
        if (deleteState == DeleteState.DELETE_REQUESTED) {
            deleteState = DeleteState.DELETED;
        }
    }

    final void afterRollback() {
        if (listener != null) {
            listener.afterRollback();
        }
        if (deleteState == DeleteState.DELETE_REQUESTED) {
            deleteState = DeleteState.NOT_DELETED;
        }
    }

    public abstract void doRollback(Transaction t) throws IOException;

    public abstract static class CommitListener {
        public abstract void afterCommit();
        public abstract void afterRollback();
    }
}
