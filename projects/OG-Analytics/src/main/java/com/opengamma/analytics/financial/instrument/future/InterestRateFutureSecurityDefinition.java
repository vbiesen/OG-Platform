/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.analytics.financial.instrument.future;

import org.apache.commons.lang.ObjectUtils;
import org.threeten.bp.LocalDate;
import org.threeten.bp.ZonedDateTime;

import com.opengamma.analytics.financial.ExpiredException;
import com.opengamma.analytics.financial.instrument.InstrumentDefinitionVisitor;
import com.opengamma.analytics.financial.instrument.InstrumentDefinitionWithData;
import com.opengamma.analytics.financial.instrument.index.IborIndex;
import com.opengamma.analytics.financial.interestrate.future.derivative.InterestRateFutureSecurity;
import com.opengamma.analytics.financial.schedule.ScheduleCalculator;
import com.opengamma.analytics.util.time.TimeCalculator;
import com.opengamma.financial.convention.calendar.Calendar;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.money.Currency;

/**
 * Description of an interest rate future security.
 */
public class InterestRateFutureSecurityDefinition implements InstrumentDefinitionWithData<InterestRateFutureSecurity, Double> {
  // TODO: Change to InstrumentDefinition (not WithData).

  /**
   * Future last trading date. Usually the date for which the third Wednesday of the month is the spot date.
   */
  private final ZonedDateTime _lastTradingDate;
  /**
   * Ibor index associated to the future.
   */
  private final IborIndex _iborIndex;
  /**
   * Fixing period of the reference Ibor starting date.
   */
  private final ZonedDateTime _fixingPeriodStartDate;
  /**
   * Fixing period of the reference Ibor end date.
   */
  private final ZonedDateTime _fixingPeriodEndDate;
  /**
   * Fixing period of the reference Ibor accrual factor.
   */
  private final double _fixingPeriodAccrualFactor;
  /**
   * Future notional.
   */
  private double _notional;
  /**
   * Future payment accrual factor. Usually a standardized number of 0.25 for a 3M future.
   */
  private final double _paymentAccrualFactor;
  /**
   * Future name.
   */
  private final String _name;

  /**
   * Constructor.
   * @param lastTradingDate The last trading date, not null
   * @param fixingPeriodStartDate The start date of the Ibor fixing period, not null
   * @param fixingPeriodEndDate The end date of the Ibor fixing period, not null. Must be after the fixing period start date
   * @param iborIndex The Ibor index, not null
   * @param notional  The notional
   * @param paymentAccrualFactor The payment accrual factor, not negative or zero
   * @param name The name, not null
   */
  public InterestRateFutureSecurityDefinition(final ZonedDateTime lastTradingDate, final ZonedDateTime fixingPeriodStartDate, final ZonedDateTime fixingPeriodEndDate, final IborIndex iborIndex,
      final double notional, final double paymentAccrualFactor, final String name) {
    ArgumentChecker.notNull(lastTradingDate, "Last trading date");
    ArgumentChecker.notNull(fixingPeriodStartDate, "Fixing period start date");
    ArgumentChecker.notNull(fixingPeriodEndDate, "Fixing period end date");
    ArgumentChecker.isTrue(fixingPeriodEndDate.isAfter(fixingPeriodStartDate), "Fixing start date must be after the fixing end date");
    ArgumentChecker.notNull(iborIndex, "Ibor index");
    ArgumentChecker.notNegativeOrZero(paymentAccrualFactor, "payment accrual factor");
    ArgumentChecker.notNull(name, "Name");
    _lastTradingDate = lastTradingDate;
    _fixingPeriodStartDate = fixingPeriodStartDate;
    _fixingPeriodEndDate = fixingPeriodEndDate;
    _fixingPeriodAccrualFactor = iborIndex.getDayCount().getDayCountFraction(_fixingPeriodStartDate, _fixingPeriodEndDate);
    _iborIndex = iborIndex;
    _notional = notional;
    _paymentAccrualFactor = paymentAccrualFactor;
    _name = name;
  }

