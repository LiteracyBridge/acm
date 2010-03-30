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
        	
        private Metadata mMetadata;        
                
        public LocalizedAudioItem() {
            mItem = new PersistentLocalizedAudioItem();
        }
        
        public LocalizedAudioItem(PersistentLocalizedAudioItem persistentLocalizedAudioItem) {
            mItem = persistentLocalizedAudioItem;
        }
        
        public PersistentLocalizedAudioItem getPersistentObject() {
            return mItem;
        }
        
	public LocalizedAudioItem(String uuId, Locale locale, File pathToItem) {
                this();
		mItem.setUuid(uuId);
                setLocale(locale);
	}
	
        private void setLocale(Locale locale) {
            PersistentLocale persistentLocale = mItem.getPersistentLocale();
            if (persistentLocale == null) {
                persistentLocale = new PersistentLocale();
            }
            persistentLocale.setCountry(locale.getCountry());
            persistentLocale.setLanguage(locale.getLanguage());
        }
        
        public Locale getLocale() {
            PersistentLocale persistentLocale = mItem.getPersistentLocale();
            if (persistentLocale == null) {
                return null;
            }
            return new Locale(persistentLocale.getLanguage(), persistentLocale.getCountry());
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
            if (mMetadata == null) {
                mMetadata = new Metadata((mItem.getPersistentMetadata() == null) 
                                         ? new PersistentMetadata() 
                                         : mItem.getPersistentMetadata());
            }
            return mMetadata;
	}
	
	public Manifest getManifest() {
            if (mItem.getPersistentManifest() == null) {
                return null;
            }
            return new Manifest(mItem.getPersistentManifest());
	}

        public LocalizedAudioItem commit() {
            mItem = mItem.<PersistentLocalizedAudioItem>commit();
            return this;
        }
    
        public void destroy() {
            mItem.destroy();
        }
    
        public LocalizedAudioItem refresh() {
            mItem = mItem.<PersistentLocalizedAudioItem>refresh();
            return this;
        }
}
