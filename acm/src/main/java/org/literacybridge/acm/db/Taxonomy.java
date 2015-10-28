package org.literacybridge.acm.db;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.persistence.EntityManager;

import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.Persistable;

import com.google.common.collect.Lists;

public class Taxonomy implements Persistable {

    private static String rootUUID = DefaultLiteracyBridgeTaxonomy.LB_TAXONOMY_UID;

    private Category mRootCategory;

    private static Taxonomy taxonomy;

    public Taxonomy() {
        PersistentString title = new PersistentString("root");
        PersistentString desc = new PersistentString("root node");
        PersistentCategory root = new PersistentCategory(title, desc, rootUUID);
        mRootCategory = new DBCategory(root);
    }

    public Taxonomy(String uuid) {
        PersistentCategory root = PersistentCategory.getFromDatabase(uuid);
        if (root == null) {
            PersistentString title = new PersistentString("root");
            PersistentString desc = new PersistentString("root node");
            root = new PersistentCategory(title, desc, uuid);
            mRootCategory = new DBCategory(root);
        }
    }

    public static Taxonomy getTaxonomy() {
        return getTaxonomy(DefaultLiteracyBridgeTaxonomy.LB_TAXONOMY_UID);
    }

    private static Taxonomy getTaxonomy(String uuid) {
        if (taxonomy != null) {
            return taxonomy;
        }

        taxonomy = new Taxonomy();

        PersistentCategory root = PersistentCategory.getFromDatabase(uuid);
        DefaultLiteracyBridgeTaxonomy.TaxonomyRevision latestRevision = DefaultLiteracyBridgeTaxonomy.loadLatestTaxonomy();

        if (root == null) {
            PersistentString title = new PersistentString("root");
            PersistentString desc = new PersistentString("root node");
            root = new PersistentCategory(title, desc, rootUUID);
            taxonomy = createNewTaxonomy(latestRevision, root, null);
            taxonomy.commit();
        } else if (root.getRevision() < latestRevision.revision) {
            updateTaxonomy(root, latestRevision);
            taxonomy.commit();
            DefaultLiteracyBridgeTaxonomy.print(taxonomy.mRootCategory);
        } else {
            taxonomy.mRootCategory = new DBCategory(root);
        }

        return taxonomy;
    }

    private static Taxonomy createNewTaxonomy(DefaultLiteracyBridgeTaxonomy.TaxonomyRevision revision, PersistentCategory existingRoot, final Map<String, PersistentCategory> existingCategories) {
        Taxonomy taxonomy = new Taxonomy();
        existingRoot.setRevision(revision.revision);
        existingRoot.setOrder(0);
        taxonomy.mRootCategory = new DBCategory(existingRoot);

        revision.createTaxonomy(taxonomy, existingCategories);
        return taxonomy;
    }

    private static void updateTaxonomy(PersistentCategory existingRoot, DefaultLiteracyBridgeTaxonomy.TaxonomyRevision latestRevision) {
        System.out.println("Updating taxonomy");

        final Map<String, PersistentCategory> existingCategories = new HashMap<String, PersistentCategory>();
        traverse(null, existingRoot, new Function() {
            @Override public void apply(PersistentCategory parent, PersistentCategory root) {
                existingCategories.put(root.getUuid(), root);
            }
        });

        existingRoot.clearPersistentChildCategories();
        for (PersistentCategory cat : existingCategories.values()) {
            cat.clearPersistentChildCategories();
            cat.setPersistentParentCategory(null);
            cat.commit();
        }

        taxonomy = createNewTaxonomy(latestRevision, existingRoot, existingCategories);
    }

    private static interface Function {
        public void apply(PersistentCategory parent, PersistentCategory root);
    }

    private static void traverse(PersistentCategory parent, PersistentCategory root, Function function) {
        function.apply(parent, root);
        for (PersistentCategory child : root.getPersistentChildCategoryList()) {
            traverse(root, child, function);
        }
    }

    public Category getRootCategory() {
        return mRootCategory;
    }

    public List<Category> getCategoryList() {
        return mRootCategory.getChildren();
    }

    public boolean isRoot(Category category) {
        return category == this.mRootCategory;
    }

    public boolean addChild(Category parent, Category newChild) {
        parent.addChild(newChild);
        return true;
    }

    /**
     * Returns the facet count for all categories that are stored
     * in the database.
     *
     * Key: database id (getId())
     * Value: count value
     *
     * Note: Returns '0' for unassigned categories.
     */
    public static Map<Integer, Integer> getFacetCounts(String filter, List<Category> categories, List<Locale> locales) {
        List<PersistentCategory> persistentCategories = null;

        if (categories != null) {
            persistentCategories = Lists.newArrayListWithCapacity(categories.size());

            for (Category cat : categories) {
                persistentCategories.add(((DBCategory) cat).getPersistentObject());
            }
        }
        return PersistentCategory.getFacetCounts(filter, persistentCategories, locales);
    }

    public static Map<String, Integer> getLanguageFacetCounts(String filter, List<Category> categories, List<Locale> locales) {
        return PersistentQueries.getLanguageFacetCounts(filter, categories, locales);
    }

    public Integer getId() {
        return mRootCategory.getId();
    }

    public Taxonomy commit() {
        mRootCategory.commit();
        return this;
    }

    @Override
    public Taxonomy commit(EntityManager em) {
        mRootCategory.commit(em);
        return this;
    }

    public void destroy() {
        mRootCategory.destroy();
    }

    public Taxonomy refresh() {
        mRootCategory.refresh();
        return this;
    }

    public static void resetTaxonomy() {
        taxonomy = null;
    }
}