  /**
   * Constructor of the interest rate future security.
   * @param lastTradingDate Future last trading date.
   * @param iborIndex Ibor index associated to the future.
   * @param notional Future notional.
   * @param paymentAccrualFactor Future payment accrual factor.
   * @param calendar The holiday calendar for the ibor leg.
   * @param name Future name.
   */
  public InterestRateFutureSecurityDefinition(final ZonedDateTime lastTradingDate, final IborIndex iborIndex, final double notional, final double paymentAccrualFactor, final String name,
      final Calendar calendar) {
    ArgumentChecker.notNull(lastTradingDate, "Last trading date");
    ArgumentChecker.notNull(iborIndex, "Ibor index");
    ArgumentChecker.notNull(name, "Name");
    this._lastTradingDate = lastTradingDate;
    this._iborIndex = iborIndex;
    _fixingPeriodStartDate = ScheduleCalculator.getAdjustedDate(_lastTradingDate, _iborIndex.getSpotLag(), calendar);
    _fixingPeriodEndDate = ScheduleCalculator
        .getAdjustedDate(_fixingPeriodStartDate, _iborIndex.getTenor(), _iborIndex.getBusinessDayConvention(), calendar, _iborIndex.isEndOfMonth());
    _fixingPeriodAccrualFactor = _iborIndex.getDayCount().getDayCountFraction(_fixingPeriodStartDate, _fixingPeriodEndDate);
    this._notional = notional;
    this._paymentAccrualFactor = paymentAccrualFactor;
    _name = name;
  }

  /**
   * Build a interest rate futures transaction from the fixing period start date.
   * @param fixingPeriodStartDate The start date of the fixing period.
   * @param iborIndex The Ibor index associated to the future.
   * @param notional Future notional.
   * @param paymentAccrualFactor Future payment accrual factor.
   * @param name The future name.
   * @param calendar The holiday calendar for the ibor leg.
   * @return The interest rate futures.
   */
  public static InterestRateFutureSecurityDefinition fromFixingPeriodStartDate(final ZonedDateTime fixingPeriodStartDate, final IborIndex iborIndex, final double notional,
      final double paymentAccrualFactor, final String name, final Calendar calendar) {
    ArgumentChecker.notNull(fixingPeriodStartDate, "Fixing period start date");
    ArgumentChecker.notNull(iborIndex, "Ibor index");
    final ZonedDateTime lastTradingDate = ScheduleCalculator.getAdjustedDate(fixingPeriodStartDate, -iborIndex.getSpotLag(), calendar);
    final ZonedDateTime fixingPeriodEndDate = ScheduleCalculator.getAdjustedDate(fixingPeriodStartDate, iborIndex, calendar);
    return new InterestRateFutureSecurityDefinition(lastTradingDate, fixingPeriodStartDate, fixingPeriodEndDate, iborIndex, notional, paymentAccrualFactor, name);
  }

  /** Scales notional to 1.0 in curve fitting to provide better conditioning of the Jacobian */
  public void setUnitNotional() {
    _notional = 1.0;
  }

  /**
   * Gets the future last trading date.
   * @return The last trading date.
   */
  public ZonedDateTime getLastTradingDate() {
    return _lastTradingDate;
  }

  /**
   * Gets the Ibor index associated to the future.
   * @return The Ibor index
   */
  public IborIndex getIborIndex() {
    return _iborIndex;
  }
  
  /**
   * Gets the _unitAmount. This represents the PNL of a single long contract if its price increases by 1.0. Also known as the 'Point Value'. 
   * @return the _unitAmount
   */
  public double getUnitAmount() {
    return _notional * _paymentAccrualFactor;
  }

  /**
   * Gets the fixing period of the reference Ibor starting date.
   * @return The fixing period starting date
   */
  public ZonedDateTime getFixingPeriodStartDate() {
    return _fixingPeriodStartDate;
  }

  /**
   * Gets the fixing period of the reference Ibor end date.
   * @return The fixing period end date.
   */
  public ZonedDateTime getFixingPeriodEndDate() {
    return _fixingPeriodEndDate;
  }

  /**
   * Gets the fixing period of the reference Ibor accrual factor.
   * @return The Fixing period accrual factor.
   */
  public double getFixingPeriodAccrualFactor() {
    return _fixingPeriodAccrualFactor;
  }

  /**
   * Gets the future notional.
   * @return The notional.
   */
  public double getNotional() {
    return _notional;
  }

