/**
 * Copyright (C) 2009 - 2010 by OpenGamma Inc.
 *
 * Please see distribution for license.
 */
package com.opengamma.financial.timeseries.config;

import java.util.Collection;

import com.google.common.collect.ImmutableSet;

/**
 * Constants for time-series meta-data field names.
 */
public final class TimeSeriesMetaDataFieldNames {

  /**
   * Field name for data source.
   */
  public static final String DATA_SOURCE_NAME = "dataSource";
  /**
   * Field name for data provider.
   */
  public static final String DATA_PROVIDER_NAME = "dataProvider";
  /**
   * Field name for security type.
   */
  public static final String SECURITY_TYPE_NAME = "securityType";
  /**
   * Field value for star(*).
   */
  public static final String STAR_VALUE = "*";
  /**
   * Valid field names
   */
  public static final Collection<String> VALID_FIELD_NAMES = ImmutableSet.of(DATA_SOURCE_NAME, DATA_PROVIDER_NAME, SECURITY_TYPE_NAME);
  /**
   * Default name for the default TSS configuration
   */
  public static final String DEFAULT_CONFIG_NAME = "DEFAULT_TSS_CONFIG";

  /**
   * Restricted constructor.
   */
  private TimeSeriesMetaDataFieldNames() {
  }

}
