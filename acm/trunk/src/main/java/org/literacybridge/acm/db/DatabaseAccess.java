package org.literacybridge.acm.db;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.Properties;

/**
 * DatabaseAccess
 * 
 * Handles all JDBC-related actions such as establishing a connection, 
 * creating tables, etc.
 */
class DatabaseAccess {
        
    /** Connection objects **/    
    private Connection dbConnection;
    private Properties dbProperties;
    private boolean isConnected;
    private String dbName;        
        
    /** Load driver, create new database if necessary **/
    public DatabaseAccess() {        
        setDBSystemDir();
        dbProperties = loadDBProperties();
        this.dbName = dbProperties.getProperty("db.name");
        String driverName = dbProperties.getProperty("derby.driver"); 
        loadDatabaseDriver(driverName);
        if(!dbExists()) {
            createDatabase();
        }
        
    }
    
    /** Returns true if database already exists **/
    private boolean dbExists() {
        boolean bExists = false;
        String dbLocation = getDatabaseLocation();
        File dbFileDir = new File(dbLocation);
        if (dbFileDir.exists()) {
            bExists = true;
        }
        return bExists;
    }
    
    /** Set the home directory for the database **/
    private void setDBSystemDir() {
        // decide on the db system directory
        String userHomeDir = System.getProperty("user.home", ".");
        String systemDir = userHomeDir + "/.literacybridge";
        System.setProperty("derby.system.home", systemDir);
        
        // create the db system directory
        File fileSystemDir = new File(systemDir);
        fileSystemDir.mkdir();
    }
    
    /** Load JDBC driver **/
    private void loadDatabaseDriver(String driverName) {
        // load Derby driver
        try {
            Class.forName(driverName);
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }
        
    }
    
    /** Load configuration data from an external file **/
    private Properties loadDBProperties() {
        InputStream dbPropInputStream = null;
        dbPropInputStream = DatabaseAccess.class.getResourceAsStream("Configuration.properties");
        dbProperties = new Properties();
        try {
            dbProperties.load(dbPropInputStream);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return dbProperties;
    }
    
    /** Create necessary tables for the ACM **/
    private boolean createTables(Connection dbConnection) {
        boolean bCreatedTables = false;
        Statement statement = null;
        try {
            statement = dbConnection.createStatement();
            
            StringBuffer sqlScript = new StringBuffer();
            try {
                String scriptName = dbProperties.getProperty("db.script");
                File scriptLocation = new File("resources" + File.separator + scriptName);
                
                BufferedReader sqlReader = new BufferedReader(new FileReader(scriptLocation)); 
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
    
    /** Create database if necessary **/
    private boolean createDatabase() {
        boolean bCreated = false;
        Connection dbConnection = null;
        
        String dbUrl = getDatabaseUrl();
        dbProperties.put("create", "true");
        
        try {
            dbConnection = DriverManager.getConnection(dbUrl, dbProperties);
            bCreated = createTables(dbConnection);
        } catch (SQLException ex) {
        }
        dbProperties.remove("create");
        return bCreated;
    }
    
    /** Establish a connection to the database **/
    public boolean connect() {
        String dbUrl = getDatabaseUrl();
        try {
            dbConnection = DriverManager.getConnection(dbUrl, dbProperties);            
            isConnected = dbConnection != null;
        } catch (SQLException ex) {
            isConnected = false;
        }
        return isConnected;
    }

    /** Send a prepared SQL statement to the database **/
    PreparedStatement prepareStatement(String stmt, int returnKeys) throws Exception {
        if (dbConnection == null) {
            throw new Exception("A connection to the database doesn't exist!");
        }
        return dbConnection.prepareStatement(stmt, returnKeys);
    }
    
    /** Returns the home directory of the current user. **/ 
    private String getHomeDir() {
        return System.getProperty("user.home");
    }
    
    /** Close the existing connection to the database **/
    public void disconnect() {
        if(isConnected) {
            String dbUrl = getDatabaseUrl();
            dbProperties.put("shutdown", "true");
            try {
                DriverManager.getConnection(dbUrl, dbProperties);
            } catch (SQLException ex) {
            }
            isConnected = false;
        }
    }
    
    /** Returns the current database directory **/
    public String getDatabaseLocation() {
        String dbLocation = System.getProperty("derby.system.home") + "/" + dbName;
        return dbLocation;
    }
    
    /** Returns database url for JDBC connection string **/ 
    public String getDatabaseUrl() {
        String dbUrl = dbProperties.getProperty("derby.url") + dbName;
        return dbUrl;
    }
        
}