  /**
   * Gets the future payment accrual factor.
   * @return The future payment accrual factor.
   */
  public double getPaymentAccrualFactor() {
    return _paymentAccrualFactor;
  }

  /**
   * Gets the future name.
   * @return The name
   */
  public String getName() {
    return _name;
  }

  /**
   * The future currency.
   * @return The currency.
   */
  public Currency getCurrency() {
    return _iborIndex.getCurrency();
  }

  @Override
  public InterestRateFutureSecurity toDerivative(final ZonedDateTime dateTime, final String... yieldCurveNames) {
    ArgumentChecker.notNull(dateTime, "date");
    ArgumentChecker.notNull(yieldCurveNames, "yield curve names");
    final LocalDate date = dateTime.toLocalDate();
//    ArgumentChecker.isTrue(yieldCurveNames.length > 1, "at least two curves required");
    final LocalDate lastMarginDateLocal = getFixingPeriodStartDate().toLocalDate();
    if (date.isAfter(lastMarginDateLocal)) {
      throw new ExpiredException("Valuation date, " + date + ", is after last margin date, " + lastMarginDateLocal);
    }
    final String discountingCurveName = yieldCurveNames[0];
    final String forwardCurveName = yieldCurveNames[yieldCurveNames.length > 1 ? 1 : 0];
    final double lastTradingTime = TimeCalculator.getTimeBetween(dateTime, getLastTradingDate());
    final double fixingPeriodStartTime = TimeCalculator.getTimeBetween(dateTime, getFixingPeriodStartDate());
    final double fixingPeriodEndTime = TimeCalculator.getTimeBetween(dateTime, getFixingPeriodEndDate());
    final InterestRateFutureSecurity future = new InterestRateFutureSecurity(lastTradingTime, _iborIndex, fixingPeriodStartTime, fixingPeriodEndTime, _fixingPeriodAccrualFactor, _notional,
        _paymentAccrualFactor, _name, discountingCurveName, forwardCurveName);
    return future;
  }

  @Override
  public InterestRateFutureSecurity toDerivative(final ZonedDateTime date, final Double data, final String... yieldCurveNames) {
    return toDerivative(date, yieldCurveNames); //TODO Should disappear.
  }

  @Override
  public <U, V> V accept(final InstrumentDefinitionVisitor<U, V> visitor, final U data) {
    ArgumentChecker.notNull(visitor, "visitor");
    return visitor.visitInterestRateFutureSecurityDefinition(this, data);
  }

  @Override
  public <V> V accept(final InstrumentDefinitionVisitor<?, V> visitor) {
    ArgumentChecker.notNull(visitor, "visitor");
    return visitor.visitInterestRateFutureSecurityDefinition(this);
  }

  @Override
  public String toString() {
    String result = "IRFuture Security: " + _name;
    result += " Last trading date: " + _lastTradingDate.toString();
    result += " Ibor Index: " + _iborIndex.getName();
    result += " Notional: " + _notional;
    return result;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    long temp;
    temp = Double.doubleToLongBits(_fixingPeriodAccrualFactor);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    result = prime * result + _fixingPeriodEndDate.hashCode();
    result = prime * result + _fixingPeriodStartDate.hashCode();
    result = prime * result + _iborIndex.hashCode();
    result = prime * result + _lastTradingDate.hashCode();
    result = prime * result + _name.hashCode();
    temp = Double.doubleToLongBits(_notional);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(_paymentAccrualFactor);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final InterestRateFutureSecurityDefinition other = (InterestRateFutureSecurityDefinition) obj;
    if (!ObjectUtils.equals(_iborIndex, other._iborIndex)) {
      return false;
    }
    if (!ObjectUtils.equals(_lastTradingDate, other._lastTradingDate)) {
      return false;
    }
    if (!ObjectUtils.equals(_name, other._name)) {
      return false;
    }
    if (Double.doubleToLongBits(_notional) != Double.doubleToLongBits(other._notional)) {
      return false;
    }
    if (Double.doubleToLongBits(_paymentAccrualFactor) != Double.doubleToLongBits(other._paymentAccrualFactor)) {
      return false;
    }
    return true;
  }

}
