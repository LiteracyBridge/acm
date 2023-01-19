package org.literacybridge.acm.importexport;

import java.io.File;

class WavMdImporter extends AnyMdImporter {
  WavMdImporter(File audioFile) {
    super(audioFile);
  }

  static String[] getSupportedFileExtensions() {
    return new String[] { "wav" };
  }
}
