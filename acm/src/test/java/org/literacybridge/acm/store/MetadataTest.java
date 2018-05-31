package org.literacybridge.acm.store;

import org.junit.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.literacybridge.acm.store.MetadataSpecification.*;

@SuppressWarnings("unchecked")
public class MetadataTest {

    @Test
    public void testSize() {
        Metadata md = new Metadata();
        assertEquals("Metadata should be empty", md.size(), 0);

        String aTitle = "title";
        md.put(DC_TITLE.getName(), aTitle);
        assertEquals("Metadata should have one entry", md.size(), 1);

        md.put(DC_TITLE.getName(), aTitle);
        assertEquals("Metadata should still have one entry", md.size(), 1);

        md.put(DC_PUBLISHER.getName(), aTitle);
        assertEquals("Metadata should have two entries", md.size(), 2);

        md.clear();
        assertEquals("Metadata should again be empty.", md.size(), 0);
    }

    @Test
    public void testGet() {
        Metadata md = new Metadata();

        String aTitle = "title";
        md.put(DC_TITLE.getName(), aTitle);
        assertEquals("Should get back same item by name", aTitle, md.get(DC_TITLE.getName()));
        assertEquals("Should get back same item by ID", aTitle, md.get(DC_TITLE));
        assertEquals("Should get back same item the long way", aTitle, md.getMetadataValue(DC_TITLE).getValue());
    }

    @Test
    public void testPut() {
        Metadata md = new Metadata();

        String aTitle = "title";
        md.putMetadataField(DC_TITLE, new MetadataValue(aTitle));
        assertEquals("Should get back the same item", aTitle, md.get(DC_TITLE));

        md.clear();
        md.put(DC_TITLE, aTitle);
        assertEquals("Should get back the same item", aTitle, md.get(DC_TITLE));

        md.clear();
        md.put(DC_TITLE.getName(), aTitle);
        assertEquals("Should get back the same item", aTitle, md.get(DC_TITLE));

        md.clear();
        md.put(DC_TITLE.getName(), new MetadataValue(aTitle));
        assertEquals("Should get back the same item", aTitle, md.get(DC_TITLE));
    }

    @Test
    public void testInteger() {
        Metadata md = new Metadata();

        Integer anInt = 42;
        md.putMetadataField(LB_STATUS, new MetadataValue(anInt));
        assertEquals("Should get back same integer", anInt, new Integer(md.get(LB_STATUS)));
        assertEquals("Should get back same integer", anInt, md.getMetadataValue(LB_STATUS).getValue());

        md.clear();
        md.put(LB_STATUS, anInt);
        assertEquals("Should get back same integer", anInt, new Integer(md.get(LB_STATUS)));
        assertEquals("Should get back same integer", anInt, md.getMetadataValue(LB_STATUS).getValue());

        md.clear();
        md.put(LB_STATUS.getName(), anInt);
        assertEquals("Should get back same integer", anInt, new Integer(md.get(LB_STATUS)));
        assertEquals("Should get back same integer", anInt, md.getMetadataValue(LB_STATUS).getValue());

        md.clear();
        md.put(LB_STATUS, new MetadataValue(anInt));
        assertEquals("Should get back same integer", anInt, new Integer(md.get(LB_STATUS)));
        assertEquals("Should get back same integer", anInt, md.getMetadataValue(LB_STATUS).getValue());
    }

    @Test
    public void testRFC3066() {
        Metadata md = new Metadata();

        String codeStr = "en";
        RFC3066LanguageCode aCode = new RFC3066LanguageCode(codeStr);
        md.putMetadataField(DC_LANGUAGE, new MetadataValue(aCode));
        assertEquals("Should get back same code", aCode, md.getMetadataValue(DC_LANGUAGE).getValue());
        assertEquals("Should get back same code", aCode, new RFC3066LanguageCode(md.get(DC_LANGUAGE)));
        assertEquals("Should get back same code", aCode, new RFC3066LanguageCode(md.get(DC_LANGUAGE.getName())));

        md.clear();
        md.put(DC_LANGUAGE, aCode);
        assertEquals("Should get back same code", aCode, md.getMetadataValue(DC_LANGUAGE).getValue());

        md.clear();
        md.put(DC_LANGUAGE.getName(), aCode);
        assertEquals("Should get back same code", aCode, md.getMetadataValue(DC_LANGUAGE).getValue());

        md.clear();
        md.put(DC_LANGUAGE.getName(), new MetadataValue(aCode));
        assertEquals("Should get back same code", aCode, md.getMetadataValue(DC_LANGUAGE).getValue());
    }

    @Test
    public void testEnumeration() {
        Metadata md = new Metadata();

        md.put(DC_TITLE, "title");
        md.put(DC_IDENTIFIER, "id");
        md.put(DC_RELATION, "relation");

        int loops = 0;
        Set<String> seenFields = new HashSet<>();
        for (Map.Entry<MetadataField<?>, MetadataValue<?>> e : md.entrySet()) {
            loops++;
            MetadataField<?> field = e.getKey();
            seenFields.add(field.getName());
        }
        assertEquals("Should have been through loop three times", 3, loops);
        assertEquals("Should have seen three distinct fields", 3, seenFields.size());

        md.put(DC_TITLE, "title");
        md.put(DC_IDENTIFIER, "id");
        md.put(DC_RELATION, "relation");
        loops = 0;
        seenFields.clear();
        for (Map.Entry<MetadataField<?>, MetadataValue<?>> e : md.entrySet()) {
            loops++;
            MetadataField<?> field = e.getKey();
            seenFields.add(field.getName());
        }
        assertEquals("Should still have been through loop three times", 3, loops);
        assertEquals("Should still have seen three distinct fields", 3, seenFields.size());
    }

    @Test
    public void testAddValuesFrom() {
        Metadata md = new Metadata();

        md.put(DC_TITLE, "title");
        md.put(DC_IDENTIFIER, "id");
        md.put(DC_RELATION, "relation");

        Metadata md2 = new Metadata();
        md2.addValuesFrom(md);
        assertEquals("Should have added three fields", 3, md2.size());
        assertEquals("Title should be 'title'", "title", md2.get(DC_TITLE));
        assertEquals("Identifier should be 'id'", "id", md2.get(DC_IDENTIFIER));
        assertEquals("Relation should be 'relation'", "relation", md2.get(DC_RELATION));

        md2.clear();
        md2.put(DC_TITLE, "not title");
        md2.put(DC_IDENTIFIER, "not id");
        md2.put(DC_RELATION, "not relation");
        md2.addValuesFrom(md);
        assertEquals("Title should be 'title'", "title", md2.get(DC_TITLE));
        assertEquals("Identifier should be 'id'", "id", md2.get(DC_IDENTIFIER));
        assertEquals("Relation should be 'relation'", "relation", md2.get(DC_RELATION));

        md2.clear();
        md2.put(DC_TITLE, "not title");
        md2.put(DC_IDENTIFIER, "not id");
        md2.put(DC_RELATION, "not relation");
        md2.addValuesFrom(md, false);
        assertEquals("Title should be 'not-title'", "not title", md2.get(DC_TITLE));
        assertEquals("Identifier should be 'not-id'", "not id", md2.get(DC_IDENTIFIER));
        assertEquals("Relation should be 'not-relation'", "not relation", md2.get(DC_RELATION));
    }
}
