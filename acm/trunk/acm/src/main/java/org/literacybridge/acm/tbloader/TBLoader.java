package org.literacybridge.acm.tbloader;

import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileSystemView;

import org.jdesktop.swingx.JXDatePicker;
// commenting out import below so that TBLoader can stand-alone as .class 
// until new ACM is running on Fidelis's laptop
//import org.literacybridge.acm.utils.OSChecker;

//import org.jdesktop.swingx.JXDatePicker;

@SuppressWarnings("serial")
public class TBLoader extends JFrame implements ActionListener {
	private static final String VERSION = "v1.10r1123";   // inclusion of flash stats TBInfo class
	private static final String END_OF_INPUT = "\\Z";
	private static final String COLLECTION_SUBDIR = "\\collected-data";
	private static String TEMP_COLLECTION_DIR = "";
	private static String TEMP_COLLECTION_DIR2 = "";
	private static final String SW_SUBDIR = ".\\software\\";
	private static final String CONTENT_SUBDIR = ".\\content\\";
	private static final String CONTENT_BASIC_SUBDIR = "basic\\";
	private static final String COMMUNITIES_SUBDIR = "options\\communities\\";
	private static final String SCRIPT_SUBDIR = SW_SUBDIR + "scripts\\";
	private static final String NO_SERIAL_NUMBER = "(not found)";
	private static final String NO_DRIVE = "(nothing connected)";
	private static final String TRIGGER_FILE_CHECK = "checkdir";
	private static String tblRevision = "(no rev)"; 
	private static String dateRotation;
	private JComboBox packageList;
	private JComboBox communityList;
	private JComboBox currentLocationList;
	private JComboBox driveList;
	private JTextField id;
	private JTextField idnext;
	private JTextField oldRevisionText;
	private JTextField newRevisionText;
	private JTextField oldPackageValue;
	private JTextField oldCommunityValue;
	private JTextField lastUpdatedText;
	private JLabel oldValue;
	private JLabel newValue;
	private static JTextArea status;
	private static JTextArea status2;
	private static String homepath;
	private static JButton update;
//	private static JButton reformat;
//	private static JButton backup;
	private static JButton xfer;
	private static JButton grabStatsOnly;
	private static JButton setCommunity;
	private static String copyTo;
	private static String revision;
	public static String packageName;
	public static String sourcePackage;
	private JCheckBox fetchIDFromServer;
	private JCheckBox handIcons;
	TBInfo tbStats;
	
	class WindowEventHandler extends WindowAdapter {
		public void windowClosing(WindowEvent evt) {
			checkDirUpdate();
			Logger.LogString("closing app");
			Logger.close();
		    System.exit(0);
		}
	}
	
	private String currentLocation[] = new String[] {
			"Select location",
			"Community",
			"Jirapa office",
			"Wa office",
			"Other"
	};
	
	private String communityNames[] = new String[] {
			"Non-specific",
			"Baazu-Jirapa",
			"Behee-Wa Municipal",
			"Die-Jirapa",
			"Duori Degri-Jirapa",
			"Gozu-Jirapa",
			"Gyangvuuri-Jirapa",
			"Jeffiri-Jirapa",
			"Jonga-Wa Municipal",
			"Nyeni-Jirapa",
			"Saabaalong-Jirapa",
			"Saawie-Jirapa",
			"Tugo Yagra-Jirapa",
			"Ul-Kpong-Jirapa",
			"Ving-Ving-Jirapa",
			"Yibile-Jirapa",
			"Zengpeni-Jirapa"
	};

	void getRevisionNumbers() {
		revision = "(No firmware)";

		File basicContentPath = new File(CONTENT_SUBDIR + packageList.getSelectedItem().toString() + "\\" + CONTENT_BASIC_SUBDIR);

		try {
			File[] files;
			if (basicContentPath.exists()) {
				// get Package
				files = basicContentPath.listFiles(new FilenameFilter() {
					@Override public boolean accept(File dir, String name) {
						String lowercase = name.toLowerCase();
						return lowercase.endsWith(".img");
					}
				});				
				if (files.length > 1)
					revision = "(Multiple Firmwares!)";
				else if (files.length == 1) {
					revision = files[0].getName();
					revision = revision.substring(0, revision.length() - 4);
				}
				newRevisionText.setText(revision);

/*				// get Package name
				File contentSystem = new File(basicContentPath,"system");
				files = contentSystem.listFiles(new FilenameFilter() {
					@Override public boolean accept(File dir, String name) {
						String lowercase = name.toLowerCase();
						return lowercase.endsWith(".pkg");
					}
				});				
				if (files.length > 1)
					sourcePackage = "(Multiple Pkg Names!)";
				else if (files.length == 1) {
					sourcePackage = files[0].getName();
					sourcePackage = sourcePackage.substring(0, sourcePackage.length() - 4);
				}
*/			}
		} catch (Exception ignore) {
			Logger.LogString("exception - ignore and keep going with default string");
		}

	}

	public TBLoader() throws Exception {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.addWindowListener(new WindowEventHandler());
		// get TBLoader revision
		File swPath = new File(".");
		File[] files = swPath.listFiles(new FilenameFilter() {
			@Override public boolean accept(File dir, String name) {
				String lowercase = name.toLowerCase();
				return lowercase.endsWith(".rev");
			}
		});				
		if (files.length > 1)
			tblRevision = "(Multiple TBL Revisions!)";
		else if (files.length == 1) {
			tblRevision = files[0].getName();
			tblRevision = tblRevision.substring(0, tblRevision.length() - 4);
		}
		setTitle("TB-Loader " + VERSION + "/" + tblRevision); 
		//"   Update:" + packageName + "   Firmware:" + revision);
		
		JPanel panel = new JPanel();
		
		JLabel packageLabel = new JLabel("Update:");
		JLabel communityLabel = new JLabel("Community:");
		JLabel currentLocationLabel = new JLabel("Current Location:");
		JLabel dateLabel = new JLabel("First Rotation Date:");
		oldPackageValue = new JTextField();
		oldPackageValue.setEditable(false);
		oldValue = new JLabel("Previous");
		newValue = new JLabel("Next");
		lastUpdatedText = new JTextField();
		lastUpdatedText.setEditable(false);
		oldCommunityValue = new JTextField();
		oldCommunityValue.setEditable(false);
		JLabel deviceLabel = new JLabel("Talking Book Device:");
		JLabel idLabel = new JLabel("Serial number:");
		JLabel revisionLabel = new JLabel("Firmware:");
		status = new JTextArea("STATUS: Ready");
		status.setEditable(false);
		status.setLineWrap(true);
		status2 = new JTextArea(2,40);
		status2.setEditable(false);
		status2.setLineWrap(true);
		id = new JTextField();
		id.setEditable(false);
		idnext = new JTextField();
		idnext.setEditable(false);
		oldRevisionText = new JTextField();
		oldRevisionText.setEditable(false);
		newRevisionText = new JTextField();
		newRevisionText.setEditable(false);
		final JXDatePicker datePicker = new JXDatePicker();
		datePicker.getEditor().setEditable(false);
		datePicker.setFormats(new String[] { "yyyy/MM/dd" }); //dd MMM yyyy
		datePicker.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dateRotation = datePicker.getDate().toString();
			}	
		});
		 	
		packageList = new JComboBox();
		packageList.addActionListener(this);
		communityList = new JComboBox();
		communityList.addActionListener(this);
		driveList = new JComboBox();
		driveList.addActionListener(this);
		currentLocationList = new JComboBox(currentLocation);		
		fetchIDFromServer = new JCheckBox("Get new serial number");
		fetchIDFromServer.setSelected(false);
		handIcons = new JCheckBox("Use hand icon msgs");
		handIcons.setSelected(false);
		update = new JButton("Update TB");
		update.addActionListener(this);
		grabStatsOnly = new JButton("Get Stats");
		grabStatsOnly.addActionListener(this);
//		reformat = new JButton("Reformat");
//		reformat.addActionListener(this);
//		backup = new JButton("Backup");
//		backup.addActionListener(this);
		xfer = new JButton("Upload Audio");
		xfer.addActionListener(this);
		setCommunity = new JButton("Set Community");
		setCommunity.addActionListener(this);
		
		
		GroupLayout layout = new GroupLayout(panel);
		panel.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setHorizontalGroup(layout.createSequentialGroup()
	        		.addGroup(layout.createParallelGroup(LEADING)
	        				.addComponent(deviceLabel)
	        				.addComponent(currentLocationLabel)
	        				.addComponent(packageLabel)
	        				.addComponent(communityLabel)
	        				.addComponent(dateLabel)
	                		.addComponent(revisionLabel)
	        				.addComponent(idLabel)
	        				)
 					.addGroup(layout.createParallelGroup(LEADING)
							.addComponent(driveList)
							.addComponent(currentLocationList)
            				.addComponent(newValue)
            				.addComponent(packageList)
	                		.addComponent(communityList)
							.addComponent(datePicker)
	                		.addComponent(newRevisionText)
	                		.addComponent(id)
            				.addComponent(fetchIDFromServer)
	    	                .addGroup(layout.createSequentialGroup()
	                				.addComponent(update)
	    	        				.addComponent(grabStatsOnly))
	    	    		     .addComponent(status2)
	    	        		)
 					.addGroup(layout.createParallelGroup(LEADING)
            				.addComponent(oldValue)
	                		.addComponent(oldPackageValue)
	                		.addComponent(oldCommunityValue)
	                		.addComponent(lastUpdatedText)
	                		.addComponent(oldRevisionText)
	                		.addComponent(idnext)
	                		.addComponent(handIcons)
	    	                .addGroup(layout.createSequentialGroup()
	    	        				.addComponent(setCommunity)
	                 				.addComponent(xfer)
              				)
	    		    		.addComponent(status)
	                  	)
	               	);
        
        layout.setVerticalGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(BASELINE)
        				.addComponent(deviceLabel)
                		.addComponent(driveList))
                .addGroup(layout.createParallelGroup(BASELINE)
                		.addComponent(currentLocationLabel)
                		.addComponent(currentLocationList))
                .addGroup(layout.createParallelGroup(BASELINE)
        				.addComponent(newValue)
                		.addComponent(oldValue))
                .addGroup(layout.createParallelGroup(BASELINE)
                        .addComponent(packageLabel)
                        .addComponent(packageList)
	    	            .addComponent(oldPackageValue))
                .addGroup(layout.createParallelGroup(BASELINE)
                		.addComponent(communityLabel)
                        .addComponent(communityList)
                		.addComponent(oldCommunityValue))
                .addGroup(layout.createParallelGroup(BASELINE)
                		.addComponent(dateLabel)
                        .addComponent(datePicker)
                		.addComponent(lastUpdatedText))
                .addGroup(layout.createParallelGroup(BASELINE)
                		.addComponent(revisionLabel)
	    	            .addComponent(newRevisionText)
	    	            .addComponent(oldRevisionText))
                .addGroup(layout.createParallelGroup(BASELINE)
                		.addComponent(idLabel)
                		.addComponent(id)
                		.addComponent(idnext))
		        .addGroup(layout.createParallelGroup(BASELINE)
		        	.addComponent(fetchIDFromServer)
    				.addComponent(handIcons))
    		    .addGroup(layout.createParallelGroup(BASELINE)
    		    	.addComponent(update)
    		    	.addComponent(grabStatsOnly)
    		    	.addComponent(setCommunity)
		        	.addComponent(xfer)
//    	    		.addComponent(backup)
//    	    		.addComponent(reformat)
    	    		)
    		    .addGroup(layout.createParallelGroup(BASELINE)
    		    		.addComponent(status2)
    		    		.addComponent(status))
            );
        
        setSize(600,500);
        add(panel, BorderLayout.CENTER);
