package org.literacybridge.acm.gui.assistants.Matcher;

public enum MATCH {
    NONE,

    EXACT,
    FUZZY,
    TOKEN,
    MANUAL,

    LEFT_ONLY,
    RIGHT_ONLY;

    public boolean isMatch() { return ordinal() >= EXACT.ordinal() && ordinal() <= MANUAL.ordinal();}
    boolean isSingle() { return ordinal() >= MATCH.LEFT_ONLY.ordinal();}
}
