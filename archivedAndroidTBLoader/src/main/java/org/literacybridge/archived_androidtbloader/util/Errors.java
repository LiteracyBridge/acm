package org.literacybridge.archived_androidtbloader.util;

/**
 * Errors to be shown to the user. Includes provision for an error code. The error code should be small,
 * easy to remember for a few minutes, easy to write down accurately. "80004005" is a good example
 * of a bad code for this purpose; these are for users to communicate back to support, not for
 * developers to track the intricacies of what's failing.
 */
public enum Errors {
    NoConfig(101);

    public final int errorNo;

    private Errors(int errorNo) {
        this.errorNo = errorNo;
    }
}
