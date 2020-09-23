package org.literacybridge.acm.gui.assistants.GreetingsImport;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Assistant.Assistant.PageHelper;
import org.literacybridge.acm.gui.assistants.common.AcmAssistantPage;
import org.literacybridge.core.spec.ProgramSpec;
import org.literacybridge.core.spec.Recipient;
import org.literacybridge.core.spec.RecipientList.RecipientAdapter;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;
import java.util.Map;
import java.util.function.Predicate;

public class WelcomePage extends AcmAssistantPage<GreetingsImportContext> {
    private final RecipientFilter recipientFilter;

    private final JTable recipientTable;
    private final RecipientModel recipientModel;

    WelcomePage(PageHelper<GreetingsImportContext> listener) {
        super(listener);
        getProgramInformation();

        setLayout(new GridBagLayout());

        GridBagConstraints gbc = getGBC();

        JLabel welcome = new JLabel("<html>"
            + "<span style='font-size:2.5em'>Welcome to the Custom Greetings Assistant.</span>"
            + "<br/><br/><p>This Assistant will guide you through importing custom greetings for recipients. Yere are the steps:</p>"
            + "<ol>"
            + "<li> Review the Recipients that need custom greetings, in the list below.</li>"
            + "<li> Choose the files and folders containing the custom greetings.</li>"
            + "<li> The Assistant will automatically match as many greetings as it can. You will "
            + "have an opportunity to match remaining files or \"unmatch\" files as needed.</li>"
            + "<li> You review and approve the final file matches.</li>"
            + "<li> The audio files are copied into the project.</li>"
            + "</ol>"
            + "<br/>Click \"Next\" to get started. "

            + "</html>");
        add(welcome, gbc);

        gbc.insets.bottom = 0;
        JCheckBox onlyMissing = new JCheckBox("Only show Recipients without custom greetings.", false);
        add(onlyMissing, gbc);

        recipientModel = new RecipientModel();
        recipientTable = new JTable(recipientModel);
        recipientTable.setDefaultRenderer(Object.class, new RecipientCellRenderer());

        TableRowSorter<RecipientModel> sorter = new TableRowSorter<>(recipientModel);
        recipientFilter = new RecipientFilter();
        sorter.setRowFilter(recipientFilter);
        recipientTable.setRowSorter(sorter);
        recipientTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(recipientTable);
        recipientTable.setFillsViewportHeight(true);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        add(scrollPane, gbc);

        // Absorb any extra space.
        //gbc.weighty = 1.0;
        //add(new JLabel(), gbc);

        onlyMissing.addActionListener(ev->{
            boolean on = ((JCheckBox)ev.getSource()).isSelected();
            recipientFilter.setPredicate(recipientAdapter ->
                !on || !context.recipientHasRecording.get(recipientAdapter.recipientid));
            recipientModel.fireTableDataChanged();
        });
    }

    @Override
    protected void onPageEntered(boolean progressing) {
        findRecipientsWithRecordings();
        recipientModel.fireTableDataChanged();
        setComplete();
    }

    @Override
    protected void onPageLeaving(boolean progressing) {
    }

    @Override
    protected String getTitle() {
        return "Introduction";
    }

    private void getProgramInformation() {
        File programSpecDir = ACMConfiguration.getInstance()
                .getCurrentDB()
                .getPathProvider()
                .getProgramSpecDir();

        context.programSpec = new ProgramSpec(programSpecDir);
        context.recipientColumnProvider = context.new RecipientColumnProvider();
    }

    private void findRecipientsWithRecordings() {
        Map<String, String> recipientsMap = context.programSpec.getRecipientsMap();
        File tbLoadersDir = ACMConfiguration.getInstance().getCurrentDB().getProgramTbLoadersDir();
        File communitiesDir = new File(tbLoadersDir, "communities");

        for (Recipient recipient : context.programSpec.getRecipients()) {
            File recipientDir = new File(communitiesDir, recipientsMap.get(recipient.recipientid));
            File languagesDir = new File(recipientDir, "languages");
            File languageDir = new File(languagesDir, recipient.languagecode);
            File greeting = new File(languageDir, "10.a18");
            context.recipientHasRecording.put(recipient.recipientid, greeting.exists() && greeting.isFile());
        }
    }

    private class RecipientCellRenderer extends DefaultTableCellRenderer {
        RecipientCellRenderer() {
            super();
            setOpaque(true);
        }
        @Override
        public JLabel getTableCellRendererComponent(JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column)
        {
            Color bg = (row%2 == 0) ? bgColor : bgAlternateColor;
            if (isSelected) {
                bg = bgSelectionColor;
            }
            super.getTableCellRendererComponent(table,
                value,
                isSelected,
                hasFocus,
                row,
                column);
            if (column == 0) {
                int modelRow = recipientTable.convertRowIndexToModel(row);
                RecipientAdapter recipient = context.programSpec.getRecipients().get(modelRow);
                String recipientid = recipient.recipientid;
                setIcon(context.recipientHasRecording.getOrDefault(recipientid, true) ? soundImage : noSoundImage);
            } else {
                setIcon(null);
            }
            setBackground(bg);
            return this;
        }
    }

    class RecipientFilter extends RowFilter<RecipientModel, Integer> {
        private Predicate<RecipientAdapter> predicate;
        void setPredicate(Predicate<RecipientAdapter> predicate) {
            this.predicate = predicate;
        }

        @Override
        public boolean include(Entry<? extends RecipientModel, ? extends Integer> entry) {
            if (predicate == null) return true;
            int rowIx = entry.getIdentifier();
            RecipientAdapter recipient = context.programSpec.getRecipients().get(rowIx);
            return predicate.test(recipient);
        }
    }

    private class RecipientModel extends AbstractRecipientsModel {
        RecipientModel() {
            super(context.recipientColumnProvider);
        }

        @Override
        public int getRowCount() {
            return context.programSpec.getRecipients().size();
        }

        @Override
        RecipientAdapter getRecipientAt(int rowIndex) {
            return context.programSpec.getRecipients().get(rowIndex);
        }
    }

}
