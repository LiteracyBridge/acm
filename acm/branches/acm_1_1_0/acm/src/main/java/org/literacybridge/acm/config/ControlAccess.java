package org.literacybridge.acm.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;

import javax.swing.JOptionPane;

import org.apache.commons.io.FileUtils;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.repository.AudioItemRepository.AudioFormat;
import org.literacybridge.acm.repository.CachingRepository;
import org.literacybridge.acm.repository.FileSystemRepository;
import org.literacybridge.acm.utils.ZipUnzip;

public class ControlAccess {

	private static String DB_ZIP_FILENAME_PREFIX = Constants.DBHomeDir;
    private static File sandboxDirectory = null;
    static boolean readOnly = false;
    private static boolean sandbox = false;
    private static String dbKey;
    private static String possessor;
    private static String currentZipFilename;
    private static String nextZipFilename;
    
    private static void setDBKey(String key) {
    	dbKey = key;
    }
    
    private static String getDBKey() {
    	return dbKey;
    }

    private static void setPossessor(String name) {
    	possessor = name;
    }
    
    private static String getPosessor() {
    	return possessor;
    }

    private static void setCurrentZipFilename(String filename) {
    	currentZipFilename = filename;

    	String baseFilename = filename.substring(DB_ZIP_FILENAME_PREFIX.length(), filename.lastIndexOf('.'));
    	int count = Integer.parseInt(baseFilename) + 1;
    	String newFilename = new String (DB_ZIP_FILENAME_PREFIX + String.valueOf(count) + filename.substring(filename.lastIndexOf('.')));
    	setNextZipFilename(newFilename);
    }

    private static String getCurrentZipFilename() {
    	return currentZipFilename;
    }
    
    private static void setNextZipFilename(String filename) {
    	nextZipFilename = filename;
    }

    public static String getNextZipFilename() {
    	return nextZipFilename;
    }
    
	public static File getSandboxDirectory() {
		return sandboxDirectory;
	}

    private static void setSandboxDirectory(File f) {
    	sandboxDirectory = f; 
    }
    
	public static boolean isACMReadOnly() {
		return readOnly;
	}	

	private void setReadOnly(boolean isRW) {
		readOnly = isRW;
	}

	private static File getDBAccessFile() {
		return new File(Configuration.getSharedACMDirectory(), Constants.DB_ACCESS_FILENAME);
	}

	public static boolean isSandbox() {
		return sandbox;
	}
	
	private static void setSandbox(boolean isSandbox) {
		sandbox = isSandbox;
	}
	
