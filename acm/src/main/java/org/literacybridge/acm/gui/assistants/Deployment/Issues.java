package org.literacybridge.acm.gui.assistants.Deployment;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * This class describes issues with a Deployment. Some are merely warnings, others are
 * fatal errors.
 */
public class Issues {
    public enum Severity {
        INFO("Info"), WARNING("Warning"), ERROR("ERROR");

        String displayName;

        Severity(String displayName) {
            this.displayName = displayName;
        }

        String displayName() {
            if (displayName != null) return displayName;
            return name();
        }
    }

    public enum Area {
        LANGUAGES("Languages"),
        CONTENT("Content"),
        PLAYLISTS("Playlists"),
        CATEGORY_PROMPTS("Prompts"),
        SYSTEM_PROMPTS("Sys Prompts"),
        CUSTOM_GREETINGS("Greetings");

        String displayName;

        Area(String displayName) {
            this.displayName = displayName;
        }

        String displayName() {
            if (displayName != null) return displayName;
            return name();
        }
    }

    private List<Issue> issues = new ArrayList<>();

    void clear() {
        issues.clear();
    }

    public void add(Severity severity, Area area, String message, Object... args) {
        Issue issue = new Issue(severity, area, message, args);
        issues.add(issue);
    }

    public List<Issue> getIssues() {
        return issues;
    }

    public List<Issue> getIssuesBySeverity() {
        List<Issue> result = new ArrayList<>(issues);
        result.sort((a, b) -> a.severity.ordinal() - b.severity.ordinal());
        return result;
    }

    public List<Issue> getIssuesByArea() {
        List<Issue> result = new ArrayList<>(issues);
        result.sort((a, b) -> a.area.ordinal() - b.area.ordinal());
        return result;
    }

    /**
     * Encapsulates a single issue.
     */
    public class Issue {
        Severity severity;
        Area area;
        String message;
        Object[] args;

        private Issue(Severity severity, Area area, String message, Object... args) {
            this.severity = severity;
            this.area = area;
            this.message = message;
            this.args = args;
        }
    }

    /**
     * Expose the list of issues as a JTable model.
     */
    public class IssueTableModel extends AbstractTableModel {

        @Override
        public int getRowCount() {
            return issues.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Issue issue = issues.get(rowIndex);
            switch (columnIndex) {
            case 0:
                return issue.severity.displayName();
            case 1:
                return issue.area.displayName();
            case 2:
                return String.format(issue.message, issue.args);
            }
            return null;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
            case 0:
                return "Severity";
            case 1:
                return "Area";
            case 2:
                return "Issue";
            }
            return null;
        }

    }
}
