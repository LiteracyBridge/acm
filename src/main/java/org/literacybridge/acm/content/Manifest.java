package main.java.org.literacybridge.acm.content;

import java.io.File;
import java.util.List;

public class Manifest {
	// TODO: if we want per-file revisions, then we need to add a revision to this class
	public static class ReferencedFile {
		// actual file (e.g. audio track) that his ReferencedFile instance points to
		File file;
		
		// fallback file, e.g. there could be a mp3 file and an additional wav fallback file
		// this is optional and can be null
		ReferencedFile fallback;
		
		// length of this audio file in milliseconds
		int lengthInMilliseconds;
		
		// size in bytes
		long sizeInBytes;
	}
	
	List<ReferencedFile> referencedFiles;
}
