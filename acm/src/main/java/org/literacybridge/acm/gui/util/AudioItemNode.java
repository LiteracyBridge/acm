package org.literacybridge.acm.gui.util;

import org.literacybridge.acm.store.AudioItem;

/**
 * Associates a random, arbitrary, unknown object with an AudioItem (of which it is a property).
 *
 * It is not clear how this is useful. Any holder of one of these objects must already know
 * the type of the object, and how to deal with it.
 * @param <T>
 */
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
