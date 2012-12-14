/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.core.holiday.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertSame;
import net.sf.ehcache.CacheManager;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.opengamma.core.holiday.Holiday;
import com.opengamma.core.holiday.HolidaySource;
import com.opengamma.id.ObjectId;
import com.opengamma.id.UniqueId;
import com.opengamma.id.VersionCorrection;
import com.opengamma.util.ehcache.EHCacheUtils;

/**
 * Test {@link EHCachingHolidaySource}.
 */
@Test
public class EHCachingHolidaySourceTest {

  private static final UniqueId UID = UniqueId.of("A", "B");
  private static final ObjectId OID = ObjectId.of("A", "B");
  private static final VersionCorrection VC = VersionCorrection.LATEST;

  private HolidaySource _underlyingSource;
  private EHCachingHolidaySource _cachingSource;
  private CacheManager _cacheManager;

  @BeforeMethod
  public void setUp() {
    _cacheManager = new CacheManager();
    _underlyingSource = mock(HolidaySource.class);
    _cachingSource = new EHCachingHolidaySource(_underlyingSource, _cacheManager);
  }

  @AfterMethod
  public void tearDown() {
    _cacheManager = EHCacheUtils.shutdownQuiet(_cacheManager);
  }

  //-------------------------------------------------------------------------
  public void getHoliday_uniqueId() {
    final Holiday h = new SimpleHoliday();
    when(_underlyingSource.get(UID)).thenReturn(h);
    assertSame(_cachingSource.get(UID), h);
    assertSame(_cachingSource.get(UID), h);
    verify(_underlyingSource, times(1)).get(UID);
  }

  public void getHoliday_objectId() {
    final Holiday h = new SimpleHoliday();
    when(_underlyingSource.get(OID, VC)).thenReturn(h);
    assertSame(_cachingSource.get(OID, VC), h);
    assertSame(_cachingSource.get(OID, VC), h);
    verify(_underlyingSource, times(1)).get(OID, VC);
  }

}
