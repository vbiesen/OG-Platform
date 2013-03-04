/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.analytics.financial.interestrate;

import com.opengamma.analytics.financial.interestrate.future.derivative.BondFutureOptionPremiumTransaction;
import com.opengamma.analytics.financial.interestrate.future.method.BondFutureOptionPremiumTransactionBlackSurfaceMethod;
import com.opengamma.analytics.financial.model.option.definition.YieldCurveWithBlackCubeBundle;
import com.opengamma.util.ArgumentChecker;

/**
 * Calculator of the value gamma, second order derivative of present value with respect to the futures rate, 
 * for InterestRateFutureOptions in the Black world.
 */
public class PresentValueBlackGammaCalculator extends InstrumentDerivativeVisitorAdapter<YieldCurveBundle, Double> {

  /**
   * The unique instance of the calculator.
   */
  private static final PresentValueBlackGammaCalculator INSTANCE = new PresentValueBlackGammaCalculator();

  /**
   * Gets the calculator instance.
   * @return The calculator.
   */
  public static PresentValueBlackGammaCalculator getInstance() {
    return INSTANCE;
  }

  /**
   * Constructor.
   */
  PresentValueBlackGammaCalculator() {
  }

  /**
  * The methods used in the calculator.
  */
  //  private static final InterestRateFutureOptionMarginTransactionBlackSurfaceMethod MARGINNED_IR_FUTURE_OPTION = InterestRateFutureOptionMarginTransactionBlackSurfaceMethod.getInstance();
  private static final BondFutureOptionPremiumTransactionBlackSurfaceMethod PREMIUM_BOND_FUTURE_OPTION = BondFutureOptionPremiumTransactionBlackSurfaceMethod.getInstance();

  //  @Override
  //  public Double visitInterestRateFutureOptionMarginTransaction(final InterestRateFutureOptionMarginTransaction transaction, final YieldCurveBundle curves) {
  //    ArgumentChecker.notNull(transaction, "transaction");
  //    ArgumentChecker.notNull(curves, "curves");
  //    ArgumentChecker.isTrue(curves instanceof YieldCurveWithBlackCubeBundle, "Yield curve bundle should contain Black cube");
  //    return MARGINNED_IR_FUTURE_OPTION.presentValueGamma(transaction, (YieldCurveWithBlackCubeBundle) curves);
  //  }
  //
  //  @Override
  //  public Double visitInterestRateFutureOptionPremiumTransaction(final InterestRateFutureOptionPremiumTransaction option, final YieldCurveBundle curves) {
  //    ArgumentChecker.notNull(curves, "curves");
  //    ArgumentChecker.notNull(option, "option");
  //    if (curves instanceof YieldCurveWithBlackCubeBundle) {
  //      final InterestRateFutureOptionPremiumSecurity underlyingOption = option.getUnderlyingOption();
  //      final InterestRateFutureOptionMarginSecurity underlyingMarginedOption = new InterestRateFutureOptionMarginSecurity(underlyingOption.getUnderlyingFuture(), underlyingOption.getExpirationTime(),
  //          underlyingOption.getStrike(), underlyingOption.isCall());
  //      final InterestRateFutureOptionMarginTransaction margined = new InterestRateFutureOptionMarginTransaction(underlyingMarginedOption, option.getQuantity(), option.getTradePrice());
  //      return MARGINNED_IR_FUTURE_OPTION.presentValueGamma(margined, (YieldCurveWithBlackCubeBundle) curves);
  //    }
  //    throw new UnsupportedOperationException("The PresentValueBlackCalculator visitor visitInterestRateFutureOptionPremiumTransaction requires a YieldCurveWithBlackCubeBundle as data.");
  //  }

  @Override
  public Double visitBondFutureOptionPremiumTransaction(final BondFutureOptionPremiumTransaction transaction, final YieldCurveBundle curves) {
    ArgumentChecker.notNull(transaction, "transaction");
    ArgumentChecker.notNull(curves, "curves");
    ArgumentChecker.isTrue(curves instanceof YieldCurveWithBlackCubeBundle, "Yield curve bundle should contain Black cube");
    return PREMIUM_BOND_FUTURE_OPTION.presentValueGamma(transaction, (YieldCurveWithBlackCubeBundle) curves);
  }

}
