package org.literacybridge.acm.gui.Assistant;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.Map;

/**
 * The abstract base class for Assistant pages. Provides access to Assistant properties.
 *
 * Derived classes must implement 'OnPageEnterer'.
 */
public abstract class AssistantPage<Context> extends JPanel {
    // This is because the SeaGlass L&F makes combo boxes too small
    // for the content. To work around, we measure the size of an
    // empty ComboBox, and add a 5px buffer.in a page measure the size of the longest string,
    // and add a 5px buffer.
    /**
     * This is because the SeaGlass L&F makes combo boxes too small for the content. To work
     * around, we measure the size of an empty ComboBox, and add a 5px buffer.
     *
     * In each page, for each combo box, measure the size of the longest string, and set the
     * combo preferred size (and, optionally, min or max size) to the string + baseComboWidth
     */
    public static final int baseComboWidth = new JComboBox().getPreferredSize().width + 5;
    public static void setComboWidth(JComboBox cb, String string) {
        int textWidth = new JLabel("").getFontMetrics(cb.getFont()).stringWidth(string);
        Dimension size = cb.getPreferredSize();
        size.width = baseComboWidth + textWidth;
        cb.setPreferredSize(size);
    }

    protected static final LineBorder redBorder = new LineBorder(Color.RED, 1, true);
    protected static final LineBorder blankBorder = new LineBorder(new Color(0, 0, 0, 0), 1, true);

    private Assistant.PageHelper<Context> pageHelper;
    private boolean isComplete = false;

    public AssistantPage(Assistant.PageHelper pageHelper) {
        super();
        this.pageHelper = pageHelper;
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(pageHelper.getAssistant().factory.background);
    }

    protected abstract void onPageEntered(boolean progressing);
    protected void onPageLeaving(boolean progressing) {}

    protected String getTitle() { return ""; }

    /**
     * Sets the completed state of the page. The Assistant uses this state to enable/disable
     * Next / Finish buttons.
     */
    protected void setComplete() { setComplete(true); }
    protected void setComplete(boolean isComplete) {
        this.isComplete = isComplete;
        pageHelper.onComplete(isComplete);
    }
    boolean isComplete() { return isComplete; }

    protected Context getContext() { return pageHelper.getContext(); }
}
