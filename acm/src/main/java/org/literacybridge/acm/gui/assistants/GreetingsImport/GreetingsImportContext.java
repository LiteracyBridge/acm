package org.literacybridge.acm.gui.assistants.GreetingsImport;

import org.literacybridge.acm.gui.assistants.Matcher.ColumnProvider;
import org.literacybridge.acm.gui.assistants.common.AbstractFilesPage;
import org.literacybridge.acm.gui.assistants.common.AbstractMatchPage;
import org.literacybridge.core.spec.ProgramSpec;
import org.literacybridge.core.spec.RecipientList;
import org.literacybridge.core.spec.RecipientList.RecipientAdapter;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GreetingsImportContext
    implements AbstractFilesPage.FileImportContext, AbstractMatchPage.MatchContext {
    private final static List<String> RecipientColumnsOfInterest = Arrays.asList("Community","Group","Agent");

    public ProgramSpec programSpec;
    public RecipientColumnProvider recipientColumnProvider;

    // Which recipients have custom greetings?
    public Map<String, Boolean> recipientHasRecording = new HashMap<>();

    // From the Files page

    /**
     * The files that the user selected to import.
     */
    final Set<File> importableRoots = new LinkedHashSet<>();
    final Set<File> importableFiles = new LinkedHashSet<>();

    // Accessors to satisfy FileImportContext.
    @Override
    public Set<File> getImportableRoots() {
        return importableRoots;
    }
    @Override
    public Set<File> getImportableFiles() {
        return importableFiles;
    }

    GreetingsMatcher matcher = new GreetingsMatcher();
    @Override
    public GreetingsMatcher getMatcher() {
        return matcher;
    }

    class RecipientColumnProviderBase {
        private final List<Integer> recipientColumnsMap;
        List<String> columnNames;

        RecipientColumnProviderBase() {
            RecipientList recipientList = programSpec.getRecipients();
            columnNames = IntStream.rangeClosed(0, recipientList.getMaxLevel())
                .mapToObj(recipientList::getNameOfLevel)
                .filter(RecipientColumnsOfInterest::contains)
                .collect(Collectors.toList());

            recipientColumnsMap = recipientList.getAdapterIndicesForValues(columnNames);
        }

        public int getColumnCount() {
            return columnNames.size();
        }

        public String getColumnName(int columnIndex) {
            return columnNames.get(columnIndex);
        }

        public Class getColumnClass(@SuppressWarnings("unused") int columnIndex) {
            return String.class;
        }

        public Object getValueAt(RecipientAdapter row, int columnIndex) {
            if (row == null) return null;
            return row.getValue(recipientColumnsMap.get(columnIndex));
        }
    }

    class RecipientColumnProvider extends RecipientColumnProviderBase implements ColumnProvider<RecipientAdapter> {
    }

    class GreetingTargetColumnProvider extends RecipientColumnProviderBase implements ColumnProvider<GreetingTarget> {
        @Override
        public Object getValueAt(GreetingTarget row, int columnIndex) {
            if (row == null) return null;
            RecipientAdapter recipient = row.getRecipient();
            return super.getValueAt(recipient, columnIndex);
        }
    }

    class GreetingMatchableColumnProvider extends RecipientColumnProviderBase implements ColumnProvider<GreetingMatchable> {
        @Override
        public Object getValueAt(GreetingMatchable data, int columnIndex) {
            if (data == null) return null;
            RecipientAdapter recipient = data.getLeft().getRecipient();
            return super.getValueAt(recipient, columnIndex);
        }
    }

}
