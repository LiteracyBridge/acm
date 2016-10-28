package org.literacybridge.acm.tbloader;

import javax.swing.*;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

/**
 * Created by bill on 8/24/16. This is a ComboBoxModel that can filter by a simple string.
 * The target application is the community list for projects that have many,
 * many communities.
 */
public class FilteringComboBoxModel<E> extends DefaultComboBoxModel<E> {
  private boolean filtering;
  private String currentFilter;
  private Vector<E> filteredElements;

  public FilteringComboBoxModel() {
    filtering = false;
    currentFilter = null;
    filteredElements = null;
  }

  /**
   * Sets the filter string, which will fire events to refresh the model.
   * @param filter The new String. Empty or null means "no filtering". Otherwise
   *               anything from the unfiltered element list that contains the
   *               filter string (ignoring case) will be included into the
   *               filtered element list.
   *
   *               If the selected item is filtered out, we try to move up the
   *               list (towards index 0) to find an element that both previously
   *               and currently match. If no such element is found, the first
   *               element is selected, if there are any elements in the new list.
   * @return The old filter string.
   */
  public String setFilterString(String filter) {
    String oldFilter = currentFilter;
    Vector<E> newElements = null;
    boolean newFiltering = filter != null && filter.length() > 0;
    if (!filtering && !newFiltering) return oldFilter;
    if (filtering && newFiltering && filter.equalsIgnoreCase(currentFilter)) return oldFilter;

    // If we will be filtering after this, get the new list, handle selection.
    if (newFiltering) {
      // Get the new filtered list.
      newElements = getFilteredElements(filter);
      // If the current selection is no longer included in the list, select something earlier.
      translateSelectedItem(newElements);
    }

    // Fire all-removed, all-added
    if (getSize() > 0)
      fireIntervalRemoved(this, 0, getSize()-1);
    filtering = newFiltering;
    filteredElements = newElements;
    currentFilter = filter;
    if (getSize() > 0)
      fireIntervalAdded(this, 0, getSize()-1);
    return oldFilter;
  }

  /**
   * Given a filter, return a list of all items from the unfiltered element
   * list that match.
   * @param filter The filter to be applied.
   * @return A list of matching elements.
   */
  private Vector<E> getFilteredElements(String filter) {
    Vector<E> result = new Vector<E>();
    // Look at the raw, unfiltered list to build the new filtered list.
    for (int ix=0; ix < super.getSize(); ix++) {
      E o = super.getElementAt(ix);
      if (match(filter, o.toString()))
        result.add(o);
    }
    return result;
  }

  private boolean match(String filter, String x) {
    return (x.toUpperCase().contains(filter.toUpperCase()));
  }

  /**
   * Called when the filter changes. If the current selection is in
   * the new list, it remains the current selection. Otherwise, the next
   * previous element from the current list that is present in the new list
   * becomes the new selection. Otherwise the first element, if there is one,
   * becomes the new selection.
   * @param newList
   */
  private void translateSelectedItem(Vector<E> newList) {
    Set<E> newElements = new HashSet<E>(newList);
    Object newSelection = null;
    // Starting with the current selection...
    int ix = getIndexOf(getSelectedItem());
    // walk backwards looking for an element in the new list.
    while (ix >= 0) {
      if (newElements.contains(getElementAt(ix))) {
        newSelection = getElementAt(ix);
        break;
      }
      ix--;
    }
    // Have *some* selection, if there are elements.
    if (newSelection == null && newList.size() > 0)
      newSelection = newList.get(0);
    // If it needs to change, change it.
    if (newSelection != getSelectedItem())
      setSelectedItem(newSelection);
  }

  @Override
  public int getSize() {
    if (filtering)
      return filteredElements.size();
    else
      return super.getSize();
  }

  @Override
  public E getElementAt(int index) {
    if (filtering)
      return filteredElements.get(index);
    else
      return super.getElementAt(index);
  }

  @Override
  public int getIndexOf(Object anObject) {
    if (filtering)
      return filteredElements.indexOf(anObject);
    else
      return super.getIndexOf(anObject);
  }

  /**
   * Helper function to throw an exception if filtering is in place. This is
   * in lieu of implementing the mutating functions in a filtering mode. We
   * don't need them in the applicationu (the list is static), but if we do
   * make a change that tries to mutate a filtered list, this will catch it.
   */
  private void noFilter() {
    if (filtering)
      throw new IllegalStateException("Can't manipulate model when a filter is in effect");
  }

  @Override
  public void addElement(E anObject) {
    noFilter();
    super.addElement(anObject);
  }

  @Override
  public void insertElementAt(E anObject, int index) {
    noFilter();
    super.insertElementAt(anObject, index);
  }

  @Override
  public void removeElementAt(int index) {
    noFilter();
    super.removeElementAt(index);
  }

  @Override
  public void removeElement(Object anObject) {
    noFilter();
    super.removeElement(anObject);
  }

  @Override
  public void removeAllElements() {
    noFilter();
    super.removeAllElements();
  }
}
