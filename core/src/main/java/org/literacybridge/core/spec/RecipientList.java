package org.literacybridge.core.spec;

import org.apache.commons.lang3.StringUtils;
import org.literacybridge.core.utils.DelayeredHierarchicalList;
import org.literacybridge.core.utils.HierarchyInfo;
import org.literacybridge.core.utils.IHierarchicalRecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RecipientList extends DelayeredHierarchicalList<RecipientList.RecipientAdapter> {
    // Note that the view presented by the parent class may omit some of these. use getNameOfLevel(ix)
    private final static String[] NAMES = {"Country", "Region", "District", "Community", "Group", "Agent", "Language"};
    private final static String[] PLURALS = {"Countries", "Regions", "Districts", "Communities", "Groups", "Agents", "Languages"};
    private final static String[] REQUIRED_ALWAYS = {"Community"};
    public final static String NON_FILE_CHARS = "[\\\\/~;:*?'\"]";

    private final Map<String, Integer> numTbsCache = new HashMap<>();

    // The 'deployments' column was added, and provides a different way than 'component' to determine if a recipient
    // should receive a given deployment. Hence, the program spec needs to know if the recipients list even has
    // a deployments column.
    private boolean hasDeploymentsColumn = false;
    public boolean hasDeploymentsColumn() {
        return this.hasDeploymentsColumn;
    }

    private final LanguageLabelProvider languageLabelProvider;

    RecipientList(LanguageLabelProvider languageLabelProvider) {
        super(new HierarchyInfo<>(NAMES, PLURALS, REQUIRED_ALWAYS));
        this.languageLabelProvider = languageLabelProvider;
    }

    public String getSingular(int level) {
        return "a " + getNameOfLevel(level);
    }

    public Recipient getRecipient(List<String> path) {
        if (path == null || path.size() == 0) {
            return null;
        }
        return getItemAtPath(path);
    }

    public List<RecipientAdapter> getRecipientsAtPath(List<String> path) {
        return new ArrayList<>(getItemsAtPath(path));
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

    public RecipientAdapter getRecipient(String recipientid) {
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

    public List<Integer> getAdapterIndicesForValues(List<String> names) {
        List<String> namesList = Arrays.asList(NAMES);
        return names.stream().map(namesList::indexOf).collect(Collectors.toList());
    }

    public void setFoundColumns(Set<String> columnsInRecips) {
        this.hasDeploymentsColumn = columnsInRecips != null && columnsInRecips.contains(Recipient.columns.deployments.name());
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
            case 5: return agent;
            case 6:
                 if (languageLabelProvider == null)
                     return languagecode;
                 String label = languageLabelProvider.getLanguageLabel(languagecode);
                 if (StringUtils.isBlank(label))
                     return languagecode;
                 return String.format("%s (%s)", label, languagecode);
            }
            return null;
        }

        public String getValue(String level) {
            // TODO: Cache these values.
            int ix = Arrays.asList(NAMES).indexOf(level);
            return getValue(ix);
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

        public String getNameForFile() {
            String fname = String.join("-", getPath());
            return fname.replaceAll(NON_FILE_CHARS, " ");
        }

        /**
         * This provides a list of the geo-political properties that are
         * sufficient to distinguish any recipient in this program.
         * @return A list of the important names.
         */
        public List<String> getDistinguishingNames() {
            List<String> result = new ArrayList<>();
            for (int i=0; i<NAMES.length; ++i) {
                if (!isOmitted(i)) {
                    result.add(NAMES[i]);
                }
            }
            return result;
        }
    }

}
