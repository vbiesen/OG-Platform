/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.analytics.financial.interestrate.payments.derivative;

import java.util.Arrays;

import org.apache.commons.lang.ObjectUtils;

import com.opengamma.analytics.financial.instrument.index.IndexON;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivativeVisitor;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.money.Currency;

/**
 * Class describing a Fed Fund swap-like floating coupon (arithmetic average on overnight rates).
 */
public final class CouponArithmeticAverageONSpread extends Coupon {

  /**
   * The overnight index on which the coupon fixes. The index currency should be the same as the coupon currency. Not null.
   */
  private final IndexON _index;
  /**
   * The times of the fixing periods. The length is one greater than the number of periods, as it includes accrual start and end.
   */
  private final double[] _fixingPeriodTimes;
  /**
   * The accrual factors (or year fractions) associated to the fixing periods in the Index day count convention.
   */
  private final double[] _fixingPeriodAccrualFactors;
  /**
   * The interest accrued over the periods already fixed multiplied by the accrual factors, i.e. the sum (\delta_i r_i). The spread is not included in the value.
   */
  private final double _rateAccrued;
  /**
   * The accrual factor (or year fraction) associated to the remaining fixing period in the Index day count convention.
   */
  private final double _fixingPeriodRemainingAccrualFactor;
  /**
   * The spread rate paid above the arithmetic average.
   */
  private final double _spread;
  /**
   * The fixed amount related to the spread.
   */
  private final double _spreadAmount;

  // TODO: Implement the rate cut-off mechanism (the two last periods use the same fixing)

  /**
   * Constructor.
   * @param currency The coupon currency.
   * @param paymentTime The coupon payment time.
   * @param paymentAccrualFactor The year fraction of the full coupon.
   * @param notional The coupon notional.
   * @param index The index associated to the coupon.
   * @param fixingPeriodTimes The times of the remaining fixing. The length is one greater than the number of periods, as it includes accrual start and end.
   * @param fixingPeriodAccrualFactors The accrual factors (or year fractions) associated to the fixing periods in the Index day count convention.
   * @param rateAccrued The interest accrued over the periods already fixed.
   * @param fixingPeriodRemainingAccrualFactor ??
   * @param spread The spread rate paid above the arithmetic average.
   */
  private CouponArithmeticAverageONSpread(Currency currency, double paymentTime, double paymentYearFraction, double notional, IndexON index, double[] fixingPeriodTimes,
      double[] fixingPeriodAccrualFactors, double rateAccrued, double fixingPeriodRemainingAccrualFactor, final double spread) {
    super(currency, paymentTime, "NOT USED", paymentYearFraction, notional);
    _index = index;
    _fixingPeriodTimes = fixingPeriodTimes;
    _fixingPeriodAccrualFactors = fixingPeriodAccrualFactors;
    _rateAccrued = rateAccrued;
    _fixingPeriodRemainingAccrualFactor = fixingPeriodRemainingAccrualFactor;
    _spread = spread;
    _spreadAmount = spread * paymentYearFraction * notional;
  }

  /**
   * Builder from financial details.
   * @param paymentTime The coupon payment time.
   * @param paymentAccrualFactor The year fraction of the full coupon.
   * @param notional The coupon notional.
   * @param index The index associated to the coupon.
   * @param fixingPeriodTimes The times of the remaining fixing. The length is one greater than the number of periods, as it includes accrual start and end.
   * @param fixingPeriodAccrualFactors The accrual factors (or year fractions) associated to the fixing periods in the Index day count convention.
   * @param rateAccrued The interest accrued over the periods already fixed.
   * @param spread The spread rate paid above the arithmetic average.
   * @return The coupon.
   */
  public static CouponArithmeticAverageONSpread from(double paymentTime, double paymentAccrualFactor, double notional, IndexON index, double[] fixingPeriodTimes, double[] fixingPeriodAccrualFactors,
      double rateAccrued, final double spread) {
    ArgumentChecker.notNull(index, "Index");
    ArgumentChecker.notNull(fixingPeriodTimes, "Fixing Times");
    ArgumentChecker.notNull(fixingPeriodAccrualFactors, "Accrual Factors");
    double fixingPeriodRemainingAccrualFactor = 0.0;
    for (int loopp = 0; loopp < fixingPeriodAccrualFactors.length; loopp++) {
      fixingPeriodRemainingAccrualFactor += fixingPeriodAccrualFactors[loopp];
    }
    return new CouponArithmeticAverageONSpread(index.getCurrency(), paymentTime, paymentAccrualFactor, notional, index, fixingPeriodTimes, fixingPeriodAccrualFactors, rateAccrued,
        fixingPeriodRemainingAccrualFactor, spread);
  }

