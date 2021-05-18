package org.literacybridge.acm.config;

public interface AccessControlResolver {

    /**
     * These are the status values that may need to be dealt with.
     */
    enum AccessStatus {
        none,                                       // No status yet
        lockError(true),                       // Can't lock; ACM already open
        processError(true),                    // Can't lock, don't know why
        previouslyCheckedOutError(true),       // Already open this user, can't open with sandbox
        noNetworkNoDbError(true),              // Can't get to server, and have no local database
        noDbError(true),                       // No local database. Dropbox problem?
        checkedOut,                                 // Already checked out. Just open it.
        newDatabase,                                // New database (on server). Just open it.
        noServer(false, true),   // Can open, with sandbox
        syncFailure(false, true),// May be able to open, sandbox only.
        outdatedDb(false, true), // Can open, with sandbox
        notAvailable(false, true, true),    // Can open with sandbox, may be outdated
        userReadOnly(false, true, true),    // Can open with sandbox, may be outdated
        available;                                  // Can open it

        private final boolean fatal;
        private final boolean okWithSandbox;
        private final boolean mayBeOutdated;

        AccessStatus() {
            this(false, false, false);
        }

        AccessStatus(boolean fatal) {
            this(fatal, false, false);
        }

        AccessStatus(boolean fatal, boolean okWithSandbox) {
            this(fatal, okWithSandbox, false);
        }

        AccessStatus(boolean fatal, boolean okWithSandbox, boolean mayBeOutdated) {
            this.okWithSandbox = okWithSandbox;
            this.fatal = fatal;
            this.mayBeOutdated = mayBeOutdated;
        }

        public boolean isAlwaysOk() { return !fatal && !okWithSandbox; }
        public boolean isFatal() { return fatal; }
        public boolean isOkWithSandbox() {
            return okWithSandbox;
        }
        public boolean isMayBeOutdated() {
            return mayBeOutdated;
        }
    }

    /**
     * THese are the status values that can be returned by open() and initDb().
     */
    enum OpenStatus {
        none(true),                // No status yet.
        serverError(true),         // Error accessing server.
        notAvailableError(true),   // Not available to checkout.
        // Open states below here
        opened,                         // Opened for read-write.
        reopened,                       // Opened for read-write; it was already open.
        newDatabase,                    // Brand new database. read-write.
        openedSandboxed;                // Open, but sandboxed.

        private final boolean fatal;

        OpenStatus() { fatal = false; }
        OpenStatus(boolean fatal) { this.fatal = fatal; }

        public boolean isFatal() { return this.fatal; }
        public boolean isOpen() {
            return this.ordinal() >= opened.ordinal();
        }
    }

    enum ACCESS_CHOICE {
        USE_READONLY,
        AS_REQUESTED
    }

    enum UPDATE_CHOICE {
        KEEP,
        DELETE
    }

    /**
     * Determine what the caller wants to do in light of the access status. Note that for some status values,
     * the database can't be opened in any case.
     *
     * @param accessControl The AccessControl object controlling the db open.
     * @param accessStatus for which the resolution was requested.
     * @return USE_READONLY if processing should attempt to continue in sandboxed mode, or
     *      NO_READONLY if processing should attempt to continue as originally requested.
     */
    ACCESS_CHOICE resolveAccessStatus(AccessControl accessControl, AccessStatus accessStatus);

    /**
     * Lets the caller decide what to do after attempting the open.
     * @param accessControl The AccessControl object controlling the db open.
     * @param status The open status.
     */
    void resolveOpenStatus(AccessControl accessControl, OpenStatus status);


    UPDATE_CHOICE resolveUpdateStatus(AccessControl accesControl, UpdateDbStatus status);

    /**
     * A default implementation suitable for non-interactive applications.
     * @return an instance that does nothing, says "No" to every question.
     */
    static AccessControlResolver getDefault() {
        return new AccessControlResolver() {
            @Override
            public ACCESS_CHOICE resolveAccessStatus(AccessControl accessControl,
                AccessStatus accessStatus) {
                return ACCESS_CHOICE.AS_REQUESTED;
            }

            @Override
            public void resolveOpenStatus(AccessControl accessControl, OpenStatus status) {}

            @Override
            public UPDATE_CHOICE resolveUpdateStatus(AccessControl accesControl, UpdateDbStatus status) {
                return UPDATE_CHOICE.KEEP;
            }
        };
    }

    /**
     * These are the values that can be returned by commitDbChanges().
     */
    enum UpdateDbStatus {
        ok,                             // Saved locally, checked in status on server.
        denied,                         // Server denied checkin.
        networkError,                   // Can't access server to checkin.
        zipError                        // Error zipping the database (metadata).
    }
}
