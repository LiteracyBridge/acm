package org.literacybridge.acm.store;

import java.io.Serializable;

public class MetadataValue<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private T value;

    public MetadataValue(T value) {
        this.value = value;
    }

    public static <T> MetadataValue<T> newValue(T value) {
        return new MetadataValue<T>(value);
    }

    public final T getValue() {
        return this.value;
    }

    public final void setValue(T value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MetadataValue<?>)) {
            return false;
        }

        return value.equals(((MetadataValue<?>) o).value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