  /**
   * Gets the index.
   * @return The index.
   */
  public IndexON getIndex() {
    return _index;
  }

  /**
   * Gets the times of the fixing periods.
   * @return The times.
   */
  public double[] getFixingPeriodTimes() {
    return _fixingPeriodTimes;
  }

  /**
   * Gets the fixingPeriodAccrualFactors field.
   * @return the fixingPeriodAccrualFactors
   */
  public double[] getFixingPeriodAccrualFactors() {
    return _fixingPeriodAccrualFactors;
  }

  /**
   * Gets the notionalAccrued field.
   * @return the notionalAccrued
   */
  public double getRateAccrued() {
    return _rateAccrued;
  }

  /**
   * Gets the fixingPeriodTotalAccrualFactor field.
   * @return the fixingPeriodTotalAccrualFactor
   */
  public double getFixingPeriodRemainingAccrualFactor() {
    return _fixingPeriodRemainingAccrualFactor;
  }

  /**
   * Returns the spread rate paid above the arithmetic average.
   * @return The spread.
   */
  public double getSpread() {
    return _spread;
  }

  /**
   * Returns the fixed amount related to the spread.
   * @return The amount.
   */
  public double getSpreadAmount() {
    return _spreadAmount;
  }

  @Override
  public Coupon withNotional(double notional) {
    return null; // TODO
  }

  @Override
  public <S, T> T accept(InstrumentDerivativeVisitor<S, T> visitor, S data) {
    ArgumentChecker.notNull(visitor, "visitor");
    return visitor.visitCouponArithmeticAverageONSpread(this, data);
  }

  @Override
  public <T> T accept(InstrumentDerivativeVisitor<?, T> visitor) {
    ArgumentChecker.notNull(visitor, "visitor");
    return visitor.visitCouponArithmeticAverageONSpread(this);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Arrays.hashCode(_fixingPeriodAccrualFactors);
    long temp;
    temp = Double.doubleToLongBits(_fixingPeriodRemainingAccrualFactor);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    result = prime * result + Arrays.hashCode(_fixingPeriodTimes);
    result = prime * result + _index.hashCode();
    temp = Double.doubleToLongBits(_rateAccrued);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(_spread);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(_spreadAmount);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    CouponArithmeticAverageONSpread other = (CouponArithmeticAverageONSpread) obj;
    if (!Arrays.equals(_fixingPeriodAccrualFactors, other._fixingPeriodAccrualFactors)) {
      return false;
    }
    if (Double.doubleToLongBits(_fixingPeriodRemainingAccrualFactor) != Double.doubleToLongBits(other._fixingPeriodRemainingAccrualFactor)) {
      return false;
    }
    if (!Arrays.equals(_fixingPeriodTimes, other._fixingPeriodTimes)) {
      return false;
    }
    if (!ObjectUtils.equals(_index, other._index)) {
      return false;
    }
    if (Double.doubleToLongBits(_rateAccrued) != Double.doubleToLongBits(other._rateAccrued)) {
      return false;
    }
    if (Double.doubleToLongBits(_spread) != Double.doubleToLongBits(other._spread)) {
      return false;
    }
    if (Double.doubleToLongBits(_spreadAmount) != Double.doubleToLongBits(other._spreadAmount)) {
      return false;
    }
    return true;
  }

}
