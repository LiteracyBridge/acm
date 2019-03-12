package org.literacybridge.acm.gui.assistants.Deployment;

import org.apache.commons.lang.StringUtils;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Assistant.Assistant.PageHelper;
import org.literacybridge.acm.gui.Assistant.AssistantPage;
import org.literacybridge.acm.gui.assistants.ContentImport.WelcomePage;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.acm.tbbuilder.TBBuilder;
import org.literacybridge.acm.utils.IOUtils;
import org.literacybridge.core.spec.Content;
import org.literacybridge.core.spec.Recipient;
import org.literacybridge.core.spec.RecipientList;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.literacybridge.acm.gui.assistants.ContentImport.WelcomePage.qualifiedPlaylistName;
import static org.literacybridge.core.tbloader.TBLoaderConstants.GROUP_FILE_EXTENSION;

public class TweaksPage extends AssistantPage<DeploymentContext> {
    private final JLabel deployment;

    private DeploymentContext context;

    private MetadataStore store = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore();

    private RecipientList recipients;
    private Set<String> languages;
    private Map<String, List<Content.Playlist>> allProgramSpecPlaylists;
    private Map<String, List<Playlist>> allAcmPlaylists;

    static TweaksPage Factory(PageHelper listener) {
        return new TweaksPage(listener);
    }

    private TweaksPage(PageHelper listener) {
        super(listener);
        context = getContext();
        setLayout(new GridBagLayout());

        Insets insets = new Insets(0, 0, 20, 0);
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
            "<html>" + "<span style='font-size:2.5em'>Adjustments</span>" + "</ul>"
                + "<br/>Make any final adjustments, and click \"Next\" to create the Deployment. "

                + "</html>");
        add(welcome, gbc);

        Box hbox = Box.createHorizontalBox();
        hbox.add(new JLabel("Creating deployment "));
        deployment = parameterText();
        hbox.add(deployment);
        hbox.add(Box.createHorizontalGlue());
        // TODO: needed?
//        hbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(hbox, gbc);


        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        add(new JLabel(""), gbc);
    }

    /**
     * Called when a selection changes.
     *
     * @param actionEvent is ignored.
     */
    private void onSelection(ActionEvent actionEvent) {
        boolean ok = true;
        setComplete(ok);
    }

    @Override
    protected void onPageEntered(boolean progressing) {
        deployment.setText(Integer.toString(context.deploymentNo));

        if (progressing) {
        }

    }

    @Override
    protected String getTitle() {
        return "Final Adjustments";
    }

    /**
     * Method to size "small" header columns. This is useful for columns with fairly consistent
     * and fairly small data. Sizes the column to show every item.
     * @param table to be sized.
     * @param columnValues A Map of Integer -> Stream<String> where the integer is the
     *                     column number, and the Stream is all the items in the column.
     */
    private void sizeColumns(JTable table, Map<Integer, Stream<String>> columnValues) {
        TableModel model = table.getModel();
        TableCellRenderer headerRenderer = table.getTableHeader().getDefaultRenderer();

        for (Map.Entry<Integer, Stream<String>> e : columnValues.entrySet()) {
            final int columnNo = e.getKey();
            TableColumn column = table.getColumnModel().getColumn(columnNo);

            int headerWidth = headerRenderer.getTableCellRendererComponent(null,
                column.getHeaderValue(),
                false,
                false,
                0,
                0).getPreferredSize().width;

            int cellWidth = e.getValue()
                .map(item -> table.getDefaultRenderer(model.getColumnClass(columnNo))
                    .getTableCellRendererComponent(table, item, false, false, 0, columnNo)
                    .getPreferredSize().width)
                .max(Integer::compareTo)
                .orElse(1);

            int w = Math.max(headerWidth, cellWidth) + 2;
            column.setPreferredWidth(w);
            column.setMaxWidth(w + 40);
            column.setWidth(w);
        }
    }

}
