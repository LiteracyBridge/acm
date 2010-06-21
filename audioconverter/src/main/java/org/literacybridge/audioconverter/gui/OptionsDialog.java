package org.literacybridge.audioconverter.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.literacybridge.audioconverter.converters.AnyToA18Converter;
import org.literacybridge.audioconverter.converters.BaseAudioConverter;

public class OptionsDialog {

	private JDialog optionsDialog = null;
	private JPanel contentPane = null;
	private JPanel backgroundPanel = null;
	private JLabel algorithmLabel = null;
	private JComboBox algorithmList = null;
	private JLabel BitRateLabel = null;
	private JTextField bitRateField = null;
	private JLabel sampleRateLabel = null;
	private JTextField sampleRateField = null;
	private JLabel headerLabel = null;
	private JComboBox headerList = null;
	private JPanel buttonPanel = null;
	private JButton cancelButton = null;
	private JButton okButton = null;
	private JLabel bpsLabel = null;
	private JLabel hzLabel = null;
	
	private static final String dialogTitle = "Audio Converter Options";
	
	private String[] mHeaderList = { "Yes", "No" };
	
	private String[] mAlgorithmList = { "A1600", "A1800", "A3600" };
	
	private int mHeaderIdx;
	private int mAlgorithmIdx;
	private String mBitRate;
	private String mSampleRate;
	
	private boolean mConvertToA18 = false;
	
	public OptionsDialog (Frame owner) {
		mHeaderIdx = 1;
		mAlgorithmIdx = 1;
		mBitRate = "16000";
		mSampleRate = "16000";
		
		if (optionsDialog == null) {
			optionsDialog = new JDialog(owner, dialogTitle);
			optionsDialog.setSize(new Dimension(315, 250));
			optionsDialog.setContentPane(getContentPane());
			optionsDialog.setLocationRelativeTo(owner);
	        optionsDialog.pack();
		}
	}
	
	public void show(boolean convertToA18) {
		mConvertToA18 = convertToA18;
		algorithmList.setEnabled(convertToA18);
		headerList.setEnabled(convertToA18);
		algorithmList.setSelectedIndex(mAlgorithmIdx);
		headerList.setSelectedIndex(mHeaderIdx);
		bitRateField.setText(mBitRate);
		sampleRateField.setText(mSampleRate);
		optionsDialog.setVisible(true);
	}
	
	public void show() {
		show(false);
	}
	
	private String getAlgorithmValue() {
		return mAlgorithmList[mAlgorithmIdx];
	}
	
	private String getHeaderValue() {
		return mHeaderList[mHeaderIdx];
	}
	
	private String getBitRateValue() {
		return mBitRate;
	}
	
	private String getSampleRateValue() {
		return mSampleRate;
	}
	
	public Map<String, String> getParameterList() {
		Map<String, String> parameters = new LinkedHashMap<String, String>();
		parameters.put(BaseAudioConverter.BIT_RATE, getBitRateValue());
		parameters.put(BaseAudioConverter.SAMPLE_RATE, getSampleRateValue());
                parameters.put(AnyToA18Converter.USE_HEADER, getHeaderValue());
                parameters.put(AnyToA18Converter.ALGORITHM, getAlgorithmValue());
		return parameters;
	}
	
	private JPanel getContentPane() {
		if (contentPane == null) {
			BorderLayout borderLayout = new BorderLayout();
			borderLayout.setHgap(0);
			borderLayout.setVgap(0);
			contentPane = new JPanel();
			contentPane.setLayout(borderLayout);
			contentPane.add(getBackgroundPanel(), BorderLayout.CENTER);
			contentPane.add(getButtonPanel(), BorderLayout.SOUTH);
		}
		return contentPane;
	}

