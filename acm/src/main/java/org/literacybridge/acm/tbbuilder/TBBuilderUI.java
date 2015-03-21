package org.literacybridge.acm.tbbuilder;

import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.Calendar;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.literacybridge.acm.Constants;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.tbloader.TBLoader.Logger;

public class TBBuilderUI extends JFrame implements ActionListener {
	private static final String VERSION = "v1.00r1221";  
	private File dropbox;
	private JComboBox projectList;
	private JComboBox packageAList;
	private JComboBox packageBList;
	private TBBuilder tbBuilder;
	private JLabel notice;
	private JComboBox languageImgAList1;
	private JComboBox languageImgBList;

	public static void main(String[] args) throws Exception {
		new TBBuilderUI();
	}

	private void fillProjectList(JComboBox l) {
		l.addItem("Select Project");
		dropbox = new File(Constants.USER_HOME_DIR,Constants.DefaultSharedDirName1);
		// TODO: should be tied into ACMConfiguration.discoverDBs() -- but this means refactoring ACM init to not be DB-specific at first
		if (dropbox.exists()) {
			File[] dirs = dropbox.listFiles(new FileFilter() {
				@Override public boolean accept(File path) {
					return path.isDirectory() && path.getName().startsWith(TBBuilder.ACM_PREFIX);
				}
			});
			for (File d:dirs) {
				l.addItem(d.getName().substring(TBBuilder.ACM_PREFIX.length()));
			}
		}
	}
	
	private String[] getPackages(String project) {
		String[] packageNames=new String[10];
		File packages = new File(dropbox,TBBuilder.ACM_PREFIX+project+"/TB-Loaders/packages");
		File[] dirs = packages.listFiles(new FileFilter() {
			@Override public boolean accept(File path) {
				return path.isDirectory();
			}
		});
		int i=0;
		for (File d:dirs) {
			packageNames[i++]=d.getName();
		}
		return packageNames;
	}

	private String[] getLanguages(String project) {
		String[] languageCodes=new String[10];
		File packages = new File(dropbox,TBBuilder.ACM_PREFIX+project+"/TB-Loaders/TB_Options/languages");
		File[] dirs = packages.listFiles(new FileFilter() {
			@Override public boolean accept(File path) {
				return path.isDirectory();
			}
		});
		int i=0;
		for (File d:dirs) {
			languageCodes[i++]=d.getName();
		}
		return languageCodes;
	}

	
	public TBBuilderUI() throws Exception {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//this.addWindowListener(new WindowEventHandler());
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
					ACMConfiguration.closeCurrentDB();
			}
		});

		setTitle("TB-Builder: Single Image Per Deployment" + VERSION); 
