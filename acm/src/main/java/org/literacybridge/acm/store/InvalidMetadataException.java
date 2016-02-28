package org.literacybridge.acm.store;

public class InvalidMetadataException extends Exception {
  private static final long serialVersionUID = 1L;

  public InvalidMetadataException(String msg) {
    super(msg);
  }
}
