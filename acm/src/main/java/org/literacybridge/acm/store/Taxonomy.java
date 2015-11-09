package org.literacybridge.acm.store;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.store.MetadataStore.Transaction;

public class Taxonomy implements Persistable {

    private static String rootUUID = DefaultLiteracyBridgeTaxonomy.LB_TAXONOMY_UID;

    private Category mRootCategory;

    private static Taxonomy taxonomy;

    public Taxonomy() {
        mRootCategory = ACMConfiguration.getCurrentDB().getMetadataStore().newCategory(rootUUID);
        mRootCategory.setDefaultCategoryDescription("root", "root node");
    }

    public Taxonomy(String uuid) {
        mRootCategory = ACMConfiguration.getCurrentDB().getMetadataStore().getCategory(rootUUID);
        if (mRootCategory == null) {
            mRootCategory = ACMConfiguration.getCurrentDB().getMetadataStore().newCategory(rootUUID);
            mRootCategory.setDefaultCategoryDescription("root", "root node");
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

        Category root = ACMConfiguration.getCurrentDB().getMetadataStore().getCategory(uuid);
        DefaultLiteracyBridgeTaxonomy.TaxonomyRevision latestRevision = DefaultLiteracyBridgeTaxonomy.loadLatestTaxonomy();

        if (root == null) {
            root = ACMConfiguration.getCurrentDB().getMetadataStore().newCategory(uuid);
            root.setDefaultCategoryDescription("root", "root node");
            taxonomy = createNewTaxonomy(latestRevision, root, null);
            ACMConfiguration.getCurrentDB().getMetadataStore().commit(taxonomy);
        } else if (root.getRevision() < latestRevision.revision) {
            updateTaxonomy(root, latestRevision);
            ACMConfiguration.getCurrentDB().getMetadataStore().commit(taxonomy);
            DefaultLiteracyBridgeTaxonomy.print(taxonomy.mRootCategory);
        } else {
            taxonomy.mRootCategory = root;
        }

        return taxonomy;
    }

    private static Taxonomy createNewTaxonomy(DefaultLiteracyBridgeTaxonomy.TaxonomyRevision revision, Category existingRoot, final Map<String, Category> existingCategories) {
        Taxonomy taxonomy = new Taxonomy();
        existingRoot.setRevision(revision.revision);
        existingRoot.setOrder(0);
        taxonomy.mRootCategory = existingRoot;

        revision.createTaxonomy(taxonomy, existingCategories);
        return taxonomy;
    }

    private static void updateTaxonomy(Category existingRoot, DefaultLiteracyBridgeTaxonomy.TaxonomyRevision latestRevision) {
        System.out.println("Updating taxonomy");

        final Map<String, Category> existingCategories = new HashMap<String, Category>();
        traverse(null, existingRoot, new Function() {
            @Override public void apply(Category parent, Category root) {
                existingCategories.put(root.getUuid(), root);
            }
        });

        existingRoot.clearChildren();
        Transaction transaction = ACMConfiguration.getCurrentDB().getMetadataStore().newTransaction();
        for (Category cat : existingCategories.values()) {
            cat.clearChildren();
            cat.setParent(null);
            transaction.add(cat);
        }
        transaction.begin();
        transaction.commit();

        taxonomy = createNewTaxonomy(latestRevision, existingRoot, existingCategories);
    }

    private static interface Function {
        public void apply(Category parent, Category root);
    }

    private static void traverse(Category parent, Category root, Function function) {
        function.apply(parent, root);
        for (Category child : root.getChildren()) {
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

    public Integer getId() {
        return mRootCategory.getId();
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
