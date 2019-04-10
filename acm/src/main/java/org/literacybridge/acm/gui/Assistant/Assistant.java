package org.literacybridge.acm.gui.Assistant;

import org.literacybridge.acm.gui.Application;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class Assistant<Context> extends JDialog {
    /**
     * This serves as the options class for the Assistant class. Create a new Factory(),
     * set the desired options, and then create the Assistant.
     */
    public static class Factory<Context> {
        private String title = "";
        private Dimension size;
        private Context context;
        private List<Function<PageHelper, AssistantPage>> pageFactories;
        private Frame owner = Application.getApplication();
        private boolean modal = true;
        Color background = new Color(0xe0f7ff);

        public Assistant<Context> create() {
            return new Assistant<>(this);
        }

        public Factory<Context> withTitle(String title) { this.title = title; return this; }
        public Factory<Context> withSize(Dimension size) { this.size = size; return this; }
        public Factory<Context> withContext(Context context) { this.context = context; return this; }
        @SafeVarargs
        public final Factory<Context> withPageFactories(Function<PageHelper, AssistantPage>... pages) { this.pageFactories = Arrays.asList(pages); return this; }
        public Factory<Context> withOwner(Frame owner) { this.owner = owner; return this; }
        public Factory<Context> asModal(boolean modal) { this.modal = modal; return this; }
        public Factory<Context> asModal() { this.modal = true; return this; }
        public Factory<Context> withBackground(Color background) { this.background = background; return this; }
    }


    public interface PageHelper<Context> {
        void onComplete(boolean isComplete);
        Assistant getAssistant();
        Context getContext();
    }

    private static final String NEXT_TEXT = "Next >>";
    private static final String FINISH_TEXT = "Finish";
    private static final String CLOSE_TEXT = "Close";

    public boolean finished = false;

    Factory factory;
    private int maxPage;
    private int currentPage = -1;
    private List<AssistantPage> pages = new ArrayList<>();

    private Context context;

    private final JLabel stepLabel;

    private JButton prevButton;
    private JButton nextButton;
    private JButton cancelButton;

    private Assistant(Factory<Context> factory) {
        super(factory.owner, factory.title, factory.modal);
        setLayout(new BorderLayout());

        this.factory = factory;

        Box stepsBox = Box.createHorizontalBox();
        stepLabel = new JLabel(" ");
        stepsBox.add(stepLabel);
        add(stepsBox, BorderLayout.NORTH);
        stepsBox.setBorder(new EmptyBorder(2,10,4,2));

        Box buttonsBox = createNavigationButtons();
        add(buttonsBox, BorderLayout.SOUTH);
        buttonsBox.setBorder(new EmptyBorder(4,10,4,8));

        this.maxPage = factory.pageFactories.size()-1;

        context = factory.context;

        navigate(0);

        setMinimumSize(factory.size==null ? new Dimension(800,600) : factory.size);
        setLocation(factory.owner.getX()+20, factory.owner.getY()+20);
    }

    /**
     * Assistant navigation buttons, on the bottom of the page.
     * @return A box of windows.
     */
    private Box createNavigationButtons() {
        Box hbox = Box.createHorizontalBox();

        prevButton = new JButton("<< Prev");
        nextButton = new JButton("Next >>");
        cancelButton = new JButton("Cancel");

        prevButton.addActionListener((e)->onPrevButton());
        nextButton.addActionListener((e)->onNextButton());
        cancelButton.addActionListener((e)->onCancelButton());

        hbox.add(Box.createHorizontalGlue());
        hbox.add(prevButton);
        hbox.add(Box.createHorizontalStrut(15));
        hbox.add(nextButton);
        hbox.add(Box.createHorizontalStrut(15));
        hbox.add(cancelButton);

        return hbox;
    }

    /**
     * Go back if possible. This button should not be enabled if there is no previous page.
     */
    private void onPrevButton() {
        if (currentPage > 0)
            navigate(currentPage - 1);
    }

    /**
     * Go forward if possible. If there is no next page, this button should not be visible,
     * but rather the Finish button should be visible.
     */
    private void onNextButton() {
        if (currentPage < maxPage)
            navigate(currentPage + 1);
        else
            setVisible(false);
    }


    /**
     * Let the client know that the user is cancelling the action.
     */
    private void onCancelButton() {
        setVisible(false);
    }

    private AssistantPage getPage(int pageNumber) {
        if (pageNumber < 0 || pageNumber > maxPage) {
            throw new IllegalStateException("Requested nonexistant page.");
        }
        if (pageNumber < pages.size()) {
            return pages.get(pageNumber);
        }
        if (pageNumber > pages.size()) {
            throw new IllegalStateException("Accessing page out of order");
        }

        List<Function<PageHelper, AssistantPage>> factories = factory.pageFactories;
        Function<PageHelper, AssistantPage> ctor = factories.get(pageNumber);
        AssistantPage newPage = ctor.apply(pageHelper);

        pages.add(newPage);
        return pages.get(pageNumber);
    }
    private PageHelper<Context> pageHelper = new PageHelper<Context>() {
        @Override
        public void onComplete(boolean isComplete) {
            setNavButtonState();
        }
        @Override
        public Assistant getAssistant() {
            return Assistant.this;
        }
        @Override
        public Context getContext() {
            return Assistant.this.context;
        }
    };

    /**
     * Is the current page a "Finish" page? A finish page is the last page of the Assistant,
     * provided that it is not a "summary" page. The idea is that when the user clicks the
     * button to advance from this page, the Assistane will finish whatever it's doing (or,
     * the Assistant's invoker will).
     *
     * Note that the last page of an Assistant will be either a finish page or a summary page.
     *
     * @return true if this is a finish page, false otherwise.
     */
    private boolean isFinishPage() {
        return currentPage == maxPage && !getPage(currentPage).isSummaryPage() ;
    }

    /**
     * Is the current page a "Summary" page? A summary page is the last page of the Assistant
     * that self-identifies as "isSummary()".
     *
     * Note that the last page of an Assistant will be either a finish page or a summary page.
     *
     * @return true if this is a summary page, false otherwise.
     */
    private boolean isSummaryPage() {
        return currentPage == maxPage && getPage(currentPage).isSummaryPage() ;
    }

    /**
     * Has the current page declared that it is in a "complete" state? When the page is
     * "complete", the next button will be enabled (whatever its title).
     *
     * Note that the complete state is ephemeral, and the page can enter and leave the
     * state multiple times.
     *
     * @return true if the current page is currently complete, false otherwise.
     */
    private boolean isPageComplete() {
        return getPage(currentPage).isComplete();
    }

    /**
     * Sets the enabled state and button text of the navigation buttons.
     *
     * Once a summary page has been entered, it can only be closed. (And not cancelled,
     * because presumably the action has already been performed.)
     *
     * The prev button is active on all pages except the first page and a summary page.
     *
     * The next button is active when there IS a next, and the page is complete.
     */
    private void setNavButtonState() {
        prevButton.setEnabled(currentPage > 0 && !isSummaryPage());
        nextButton.setEnabled(isPageComplete());
        cancelButton.setEnabled(!isSummaryPage());

        if (isSummaryPage()) {
            nextButton.setText(CLOSE_TEXT);
        } else if (isFinishPage()) {
            nextButton.setText(FINISH_TEXT);
        } else {
            nextButton.setText(NEXT_TEXT);
        }
    }

    /**
     * Navigate to the given page number.
     * @param newPage to go to.
     */
    private void navigate(int newPage) {
        if (currentPage == newPage) return;
        boolean progressing = newPage > currentPage;
        if (currentPage >= 0) {
            AssistantPage page = getPage(currentPage);
            page.onPageLeaving(progressing);
            page.setVisible(false);
            this.remove(page);
        }
        currentPage = newPage;
        AssistantPage page = getPage(currentPage);
        this.add(page, BorderLayout.CENTER);

        String title = page.getTitle();
        title = (title == null || title.length() == 0) ? "" : ": "+title;
        stepLabel.setText(String.format("Step %d of %d%s", currentPage+1, maxPage+1, title));

        setNavButtonState();
        // Let the UI settle, then inform the new page that it has been entered.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                page.onPageEntered(progressing);
                page.setVisible(true);
            }
        });
    }

    /**
     * Because Sea Glass L&F is buggy.
     */
    Color backgroundColor = Color.white;
    @Override
    public void setBackground(Color bgColor) {
        // Workaround for weird bug in seaglass look&feel that causes a
        // java.awt.IllegalComponentStateException when e.g. a combo box
        // in this dialog is clicked on
        if (bgColor.getAlpha() == 0) {
            super.setBackground(backgroundColor);
        } else {
            super.setBackground(bgColor);
            backgroundColor = bgColor;
        }
    }

}
