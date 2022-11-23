package org.literacybridge.acm.tbloader;

import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.gui.util.UIUtils;
import org.literacybridge.core.spec.Recipient;
import org.literacybridge.core.spec.RecipientList.RecipientAdapter;
import org.literacybridge.core.tbloader.TbOperation;
import org.literacybridge.core.tbloader.TbsCollected;
import org.literacybridge.core.tbloader.TbsDeployed;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.literacybridge.acm.gui.Assistant.AssistantPage.sizeColumns;
import static org.literacybridge.acm.gui.util.UIUtils.UiOptions.TOP_THIRD;
import static org.literacybridge.acm.utils.SwingUtils.addEscapeListener;

public class TbHistoryDetails extends JDialog {
    private List<RecipientAdapter> relevantRecipients;
    private Map<String, Recipient> recipientMap;
    private final TbHistory history = TbHistory.getInstance();
    private final TbHistorySummarizer summarizer = history.getSummarizer();
    private JTable dataTable;
    private HistoryDetailsModel dataModel;
    private Dimension preferredRowSize;

    public TbHistoryDetails(List<RecipientAdapter> relevantRecipients, int eventId) {
        this.relevantRecipients = relevantRecipients;
        this.recipientMap = relevantRecipients.stream()
            .collect(Collectors.toMap(Recipient::getRecipientid, x -> x));

        setLayout(new BorderLayout());

        // Add the Close button
        JButton closeButton = new JButton(LabelProvider.getLabel("Close"));
        closeButton.addActionListener(e -> this.setVisible(false));
        Box buttonBox = Box.createHorizontalBox();
        buttonBox.add(Box.createHorizontalGlue());
        buttonBox.add(closeButton);
        buttonBox.add(Box.createHorizontalStrut(20));
        buttonBox.setBorder(new EmptyBorder(10, 10, 10, 10));
        add(buttonBox, BorderLayout.SOUTH);

        JPanel tableHolder = new JPanel();
        tableHolder.setBorder(new EmptyBorder(0, 10, 0, 10));
        tableHolder.setLayout(new BorderLayout());
        add(tableHolder, BorderLayout.CENTER);

        Supplier<HistoryDetailsModel> modelCtor = getModelCtor(eventId);
        if (modelCtor != null) {
            dataModel = modelCtor.get();
            JComponent dataTable = makeTable(dataModel);
            tableHolder.add(dataTable, BorderLayout.CENTER);
            setTitle(dataModel.tableTitle());
        } else {
            JPanel nyiPanel = new JPanel();
            nyiPanel.setLayout(new FlowLayout());
            JLabel nyi = new JLabel("<html><span style='font-size:1.50em;'>Not yet implemented.</style></html>");
            nyiPanel.add(nyi);
            add(nyiPanel, BorderLayout.CENTER);
            setTitle("Not Implemented");
        }

        int w = 650, h = 450;
        if (preferredRowSize != null) {
            w = preferredRowSize.width + 40;
            h = preferredRowSize.height * dataTable.getModel().getRowCount() + 100;
            w = Math.max(w, 300);
            h = Math.max(h, 450);
        }
        Rectangle deviceBounds = getGraphicsConfiguration().getDevice().getDefaultConfiguration().getBounds();
        w = Math.min(w, deviceBounds.width);
        h = Math.min(h, deviceBounds.height);
        Dimension dialogSize = new Dimension(w, h);
        setSize(dialogSize);
        setAlwaysOnTop(true);
//        setModalityType(ModalityType.APPLICATION_MODAL);

        addEscapeListener(this);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        UIUtils.centerWindow(this, TOP_THIRD);

        // For debugging sizing issues.
//        addComponentListener(new java.awt.event.ComponentAdapter() {
//            @Override
//            public void componentResized(ComponentEvent e) {
//                System.out.printf("Size %dx%d%n", TbHistoryDetails.this.getWidth(), TbHistoryDetails.this.getHeight());
//            }
//        });
//
        history.addChangeListener(historyChangeListener);
    }

    @Override
    public void setVisible(boolean visible) {
        if (!visible) history.removeChangeListener(historyChangeListener);
        super.setVisible(visible);
    }

    private final ChangeListener historyChangeListener = (changeEvent) -> {
        relevantRecipients = summarizer.getRelevantRecipients();
        recipientMap = relevantRecipients.stream()
            .collect(Collectors.toMap(Recipient::getRecipientid, x -> x));
        dataModel.historyUpdated();
    };

