/**
 * Copyright (C) 2009 - 2010 by OpenGamma Inc.
 *
 * Please see distribution for license.
 */
package com.opengamma.util.test;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;

import com.opengamma.OpenGammaRuntimeException;

/**
 * Command-line interface to create or clear databases.
 *
 * @author pietari
 */
public class DBTool extends Task {
  
  public interface TableCreationCallback {
    
    public void tablesCreatedOrUpgraded (final String version);
    
  }
  
  // What to do - should be set once
  private String _catalog;
  private String _schema;
  private boolean _create = false;
  private boolean _drop = false;
  private boolean _clear = false;
  private boolean _createTestDb = false;
  private boolean _createTables = false;
  private String _testDbType;
  private String _basedir;
  private String _targetVersion;
  private String _createVersion;
  
  // What to do it on - can change
  private DBDialect _dialect;
  private String _jdbcUrl;
  private String _dbServerHost;
  private String _user;
  private String _password;
  
  
  public DBTool() {
  }
  
  public DBTool(String dbServerHost,
      String user,
      String password) {
    setDbServerHost(dbServerHost);
    setUser(user);
    setPassword(password);
  }
  
  public void initialise() {
    if (_dbServerHost == null || _user == null || _password == null) {
      throw new OpenGammaRuntimeException("Server/user/password not initialised");
    }
    
    Map<String, DBDialect> url2Dialect = new HashMap<String, DBDialect>(); // add new supported DB types to this Map
    url2Dialect.put("jdbc:postgresql", PostgresDialect.getInstance());
    url2Dialect.put("jdbc:derby", DerbyDialect.getInstance());  
    
    String dbUrlLowercase = _dbServerHost.toLowerCase();
    for (Map.Entry<String, DBDialect> entry : url2Dialect.entrySet()) {
      if (dbUrlLowercase.indexOf(entry.getKey()) != -1) {
        _dialect = entry.getValue();        
        break;
      }
    }
    
    if (_dialect == null) {
      throw new OpenGammaRuntimeException("Database " + _dbServerHost + " not supported. The database URL must contain one of: " + url2Dialect.entrySet());
    }

    _dialect.initialise(_dbServerHost, _user, _password);
  }
  
  public void shutdown() {
    _dialect.shutdown();
  }
  
  public void setDbServerHost(String dbServerHost) {
    _dbServerHost = dbServerHost;
  }

  public void setUser(String user) {
    _user = user;
  }

  public void setPassword(String password) {
    _password = password;
  }

  public String getDbServerHost() {
    return _dbServerHost;
  }

  public String getUser() {
    return _user;
  }

  public String getPassword() {
    return _password;
  }
  
  public String getJdbcUrl() {
    return _jdbcUrl;
  }

  public void setJdbcUrl(String jdbcUrl) {
    _jdbcUrl = jdbcUrl;
  }
  
  public String getCatalog() {
    return _catalog;
  }

  public void setCatalog(String catalog) {
    _catalog = catalog;
  }

  public String getSchema() {
    return _schema;
  }

  public void setSchema(String schema) {
    _schema = schema;
  }

  public void setCreate(boolean create) {
    _create = create;
  }
  
  public void setCreate(String create) {
    setCreate(create.equalsIgnoreCase("true"));
  }

  public void setDrop(boolean drop) {
    _drop = drop;
  }
  
  public void setDrop(String drop) {
    setDrop(drop.equalsIgnoreCase("true"));
  }

  public void setClear(boolean clear) {
    _clear = clear;
  }
  
  public void setClear(String clear) {
    setClear(clear.equalsIgnoreCase("true"));
  }

  public void setCreateTestDb(String testDbType) {
    _createTestDb = (testDbType != null);
    _testDbType = testDbType;
  }
  
  public String getBasedir() {
    return _basedir;
  }

  public void setBasedir(String basedir) {
    _basedir = basedir;
    TestProperties.setBaseDir(_basedir);
  }
  
  public void setCreateVersion (final String createVersion) {
    _createVersion = createVersion;
  }
  
  public String getCreateVersion () {
    return _createVersion;
  }
  
  public void setTargetVersion (final String targetVersion) {
    _targetVersion = targetVersion;
  }
  
  public String getTargetVersion () {
    return _targetVersion;
  }
  
  public void setCreateTables(boolean create) {
    _createTables = create;
  }
  
  public void setCreateTables(String create) {
    setCreateTables(create.equalsIgnoreCase("true"));
  }
  
  public void createTestSchema() {
    createSchema(getTestCatalog(), getTestSchema());
  }
  
  public void dropTestSchema() {
    dropSchema(getTestCatalog(), getTestSchema());
  }
  
