package org.literacybridge.acm.db;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.persistence.EntityManager;

import org.literacybridge.acm.gui.util.language.LanguageUtil;
import org.literacybridge.acm.store.Category;

class DBCategory extends Category {
    private PersistentCategory mCategory;

    public DBCategory() {
        mCategory = new PersistentCategory();
    }

    public DBCategory(String uuid) {
        this();
        mCategory.setUuid(uuid);
    }

    public DBCategory(PersistentCategory category) {
        mCategory = category;
    }

    @Override public int hashCode() {
        return mCategory.getUuid().hashCode();
    }

    @Override public boolean equals(Object o) {
        if (o == null || !(o instanceof Category)) {
            return false;
        }

        Category other = (Category) o;
        return other.getUuid().equals(getUuid());
    }

    public PersistentCategory getPersistentObject() {
        return mCategory;
    }

    public int getOrder() {
        return mCategory.getOrder();
    }

    public void setOrder(int order) {
        mCategory.setOrder(order);
    }

    public void setLocalizedCategoryDescription(Locale locale, String name,
            String description) {
        if ((mCategory.getTitle() == null)
                || (mCategory.getDescription() == null)) {
            throw new IllegalStateException(
                    "Could not add localized category title/description. There is no default title/description set yet.");
        }
        PersistentString title = mCategory.getTitle();
        if (doesLocaleExists(locale, title
                .getPersistentLocalizedStringList()) == -1) {
            PersistentLocalizedString localizedTitle = new PersistentLocalizedString();
            localizedTitle.setTranslation(name);
            title.addPersistentLocalizedString(localizedTitle);
        }
        PersistentString desc = mCategory.getDescription();
        if (doesLocaleExists(locale, desc
                .getPersistentLocalizedStringList()) == -1) {
            PersistentLocalizedString localizedDesc = new PersistentLocalizedString();
            localizedDesc.setTranslation(description);
            desc.addPersistentLocalizedString(localizedDesc);
        }
    }

    private int doesLocaleExists(Locale locale,
            List<PersistentLocalizedString> list) {
        for (int i = 0; i <= list.size() - 1; i++) {
            if (compareLocale(locale, list.get(i).getPersistentLocale())) {
                return i;
            }
        }
        return -1;
    }

    private boolean compareLocale(Locale l1, PersistentLocale l2) {
        if ((l1.getCountry().equals(l2.getCountry()))
                && (l1.getLanguage().equals(l2.getLanguage()))) {
            return true;
        }
        return false;
    }

    public void setDefaultCategoryDescription(String name,
            String description) {
        mCategory.setTitle(new PersistentString(name));
        mCategory.setDescription(new PersistentString(description));
    }

    public String getCategoryName(Locale languageCode) {
        // TODO: Support localized category names
        return mCategory.getTitle().toString();
    }

    public DBCategory getParent() {
        PersistentCategory parent = mCategory.getPersistentParentCategory();
        if (parent == null) {
            return null;
        }
        return new DBCategory(mCategory.getPersistentParentCategory());
    }

    public void addChild(Category childCategory) {
        mCategory.addPersistentChildCategory(((DBCategory) childCategory)
                .getPersistentObject());
    }

    public List<Category> getChildren() {
        List<Category> children = new LinkedList<Category>();
        for (PersistentCategory child : mCategory
                .getPersistentChildCategoryList()) {
            children.add(new DBCategory(child));
        }
        return children;
    }

    public List<Category> getSortedChildren() {
        List<Category> children = getChildren();
        Collections.sort(children, new Comparator<Category>() {
            @Override public int compare(Category c1, Category c2) {
                return c1.getOrder() - c2.getOrder();
            }
        });

        return children;
    }

    //      private List<Category> getAllChildren(PersistentCategory category,
    //              List<Category> children) {
    //          List<PersistentCategory> categories = category
    //                  .getPersistentChildCategoryList();
    //          if (categories.size() != 0) {
    //              for (PersistentCategory c : categories) {
    //                  getAllChildren(c, children);
    //              }
    //          }
    //          children.add(new Category(category));
    //          return children;
    //      }

    public boolean hasChildren() {
        List<PersistentCategory> children = mCategory
                .getPersistentChildCategoryList();
        return children != null && !children.isEmpty();
    }

    public Integer getId() {
        return mCategory.getId();
    }

    public Category commit() {
        mCategory = mCategory.<PersistentCategory> commit();
        return this;
    }

    @Override
    public Category commit(EntityManager em) {
        mCategory = mCategory.<PersistentCategory> commit(em);
        return this;
    }

    public void destroy() {
        mCategory.destroy();
    }

    public Category refresh() {
        mCategory = mCategory.<PersistentCategory> refresh();
        return this;
    }

    public String getUuid() {
        return mCategory.getUuid();
    }

    public void setUuid(String uuid) {
        this.mCategory.setUuid(uuid);
    }

    public static Category getFromDatabase(String uid) {
        PersistentCategory category = PersistentCategory.getFromDatabase(uid);
        if (category == null) {
            return null;
        }
        return new DBCategory(category);
    }

    @Override public String toString() {
        return getCategoryName(LanguageUtil.getUILanguage()).toString();
    }
}
