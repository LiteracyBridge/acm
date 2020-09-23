package org.literacybridge.acm.gui.dialogs;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.cloud.Authenticator;
import org.literacybridge.acm.cloud.Authenticator.AwsInterface;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Assistant.GBC;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import static java.awt.Dialog.ModalityType.APPLICATION_MODAL;

public class AcmCheckoutTest extends JDialog {
    private final Color dialogBackground = new Color(236,236,236);
    private Color backgroundColor = dialogBackground;
    private JComboBox<String> functionChooser;
    private JTextField programField;
    private JTextArea results;
    private JTextField phoneField;
    private JTextField nameField;
    private JTextField versionField;
    private JTextField computernameField;
    private JTextField keyField;
    private JTextField filenameField;
    private JTextField commentsField;
    private JTextField toField;
    private JTextField fromField;

    public AcmCheckoutTest(final JFrame parent) {
        super(parent, "ACM Checkout Test", APPLICATION_MODAL);

        setLayout(new BorderLayout());
        add(layoutComponents(), BorderLayout.CENTER);
        add(layoutButtons(), BorderLayout.SOUTH);

        setResizable(true);
        Dimension d = getPreferredSize();
        d.width = Math.max(d.width, 500);
        d.height = Math.max(d.height, 600);
        setSize(d);

    }

