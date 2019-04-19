package org.literacybridge.acm.gui.assistants.ContentImport;

import org.apache.commons.io.FilenameUtils;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.AbstractMutableTreeTableNode;
import org.jdesktop.swingx.treetable.DefaultTreeTableModel;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Assistant.Assistant.PageHelper;
import org.literacybridge.acm.gui.Assistant.AssistantPage;
import org.literacybridge.acm.utils.OsUtils;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.text.DateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class FilesPage extends ContentImportPage<ContentImportContext> {

    private enum AUDIO_EXTS {
        MP3, OGG, A18, WAV;

        static boolean isAudioFile(String name) {
            String ext = FilenameUtils.getExtension(name);
            if (ext == null) return false;
            ext = ext.toUpperCase();
            return exts.contains(ext);
        }

        static Set<String> exts = new HashSet<>();
        static { for (AUDIO_EXTS ext : AUDIO_EXTS.values()) {exts.add(ext.name());}}
    }

    private final JLabel choosePrompt;
    private final JButton chooseFiles;
    private final JScrollPane filesPreviewScroller;
    private final JLabel filesPreviewLabel;
    private final JLabel deployment;
    private final JLabel language;
    private final JFileChooser fileChooser;
    private final DirectoryNode fileTreeRootNode;
    private final FileTreeModel fileTreeModel;
    private final FileTree fileTreeTable;

    FilesPage(PageHelper<ContentImportContext> listener) {
        super(listener);

        setLayout(new GridBagLayout());

        GridBagConstraints gbc = getGBC();

        JLabel welcome = new JLabel(
            "<html>" + "<span style='font-size:2em'>Choose Files to Import.</span>"
                + "<br/><br/><p>Choose the files that you wish to import.</p>" + "</html>");
        add(welcome, gbc);

        Box hbox = Box.createHorizontalBox();
        hbox.add(new JLabel("Importing message content for deployment "));
        deployment = makeBoxedLabel();
        hbox.add(deployment);
        hbox.add(new JLabel(" and language "));
        language = makeBoxedLabel();
        hbox.add(language);
        hbox.add(Box.createHorizontalGlue());
        add(hbox, gbc);

        hbox = Box.createHorizontalBox();
        choosePrompt = new JLabel("Click here to choose files: ");
        hbox.add(choosePrompt);
        chooseFiles = new JButton("Choose File(s)");
        chooseFiles.addActionListener(this::onChooseFiles);
        hbox.add(chooseFiles);
        hbox.add(Box.createHorizontalGlue());
        add(hbox, gbc);

        // Title preview.
        filesPreviewLabel = new JLabel("Files chosen to import:");
        Insets insets = new Insets(0, 0, 0, 0);
        gbc.insets = insets;
        add(filesPreviewLabel, gbc);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(0, 0));

        fileTreeRootNode = new DirectoryNode(new File(""));
        fileTreeModel = new FileTreeModel(fileTreeRootNode);
        fileTreeTable = new FileTree(fileTreeModel);
        fileTreeTable.setRootVisible(false);
        fileTreeTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        FileTreeTableRenderer fileTreeTableRenderer = new FileTreeTableRenderer();
        fileTreeTable.setDefaultRenderer(Object.class, fileTreeTableRenderer);
        fileTreeTable.setTreeCellRenderer(fileTreeTableRenderer);

        filesPreviewScroller = new JScrollPane(fileTreeTable);
        panel.add(filesPreviewScroller, BorderLayout.CENTER);

        gbc.ipadx = 10;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets.bottom = 0;
        add(panel, gbc);

        fileChooser = new JFileChooser();
        fileChooser.setApproveButtonText("Choose");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileFilter(chooserFilter);

        // Testing code
        if (ACMConfiguration.isTestData()) {
            List<File> testingFiles = Collections.singletonList(new File("/Users/bill/A-test1"));
            List<File> expandedFiles = expandDirectories(testingFiles);
            context.importableFiles.addAll(expandedFiles);
        }

    }

    /**
     * Filter for the choose file dialog. Accepts directories and audio files.
     */
    private FileFilter chooserFilter = new FileFilter() {
        @Override
        public boolean accept(File f) {
            // Ignore hidden files.
            if (f.isHidden() || f.getName().startsWith(".")) return false;
            return f.isDirectory() || AUDIO_EXTS.isAudioFile(f.getName());
        }

        @Override
        public String getDescription() {
            return "Audio files and directories";
        }
    };

    /**
     * User clicked the "add files" button.
     *
     * @param actionEvent is unused.
     */
    private void onChooseFiles(ActionEvent actionEvent) {
        fileChooser.showOpenDialog(this);

        List<File> rootFiles = Arrays.asList(fileChooser.getSelectedFiles());
        context.importableRoots.addAll(rootFiles);
        choosePrompt.setText("Click to choose more files: ");

        fillFileList();
    }

    @Override
    protected void onPageEntered(boolean progressing) {
        if (ACMConfiguration.isTestData() && progressing && context.importableRoots.size()==0) {
            context.importableRoots.addAll(Collections.singletonList(new File("/Users/bill/A-test1")));
        }

        // Fill deployment and language
        deployment.setText(Integer.toString(context.deploymentNo));
        language.setText(context.languagecode);

        fillFileList();
    }

    @Override
    protected String getTitle() {
        return "Files to Import";
    }

    /**
     * Fill the filename preview list.
     */
    private void fillFileList() {
        List<File> expandedFiles = expandRoots(new ArrayList<>(context.importableRoots));
        context.importableFiles.clear();
        context.importableFiles.addAll(expandedFiles);
        fileTreeTable.sizeColumns();

        filesPreviewScroller.setVisible(!fileTreeModel.isEmpty());
        filesPreviewLabel.setVisible(!fileTreeModel.isEmpty());
        chooseFiles.setBorder(fileTreeModel.isEmpty() ? redBorder : blankBorder);
        setComplete(!fileTreeModel.isEmpty());
    }

    /**
     * Given a list of files and/or directories, expand the directories to
     * any audio files contained therein. Directories are searched recursively.
     *
     * @param files and directories to search.
     * @return list of audio files.
     */
    private List<File> expandDirectories(List<File> files) {
        List<File> result = new ArrayList<>();
        List<File> workQueue = new ArrayList<>(files);

        while (workQueue.size() > 0) {
            File file = workQueue.remove(0);
            if (file.isDirectory()) {

                File[] dirContents = file.listFiles(dirfile -> chooserFilter.accept(dirfile));
                if (dirContents != null) workQueue.addAll(Arrays.asList(dirContents));
            } else {
                result.add(file);
            }
        }
        return result;
    }

    private List<File> expandRoots(List<File> roots) {
        // removeAllChildren()
        while (fileTreeModel.getRoot().getChildCount() > 0) {
            AbstractFileNode node = (AbstractFileNode)fileTreeModel.getRoot().getChildAt(0);
            fileTreeModel.removeNodeFromParent(node);
        }
        DirectoryNode virtualRoot;
        // If there are multiple items, or the only item is a file, add a node for
        // the containing directory.
        if (roots.size() > 1 || roots.size() == 1 && roots.get(0).isFile()) {
            virtualRoot = new DirectoryNode(roots.get(0).getParentFile());
            fileTreeModel.insertNodeInto(virtualRoot, fileTreeRootNode, fileTreeModel.getRoot().getChildCount());
        } else {
            virtualRoot = fileTreeRootNode;
        }
        List<File> result = new ArrayList<>();
        for (File root : roots) {
            AbstractFileNode rootNode = nodeFromFile(root);
            if (rootNode != null) {
                fileTreeModel.insertNodeInto(rootNode, virtualRoot, virtualRoot.getChildCount());
                result.addAll(rootNode.files());
            }
        }
        fileTreeTable.expandAll();
        return result;
    }

    private AbstractFileNode nodeFromFile(File file) {
        if (file.isDirectory()) {
            DirectoryNode dirNode = new DirectoryNode(file);
            File[] dirContents = file.listFiles(dirfile -> chooserFilter.accept(dirfile));
            assert dirContents != null;
            for (File childFile : dirContents) {
                AbstractFileNode childNode = nodeFromFile(childFile);
                if (childNode != null) {
                    dirNode.add(childNode);
                }
            }
            return (dirNode.getChildCount() > 0) ? dirNode : null;
        } else {
            return new FileNode(file);
        }
    }

    private class FileTree extends JXTreeTable {
        FileTree(FileTreeModel fileTreeModel) {
            super(fileTreeModel);
        }

        void sizeColumns() {
            List<SizingParams> params = new ArrayList<>();

            // Set column 1 width (Status) on header & values.
            params.add(new SizingParams(1, SizingParams.IGNORE, 20, 60));

            // Set column 2 width (Size) on header & values.
            params.add(new SizingParams(2, SizingParams.IGNORE, 20, 60));

            AssistantPage.sizeColumns(this, params);

            // The timestamp and size columns have been sized to fit themselves. Name will get the rest.
        }
    }

    /**
     * Abstract base class for File and Directory nodes. Both of those have a backing File object.
     */
    private abstract class AbstractFileNode extends AbstractMutableTreeTableNode {
        String[] columns = { "Name", "Timestamp", "Size" };

        AbstractFileNode(File file, boolean allowsChildren) {
            super(file, allowsChildren);
        }

        public File getFile() {
            return (File)getUserObject();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public Object getValueAt(int column) {
            switch (column) {
            case 0:
                return getFile().getName();
            case 1:
                return new Date(getFile().lastModified());
            case 2:
                return getFile().length();
            }
            return null;
        }

        /**
         * Gets a list of all File objects represented by the object, either directly,
         * or as child files of a directory.
         *
         * @return the list of files.
         */
        abstract List<File> files();
    }

    /**
     * A filesystem File node. Consists of itself.
     */
    private class FileNode extends AbstractFileNode {
        FileNode(File file) { super(file, false); }

        /**
         * The files of a FileNode is just the file itself.
         *
         * @return the file as a singleton list.
         */
        List<File> files() { return Collections.singletonList(getFile()); }
    }

    /**
     * A filesystem Directory node. Consists of itself and any children explicitly added.
     */
    private class DirectoryNode extends AbstractFileNode {
        DirectoryNode(File file) { super(file, true); }

        /**
         * The files that have been added to this directory node, or to any child
         * directory nodes.
         *
         * @return a list of all files in this directory node.
         */
        List<File> files() {
            return enumerationAsStream(children())
                .map(o -> (AbstractFileNode) o)
                .map(AbstractFileNode::files)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        }

        @Override
        public Object getValueAt(int column) {
            if (column == 2) return "";
            return super.getValueAt(column);
        }
    }

    /**
     * TreeTable model for the file-ish view.
     */
    private class FileTreeModel extends DefaultTreeTableModel {
        String[] columns = { "Name", "Timestamp", "Size" };

        FileTreeModel(DirectoryNode root) {
            super(root);
        }

        boolean isEmpty() {
            return getRoot().getChildCount() == 0;
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(Object node, int column) {
            return ((AbstractFileNode) node).getValueAt(column);
        }

        @Override
        public Object getChild(Object parent, int index) {
            if (parent instanceof FileNode) return null;
            if (parent instanceof DirectoryNode) return ((DirectoryNode) parent).getChildAt(index);
            return getRoot().getChildAt(index);
        }

        @Override
        public int getChildCount(Object parent) {
            if (parent instanceof DirectoryNode) return ((DirectoryNode) parent).getChildCount();
            if (parent instanceof FileNode) return 0;
            return getRoot().getChildCount();
        }

        @Override
        public int getIndexOfChild(Object parent, Object child) {
            if (parent instanceof DirectoryNode)
                return ((DirectoryNode) parent).getIndex((AbstractFileNode) child);
            return getRoot().getIndex((AbstractFileNode) child);
        }

        @Override
        public boolean isLeaf(Object node) {
            return node instanceof FileNode;
        }
    }

    private class FileTreeTableRenderer extends JLabel
        implements TreeCellRenderer, TableCellRenderer {

        Font defaultFont, monoFont;

        private String renderValue(Object value, boolean isSelected, int column) {
            switch (column) {
            case 0:
                if (value instanceof File) {
                    return ((File) value).getName();
                }
            case 1:
                if (value instanceof Date) {
                    return fileDateStr((Date) value);
                }
            case 2:
                if (value instanceof Long) {
                    return fileSizeStr((Long) value);
                }
            }
            return value.toString();
        }

        FileTreeTableRenderer() {
            super();
            defaultFont = getFont();
            monoFont = new Font("Monospaced", defaultFont.getStyle(), defaultFont.getSize() - 1);
            setFont(monoFont);
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column)
        {
            int modelColumn = table.convertColumnIndexToModel(column);
            String str = renderValue(value, isSelected, modelColumn);
//            Color bg = (row%2 == 0) ? bgColor : bgAlternateColor;
            Color bg = (isSelected) ? bgSelectionColor : bgColor;
            setBackground(bg);
            setFont(modelColumn == 2 ? monoFont : defaultFont);
            setText(str);
            return this;
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree,
            Object value,
            boolean selected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus)
        {
            // This method is only called to render the first column, column 0. Sometimes,
            // it is passed the LastComponent() from the tree path, not the actual  value,
            // so if we get a node, convert it to the value.
            if (value instanceof AbstractFileNode) value = ((AbstractFileNode) value).getValueAt(0);
            String str = renderValue(value, selected, 0);
//            Color bg = (row%2 == 0) ? bgColor : bgAlternateColor;
            Color bg = (selected) ? bgSelectionColor : bgColor;
            setBackground(bg);
            setFont(defaultFont);
            setText(str);
            return this;
        }

    }

    private String fileSizeStr(long longSize) {
        String[] suffixes = { "B ", "kB", "mB", "gB", "tB", "pB", "eB" };
        double size = (double) longSize;
        int ix = 0;
        while (size > 1024D) {
            ix++;
            size /= 1024D;
        }
        String fmt = (size >= 10D) ? "%3.0f %s" : "%3.1f %s";
        String rep = String.format(fmt, size, suffixes[ix]);
        return String.format("%7s", rep);
    }

    private String fileDateStr(Date date) {
        if (!OsUtils.MAC_OS) {
            // Windows style formatting
            Locale here = Locale.getDefault();
            DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, here);
            DateFormat tf = DateFormat.getTimeInstance(DateFormat.SHORT, here);
            return df.format(date) + " " + tf.format(date);
        } else {
            // Bash style formatting
            LocalDateTime ldt = date
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
            DateTimeFormatter formatter;

            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            int year = cal.get(Calendar.YEAR);
            if (year == Calendar
                .getInstance()
                .get(Calendar.YEAR)) {
                formatter = DateTimeFormatter.ofPattern("MMM ppd ppH:mm");
            } else {
                formatter = DateTimeFormatter.ofPattern("MMM ppd  YYYY");
            }
            return formatter.format(ldt);
        }
    }

}
