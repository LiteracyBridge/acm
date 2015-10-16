package org.literacybridge.acm.content;

import java.util.Locale;

import org.literacybridge.acm.db.Persistable;
import org.literacybridge.acm.db.PersistentLocale;
import org.literacybridge.acm.db.PersistentLocalizedAudioItem;
import org.literacybridge.acm.db.PersistentMetadata;
import org.literacybridge.acm.metadata.Metadata;

public class LocalizedAudioItem implements Persistable {

    private PersistentLocalizedAudioItem mItem;

    public LocalizedAudioItem(
            PersistentLocalizedAudioItem persistentLocalizedAudioItem) {
        mItem = persistentLocalizedAudioItem;
    }

    public PersistentLocalizedAudioItem getPersistentObject() {
        return mItem;
    }

    public LocalizedAudioItem(String uuid) {
        mItem = new PersistentLocalizedAudioItem(uuid);
    }

    public Locale getLocale() {
        PersistentLocale persistentLocale = mItem.getPersistentLocale();
        if (persistentLocale == null) {
            return null;
        }
        return new Locale(persistentLocale.getLanguage(), persistentLocale
                .getCountry());
    }

    public Integer getId() {
        return mItem.getId();
    }

    public String getUuid() {
        return mItem.getUuid();
    }

    public AudioItem getParentAudioItem() {
        if (mItem.getPersistentAudioItem() == null) {
            return null;
        }
        return new AudioItem(mItem.getPersistentAudioItem());
    }

    public LocalizedAudioItem commit() {
        mItem = mItem.<PersistentLocalizedAudioItem> commit();
        return this;
    }

    public void destroy() {
        mItem.destroy();
    }

    public LocalizedAudioItem refresh() {
        mItem = mItem.<PersistentLocalizedAudioItem> refresh();
        return this;
    }
}