    private JComponent layoutComponents() {
        Authenticator authenticator = Authenticator.getInstance();
        JPanel dialogPanel = new JPanel();
        dialogPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));
        dialogPanel.setLayout(new GridBagLayout());
        dialogPanel.setOpaque(true);
        dialogPanel.setBackground(dialogBackground);

        GBC gbc_labels  = new GBC().setGridx(0).setFill(GridBagConstraints.HORIZONTAL).setAnchor(GridBagConstraints.LINE_END);
        GBC gbc_controls = gbc_labels.withGridx(1).setWeightx(1).setAnchor(GridBagConstraints.LINE_START);

        dialogPanel.add(new JLabel("Action:"), gbc_labels);

        dialogPanel.add(makeFunctionChooser(), gbc_controls);

        dialogPanel.add(new JLabel("Program:"), gbc_labels);
        programField = new JTextField(ACMConfiguration.getInstance().getCurrentDB().getProgramName());
        dialogPanel.add(programField, gbc_controls);

        dialogPanel.add(new JLabel("Name:"), gbc_labels);
        nameField = new JTextField(authenticator.getUserEmail());
        dialogPanel.add(nameField, gbc_controls);

        dialogPanel.add(new JLabel("Phone:"), gbc_labels);
        phoneField = new JTextField(authenticator.getUserName());
        dialogPanel.add(phoneField, gbc_controls);

        dialogPanel.add(new JLabel("Version:"), gbc_labels);
        versionField = new JTextField(Constants.ACM_VERSION);
        dialogPanel.add(versionField, gbc_controls);

        String computerName;
        try {
            computerName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e1) {
            computerName = "UNKNOWN";
        }
        dialogPanel.add(new JLabel("Computer Name:"), gbc_labels);
        computernameField = new JTextField(computerName);
        dialogPanel.add(computernameField, gbc_controls);

        dialogPanel.add(new JLabel("Key:"), gbc_labels);
        keyField = new JTextField("");
        dialogPanel.add(keyField, gbc_controls);

        dialogPanel.add(new JLabel("File name:"), gbc_labels);
        filenameField = new JTextField("db1.zip");
        dialogPanel.add(filenameField, gbc_controls);

        dialogPanel.add(new JLabel("From:"), gbc_labels);
        fromField = new JTextField(authenticator.getUserEmail());
        dialogPanel.add(fromField, gbc_controls);

        dialogPanel.add(new JLabel("To:"), gbc_labels);
        toField = new JTextField(authenticator.getUserEmail());
        dialogPanel.add(toField, gbc_controls);

        dialogPanel.add(new JLabel("Comments:"), gbc_labels);
        commentsField = new JTextField("");
        dialogPanel.add(commentsField, gbc_controls);

        results = new JTextArea(2, 80);
        results.setEditable(false);
        results.setLineWrap(true);
        JScrollPane resultsScroller = new JScrollPane(results);
        resultsScroller.setBorder(null); // eliminate black border around status log
        resultsScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        dialogPanel.add(resultsScroller, gbc_controls.withGridwidth(2).setFill(GridBagConstraints.BOTH).setWeighty(2.0));

        return dialogPanel;
    }

    private JComboBox<String> makeFunctionChooser() {
        functionChooser = new JComboBox<>();
        functionChooser.addItem("checkout");
        functionChooser.addItem("checkin");
        functionChooser.addItem("discard");
        functionChooser.addItem("revokecheckout");
        functionChooser.addItem("create");
        functionChooser.addItem("reset");
        functionChooser.addItem("statuscheck");
        functionChooser.addItem("list");
        functionChooser.addItem("report");
        return functionChooser;
    }

    private String paramString(String name, JTextField valueField) {
        String value = valueField.getText();
        if (StringUtils.isNotBlank(value)) {
            return String.format("&%s=%s", name, value);
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private void onOk() {
        Authenticator authenticator = Authenticator.getInstance();
        String devoUrl = "https://cqmltfugtl.execute-api.us-west-2.amazonaws.com/devo";
        results.setText("");
        results.setText(String.format("%s %s%n", functionChooser.getSelectedItem(), programField.getText()));
        StringBuilder url = new StringBuilder(devoUrl);
        url.append("/acm");
        url.append('/').append(functionChooser.getSelectedItem());
        url.append('/').append(programField.getText());
        url.append("?version=").append(versionField.getText());
        url.append(paramString("name", nameField));
        url.append(paramString("computername", computernameField));
        url.append(paramString("key", keyField));
        url.append(paramString("filename", filenameField));
        url.append(paramString("comments", commentsField));

        JSONObject result;
        AwsInterface awsInterface = authenticator.getAwsInterface();
        if (functionChooser.getSelectedItem().equals("report")) {
            JSONObject requestBody = new JSONObject();
            requestBody.put("name", ACMConfiguration.getInstance().getUserName());
            requestBody.put("contact", ACMConfiguration.getInstance().getUserContact());
            requestBody.put("version", Constants.ACM_VERSION);
            requestBody.put("computername", computernameField.getText());
            requestBody.put("from", fromField.getText());
            JSONArray recipient = new JSONArray();
            recipient.addAll(Arrays.asList(toField.getText().split("[;,]")));
            requestBody.put("recipient", recipient);
            requestBody.put("subject", programField.getText());
            requestBody.put("body", commentsField.getText());
            requestBody.put("html", Boolean.TRUE);
            result = awsInterface.authenticatedPostCall(url.toString(), requestBody);
        } else {
            result = awsInterface.authenticatedGetCall(url.toString());
        }
        if (result != null) {
            results.setText(result.toJSONString());
        }
    }

    private JComponent layoutButtons() {
        Box hbox = Box.createHorizontalBox();
        hbox.setBorder(new EmptyBorder(4,10,4,8));
        hbox.add(Box.createHorizontalGlue());

        JButton okButton = new JButton("Ok");
        okButton.addActionListener(e -> onOk());
        hbox.add(okButton);
        hbox.add(Box.createHorizontalStrut(15));

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> setVisible(false));
        hbox.add(cancelButton);
        hbox.add(Box.createHorizontalStrut(15));

        return hbox;
    }

    @Override
    public void setBackground(Color bgColor) {
        // Workaround for weird bug in seaglass look&feel that causes a
        // java.awt.IllegalComponentStateException when e.g. a combo box
        // in this dialog is clicked on
        if (bgColor.getAlpha() == 0) {
            super.setBackground(backgroundColor);
        } else {
            super.setBackground(bgColor);
            backgroundColor = bgColor;
        }
    }

}
