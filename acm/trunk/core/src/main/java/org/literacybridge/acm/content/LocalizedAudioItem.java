package org.literacybridge.acm.content;

import java.io.File;
import java.util.Locale;

import org.literacybridge.acm.db.Persistable;
import org.literacybridge.acm.db.PersistentAudioItem;
import org.literacybridge.acm.db.PersistentLocale;
import org.literacybridge.acm.db.PersistentLocalizedAudioItem;
import org.literacybridge.acm.db.PersistentManifest;
import org.literacybridge.acm.db.PersistentMetadata;
import org.literacybridge.acm.db.PersistentReferencedFile;
import org.literacybridge.acm.metadata.Metadata;

public class LocalizedAudioItem implements Persistable {

	private PersistentLocalizedAudioItem mItem;

	public LocalizedAudioItem() {
		mItem = new PersistentLocalizedAudioItem();
	}

	public LocalizedAudioItem(
			PersistentLocalizedAudioItem persistentLocalizedAudioItem) {
		mItem = persistentLocalizedAudioItem;
	}

	public PersistentLocalizedAudioItem getPersistentObject() {
		return mItem;
	}

	public LocalizedAudioItem(String uuId, Locale locale) {
		this();
		mItem.setUuid(uuId);
		setLocale(locale);
	}

	private void setLocale(Locale locale) {
		//TODO prevent multiple locale entries with the same value
		PersistentLocale persistentLocale = new PersistentLocale();
		persistentLocale.setCountry(locale.getCountry());
		persistentLocale.setLanguage(locale.getLanguage());
		mItem.setPersistentLocale(persistentLocale);
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

	public void setUuid(String uuid) {
		mItem.setUuid(uuid);
	}

	public Metadata getMetadata() {
		if (mItem.getPersistentMetadata() == null) {
			PersistentMetadata m = new PersistentMetadata();
			m.setPersistentLocalizedAudioItem(mItem);
			mItem.setPersistentMetadata(m);
		}
		return new Metadata(mItem.getPersistentMetadata());
	}

	public Manifest getManifest() {
		if (mItem.getPersistentManifest() == null) {
			return null;
		}
		return new Manifest(mItem.getPersistentManifest());
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
