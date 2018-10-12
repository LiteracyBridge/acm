package org.literacybridge.core.spec;

import org.apache.commons.lang3.StringUtils;
import org.literacybridge.core.utils.DelayeredHierarchicalList;
import org.literacybridge.core.utils.HierarchyInfo;
import org.literacybridge.core.utils.IHierarchicalRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipientList extends DelayeredHierarchicalList<RecipientList.RecipientAdapter> {
    // Note that the view presented by the parent class may omit some of these. use getNameOfLevel(ix)
    private final static String[] NAMES = {"Country", "Region", "District", "Community", "Group", "Support Entity / Agent"};
    private final static String[] PLURALS = {"Countries", "Regions", "Districts", "Communities", "Groups", "Support Entities / Agents"};

    private final Map<String, Integer> numTbsCache = new HashMap<>();

    RecipientList() {
        super(new HierarchyInfo<>(NAMES, PLURALS));
    }

    public String getSingular(int level) {
        return "a " + getNameOfLevel(level);
    }

    public Recipient getRecipient(List<String> path) {
        return getItemAtPath(path);
    }

    /**
     * How many TBs are in all the Recipients under a given path?
     * @param path for which to count TBs.
     * @return number of TBs.
     */
    public int getNumTbs(List<String> path) {
        Integer cachedNum = numTbsCache.get(path.toString());
        if (cachedNum != null) return cachedNum;
        List<RecipientAdapter> recips = getItemsAtPath(path);
        int n = 0;
        for (RecipientAdapter r : recips) {
            n += r.numtbs;
        }
        numTbsCache.put(path.toString(), n);
        return n;
    }

    private RecipientAdapter getRecipient(String recipientid) {
        for (RecipientAdapter recip : this) {
            if (recip.recipientid.equals(recipientid))
                return recip;
        }
        return null;
    }

    public List<String> getPath(String recipientid) {
        RecipientAdapter recip = getRecipient(recipientid);
        if (recip == null) return new ArrayList<>();
        return recip.getPath();
    }

    /**
     * Helper to create a RecipientAdapter and add to the list.
     * @param record describing the Recipient.
     * @return true if added successfully.
     */
    public boolean add(Map<String, String> record) {
        RecipientAdapter r = new RecipientAdapter(record);
        return add(r);
    }

    /**
     * Wrapper class for Recipient. Allows an IHierarchicalList to access members by
     * level number.
     *
     * Also provides a "delayered" getName(), which omits irrelevant levels from the name.
     */
    public class RecipientAdapter extends Recipient implements IHierarchicalRecord {
        @Override
        public String getValue(int level) {
            switch (level) {
            case 0: return country;
            case 1: return region;
            case 2: return district;
            case 3: return communityname;
            case 4: return groupname;
            case 5: return supportentity;
            }
            return null;
        }

        public List<String> getPath() {
            return RecipientList.this.getPathForItem(this);
        }

        RecipientAdapter(Map<String, String> properties) {
            super(properties);
        }

        public String getName() {
            List<String> path = RecipientList.this.deLayerPath(getPath());
            StringBuilder result = new StringBuilder();
            String joiner = "";
            for (String p : path) {
                if (StringUtils.isNotEmpty(p)) {
                    result.append(joiner).append(p);
                    joiner = " / ";
                }
            }
            return result.toString();
        }
    }

}
