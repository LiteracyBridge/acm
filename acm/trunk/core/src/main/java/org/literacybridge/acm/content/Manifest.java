package org.literacybridge.acm.content;

import java.io.File;

import java.util.LinkedList;
import java.util.List;

import org.literacybridge.acm.db.Persistable;
import org.literacybridge.acm.db.PersistentManifest;
import org.literacybridge.acm.db.PersistentReferencedFile;
import org.literacybridge.acm.metadata.Metadata;

public class Manifest implements Persistable {
    
        private PersistentManifest mManifest;
        
        public Manifest() {
            mManifest = new PersistentManifest();
        }

        public Manifest(PersistentManifest manifest) {
            mManifest = manifest;
        }

        public Integer getId() {
            return mManifest.getId();
        }
    
        public Manifest commit() {
            mManifest = mManifest.<PersistentManifest>commit();
            return this;
        }
    
        public void destroy() {
            mManifest.destroy();
        }
    
        public Manifest refresh() {
            mManifest = mManifest.<PersistentManifest>refresh();
            return this;
        }
        
        List<ReferencedFile> getReferencedFiles() {
            List<ReferencedFile> referencedFiles = new LinkedList<ReferencedFile>();
            for (PersistentReferencedFile file : mManifest.getPersistentReferencedFileList()) {
                referencedFiles.add(new ReferencedFile(file));    
            }
            return referencedFiles;
        }

        // TODO: if we want per-file revisions, then we need to add a revision to this class
	public static class ReferencedFile implements Persistable {
                private PersistentReferencedFile mReferencedFile;
                
                public ReferencedFile() {
                    mReferencedFile = new PersistentReferencedFile();
                }
            
                public ReferencedFile(PersistentReferencedFile file) {
                    mReferencedFile = file;
                }
            
		// actual file (e.g. audio track) that his ReferencedFile instance points to
		File file;
		
		// fallback file, e.g. there could be a mp3 file and an additional wav fallback file
		// this is optional and can be null
		ReferencedFile fallback;
		
		// length of this audio file in milliseconds
		int lengthInMilliseconds;
		
		// size in bytes
		long sizeInBytes;
                
                public Integer getId() {
                    return mReferencedFile.getId();
                }
        
                public ReferencedFile commit() {
                    mReferencedFile = mReferencedFile.<PersistentReferencedFile>commit();
                    return this;
                }
                
                public void destroy() {
                    mReferencedFile.destroy();
                }
                
                public ReferencedFile refresh() {
                    mReferencedFile = mReferencedFile.<PersistentReferencedFile>refresh();
                    return this;
                }
    }
}
