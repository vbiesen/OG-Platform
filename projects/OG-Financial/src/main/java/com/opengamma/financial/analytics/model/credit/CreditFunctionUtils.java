/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.analytics.model.credit;

import org.threeten.bp.LocalDate;
import org.threeten.bp.Period;
import org.threeten.bp.ZonedDateTime;

import com.opengamma.util.time.Tenor;

/**
 * 
 */
public class CreditFunctionUtils {

  @SuppressWarnings("rawtypes")
  public static Tenor[] getTenors(final Comparable[] xs) {
    final Tenor[] tenors = new Tenor[xs.length];
    for (int i = 0; i < xs.length; i++) {
      if (xs[i] instanceof Tenor) {
        tenors[i] = (Tenor) xs[i];
      } else {
        tenors[i] = Tenor.of(Period.parse((String) xs[i]));
      }
    }
    return tenors;
  }

  public static Double[] getSpreads(final Object[] ys) {
    final Double[] spreads = new Double[ys.length];
    for (int i = 0; i < ys.length; i++) {
      spreads[i] = (Double) ys[i];
    }
    return spreads;
  }

  public static String[] getFormattedBucketedXAxis(final LocalDate[] dates, final ZonedDateTime valuationDateTime) {
    final LocalDate valuationDate = IMMDateGenerator.getPreviousIMMDate(valuationDateTime.toLocalDate());
    final int n = dates.length;
    final String[] result = new String[n];
    for (int i = 0; i < n; i++) {
      final Period periodBetween = Period.between(valuationDate, dates[i]);
      result[i] = periodBetween.toString();
    }
    return result;
  }
}