//      add(status, BorderLayout.SOUTH);
//      add(xfer, BorderLayout.EAST);
		setLocationRelativeTo(null);
		
		setCopyToPath();
		Logger.init();
		resetUI(true);
		Logger.LogString("about to set visibility");
		setVisible(true);
		Logger.LogString("set visibility");
		Logger.LogString("starting drive monitoring");
		deviceMonitorThread.setDaemon(true);
		deviceMonitorThread.start();
		startUpDone = true;
		JOptionPane.showMessageDialog(null, "Remember to power Talking Book with batteries before connecting with USB.",
                "Use Batteries!", JOptionPane.DEFAULT_OPTION);
	}
	
	public static boolean startUpDone = false;
	public static boolean monitoringDrive = false;
	public static boolean updatingTB = false;
	
	public static void main(String[] args) throws Exception {
/*		if (args.length == 0) {
			System.out.println("Usage: java -jar device_registration.jar [window title]");
		} else {
			File f = new File(args[0]);
			new TBLoader();
		} 
*/
		new TBLoader();
	}

	private static String getDateTime() {
		Calendar cal = Calendar.getInstance();
		int month = cal.get(Calendar.MONTH) + 1	;
		int date = cal.get(Calendar.DAY_OF_MONTH);
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int min = cal.get(Calendar.MINUTE);
		int sec = cal.get(Calendar.SECOND);
		String dateTime = String.valueOf(month) + "m" + String.valueOf(date) + "d" + 
				String.valueOf(hour) + "h" + String.valueOf(min) + "m" + String.valueOf(sec) + "s";
		return dateTime;
	}
	
	private static String getLogFileName() {
		String filename;
		File f;
		
		filename = copyTo + "\\logs";
		f = new File(filename);
		if (!f.exists())
			f.mkdirs();
		filename += "\\log-" + getDateTime()+".txt";
		return filename;
	}
	private void setCopyToPath() {
		int i = 0;
		final int MAX_PATHS = 5;
		String paths[]= new String[MAX_PATHS];

		try {
			Process proc = Runtime.getRuntime().exec("cmd /C echo %HOMEDRIVE%%HOMEPATH%");
			BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			homepath = new String(reader.readLine());
			reader.close();
			TEMP_COLLECTION_DIR = new String(homepath + "\\LiteracyBridge");
			TEMP_COLLECTION_DIR2 = new String(homepath + "\\TB-Loader");
			Logger.LogString("Temp Collection Dir: " + TEMP_COLLECTION_DIR);
			File f = new File(TEMP_COLLECTION_DIR);
			f.mkdirs();
			// now that local path is established, the 'collected-data' subdir is what should 
			// only be present when there is local storage (the xfer button is only enabled if it's there
			TEMP_COLLECTION_DIR += COLLECTION_SUBDIR; 
			TEMP_COLLECTION_DIR2 += COLLECTION_SUBDIR; 
			reader = new BufferedReader(new FileReader(new File(SW_SUBDIR + "paths.txt")));
			copyTo = "";
			while (reader.ready() && i < MAX_PATHS) {
				paths[i] = reader.readLine().replaceAll("%HOMEPATH%", Matcher.quoteReplacement(homepath));
				Logger.LogString(paths[i]);
				if ((new File(paths[i])).exists()) {
					copyTo = paths[i];
					Logger.LogString("GOOD PATH = "+copyTo);
					break;						
				}
			}
			reader.close();
		} catch (Exception e) {
			Logger.LogString(e.toString());
			Logger.init();
			e.printStackTrace();
		}
		if (copyTo =="") {
			Logger.LogString("***No Dropbox path not found. Storing all files locally.");
			copyTo = TEMP_COLLECTION_DIR;
		}
		else {
			copyTo = copyTo += COLLECTION_SUBDIR;
			new File(copyTo).mkdir();  // creates COLLECTION_SUBDIR if good path is found
		}
		Logger.LogString("copyTo:"+copyTo);
	}
	
	int idCounter = 0;
	
	private String fetchNextDeviceID() throws Exception {
		try {
			if (!debug) {
				URL url = new URL("http://literacybridge.org/TBsrns.php");
				InputStream in = url.openStream();
				Scanner scanner = new Scanner(in);
			    scanner.useDelimiter(END_OF_INPUT);
			    String json = scanner.next();
			    return json;
			} else {
				return "TB-" + idCounter++;
			}
		} catch (Exception ex){
			Logger.LogString("Tried to get a new serial number, but the Internet does not seem to be available.\nPlease check your internet connection if you need to get a new serial number for this device.");
			Logger.LogString(ex.toString());
			ex.printStackTrace();
			JOptionPane.showMessageDialog(this, "Tried to get a new serial number, but the Internet does not seem to be available.\nPlease check your internet connection if you need to get a new serial number for this device.",
	                "Error", JOptionPane.ERROR_MESSAGE);
			throw ex;
		}
	}
	
	private File prevSelected = null;
	private int prevSelectedCommunity = -1;
	
	private void getStatsFromCurrentDrive() throws IOException {
		DriveInfo di = (DriveInfo) driveList.getSelectedItem();
		if (di.drive == null)
			return;
		File rootPath = new File(di.drive.getAbsolutePath());
		File statsPath = new File(rootPath,"statistics/stats/flashData.bin");
		tbStats = new TBInfo(statsPath.toString());
	}
		
	private String getCommunityFromCurrentDrive() {
		String communityName = communityNames[0];
		DriveInfo di = (DriveInfo) driveList.getSelectedItem();
		if (di.drive == null) {
			return communityName;
		}
		File rootPath = new File(di.drive.getAbsolutePath());
		File systemPath = new File(di.drive.getAbsolutePath(), "system");
		try {
			File[] files;
			// get Location file info
			// check root first, in case device was just assigned a new community (e.g. from this app)
			files = rootPath.listFiles(new FilenameFilter() {
				@Override public boolean accept(File dir, String name) {
					String lowercase = name.toLowerCase();
					return lowercase.endsWith(".loc");
				}
			});
			if (files == null) {
				Logger.LogString("This does not look like a TB: " + rootPath);
				
			}
			else if (files.length == 1) {
				String locFileName = files[0].getName();
				communityName = locFileName.substring(0, locFileName.length() - 4);
			}
			else if (files.length == 0 && systemPath.exists()) {
				// get Location file info
				files = systemPath.listFiles(new FilenameFilter() {
					@Override public boolean accept(File dir, String name) {
						String lowercase = name.toLowerCase();
						return lowercase.endsWith(".loc");
					}
				});				
				if (files.length == 1) {
					String locFileName = files[0].getName();
					communityName = locFileName.substring(0, locFileName.length() - 4);
				}
			} 
		} catch (Exception ignore) {
			Logger.LogString(ignore.toString());
			Logger.init();
			ignore.printStackTrace();	
			// ignore and keep going with empty string
		}
		Logger.LogString("TB's current community name is "+communityName);
		return communityName;
	}
	
	private synchronized void fillPackageList() {

		int indexSelected = -1;
		File contentPath = new File (CONTENT_SUBDIR);
		packageList.removeAllItems();
		File[] packageFolder = contentPath.listFiles();
		for (int i = 0; i<packageFolder.length; i++) {
			packageList.addItem(packageFolder[i].getName());
			if (tblRevision.startsWith(packageFolder[i].getName())) {
				indexSelected = i;
			}
		}
		if (indexSelected != -1) {
			packageList.setSelectedIndex(indexSelected);
		}
	}

	private synchronized void fillCommunityList() throws IOException {
		
		communityList.removeAllItems();
		for (int i=0; i < communityNames.length; i++) {
			communityList.addItem(communityNames[i]);
		}
		setCommunityList();
		if (communityList.getSelectedIndex() == 6 ||communityList.getSelectedIndex() == 8) 
			this.handIcons.setSelected(true);
		else
			this.handIcons.setSelected(false);
	}

	private synchronized void setCommunityList() throws IOException {
		String driveCommunity;
		int communityMatchIndex = -1;

		getStatsFromCurrentDrive();
		driveCommunity = getCommunityFromCurrentDrive();
		if (tbStats != null && tbStats.location != null && !tbStats.location.equals(""))
			oldCommunityValue.setText(tbStats.location);
		else
			oldCommunityValue.setText(driveCommunity);
		
		if (prevSelectedCommunity != -1)
			communityMatchIndex = prevSelectedCommunity;
		else {
			communityList.removeAllItems();
			for (int i=0; i < communityNames.length; i++) {
				communityList.addItem(communityNames[i]);
				if (communityNames[i].equalsIgnoreCase(driveCommunity))
					communityMatchIndex = i;
			}
		}
		communityList.setSelectedIndex(communityMatchIndex);
	}

	private synchronized void setSNandRevFromCurrentDrive() {
		String sn="";
		String rev="unknown";
		String pkg="unknown";
		DriveInfo di;
		if (driveList == null)
			return;
		di= (DriveInfo) driveList.getSelectedItem();
		if (di == null || di.drive == null)
			return;
		File systemPath = new File(di.drive.getAbsolutePath(), "system");
		try {
			File[] files;
			if (systemPath.exists()) {
				// get Serial Number file info
				
				if (tbStats != null && tbStats.serialNumber != null && !tbStats.serialNumber.equals(""))
					sn = tbStats.serialNumber;
				else {
					files = systemPath.listFiles(new FilenameFilter() {
						@Override public boolean accept(File dir, String name) {
							String lowercase = name.toLowerCase();
							return lowercase.endsWith(".srn") && !lowercase.startsWith("-erase");
						}
					});				
					if (files.length > 0) {
						String tsnFileName = files[0].getName();
						sn = tsnFileName.substring(0, tsnFileName.length() - 4);
		//				if (sn.startsWith("srn."))
		//					sn = sn.substring(4);
					}
				}	
				// get Revision number from .rev or .img file
				files = systemPath.listFiles(new FilenameFilter() {
					@Override public boolean accept(File dir, String name) {
						String lowercase = name.toLowerCase();
						return lowercase.endsWith(".img") || lowercase.endsWith(".rev") ;
					}
				});
				
				rev = "unknown";
				for (int i= 0; i<files.length;i++) {
					String revFileName = files[i].getName();
					revFileName = revFileName.substring(0, revFileName.length() - 4);
					//System.out.println(revFileName);
					if (i == 0)
						rev = revFileName;
					else if (!rev.equalsIgnoreCase(revFileName)) {
						break;
					} 
				}
				if (rev.length() == 0)
					rev = "unknown";  // eliminate problem of zero length filenames being inserted into batch statements
			}
			if (tbStats != null && tbStats.contentPackage != null && !tbStats.contentPackage.equals("")) 
				pkg = tbStats.contentPackage;
			else {
				// get packaage name from .pkg file
				files = systemPath.listFiles(new FilenameFilter() {
					@Override public boolean accept(File dir, String name) {
						String lowercase = name.toLowerCase();
						return lowercase.endsWith(".pkg");
					}
				});
				if (files.length == 1) {
					pkg = files[0].getName().substring(0, files[0].getName().length() - 4);
				}
			}
			oldPackageValue.setText(pkg);

			
			// get last updated date from file
			if (tbStats != null && tbStats.updateDate != -1) 
				lastUpdatedText.setText(tbStats.updateYear + "/" + tbStats.updateMonth + "/" + tbStats.updateDate); 
			else {
				files = systemPath.listFiles(new FilenameFilter() {
					@Override public boolean accept(File dir, String name) {
						String lowercase = name.toLowerCase();
						return lowercase.equals("last_updated.txt");
					}
				});
				if (files.length == 1) {
					try{
					  FileInputStream fstream = new FileInputStream(files[0]);
					  DataInputStream in = new DataInputStream(fstream);
					  BufferedReader br = new BufferedReader(new InputStreamReader(in));
					  String strLine;
					  if ((strLine = br.readLine()) != null)   {
						  int m = strLine.indexOf('m');
						  int d = strLine.indexOf('d');
						  lastUpdatedText.setText("2013/" + strLine.substring(1,m) + "/" + strLine.substring(m+1,d));
					  }
					  //Close the input stream
					  in.close();
					  } catch (Exception e){//Catch exception if any
					  System.err.println("Error: " + e.getMessage());
					}					 
				}
			}			
		} catch (Exception ignore) {
			Logger.LogString("exception - ignore and keep going with empty string");
		}
		if (sn.equals("")) {
			sn = NO_SERIAL_NUMBER;
		}
		id.setText(sn);
		idnext.setText(sn);
		di.serialNumber = sn;
		di.revision = rev;
		oldRevisionText.setText(rev);
		if (di.serialNumber.equals("UNKNOWN") || di.serialNumber.equals(NO_SERIAL_NUMBER)) {
			this.fetchIDFromServer.setSelected(true);			
		}
	}	

	private synchronized void fillList(File[] roots) {
		driveList.removeAllItems();
		int index = -1;
		int i = 0;
		for (File root : roots) {
			if (root.getAbsoluteFile().toString().compareTo("D:") >= 0 && root.listFiles() != null) {
				String label = FileSystemView.getFileSystemView().getSystemDisplayName(root);
				if (label.trim().equals("CD Drive"))
					continue;
				driveList.addItem(new DriveInfo(root, label));
				if (prevSelected != null && root.getAbsolutePath().equals(prevSelected.getAbsolutePath())) {
					index = i;
				} else if (label.startsWith("TB") || label.startsWith("ECHO"))
					index = i;
				i++;
			}
		}
		if (driveList.getItemCount() == 0) {
			Logger.LogString("No drives");
			driveList.addItem(new DriveInfo(null,NO_DRIVE));
			index = 0;
		}
			
		if (index == -1) {
			index = i-1;
		}
		if (index != -1) {
			driveList.setSelectedIndex(index);
		}
	}

	private synchronized File[] getRoots() {
		File[] roots = null;
		// changing line below to allow TBLoader to run as a single .class file 
		// (until new ACM version is running on Fidelis's laptop)
		if (System.getProperty("os.name").startsWith("Windows")) { // (OSChecker.WINDOWS) {
			roots = File.listRoots();
		} else if (System.getProperty("os.name").startsWith("Mac OS")) { //(OSChecker.MAC_OS) {
			roots = new File("/Volumes").listFiles();
		}
		return roots;
	}
	
	private Thread deviceMonitorThread = new Thread() {
		@Override public void run() {
			Set<String> oldList = new HashSet<String>();
			
			while (true) { // don't do this during a TB update

				if (!updatingTB) {
					File[] roots = getRoots();
					
					boolean refresh = oldList.size() != roots.length;
					if (!refresh) {
						for (File root : roots) {
							if (!oldList.contains(root.getAbsolutePath())) {
								refresh = true;
								break;
							}
						}
					}
				
					if (refresh) {
						monitoringDrive = true;
						Logger.LogString("deviceMonitor sees new drive");
						fillList(roots);
	//					Logger.LogString("monitor filled drive list");
						try {
							fillCommunityList();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
	//					Logger.LogString("monitor filled community list");
						setSNandRevFromCurrentDrive();
						getRevisionNumbers();
	//					Logger.LogString("monitor got SN");
						refreshUI();
	//					Logger.LogString("monitor refreshed UI");
						oldList.clear();
						for (File root : roots) {
							oldList.add(root.getAbsolutePath());
						}
						monitoringDrive = false;
	//					Logger.LogString("monitor updates complete");
					}
				}
				try {
					sleep(2000);
				} catch (InterruptedException e) {
					Logger.LogString(e.toString());
					Logger.init();
					throw new RuntimeException(e);
				}

			}
		}
	};

	private void logTBData(String action) {
		BufferedWriter bw;
		Date d = new Date();
		Calendar cal = Calendar.getInstance();
		int month = cal.get(Calendar.MONTH) + 1	;
		int date = cal.get(Calendar.DAY_OF_MONTH);
		int year = cal.get(Calendar.YEAR);
		String filename = copyTo + "/tbData-" + year + "-" + month + "-" + date + ".txt";
		try {
			bw = new BufferedWriter(new FileWriter(filename,true));
			if (bw != null) {								
				bw.write(getDateTime() + ",");
				bw.write(currentLocationList.getSelectedItem().toString() + ",");
				bw.write(id.getText() + ",");
				bw.write(action + ",");
				bw.write(oldPackageValue.getText() + ",");
				bw.write(oldCommunityValue.getText() + ",");
				bw.write(lastUpdatedText.getText() + ",");
				bw.write(oldRevisionText.getText() + ",");
				bw.write(idnext.getText() + ",");
				bw.write(packageList.getSelectedItem().toString() + ",");
				bw.write(communityList.getSelectedItem().toString() + ",");
				bw.write(dateRotation + ",");
				bw.write(newRevisionText.getText() + ",");
				bw.write(fetchIDFromServer.isSelected() + ",");
				bw.write(handIcons.isSelected() + ",");
				bw.write(tbStats.corrupted + ",");
				bw.write(tbStats.corruptionDay + ",");
				bw.write(tbStats.countReflashes + ",");
				bw.write(tbStats.updateNumber + ",");
				bw.write(tbStats.contentPackage + ",");
				bw.write(tbStats.location + ",");
				bw.write(tbStats.updateYear + "/" + tbStats.updateMonth + "/" + tbStats.updateDate + ",");
				bw.write(tbStats.cumulativeDays + ",");
				bw.write(tbStats.lastInitVoltage + ",");
				bw.write(tbStats.powerups + ",");
				bw.write(tbStats.periods + ",");
				bw.write(tbStats.totalRotations + ",");
				bw.write(tbStats.totalMessages + ",");
				int totalSecondsPlayed=0, countStarted=0,countQuarter=0,countHalf=0,countThreequarters=0,countCompleted=0,countApplied=0,countUseless=0;
				for (int m=0;m < tbStats.totalMessages; m++) {
					for (int r=0;r < (tbStats.totalRotations<5?tbStats.totalRotations:5);r++) {
						totalSecondsPlayed += tbStats.stats[m][r].totalSecondsPlayed;
						countStarted += tbStats.stats[m][r].countStarted;
						countQuarter += tbStats.stats[m][r].countQuarter;
						countHalf += tbStats.stats[m][r].countHalf;
						countThreequarters += tbStats.stats[m][r].countThreequarters;
						countCompleted += tbStats.stats[m][r].countCompleted;
						countApplied += tbStats.stats[m][r].countApplied;
						countUseless += tbStats.stats[m][r].countUseless;
						}
				}
				bw.write(totalSecondsPlayed/60 + ",");
				bw.write(countStarted + ",");
				bw.write(countQuarter + ",");
				bw.write(countHalf + ",");
				bw.write(countThreequarters + ",");
				bw.write(countCompleted + ",");
				bw.write(countApplied + ",");
				bw.write(countUseless+ ",");
				for (int r=0; r<(tbStats.totalRotations<5?tbStats.totalRotations:5); r++) {
					bw.write(r + "," + tbStats.totalPlayedSecondsPerRotation(r)/60 + "," + tbStats.rotations[r].startingPeriod + ",");
					bw.write(tbStats.rotations[r].hoursAfterLastUpdate + "," + tbStats.rotations[r].initVoltage);
				}
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
/*		s.append("Serial Number : " + this.serialNumber + NEW_LINE);
		s.append(NEW_LINE);
*/	}
	
	public static void setCommunity(String path, String community, String dateTime) {
		String locFilename = path + community + ".loc";
		String rtcFilename = path + dateTime + ".rtc";
		
		try {
/*			goodCard = executeFile(new File(SCRIPT_SUBDIR + "checkConnection.txt"));
			if (!goodCard) {
				return;
			}
			TBLoader.status2.setText("Checking Memory Card");
			hasCorruption = !executeFile(new File(SCRIPT_SUBDIR + "chkdsk.txt"));
			if (hasCorruption) {
				TBLoader.status2.setText(TBLoader.status2.getText() + "...Corrupted...Getting Stats");
			} else {
				TBLoader.status2.setText(TBLoader.status2.getText() + "...Good Card...Getting Stats");
			}
*/			
			execute("cmd /C del \"" + path + "*.loc\" \"" + path + "system\\*.loc\" \"" + path + "*.rtc\"");

			File locFile = new File(locFilename);
			File rtcFile = new File(rtcFilename);
			File inspectFile = new File(path + "inspect");
			locFile.createNewFile();
			rtcFile.createNewFile();
			inspectFile.createNewFile();
		} catch (Exception e) {
			Logger.LogString(e.toString());
			Logger.init();
			e.printStackTrace();
		}
	}

	private void xferFiles() {
		int response = JOptionPane.showConfirmDialog(this, "Do you currently have a good\n" +
				"Internet connection to upload large files" +
				"\n   in" + TEMP_COLLECTION_DIR + 
				"\n   to " + copyTo + "?",
                "Confirm", JOptionPane.YES_NO_OPTION);
		if (response != JOptionPane.YES_OPTION)
			return;
		xfer.setEnabled(false);
		update.setEnabled(false);
		grabStatsOnly.setEnabled(false);
		setCommunity.setEnabled(false);
		Logger.LogString("Transfering audio files to dropbox.");
		try {
			execute("cmd /C " + SW_SUBDIR + "robocopy \"" + TEMP_COLLECTION_DIR + "\" \"" + copyTo +"\" /MOVE /E");
			execute("cmd /C " + SW_SUBDIR + "robocopy \"" + TEMP_COLLECTION_DIR2 + "\" \"" + copyTo +"\" /MOVE /E");
		} catch (Exception e) {
			Logger.LogString(e.toString());
			Logger.init();
			e.printStackTrace();
		}
		Logger.LogString("Files transferred");
		JOptionPane.showMessageDialog(this, "Files transferred",
                "Success", JOptionPane.DEFAULT_OPTION);
	}

/* @Override
   public void itemStateChanged(ItemEvent evt) {
		Logger.LogString("Drive changed: " + evt.getItem());
		// JComboBox cb = (JComboBox)evt.getSource();
		setCommunityList();
	}
*/
	@Override
	public void actionPerformed(ActionEvent e) {
		DriveInfo di;
		Object o = e.getSource();
		JButton b;
		if (monitoringDrive || !startUpDone)
			return;
		if (o instanceof JButton) {
				if (dateRotation == null || currentLocationList.getSelectedIndex()==0) {
					JOptionPane.showMessageDialog(null, "You must first select a rotation date and a location.",
			                "Need Date and Location!", JOptionPane.DEFAULT_OPTION);
					return;
				}
				b = (JButton)e.getSource();
			}
		else if (o instanceof JComboBox) {
			if (o == driveList) {
				di = (DriveInfo)((JComboBox)e.getSource()).getSelectedItem();
				Logger.LogString("Drive changed: " + di.drive + di.label);
				// JComboBox cb = (JComboBox)evt.getSource();
				id.setText("");
				idnext.setText("");
				try {
					fillCommunityList();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				setSNandRevFromCurrentDrive();
				getRevisionNumbers();
			} else if (o == communityList) {
				JComboBox cl = (JComboBox)o;
				if (cl.getSelectedIndex() == 6 ||cl.getSelectedIndex() == 8) 
					this.handIcons.setSelected(true);
				else
					this.handIcons.setSelected(false);
			}
			return;
		} else
			return;
		
		disableAll();
		try {
			Logger.LogString("ACTION: " + b.getText());
			Object item = driveList.getSelectedItem();
			di = (DriveInfo)item;
			File drive = di.drive;
			if (drive == null) { 
				refreshUI();
				return;
			}
			Logger.LogString("ID:"+((DriveInfo)driveList.getSelectedItem()).serialNumber);
			setSNandRevFromCurrentDrive();
			if (di.serialNumber == NO_SERIAL_NUMBER) {
				// use drive label if exists
				int parens = di.label.indexOf(" (");
				if (parens > 0)
					di.serialNumber = di.label.substring(0, parens);			
				if (di.serialNumber.startsWith("TBXXXX") || (!di.serialNumber.startsWith("ECH") && !di.serialNumber.startsWith("TB"))) {
					Logger.LogString("No .srn found.  Disk label was " + di.serialNumber + ". Using temp id for now.");
					String milliseconds = new String (String.valueOf((Calendar.getInstance().getTimeInMillis())));
					milliseconds = milliseconds.substring(milliseconds.length()-5, milliseconds.length());
					di.serialNumber = "TBt" + milliseconds;
				}
			}
			status.setText("STATUS: Starting\n");
			if (b == xfer) {
				xferFiles();
				Logger.init();
				return;
			} 
			String dateTime = getDateTime();
			if (item != null) {
				prevSelected = drive;
			}
			
			String devicePath = drive.getAbsolutePath();
			String community = communityList.getSelectedItem().toString();
			Logger.LogString("Community: " + community);
			if ((/*b == reformat || */  b == update) && fetchIDFromServer.isSelected()) {
				di.serialNumber = fetchNextDeviceID();
				di.isNewSerialNumber = true;
				this.id.setText(di.serialNumber);
				Logger.LogString("SN: " + di.serialNumber);
			} 
			if ((b == update || b == setCommunity) && communityList.getSelectedIndex() == 0) {
				int response = JOptionPane.showConfirmDialog(this, "No community selected.\nAre you sure?", 
	                "Confirm", JOptionPane.YES_NO_OPTION);
				if (response != JOptionPane.YES_OPTION) {
					Logger.LogString("No community selected. Are you sure? NO");
					refreshUI();
					return;
				} else 
					Logger.LogString("No community selected. Are you sure? YES");
			} else
				prevSelectedCommunity = communityList.getSelectedIndex();
			if ( (b == update || b == setCommunity) && (communityList.getSelectedIndex() == 6 ||communityList.getSelectedIndex() == 8) && !handIcons.isSelected()) {
				// warn if reformatting for Jonga or Gyanvuuri but not adding hand icon messages
				int response = JOptionPane.showConfirmDialog(this, "Hand icons not selected for " + community + ". \nAre you sure?",
	                "Confirm", JOptionPane.YES_NO_OPTION);
				if (response != JOptionPane.YES_OPTION) {
					Logger.LogString("Hand icons not selected for " + community + ". Are you sure? NO");
					refreshUI();
					return;
				} else
					Logger.LogString("Hand icons not selected for " + community + ". Are you sure? YES");
			} else if ( (b == update || b == setCommunity) &&  (communityList.getSelectedIndex() != 6 && communityList.getSelectedIndex() != 8) && handIcons.isSelected()){
				// warn if adding hand icon messages to arrow communities when either updating or reformatting 
				int response = JOptionPane.showConfirmDialog(this, "Hand icons are selected for " + community + ". \nAre you sure?",
		                "Confirm", JOptionPane.YES_NO_OPTION);
				if (response != JOptionPane.YES_OPTION) {
					Logger.LogString("Hand icons are selected for " + community + ". Are you sure? NO");
					refreshUI();
					return;
				} else
					Logger.LogString("Hand icons are selected for " + community + ". Are you sure? YES");
			} else
				prevSelectedCommunity = communityList.getSelectedIndex();
			if (b == grabStatsOnly) {
				updatingTB = true;
				disableAll();
				boolean goodDisk = true;//chkDsk(di);
				if (goodDisk) {
					//Logger.LogString("chkdsk was good.");
					CopyThread t;
					t = new CopyThread(this, this.oldPackageValue.getText(), packageList.getSelectedItem().toString(), devicePath, community, this.oldCommunityValue.getText(), di.serialNumber, di.isNewSerialNumber,  dateTime, handIcons.isSelected(), revision,di.revision,"grabStatsOnly");
					t.start();
				} 				
			}
			else if (b == update) {
				updatingTB = true;
				disableAll();
				boolean goodDisk = true;//chkDsk(di);
				if (goodDisk) {
					//Logger.LogString("chkdsk was good.");
					CopyThread t;
					t = new CopyThread(this, this.oldPackageValue.getText(), packageList.getSelectedItem().toString(), devicePath, community, this.oldCommunityValue.getText(), di.serialNumber, di.isNewSerialNumber,  dateTime, handIcons.isSelected(), revision,di.revision,"update");
					t.start();
				} 
/*				else {
					Logger.LogString("chkdsk was bad for " + di.label.toString());
					int response = JOptionPane.showConfirmDialog(null, "TB is corrupted.\nBackup takes 6-8 minutes.\nBegin?",
			              "Confirm", JOptionPane.OK_CANCEL_OPTION);
					if (response == JOptionPane.OK_OPTION) {
						Logger.LogString("TB is corrupted.  Backup takes 6-8 minutes. Begin?  YES");
						backup(devicePath, community, di.serialNumber, dateTime);
						CopyThread t = new CopyThread(this, devicePath, community, di.serialNumber, dateTime, handIcons.isSelected(), revision,di.revision,"corrupted");
						t.start();
					} else {
						Logger.LogString("TB is corrupted.  Backup takes 6-8 minutes. Begin?  NO");
					}
				}
*/			} 
/*			else if (b == backup) {
				int response = JOptionPane.showConfirmDialog(null, "Backup takes 6-8 minutes.\nBegin?",
			              "Confirm", JOptionPane.OK_CANCEL_OPTION);
				if (response == JOptionPane.OK_OPTION) {
					Logger.LogString("Backup takes 6-8 minutes. Begin?  YES");
					backup(devicePath, community, di.serialNumber, dateTime);
				} else
					Logger.LogString("Backup takes 6-8 minutes. Begin?  NO");
			} else if (b == reformat) {
					Logger.LogString("REFORMATTING");
					CopyThread t = new CopyThread(this, devicePath, community, di.serialNumber, dateTime, handIcons.isSelected(), revision,di.revision,"compare-basic");//"reformat");
					t.start();
			} 
*/			else if (b == setCommunity) {
				//setCommunity(devicePath, community, dateTime);
				CopyThread t;
				t = new CopyThread(this, this.oldPackageValue.getText(), packageList.getSelectedItem().toString(), devicePath, community, this.oldCommunityValue.getText(), di.serialNumber, di.isNewSerialNumber, dateTime, handIcons.isSelected(), revision,di.revision,"setCommunity");
				t.start();
			}
			refreshUI();
			Logger.init();
			return;
		} catch (Exception ex) {
			Logger.LogString(ex.toString());
			ex.printStackTrace();
			JOptionPane.showMessageDialog(this, "An error occured.",
	                "Error", JOptionPane.ERROR_MESSAGE);
			resetUI(false);
		} 
	}
/*
	private void backup(String devicePath, String community, String serialNumber, String dateTime) {
		String imagePathForMkDir, imagePathForCmdLine, imageFile;
		try {
			imagePathForCmdLine = "\"" + TEMP_COLLECTION_DIR + "\\" + community + "\\" + serialNumber + "\\" + dateTime;
			imagePathForMkDir = TEMP_COLLECTION_DIR + "\\" + community + "\\" + serialNumber + "\\" + dateTime;
			Logger.LogString("backing up to " + imagePathForCmdLine);
			File f = new File(imagePathForMkDir);
			for (int i=0; i<3;i++) {
				if (f.mkdirs()) {
					Logger.LogString("directories created");
					break;
				} else
					Logger.LogString("directories not created yet");
			}
			imageFile = imagePathForCmdLine + "\\" + serialNumber + ".zip\"";
			execute("cmd /C " + CREATE_BACKUP + devicePath + " " + imageFile + " " + SW_SUBDIR);
			JOptionPane.showMessageDialog(this, "Backup complete",
	                "Completed", JOptionPane.DEFAULT_OPTION);
			Logger.LogString("Backup complete");
		} catch (Exception e) {
			Logger.LogString(e.getLocalizedMessage());
			e.printStackTrace();
		}
		refreshUI();
	}
*/
	private void resetUI(boolean resetDrives) {
		Logger.LogString("Resetting UI");
		fillPackageList();
		Logger.LogString(" -clearing id text");
		id.setText("");
		idnext.setText("");
		if (resetDrives && !monitoringDrive) {
			Logger.LogString(" -fill drives list");
			fillList(getRoots());
		} else if (resetDrives && monitoringDrive) {
			Logger.LogString(" - drive list currently being filled by drive monitor");
		}
//		Logger.LogString(" -fill communities list");
//		fillCommunityList();
		Logger.LogString(" -refresh UI");
		refreshUI();
	}
	
	private synchronized boolean driveConnected() {
		boolean connected = false;
		File drive;
		
		if (driveList.getItemCount() > 0) {		
			drive = ((DriveInfo)driveList.getSelectedItem()).drive;
			if (drive != null)
				connected = true;
		}
		return connected;
	}

	private synchronized void checkDirUpdate() {
		String triggerFile = copyTo + "\\" + TRIGGER_FILE_CHECK;
		File f = new File (triggerFile);
		if (f.exists()) {
			status.setText("Updating list of files to send");
			try {
				f.delete();
				execute("cmd /C dir " + copyTo + " /S > " + copyTo + "\\dir.txt");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}			
	}
	
	private void refreshUI() {
		boolean connected;
		disableAll();
		checkDirUpdate();
		
		update.setText("Update TB");
//		reformat.setText("Reformat");
//		backup.setText("Backup");
		setCommunity.setText("Set Community");
		handIcons.setEnabled(true);
		fetchIDFromServer.setEnabled(true);
		connected = driveConnected();
		if (connected && !updatingTB) {
			update.setEnabled(true);
			grabStatsOnly.setEnabled(true);
//			reformat.setEnabled(true);
//			backup.setEnabled(true);
			xfer.setEnabled(true);
			setCommunity.setEnabled(true);
			xfer.setEnabled((new File(TEMP_COLLECTION_DIR)).exists());			
			status.setText("STATUS: Ready");
			status2.setText("");
			Logger.LogString("STATUS: Ready");
		} else {
			update.setEnabled(false);
			grabStatsOnly.setEnabled(false);
			setCommunity.setEnabled(false);
			xfer.setEnabled(false);
			if (!connected) {
        		oldPackageValue.setText("");
        		oldCommunityValue.setText("");
        		oldRevisionText.setText("");
        		idnext.setText("");
				id.setText("");
				lastUpdatedText.setText("");
				Logger.LogString("STATUS: " + NO_DRIVE);
				status.setText("STATUS: " + NO_DRIVE);
			}
//			reformat.setEnabled(false);
//			backup.setEnabled(false);
		}
	}

	private void disableAll() {
		// This is used after startup and when there have been no changes to the drives 
		update.setEnabled(false);
//		reformat.setEnabled(false);
//		backup.setEnabled(false);
		xfer.setEnabled(false);	
		setCommunity.setEnabled(false);
		handIcons.setEnabled(false);
		fetchIDFromServer.setEnabled(false);
		
	}
	
	void finishCopy(boolean success, final String idString, final String mode) {
		final TBLoader parent = this;
		updatingTB = false;
		parent.refreshUI();
		Logger.init();
	}

	public static class Logger {
		private static BufferedWriter bw;

		public Logger () {
			init();
		}
		public synchronized static void LogString(String s) {
			if (s == null)
				return;
			try {
				System.out.println("Log:" + s);
				if (bw != null) {
					bw.write(getDateTime() + ": " + s+"\r\n");
				}
			} catch (IOException e) {
				close();
				open();
				try {
					bw.write(getDateTime() + ": " + s+"\r\n");
				} catch (IOException ee) {
					e.printStackTrace();
					ee.printStackTrace();					
				}
			}
		}

		public static void close() {
			try {
				if (bw != null) {
					bw.flush();
					bw.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		private static void open() {
			try {
				bw = new BufferedWriter(new FileWriter(getLogFileName()));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public static void init () {
			try {
				close();
				open();
				LogString("Version: " + VERSION);
				LogString("App Path: " + new File(".").getAbsolutePath());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static class CopyThread extends Thread {
		final String packageName;
		final String sourcePackage;
		final String devicePath;
		final String community;
		final String sourceCommunity;
		final String id;
		final boolean isNewSerialNumber;
		final String datetime;
		final TBLoader callback;
		final boolean useHandIcons;
		final String sourceRevision;
		final String targetRevision;
		final String mode;
		boolean criticalError = false;
		boolean alert = false;
		boolean success = false;
		long startTime;
		

		private void setStartTime() {
			startTime = System.nanoTime();
		}
		
		private String getDuration() {
			String elapsedTime;
			double durationSeconds;
			int durationMinutes;
			long durationNanoseconds = System.nanoTime() - startTime;
			durationSeconds = (double)durationNanoseconds  / 1000000000.0;
			if (durationSeconds > 60) {
				durationMinutes = (int)durationSeconds / 60;
				durationSeconds -= durationMinutes *60;
				elapsedTime = new String(Integer.toString(durationMinutes) + " minutes " + Integer.toString((int) durationSeconds) + " seconds");
			} else
				elapsedTime = new String(Integer.toString((int) durationSeconds) + " seconds");
			return elapsedTime;
		}
		
		private boolean executeFile(File file) { 
			boolean success = true;
			String errorLine = "";
			criticalError = false;
			Calendar cal = Calendar.getInstance();
			String month = String.valueOf(cal.get(Calendar.MONTH) + 1);
			String dateInMonth = String.valueOf(cal.get(Calendar.DAY_OF_MONTH));
			String year = String.valueOf(cal.get(Calendar.YEAR));
			
			try {
				BufferedReader reader;
				String handValue;
				if (this.useHandIcons)
					handValue = "1";
				else
					handValue = "0";
				reader = new BufferedReader(new FileReader(file));
				Logger.LogString("ID="+id);
				while (reader.ready() && !criticalError) {
					String cmd = reader.readLine();
					if (cmd.startsWith("rem ")) {
						status.setText("STATUS: " + cmd.substring(4));
						Logger.LogString(cmd.substring(4));
						continue;
					}
					cmd = cmd.replaceAll("\\$\\{device_drive\\}", devicePath.substring(0, 2));
					cmd = cmd.replaceAll("\\$\\{community\\}", community);
					cmd = cmd.replaceAll("\\$\\{source_community\\}", sourceCommunity);
					cmd = cmd.replaceAll("\\$\\{srn\\}", id);
					cmd = cmd.replaceAll("\\$\\{device_id\\}", id);  //backward compat with label command in one of the scripts
					cmd = cmd.replaceAll("\\$\\{datetime\\}", datetime);
					cmd = cmd.replaceAll("\\$\\{dateInMonth\\}", dateInMonth);
					cmd = cmd.replaceAll("\\$\\{month\\}", month);					
					cmd = cmd.replaceAll("\\$\\{year\\}", year);					
					cmd = cmd.replaceAll("\\$\\{send_now_dir\\}", Matcher.quoteReplacement(copyTo));
					cmd = cmd.replaceAll("\\$\\{holding_dir\\}", Matcher.quoteReplacement(TEMP_COLLECTION_DIR));
					cmd = cmd.replaceAll("\\$\\{hand\\}", handValue);
					cmd = cmd.replaceAll("\\$\\{source_revision\\}", sourceRevision);
					cmd = cmd.replaceAll("\\$\\{target_revision\\}", targetRevision);
					cmd = cmd.replaceAll("\\$\\{package\\}", packageName);
					cmd = cmd.replaceAll("\\$\\{source_package\\}", sourcePackage);
					cmd = cmd.replaceAll("\\$\\{newSRN\\}", isNewSerialNumber ? "1" : "0");
					alert = cmd.startsWith("!");
					if (alert)
						cmd = cmd.substring(1);
					errorLine = execute("cmd /C " + cmd);
					if (errorLine != null && alert) {
						JOptionPane.showMessageDialog(null, errorLine,
								"Error", JOptionPane.ERROR_MESSAGE);
						criticalError = true;
						success = false;
						break;
					}
				}
				reader.close();
			} catch (Exception e) {
				Logger.LogString(e.toString());
				Logger.init();
				e.printStackTrace();
			}
			return success;
		}
		
		public CopyThread(TBLoader callback, String sourcePackage, String packageName, String devicePath, String community, String sourceCommunity, String id, boolean isNewSerialNumber, String datetime, boolean useHandIcons, String sourceRevision, String targetRevision,String mode) {
			this.callback = callback;
			this.packageName = packageName;
			this.sourcePackage = sourcePackage;
			this.community 	= community;
			this.sourceCommunity = sourceCommunity;
			this.devicePath = devicePath;
			this.id = id;
			this.isNewSerialNumber = isNewSerialNumber;
			this.datetime = datetime;
			this.useHandIcons = useHandIcons;
			this.mode = mode;
			this.sourceRevision = sourceRevision;
			this.targetRevision = targetRevision;
		}		

		private void grabStatsOnly() {
			try {
				boolean gotStats,hasCorruption,verified, goodCard;
				setStartTime();
				success = false;
				goodCard = executeFile(new File(SCRIPT_SUBDIR + "checkConnection.txt"));
				if (!goodCard) {
					return;
				}
				TBLoader.status2.setText("Checking Memory Card");
				Logger.LogString("STATUS:Checking Memory Card");
				hasCorruption = !executeFile(new File(SCRIPT_SUBDIR + "chkdsk.txt"));
				if (hasCorruption) {
					callback.tbStats.corrupted = true;
					TBLoader.status2.setText(TBLoader.status2.getText() + "...Corrupted\nGetting Stats");
					Logger.LogString("STATUS:Corrupted...Getting Stats");
					executeFile(new File(SCRIPT_SUBDIR + "chkdsk-save.txt"));
				} else {
					TBLoader.status2.setText(TBLoader.status2.getText() + "...Good\nGetting Stats");
					Logger.LogString("STATUS:Good Card\nGetting Stats");
				}
				gotStats = executeFile(new File(SCRIPT_SUBDIR + "grab.txt"));
				callback.logTBData("stats-only");
				if (gotStats) {
					TBLoader.status2.setText(TBLoader.status2.getText() + "...Got Stats\nDisconnecting");
					Logger.LogString("STATUS: Got stats!");
					Logger.LogString("STATUS: Disconnecting TB");
					executeFile(new File(SCRIPT_SUBDIR + "disconnect.txt"));
					TBLoader.status2.setText(TBLoader.status2.getText() + "...Complete");
					Logger.LogString("STATUS:Complete");
					success = true;
					JOptionPane.showMessageDialog(null, "Got Stats!",
			                "Success", JOptionPane.DEFAULT_OPTION);
				}
				else {
					TBLoader.status2.setText(TBLoader.status2.getText() + "...No Stats!\n");
					Logger.LogString("STATUS:No Stats!");
					JOptionPane.showMessageDialog(null, "Could not get stats for some reason.",
			                "Failure", JOptionPane.DEFAULT_OPTION);					
				}
			} finally {
				callback.finishCopy(success, id, this.mode);
			}
		}
		
		private void update() {
			try {
				boolean gotStats,hasCorruption,verified, goodCard;
				setStartTime();
				success = false;
				goodCard = executeFile(new File(SCRIPT_SUBDIR + "checkConnection.txt"));
				if (!goodCard) {
					return;
				}
				TBLoader.status2.setText("Checking Memory Card");
				Logger.LogString("STATUS:Checking Memory Card");
				hasCorruption = !executeFile(new File(SCRIPT_SUBDIR + "chkdsk.txt"));
				if (hasCorruption) {
					callback.tbStats.corrupted = true;
					TBLoader.status2.setText(TBLoader.status2.getText() + "...Corrupted\nGetting Stats");
					Logger.LogString("STATUS:Corrupted...Getting Stats\n");
					executeFile(new File(SCRIPT_SUBDIR + "chkdsk-save.txt"));
				} else {
					TBLoader.status2.setText(TBLoader.status2.getText() + "...Good\nGetting Stats");
					Logger.LogString("STATUS:Good Card...Getting Stats\n");
				}
				gotStats = executeFile(new File(SCRIPT_SUBDIR + "grab.txt"));
				if (gotStats) {
					TBLoader.status2.setText(TBLoader.status2.getText() + "...Got Stats\n");
					Logger.LogString("STATUS:Got Stats\n");
				}
				else {
					TBLoader.status2.setText(TBLoader.status2.getText() + "...No Stats!\n");
					Logger.LogString("STATUS:No Stats!\n");
				}
				if (hasCorruption) {
					TBLoader.status2.setText(TBLoader.status2.getText() + "Reformatting");
					Logger.LogString("STATUS:Reformatting");
					goodCard = executeFile(new File(SCRIPT_SUBDIR + "reformat.txt"));
					if (!goodCard) {
						TBLoader.status2.setText(TBLoader.status2.getText() + "...Failed\n");
						Logger.LogString("STATUS:Reformat Failed");
						Logger.LogString("Could not reformat memory card.\nMake sure you have a good USB connection\nand that the Talking Book is powered with batteries, then try again.\n\nIf you still cannot reformat, replace the memory card.");
						JOptionPane.showMessageDialog(null, "Could not reformat memory card.\nMake sure you have a good USB connection\nand that the Talking Book is powered with batteries, then try again.\n\nIf you still cannot reformat, replace the memory card.",
				                "Failure!", JOptionPane.ERROR_MESSAGE);
						return;
					} else { 
						TBLoader.status2.setText(TBLoader.status2.getText() + "...Good\n");
						Logger.LogString("STATUS:Format was good");
					}
				} else {
					Logger.LogString("STATUS:Relabeling volume");
					TBLoader.status2.setText(TBLoader.status2.getText() + "Relabeling\n");
					executeFile(new File(SCRIPT_SUBDIR + "relabel.txt"));
				}
				TBLoader.status2.setText(TBLoader.status2.getText() + "Updating TB Files");
				Logger.LogString("STATUS:Updating TB Files");
				executeFile(new File(SCRIPT_SUBDIR + "update.txt"));
				TBLoader.status2.setText(TBLoader.status2.getText() + "...Updated\n");
				Logger.LogString("STATUS:Updated");
				verified = executeFile(new File(SCRIPT_SUBDIR + "verify.txt"));
				if (verified) {
					TBLoader.status2.setText(TBLoader.status2.getText() + "Verified Basic\nAdding Community Content\n");
					Logger.LogString("STATUS:Verified Basic...Adding Any Custom Community Content");
					verified = executeFile(new File(SCRIPT_SUBDIR + "customCommunity.txt"));					
				}
				callback.logTBData("update");
				if (verified) {
					String duration;
					TBLoader.status2.setText(TBLoader.status2.getText() + "Updated & Verified\nDisconnecting TB");
					Logger.LogString("STATUS:Updated & Verified...Disconnecting TB");
					executeFile(new File(SCRIPT_SUBDIR + "disconnect.txt"));
					TBLoader.status2.setText(TBLoader.status2.getText() + "...Complete");
					Logger.LogString("STATUS:Complete");
					success = true;
					duration = getDuration();
					Logger.LogString("Talking Book has been updated and verified\nin "+ duration + ".");
					JOptionPane.showMessageDialog(null, "Talking Book has been updated and verified\nin " + duration + ".",
			                "Success", JOptionPane.DEFAULT_OPTION);
				} else {
					success = false;
					TBLoader.status2.setText(TBLoader.status2.getText() + "...Failed Verification\n");
					Logger.LogString("STATUS:Failed Verification");
					JOptionPane.showMessageDialog(null, "Update failed verification.  Try again or replace memory card.",
			                "Failure", JOptionPane.DEFAULT_OPTION);
					Logger.LogString("Update failed verification.  Try again or replace memory card.");
				}
				for (int i=1;i<=(success?3:6);i++)
					Toolkit.getDefaultToolkit().beep();
			} catch (Exception e) {
				if (alert) {
					JOptionPane.showMessageDialog(null, e.getMessage(),
		                "Error", JOptionPane.ERROR_MESSAGE);
					criticalError = true;
					Logger.LogString("CRITICAL ERROR:" + e.getMessage());
				} else
					Logger.LogString("NON-CRITICAL ERROR:" + e.getMessage());
				Logger.init();
				e.printStackTrace();
			} finally {
				callback.finishCopy(success, id, this.mode);
			}
			
		}
		
		private void setCommunity() {
			try {
				boolean hasCorruption,goodCard;
				success = false;
				Logger.LogString("Setting Community -- first checking connectin and memory card.");
				goodCard = executeFile(new File(SCRIPT_SUBDIR + "checkConnection.txt"));
				if (!goodCard) {
					return;
				}
				TBLoader.status2.setText("Checking Memory Card");
				Logger.LogString("STATUS:Checking Memory Card");
				hasCorruption = !executeFile(new File(SCRIPT_SUBDIR + "chkdsk.txt"));
				if (hasCorruption) {
					callback.tbStats.corrupted = true;
					Logger.LogString("Could not set community due to memory card corruption.  Run 'Update' to attempt to fix.");
					TBLoader.status2.setText(TBLoader.status2.getText() + "...Corrupted");
					Logger.LogString("STATUS:Corrupted");
					JOptionPane.showMessageDialog(null, "Could not set community due to memory card corruption.  Run 'Update' to attempt to fix.",
			                "Failure", JOptionPane.DEFAULT_OPTION);
					return;
				}
				TBLoader.status2.setText(TBLoader.status2.getText() + "...Good\nSetting Community");
				Logger.LogString("STATUS:Memory Card Good...Setting Community");
				TBLoader.setCommunity(devicePath, community, this.datetime);
				TBLoader.status2.setText(TBLoader.status2.getText() + "...Set\n");
				Logger.LogString("STATUS:Community Set");
				success = executeFile(new File(SCRIPT_SUBDIR + "customCommunity.txt"));
				callback.logTBData("set community");
				if (success) {
					TBLoader.status2.setText(TBLoader.status2.getText() + "Custom Files Applied\n");
					Logger.LogString("STATUS:Custom Files Applied");
					executeFile(new File(SCRIPT_SUBDIR + "disconnect.txt"));
					TBLoader.status2.setText(TBLoader.status2.getText() + "Complete\n");
					Logger.LogString("STATUS:Complete");
					Logger.LogString("Community set to " + community + " and custom files applied.");
					JOptionPane.showMessageDialog(null, "Community set to " + community + ".",
			                "Success", JOptionPane.DEFAULT_OPTION);
				}
			for (int i=1;i<=3;i++)
					Toolkit.getDefaultToolkit().beep();
			} catch (Exception e) {
				if (alert) {
					JOptionPane.showMessageDialog(null, e.getMessage(),
		                "Error", JOptionPane.ERROR_MESSAGE);
					Logger.LogString("CRITICAL ERROR:" + e.getMessage());
					criticalError = true;
				}
				Logger.LogString(e.toString());
				Logger.init();
				e.printStackTrace();
			} finally {
				callback.finishCopy(success, id, this.mode);
			}
		}
		
		@Override public void run() {
			if (this.mode == "update")
				update();			
			else if (this.mode == "grabStatsOnly")
				grabStatsOnly();
			else if (this.mode == "setCommunity")
				setCommunity();			
		}
	}

	private static final boolean debug = false;

/*	static boolean chkDsk(DriveInfo di) throws Exception {
		String line;
		String folders[] = {"system","languages","messages","statistics","log","log-archive","messages/lists","messages/audio","languages/dga","statistics/stats","statistics/ostats"};
		boolean goodDisk = true;
		
		Logger.LogString("cmd /C echo n|chkdsk " + di.drive.toString());
		// perform check disk
		Process proc = Runtime.getRuntime().exec("cmd /C echo n|chkdsk " + di.drive.toString());

		BufferedReader br1 = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		BufferedReader br2 = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

		while (goodDisk && (line = br1.readLine()) != null) {
			int i;
			Logger.LogString(line);
			if ((i = line.indexOf("is cross-linked on allocation unit")) > -1) {
				System.out.println(i);
				Logger.LogString(line.substring(0, i));
				di.corruptions[di.corruptionCount] = line.substring(0, i);
				Logger.LogString("***BAD:" + di.corruptions[di.corruptionCount]);
				di.corruptionCount++;
			}				
			if (line.startsWith("Windows found problems with the file system.")) {
				goodDisk = false;
			}
			if (line.startsWith("Volume ") && line.contains(" created ")) {
//				di.label = new String(line.substring(7, line.indexOf(' ', 7)));
				Logger.LogString("***LABEL:" + line.substring(7, line.indexOf(' ', 7)));
			}
		} 
		
		for (int i = 0; i < folders.length; i++) {
			String folderName = new String(di.drive.toString() + folders[i]);
			Logger.LogString(folderName);
			if (!(new File(folderName)).exists()) {
				di.corruptions[di.corruptionCount] = folderName;
				Logger.LogString("***BAD:" + di.corruptions[di.corruptionCount]);
				di.corruptionCount++;
				goodDisk = false;
			}
		}
		
		do {
			line = br2.readLine();
			Logger.LogString(line); 
			
		} while (line != null);
		br1.close();
		br2.close();
		proc.waitFor();
		return goodDisk;
	}	
*/
	private static String dosErrorCheck(String line) {
		String errorMsg = null;
		
		if (line.contains("New")) {
			//  file copy validation failed (some files missing on target) 
			errorMsg = line.substring(line.length()-30);
		} else if (line.contains("Invalid media or Track 0 bad - disk unusable")) {
			// formatting error
			errorMsg = "Bad memory card.  Please discard and replace it.";
		} else if (line.contains("Specified drive does not exist.") || line.startsWith("The volume does not contain a recognized file system.")) {
			errorMsg = "Either bad memory card or USB connection problem.  Try again.";
		} else if (line.contains("Windows found problems with the file system") /* || line.startsWith("File Not Found") */ || line.startsWith("The system cannot find the file")) {
			// checkdisk shows corruption
			errorMsg = "File system corrupted";
		} else if (line.startsWith("The system cannot find the path specified.")) {
			errorMsg = "TB not found.  Unplug/replug USB and try again.";
		}
		return errorMsg;
	}
	//Log:ID=TB000336
	//Log:REFORMAT
	//Log:Formatting Talking Book
	//Log:Executing:cmd /C format F: /FS:FAT32 /v:TB000336 /Y /Q
	//Log:Cannot open volume for direct access.
	
	//Log:  Options : *.* /S /E /COPY:DAT /PURGE /MIR /NP /R:1000000 /W:30 
	//Log:------------------------------------------------------------------------------
	//Log:2013/07/11 23:40:38 ERROR 2 (0x00000002) Creating Destination Directory F:\
	//Log:The system cannot find the file specified.

	static String execute(String cmd) throws Exception {
		String line;
		String errorLine = null;
		Logger.LogString("Executing:" + cmd);
		Process proc = Runtime.getRuntime().exec(cmd);

		BufferedReader br1 = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		BufferedReader br2 = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
		
		do {
			line = br1.readLine();
			Logger.LogString(line);
			if (line!=null && errorLine == null)
				errorLine = dosErrorCheck(line);				
		} while (line != null);
		
		
		do {
			line = br2.readLine();
			Logger.LogString(line);
			if (line!=null && errorLine == null)
				errorLine = dosErrorCheck(line);
		} while (line != null);
		
		proc.waitFor();
		return errorLine;
	}
	
	private static class DriveInfo {
		final File drive;
		String label;
		String serialNumber;
		boolean isNewSerialNumber;
		String revision;
		static final int MAX_CORRUPTIONS = 20;
		boolean corrupted;
		
		public DriveInfo(File drive, String label) {
			this.drive = drive;
			this.label = label.trim();
			this.corrupted = false;
			this.serialNumber = "";
			this.isNewSerialNumber = false;
			this.revision = "";
			//updateLastIssue();
		}

/*		public boolean updateLastIssue() {
			boolean foundIssue = false;
			int rev = 0;
			int i;
			
			if (this.drive == null) 
				return false;
			try {
				File[] files;
				File systemPath = new File(this.drive.getAbsolutePath(), "system");
				if (!systemPath.exists())
					return false;
				files = systemPath.listFiles(new FilenameFilter() {
					@Override public boolean accept(File dir, String name) {
						String lowercase = name.toLowerCase();
						return (lowercase.endsWith(".rev") || lowercase.endsWith(".img"));
					}
				});
				for (i = 0; i<files.length; i++) {
					String filenameRev = files[i].getName();
					filenameRev = filenameRev.substring(1,filenameRev.indexOf('.'));
					rev = Integer.parseInt(filenameRev);
					if (rev > 700) {
						foundIssue = true;
						break;
					}
				}
				if (foundIssue) {
					if (rev < 860)
						this.lastIssue = 1;
					else if (rev < 900)
						this.lastIssue = 2;
					else this.lastIssue = 3;
					Logger.LogString(this.label + " is currently Issue #" + String.valueOf(this.lastIssue) + " (based on " + files[i].getName() + ")");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return foundIssue;
		}
*/		
		
		@Override
		public String toString() {
			if (label.isEmpty()) {
				return drive.toString();
			}
			return label;
		}
	}
	
	private static class TBInfo {
		// struct SystemData
		// int structType
		boolean corrupted;
		boolean debug = false;
		String serialNumber;
		String updateNumber;
		short countReflashes;
		String location;
		String contentPackage;
		short updateDate = -1;
		short updateMonth = -1;
		short updateYear = -1;
		
		// struct SystemCounts2
		// short structType
		short periods;
		short cumulativeDays;
		short corruptionDay;
		short powerups;
		short lastInitVoltage;
		RotationTiming[] rotations = new RotationTiming[5];
		
		// struct NORmsgMap
		// short structType
		short totalMessages;
		String[] msgIdMap = new String[20]; // 40 messages, 20 chars
		
		//struct NORallMsgStats
		// short totalMessages
		short totalRotations;
		NORmsgStats[][] stats = new NORmsgStats[20][5];
		NORmsgStats[] statsAllRotations = new NORmsgStats[20];
		RandomAccessFile f;
		
		private class RotationTiming {
			short rotationNumber;
			short startingPeriod;
			short hoursAfterLastUpdate;
			short initVoltage;
			
			public RotationTiming() throws IOException {
				f.skipBytes(2);
				//System.out.println("pointer:"+f.getFilePointer());
				this.rotationNumber = readShort();
				this.startingPeriod = readShort();
				this.hoursAfterLastUpdate = readShort();
				this.initVoltage = readShort();
				//System.out.println("pointer:"+f.getFilePointer());
			}
		}
		
		private class NORmsgStats {
			// short structType
			short indexMsg;
			short numberRotation;
			short countStarted;
			short countQuarter;
			short countHalf;
			short countThreequarters;
			short countCompleted;
			short countApplied;
			short countUseless;
			short totalSecondsPlayed;
			
			public NORmsgStats() throws IOException {
				f.skipBytes(2);
				this.indexMsg = readShort();
				this.numberRotation = readShort();
				this.countStarted = readShort();
				this.countQuarter = readShort();
				this.countHalf = readShort();
				this.countThreequarters = readShort();
				this.countCompleted = readShort();
				this.countApplied = readShort();
				this.countUseless = readShort();
				this.totalSecondsPlayed = readShort();
			}
		}

		public TBInfo(String flashDataPath) throws IOException {
			corrupted = false;
			File file = new File(flashDataPath);
			if (!file.exists()) {
				System.out.print("No flash binary file to analyze.");
				return;
			}
			f = new RandomAccessFile(flashDataPath,"r");
			f.skipBytes(2);
			this.countReflashes = readShort();
			this.serialNumber = readString(10);
			this.updateNumber = readString(10);
			this.location = readString(40);
			this.contentPackage = readString(10);
			this.updateDate = readShort();
			this.updateMonth = readShort();
			this.updateYear = readShort();
			
			f.skipBytes(2);
			this.periods = readShort();
			this.cumulativeDays = readShort();
			this.corruptionDay = readShort();
			this.powerups = readShort();
			this.lastInitVoltage = readShort();
			for (int i=0;i < 5;i++) {
				if (debug) {
					System.out.println("i:"+i);
					System.out.println("pointer:"+f.getFilePointer());
				}
				rotations[i] = new RotationTiming();
			}
			
			f.skipBytes(2);
			this.totalMessages = readShort();
			for (int i=0; i < 20; i++) {
				if (i < this.totalMessages)
					this.msgIdMap[i] = readString(20);
				else
					f.skipBytes(40);
			}
			
			f.skipBytes(2);
			if (debug)
				System.out.print("About to read totalrotations:");
			this.totalRotations = readShort();
			for (int m=0; m < this.totalMessages; m++) {
				for (int r=0; r < 5; r++) {
					if (debug)
						System.out.println("msg:"+m+" rot:"+r+" at "+f.getFilePointer());
					this.stats[m][r] = new NORmsgStats();
				}
			}
			f.close();
			System.out.print(this.toString());
		}

		short readShort() throws IOException {
			 long sum = 0;
			 int b, i;
			 short ret;

			 for (int l= 0; l<2; l++) {
				 b = f.readByte() & 0xFF; // remove sign
				 //System.out.print("          b:"+b);
				 i = b << (8 * l);
				 sum += (0xFFFF & i);
			 }
			 ret = (short) sum;
			 if (debug)
				 System.out.println("       readShort (" + sum + ") at " + f.getFilePointer() + ": "+ ret);
			 return ret;
		}
		
		String readString(int maxChars) throws IOException {
			char[] c = new char[maxChars];
			char a;
			byte b;
			boolean endString = false;
			long start = f.getFilePointer();
			for (int i=0; i < maxChars; i++) {
				c[i] = (char)f.readByte();
				if (endString)
					c[i] = 0;
				else if (c[i] == 0)
					endString = true;
				b = f.readByte();
				//f.skipBytes(1);
			}			
			if (debug)
				System.out.println("     string:" + String.valueOf(c) + " at " + start);
			return new String(c).trim();
		}		
		
		public long totalPlayedSecondsPerMsg (int msg) {
			long totalSec = 0;
			for (int r=0;r<5;r++) {
				totalSec += this.stats[msg][r].totalSecondsPlayed;
			}
			return totalSec;
		}
		
		public long totalPlayedSecondsPerRotation (int rotation) {
			long totalSec = 0;
			for (int m=0;m<this.totalMessages;m++) {
				totalSec += this.stats[m][rotation].totalSecondsPlayed;
			}
			return totalSec;
		}

		public String toString() {
			StringBuilder s = new StringBuilder();
			String NEW_LINE = System.getProperty("line.separator");
			
			s.append("Serial Number : " + this.serialNumber + NEW_LINE);
			s.append("Reflashes     : " + this.countReflashes  + NEW_LINE);
			s.append("Update Number : " + this.updateNumber + NEW_LINE);
			s.append("Package       : " + this.contentPackage + NEW_LINE);
			s.append("Location      : " + this.location + NEW_LINE);
			s.append("Last Updated  : " + this.updateYear + "/" + this.updateMonth + "/" + this.updateDate + NEW_LINE);
			s.append(NEW_LINE);
			s.append("Powered Days  : " + this.cumulativeDays + NEW_LINE);
			s.append("Last PowerupV : " + this.lastInitVoltage + NEW_LINE);
			s.append("StartUps      : " + this.powerups + NEW_LINE);
			s.append("Corruption Day: " + this.corruptionDay + NEW_LINE);
			s.append("Periods       : " + this.periods + NEW_LINE);
			s.append("Rotations     : " + this.totalRotations + NEW_LINE);
			s.append(NEW_LINE);
			s.append("TOTAL STATS (" + this.totalMessages + " messages)" + NEW_LINE);
			int totalSecondsPlayed=0, countStarted=0,countQuarter=0,countHalf=0,countThreequarters=0,countCompleted=0,countApplied=0,countUseless=0;
			for (int m=0;m < this.totalMessages; m++) {
				for (int r=0;r < (this.totalRotations<5?this.totalRotations:5);r++) {
					totalSecondsPlayed += this.stats[m][r].totalSecondsPlayed;
					countStarted += this.stats[m][r].countStarted;
					countQuarter += this.stats[m][r].countQuarter;
					countHalf += this.stats[m][r].countHalf;
					countThreequarters += this.stats[m][r].countThreequarters;
					countCompleted += this.stats[m][r].countCompleted;
					countApplied += this.stats[m][r].countApplied;
					countUseless += this.stats[m][r].countUseless;
					}
			}
			s.append("       Time:" + totalSecondsPlayed/60 + "min " + totalSecondsPlayed%60 + "sec   Started:" + countStarted + "   P:" + countQuarter + 
					 "   H:"  + countHalf + "   M:" + countThreequarters + 
					 "   F:" + countCompleted);
			s.append("   A:" + countApplied + "   U:" + countUseless + NEW_LINE);
			s.append(NEW_LINE);	
			
			for (int r=0; r<(this.totalRotations<5?this.totalRotations:5); r++) {
				s.append("  Rotation:" + r + "     " + totalPlayedSecondsPerRotation(r)/60 + "min " + totalPlayedSecondsPerRotation(r)%60 + "sec    Starting Period:"
						+ this.rotations[r].startingPeriod + "   Hours After Update:" + 
						this.rotations[r].hoursAfterLastUpdate + "   Init Voltage:" + this.rotations[r].initVoltage + NEW_LINE);
			}
			s.append(NEW_LINE);
			s.append("Message Stats  (" + this.totalMessages + " messages)" + NEW_LINE);
			for (int m=0;m < this.totalMessages; m++) {
				s.append("  MESSAGE ID:" + this.msgIdMap[m] + " (" + totalPlayedSecondsPerMsg(m)/60 + "min "+ totalPlayedSecondsPerMsg(m)%60 + "sec)" + NEW_LINE);
				for (int r=0;r<(this.totalRotations<5?this.totalRotations:5); r++) {
					s.append("     ROTATION: " + r);
					s.append("       Time:" + this.stats[m][r].totalSecondsPlayed/60 + "min " + this.stats[m][r].totalSecondsPlayed%60 + "sec   Started:" + this.stats[m][r].countStarted + "   P:" + this.stats[m][r].countQuarter + 
							 "   H:"  + this.stats[m][r].countHalf + "   M:" + this.stats[m][r].countThreequarters + 
							 "   F:" + this.stats[m][r].countCompleted);
					s.append("   A:" + this.stats[m][r].countApplied + "   U:" + this.stats[m][r].countUseless + NEW_LINE);
				}
				s.append(NEW_LINE);
			}
			return s.toString();
		}
	}
}
