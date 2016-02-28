package org.literacybridge.acm.gui.messages;

import javax.swing.SortOrder;

public class AudioItemTableSortOrderMessage extends Message {
  private final Object identifier;
  private final SortOrder sortOrder;

  public AudioItemTableSortOrderMessage(Object identifier,
      SortOrder sortOrder) {
    this.identifier = identifier;
    this.sortOrder = sortOrder;
  }

  public Object getIdentifier() {
    return identifier;
  }

  public SortOrder getSortOrder() {
    return sortOrder;
  }
}
