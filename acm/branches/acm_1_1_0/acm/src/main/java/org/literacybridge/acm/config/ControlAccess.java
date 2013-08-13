package org.literacybridge.acm.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import org.apache.commons.io.FileUtils;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.repository.AudioItemRepository.AudioFormat;
import org.literacybridge.acm.repository.CachingRepository;
import org.literacybridge.acm.repository.FileSystemRepository;
import org.literacybridge.acm.utils.ZipUnzip;

public class ControlAccess {
	private static final Logger LOG = Logger.getLogger(ControlAccess.class.getName());
	private final static String DB_ZIP_FILENAME_PREFIX = Constants.DBHomeDir;
	private final static String DB_ZIP_FILENAME_INITIAL = new String (DB_ZIP_FILENAME_PREFIX + "1.zip");
	private final static String DB_DOES_NOT_EXIST = "NULL"; // PHP returns this if no checkin file found
	private final static String DB_KEY_OVERRIDE = new String("force");
    boolean readOnly = false;
    private boolean sandbox = false;
    private String possessor;
    private DBInfo dbInfo;
    private final DBConfiguration config;
    
    public ControlAccess(DBConfiguration config) {
    	this.config = config;
    }
    
    private void setDBKey(String key) {
    	dbInfo.setDbKey(key);
    }
    
    private String getDBKey() {
    	return dbInfo.getDbKey();
    }

    private void setPossessor(String name) {
    	possessor = name;
    }
    
    private String getPosessor() {
    	return possessor;
    }

    private boolean setZipFilenames(String currentFilename) {
    	boolean goodName = false;
    	String nextFilename = null;
    	
    	if (currentFilename == null) { 
    		goodName = false;
    		// no db zip exists in dropbox but the db has been checked in; no need for setNextZipFilename, because time to shutdown
    	} else if (currentFilename.equalsIgnoreCase(DB_DOES_NOT_EXIST)) {
    		// ACM does not yet exist, so create name for newly created zip to use on updateDB()
    		nextFilename = new String(DB_ZIP_FILENAME_INITIAL);
    		goodName = true;
    	} else {
	    	String baseFilename = currentFilename.substring(DB_ZIP_FILENAME_PREFIX.length(), currentFilename.lastIndexOf('.'));
	    	try {
		    	int count = Integer.parseInt(baseFilename) + 1;
		    	nextFilename = new String (DB_ZIP_FILENAME_PREFIX + String.valueOf(count) + currentFilename.substring(currentFilename.lastIndexOf('.')));
		    	goodName = true;
	    	} catch (NumberFormatException e) {
	    		// there's some strange .zip -- probably a "(conflicted copy)" or something else weird -- don't use it!
	    		currentFilename = null;
	    		goodName = false;
	    		LOG.log(Level.WARNING, "Unable to parse filename " + currentFilename);
	    	}
    	}
    	dbInfo.setFilenames(currentFilename,nextFilename);
    	return goodName;
    }

    private String getCurrentZipFilename() {
    	return dbInfo.getCurrentFilename();
    }
    
    public String getNextZipFilename() {
    	return dbInfo.getNextFilename();
    }
    
	public File getSandboxDirectory() {
		File fSandbox = null; 
		if (isSandbox()) {
			fSandbox = new File(config.getTempACMsDirectory(),config.getSharedACMname() + "/" + Constants.RepositoryHomeDir);
		}
		return fSandbox;
	}

// Commenting out since getSandboxDirectory() now calculates path from Constants and other roots
//    private static void setSandboxDirectory(File f) {
//   	sandboxDirectory = f; 
//    }
    
	public boolean isACMReadOnly() {
		return readOnly;
	}	

	private void setReadOnly(boolean isRW) {
		readOnly = isRW;
	}

	private File getDBAccessFile() {
		return new File(config.getSharedACMDirectory(), Constants.DB_ACCESS_FILENAME);
	}

	public boolean isSandbox() {
		return sandbox;
	}
	
	private void setSandbox(boolean isSandbox) {
		sandbox = isSandbox;
	}

