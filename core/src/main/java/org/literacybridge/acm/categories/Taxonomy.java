package org.literacybridge.acm.categories;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.content.LocalizedAudioLabel;
import org.literacybridge.acm.content.Manifest;
import org.literacybridge.acm.db.Persistable;
import org.literacybridge.acm.db.PersistentAudioItem;
import org.literacybridge.acm.db.PersistentCategory;
import org.literacybridge.acm.db.PersistentLocale;
import org.literacybridge.acm.db.PersistentLocalizedString;
import org.literacybridge.acm.db.PersistentString;

public class Taxonomy implements Persistable {
    
        private static String rootUUID = "0";
    
        private Category mRootCategory;
	
	public Taxonomy() {
	    PersistentCategory root = PersistentCategory.getFromDatabase(rootUUID);
	    if (root == null) {
	        PersistentString title = new PersistentString("root");
	        PersistentString desc = new PersistentString("root node");
                root = new PersistentCategory(title, desc, rootUUID);
	    }
            mRootCategory = new Category(root);
	}
                
        public Taxonomy(String uuid) {
            PersistentCategory root = PersistentCategory.getFromDatabase(uuid);
            if (root == null) {
                throw new IllegalArgumentException("Root category doesn't exist in the datababase.");
            }
            mRootCategory = new Category(root);
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

        public Taxonomy commit() {
            mRootCategory.commit();
            return this;
        }
    
        public void destroy() {
            mRootCategory.destroy();
        }
    
        public Taxonomy refresh() {
            mRootCategory.refresh();
            return this;
        }

    public static class Category implements Persistable {
                private PersistentCategory mCategory;
		
                public Category() {
                    mCategory = new PersistentCategory();
                }
                
                public Category(String uuid) {
                    this();
                    mCategory.setUuid(uuid);
                }
                
                public Category(PersistentCategory category) {
                    mCategory = category;
                }
		
                public PersistentCategory getPersistentObject() {
                    return mCategory;
                }
                
                public void setLocalizedCategoryDescription(Locale locale, String name, String description) {
                    if ((mCategory.getTitle() == null) || (mCategory.getDescription() == null)) {
                        throw new IllegalStateException("Could not add localized category title/description. There is no default title/description set yet.");
                    }
                    PersistentString title = mCategory.getTitle();
                    if (doesLocaleExists(locale, title.getPersistentLocalizedStringList()) == -1) {
                        PersistentLocalizedString localizedTitle = new PersistentLocalizedString();
                        localizedTitle.setTranslation(name);
                        title.addPersistentLocalizedString(localizedTitle);
                    }
                    PersistentString desc = mCategory.getDescription();
                    if (doesLocaleExists(locale, desc.getPersistentLocalizedStringList()) == -1) {
                        PersistentLocalizedString localizedDesc = new PersistentLocalizedString();
                        localizedDesc.setTranslation(description);
                        desc.addPersistentLocalizedString(localizedDesc);
                    }                    
                }
                
                private int doesLocaleExists(Locale locale, List<PersistentLocalizedString> list) {
                    for (int i=0; i <= list.size()-1; i++) {
                        if (compareLocale(locale, list.get(i).getPersistentLocale())) {
                            return i;
                        }
                    }                   
                    return -1;
                }
                
                private boolean compareLocale(Locale l1, PersistentLocale l2) {
                    if ((l1.getCountry().equals(l2.getCountry())) &&
                        (l1.getLanguage().equals(l2.getLanguage()))) {
                        return true;        
                    }               
                    return false;
                }
                
		public void setDefaultCategoryDescription(String name, String description) {
                    mCategory.setTitle(new PersistentString(name));
                    mCategory.setDescription(new PersistentString(description));
		}
				
		public LocalizedAudioLabel getCategoryName(Locale languageCode) {
                    return new LocalizedAudioLabel(mCategory.getTitle().toString(), mCategory.getDescription().toString(), null);
		}
		
		public Category getParent() {
                    PersistentCategory parent = mCategory.getPersistentParentCategory();
                    if (parent == null) {
                        return null;
                    }
                    return new Category(mCategory.getPersistentParentCategory());
		}
		
                public void addChild(Category childCategory) {
                    mCategory.addPersistentChildCategory(childCategory.getPersistentObject());
                }                
                
		public List<Category> getChildren() {
		    List<Category> children = new LinkedList<Category>();
                    for (PersistentCategory child : mCategory.getPersistentChildCategoryList()) {
                        children.addAll(getAllChildren(child, new LinkedList<Category>()));
                    }
                    return children;
		}
                
                private List<Category> getAllChildren(PersistentCategory category, List<Category> children) {
                    List<PersistentCategory> categories = category.getPersistentChildCategoryList();
                    if (categories.size() != 0) {
                        for (PersistentCategory c : categories) {
                            getAllChildren(c, children);
                        }
                    }
                    children.add(new Category(category));
                    return children;
                }
		
		public boolean hasChildren() {
                    List<PersistentCategory> children = mCategory.getPersistentChildCategoryList();
                    return children != null && !children.isEmpty();
		}
                
                public List<AudioItem> getAudioItemList() {
                    List<AudioItem> audioItems = new LinkedList<AudioItem>();
                    for (PersistentAudioItem item : mCategory.getPersistentAudioItemList()) {
                        audioItems.add(new AudioItem(item));
                    }
                    return audioItems;
                }
                
                public Integer getId() {
                    return mCategory.getId();
                }
        
                public Category commit() {
                    mCategory = mCategory.<PersistentCategory>commit();
                    return this;
                }
                
                public void destroy() {
                    mCategory.destroy();
                }
                
                public Category refresh() {
                    mCategory = mCategory.<PersistentCategory>refresh();
                    return this;
                }
                

                public String getUuid() {
                    return mCategory.getUuid();
                }
        
                public void setUuid(String uuid) {
                    this.mCategory.setUuid(uuid);
                }
                
                public Category getFromDatabase() {
                    PersistentCategory category = PersistentCategory.getFromDatabase(getId());
                    if (category == null) {
                        return null;
                    }
                    return new Category(category);
                }
    }

}