	private static boolean isOnline() {
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

	public static void determineAccess() {
//		if (!ControlAccess.isACMReadOnly() || !isSandbox())
			determineRWStatus();
//		else 
//			System.out.println("Command-line forced read-only or sandbox mode.");
//		if (ControlAccess.isACMReadOnly() || isSandbox()) {
			// ACM should be read-only -- change db pointer
			createDBMirror();
			File fDB = new File(Configuration.getACMDirectory(),Constants.DBHomeDir);
			Configuration.setDatabaseDirectory(fDB);
//			if (isSandbox()) {
				File fSandbox = new File(Configuration.getACMDirectory(),Constants.RepositoryHomeDir);
				setSandboxDirectory(fSandbox);				
//			} else 
//				setSandboxDirectory(null);
	//	}
		System.out.println("  Database:" + Configuration.getDatabaseDirectory());
		System.out.println("  Repository:" + Configuration.getRepositoryDirectory());
//		if (isSandbox()) {
			System.out.println("  Sandbox:" + getSandboxDirectory());
//		}
		System.out.println("  UserRWAccess:" + userHasWriteAccess());
		System.out.println("  online:" + isOnline());

		Configuration.setRepository(new CachingRepository(
				new FileSystemRepository(Configuration.getCacheDirectory(), 
						new FileSystemRepository.FileSystemGarbageCollector(Constants.CACHE_SIZE_IN_BYTES,
							new FilenameFilter() {
								@Override public boolean accept(File file, String name) {
									return name.toLowerCase().endsWith("." + AudioFormat.WAV.getFileExtension());
								}
							})),
				new FileSystemRepository(Configuration.getRepositoryDirectory()),
				new FileSystemRepository(getSandboxDirectory())));
//		instance.repository.convert(audioItem, targetFormat);				
	}

	private static void createDBMirror() {
		// String line;
		File dbZip = new File(Configuration.getSharedACMDirectory(),ControlAccess.getCurrentZipFilename());
		File toDir = new File(Configuration.getACMDirectory());
		try {
			File oldDB = new File (toDir,Constants.DBHomeDir);
			FileUtils.deleteDirectory(oldDB);
			System.out.println("Started DB Mirror:"+ Calendar.getInstance().get(Calendar.MINUTE)+":"+Calendar.getInstance().get(Calendar.SECOND)+":"+Calendar.getInstance().get(Calendar.MILLISECOND));
			ZipUnzip.unzip(dbZip, toDir, Constants.DBHomeDir);
			//FileUtils.copyDirectoryToDirectory(fromDir, toDir);
			System.out.println("Completed DB Mirror:"+ Calendar.getInstance().get(Calendar.MINUTE)+":"+Calendar.getInstance().get(Calendar.SECOND)+":"+Calendar.getInstance().get(Calendar.MILLISECOND));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	private static void determineRWStatus() {
		boolean sandboxMode = false;
		boolean forced = false;
		String dialogMessage = new String();
		boolean dbAvailable = false;
		
		if (!isOnline()) {
			sandboxMode = true;
			dialogMessage = "Cannot connect to dropbox.com.";
		} else {
			dbAvailable = checkOutDB(Configuration.getSharedACMname(), new String("statusCheck"), new String ("v1"));
		}
		if (!userHasWriteAccess()) {
			sandboxMode = true; 
		} else if (!dbAvailable) {
			sandboxMode = true;
			dialogMessage = "Another user currently has write access to the ACM.\n";
			dialogMessage += getPosessor() + "\n";
		} 
		if (sandboxMode && userHasWriteAccess()) {
			Object[] options = {"Use Demo Mode", "Shutdown", "Force Write Mode"};
			int n = JOptionPane.showOptionDialog(null, dialogMessage,"Cannot Get Write Access",JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
			if (n == -1) // user closed option pane
				System.exit(0);
			else if (n==1) {
				JOptionPane.showMessageDialog(null,"Shutting down.");
				System.exit(0);
			} else if (n==2) {
				dialogMessage = "Forcing write mode will cause another user\nto lose their work!\n\n" +
						"If you are sure you want to force write access,\ntype the word 'force' below.";
				String confirmation = (String)JOptionPane.showInputDialog(null,dialogMessage);
				if (confirmation != null && confirmation.equalsIgnoreCase("force")) {
					sandboxMode = false;
					forced = dbAvailable = checkOutDB(Configuration.getSharedACMname(), new String("force"), new String ("v1"));
					if (forced)
						JOptionPane.showMessageDialog(null,"You now have write access.");
					else
						JOptionPane.showMessageDialog(null,"Forcing did not work.");
				}
				else {
					sandboxMode = true;
				}
			}
		}
		if (!forced && !sandboxMode && !Configuration.isDisableUI()) {
			Object[] options = {"Update Shared Database", "Use Demo Mode"};
			int n = JOptionPane.showOptionDialog(null, "Do you want to update the shared database?","Update or Demo Mode?",JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
			if (n == -1) // user closed option pane
				System.exit(0);
			else if (n==1)
				sandboxMode = true;
		}
		setSandbox(sandboxMode);
		if (sandboxMode) {
			if (!Configuration.isDisableUI()) {
				JOptionPane.showMessageDialog(null,"The ACM is running in demonstration mode.\nPlease remeber that your changes will not be saved.");
			}
		} else {
		    // ACM is now read-write, so need to lock other users
			dbAvailable = checkOutDB(Configuration.getSharedACMname(), Configuration.getUserName() + ":" + Configuration.getUserContact(), new String ("v1"));
		}
	}
	
	public static boolean userHasWriteAccess() {
		String writeUser, thisUser;
		boolean userHasWriteAccess = false;
		
		thisUser = Configuration.getUserName();
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
		} else {
			// Cannot find database access list. Forcing Read-Only mode. 
			return false;
		}
		return userHasWriteAccess;
	}

	private static boolean checkOutDB(String db, String contact, String version) {
		boolean status = true;
		String filename = null, key = null, possessor = null;
		
		URL url;
		try {
			url = new URL("http://literacybridge.org/checkout.php?db=" + db + "&contact=" + contact + "&version=" + version);
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
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	    if (filename != null)
	    	setCurrentZipFilename(filename);
 	    if (status) {
 	    	if (key != null) 
 	    		setDBKey(key);
 	    } else if (possessor != null) {
 	    	setPossessor(possessor);
 	    }
	    return status;
	}

	private static boolean checkInDB(String db, String key, String filename) {
		boolean status = true;
		final String END_OF_INPUT = "\\Z";
		String s = null;
		
		URL url;
		try {
			url = new URL("http://literacybridge.org/checkin.php?db=" + db + "&key=" + key + "&filename=" + filename);
			InputStream in;
			in = url.openStream();
			Scanner scanner = new Scanner(in);
		    scanner.useDelimiter(END_OF_INPUT);
	 	    s = scanner.next();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (s == null)
			status = false;
		else
			status = s.trim().equals("ok"); 
 	    return status;
	}

	public static void updateDB() {
		File inFolder= Configuration.getDatabaseDirectory();
		File outFile= new File (Configuration.getSharedACMDirectory(), ControlAccess.getNextZipFilename()); 
		try {
			ZipUnzip.zip(inFolder, outFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		boolean status = checkInDB(Configuration.getSharedACMname(), ControlAccess.getDBKey(), ControlAccess.getNextZipFilename()); 
		if (status) {
			File oldDB = new File (Configuration.getSharedACMDirectory(),ControlAccess.getCurrentZipFilename());
			oldDB.delete();
		}
	}
}

