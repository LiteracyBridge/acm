package core.utils;

import org.junit.Test;
import org.literacybridge.core.utils.DelayeredHierarchicalList;
import org.literacybridge.core.utils.HierarchyInfo;
import org.literacybridge.core.utils.IHierarchicalRecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;

public class DelayeredHierarchicalListTest {
    class RI extends HashMap<String, String> {}

    TestList buildList() {
        TestList result = new TestList();
        Map<String, String> myMap = new RI() {{
            put("a", "b");
            put("c", "d");
        }};
        result.add(new TestRec("US", "West Coast", "WA", "Seattle", "BLM", "", "Seattle BLM"));
        result.add(new TestRec("US", "West Coast", "WA", "Chimacum", "Chickens", "H", "Chicken H"));
        result.add(new TestRec("US", "West Coast", "WA", "Chimacum", "Chickens", "I", "Chicken I"));
        result.getLevels(); // inits the 'omitted' array.
        return result;
    }

    List<String> makePath(TestList tl, String... components) {
        List<String> result = new ArrayList<>();
        int n = Math.min(components.length, NAMES.length);
        for (int ix=0; ix<n; ix++) {
            if (!tl.isOmitted(ix)) {
                result.add(components[ix]);
            }
        }
        return result;
    }

    @Test public void testBuildList() {
        TestList tl = buildList();
    }

    @Test public void testMakePath() {
        TestList tl = buildList();
        List<String> path = makePath(tl, "US", "West Coast", "WA", "Seattle", "BLM", "");
        assertEquals("Sparse list has only two levels.", 2, path.size());
    }

    @Test public void testGetItemAtPath() {
        TestList tl = buildList();
        TestRec tr = tl.getItemAtPath(makePath(tl, "US", "West Coast", "WA", "Seattle", "BLM", ""));
        assertEquals("getItemAtPath", "Seattle BLM", tr.whoami);
    }

    @Test public void testGetItemAtBlankPath() {
        TestList tl = buildList();
        TestRec tr = tl.getItemAtPath(makePath(tl, "", "", "", "", "", ""));
        assertEquals("blank path should return null", null, tr);
    }

    @Test public void testGetChildrenOfPath() {
        TestList tl = buildList();
        Collection<String> tc = tl.getChildrenOfPath(makePath(tl, "US", "West Coast", "WA"));
        assertEquals("getChildrenOfPath('WA')", 2, tc.size());
    }

    // Note that the view presented by the parent class may omit some of these. use getNameOfLevel(ix)
    private final static String[] NAMES = {"Country", "Region", "District", "Community", "Group", "Agent"};
    private final static String[] PLURALS = {"Countries", "Regions", "Districts", "Communities", "Groups", "Agents"};
    private final static String[] REQUIRED_ALWAYS = {"Community"};

    static class TestRec implements IHierarchicalRecord {
        final public String country, region, district, communityname, groupname, agent;
        final public String whoami;

        TestRec(String country,
                String region,
                String district,
                String communityname,
                String groupname,
                String agent,
                String whoami) {
            this.country = country;
            this.region = region;
            this.district = district;
            this.communityname = communityname;
            this.groupname = groupname;
            this.agent = agent;
            this.whoami = whoami;
        }

        public TestRec(Map<String, String> values) {
            this.country = values.get("country");
            this.region = values.get("region");
            this.district = values.get("district");
            this.communityname = values.get("communityname");
            this.groupname = values.get("groupname");
            this.agent = values.get("agent");
            this.whoami = values.getOrDefault("whoami", "I don't know!");
        }

        @Override
        public String getValue(int level) {
            switch (level) {
                case 0: return country;
                case 1: return region;
                case 2: return district;
                case 3: return communityname;
                case 4: return groupname;
                case 5: return agent;
            }
            return null;
        }

        @Override
        public String toString() {
            return country + " / " + region + " / " + district + " / " + communityname + " / " + groupname + " / " + agent + ": " + whoami;
        }
    }

    static class TestList extends DelayeredHierarchicalList<TestRec> {

        public TestList() {
            super(new HierarchyInfo<>(NAMES, PLURALS, REQUIRED_ALWAYS));
        }
    }

}
