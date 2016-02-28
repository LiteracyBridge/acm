package org.literacybridge.acm.gui.util;

import org.literacybridge.acm.store.AudioItem;

public class AudioItemNode<T> {
  private final AudioItem audioItem;
  private final T value;
  private boolean enabled;

  public AudioItemNode(AudioItem audioItem, T value) {
    this.value = value;
    this.audioItem = audioItem;
    enabled = false;
  }

  public T getValue() {
    return value;
  }

  @Override
  public String toString() {
    return value != null ? value.toString() : "";
  }

  public AudioItem getAudioItem() {
    return audioItem;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}
