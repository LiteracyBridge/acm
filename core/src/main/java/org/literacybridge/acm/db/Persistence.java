package org.literacybridge.acm.db;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;

import org.eclipse.persistence.jpa.JpaHelper;
import org.literacybridge.acm.Constants;

public class Persistence {
    
    private static Logger sLogger = Logger.getLogger(Persistence.class.getName());
    
    private static EntityManagerFactory sEmf;
    private static Properties sConnectionProperties = new Properties();
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
    
    public static synchronized void initialize() throws Exception {
        setDBSystemDir();
        if(!dbExists()) {
            createDatabase();
        }        
        sEmf = createEntityManagerFactory();
    }    
    
    public static synchronized void uninitialize() {
        closeEntityManagerFactory();
    }    
    
    private static synchronized EntityManagerFactory getEntityManagerFactory() {
        return sEmf;
    }    
    
    public static synchronized EntityManager getEntityManager() {            
        return getEntityManagerFactory().createEntityManager();
    }    
    
    private static EntityManagerFactory createEntityManagerFactory() throws Exception {        
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
     
        	sLogger.log(Level.INFO, "Connect to database: " + url);      
        } catch (Exception exception) {
            sLogger.fine("Error reading database connection parameter file (" + DB_CONNECTION_PROPS_FILE + ")");
            // There is no purpose in trying to continue as the basic connection
            // parameter are missing or incomplete so rethrow the exception
            throw exception;
        }
        
        EntityManagerFactory emf = 
            javax.persistence.Persistence.createEntityManagerFactory(sPersistenceUnit, sConnectionProperties);
        try {
            JpaHelper.getEntityManagerFactory(emf).getServerSession(); // database connection test
        } catch (javax.persistence.PersistenceException pex) {
            throw new PersistenceException(pex);
        }
        return emf;
    }

    public static synchronized void closeEntityManagerFactory() {
        if (sEmf != null) {
            sEmf.close();
            sEmf = null;
        }
    }  
    
    private static void setDBSystemDir() {
        // decide on the db system directory
        System.setProperty("derby.system.home", Constants.DATABASE_DIR.getAbsolutePath());
        
        // create the db system directory
        Constants.DATABASE_DIR.mkdir();
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
    private static boolean createDatabase() {
        boolean bCreated = false;
        Connection dbConnection = null;
        
        String dbUrl = "jdbc:derby:" + getDBSystemDir() + File.separator + DBNAME;
        dbUrl+=";create=true";
        
        try {
            Class.forName(sConnectionProperties.getProperty(DRIVER));
            dbConnection = DriverManager.getConnection(dbUrl);
            bCreated = createTables(dbConnection);
        } catch (Exception ex) {
            sLogger.log(Level.WARNING, "Creating database failed.", ex);
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
                        sqlScript = new StringBuffer();
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
