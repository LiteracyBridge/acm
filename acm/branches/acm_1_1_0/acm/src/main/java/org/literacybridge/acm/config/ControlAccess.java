package org.literacybridge.acm.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.Date;

import javax.swing.JOptionPane;

import org.apache.commons.io.FileUtils;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.repository.AudioItemRepository.AudioFormat;
import org.literacybridge.acm.repository.CachingRepository;
import org.literacybridge.acm.repository.FileSystemRepository;

public class ControlAccess {

    private static File sandboxDirectory = null;
    static boolean readOnly = false;
    private static boolean sandbox = false;

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

	private static File getLockFile() {
		return new File(Configuration.getDatabaseDirectory(), Constants.USER_WRITE_LOCK_FILENAME);
	}

	private static File getDBAccessFile() {
		return new File(Configuration.getDatabaseDirectory(), Constants.DB_ACCESS_FILENAME);
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
			connection = new URL("http://dropbox.com").openConnection();
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
		if (!ControlAccess.isACMReadOnly() || !isSandbox())
			determineRWStatus();
		else 
			System.out.println("Command-line forced read-only or sandbox mode.");
		if (ControlAccess.isACMReadOnly() || isSandbox()) {
			// ACM should be read-only -- change db pointer
			createDBMirror();
			File fDB = new File(Configuration.getACMDirectory(),Constants.DBHomeDir);
			Configuration.setDatabaseDirectory(fDB);
			if (isSandbox()) {
				File fSandbox = new File(Configuration.getACMDirectory(),Constants.RepositoryHomeDir);
				setSandboxDirectory(fSandbox);				
			} else 
				setSandboxDirectory(null);
		}
		System.out.println("  Database:" + Configuration.getDatabaseDirectory());
		System.out.println("  Repository:" + Configuration.getRepositoryDirectory());
		if (isSandbox()) {
			System.out.println("  Sandbox:" + getSandboxDirectory());
		}
		System.out.println("  UserRWAccess:" + userHasWriteAccess());
		System.out.println("  online:" + isOnline());
		System.out.println("  isAnotherUserWriting:" + isAnotherUserWriting());

		Configuration.setRepository(new CachingRepository(
				new FileSystemRepository(Configuration.getCacheDirectory(), 
						new FileSystemRepository.FileSystemGarbageCollector(Constants.CACHE_SIZE_IN_BYTES,
							new FilenameFilter() {
								@Override public boolean accept(File file, String name) {
									return name.toLowerCase().endsWith("." + AudioFormat.WAV.getFileExtension());
								}
							})),
				new FileSystemRepository(Configuration.getRepositoryDirectory()),
				getSandboxDirectory()==null?null:new FileSystemRepository(getSandboxDirectory())));
//		instance.repository.convert(audioItem, targetFormat);				
	}

	private static void createDBMirror() {
		// String line;
		String fromDirname = Configuration.getDatabaseDirectory().getAbsolutePath();
		String toDirname = Configuration.getACMDirectory();
		File fromDir = new File(fromDirname);
		File toDir = new File(toDirname);
		try {
			System.out.println("Started DB Mirror:"+ Calendar.getInstance().get(Calendar.MINUTE)+":"+Calendar.getInstance().get(Calendar.SECOND)+":"+Calendar.getInstance().get(Calendar.MILLISECOND));
			FileUtils.copyDirectoryToDirectory(fromDir, toDir);
			System.out.println("Completed DB Mirror:"+ Calendar.getInstance().get(Calendar.MINUTE)+":"+Calendar.getInstance().get(Calendar.SECOND)+":"+Calendar.getInstance().get(Calendar.MILLISECOND));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	private static void determineRWStatus() {
		boolean sandboxMode = false;
		boolean forced = false;

		if (!userHasWriteAccess()) {
			sandboxMode = true; 
		} else {
			String dialogMessage = new String();
			Object[] options = {"Use Demo Mode", "Shutdown", "Force Write Mode"};
			if (!isOnline()) {
				sandboxMode = true;
				dialogMessage = "Cannot connect to dropbox.com.";
			} else if (isAnotherUserWriting()) {
				sandboxMode = true;
				dialogMessage = "Another user currently has write access to the ACM.\n";
				String line;
				File f = getLockFile();
				if (f.exists()) {
					try {
						BufferedReader in = new BufferedReader(new FileReader(f));
						while ((line = in.readLine()) != null) {
							dialogMessage += line + "\n";
						}
						in.close();
					} catch (IOException e) {
						throw new RuntimeException("Unable to load lock file: " + f, e);
					}
				}
			} 
			if (sandboxMode) {
				int n = JOptionPane.showOptionDialog(null, dialogMessage,"Cannot Get Write Access",JOptionPane.YES_NO_CANCEL_OPTION,
						JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
				if (n == -1) // user closed option pane
					System.exit(0);
				else if (n==1) {
					JOptionPane.showMessageDialog(null,"Shutting down.");
					System.exit(0);
				} else if (n==2) {
					dialogMessage = "Forcing write mode could cause another user\nto lose their work or could corrupt the database!!!\n\n" +
							"If you are sure you want to force write access,\ntype the word 'force' below.";
					String confirmation = (String)JOptionPane.showInputDialog(null,dialogMessage);
					if (confirmation != null && confirmation.equalsIgnoreCase("force")) {
						sandboxMode = false;
						forced = true;
						JOptionPane.showMessageDialog(null,"You now have write access, but database corruption may occur\nif another user is also currently writing to the ACM.");
					}
					else {
						sandboxMode = true;
					}
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
			try {
				File lockFile = getLockFile();
				BufferedWriter output = new BufferedWriter(new FileWriter(lockFile));
			    output.write("User:"+ Configuration.getUserName() + "    ");
				output.write("Contact:" + Configuration.getUserContact() + "\n");
			    output.write("Time:" + new Date().toString());
				output.close();
				lockFile.deleteOnExit();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static boolean isAnotherUserWriting() {
		boolean result;
		File f = getLockFile();
		result = f.exists();
		return result;
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

}
