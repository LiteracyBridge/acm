package org.literacybridge.acm.gui.assistants.Deployment;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

/**
 * This class describes issues with a Deployment. Some are merely warnings, others are
 * fatal errors.
 */
public class Issues {
    public enum Severity {
        FATAL("Fatal"),
        ERROR("Errors"),
        WARNING("Warnings"),
        INFO("Information");

        final String displayName;

        Severity(String displayName) {
            this.displayName = displayName;
        }

        String displayName() {
            if (displayName != null) return displayName;
            return name();
        }
    }

    public enum Area {
        FIRMWARE("Firmware"),
        LANGUAGES("Languages"),
        INTRO_MESSAGE("Intro Message"),
        CONTENT("Content"),
        PLAYLISTS("Playlists"),
        CATEGORY_PROMPTS("Prompts"),
        SYSTEM_PROMPTS("Sys Prompts"),
        CUSTOM_GREETINGS("Greetings"),
        DEPLOYMENT("Deployment")
        ;

        final String displayName;

        Area(String displayName) {
            this.displayName = displayName;
        }

        String displayName() {
            if (displayName != null) return displayName;
            return name();
        }
    }

    private final List<Issue> issues = new ArrayList<>();

    void clear() {
        issues.clear();
    }

    /**
     * Creates an Issue and adds it to the list of issues.
     * @param severity of the issue.
     * @param area to which the issue relates.
     * @param message informative message about the issue.
     * @param args arguments to String.format(message, args)
     * @return the issue.
     */
    public Issue add(Severity severity, Area area, String message, Object... args) {
        Issue issue = new Issue(severity, area, message, args);
        issues.add(issue);
        return issue;
    }

    /**
     * Adds our issues to the given tree. It may be, but doesn't have to be, empty.
     * @param issueTreeRoot to be filled
     */
    void fillNodeFromList(DefaultMutableTreeNode issueTreeRoot) {
        for (Issue issue : issues) {
            Severity severity = issue.severity;
            Area area = issue.area;
            // Find or insert the node for severity. It may already exist under issueTreeRoot, or
            // it may need to be created. The severity nodes are ordered from most-severe
            // to least severe, so search until we find a node of equal (use it) or lesser
            // (insert before it) severity.
            SeverityNode severityNode = null;
            @SuppressWarnings("rawtypes")
            Enumeration e = issueTreeRoot.children();
            while (e.hasMoreElements() && severityNode == null) {
                SeverityNode sNode = (SeverityNode) e.nextElement();
                if (sNode.getSeverity() == severity) {
                    severityNode = sNode;
                } else if (sNode.getSeverity().ordinal() > severity.ordinal()) {
                    // Found where it should go (before this).
                    severityNode = new SeverityNode(issue);
                    issueTreeRoot.insert(severityNode, issueTreeRoot.getIndex(sNode));
                }
            }
            // Didn't find a spot in the list; add to the end.
            if (severityNode == null) {
                severityNode = new SeverityNode(issue);
                issueTreeRoot.add(severityNode);
            }

            // Find or insert the node for the area, under the severity node.
            AreaNode areaNode = null;
            e = severityNode.children();
            while (e.hasMoreElements() && areaNode==null) {
                // Don't care about order; makes it much simpler.
                AreaNode aNode = (AreaNode) e.nextElement();
                if (aNode.getArea() == area) {
                    areaNode = aNode;
                }
            }
            if (areaNode == null) {
                areaNode = new AreaNode(issue);
                severityNode.add(areaNode);
            }

            // Add the issue to its area.
            areaNode.add(new IssueNode(issue));
        }
    }

    /**
     * Determine if there is any issue with a given severity
     * @param severity of interest
     * @return true is such an issue exists
     */
    private boolean hasIssue(Severity severity) {
        Issue issue = issues
            .stream()
            .filter(is -> is.severity==severity)
            .findFirst()
            .orElse(null);
        return issue != null;
    }

    boolean hasWarning() {
        return hasIssue(Severity.WARNING);
    }

    boolean hasError() {
        return hasIssue(Severity.ERROR);
    }

    boolean hasFatalError() {
        return hasIssue(Severity.FATAL);
    }

    boolean hasNoIssues() {
        return issues.isEmpty();
    }


    /**
     * Encapsulates a single issue. Details are optional.
     */
    public static class Issue {
        Severity severity;
        Area area;
        String message;
        Object[] args;
        List<String> details;

        Severity getSeverity() {
            return severity;
        }

        private Issue(Severity severity, Area area, String message, Object... args) {
            this.severity = severity;
            this.area = area;
            this.message = message;
            this.args = args;
        }

        public String format() {
            return String.format(message, args);
        }

        public void addDetail(String detail) {
            if (this.details == null) {
                this.details = new ArrayList<>();
            }
            this.details.add(detail);
        }

        public boolean hasDetails() {
            return this.details != null && this.details.size() > 0;
        }

        public Collection<String> getDetails() {
            return details;
        }
    }

    static class SeverityNode extends DefaultMutableTreeNode {
        SeverityNode(Issue issue) {
            super(issue.getSeverity(), true);
        }

        @Override
        public String toString() {
            Object o = getUserObject();
            if (o instanceof Severity) return ((Severity)o).displayName();
            return o.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof SeverityNode)) return false;
            return ((SeverityNode)obj).getSeverity() == getSeverity();
        }

        Severity getSeverity() {
            return (Severity) getUserObject();
        }
    }
    static class AreaNode extends DefaultMutableTreeNode {
        AreaNode(Issue issue) {
            super(issue.area, true);
        }

        @Override
        public String toString() {
            Object o = getUserObject();
            if (o instanceof Area) return ((Area)o).displayName();
            return o.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof AreaNode)) return false;
            return ((AreaNode)obj).getArea() == getArea();
        }

        Area getArea() {
            return (Area) getUserObject();
        }
    }
    static class IssueNode extends DefaultMutableTreeNode {
        IssueNode(Issue issue) {
            super(issue, true);
            if (issue.hasDetails()) {
                for (String detail : issue.getDetails()) {
                    this.add(new IssueDetailNode(detail));
                }
            }
        }

        @Override
        public String toString() {
            Object o = getUserObject();
            if (o instanceof Issue) return ((Issue)o).format();
            return o.toString();
        }
    }

    static class IssueDetailNode extends DefaultMutableTreeNode {
        IssueDetailNode(String detail) {
            super(detail, false);
        }

        @Override
        public String toString() {
            return getUserObject().toString();
        }
    }

}
