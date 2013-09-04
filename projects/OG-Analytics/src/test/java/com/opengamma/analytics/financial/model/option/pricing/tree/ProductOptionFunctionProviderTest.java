/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.analytics.financial.model.option.pricing.tree;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.analytics.financial.model.volatility.BlackFormulaRepository;
import com.opengamma.util.ArgumentChecker;

/**
 * 
 */
public class ProductOptionFunctionProviderTest {

  private static final BinomialTreeOptionPricingModel _model = new BinomialTreeOptionPricingModel();
  private static final double SPOT = 105.;
  private static final double[] STRIKES = new double[] {9900., 11500., 14000. };
  private static final double TIME = 4.2;
  private static final double[] INTERESTS = new double[] {0.017, 0.05 };
  private static final double[] VOLS = new double[] {0.05, 0.1, 0.5 };
  private static final double[] DIVIDENDS = new double[] {0.005, 0.014 };

  /**
   * Analytic formula is available if K =0
   */
  @Test
  public void priceTest() {
    final double[] spotSet2 = new double[] {SPOT * 0.9, SPOT * 1.1 };
    final double sigma2 = 0.15;
    final double[] rhoSet = new double[] {-0.1, 0.6 };
    final int nSteps = 184;

    final double div2 = 0.01;

    final boolean[] tfSet = new boolean[] {true, false };
    for (final boolean isCall : tfSet) {
      for (final double interest : INTERESTS) {
        for (final double vol : VOLS) {
          for (final double spot2 : spotSet2) {
            for (final double rho : rhoSet) {
              for (final double strike : STRIKES) {
                for (final double dividend : DIVIDENDS) {
                  final OptionFunctionProvider2D function = new ProductOptionFunctionProvider(strike, nSteps, isCall);
                  final double rhoVols = rho * vol * sigma2;
                  double exactDiv = Math.exp(-interest * TIME) * BlackFormulaRepository.price(SPOT * spot2 * Math.exp((2 * interest - dividend - div2 + rhoVols) * TIME), strike, TIME,
                      Math.sqrt(vol * vol + sigma2 * sigma2 + 2. * rhoVols), isCall);
                  final double resDiv = _model.getPrice(function, SPOT, spot2, TIME, vol, sigma2, rho, interest, dividend, div2);
                  final double refDiv = Math.max(exactDiv, 1.) * 1.e-2;
                  assertEquals(resDiv, exactDiv, refDiv);
                }
              }
            }
          }
        }
      }
    }
  }

  /**
   * 
   */
  @Test
  public void greeksTest() {
    final double[] spotSet2 = new double[] {SPOT * 0.9, SPOT * 1.1 };
    final double sigma2 = 0.15;
    final double[] rhoSet = new double[] {-0.1, 0.6 };
    final int nSteps = 194;
    final double div2 = 0.01;
    final double eps = 1.e-6;

    final boolean[] tfSet = new boolean[] {true, false };
    for (final boolean isCall : tfSet) {
      for (final double interest : INTERESTS) {
        for (final double vol : VOLS) {
          for (final double spot2 : spotSet2) {
            for (final double rho : rhoSet) {
              for (final double dividend : DIVIDENDS) {
                for (final double strike : STRIKES) {
                  final OptionFunctionProvider2D function = new ProductOptionFunctionProvider(strike, nSteps, isCall);
                  final double rhoVols = rho * vol * sigma2;
                  final double volhat = Math.sqrt(vol * vol + sigma2 * sigma2 + 2. * rhoVols);
                  final double fValue = SPOT * spot2 * Math.exp((2 * interest - dividend - div2 + rhoVols) * TIME);

                  final double price = Math.exp(-interest * TIME) * BlackFormulaRepository.price(fValue, strike, TIME, volhat, isCall);
                  final double delta1 = Math.exp(-interest * TIME) * spot2 * Math.exp((2 * interest - dividend - div2 + rhoVols) * TIME) *
                      BlackFormulaRepository.delta(fValue, strike, TIME, volhat, isCall);
                  final double delta2 = Math.exp(-interest * TIME) * SPOT * Math.exp((2 * interest - dividend - div2 + rhoVols) * TIME) *
                      BlackFormulaRepository.delta(fValue, strike, TIME, volhat, isCall);

                  //                  final double upForTheta = Math.exp(-interest * (TIME + eps)) *
                  //                      BlackFormulaRepository.price(SPOT * spot2 * Math.exp((2 * interest - dividend - div2 + rhoVols) * (TIME + eps)), strike, TIME + eps, volhat, isCall);
                  //                  final double downForTheta = Math.exp(-interest * (TIME - eps)) *
                  //                      BlackFormulaRepository.price(SPOT * spot2 * Math.exp((2 * interest - dividend - div2 + rhoVols) * (TIME - eps)), strike, (TIME - eps), volhat, isCall);
                  //                  final double theta = -0.5 * (upForTheta - downForTheta) / eps;

                  final double gamma1 = Math.exp(-interest * TIME) * spot2 * Math.exp(2. * (2 * interest - dividend - div2 + rhoVols) * TIME) * spot2 *
                      BlackFormulaRepository.gamma(fValue, strike, TIME, volhat);
                  final double gamma2 = Math.exp(-interest * TIME) * SPOT * Math.exp(2. * (2 * interest - dividend - div2 + rhoVols) * TIME) * SPOT *
                      BlackFormulaRepository.gamma(fValue, strike, TIME, volhat);

                  final double upForCross = Math.exp(-interest * TIME) * (SPOT + eps) * Math.exp((2 * interest - dividend - div2 + rhoVols) * TIME) *
                      BlackFormulaRepository.delta((SPOT + eps) * spot2 * Math.exp((2 * interest - dividend - div2 + rhoVols) * TIME), strike, TIME, volhat, isCall);
                  final double downForCross = Math.exp(-interest * TIME) * (SPOT - eps) * Math.exp((2 * interest - dividend - div2 + rhoVols) * TIME) *
                      BlackFormulaRepository.delta((SPOT - eps) * spot2 * Math.exp((2 * interest - dividend - div2 + rhoVols) * TIME), strike, TIME, volhat, isCall);
                  final double cross = 0.5 * (upForCross - downForCross) / eps;

                  /*
                   * Poor approximation of theta
                   */
                  //                  final double[] ref = new double[] {price, delta1, delta2, theta, gamma1, gamma2, cross };

                  final double[] res = _model.getGreeks(function, SPOT, spot2, TIME, vol, sigma2, rho, interest, dividend, div2);
                  final double[] refMod = new double[] {price, delta1, delta2, gamma1, gamma2, cross };
                  final double[] resMod = new double[] {res[0], res[1], res[2], res[4], res[5], res[6] };
                  assertGreeks(resMod, refMod, 1.e-2);
                }
              }
            }
          }
        }
      }
    }
  }

  private void assertGreeks(final double[] res, final double[] ref, final double eps) {
    final int size = res.length;
    ArgumentChecker.isTrue(size == ref.length, "wrong data length");
    for (int i = 0; i < size; ++i) {
      final double error = Math.max(Math.abs(ref[i]), 1.) * eps;
      assertEquals(res[i], ref[i], error);
    }
  }

}