  public void clearTestTables() {
    clearTables(getTestCatalog(), getTestSchema());
  }

  public void createSchema(String catalog, String schema) {
    _dialect.createSchema(catalog, schema);
  }
  
  public void dropSchema(String catalog, String schema) {
    _dialect.dropSchema(catalog, schema);
  }
  
  public void clearTables(String catalog, String schema) {
    _dialect.clearTables(catalog, schema);    
  }
  
  
  
  public static String getTestCatalogStatic() {
    return "test_" + System.getProperty("user.name");
  }
  
  public String getTestCatalog() {
    return getTestCatalogStatic();    
  }
  
  public String getTestSchema() {
    return null; // use default    
  }
  
  public String getTestDatabaseURL() {
    return _dbServerHost + "/" + getTestCatalog();         
  }
  
  public Dialect getHibernateDialect() {
    return _dialect.getHibernateDialect();
  }
  
  public Class<?> getJDBCDriverClass() {
    return _dialect.getJDBCDriverClass();
  }
  
  public Configuration getHibernateConfiguration(String jdbcUrl) {
    Configuration configuration = new Configuration();
    configuration.setProperty(Environment.DRIVER, getJDBCDriverClass().getName());
    configuration.setProperty(Environment.URL, jdbcUrl);
    configuration.setProperty(Environment.USER, getUser());
    configuration.setProperty(Environment.PASS, getPassword());
    configuration.setProperty(Environment.DIALECT, getHibernateDialect().getClass().getName());
    configuration.setProperty(Environment.SHOW_SQL, "true");
    return configuration;
  }
  
  
  
  public void createTestTables(final TableCreationCallback callback) {
    createTables(getTestCatalog(), callback);
  }
  
  private void executeScript (String catalog, File file) {
    String sql;
    try {
      sql = FileUtils.readFileToString(file);
    } catch (IOException e) {
      throw new OpenGammaRuntimeException("Cannot read file " + file.getAbsolutePath(), e);      
    }
    executeSql(catalog, sql);
  }
  
  public void createTables(String catalog, final TableCreationCallback callback) {
    final File file = new File(_basedir, "db/" + _dialect.getDatabaseName());
    final String[] scriptDirs = file.list (new FilenameFilter () {
      @Override
      public boolean accept(File dir, String name) {
        return name.startsWith ("scripts_");
      }
    });
    Arrays.sort (scriptDirs);
    if (getTargetVersion () == null) {
      setTargetVersion (scriptDirs[scriptDirs.length - 1].substring (8));
    }
    if (getCreateVersion () == null) {
      setCreateVersion (getTargetVersion ());
    }
    boolean created = false;
    for (int i = 0; i < scriptDirs.length; i++) {
      final String version = scriptDirs[i].substring (8);
      if (getCreateVersion ().compareTo (version) <= 0) {
        final File scriptDir = new File (file, scriptDirs[i]);
        if (!created) {
          System.out.println ("\tCreating DB version " + version);
          executeScript (catalog, new File (scriptDir, "create-db.sql"));
          created = true;
        } else {
          if (getTargetVersion ().compareTo (version) > 0) break;
          System.out.println ("\tUpgrading to DB version " + version);
          executeScript (catalog, new File (scriptDir, "upgrade-db.sql"));
        }
        if (callback != null) {
          callback.tablesCreatedOrUpgraded (version);
        }
        if (getTargetVersion ().compareTo (version) == 0) break;
      }
    }
  }
  
  public String[] getDatabaseVersions () {
    final File file = new File(_basedir, "db/" + _dialect.getDatabaseName());
    final String[] scriptDirs = file.list (new FilenameFilter () {
      @Override
      public boolean accept(File dir, String name) {
        return name.startsWith ("scripts_");
      }
    });
    Arrays.sort (scriptDirs);
    final String[] versions = new String[scriptDirs.length];
    for (int i = 0; i < scriptDirs.length; i++) {
      versions[i] = scriptDirs[i].substring (8);
    }
    return versions;
  }
  
  public void executeSql(String catalog, String sql) {
    _dialect.executeSql(catalog, sql);    
  }
  
