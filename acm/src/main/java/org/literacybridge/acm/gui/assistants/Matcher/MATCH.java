package org.literacybridge.acm.gui.assistants.Matcher;

public enum MATCH {
    NONE,

    EXACT,
    FUZZY,
    TOKEN,

    LEFT_ONLY,
    RIGHT_ONLY;

    public boolean isMatch() { return ordinal() >= EXACT.ordinal() && ordinal() <= TOKEN.ordinal();}
    boolean isSingle() { return ordinal() >= MATCH.LEFT_ONLY.ordinal();}

    public static MATCH[] matches = new MATCH[] {EXACT, FUZZY, TOKEN, LEFT_ONLY, RIGHT_ONLY};
    public static String[] matchNames = new String[] {EXACT.toString(), FUZZY.toString(), TOKEN.toString(), LEFT_ONLY.toString(), RIGHT_ONLY.toString()};
}
