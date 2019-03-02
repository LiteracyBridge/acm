package org.literacybridge.acm.gui.MainWindow.audioItems;

import java.util.Comparator;
import java.util.Locale;

import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.gui.util.AudioItemNode;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.MetadataField;
import org.literacybridge.acm.store.MetadataValue;

/**
 * This class contains configuration for each column in the AudioItemTable, such
 * as the column width and label. It also has a ValueProdier, which returns the
 * content of this column for a particular row, and optionally provides a
 * comparator for the values.
 *
 * @param <T>
 *          The type of data values in this column.
 */
public final class ColumnInfo<T> {
  public final static int WIDTH_NOT_SET = -1;

  private int columnIndex;
  private Comparator<AudioItemNode<T>> comparator;
  private final String columnLabelProperty;
  private final int preferredWidth;
  private final int maxWidth;
  private final ValueProvider<T> valueProvider;

  private ColumnInfo(String columnLabelProperty, int maxWidth,
      int preferredWidth, ValueProvider<T> valueProvider) {
    this.columnLabelProperty = columnLabelProperty;
    this.preferredWidth = preferredWidth;
    this.maxWidth = maxWidth;
    this.valueProvider = valueProvider;
  }

  ColumnInfo<T> setColumnIndex(int columnIndex) {
    this.columnIndex = columnIndex;
    return this;
  }

  ColumnInfo<T> setComparator(Comparator<AudioItemNode<T>> comparator) {
    this.comparator = comparator;
    return this;
  }

  public int getColumnIndex() {
    return columnIndex;
  }

  public String getColumnName(Locale locale) {
    return columnLabelProperty == null ? ""
        : LabelProvider.getLabel(columnLabelProperty, locale);
  }

  public int getPreferredWidth() {
    return preferredWidth;
  }

  public int getMaxWidth() {
    return maxWidth;
  }

  public ValueProvider<T> getValueProvider() {
    return valueProvider;
  }

  /**
   * @return null, if no custom comparator was set for this ColumnInfo. In that
   *         case the table will use a default String comparator using the
   *         values' toString() methods.
   */
  public Comparator<AudioItemNode<T>> getComparator() {
    return comparator;
  }

  static <T> ColumnInfo<T> newColumnInfo(String columnLabelProperty,
      int maxWidth, int preferredWidth, ValueProvider<T> valueProvider) {
    return new ColumnInfo<>(columnLabelProperty, maxWidth, preferredWidth,
        valueProvider);
  }

  static <T> ColumnInfo<T> newMetadataColumnInfo(String columnLabelProperty,
      int maxWidth, int preferredWidth, final MetadataField<T> metadataField) {
    return new ColumnInfo<>(columnLabelProperty, maxWidth, preferredWidth,
        new ValueProvider<T>(true) {
          @Override
          protected AudioItemNode<T> getValue(AudioItem audioItem) {
            MetadataValue<T> value = audioItem.getMetadata()
                .getMetadataValue(metadataField);
            return new AudioItemNode<T>(audioItem,
                value != null ? value.getValue() : null);
          }
        });
  }

  public static abstract class ValueProvider<T> {
    // The AudioItemTableModel needs to know for each column if the value
    // returned by the
    // ValueProvider can be cashed until invalidated by a DataChangeEventType,
    // or if
    // ValueProvider#getValue() must be called every time.

    /**
     * BE: This "cache" is broken. Rather than lazy-populating the values, they're all simply
     * pre-computed. There is no means to invalidate the cache when large-scale changes
     * happen. So data is just "stale", not cached. :(
     * I expect that ripping it out entirely would give the biggest perf boost.
     * Sigh.
     * Add it to the list.
     */
    private final boolean valueCachable;

    public ValueProvider(boolean valueCachable) {
      this.valueCachable = valueCachable;
    }

    public boolean isValueCachable() {
      return false;
    }

    protected abstract AudioItemNode<T> getValue(AudioItem audioItem);
  }
}