    private JComponent makeTable(TableModel tableModel) {
        // Make a table based on the model. Make grid visible. Make it scrollable.
        dataTable = new JTable(tableModel);
        dataTable.setGridColor(Color.lightGray);
        JScrollPane scrollPane = new JScrollPane(dataTable);
        dataTable.setFillsViewportHeight(true);
        // Make the columns sortable, and sort them.
        dataTable.setRowSorter(new TableRowSorter<>(dataTable.getModel()));
        Integer[] columnNos = IntStream.range(0, dataTable.getModel().getColumnCount()).boxed().toArray(Integer[]::new);
        List<RowSorter.SortKey> sortKeys = new ArrayList<>();
        IntStream.range(0, columnNos.length).forEach(x -> sortKeys.add(new RowSorter.SortKey(x, SortOrder.ASCENDING)));
        dataTable.getRowSorter().setSortKeys(sortKeys);
        // Size the columns based on the contained data.
        preferredRowSize = sizeColumns(dataTable, columnNos);
        return scrollPane;
    }

    private Supplier<HistoryDetailsModel> getModelCtor(int id) {
        switch (id) {
            //case TbHistoryPanel.NUM_TBS_ID:
            // todo
            case TbHistoryPanel.COLLECTION_DEPLOYED_ID:
                return TbsDeployedAllModel::new;
            case TbHistoryPanel.COLLECTION_COLLECTED_ID:
                return TbsCollectedModel::new;
            case TbHistoryPanel.COLLECTION_TO_COLLECT_ID:
                return ToCollectModel::new;
            case TbHistoryPanel.DEPLOYMENT_DEPLOYED_ID:
                return TbsDeployedModel::new;
            case TbHistoryPanel.DEPLOYMENT_TO_DEPLOY_ID:
                return ToDeployModel::new;
        }
        return null;
    }

    private abstract class HistoryDetailsModel extends AbstractTableModel {
        void historyUpdated() {
            fetchData();
            fireTableDataChanged();
            Integer[] columnNos = IntStream.range(0, dataTable.getModel().getColumnCount())
                .boxed()
                .toArray(Integer[]::new);
            // Size the columns based on the contained data.
            sizeColumns(dataTable, columnNos);
        }

        abstract void fetchData();

        abstract String tableTitle();
    }

    private abstract class RecipientAndCountModel extends HistoryDetailsModel {
        String[] columnNames;
        protected List<String> recipientList;

        RecipientAndCountModel(String... additionalNames) {
            super();
            List<String> columnNames = relevantRecipients.get(0).getDistinguishingNames();
            // uncomment to add a sequence number
//            int addedIx = 0;
//            columnNames.add(addedIx++, "#");
            columnNames.addAll(Arrays.asList(additionalNames));
            this.columnNames = columnNames.toArray(new String[0]);
        }

        @Override
        public int getRowCount() {
            return recipientList.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            String recipientId = recipientList.get(rowIndex);
            Recipient recipient = recipientMap.get(recipientId);
            switch (columnNames[columnIndex]) {
                case "#":
                    return (rowIndex+1);
                case "Name":
                    return recipient.getName();
                case "Country":
                    return recipient.getCountry();
                case "Region":
                    return recipient.getRegion();
                case "District":
                    return recipient.getDistrict();
                case "Community": // Community
                    return recipient.getCommunityname();
                case "Group": // Gropu
                    return recipient.getGroupname();
                case "Agent": // Agent
                    return recipient.getAgent();
                case "Language": // language
                    return recipient.getLanguagecode();
                case "Spec TBs": // Num tbs
                    return recipient.getNumtbs();
            }
            return null;
        }
    }

    private class ToDeployModel extends RecipientAndCountModel {
        protected Map<String, Integer> toDeployCounts;
        protected Map<String, Integer> deployedCounts;

        ToDeployModel() {
            super("Spec TBs", "Updated", "To Update");
            fetchData();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            String recipientId = recipientList.get(rowIndex);
            switch (columnNames[columnIndex]) {
                case "To Update":  // Remaining
                    return toDeployCounts.getOrDefault(recipientId, 0);
                case "Updated":
                    return deployedCounts.getOrDefault(recipientId, 0);
            }
            return super.getValueAt(rowIndex, columnIndex);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnNames[columnIndex]) {
                case "Spec TBs":
                case "Updated":
                case "To Update":
                    return Integer.class;
            }
            return super.getColumnClass(columnIndex);
        }

        @Override
        void fetchData() {
            toDeployCounts = summarizer.getToDeployPerRecipient();
            deployedCounts = summarizer.getNumTbsDeployedLatestPerRecipient();
            recipientList = new ArrayList<>(toDeployCounts.keySet());
        }