	private void deleteLocalDB() {
		try {
			// deleting old local DB so that next startup knows everything shutdown normally
			File oldDB = new File(config.getTempACMsDirectory(), config.getSharedACMname());
			FileUtils.deleteDirectory(oldDB);
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	private boolean isOnline() {
		boolean result = false;
		URLConnection connection = null;
		try {
			connection = new URL("http://literacybridge.org").openConnection();
			connection.connect();
			result = true;
		} catch (MalformedURLException e) {
			// this should not ever happen (if the URL above is good)
			e.printStackTrace();
		} catch (IOException e) {
			result = false;
			//e.printStackTrace();
		} 
		return result;
	}

	public void init() {
		try {
			@SuppressWarnings("unused")
			LockACM l = new LockACM(config);
		} catch (RuntimeException e) {
			JOptionPane.showMessageDialog(null, "This ACM is already opened.");
			System.exit(0);
		}
		if (dbInfo == null)
			dbInfo = new DBInfo(config);
		if (dbInfo.isCheckedOut()) {
			setSandbox(false);
			if (!ACMConfiguration.isDisableUI()) {
				JOptionPane.showMessageDialog(null,"You have already checked out this ACM.\nYou can now continue making changes to it.");
			}				
		} else {
			deleteLocalDB();			
			determineRWStatus();
			createDBMirror();				
		}
		System.out.println("  Repository:                     " + config.getRepositoryDirectory());
		System.out.println("  Temp Database:                  " + config.getDatabaseDirectory());
		System.out.println("  Temp Repository (sandbox mode): " + getSandboxDirectory());
		System.out.println("  UserRWAccess:                   " + userHasWriteAccess());
		System.out.println("  online:                         " + isOnline());

		config.setRepository(new CachingRepository(
				new FileSystemRepository(config.getCacheDirectory(), 
						new FileSystemRepository.FileSystemGarbageCollector(Constants.CACHE_SIZE_IN_BYTES,
							new FilenameFilter() {
								@Override public boolean accept(File file, String name) {
									return name.toLowerCase().endsWith("." + AudioFormat.WAV.getFileExtension());
								}
							})),
				new FileSystemRepository(config.getRepositoryDirectory()),
				isSandbox()?new FileSystemRepository(getSandboxDirectory()):null));
//		instance.repository.convert(audioItem, targetFormat);				
	}

	private void createDBMirror() {
		// String line;
		File toDir = config.getDatabaseDirectory();
		String dbFilename = getCurrentZipFilename();
		try {
			//File oldDB = new File (toDir,Configuration.getSharedACMname());
			//if (dbFilename.equalsIgnoreCase(ControlAccess.DB_DOES_NOT_EXIST)) {
			//	oldDB.mkdir(); // recreate db directory that was just deleted
			//	System.out.println("Created new empty DB dir");
			//} else {
				File dbZip = new File(config.getSharedACMDirectory(),dbFilename);
				System.out.println("Started DB Mirror:"+ Calendar.getInstance().get(Calendar.MINUTE)+":"+Calendar.getInstance().get(Calendar.SECOND)+":"+Calendar.getInstance().get(Calendar.MILLISECOND));
				ZipUnzip.unzip(dbZip, toDir);
				//FileUtils.copyDirectoryToDirectory(fromDir, toDir);
				System.out.println("Completed DB Mirror:"+ Calendar.getInstance().get(Calendar.MINUTE)+":"+Calendar.getInstance().get(Calendar.SECOND)+":"+Calendar.getInstance().get(Calendar.MILLISECOND));
			//} 
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}

	private boolean setCurrentFileToLastModified() {
		String filenameFallback = null;
		File[] files;
		long lastModified = 0;
		boolean foundOne = false;
		
		files = config.getSharedACMDirectory().listFiles(new FilenameFilter() {
			@Override public boolean accept(File dir, String name) {
				String lowercase = name.toLowerCase();
				return lowercase.endsWith(".zip");
			}			
		});
		for (File file : files) {
			if (file.lastModified() > lastModified) {
				lastModified = file.lastModified();
				filenameFallback = file.getName();
				if (setZipFilenames(filenameFallback)) {
					foundOne = true;
				}
			}
		}
		return foundOne;
	}
	
	private boolean haveLatestDB() {
		String filenameShouldHave = getCurrentZipFilename();
		
		if (filenameShouldHave.equalsIgnoreCase(ControlAccess.DB_DOES_NOT_EXIST))
			return true;  // if the ACM is new, you have the latest there is (nothing)
		
		File fileShouldHave = new File(config.getSharedACMDirectory(),filenameShouldHave);
		boolean status;
		if (fileShouldHave.exists()) {
			status = true;
		} else {
			// latest db zip to be checked in is not yet in dropbox, so find most recent zip and force sandbox mode
			status = false;
			setCurrentFileToLastModified();
		}
		return status;
	}
	
	private void determineRWStatus() {
		boolean sandboxMode = false;
		String dialogMessage = new String();
		boolean dbAvailable = false;
		boolean outdatedDB = false;
		boolean online;
		
		if (!isOnline()) {
			online = false;
			sandboxMode = true;
			if (setCurrentFileToLastModified()) {	
				if (!ACMConfiguration.isDisableUI()) {
					JOptionPane.showMessageDialog(null,"Cannot connect to Literacy Bridge server.");
				}
			} else {
				if (!ACMConfiguration.isDisableUI()) {
					JOptionPane.showMessageDialog(null,"Cannot connect to Literacy Bridge server and no available database. Shutting down.");
				}
				System.exit(0);
			}
		} else {
			online = true;
			int onlineChoice;
			do {
				onlineChoice = 1; // don't repeat loop unless user chooses to
				try {
					dbAvailable = checkOutDB(config.getSharedACMname(), new String("statusCheck"));
					break;
				} catch (IOException e) {					
					dbAvailable = false;
					Object[] options = {"Try again", "Use Demo Mode"};
					if (!ACMConfiguration.isDisableUI()) {
						onlineChoice = JOptionPane.showOptionDialog(null, "Cannot reach Literacy Bridge web server.  Do you want to get online now and try again or use Demo Mode?", "Cannot Connect to Server",JOptionPane.YES_NO_CANCEL_OPTION,
								JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
					}
					if (onlineChoice == -1)
						System.exit(0);
				}
			} while (onlineChoice == 0);
			if (dbAvailable && !haveLatestDB()) {
				if (getCurrentZipFilename() == null) {
					// no zip exists at all -- must shut down
					if (!ACMConfiguration.isDisableUI()) {
						JOptionPane.showMessageDialog(null,"There is no copy of this ACM database on this computer.\nIt may be that the database has not been uploaded and downloaded yet.\nShutting down.");
					}
					System.exit(0);					
				} else {
					outdatedDB = true;
				}
			}
			if (!userHasWriteAccess() || !dbAvailable || outdatedDB) {
				sandboxMode = true; 
			} 
			if (outdatedDB) {
				dialogMessage = "The latest version of the ACM database has not yet downloaded to this computer.\nYou may shutdown and wait or begin demonstration mode with the previous version.";
			} else if (!dbAvailable && online) {
				dialogMessage = "Another user currently has write access to the ACM.\n";
				dialogMessage += getPosessor() + "\n";
			} 
			if (online && sandboxMode && userHasWriteAccess() && !ACMConfiguration.isDisableUI()) {
				Object[] optionsNoForce = {"Use Demo Mode", "Shutdown"};
				Object[] optionsForce = {"Use Demo Mode", "Shutdown", "Force Write Mode"};
	
				int n = JOptionPane.showOptionDialog(null, dialogMessage,"Cannot Get Write Access",JOptionPane.YES_NO_CANCEL_OPTION,
						JOptionPane.QUESTION_MESSAGE, null, (optionsNoForce), optionsNoForce[0]);
				if (n == -1 || n == 1) { // user closed option pane or selected Shutdown
					JOptionPane.showMessageDialog(null,"Shutting down.");
					System.exit(0);
				} 
//				PEOPLE DON'T RESPECT THE FORCE WARNING, SO WE ARE TAKING AWAY THAT POWER
//				else if (n==2) {
//					dialogMessage = "Forcing write mode will cause " + getPosessor() + "\nto lose their work!\n\n" +
//							"If you are sure you want to force write access,\ntype the word 'force' below.";
//					String confirmation = (String)JOptionPane.showInputDialog(null,dialogMessage);
//					if (confirmation != null && confirmation.equalsIgnoreCase("force")) {
//						sandboxMode = false;
//						do {	
//							onlineChoice = 1; // don't repeat loop unless user chooses to
//							try {
//								forced = dbAvailable = checkOutDB(Configuration.getSharedACMname(), DB_KEY_OVERRIDE);				
//								sandboxMode = false;
//								break;
//							} catch (IOException e) {					
//								forced = dbAvailable = false;
//								sandboxMode = true;
//								Object[] options = {"Try again", "Use Demo Mode"};
//								onlineChoice = JOptionPane.showOptionDialog(null, "Cannot reach Literacy Bridge web server.  Do you want to get online now and try again or use Demo Mode?", "Cannot Connect to Server",JOptionPane.YES_NO_CANCEL_OPTION,
//										JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
//								if (onlineChoice == -1)
//									System.exit(0);
//							}
//						} while (onlineChoice == 0);
//
//						if (forced)
//							JOptionPane.showMessageDialog(null,"You now have write access.");
//						else
//							JOptionPane.showMessageDialog(null,"Forcing did not work.");
//					}
//					else {
//						sandboxMode = true;
//					}
//				}
			}
			if (!sandboxMode) {
				int n = 0;
				if (!ACMConfiguration.isDisableUI()) {
					if (getCurrentZipFilename().equalsIgnoreCase(DB_DOES_NOT_EXIST)) {
						JOptionPane.showMessageDialog(null,"ACM does not exist yet. Creating a new ACM and giving you write access.");					
					} else {
						Object[] options = {"Update Shared Database", "Use Demo Mode"};
						n = JOptionPane.showOptionDialog(null, "Do you want to update the shared database?","Update or Demo Mode?",JOptionPane.YES_NO_OPTION,
								JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
					}
				}
				if (n == -1) // user closed option pane
					System.exit(0);
				else if (n==1)
					sandboxMode = true;
				else {
					do {	
						onlineChoice = 1; // don't repeat loop unless user chooses to
						try {
							dbAvailable = checkOutDB(config.getSharedACMname(),"checkout");
							sandboxMode = false;
							break;
						} catch (IOException e) {					
							dbAvailable = false;
							sandboxMode = true;
							if (!ACMConfiguration.isDisableUI()) {
								Object[] options = {"Try again", "Use Demo Mode"};
								onlineChoice = JOptionPane.showOptionDialog(null, "Cannot reach Literacy Bridge web server.  Do you want to get online now and try again or use Demo Mode?", "Cannot Connect to Server",JOptionPane.YES_NO_CANCEL_OPTION,
										JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
							}
							if (onlineChoice == -1)
								System.exit(0);
						}
					} while (onlineChoice == 0);


					if (!dbAvailable && !sandboxMode && !ACMConfiguration.isDisableUI()) {
						JOptionPane.showMessageDialog(null,"Sorry, but another user must have just checked out this ACM a moment ago!\nTry contacting " + getPosessor() + "\nAfter clicking OK, the ACM will shutdown.");
						System.exit(0);
					} else if (!sandboxMode) {
						if (getCurrentZipFilename().equalsIgnoreCase(DB_DOES_NOT_EXIST)) {
							setDBKey(DB_KEY_OVERRIDE);
						}
					}
				}
			}
		}
		setSandbox(sandboxMode);
		if (sandboxMode) {
			if (!ACMConfiguration.isDisableUI()) {
				JOptionPane.showMessageDialog(null,"The ACM is running in demonstration mode.\nPlease remember that your changes will not be saved.");
			}
		} 
	}
	
	public boolean userHasWriteAccess() {
		String writeUser, thisUser;
		boolean userHasWriteAccess = false;
		
		thisUser = config.getUserName();
		if (thisUser == null) {
			// No User Name found in config.properties.  Forcing Read-Only mode.
			return false;
		}
		File f = getDBAccessFile();
		if (f.exists()) {
			try {
				BufferedReader in = new BufferedReader (new FileReader(f));
				while ((writeUser = in.readLine()) != null) {
					if (writeUser.equalsIgnoreCase(thisUser)) {
						userHasWriteAccess = true;
						break;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else if (config.getSharedACMDirectory().list().length == 0) {
			// empty directory -- which means that a new directory was created to start an ACM in
			// Since the directory already exists, it is not the case that the user just hasn't accepted the dropbox invitaiton yet.
			// So, give this user write access to the newly created ACM
			userHasWriteAccess = true;
			try {
				BufferedWriter out = new BufferedWriter(new FileWriter(f));
				out.write(thisUser + "\n");
				out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			// Cannot find database access list. Forcing Read-Only mode. 
			return false;
		}
		return userHasWriteAccess;
	}

	private boolean checkOutDB(String db, String action) throws IOException {
		boolean status = true;
		String filename = null, key = null, possessor = null;
		URL url;
		String computerName;

		try {
			computerName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e1) {
			computerName = "UNKNOWN";
		}
		

		url = new URL("http://literacybridge.org/checkout.php?db=" + db + "&action=" + action + "&name=" + config.getUserName() + "&contact=" + config.getUserContact()+ "&version=" + Constants.ACM_VERSION + "&computername=" + computerName);
		InputStream in;
		in = url.openStream();
		Scanner scanner = new Scanner(in);
	    //scanner.useDelimiter(END_OF_INPUT);
 	    while (scanner.hasNext()) {
 	    	String s = scanner.next();
 	    	if (s.startsWith("key="))
 	    		key = s.substring(s.indexOf('=')+1);
 	    	else if (s.startsWith("filename="))
 	    		filename = s.substring(s.indexOf('=')+1);
 	    	else if (s.startsWith("possessor=")) {
 	    		possessor = s.substring(s.indexOf('=')+1);
 	    		status = false;
 	    	} 	 	    	
 	    }
	    if (filename != null)
	    	setZipFilenames(filename);
 	    if (status) {
 	    	if (key != null) {
 	    		setDBKey(key);
 	    	    dbInfo.setCheckedOut(true);
 	    	}
 	    } else if (possessor != null) {
 	    	setPossessor(possessor);
 	    }
	    return status;
	}

	private boolean checkInDB(String db, String key, String filename) throws IOException {
		boolean status = false;
		final String END_OF_INPUT = "\\Z";
		String s = null;
		String computerName;
		URL url;
		String action;
		
		if (filename == null) {
			action = "discard";
			filename = "";
		} else {
			action = "checkin";
		}
		
		try {
			computerName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e1) {
			computerName = "UNKNOWN";
		}
		
//		try {
			url = new URL("http://literacybridge.org/checkin.php?db=" + db + "&action=" + action + "&key=" + key + "&filename=" + filename + "&name=" + config.getUserName() + "&contact=" + config.getUserContact()+ "&version=" + Constants.ACM_VERSION + "&computername=" + computerName);
			InputStream in;
			in = url.openStream();
			Scanner scanner = new Scanner(in);
		    scanner.useDelimiter(END_OF_INPUT);
	 	    s = scanner.next();
//		}
//		catch (IOException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		if (action.equals("discard"))
			status = true;
	 	else if (s == null)
			status = false;
		else
			status = s.trim().equals("ok"); 
 	    return status;
	}

	public boolean updateDB() {
		boolean status = false;
		boolean saveWork = true;
		String key = getDBKey();
		String filename = getNextZipFilename();
		File inFolder= config.getDatabaseDirectory();
		File outFile= new File (config.getSharedACMDirectory(), getNextZipFilename()); 
		int n;
		if (!ACMConfiguration.isDisableUI()) {
			Object[] optionsSaveWork = {"Save Work", "Throw Away Your Latest Changes"};
			n = JOptionPane.showOptionDialog(null, "If you made a mistake you can throw away all your changes now.", "Save Work?",JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE, null, optionsSaveWork, optionsSaveWork[0]);
			if (n == 1) {
				n = JOptionPane.showOptionDialog(null, "Are you sure you want to throw away all your work since opening the ACM?", "Are You Sure?",JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.WARNING_MESSAGE, null, null, JOptionPane.CANCEL_OPTION);
				if (n == JOptionPane.OK_OPTION) {
					saveWork = false;
					filename = null;  // release checkout
				}
			}
		}
		n = 0; // we always want to start this loop.  Even if the user wants to discard changes, we need to try to release the checkout 
		while (!status && n==0) { 
			n = 1;  // shutdown by default if no UI
			if (saveWork) {
				try {
					ZipUnzip.zip(inFolder, outFile);
				} catch (IOException e) {
					status = false;
					if (!ACMConfiguration.isDisableUI()) {
						Object[] options = {"Keep Your Changes", "Throw Away Your Latest Changes"};
						n = JOptionPane.showOptionDialog(null, "There is a problem getting your changes into Dropbox.  Do you want to keep your changes and try to get this problem fixed or throw away your latest changes?", "Problem creating zip file on Dropbox",JOptionPane.YES_NO_CANCEL_OPTION,
								JOptionPane.WARNING_MESSAGE, null, options, options[0]);
					}
					if (n==1) {
						saveWork = false; 
						filename = null;  // release checkout
					} else {
						break; // keep changes - so don't try to check-in, which happens in the remainder of the while loop below
					}
				}
			}
			try {
				status = checkInDB(config.getSharedACMname(), key, filename); 
				if (!status && saveWork && !ACMConfiguration.isDisableUI()) {
					//Object[] options = {"Force your version","Throw Away Your Latest Changes"};
					JOptionPane.showMessageDialog(null, "Someone has forced control of this ACM, so you cannot check-in your changes.\nIf you are worried about losing a lot of work, contact Cliff and he may be able to save you and your work.");
					saveWork = false;  // zip is alredy written to dropbox an could be recovered.  !saveWork & status will delete checkoutfile marker but not any zips.
					status = true; // to 
					//if (n==0)
					//	key = ControlAccess.DB_KEY_OVERRIDE;
					//else 
				}
			} catch (IOException e) {
				status = false;
				if (!ACMConfiguration.isDisableUI()) {
					Object[] options = {"Try again", "Shutdown"};
					n = JOptionPane.showOptionDialog(null, "Cannot reach Literacy Bridge web server.\nDo you want to get online and try again or shutdown and try later?", "Cannot Connect to Server",JOptionPane.YES_NO_CANCEL_OPTION,
							JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
				}
			}
		} 
		
		if (!ACMConfiguration.isDisableUI()) {
			if (status && saveWork)
				JOptionPane.showMessageDialog(null,"Your changes have been checked in.\n\nPlease stay online for a few minutes so your changes\ncan be uploaded (until Dropbox is 'Up to date').");
			else if (saveWork && !status)
				JOptionPane.showMessageDialog(null,"Your changes could not be checked in now, but you still have this ACM\nchecked out and can submit your changes later.");
			else if (status && !saveWork)
				JOptionPane.showMessageDialog(null,"Your changes have been discarded.");
			else if (!status && !saveWork)
				JOptionPane.showMessageDialog(null,"Could not release your checkout.  Please try again later so that others can checkout this ACM.");
		}
		
		if (status && saveWork) {
			// deleting old zip since we just got confirmation that the new zip was checkedin
			File oldzip = new File (config.getSharedACMDirectory(), getCurrentZipFilename());
			oldzip.delete();
		}
		if (status) {  // whether saving work or not, only delete checkout file if status==true, meaning checkIn was successful
			dbInfo.deleteCheckoutFile(); // do this first since it's most important to be deleted and the next line sometimes is unable to delete the entire directory
			// deleteLocalDB();  -- this line always fails to delete some files, so we'll just delete on startup if there is no checkout file
		}
		return status;
	}
}

