package org.literacybridge.acm.gui.assistants.ContentImport;

import org.apache.commons.io.FilenameUtils;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Assistant.Assistant.PageHelper;
import org.literacybridge.acm.gui.Assistant.AssistantPage;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class FilesPage extends AssistantPage<ContentImportContext> {

    private final JButton chooseFiles;
    private final JLabel choosePrompt;

    private enum AUDIO_EXTS {
        MP3, OGG, A18, WAV;

        static boolean isAudioFile(String name) {
            String ext = FilenameUtils.getExtension(name);
            if (ext == null) return false;
            ext = ext.toUpperCase();
            return exts.contains(ext);
        }
        static Set<String> exts = new HashSet<>();
        static { for (AUDIO_EXTS ext : AUDIO_EXTS.values()) {exts.add(ext.name());}};
    };

    private final DefaultListModel<String> filesPreviewModel;
    private final JScrollPane filesPreviewScroller;
    private final JLabel filesPreviewLabel;
    private final JLabel deployment;
    private final JLabel language;
    private final JFileChooser fileChooser;

    private ContentImportContext context;

    public FilesPage(PageHelper listener) {
        super(listener);
        context = getContext();
        context.importableFiles = new LinkedHashSet<>();

        setLayout(new GridBagLayout());

        Insets insets = new Insets(0,0,15,0);
        GridBagConstraints gbc = new GridBagConstraints(0,
            GridBagConstraints.RELATIVE,
            1,
            1,
            1.0,
            0.0,
            GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL,
            insets,
            1,
            1);

        JLabel welcome = new JLabel(
            "<html>" + "<span style='font-size:2em'>Choose Files to Import.</span>"
                + "<br/><br/><p>Choose the files that you wish to import.</p>"
                + "</html>");
        add(welcome, gbc);

        Box hbox = Box.createHorizontalBox();
        hbox.add(new JLabel("Importing message content for deployment "));
        deployment = parameterText();
        hbox.add(deployment);
        hbox.add(new JLabel(" and language "));
        language = parameterText();
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
        insets = new Insets(0,0,00,0);
        gbc.insets = insets;
        add(filesPreviewLabel, gbc);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(0,0));
        filesPreviewModel = new DefaultListModel<>();
        JList<String> filesPreview = new JList<>(filesPreviewModel);
        filesPreviewScroller = new JScrollPane(filesPreview);
        panel.add(filesPreviewScroller, BorderLayout.CENTER);
        gbc.ipadx = 10;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        insets = new Insets(0,10,0,30);
        gbc.insets = insets;
        add(panel, gbc);

        fileChooser = new JFileChooser();
        fileChooser.setApproveButtonText("Choose");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileFilter(chooserFilter);

        // Testing code
        if (ACMConfiguration.isTestData()) {
            List<File> testingFiles = Arrays.asList(new File("/Users/bill/A-test1"));
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
            return f.isDirectory() || AUDIO_EXTS.isAudioFile(f.getName());
        }
        @Override
        public String getDescription() {
            return "Audio files and directories";
        }
    };

    /**
     * User clicked the "add files" button.
     * @param actionEvent is unused.
     */
    private void onChooseFiles(ActionEvent actionEvent) {
        int result = fileChooser.showOpenDialog(this);
        List<File> selectedFiles = Arrays.asList(fileChooser.getSelectedFiles());
        List<File> expandedFiles = expandDirectories(selectedFiles);
        context.importableFiles.addAll(expandedFiles);

        fillFileList();
        choosePrompt.setText("Click to choose more files: ");
    }

    @Override
    protected void onPageEntered(boolean progressing) {
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
        filesPreviewModel.clear();
        context.importableFiles.stream()
            .map(File::getName)
            .forEach(filesPreviewModel::addElement);

        filesPreviewScroller.setVisible(!filesPreviewModel.isEmpty());
        filesPreviewLabel.setVisible(!filesPreviewModel.isEmpty());
        chooseFiles.setBorder( filesPreviewModel.isEmpty() ? redBorder : blankBorder);
        setComplete(!filesPreviewModel.isEmpty());
    }

    /**
     * Given a list of files and/or directories, expand the directories to
     * any audio files contained therein. Directories are searched recursively.
     * @param files and directories to search.
     * @return list of audio files.
     */
    private List<File> expandDirectories(List<File> files) {
        List<File> result = new ArrayList<>();
        List<File> workQueue = new ArrayList<>(files);

        while (workQueue.size() > 0) {
            File file = workQueue.remove(0);
            if (file.isDirectory()) {

                File[] dirContents = file.listFiles(f->chooserFilter.accept(f));
                if (dirContents != null)
                    workQueue.addAll(Arrays.asList(dirContents));
            } else {
                result.add(file);
            }
        }
        return result;
    }
}