  @Override
  public void execute() throws BuildException {
    if (!_createTestDb) {
      
      if (_dbServerHost == null) {
        
        // Parse the server host and catalog from a JDBC URL
        if (_jdbcUrl != null) {
      
          int lastSlash = _jdbcUrl.lastIndexOf('/');
          if (lastSlash == -1 || lastSlash == _jdbcUrl.length() - 1) {
            throw new BuildException("JDBC URL must contain a slash separating the server host and the database name");
          }
          
          _dbServerHost = _jdbcUrl.substring(0, lastSlash);
          _catalog = _jdbcUrl.substring(lastSlash + 1);
          
        } else {
          throw new BuildException("No DB server specified.");
        }
      }
      
      if (_catalog == null) {
        throw new BuildException("No database on the DB server specified.");
      }
    }
    
    if (!_create && !_drop && !_clear && !_createTestDb && !_createTables) {
      throw new BuildException("Nothing to do.");
    }
    
    if (_clear) {
      System.out.println("Clearing tables...");
      initialise();
      clearTables(_catalog, _schema);
    }
    
    if (_drop) {
      System.out.println("Dropping schema...");
      initialise();
      dropSchema(_catalog, _schema);
    }

    if (_create) {
      System.out.println("Creating schema...");
      initialise();
      createSchema(_catalog, _schema);      
    }
    
    if (_createTables) {
      System.out.println("Creating tables...");
      initialise();
      createTables(_catalog, null);
    }
    
    if (_createTestDb) {
      for (String dbType : TestProperties.getDatabaseTypes(_testDbType)) {
        System.out.println("Creating " + dbType + " test database...");
        
        String dbUrl = TestProperties.getDbHost(dbType);
        String user = TestProperties.getDbUsername(dbType);
        String password = TestProperties.getDbPassword(dbType);
        
        setDbServerHost(dbUrl);
        setUser(user);
        setPassword(password);
        
        initialise();
        dropTestSchema(); // make sure it's empty if it already existed
        createTestSchema();
        createTestTables(null);
        shutdown();
      }
    }
    
    System.out.println("All tasks succeeded.");
  }
  
  

  public static void usage(Options options) {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("java com.opengamma.util.test.DBTool [args]", options);
  }
  
  public static void main(String[] args) {
    
    Options options = new Options();
    options.addOption("jdbcUrl", "jdbcUrl", true, "DB server URL + database - for example, jdbc:postgresql://localhost:1234/OpenGammaTests. You can use" +
    		" either this option or specify server and database separately.");
    options.addOption("server", "server", true, "DB server URL (no database at the end) - for example, jdbc:postgresql://localhost:1234");
    options.addOption("database", "database", true, "Name of database on the DB server - for example, OpenGammaTests");
    options.addOption("user", "user", true, "User name to the DB");
    options.addOption("password", "password", true, "Password to the DB");
    options.addOption("schema", "schema", true, "Name of schema within database. Optional. If not specified, the default schema for the database is used.");
    options.addOption("create", "create", false, "Creates the given database/schema. The database will be empty.");
    options.addOption("drop", "drop", false, "Drops all tables and sequences within the given database/schema");
    options.addOption("clear", "clear", false, "Clears all tables within the given database/schema");
    options.addOption("createtestdb", "createtestdb", true, "Drops schema in database test_<user.name> and recreates it (including tables). " +
        "{dbtype} should be one of derby, postgres, all. Connection parameters are read from test.properties so you do not need " +
        "to specify server, user, or password.");
    options.addOption("createtables", "createtables", true, "Runs {basedir}/db/{dbtype}/scripts_<latest version>/create-db.sql.");
    options.addOption("targetversion", "targetversion", true, "Version number for the end result database. Optional. If not specified, assumes latest version.");
    options.addOption("createversion", "createversion", true, "Version number to run the creation script from. Optional. If not specified, defaults to {targetversion}.");
    options.addOption("basedir", "basedir", true, "Base directory for reading create db scripts and property files. Optional. If not specified, the working directory is used.");
    
    CommandLineParser parser = new PosixParser();
    CommandLine line = null;
    try {
      line = parser.parse(options, args);
    } catch (ParseException e) {
      e.printStackTrace();
      usage(options);
      System.exit(-1);
    }
    
    DBTool tool = new DBTool();
    tool.setJdbcUrl(line.getOptionValue("jdbcUrl"));
    tool.setDbServerHost(line.getOptionValue("server"));
    tool.setUser(line.getOptionValue("user"));
    tool.setPassword(line.getOptionValue("password"));
    tool.setCatalog(line.getOptionValue("database"));
    tool.setSchema(line.getOptionValue("schema"));
    tool.setCreate(line.hasOption("create"));
    tool.setDrop(line.hasOption("drop"));
    tool.setClear(line.hasOption("clear"));
    tool.setCreateTestDb(line.getOptionValue("createtestdb"));
    tool.setCreateTables(line.getOptionValue("createtables"));
    tool.setBasedir(line.getOptionValue("basedir"));
    tool.setTargetVersion(line.getOptionValue("targetversion"));
    tool.setCreateVersion(line.getOptionValue("createversion"));
    
    try {
      tool.execute();
    } catch (BuildException e) {
      System.out.println(e.getMessage());
      usage(options);
      System.exit(-1);
    }

    System.exit(0);
  }

}
