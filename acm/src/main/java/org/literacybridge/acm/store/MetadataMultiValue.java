package org.literacybridge.acm.store;

import java.util.List;

import com.google.common.collect.Lists;

/**
 * Convenience class for multi-valued {@link MetadataField}s.
 * Use this {@link MetadataValue} implementation if a single {@link MetadataField} should contain a
 * list of multiple {@link MetadataValue}s.
 */
public class MetadataMultiValue<T> extends MetadataValue<List<MetadataValue<T>>> {
    public MetadataMultiValue(T value) {
        super(Lists.<MetadataValue<T>>newArrayList());
    }

    public void addValue(MetadataValue<T> value) {
        getValue().add(value);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (MetadataValue<T> value : getValue()) {
            builder.append(value.toString());
            builder.append(",");
        }

        if (builder.length() > 0) {
            // remove trailing ','
            builder.setLength(builder.length() - 1);
        }

        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MetadataValue<?>)) {
            return false;
        }

        MetadataValue<?> otherValue = (MetadataValue<?>) o;

        if (!(otherValue.getValue() instanceof MetadataMultiValue<?>)) {
            return false;
        }

        MetadataMultiValue<?> otherMultiValue = (MetadataMultiValue<?>) otherValue.getValue();

        for (int i = 0; i < getValue().size(); i++) {
            if (!getValue().get(i).equals(otherMultiValue.getValue().get(i))) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        int code = 0;
        for (MetadataValue<T> value : getValue()) {
            code = code * 31 + value.hashCode();
        }

        return code;
    }
}