        @Override
        String tableTitle() {
            return "Number of TBs to be Updated";
        }
    }

    private class ToCollectModel extends RecipientAndCountModel {
        protected Map<String, Integer> toCollectCounts;
        protected Map<String, Integer> activeCounts;

        ToCollectModel() {
            super("Spec TBs", "Deployed", "Collected", "To Collect");
            fetchData();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            String recipientId = recipientList.get(rowIndex);
            switch (columnNames[columnIndex]) {
                case "Deployed":
                    return activeCounts.getOrDefault(recipientId, 0);
                case "Collected":
                    return Math.max(0,
                        activeCounts.getOrDefault(recipientId, 0) - toCollectCounts.getOrDefault(recipientId, 0));
                case "To Collect": // Remaining
                    return toCollectCounts.getOrDefault(recipientId, 0);
            }
            return super.getValueAt(rowIndex, columnIndex);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnNames[columnIndex]) {
                case "Spec TBs":
                case "Deployed":
                case "Collected":
                case "To Collect":
                    return Integer.class;
            }
            return super.getColumnClass(columnIndex);
        }

        @Override
        void fetchData() {
            toCollectCounts = summarizer.getToCollectPerRecipient();
            activeCounts = summarizer.getNumTbsDeployedLatestPerRecipient();
            recipientList = new ArrayList<>(toCollectCounts.keySet());
        }

        @Override
        String tableTitle() {
            return "Number of TBs to be Collected";
        }
    }

    private abstract class TbAndOperationModel<T extends TbOperation> extends HistoryDetailsModel {
        String[] columnNames;
        protected List<T> operationList;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm a");

        TbAndOperationModel(String... additionalNames) {
            super();
            List<String> columnNames = relevantRecipients.get(0).getDistinguishingNames();
            int addedIx = 0;
            // Uncomment to add a sequence number
//            columnNames.add(addedIx++, "#");
            columnNames.add(addedIx++, "User");
            columnNames.add(addedIx++, "When");
            columnNames.addAll(Arrays.asList(additionalNames));
            this.columnNames = columnNames.toArray(new String[0]);
        }

        @Override
        public int getRowCount() {
            return operationList.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            T operation = operationList.get(rowIndex);
            String recipientId = operation.getRecipientid();
            Recipient recipient = recipientMap.get(recipientId);
            switch (columnNames[columnIndex]) {
                case "#":
                    return (rowIndex+1);
                case "User":
                    return operation.getUsername();
                case "Date":
                case "When":
                    Date d = operation.getOperationTimestamp();
                    return dateFormat.format(d);
                case "Name":
                    return recipient.getName();
                case "Country":
                    return recipient.getCountry();
                case "Region":
                    return recipient.getRegion();
                case "District":
                    return recipient.getDistrict();
                case "Community": // Community
                    return recipient.getCommunityname();
                case "Group": // Gropu
                    return recipient.getGroupname();
                case "Agent": // Agent
                    return recipient.getAgent();
                case "Language": // language
                    return recipient.getLanguagecode();
                case "Spec TBs": // Num tbs
                    return recipient.getNumtbs();
                case "TB Updated":
                case "TB Collected":
                case "TalkingBook ID":
                    return operation.getTalkingbookid();
            }
            return null;
        }
    }

    private class TbsDeployedModel extends TbAndOperationModel<TbsDeployed> {
        public TbsDeployedModel() {
            super("TB Updated");
            fetchData();
        }

        @Override
        void fetchData() {
            operationList = new ArrayList<>(summarizer.getTbsDeployedLatest().values());
        }

        @Override
        String tableTitle() {
            return "TBs that Have Been Updated with Latest Content";
        }
    }

    private class TbsCollectedModel extends TbAndOperationModel<TbsCollected> {
        public TbsCollectedModel() {
            super("TB Collected");
            fetchData();
        }

        @Override
        void fetchData() {
            operationList = new ArrayList<>(summarizer.getTbsCollected().values());
        }

        @Override
        String tableTitle() {
            return "TBs From Which Statistics Have Been Collected";
        }
    }

    private class TbsDeployedAllModel extends TbAndOperationModel<TbsDeployed> {
        public TbsDeployedAllModel() {
            super("TB Updated");
            fetchData();
        }

        @Override
        void fetchData() {
            operationList = new ArrayList<>(summarizer.getTbsDeployedAll().values());
        }

        @Override
        String tableTitle() {
            return "TBs That Have Been Loaded with Any Content";
        }
    }
}
