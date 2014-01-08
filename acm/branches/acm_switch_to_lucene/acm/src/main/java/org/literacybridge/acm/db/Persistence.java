package org.literacybridge.acm.db;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;

import org.apache.commons.lang.StringUtils;
import org.eclipse.persistence.jpa.JpaHelper;
import org.literacybridge.acm.categories.Taxonomy;
import org.literacybridge.acm.categories.Taxonomy.Category;
import org.literacybridge.acm.config.DBConfiguration;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.metadata.MetadataSpecification;
import org.literacybridge.acm.metadata.MetadataValue;
import org.literacybridge.acm.repository.A18DurationUtil;

import com.google.common.collect.Lists;

public class Persistence {
	public static final class DatabaseConnection {
		private final Properties sConnectionProperties;
		private EntityManagerFactory sEmf;
		
		public DatabaseConnection(Properties sConnectionProperties,
				EntityManagerFactory sEmf) {
			this.sConnectionProperties = sConnectionProperties;
			this.sEmf = sEmf;
		}

		private Properties getConnectionProperties() {
			return sConnectionProperties;
		}
		
		private EntityManagerFactory getEntityManagerFactory() {
			return sEmf;
		}
		
		public EntityManager getEntityManager() {
			return sEmf.createEntityManager();
		}
		
		public void close() {
	        if (sEmf != null) {
	            sEmf.close();
	            sEmf = null;
	        }  
		}
	}
	
    
    private static Logger LOG = Logger.getLogger(Persistence.class.getName());
    
    private static String sPersistenceUnit = "lbPersistenceUnit";
    
    private static String DBNAME = "literacybridge";
    
    private static final String DRIVER = "eclipselink.jdbc.driver";
    private static final String LOG_LEVEL = "eclipselink.logging.level";
    private static final String TARGET_DB = "eclipselink.target-database";
    private static final String WRITE_CONNECTIONS_MAX = "eclipselink.jdbc.write-connections.max";
    private static final String WRITE_CONNECTIONS_MIN = "eclipselink.jdbc.write-connections.min";
    private static final String READ_CONNECTIONS_MAX = "eclipselink.jdbc.read-connections.max";
    private static final String READ_CONNECTIONS_MIN = "eclipselink.jdbc.read-connections.min";
    private static final String CACHE_TYPE_DEFAULT = "eclipselink.cache.type.default";
    private static final String CACHE_TYPE_SHARED_DEFAULT = "eclipselink.cache.shared.default";
    private static final String URL = "eclipselink.jdbc.url";
    private static final String PROTOCOL="db.protocol";
    private static final String PASSWORD = "eclipselink.jdbc.password";
    
    private static final String DEFAULT_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
    private static final String DEFAULT_PROTOCOL = "";
    private static final String DEFAULT_LOG_LEVEL = "OFF";
    private static final String DEFAULT_TARGET_DB = "Derby";
    private static final String DEFAULT_WRITE_CONNECTIONS_MAX = "1";
    private static final String DEFAULT_WRITE_CONNECTIONS_MIN = "1";
    private static final String DEFAULT_READ_CONNECTIONS_MAX = "1";
    private static final String DEFAULT_READ_CONNECTIONS_MIN = "1";
    private static final String DEFAULT_CACHE_TYPE_DEFAULT = "SoftWeak";
    private static final String DEFAULT_CACHE_TYPE_SHARED_DEFAULT = "false";
    private static final String DEFAULT_URL = "jdbc:derby:";
    private static final String DEFAULT_PASSWORD = "";
    
    private static final String DB_CONNECTION_PROPS_FILE = "configuration.properties";

    
    private Persistence() {}
    

    
    
    public static DatabaseConnection initialize(DBConfiguration config) throws Exception {
        setDBSystemDir(config.getDatabaseDirectory());

    	// load configuration before calling database
    	Properties props = LoadAndInitializeSettings();        	

        if(!dbExists()) {
            createDatabase(props);
        }        
        EntityManagerFactory sEmf = createEntityManagerFactory(props);
        return new DatabaseConnection(props, sEmf);
    }    
    
    public static synchronized void maybeRunMigration() throws Exception {
    	// set playlist ordering if not set before
    	List<PersistentTag> allTags = PersistentTag.getFromDatabase();
    	for (PersistentTag tag : allTags) {
    		List<PersistentAudioItem> audioItems = tag.getPersistentAudioItemList();
    		int pos = 1;
    		for (PersistentAudioItem audioItem : audioItems) {
    			PersistentTagOrdering ordering = PersistentTagOrdering.getFromDatabase(audioItem, tag);
    			if (ordering.getPosition() == null) {
    				// this simple approach works, since all entries in the corresponding table will either be null or set 
    				ordering.setPosition(pos++);
    				ordering.commit();
    			}
    			
    		}
    	}
    	
    	for (AudioItem audioItem : AudioItem.getFromDatabase()) {
        	// =================================================================
    		// 1) calculate duration of audio items
    		List<MetadataValue<String>> values = audioItem.getMetadata().getMetadataValues(
					MetadataSpecification.LB_DURATION);
			if (values == null || StringUtils.isEmpty(values.get(0).getValue())) {
				A18DurationUtil.updateDuration(audioItem);
			}
			
	    	// =================================================================
	    	// 2) make sure categories are stored correctly, i.e. an audioitem's category list should contain
	    	// all parents of the leaf categories
			if (!audioItem.hasCategory(Taxonomy.getTaxonomy().getRootCategory())) {
				List<Category> categories = Lists.newLinkedList(audioItem.getCategoryLeavesList());
				if (categories.isEmpty()) {
					LOG.log(Level.WARNING, "Audioitem " + audioItem.getUuid() + " does not contain any leaf categories. Assigning new general leaf category.");
					categories = Lists.newLinkedList(audioItem.getCategoryList());
				}
				audioItem.removeAllCategories();
				for (Category category : categories) {
					audioItem.addCategory(category);
				}
				audioItem.commit();
			}
    	}
	}
    
