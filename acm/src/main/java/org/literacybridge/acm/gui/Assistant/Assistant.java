package org.literacybridge.acm.gui.Assistant;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
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
        private List<Function<PageHelper, AssistantPage>> pageCtors;
        private boolean lastPageIsSummary = false;
        private Frame owner = null;
        private boolean modal = true;
        Color background = new Color(0xe0f7ff);

        public Assistant<Context> create() {
            return new Assistant<>(this);
        }

        public Factory<Context> withTitle(String title) { this.title = title; return this; }
        public Factory<Context> withSize(Dimension size) { this.size = size; return this; }
        public Factory<Context> withContext(Context context) { this.context = context; return this; }
        @SafeVarargs
        public final Factory<Context> withPageCtors(Function<PageHelper, AssistantPage>... pages) { this.pageCtors = Arrays.asList(pages); return this; }
        public Factory<Context> withOwner(Frame owner) { this.owner = owner; return this; }
        public Factory<Context> asModal(boolean modal) { this.modal = modal; return this; }
        public Factory<Context> asModal() { this.modal = true; return this; }
        public Factory<Context> withBackground(Color background) { this.background = background; return this; }
        public Factory<Context> withLastPageSummary() { this.lastPageIsSummary = true; return this; }
    }


    public interface PageHelper<Context> {
        void onComplete(boolean isComplete);
        Assistant getAssistant();
        Context getContext();
    }

    public boolean finished = false;

    Factory factory;
    private int maxPage;
    private int currentPage = -1;
    private List<AssistantPage> pages = new ArrayList<>();

    private Context context;

    private final JLabel stepLabel;

    private JButton prevButton;
    private JButton nextButton;
    private JButton finishButton;
    private JButton closeButton;
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

        this.maxPage = factory.pageCtors.size()-1;

        context = factory.context;

        navigate(0);

        setMinimumSize(factory.size==null ? new Dimension(800,600) : factory.size);
    }

    /**
     * Assistant navigation buttons, on the bottom of the page.
     * @return A box of windows.
     */
    private Box createNavigationButtons() {
        Box hbox = Box.createHorizontalBox();

        prevButton = new JButton("<< Prev");
        nextButton = new JButton("Next >>");
        finishButton = new JButton("Finish");
        closeButton = new JButton("Close");
        closeButton.setVisible(false);
        cancelButton = new JButton("Cancel");

        prevButton.addActionListener((e)->onPrevButton());
        nextButton.addActionListener((e)->onNextButton());
        finishButton.addActionListener((e)->onFinishButton());
        closeButton.addActionListener((e)->onCloseButton());
        cancelButton.addActionListener((e)->onCancelButton());

        hbox.add(Box.createHorizontalGlue());
        hbox.add(prevButton);
        hbox.add(Box.createHorizontalStrut(15));
        hbox.add(nextButton);
        hbox.add(finishButton);
        hbox.add(closeButton);
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
    }

    /**
     *  Let the client know that the user is done.
     */
    private void onFinishButton() {
        AssistantPage page = getPage(currentPage);
        if (factory.lastPageIsSummary)
            navigate(currentPage + 1);
        else
            setVisible(false);
    }
    
    private void onCloseButton() {
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

        List<Function<PageHelper, AssistantPage>> factories = factory.pageCtors;
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

    private boolean isFinishPage(int page) {
        return page == (factory.lastPageIsSummary ? maxPage-1 : maxPage);
    }

    private boolean isSummaryPage(int page) {
        return page == (factory.lastPageIsSummary ? maxPage : -2);
    }

    private boolean isFinalPage(int page) {
        return page == maxPage;
    }

    private void setNavButtonState() {
        prevButton.setEnabled(currentPage > 0);
        if (isFinishPage(currentPage)) {
            finishButton.setVisible(true);
            nextButton.setVisible(false);
        } else if (isSummaryPage(currentPage)) {
            // Make the next-to-last button be the "Close" button.
            finishButton.setVisible(false);
            nextButton.setVisible(false);
            closeButton.setVisible(true);

            // All this page can do is close, no back, and the job's done, so no cancel.
            prevButton.setEnabled(false);
            cancelButton.setEnabled(false);
            closeButton.setEnabled(pages.get(currentPage).isComplete());
        } else {
            finishButton.setVisible(false);
            nextButton.setVisible(true);
            nextButton.setEnabled(pages.get(currentPage).isComplete());
        }
    }

    private void navigate(int newPage) {
        if (currentPage == newPage) return;
        boolean progressing = newPage > currentPage;
        if (currentPage >= 0) {
            AssistantPage page = getPage(currentPage);
            page.onPageLeaving(progressing);
            this.remove(page);
        }
        currentPage = newPage;
        AssistantPage page = getPage(currentPage);
        page.onPageEntered(progressing);
        this.add(page, BorderLayout.CENTER);

        String title = page.getTitle();
        title = (title == null || title.length() == 0) ? "" : ": "+title;
        stepLabel.setText(String.format("Step %d of %d%s", currentPage+1, maxPage+1, title));

        repaint();
        revalidate();

        setNavButtonState();
    }


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
