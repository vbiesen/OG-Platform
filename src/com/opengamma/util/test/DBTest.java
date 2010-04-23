/**
 * Copyright (C) 2009 - 2010 by OpenGamma Inc.
 *
 * Please see distribution for license.
 */
package com.opengamma.util.test;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.test.DBTool.TableCreationCallback;

/**
 * 
 *
 * @author pietari
 */
@RunWith(Parameterized.class)
abstract public class DBTest implements TableCreationCallback {
  
  private final String _databaseType;
  private final DBTool _dbtool;
  
  protected DBTest(String databaseType, String version) {
    ArgumentChecker.checkNotNull(databaseType, "Database type");
    _databaseType = databaseType;
    _dbtool = TestProperties.getDbTool(databaseType);
    _dbtool.setCreateVersion (version);
    _dbtool.dropTestSchema();
    _dbtool.createTestSchema();
    _dbtool.createTestTables(this);
  }
  
  @Parameters
  public static Collection<Object[]> getDatabaseTypes() {
    String databaseType = System.getProperty("test.database.type");
    if (databaseType == null) {
      databaseType = "derby"; // If you run from Eclipse, use Derby only
    }
    ArrayList<Object[]> returnValue = new ArrayList<Object[]>();
    for (String db : TestProperties.getDatabaseTypes(databaseType)) {
      final DBTool dbTool = TestProperties.getDbTool (db);
      final String[] versions = dbTool.getDatabaseVersions ();
      for (int i = versions.length - 1; i >= 0; i--) {
        returnValue.add(new Object[] { db, versions[i] });
      }
    }
    return returnValue;
  }
  
  @Before
  public void setUp() throws Exception {
    _dbtool.clearTestTables();
  }
  
  @After
  public void tearDown() throws Exception {
    _dbtool.shutdown(); // avoids locking issues with Derby
  }

  public DBTool getDbTool() {
    return _dbtool;
  }
  
  public String getDatabaseType() {
    return _databaseType;
  }
  
  /**
   * Override this if you wish to do something with the database while it is in its "upgrading" state - e.g. populate with test data
   * at a particular version to test the data transformations on the next version upgrades.
   */
  public void tablesCreatedOrUpgraded (final String version) {
    // No action 
  }

}
