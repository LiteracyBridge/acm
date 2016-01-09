package org.literacybridge.acm.store;

import java.io.IOException;

import org.literacybridge.acm.store.MetadataStore.Transaction;

public interface Persistable {
    <T extends Transaction> void _commitTransaction(T t) throws IOException;
}
