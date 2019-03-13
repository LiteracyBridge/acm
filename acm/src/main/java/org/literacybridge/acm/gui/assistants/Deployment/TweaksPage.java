package org.literacybridge.acm.gui.assistants.Deployment;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Assistant.Assistant.PageHelper;
import org.literacybridge.acm.gui.Assistant.AssistantPage;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.core.spec.Content;
import org.literacybridge.core.spec.RecipientList;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class TweaksPage extends AssistantPage<DeploymentContext> {
    private final JLabel deployment;
    private final JCheckBox includeUfCategory;
    private final JCheckBox includeTbCategory;
    private final JCheckBox noPublish;

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
        add(hbox, gbc);

        includeUfCategory = new JCheckBox("Include User Feedback Category.");
        includeUfCategory.setSelected(true);
        add(includeUfCategory, gbc);
        includeUfCategory.addActionListener(this::onSelection);
        includeTbCategory = new JCheckBox("Include Talking Book category ('The Talking Book is an audio computer, "
            +"that shares knowledge...')");
        add(includeTbCategory, gbc);
        includeTbCategory.addActionListener(this::onSelection);
        noPublish = new JCheckBox("Do not publish the Deployment; create only.");
        add(noPublish, gbc);

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
    protected void onPageLeaving(boolean progressing) {
        context.includeUfCategory = includeUfCategory.isSelected();
        context.includeTbCategory = includeTbCategory.isSelected();
        context.noPublish = noPublish.isSelected();
    }

    @Override
    protected String getTitle() {
        return "Final Adjustments";
    }

}
