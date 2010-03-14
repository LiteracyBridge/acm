package org.literacybridge.acm.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.ArrayList;
import java.util.logging.Logger;

import org.literacybridge.acm.content.AudioItem;

/**
 * PersistenceManager
 * 
 * This class is supposed to be the access point if data needs to be stored to 
 * and retrieved from the database.
 */
public class PersistenceManager {

    /** PersistenceManager Logger **/
    private static Logger sLogger = 
        Logger.getLogger(PersistenceManager.class.getName());

    /** Database Access Object **/
    private DatabaseAccess mDao;

    private static boolean mIsInitialized = false;

    /** Singleton **/
    private static PersistenceManager mPersistenceManager;
    
    
    /**
     * Prepared statements for AudioItem
     */
    private static PreparedStatement stmtSaveNewAudioItem;  
    private static PreparedStatement stmtUpdateExistingRecord;
    private static PreparedStatement stmtGetAudioItems;
    private static PreparedStatement stmtDeleteAudioItem;
    
    private static final String sqlSaveNewAudioItem =
        "INSERT INTO t_audioitem(UUID) VALUES(?)";

    private static final String sqlGetAudioItems = 
          "SELECT ID, UUID FROM t_audioitem";    
    
    
    private PersistenceManager() {}
    
    /** Establish a connection to the derby database **/
    private void connect() {
        if (mDao == null) {
            mDao = new DatabaseAccess();
        }
        mDao.connect();  
    }
    
    /** Close exiting connection to the derby database **/
    private void disconnect() {
        mDao.disconnect();
    }

    /** Get Database Access Object **/
    private DatabaseAccess getDatabaseAccess() {
        return mDao;
    }

    /** Returns true if the PersistenceManager has been initialized **/
    private static boolean isInitialized() {
        return mIsInitialized;
    }

    /** Set initialization state **/
    private static void setIsInitialized(boolean status) {
        mIsInitialized = status;
    }

    /** Initialize PersistenceManager **/
    public static void initialize() throws Exception {
        if (isInitialized()) {
            return;
        }
        getInstance().connect();
        getInstance().prepareStatements();
        setIsInitialized(true);
    }

    /** Close all existing database connections **/
    public static void uninitialize() throws Exception {
        getInstance().disconnect();
    }

    /** Send all prepared statements to the database **/
    private void prepareStatements() throws Exception {
        stmtSaveNewAudioItem = 
            getInstance().getDatabaseAccess().prepareStatement(sqlSaveNewAudioItem, 
                                 Statement.RETURN_GENERATED_KEYS);
        
        stmtGetAudioItems = 
            getInstance().getDatabaseAccess().prepareStatement(sqlGetAudioItems, 
                                 Statement.NO_GENERATED_KEYS);
    }

    /** Get the instance of the PersistenceManager **/
    private static PersistenceManager getInstance() throws Exception {
        if (mPersistenceManager == null) {
            mPersistenceManager = new PersistenceManager();
        }
        return mPersistenceManager;
    }

    /** Returns the corresponding persistence object for a passed class. **/
    private Persistable getPersistableObject(Class c) {
        if (c.equals(AudioItem.class)) {
            return new PersistentAudioItem(); 
        }
        return null;
    }

    /** Retrieves all objects for the passed class from the database. **/
    public static <T> T collect(Class c) throws Exception {
        return getInstance()
            .getPersistableObject(c)
            .<T>collect();
    }

    /** Retrieve a specific object from the database **/
    public static <T> T collect(Class c, int id) throws Exception {
        return getInstance()
            .getPersistableObject(c)
            .<T>collect(id);
    }
    
    /** Persist a new object in the database **/
    public static <T> int save(T object) throws Exception {
        return getInstance()
            .getPersistableObject(object.getClass())
            .save(object);
    }
    
    /** Edit an existing object **/
    public static <T> void edit(T object) {
        
    }
    
    /** Delete an object from the database **/
    public static <T> void delete(T object) {
        
    }
    
    /** Delete an object from the database by passing it's ID **/
    public static <T> void delete(int id) {
        
    }
    
    /** 
     * Inner persistence class for AudioItem
     */
    private class PersistentAudioItem implements Persistable {
        
        /** Store an AudioItem into the database **/
        public <T> int save(T object) {
            AudioItem item = (AudioItem) object;
            int id = -1;
            try {
                stmtSaveNewAudioItem.clearParameters();
                stmtSaveNewAudioItem.setString(1, item.getUUId());
                int rowCount = stmtSaveNewAudioItem.executeUpdate();
                ResultSet results = stmtSaveNewAudioItem.getGeneratedKeys();
                if (results.next()) {
                    id = results.getInt(1);
                }
                
            } catch(SQLException sqle) {
                sqle.printStackTrace();
            }
            return id;
        }

        /** Edit an existing AudioItem **/
        public <T> boolean edit(T object) {
            boolean bEdited = false;
            try {
                stmtUpdateExistingRecord.clearParameters();
                
                stmtUpdateExistingRecord.setString(1, "");

                stmtUpdateExistingRecord.executeUpdate();
                bEdited = true;
            } catch(SQLException sqle) {
                sqle.printStackTrace();
            }
            return bEdited;            
        }

        /** Delete an AudioItem from the database by it's ID **/
        public boolean delete(int id) {
            boolean bDeleted = false;
            try {
                stmtDeleteAudioItem.clearParameters();
                stmtDeleteAudioItem.setInt(1, id);
                stmtDeleteAudioItem.executeUpdate();
                bDeleted = true;
            } catch (SQLException sqle) {
                sqle.printStackTrace();
            }
            
            return bDeleted;            
        }

        /** Delete an AudioItem from the database **/
        public <T> boolean delete(T object) {
            AudioItem item = (AudioItem) object;
            return delete(item.getId());
        }

        /** Retrieves all AudioItem instances from the database **/
        public <T> T collect() {
            Collection<AudioItem> listEntries = new ArrayList<AudioItem>();
            ResultSet results = null;
            
            try {
                stmtGetAudioItems.clearParameters();
                results = stmtGetAudioItems.executeQuery();
                while(results.next()) {
                    int id = results.getInt(1);  
                    String uuid = results.getString(2);
                    
                    AudioItem item = new AudioItem(uuid);
                    item.setId(id);
                    
                    listEntries.add(item);
                }
                
            } catch (SQLException sqle) {
                sqle.printStackTrace();
                
            }
            
            return (T) listEntries;
        }

        /** Retrieves a specific AudioItem from the database **/
        public <T> T collect(int id) {
            T item = null;
            try {
                stmtGetAudioItems.clearParameters();
                stmtGetAudioItems.setInt(1, id);
                ResultSet result = stmtGetAudioItems.executeQuery();
                if (result.next()) {

                }
            } catch(SQLException sqle) {
                sqle.printStackTrace();
            }
            
            return item;
        }        

    }
     
    /** For testing purposes **/
    public static void main(String[] args) throws Exception {
        PersistenceManager.initialize();
        
        AudioItem item = new AudioItem("uuid");
        
        PersistenceManager.save(item);
        
        Collection<AudioItem> items = PersistenceManager.collect(AudioItem.class);
        
        for (AudioItem audioitem : items) {
            sLogger.info("AudioItem ID: " + audioitem.getId() 
                         + "\t" + "UUID:" + audioitem.getUUId());
        }
        
        PersistenceManager.uninitialize();
    }
  
}
