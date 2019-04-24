package org.literacybridge.acm.gui.assistants.Deployment;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * This class describes issues with a Deployment. Some are merely warnings, others are
 * fatal errors.
 */
public class Issues {
    public enum Severity {
        ERROR("Errors"),
        WARNING("Warnings"),
        INFO("Information");

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

    void addToTree(DefaultMutableTreeNode issueTreeRoot) {
        for (Issue issue : issues) {
            Severity severity = issue.severity;
            Area area = issue.area;
            // Get the node for severity.
            SeverityNode severityNode = null;
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
            // Get the node for the area.
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
            areaNode.add(new IssueNode(issue));
        }
    }

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


    /**
     * Encapsulates a single issue.
     */
    public class Issue {
        Severity severity;
        Area area;
        String message;
        Object[] args;

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
    }

    class SeverityNode extends DefaultMutableTreeNode {
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
    class AreaNode extends DefaultMutableTreeNode {
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
    class IssueNode extends DefaultMutableTreeNode {
        IssueNode(Issue issue) {
            super(issue, true);
        }

        @Override
        public String toString() {
            Object o = getUserObject();
            if (o instanceof Issue) return ((Issue)o).format();
            return o.toString();
        }
    }


}
