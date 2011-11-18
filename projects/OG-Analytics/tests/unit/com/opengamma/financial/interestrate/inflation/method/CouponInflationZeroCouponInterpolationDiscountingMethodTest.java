/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.interestrate.inflation.method;

import static org.testng.AssertJUnit.assertEquals;

import java.util.List;

import javax.time.calendar.Period;
import javax.time.calendar.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.financial.convention.businessday.BusinessDayConvention;
import com.opengamma.financial.convention.businessday.BusinessDayConventionFactory;
import com.opengamma.financial.convention.calendar.Calendar;
import com.opengamma.financial.instrument.index.IborIndex;
import com.opengamma.financial.instrument.index.IndexDeposit;
import com.opengamma.financial.instrument.index.PriceIndex;
import com.opengamma.financial.instrument.inflation.CouponInflationZeroCouponInterpolationDefinition;
import com.opengamma.financial.interestrate.PresentValueInflationCalculator;
import com.opengamma.financial.interestrate.inflation.derivatives.CouponInflationZeroCouponInterpolation;
import com.opengamma.financial.interestrate.market.MarketBundle;
import com.opengamma.financial.interestrate.market.MarketDataSets;
import com.opengamma.financial.interestrate.market.PresentValueCurveSensitivityMarket;
import com.opengamma.financial.interestrate.method.market.SensitivityFiniteDifferenceMarket;
import com.opengamma.financial.schedule.ScheduleCalculator;
import com.opengamma.math.differentiation.FiniteDifferenceType;
import com.opengamma.util.money.CurrencyAmount;
import com.opengamma.util.time.DateUtils;
import com.opengamma.util.tuple.DoublesPair;

/**
 * Tests the present value and its sensitivities for zero-coupon with reference index interpolated between months.
 */
public class CouponInflationZeroCouponInterpolationDiscountingMethodTest {
  private static final MarketBundle MARKET = MarketDataSets.createMarket1();
  private static final PriceIndex[] PRICE_INDEXES = MarketDataSets.getPriceIndexes();
  private static final PriceIndex PRICE_INDEX_EUR = PRICE_INDEXES[0];
  private static final PriceIndex PRICE_INDEX_US = PRICE_INDEXES[2];
  private static final IndexDeposit[] IBOR_INDEXES = MarketDataSets.getDepositIndexes();
  private static final IborIndex EURIBOR3M = (IborIndex) IBOR_INDEXES[0];
  private static final IborIndex USDLIBOR3M = (IborIndex) IBOR_INDEXES[3];
  private static final Calendar CALENDAR_EUR = EURIBOR3M.getCalendar();
  private static final Calendar CALENDAR_USD = USDLIBOR3M.getCalendar();
  private static final BusinessDayConvention BUSINESS_DAY = BusinessDayConventionFactory.INSTANCE.getBusinessDayConvention("Modified Following");
  private static final ZonedDateTime START_DATE = DateUtils.getUTCDate(2008, 8, 18);
  private static final Period COUPON_TENOR = Period.ofYears(10);
  private static final ZonedDateTime PAYMENT_DATE = ScheduleCalculator.getAdjustedDate(START_DATE, BUSINESS_DAY, CALENDAR_EUR, COUPON_TENOR);
  private static final double NOTIONAL = 98765432;
  private static final int MONTH_LAG = 3;
  private static final double INDEX_MAY_2008_INT = 108.4548387; // May index: 108.23 - June Index = 108.64
  private static final CouponInflationZeroCouponInterpolationDefinition ZERO_COUPON_1_DEFINITION = CouponInflationZeroCouponInterpolationDefinition.from(START_DATE, PAYMENT_DATE, NOTIONAL,
      PRICE_INDEX_EUR, INDEX_MAY_2008_INT, MONTH_LAG, false);
  private static final ZonedDateTime PRICING_DATE = DateUtils.getUTCDate(2011, 8, 3);
  private static final CouponInflationZeroCouponInterpolation ZERO_COUPON_1 = ZERO_COUPON_1_DEFINITION.toDerivative(PRICING_DATE, "not used");
  private static final CouponInflationZeroCouponInterpolationDiscountingMethod METHOD = CouponInflationZeroCouponInterpolationDiscountingMethod.getInstance();
  private static final PresentValueInflationCalculator PVIC = PresentValueInflationCalculator.getInstance();

  @Test
  /**
   * Tests the present value.
   */
  public void presentValue() {
    CurrencyAmount pv = METHOD.presentValue(ZERO_COUPON_1, MARKET);
    double df = MARKET.getCurve(ZERO_COUPON_1.getCurrency()).getDiscountFactor(ZERO_COUPON_1.getPaymentTime());
    double indexMonth0 = MARKET.getCurve(PRICE_INDEX_EUR).getPriceIndex(ZERO_COUPON_1.getReferenceEndTime()[0]);
    double indexMonth1 = MARKET.getCurve(PRICE_INDEX_EUR).getPriceIndex(ZERO_COUPON_1.getReferenceEndTime()[1]);
    double finalIndex = ZERO_COUPON_1_DEFINITION.getWeight() * indexMonth0 + (1 - ZERO_COUPON_1_DEFINITION.getWeight()) * indexMonth1;
    double pvExpected = (finalIndex / INDEX_MAY_2008_INT - 1) * df * NOTIONAL;
    assertEquals("Zero-coupon inflation: Present value", pvExpected, pv.getAmount(), 1.0E-2);
  }

