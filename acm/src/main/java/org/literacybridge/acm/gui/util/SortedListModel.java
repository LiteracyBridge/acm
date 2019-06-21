package org.literacybridge.acm.gui.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.AbstractListModel;

public class SortedListModel<T extends Comparable<T>>
    extends AbstractListModel {
  private SortedSet<T> model;

  public SortedListModel() {
    model = new TreeSet<T>();
  }

  @Override
  public int getSize() {
    return model.size();
  }

  @Override
  public T getElementAt(int index) {
    return (T) model.toArray()[index];
  }

  public SortedSet<T> getModel() {
    return new TreeSet<>(model);
  }

  public void add(T element) {
    if (model.add(element)) {
      fireContentsChanged(this, 0, getSize());
    }
  }

  public void addAll(T elements[]) {
    Collection<T> c = Arrays.asList(elements);
    model.addAll(c);
    fireContentsChanged(this, 0, getSize());
  }

  public void clear() {
    model.clear();
    fireContentsChanged(this, 0, getSize());
  }

  public boolean contains(T element) {
    return model.contains(element);
  }

  public T firstElement() {
    return model.first();
  }

  public Iterator<T> iterator() {
    return model.iterator();
  }

  public T lastElement() {
    return model.last();
  }

  public boolean removeElement(T element) {
    boolean removed = model.remove(element);
    if (removed) {
      fireContentsChanged(this, 0, getSize());
    }
    return removed;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SortedListModel)) return false;
    SortedListModel<?> that = (SortedListModel<?>) o;
    return model.equals(that.model);
  }

  @Override
  public int hashCode() {
    return Objects.hash(model);
  }
}