    public static Properties LoadAndInitializeSettings() throws Exception {
    	Properties sConnectionProperties = new Properties();
		
		try {      	
			sConnectionProperties.setProperty(DRIVER, DEFAULT_DRIVER);            

            sConnectionProperties.setProperty(LOG_LEVEL, DEFAULT_LOG_LEVEL);
            sConnectionProperties.setProperty(TARGET_DB, DEFAULT_TARGET_DB);
            sConnectionProperties.setProperty(WRITE_CONNECTIONS_MAX, DEFAULT_WRITE_CONNECTIONS_MAX);
            sConnectionProperties.setProperty(WRITE_CONNECTIONS_MIN, DEFAULT_WRITE_CONNECTIONS_MIN);
            sConnectionProperties.setProperty(READ_CONNECTIONS_MAX, DEFAULT_READ_CONNECTIONS_MAX);
            sConnectionProperties.setProperty(READ_CONNECTIONS_MIN, DEFAULT_READ_CONNECTIONS_MIN);
            sConnectionProperties.setProperty(CACHE_TYPE_DEFAULT, DEFAULT_CACHE_TYPE_DEFAULT);
            sConnectionProperties.setProperty(CACHE_TYPE_SHARED_DEFAULT, DEFAULT_CACHE_TYPE_SHARED_DEFAULT);  
            sConnectionProperties.setProperty(PROTOCOL, DEFAULT_PROTOCOL);
            sConnectionProperties.setProperty(PASSWORD, DEFAULT_PASSWORD);
            
            // Load settings from file. Overrides existing once!
            sConnectionProperties.load(Persistence.class.getResourceAsStream("/" + DB_CONNECTION_PROPS_FILE));

            // Create the connection url
        	String url = DEFAULT_URL 
        				+ sConnectionProperties.getProperty(PROTOCOL) 
        				+ getDBSystemDir() 
        				+ File.separator 
        				+ DBNAME;
            sConnectionProperties.setProperty(URL, url);
     
            LOG.log(Level.INFO, "Connect to database: " + sConnectionProperties.getProperty(URL));      
	 	} catch (Exception exception) {
	 		LOG.fine("Error reading database connection parameter file (" + DB_CONNECTION_PROPS_FILE + ")");
            // There is no purpose in trying to continue as the basic connection
            // parameter are missing or incomplete so rethrow the exception
            throw exception;
        }
		
		return sConnectionProperties;
    }
    
    private static EntityManagerFactory createEntityManagerFactory(Properties sConnectionProperties) throws Exception {        
        EntityManagerFactory emf = 
            javax.persistence.Persistence.createEntityManagerFactory(sPersistenceUnit, sConnectionProperties);
        try {
            JpaHelper.getEntityManagerFactory(emf).getServerSession(); // database connection test
        } catch (javax.persistence.PersistenceException pex) {
            throw new PersistenceException(pex);
        }
        return emf;
    }
    
    private static void setDBSystemDir(File dbRootDir) {
        // decide on the db system directory
        System.setProperty("derby.system.home", dbRootDir.getAbsolutePath());
        
        // create the db system directory
        //Constants.DATABASE_DIR.mkdir();
        dbRootDir.mkdir();
    }    
    
    private static String getDBSystemDir() {
        return System.getProperty("derby.system.home");
    }
    
    private static boolean dbExists() {
        boolean bExists = false;
        String dbLocation = getDBSystemDir() + File.separator + DBNAME;
        File dbFileDir = new File(dbLocation);
        if (dbFileDir.exists()) {
            bExists = true;
        }
        return bExists;
    }    
    
    /** Create database if necessary **/
    private static boolean createDatabase(Properties sConnectionProperties) throws Exception {
    	LoadAndInitializeSettings(); // JTBD
    	
        boolean bCreated = false;
        Connection dbConnection = null;
        
        String dbUrl = "jdbc:derby:" + getDBSystemDir() + File.separator + DBNAME;
        dbUrl+=";create=true";
        
        try {
        	String driverName = sConnectionProperties.getProperty(DRIVER);
            Class.forName(driverName);
            dbConnection = DriverManager.getConnection(dbUrl);
            bCreated = createTables(dbConnection);
        } catch (Exception ex) {
        	LOG.log(Level.WARNING, "Creating database failed.", ex);
        }
        return bCreated;
    }    
    
    private static boolean createTables(Connection dbConnection) {
        boolean bCreatedTables = false;
        Statement statement = null;
        try {
            statement = dbConnection.createStatement();
            
            StringBuffer sqlScript = new StringBuffer();
            try {
                String scriptName = "db_schema.sql";
                
                BufferedReader sqlReader = new BufferedReader(new InputStreamReader(Persistence.class.getResourceAsStream("/" + scriptName))); 
                String line = null;
                while ((line = sqlReader.readLine()) != null) {
                    sqlScript.append(line.replace(';', ' ') + "\n");
                    if (line.contains(";")) {
                        statement.addBatch(sqlScript.toString());
                        sqlScript.setLength(0);
                    }
                }
                sqlReader.close();
            } catch(IOException ex) {
                    ex.printStackTrace();
            }   
            statement.executeBatch();
            bCreatedTables = true;
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        
        return bCreatedTables;
    }        
}