//		JLabel dateLabel = new JLabel("Deployment Number");
//		JTextField deploymentNumberText = new JTextField("Deployment Number");
//		deploymentNumberText.setEditable(true);
		JPanel panel = new JPanel();

		notice = new JLabel("________");
		
		
		projectList = new JComboBox(); 
		fillProjectList(projectList);
		
		
		JComboBox updateYearList = new JComboBox();
		Calendar cal = Calendar.getInstance();
		int year = cal.get(Calendar.YEAR);
		updateYearList.addItem(String.valueOf(year));
		updateYearList.addItem(String.valueOf(year+1));
		
		JComboBox updateNumberList = new JComboBox();
		for (int i=1;i<10;i++) {
			updateNumberList.addItem(String.valueOf(i));
		}
		// TODO: allow for more than 2 images/packages; however we have never had a need for more than two,
		// so this isn't priority.
		packageAList = new JComboBox();
		packageBList = new JComboBox();
		languageImgAList1 = new JComboBox();
		JComboBox groupAList = new JComboBox();  // should be a JList to allow multiple groups in future

		languageImgBList = new JComboBox();  
		JComboBox groupBList = new JComboBox();  // should be a JList to allow multiple groups in future
		JButton buttonBuild = new JButton("Build");
		JButton buttonPublish = new JButton("Publish");

		JLabel projectLabel = new JLabel("Project");
		JLabel updateYearLabel = new JLabel("Update Year");
		JLabel updateNumberLabel = new JLabel("Update Number");
		JLabel packageALabel = new JLabel("Package A");
		JLabel languageALabel = new JLabel("Language A");
		JLabel groupALabel = new JLabel("Group A");
		JLabel packageBLabel = new JLabel("Package B");
		JLabel languageBLabel = new JLabel("Language B");
		JLabel groupBLabel = new JLabel("Group B");
		
		
		GroupLayout layout = new GroupLayout(panel);
		panel.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        layout.setHorizontalGroup(layout.createSequentialGroup()
        	.addGroup(layout.createParallelGroup(LEADING)
        			.addComponent(projectLabel)
        			.addComponent(updateYearLabel)
	           		.addComponent(updateNumberLabel)
	           		.addComponent(packageALabel)
	           		.addComponent(languageALabel)
	           		.addComponent(groupALabel)
	           		.addComponent(packageBLabel)
	           		.addComponent(languageBLabel)
	           		.addComponent(groupBLabel)
        			)
			.addGroup(layout.createParallelGroup(LEADING)
        			.addComponent(notice)
					.addComponent(projectList)
					.addComponent(updateYearList)
	        		.addComponent(updateNumberList)
	        		.addComponent(packageAList)
	        		.addComponent(languageImgAList1)
	        		.addComponent(groupAList)
	        		.addComponent(packageBList)
	        		.addComponent(languageImgBList)
	        		.addComponent(groupBList)
	        		.addComponent(buttonBuild)
	        		.addComponent(buttonPublish)
					)
				);
        layout.setVerticalGroup(layout.createSequentialGroup()
        		.addComponent(notice)
        		.addGroup(layout.createParallelGroup(BASELINE)
        				.addComponent(projectLabel)
        				.addComponent(projectList)
        				)
        		.addGroup(layout.createParallelGroup(BASELINE)
        				.addComponent(updateYearLabel)
        				.addComponent(updateYearList)
        				)
        		.addGroup(layout.createParallelGroup(BASELINE)
	        		.addComponent(updateNumberLabel)
	        		.addComponent(updateNumberList)
	        		)
        		.addGroup(layout.createParallelGroup(BASELINE)
	        		.addComponent(packageALabel)
	        		.addComponent(packageAList)
	        		)
        		.addGroup(layout.createParallelGroup(BASELINE)
	        		.addComponent(languageALabel)
	        		.addComponent(languageImgAList1)
	        		)
        		.addGroup(layout.createParallelGroup(BASELINE)
	        		.addComponent(groupALabel)
	        		.addComponent(groupAList)
	        		)
        		.addGroup(layout.createParallelGroup(BASELINE)
    				.addComponent(packageBLabel)
    				.addComponent(packageBList)
    				)
        		.addGroup(layout.createParallelGroup(BASELINE)
        			.addComponent(languageBLabel)
        			.addComponent(languageImgBList)
        			)
        		.addGroup(layout.createParallelGroup(BASELINE)
        				.addComponent(groupBLabel)
        				.addComponent(groupBList)
        			)
        		.addGroup(layout.createParallelGroup(BASELINE)
        				.addComponent(buttonBuild)
        			)
        		.addGroup(layout.createParallelGroup(BASELINE)
        				.addComponent(buttonPublish)
      				)
        		);
	
	    setSize(600,500);
	    add(panel, BorderLayout.CENTER);
		setLocationRelativeTo(null);
		setVisible(true);
		projectList.addActionListener(this);
	}

	
	
	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if (o instanceof JComboBox) {
			if (o==projectList) {
				if (projectList.getSelectedIndex() > 0) {
					if (tbBuilder == null) {
						try {
							notice.setText("STARTING DB"); 
							notice.setForeground(Color.RED);
							notice.setVisible(true);
							tbBuilder = new TBBuilder(TBBuilder.ACM_PREFIX + projectList.getSelectedItem().toString());
							notice.setText("");
							notice.setVisible(false);
						} catch (Exception e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} 
					} else {
						try {
							ACMConfiguration.setCurrentDB(TBBuilder.ACM_PREFIX + projectList.getSelectedItem().toString(), false);
						} catch (Exception e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
					String contentPackages[] = getPackages(projectList.getSelectedItem().toString());
					for (String s:contentPackages) {
						if (s==null)
							break;
						packageAList.addItem(s);
						packageBList.addItem(s);
					}
					String contentLanguages[] = getLanguages(projectList.getSelectedItem().toString());
					for (String s:contentLanguages) {
						if (s==null)
							break;
						languageImgAList1.addItem(s);
						languageImgBList.addItem(s);
					}
				}				
			}
		}
	}

}
/*

*/