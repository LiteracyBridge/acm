package org.literacybridge.acm.store;

import java.util.List;
import java.util.Locale;

public abstract class Category implements Persistable {
    public abstract int getOrder();

    public abstract void setOrder(int order);

    public abstract void setLocalizedCategoryDescription(Locale locale, String name,
            String description);

    public abstract void setDefaultCategoryDescription(String name,
            String description);

    public abstract String getCategoryName(Locale languageCode);

    public abstract Category getParent();

    public abstract void addChild(Category childCategory);

    public abstract List<Category> getChildren();

    public abstract List<Category> getSortedChildren();

    public abstract boolean hasChildren();

    public abstract Integer getId();

    public abstract String getUuid();

    public abstract void setUuid(String uuid);
}