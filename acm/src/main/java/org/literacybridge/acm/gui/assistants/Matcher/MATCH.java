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
    public boolean isNonStrictMatch() { return ordinal() >= FUZZY.ordinal() && ordinal() <= TOKEN.ordinal();}
    public boolean isUnmatched() { return ordinal() >= MATCH.LEFT_ONLY.ordinal();}

    public static final int PERFECT = 100;
}