  @Test
  /**
   * Tests the present value: Method vs Calculator.
   */
  public void presentValueMethodVsCalculator() {
    CurrencyAmount pvMethod = METHOD.presentValue(ZERO_COUPON_1, MARKET);
    CurrencyAmount pvCalculator = PVIC.visit(ZERO_COUPON_1, MARKET);
    assertEquals("Zero-coupon inflation: Present value", pvMethod, pvCalculator);
  }

  @Test
  /**
   * Test the present value curves sensitivity.
   */
  public void presentValueCurveSensitivity() {
    final PresentValueCurveSensitivityMarket pvs = METHOD.presentValueCurveSensitivity(ZERO_COUPON_1, MARKET);
    pvs.clean();
    final double deltaTolerancePrice = 1.0E+1;
    //Testing note: Sensitivity is for a movement of 1. 1E+2 = 1 cent for a 1 bp move. Tolerance increased to cope with numerical imprecision of finite difference.
    final double deltaShift = 1.0E-6;
    // 2. Discounting curve sensitivity
    final double[] nodeTimesDisc = new double[] {ZERO_COUPON_1.getPaymentTime()};
    final double[] sensiDisc = SensitivityFiniteDifferenceMarket.curveSensitivity(ZERO_COUPON_1, MARKET, ZERO_COUPON_1.getCurrency(), nodeTimesDisc, deltaShift, METHOD, FiniteDifferenceType.CENTRAL);
    assertEquals("Sensitivity finite difference method: number of node", 1, sensiDisc.length);
    final List<DoublesPair> sensiPvDisc = pvs.getYieldCurveSensitivities().get(MARKET.getCurve(ZERO_COUPON_1.getCurrency()).getCurve().getName());
    for (int loopnode = 0; loopnode < sensiDisc.length; loopnode++) {
      final DoublesPair pairPv = sensiPvDisc.get(loopnode);
      assertEquals("Sensitivity coupon pv to forward curve: Node " + loopnode, nodeTimesDisc[loopnode], pairPv.getFirst(), 1E-8);
      assertEquals("Sensitivity finite difference method: node sensitivity", pairPv.second, sensiDisc[loopnode], deltaTolerancePrice);
    }
    // 3. Price index curve sensitivity
    final double[] nodeTimesPrice = ZERO_COUPON_1.getReferenceEndTime();
    final double[] sensiPrice = SensitivityFiniteDifferenceMarket.curveSensitivity(ZERO_COUPON_1, MARKET, ZERO_COUPON_1.getPriceIndex(), nodeTimesPrice, deltaShift, METHOD,
        FiniteDifferenceType.CENTRAL);
    assertEquals("Sensitivity finite difference method: number of node", 2, sensiPrice.length);
    final List<DoublesPair> sensiPvPrice = pvs.getPriceCurveSensitivities().get(MARKET.getCurve(ZERO_COUPON_1.getPriceIndex()).getCurve().getName());
    for (int loopnode = 0; loopnode < sensiPrice.length; loopnode++) {
      final DoublesPair pairPv = sensiPvPrice.get(loopnode);
      assertEquals("Sensitivity coupon pv to forward curve: Node " + loopnode, nodeTimesPrice[loopnode], pairPv.getFirst(), 1E-8);
      assertEquals("Sensitivity finite difference method: node sensitivity", pairPv.second, sensiPrice[loopnode], deltaTolerancePrice);
    }
  }

  @Test
  /**
   * Tests the present value for curves with seasonal adjustment.
   */
  public void presentValueSeasonality() {
    MarketBundle marketSeason = MarketDataSets.createMarket2(PRICING_DATE);
    int tenorYear = 5;
    double notional = 100000000;
    ZonedDateTime settleDate = ScheduleCalculator.getAdjustedDate(PRICING_DATE, CALENDAR_USD, USDLIBOR3M.getSettlementDays());
    ZonedDateTime paymentDate = ScheduleCalculator.getAdjustedDate(settleDate, BUSINESS_DAY, CALENDAR_USD, USDLIBOR3M.isEndOfMonth(), Period.ofYears(tenorYear));
    double weightSettle = 1.0 - (settleDate.getDayOfMonth() - 1.0) / settleDate.getMonthOfYear().getLastDayOfMonth(settleDate.isLeapYear());
    double indexStart = weightSettle * 225.964 + (1 - weightSettle) * 225.722;
    CouponInflationZeroCouponInterpolationDefinition zeroCouponUsdDefinition = CouponInflationZeroCouponInterpolationDefinition.from(settleDate, paymentDate, notional, PRICE_INDEX_US, indexStart,
        MONTH_LAG, false);
    CouponInflationZeroCouponInterpolation zeroCouponUsd = zeroCouponUsdDefinition.toDerivative(PRICING_DATE, "not used");
    CurrencyAmount pvInflation = METHOD.presentValue(zeroCouponUsd, marketSeason);
    double df = MARKET.getCurve(zeroCouponUsd.getCurrency()).getDiscountFactor(zeroCouponUsd.getPaymentTime());
    double indexMonth0 = marketSeason.getCurve(PRICE_INDEX_US).getPriceIndex(zeroCouponUsd.getReferenceEndTime()[0]);
    double indexMonth1 = marketSeason.getCurve(PRICE_INDEX_US).getPriceIndex(zeroCouponUsd.getReferenceEndTime()[1]);
    double finalIndex = zeroCouponUsdDefinition.getWeight() * indexMonth0 + (1 - zeroCouponUsdDefinition.getWeight()) * indexMonth1;
    double pvExpected = (finalIndex / indexStart - 1) * df * notional;
    assertEquals("PV in market with seasonal adjustment", pvExpected, pvInflation.getAmount(), 1E-2);
  }
}