	private JPanel getBackgroundPanel() {
		if (backgroundPanel == null) {
			GridBagConstraints gridBagConstraints31 = new GridBagConstraints();
			gridBagConstraints31.gridx = 2;
			gridBagConstraints31.fill = GridBagConstraints.VERTICAL;
			gridBagConstraints31.anchor = GridBagConstraints.WEST;
			gridBagConstraints31.gridy = 2;
			hzLabel = new JLabel();
			hzLabel.setText("Hz");
			GridBagConstraints gridBagConstraints21 = new GridBagConstraints();
			gridBagConstraints21.gridx = 2;
			gridBagConstraints21.anchor = GridBagConstraints.WEST;
			gridBagConstraints21.fill = GridBagConstraints.VERTICAL;
			gridBagConstraints21.insets = new Insets(0, 0, 0, 0);
			gridBagConstraints21.gridy = 1;
			bpsLabel = new JLabel();
			bpsLabel.setText("bps");
			GridBagConstraints gridBagConstraints7 = new GridBagConstraints();
			gridBagConstraints7.fill = GridBagConstraints.VERTICAL;
			gridBagConstraints7.gridy = 3;
			gridBagConstraints7.weightx = 1.0;
			gridBagConstraints7.anchor = GridBagConstraints.WEST;
			gridBagConstraints7.insets = new Insets(5, 15, 5, 0);
			gridBagConstraints7.ipadx = 10;
			gridBagConstraints7.gridx = 1;
			GridBagConstraints gridBagConstraints6 = new GridBagConstraints();
			gridBagConstraints6.gridx = 0;
			gridBagConstraints6.fill = GridBagConstraints.VERTICAL;
			gridBagConstraints6.anchor = GridBagConstraints.EAST;
			gridBagConstraints6.gridy = 3;
			headerLabel = new JLabel();
			headerLabel.setText("Header:");
			GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
			gridBagConstraints5.fill = GridBagConstraints.BOTH;
			gridBagConstraints5.gridy = 2;
			gridBagConstraints5.weightx = 1.0;
			gridBagConstraints5.insets = new Insets(5, 15, 5, 15);
			gridBagConstraints5.gridx = 1;
			GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
			gridBagConstraints4.gridx = 0;
			gridBagConstraints4.anchor = GridBagConstraints.EAST;
			gridBagConstraints4.fill = GridBagConstraints.VERTICAL;
			gridBagConstraints4.gridy = 2;
			sampleRateLabel = new JLabel();
			sampleRateLabel.setText("Sample rate:");
			GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
			gridBagConstraints3.fill = GridBagConstraints.BOTH;
			gridBagConstraints3.gridy = 1;
			gridBagConstraints3.weightx = 1.0;
			gridBagConstraints3.anchor = GridBagConstraints.WEST;
			gridBagConstraints3.insets = new Insets(5, 15, 5, 15);
			gridBagConstraints3.gridwidth = 1;
			gridBagConstraints3.gridx = 1;
			GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
			gridBagConstraints2.gridx = 0;
			gridBagConstraints2.anchor = GridBagConstraints.EAST;
			gridBagConstraints2.insets = new Insets(0, 0, 0, 0);
			gridBagConstraints2.fill = GridBagConstraints.VERTICAL;
			gridBagConstraints2.gridy = 1;
			BitRateLabel = new JLabel();
			BitRateLabel.setText("Bit rate:");
			GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
			gridBagConstraints1.fill = GridBagConstraints.VERTICAL;
			gridBagConstraints1.gridy = 0;
			gridBagConstraints1.weightx = 1.0;
			gridBagConstraints1.anchor = GridBagConstraints.WEST;
			gridBagConstraints1.insets = new Insets(5, 15, 5, 15);
			gridBagConstraints1.ipadx = 10;
			gridBagConstraints1.gridx = 1;
			GridBagConstraints gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.anchor = GridBagConstraints.EAST;
			gridBagConstraints.gridwidth = 1;
			gridBagConstraints.insets = new Insets(0, 0, 0, 0);
			gridBagConstraints.fill = GridBagConstraints.VERTICAL;
			gridBagConstraints.gridy = 0;
			algorithmLabel = new JLabel();
			algorithmLabel.setText("Algorithm:");
			backgroundPanel = new JPanel();
			backgroundPanel.setLayout(new GridBagLayout());
			backgroundPanel.setBorder(
	                BorderFactory.createCompoundBorder(
	                                BorderFactory.createTitledBorder("Options"),
	                                BorderFactory.createEmptyBorder(5,5,5,5)));
			backgroundPanel.add(algorithmLabel, gridBagConstraints);
			backgroundPanel.add(getAlgorithmList(), gridBagConstraints1);
			backgroundPanel.add(BitRateLabel, gridBagConstraints2);
			backgroundPanel.add(getBitRateField(), gridBagConstraints3);
			backgroundPanel.add(sampleRateLabel, gridBagConstraints4);
			backgroundPanel.add(getSampleRateField(), gridBagConstraints5);
			backgroundPanel.add(headerLabel, gridBagConstraints6);
			backgroundPanel.add(getHeaderList(), gridBagConstraints7);
			backgroundPanel.add(bpsLabel, gridBagConstraints21);
			backgroundPanel.add(hzLabel, gridBagConstraints31);
		}
		return backgroundPanel;
	}

	private JComboBox getAlgorithmList() {
		if (algorithmList == null) {
			algorithmList = new JComboBox(mAlgorithmList);
			algorithmList.setSelectedIndex(mAlgorithmIdx);
		}
		return algorithmList;
	}

	private JTextField getBitRateField() {
		if (bitRateField == null) {
			bitRateField = new JTextField();
			bitRateField.setText(mBitRate);
		}
		return bitRateField;
	}

	private JTextField getSampleRateField() {
		if (sampleRateField == null) {
			sampleRateField = new JTextField();
			sampleRateField.setText(mSampleRate);
		}
		return sampleRateField;
	}

	private JComboBox getHeaderList() {
		if (headerList == null) {
			headerList = new JComboBox(mHeaderList);
			headerList.setSelectedIndex(mHeaderIdx);
		}
		return headerList;
	}

	private JPanel getButtonPanel() {
		if (buttonPanel == null) {
			FlowLayout flowLayout = new FlowLayout();
			flowLayout.setAlignment(java.awt.FlowLayout.RIGHT);
			buttonPanel = new JPanel();
			buttonPanel.setLayout(flowLayout);
			buttonPanel.add(getOkButton(), null);
			buttonPanel.add(getCancelButton(), null);
		}
		return buttonPanel;
	}

	private JButton getCancelButton() {
		if (cancelButton == null) {
			cancelButton = new JButton();
			cancelButton.setText("Cancel");
			cancelButton.addActionListener(
					new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							optionsDialog.setVisible(false);
						}						
					});
		}
		return cancelButton;
	}

	private JButton getOkButton() {
		if (okButton == null) {
			okButton = new JButton();
			okButton.setText("Ok");
			okButton.addActionListener(
					new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							mHeaderIdx = getHeaderList().getSelectedIndex();
							mAlgorithmIdx = getAlgorithmList().getSelectedIndex();
							mBitRate = getBitRateField().getText();
							mSampleRate = getSampleRateField().getText();
							optionsDialog.setVisible(false);
						}						
					});			
		}
		return okButton;
	}

}
