/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.analytics.financial.model.option.pricing.tree;

import org.apache.commons.lang.Validate;

import com.google.common.primitives.Doubles;
import com.opengamma.analytics.financial.greeks.Greek;
import com.opengamma.analytics.financial.greeks.GreekResultCollection;
import com.opengamma.analytics.financial.model.option.definition.OptionPayoffFunction;
import com.opengamma.analytics.financial.model.option.definition.StandardOptionDataBundle;
import com.opengamma.util.ArgumentChecker;

/**
 *
 */
public class BinomialTreeOptionPricingModel extends TreeOptionPricingModel {

  private enum BarrierTypes {
    DownAndOut, UpAndOut,
    //    DownAndIn, UpAndIn; //These two MUST be computed via in-out parity//
  }

  private final double _barrier;
  private final BarrierTypes _typeName;
  private PayoffFunction _function;

  /*
   * TODO time-varying vol may not be compatible to discrete dividends due to limited control of dt
   *
   * TODO check convergence of theta
   *
   * TODO More accuracy for barrier case??? such as barrier options, two asset correlation option
   *
   * <<Slight modification of American>>
   * TODO Bermudan option (PLAT-4459)
   */

  @SuppressWarnings("synthetic-access")
  public BinomialTreeOptionPricingModel() {
    _barrier = 0.;
    _typeName = null;
    _function = new VanillaPayoff();
  }

  @SuppressWarnings("synthetic-access")
  public BinomialTreeOptionPricingModel(final double barrier, final String typeName) {
    _barrier = barrier;
    _typeName = BarrierTypes.valueOf(typeName);
    switch (_typeName) {
      case DownAndOut:
        _function = new DownAndOutPayoff();
        break;
      case UpAndOut:
        _function = new UpAndOutPayoff();
        break;
    }
  }

  @Override
  public double getPrice(final LatticeSpecification lattice, final OptionFunctionProvider1D function, final double spot, final double timeToExpiry, final double volatility,
      final double interestRate, final double dividend) {
    ArgumentChecker.notNull(lattice, "lattice");
    ArgumentChecker.notNull(function, "function");
    ArgumentChecker.isTrue(spot > 0., "Spot should be positive");
    ArgumentChecker.isTrue(Doubles.isFinite(spot), "Spot should be finite");
    ArgumentChecker.isTrue(timeToExpiry > 0., "timeToExpiry should be positive");
    ArgumentChecker.isTrue(Doubles.isFinite(timeToExpiry), "timeToExpiry should be finite");
    ArgumentChecker.isTrue(volatility > 0., "volatility should be positive");
    ArgumentChecker.isTrue(Doubles.isFinite(volatility), "volatility should be finite");
    ArgumentChecker.isTrue(Doubles.isFinite(interestRate), "interestRate should be finite");
    ArgumentChecker.isTrue(Doubles.isFinite(dividend), "dividend should be finite");

    final LatticeSpecification modLattice = (lattice instanceof TimeVaryingLatticeSpecification) ? new TrigeorgisLatticeSpecification() : lattice;
    if (function instanceof BarrierOptionFunctionProvider) {
      final BarrierOptionFunctionProvider barrierFunction = (BarrierOptionFunctionProvider) function;
      if (barrierFunction.getChecker().checkOut(spot) || barrierFunction.getChecker().checkStrikeBehindBarrier()) {
        return 0.;
      }
    }

    final int nSteps = function.getNumberOfSteps();
    final double strike = function.getStrike();

    final double dt = timeToExpiry / nSteps;
    final double discount = Math.exp(-interestRate * dt);
    final double[] params = modLattice.getParameters(spot, strike, timeToExpiry, volatility, interestRate - dividend, nSteps, dt);
    final double upFactor = params[0];
    final double downFactor = params[1];
    final double upProbability = params[2];
    final double downProbability = params[3];
    final double upOverDown = upFactor / downFactor;
    ArgumentChecker.isTrue(upProbability > 0., "upProbability should be greater than 0.");
    ArgumentChecker.isTrue(upProbability < 1., "upProbability should be smaller than 1.");

    final double assetPrice = spot * Math.pow(downFactor, nSteps);
    double[] values = function.getPayoffAtExpiry(assetPrice, upOverDown);
    for (int i = nSteps - 1; i > -1; --i) {
      values = function.getNextOptionValues(discount, upProbability, downProbability, values, spot, 0., downFactor, upOverDown, i);
    }

    return values[0];
  }

  /*
   * Array is used for dividend to realize constant cost of carry given by b = r - q
   */
  @Override
  public double getPrice(final OptionFunctionProvider1D function, final double spot, final double timeToExpiry, final double[] volatility, final double[] interestRate, final double[] dividend) {
    ArgumentChecker.notNull(function, "function");
    ArgumentChecker.notNull(volatility, "volatility");
    ArgumentChecker.notNull(interestRate, "interestRate");
    ArgumentChecker.notNull(dividend, "dividend");

    ArgumentChecker.isTrue(spot > 0., "Spot should be positive");
    ArgumentChecker.isTrue(Doubles.isFinite(spot), "Spot should be finite");
    ArgumentChecker.isTrue(timeToExpiry > 0., "timeToExpiry should be positive");
    ArgumentChecker.isTrue(Doubles.isFinite(timeToExpiry), "timeToExpiry should be finite");

    final TimeVaryingLatticeSpecification vLattice = new TimeVaryingLatticeSpecification();
    final int nSteps = function.getNumberOfSteps();

    ArgumentChecker.isTrue(nSteps == interestRate.length, "Wrong interestRate length");
    ArgumentChecker.isTrue(nSteps == volatility.length, "Wrong volatility length");
    ArgumentChecker.isTrue(nSteps == dividend.length, "Wrong dividend length");

    for (int i = 0; i < nSteps; ++i) {
      ArgumentChecker.isTrue(volatility[i] > 0., "volatility should be positive");
      ArgumentChecker.isTrue(Doubles.isFinite(volatility[i]), "volatility should be finite");
      ArgumentChecker.isTrue(Doubles.isFinite(interestRate[i]), "interestRate should be finite");
      ArgumentChecker.isTrue(Doubles.isFinite(dividend[i]), "dividend should be finite");
    }

    if (function instanceof BarrierOptionFunctionProvider) {
      final BarrierOptionFunctionProvider barrierFunction = (BarrierOptionFunctionProvider) function;
      if (barrierFunction.getChecker().checkOut(spot) || barrierFunction.getChecker().checkStrikeBehindBarrier()) {
        return 0.;
      }
    }

    final double[] nu = vLattice.getShiftedDrift(volatility, interestRate, dividend);
    final double spaceStep = vLattice.getSpaceStep(timeToExpiry, volatility, nSteps, nu);
    final double downFactor = Math.exp(-spaceStep);
    final double upOverDown = Math.exp(2. * spaceStep);

    final double[] upProbability = new double[nSteps];
    final double[] downProbability = new double[nSteps];
    final double[] df = new double[nSteps];
    for (int i = 0; i < nSteps; ++i) {
      final double[] params = vLattice.getParameters(volatility[i], nu[i], spaceStep);
      upProbability[i] = params[1];
      downProbability[i] = 1. - params[1];
      df[i] = Math.exp(-interestRate[i] * params[0]);
      ArgumentChecker.isTrue(upProbability[i] > 0., "upProbability should be greater than 0.");
      ArgumentChecker.isTrue(upProbability[i] < 1., "upProbability should be smaller than 1.");
    }

    final double assetPrice = spot * Math.pow(downFactor, nSteps);
    double[] values = function.getPayoffAtExpiry(assetPrice, upOverDown);
    for (int i = nSteps - 1; i > -1; --i) {
      values = function.getNextOptionValues(df[i], upProbability[i], downProbability[i], values, spot, 0., downFactor, upOverDown, i);
    }

    return values[0];
  }

  @Override
  public double getPrice(final LatticeSpecification lattice, final OptionFunctionProvider1D function, final double spot, final double timeToExpiry, final double volatility,
      final double interestRate, final DividendFunctionProvider dividend) {
    ArgumentChecker.notNull(lattice, "lattice");
    ArgumentChecker.notNull(function, "function");
    ArgumentChecker.notNull(dividend, "dividend");

    ArgumentChecker.isTrue(spot > 0., "Spot should be positive");
    ArgumentChecker.isTrue(Doubles.isFinite(spot), "Spot should be finite");
    ArgumentChecker.isTrue(timeToExpiry > 0., "timeToExpiry should be positive");
    ArgumentChecker.isTrue(Doubles.isFinite(timeToExpiry), "timeToExpiry should be finite");
    ArgumentChecker.isTrue(volatility > 0., "volatility should be positive");
    ArgumentChecker.isTrue(Doubles.isFinite(volatility), "volatility should be finite");
    ArgumentChecker.isTrue(Doubles.isFinite(interestRate), "interestRate should be finite");

    final LatticeSpecification modLattice = (lattice instanceof TimeVaryingLatticeSpecification) ? new TrigeorgisLatticeSpecification() : lattice;
    if (function instanceof BarrierOptionFunctionProvider) {
      final BarrierOptionFunctionProvider barrierFunction = (BarrierOptionFunctionProvider) function;
      if (barrierFunction.getChecker().checkOut(spot) || barrierFunction.getChecker().checkStrikeBehindBarrier()) {
        return 0.;
      }
    }

    final int nSteps = function.getNumberOfSteps();
    final double strike = function.getStrike();

    final double dt = timeToExpiry / nSteps;
    ArgumentChecker.isTrue(dividend.checkTimeSteps(dt), "Number of steps is too small");

    final double discount = Math.exp(-interestRate * dt);
    final double[] params = modLattice.getParameters(spot, strike, timeToExpiry, volatility, interestRate, nSteps, dt);
    final double upFactor = params[0];
    final double downFactor = params[1];
    final double upProbability = params[2];
    final double downProbability = params[3];
    final double upOverDown = upFactor / downFactor;
    ArgumentChecker.isTrue(upProbability > 0., "upProbability should be greater than 0.");
    ArgumentChecker.isTrue(upProbability < 1., "upProbability should be smaller than 1.");

    final int[] divSteps = dividend.getDividendSteps(dt);

    double assetPriceBase = dividend.spotModifier(spot, interestRate);
    final double assetPriceTerminal = assetPriceBase * Math.pow(downFactor, nSteps);
    double[] values = function.getPayoffAtExpiry(assetPriceTerminal, upOverDown);

    int counter = 0;
    final int nDivs = dividend.getNumberOfDividends();

    if (dividend instanceof ProportionalDividendFunctionProvider) {
      for (int i = nSteps - 1; i > -1; --i) {
        for (int k = nDivs - 1 - counter; k > -1; --k) {
          if (i == divSteps[k]) {
            assetPriceBase = dividend.dividendCorrections(assetPriceBase, 0., 0., k);
            ++counter;
          }
        }
        values = function.getNextOptionValues(discount, upProbability, downProbability, values, assetPriceBase, 0., downFactor, upOverDown, i);
      }
    } else {
      double sumDiscountDiv = 0.;
      for (int i = nSteps - 1; i > -1; --i) {
        sumDiscountDiv *= Math.exp(-interestRate * dt);
        for (int k = nDivs - 1 - counter; k > -1; --k) {
          if (i == divSteps[k]) {
            sumDiscountDiv = dividend.dividendCorrections(sumDiscountDiv, interestRate, dt * i, k);
            ++counter;
          }
        }
        values = function.getNextOptionValues(discount, upProbability, downProbability, values, assetPriceBase, sumDiscountDiv, downFactor, upOverDown, i);
      }
    }

    return values[0];
  }

  @Override
  public double getPrice(final OptionFunctionProvider2D function, final double spot1, final double spot2, final double timeToExpiry, final double volatility1, final double volatility2,
      final double correlation, final double interestRate, final double dividend1, final double dividend2) {
    ArgumentChecker.notNull(function, "function");

    ArgumentChecker.isTrue(spot1 > 0., "spot1 should be positive");
    ArgumentChecker.isTrue(Doubles.isFinite(spot1), "spot1 should be finite");
    ArgumentChecker.isTrue(spot2 > 0., "spot2 should be positive");
    ArgumentChecker.isTrue(Doubles.isFinite(spot2), "spot2 should be finite");
    ArgumentChecker.isTrue(timeToExpiry > 0., "timeToExpiry should be positive");
    ArgumentChecker.isTrue(Doubles.isFinite(timeToExpiry), "timeToExpiry should be finite");
    ArgumentChecker.isTrue(volatility1 > 0., "volatility1 should be positive");
    ArgumentChecker.isTrue(Doubles.isFinite(volatility1), "volatility1 should be finite");
    ArgumentChecker.isTrue(volatility2 > 0., "volatility2 should be positive");
    ArgumentChecker.isTrue(Doubles.isFinite(volatility2), "volatility2 should be finite");
    ArgumentChecker.isTrue(correlation >= -1. && correlation <= 1., "correlation should be -1. <= rho <= 1.");
    ArgumentChecker.isTrue(Doubles.isFinite(interestRate), "interestRate should be finite");
    ArgumentChecker.isTrue(Doubles.isFinite(dividend1), "dividend1 should be finite");
    ArgumentChecker.isTrue(Doubles.isFinite(dividend2), "dividend2 should be finite");

    final int nSteps = function.getNumberOfSteps();

    final double vol12 = volatility1 * volatility2;
    final double vol11 = volatility1 * volatility1;
    final double vol22 = volatility2 * volatility2;

    final double dt = timeToExpiry / nSteps;
    final double discount = Math.exp(-interestRate * dt);
    final double rootDt = Math.sqrt(dt);
    final double dx1 = volatility1 * rootDt;
    final double dx2 = volatility2 * rootDt;

    final double dx12 = dx1 * dx2;
    final double nu1Factored = (interestRate - dividend1 - 0.5 * vol11) * dx2 * dt;
    final double nu2Factored = (interestRate - dividend2 - 0.5 * vol22) * dx1 * dt;
    final double vol12Factored = vol12 * correlation * dt;

    final double uuProbability = 0.25 * (dx12 + nu1Factored + nu2Factored + vol12Factored) / dx12;
    final double udProbability = 0.25 * (dx12 + nu1Factored - nu2Factored - vol12Factored) / dx12;
    final double duProbability = 0.25 * (dx12 - nu1Factored + nu2Factored - vol12Factored) / dx12;
    final double ddProbability = 0.25 * (dx12 - nu1Factored - nu2Factored + vol12Factored) / dx12;
    ArgumentChecker.isTrue(uuProbability > 0. && uuProbability < 1., "uuProbability should be 0 < p < 1.");
    ArgumentChecker.isTrue(udProbability > 0. && udProbability < 1., "udProbability should be 0 < p < 1.");
    ArgumentChecker.isTrue(duProbability > 0. && duProbability < 1., "duProbability should be 0 < p < 1.");
    ArgumentChecker.isTrue(ddProbability > 0. && ddProbability < 1., "ddProbability should be 0 < p < 1.");

    final double downFactor1 = Math.exp(-dx1);
    final double downFactor2 = Math.exp(-dx2);
    final double upOverDown1 = Math.exp(2. * dx1);
    final double upOverDown2 = Math.exp(2. * dx2);

    final double assetPrice1 = spot1 * Math.pow(downFactor1, nSteps);
    final double assetPrice2 = spot2 * Math.pow(downFactor2, nSteps);
    double[][] values = function.getPayoffAtExpiry(assetPrice1, assetPrice2, upOverDown1, upOverDown2);
    for (int i = nSteps - 1; i > -1; --i) {
      values = function.getNextOptionValues(discount, uuProbability, udProbability, duProbability, ddProbability, values, spot1, spot2, downFactor1, downFactor2, upOverDown1, upOverDown2, i);
    }

    return values[0][0];
  }

  @Override
  public GreekResultCollection getGreeks(final LatticeSpecification lattice, final OptionFunctionProvider1D function, final double spot, final double timeToExpiry, final double volatility,
      final double interestRate, final double dividend) {
    ArgumentChecker.notNull(lattice, "lattice");
    ArgumentChecker.notNull(function, "function");
    ArgumentChecker.isTrue(spot > 0., "Spot should be positive");
    ArgumentChecker.isTrue(Doubles.isFinite(spot), "Spot should be finite");
    ArgumentChecker.isTrue(timeToExpiry > 0., "timeToExpiry should be positive");
    ArgumentChecker.isTrue(Doubles.isFinite(timeToExpiry), "timeToExpiry should be finite");
    ArgumentChecker.isTrue(volatility > 0., "volatility should be positive");
    ArgumentChecker.isTrue(Doubles.isFinite(volatility), "volatility should be finite");
    ArgumentChecker.isTrue(Doubles.isFinite(interestRate), "interestRate should be finite");
    ArgumentChecker.isTrue(Doubles.isFinite(dividend), "dividend should be finite");

    final GreekResultCollection collection = new GreekResultCollection();
    final LatticeSpecification modLattice = (lattice instanceof TimeVaryingLatticeSpecification) ? new TrigeorgisLatticeSpecification() : lattice;

    final int nSteps = function.getNumberOfSteps();
    final double strike = function.getStrike();

    final double dt = timeToExpiry / nSteps;
    final double discount = Math.exp(-interestRate * dt);
    final double[] params = modLattice.getParameters(spot, strike, timeToExpiry, volatility, interestRate - dividend, nSteps, dt);
    final double upFactor = params[0];
    final double downFactor = params[1];
    final double upProbability = params[2];
    final double downProbability = params[3];
    final double upOverDown = upFactor / downFactor;
    ArgumentChecker.isTrue(upProbability > 0., "upProbability should be greater than 0.");
    ArgumentChecker.isTrue(upProbability < 1., "upProbability should be smaller than 1.");

    final double assetPrice = spot * Math.pow(downFactor, nSteps);
    double[] values = function.getPayoffAtExpiry(assetPrice, upOverDown);
    final double[] res = new double[4];

    final double[] pForDelta = new double[] {spot * downFactor, spot * upFactor };
    final double[] pForGamma = new double[] {pForDelta[0] * downFactor, pForDelta[0] * upFactor, pForDelta[1] * upFactor };

    for (int i = nSteps - 1; i > -1; --i) {
      values = function.getNextOptionValues(discount, upProbability, downProbability, values, spot, 0., downFactor, upOverDown, i);
      if (i == 2) {
        res[2] = 2. * ((values[2] - values[1]) / (pForGamma[2] - pForGamma[1]) - (values[1] - values[0]) / (pForGamma[1] - pForGamma[0])) / (pForGamma[2] - pForGamma[0]);
        res[3] = values[1];
      }
      if (i == 1) {
        res[1] = (values[1] - values[0]) / (pForDelta[1] - pForDelta[0]);
      }
    }
    res[0] = values[0];
    res[3] = modLattice.getTheta(spot, volatility, interestRate, dividend, dt, res);

    collection.put(Greek.FAIR_PRICE, res[0]);
    collection.put(Greek.DELTA, res[1]);
    collection.put(Greek.GAMMA, res[2]);
    collection.put(Greek.THETA, res[3]);

    return collection;
  }

  /*
   * Array is used for dividend to realize constant cost of carry given by b = r - q
   */
  @Override
  public GreekResultCollection getGreeks(final OptionFunctionProvider1D function, final double spot, final double timeToExpiry, final double[] volatility, final double[] interestRate,
      final double[] dividend) {
    ArgumentChecker.notNull(function, "function");
    ArgumentChecker.notNull(volatility, "volatility");
    ArgumentChecker.notNull(interestRate, "interestRate");
    ArgumentChecker.notNull(dividend, "dividend");

    ArgumentChecker.isTrue(spot > 0., "Spot should be positive");
    ArgumentChecker.isTrue(Doubles.isFinite(spot), "Spot should be finite");
    ArgumentChecker.isTrue(timeToExpiry > 0., "timeToExpiry should be positive");
    ArgumentChecker.isTrue(Doubles.isFinite(timeToExpiry), "timeToExpiry should be finite");

    final TimeVaryingLatticeSpecification vLattice = new TimeVaryingLatticeSpecification();
    final int nSteps = function.getNumberOfSteps();

    ArgumentChecker.isTrue(nSteps == interestRate.length, "Wrong interestRate length");
    ArgumentChecker.isTrue(nSteps == volatility.length, "Wrong volatility length");
    ArgumentChecker.isTrue(nSteps == dividend.length, "Wrong dividend length");

    for (int i = 0; i < nSteps; ++i) {
      ArgumentChecker.isTrue(volatility[i] > 0., "volatility should be positive");
      ArgumentChecker.isTrue(Doubles.isFinite(volatility[i]), "volatility should be finite");
      ArgumentChecker.isTrue(Doubles.isFinite(interestRate[i]), "interestRate should be finite");
      ArgumentChecker.isTrue(Doubles.isFinite(dividend[i]), "dividend should be finite");
    }

    final GreekResultCollection collection = new GreekResultCollection();

    final double[] nu = vLattice.getShiftedDrift(volatility, interestRate, dividend);
    final double spaceStep = vLattice.getSpaceStep(timeToExpiry, volatility, nSteps, nu);
    final double upFactor = Math.exp(spaceStep);
    final double downFactor = Math.exp(-spaceStep);
    final double upOverDown = Math.exp(2. * spaceStep);

    final double[] upProbability = new double[nSteps];
    final double[] downProbability = new double[nSteps];
    final double[] df = new double[nSteps];
    final double[] dt = new double[2];
    for (int i = 0; i < nSteps; ++i) {
      final double[] params = vLattice.getParameters(volatility[i], nu[i], spaceStep);
      upProbability[i] = params[1];
      downProbability[i] = 1. - params[1];
      df[i] = Math.exp(-interestRate[i] * params[0]);
      if (i == 0) {
        dt[0] = params[0];
      }
      if (i == 2) {
        dt[1] = params[1];
      }
      ArgumentChecker.isTrue(upProbability[i] > 0., "upProbability should be greater than 0.");
      ArgumentChecker.isTrue(upProbability[i] < 1., "upProbability should be smaller than 1.");
    }

    final double assetPrice = spot * Math.pow(downFactor, nSteps);
    double[] values = function.getPayoffAtExpiry(assetPrice, upOverDown);
    final double[] res = new double[4];

    final double[] pForDelta = new double[] {spot * downFactor, spot * upFactor };
    final double[] pForGamma = new double[] {pForDelta[0] * downFactor, pForDelta[0] * upFactor, pForDelta[1] * upFactor };

    for (int i = nSteps - 1; i > -1; --i) {
      values = function.getNextOptionValues(df[i], upProbability[i], downProbability[i], values, spot, 0., downFactor, upOverDown, i);
      if (i == 2) {
        res[2] = 2. * ((values[2] - values[1]) / (pForGamma[2] - pForGamma[1]) - (values[1] - values[0]) / (pForGamma[1] - pForGamma[0])) / (pForGamma[2] - pForGamma[0]);
        res[3] = values[1];
      }
      if (i == 1) {
        res[1] = (values[1] - values[0]) / (pForDelta[1] - pForDelta[0]);
      }
    }
    res[0] = values[0];
    res[3] = vLattice.getTheta(dt[0], dt[1], res);

    collection.put(Greek.FAIR_PRICE, res[0]);
    collection.put(Greek.DELTA, res[1]);
    collection.put(Greek.GAMMA, res[2]);
    collection.put(Greek.THETA, res[3]);

    return collection;
  }

  @Override
  public GreekResultCollection getGreeks(final LatticeSpecification lattice, final OptionFunctionProvider1D function, final double spot, final double timeToExpiry,
      final double volatility, final double interestRate, final DividendFunctionProvider dividend) {
    ArgumentChecker.notNull(lattice, "lattice");
    ArgumentChecker.notNull(function, "function");
    ArgumentChecker.notNull(dividend, "dividend");

    ArgumentChecker.isTrue(spot > 0., "Spot should be positive");
    ArgumentChecker.isTrue(Doubles.isFinite(spot), "Spot should be finite");
    ArgumentChecker.isTrue(timeToExpiry > 0., "timeToExpiry should be positive");
    ArgumentChecker.isTrue(Doubles.isFinite(timeToExpiry), "timeToExpiry should be finite");
    ArgumentChecker.isTrue(volatility > 0., "volatility should be positive");
    ArgumentChecker.isTrue(Doubles.isFinite(volatility), "volatility should be finite");
    ArgumentChecker.isTrue(Doubles.isFinite(interestRate), "interestRate should be finite");

    final GreekResultCollection collection = new GreekResultCollection();
    final LatticeSpecification modLattice = (lattice instanceof TimeVaryingLatticeSpecification) ? new TrigeorgisLatticeSpecification() : lattice;

    final int nSteps = function.getNumberOfSteps();
    final double strike = function.getStrike();

    final double dt = timeToExpiry / nSteps;
    ArgumentChecker.isTrue(dividend.checkTimeSteps(dt), "Number of steps is too small");

    final double discount = Math.exp(-interestRate * dt);
    final double[] params = modLattice.getParameters(spot, strike, timeToExpiry, volatility, interestRate, nSteps, dt);
    final double upFactor = params[0];
    final double downFactor = params[1];
    final double upProbability = params[2];
    final double downProbability = params[3];
    final double upOverDown = upFactor / downFactor;
    ArgumentChecker.isTrue(upProbability > 0., "upProbability should be greater than 0.");
    ArgumentChecker.isTrue(upProbability < 1., "upProbability should be smaller than 1.");

    final int[] divSteps = dividend.getDividendSteps(dt);

    double assetPriceBase = dividend.spotModifier(spot, interestRate);
    final double assetPriceTerminal = assetPriceBase * Math.pow(downFactor, nSteps);
    double[] values = function.getPayoffAtExpiry(assetPriceTerminal, upOverDown);

    int counter = 0;
    final int nDivs = dividend.getNumberOfDividends();
    final double[] res = new double[4];

    if (dividend instanceof ProportionalDividendFunctionProvider) {
      for (int i = nSteps - 1; i > -1; --i) {
        for (int k = nDivs - 1 - counter; k > -1; --k) {
          if (i == divSteps[k]) {
            assetPriceBase = dividend.dividendCorrections(assetPriceBase, 0., 0., k);
            ++counter;
          }
        }
        values = function.getNextOptionValues(discount, upProbability, downProbability, values, assetPriceBase, 0., downFactor, upOverDown, i);
        if (i == 2) {
          final double[] pForGamma = dividend.getAssetPricesForGamma(spot, interestRate, divSteps, upFactor, downFactor, 0.);
          res[2] = 2. * ((values[2] - values[1]) / (pForGamma[2] - pForGamma[1]) - (values[1] - values[0]) / (pForGamma[1] - pForGamma[0])) / (pForGamma[2] - pForGamma[0]);
          res[3] = values[1];
        }
        if (i == 1) {
          final double[] pForDelta = dividend.getAssetPricesForDelta(spot, interestRate, divSteps, upFactor, downFactor, 0.);
          res[1] = (values[1] - values[0]) / (pForDelta[1] - pForDelta[0]);
        }
      }
    } else {
      double sumDiscountDiv = 0.;
      for (int i = nSteps - 1; i > -1; --i) {
        sumDiscountDiv *= Math.exp(-interestRate * dt);
        for (int k = nDivs - 1 - counter; k > -1; --k) {
          if (i == divSteps[k]) {
            sumDiscountDiv = dividend.dividendCorrections(sumDiscountDiv, interestRate, dt * i, k);
            ++counter;
          }
        }
        values = function.getNextOptionValues(discount, upProbability, downProbability, values, assetPriceBase, sumDiscountDiv, downFactor, upOverDown, i);
        if (i == 2) {
          final double[] pForGamma = dividend.getAssetPricesForGamma(assetPriceBase, interestRate, divSteps, upFactor, downFactor, sumDiscountDiv);
          res[2] = 2. * ((values[2] - values[1]) / (pForGamma[2] - pForGamma[1]) - (values[1] - values[0]) / (pForGamma[1] - pForGamma[0])) / (pForGamma[2] - pForGamma[0]);
          res[3] = values[1];
        }
        if (i == 1) {
          final double[] pForDelta = dividend.getAssetPricesForDelta(assetPriceBase, interestRate, divSteps, upFactor, downFactor, sumDiscountDiv);
          res[1] = (values[1] - values[0]) / (pForDelta[1] - pForDelta[0]);
        }
      }
    }

    res[0] = values[0];
    res[3] = modLattice.getTheta(spot, volatility, interestRate, 0., dt, res);
    collection.put(Greek.FAIR_PRICE, res[0]);
    collection.put(Greek.DELTA, res[1]);
    collection.put(Greek.GAMMA, res[2]);
    collection.put(Greek.THETA, res[3]);

    return collection;
  }

  @Override
  public double[] getGreeks(final OptionFunctionProvider2D function, final double spot1, final double spot2, final double timeToExpiry, final double volatility1,
      final double volatility2,
      final double correlation, final double interestRate, final double dividend1, final double dividend2) {
    ArgumentChecker.notNull(function, "function");

    ArgumentChecker.isTrue(spot1 > 0., "spot1 should be positive");
    ArgumentChecker.isTrue(Doubles.isFinite(spot1), "spot1 should be finite");
    ArgumentChecker.isTrue(spot2 > 0., "spot2 should be positive");
    ArgumentChecker.isTrue(Doubles.isFinite(spot2), "spot2 should be finite");
    ArgumentChecker.isTrue(timeToExpiry > 0., "timeToExpiry should be positive");
    ArgumentChecker.isTrue(Doubles.isFinite(timeToExpiry), "timeToExpiry should be finite");
    ArgumentChecker.isTrue(volatility1 > 0., "volatility1 should be positive");
    ArgumentChecker.isTrue(Doubles.isFinite(volatility1), "volatility1 should be finite");
    ArgumentChecker.isTrue(volatility2 > 0., "volatility2 should be positive");
    ArgumentChecker.isTrue(Doubles.isFinite(volatility2), "volatility2 should be finite");
    ArgumentChecker.isTrue(correlation >= -1. && correlation <= 1., "correlation should be -1. <= rho <= 1.");
    ArgumentChecker.isTrue(Doubles.isFinite(interestRate), "interestRate should be finite");
    ArgumentChecker.isTrue(Doubles.isFinite(dividend1), "dividend1 should be finite");
    ArgumentChecker.isTrue(Doubles.isFinite(dividend2), "dividend2 should be finite");

    final int nSteps = function.getNumberOfSteps();
    final double[] res = new double[7];

    final double vol12 = volatility1 * volatility2;
    final double vol11 = volatility1 * volatility1;
    final double vol22 = volatility2 * volatility2;

    final double dt = timeToExpiry / nSteps;
    final double discount = Math.exp(-interestRate * dt);
    final double rootDt = Math.sqrt(dt);
    final double dx1 = volatility1 * rootDt;
    final double dx2 = volatility2 * rootDt;

    final double dx12 = dx1 * dx2;
    final double nu1Factored = (interestRate - dividend1 - 0.5 * vol11) * dx2 * dt;
    final double nu2Factored = (interestRate - dividend2 - 0.5 * vol22) * dx1 * dt;
    final double vol12Factored = vol12 * correlation * dt;

    final double uuProbability = 0.25 * (dx12 + nu1Factored + nu2Factored + vol12Factored) / dx12;
    final double udProbability = 0.25 * (dx12 + nu1Factored - nu2Factored - vol12Factored) / dx12;
    final double duProbability = 0.25 * (dx12 - nu1Factored + nu2Factored - vol12Factored) / dx12;
    final double ddProbability = 0.25 * (dx12 - nu1Factored - nu2Factored + vol12Factored) / dx12;
    ArgumentChecker.isTrue(uuProbability > 0. && uuProbability < 1., "uuProbability should be 0 < p < 1.");
    ArgumentChecker.isTrue(udProbability > 0. && udProbability < 1., "udProbability should be 0 < p < 1.");
    ArgumentChecker.isTrue(duProbability > 0. && duProbability < 1., "duProbability should be 0 < p < 1.");
    ArgumentChecker.isTrue(ddProbability > 0. && ddProbability < 1., "ddProbability should be 0 < p < 1.");

    final double downFactor1 = Math.exp(-dx1);
    final double downFactor2 = Math.exp(-dx2);
    final double upOverDown1 = Math.exp(2. * dx1);
    final double upOverDown2 = Math.exp(2. * dx2);

    final double assetPrice1 = spot1 * Math.pow(downFactor1, nSteps);
    final double assetPrice2 = spot2 * Math.pow(downFactor2, nSteps);
    final double[] pForDelta1 = new double[] {spot1 * downFactor1, spot1 / downFactor1 };
    final double[] pForDelta2 = new double[] {spot2 * downFactor2, spot2 / downFactor2 };
    final double[] pForGamma1 = new double[] {pForDelta1[0] * downFactor1, spot1, pForDelta1[1] / downFactor1 };
    final double[] pForGamma2 = new double[] {pForDelta2[0] * downFactor2, spot2, pForDelta2[1] / downFactor2 };
    double[][] values = function.getPayoffAtExpiry(assetPrice1, assetPrice2, upOverDown1, upOverDown2);
    for (int i = nSteps - 1; i > -1; --i) {
      values = function.getNextOptionValues(discount, uuProbability, udProbability, duProbability, ddProbability, values, spot1, spot2, downFactor1, downFactor2, upOverDown1, upOverDown2, i);
      if (i == 2) {
        final double valDiff1huu = values[2][2] - values[1][2];
        final double valDiff1luu = values[1][2] - values[0][2];
        final double valDiff1hud = values[2][1] - values[1][1];
        final double valDiff1lud = values[1][1] - values[0][1];
        final double valDiff1hdd = values[2][0] - values[1][0];
        final double valDiff1ldd = values[1][0] - values[0][0];
        final double valDiff2huu = values[2][2] - values[2][1];
        final double valDiff2luu = values[2][1] - values[2][0];
        final double valDiff2hud = values[1][2] - values[1][1];
        final double valDiff2lud = values[1][1] - values[1][0];
        final double valDiff2hdd = values[0][2] - values[0][1];
        final double valDiff2ldd = values[0][1] - values[0][0];
        final double diff1h = pForGamma1[2] - pForGamma1[1];
        final double diff1l = pForGamma1[1] - pForGamma1[0];
        final double diff1 = pForGamma1[2] - pForGamma1[0];
        final double diff2h = pForGamma2[2] - pForGamma2[1];
        final double diff2l = pForGamma2[1] - pForGamma2[0];
        final double diff2 = pForGamma2[2] - pForGamma2[0];
        res[3] = values[1][1];
        res[4] = 2. * ((valDiff1huu + valDiff1hud + valDiff1hdd) / diff1h - (valDiff1luu + valDiff1lud + valDiff1ldd) / diff1l) / diff1 / 3.;
        res[5] = 2. * ((valDiff2huu + valDiff2hud + valDiff2hdd) / diff2h - (valDiff2luu + valDiff2lud + valDiff2ldd) / diff2l) / diff2 / 3.;
      }
      if (i == 1) {
        final double muudu = values[1][1] - values[0][1];
        final double muddd = values[1][0] - values[0][0];
        final double muuud = values[1][1] - values[1][0];
        final double mdudd = values[0][1] - values[0][0];
        final double diff1 = pForDelta1[1] - pForDelta1[0];
        final double diff2 = pForDelta2[1] - pForDelta2[0];
        res[1] = 0.5 * (muudu + muddd) / diff1;
        res[2] = 0.5 * (muuud + mdudd) / diff2;
        res[6] = (muudu - muddd) / diff1 / diff2;
      }
    }
    res[3] = 0.5 * (res[3] - values[0][0]) / dt;
    res[0] = values[0][0];

    return res;
  }

  /*
   *
   *
   * *********************************
   * Old methods below, removed later
   * *********************************
   *
   *
   */

  public double getEuropeanPrice(final LatticeSpecification lattice, final double spot, final double strike, final double timeToExpiry, final double volatility, final double interestRate,
      final int nSteps, final boolean isCall) {
    return getEuropeanPrice(lattice, spot, strike, timeToExpiry, volatility, interestRate, 0., nSteps, isCall);
  }

  public double getEuropeanPrice(final LatticeSpecification lattice, final double spot, final double strike, final double timeToExpiry, final double volatility, final double interestRate,
      final double dividend, final int nSteps, final boolean isCall) {
    final LatticeSpecification modLattice = (lattice instanceof TimeVaryingLatticeSpecification) ? new TrigeorgisLatticeSpecification() : lattice;

    final double dt = timeToExpiry / nSteps;
    final double discount = Math.exp(-interestRate * dt);
    final double[] params = modLattice.getParameters(spot, strike, timeToExpiry, volatility, interestRate - dividend, nSteps, dt);
    final double upFactor = params[0];
    final double downFactor = params[1];
    final double upProbability = params[2];
    final double downProbability = params[3];
    final double upOverDown = upFactor / downFactor;
    final double sig = isCall ? 1. : -1.;

    final double assetPrice = spot * Math.pow(downFactor, nSteps);
    double[] values = _function.getPayoffAtExpiry(assetPrice, strike, nSteps, sig, upOverDown);
    for (int i = nSteps - 1; i > -1; --i) {
      //      for (int j = 0; j < i + 1; ++j) {
      //        values[j] = discount * (upProbability * values[j + 1] + downProbability * values[j]);
      //      }
      values = _function.getNextOptionValues(discount, upProbability, downProbability, values, spot, downFactor, upOverDown, i);
    }

    return values[0];
  }

  public double getEuropeanPriceProportionalDividends(final LatticeSpecification lattice, final double spot, final double strike, final double timeToExpiry, final double volatility,
      final double interestRate, final double[] dividendTimes, final double[] dividends, final int nSteps, final boolean isCall) {
    final LatticeSpecification modLattice = (lattice instanceof TimeVaryingLatticeSpecification) ? new TrigeorgisLatticeSpecification() : lattice;

    final double dt = timeToExpiry / nSteps;
    final double discount = Math.exp(-interestRate * dt);
    final double[] params = modLattice.getParameters(spot, strike, timeToExpiry, volatility, interestRate, nSteps, dt);
    final double upFactor = params[0];
    final double downFactor = params[1];
    final double upProbability = params[2];
    final double downProbability = params[3];
    final double upOverDown = upFactor / downFactor;
    final double sig = isCall ? 1. : -1.;

    final int nDivs = dividendTimes.length;
    double assetPrice = spot * Math.pow(downFactor, nSteps);
    for (int i = 0; i < nDivs; ++i) {
      assetPrice *= (1. - dividends[i]);
    }
    final double[] values = _function.getPayoffAtExpiry(assetPrice, strike, nSteps, sig, upOverDown);

    for (int i = nSteps - 1; i > -1; --i) {
      for (int j = 0; j < i + 1; ++j) {
        values[j] = discount * (upProbability * values[j + 1] + downProbability * values[j]);
      }
    }

    return values[0];
  }

  public double getEuropeanPriceCashDividends(final LatticeSpecification lattice, final double spot, final double strike, final double timeToExpiry, final double volatility,
      final double interestRate, final double[] dividendTimes, final double[] dividends, final int nSteps, final boolean isCall) {
    final LatticeSpecification modLattice = (lattice instanceof TimeVaryingLatticeSpecification) ? new TrigeorgisLatticeSpecification() : lattice;

    final double dt = timeToExpiry / nSteps;
    final double discount = Math.exp(-interestRate * dt);
    final double[] params = modLattice.getParameters(spot, strike, timeToExpiry, volatility, interestRate, nSteps, dt);
    final double upFactor = params[0];
    final double downFactor = params[1];
    final double upProbability = params[2];
    final double downProbability = params[3];
    final double upOverDown = upFactor / downFactor;
    final double sig = isCall ? 1. : -1.;

    final int nDivs = dividendTimes.length;
    double assetPrice = spot;
    for (int i = 0; i < nDivs; ++i) {
      assetPrice -= dividends[i] * Math.exp(-interestRate * dividendTimes[i]);
    }
    assetPrice *= Math.pow(downFactor, nSteps);
    final double[] values = _function.getPayoffAtExpiry(assetPrice, strike, nSteps, sig, upOverDown);
    for (int i = nSteps - 1; i > -1; --i) {
      for (int j = 0; j < i + 1; ++j) {
        values[j] = discount * (upProbability * values[j + 1] + downProbability * values[j]);
      }
    }

    return values[0];
  }

  public double getEuropeanPrice(final LatticeSpecification lattice, final double spot, final double strike, final double timeToExpiry, final double[] volatility,
      final double[] interestRate, final int nSteps, final boolean isCall) {
    Validate.isTrue(lattice instanceof TimeVaryingLatticeSpecification);
    final TimeVaryingLatticeSpecification vLattice = (TimeVaryingLatticeSpecification) lattice;

    ArgumentChecker.isTrue(nSteps == interestRate.length, "Wrong interestRate length");
    ArgumentChecker.isTrue(nSteps == volatility.length, "Wrong volatility length");

    final double[] nu = vLattice.getShiftedDrift(volatility, interestRate);
    final double spaceStep = vLattice.getSpaceStep(timeToExpiry, volatility, nSteps, nu);
    final double downFactor = Math.exp(-spaceStep);
    final double upOverDown = Math.exp(2. * spaceStep);

    final double[] upProbability = new double[nSteps];
    final double[] downProbability = new double[nSteps];
    final double[] df = new double[nSteps];
    for (int i = 0; i < nSteps; ++i) {
      final double[] params = vLattice.getParameters(volatility[i], nu[i], spaceStep);
      upProbability[i] = params[1];
      downProbability[i] = 1. - params[1];
      df[i] = Math.exp(-interestRate[i] * params[0]);
    }
    final double sig = isCall ? 1. : -1.;

    final double assetPrice = spot * Math.pow(downFactor, nSteps);
    double[] values = _function.getPayoffAtExpiry(assetPrice, strike, nSteps, sig, upOverDown);
    for (int i = nSteps - 1; i > -1; --i) {
      //      for (int j = 0; j < i + 1; ++j) {
      //        values[j] = df[i] * (upProbability[i] * values[j + 1] + downProbability[i] * values[j]);
      //      }
      values = _function.getNextOptionValues(df[i], upProbability[i], downProbability[i], values, spot, downFactor, upOverDown, i);
    }

    return values[0];
  }

  public double[] getEuropeanGreeks(final LatticeSpecification lattice, final double spot, final double strike, final double timeToExpiry, final double volatility, final double interestRate,
      final int nSteps, final boolean isCall) {
    return getEuropeanGreeks(lattice, spot, strike, timeToExpiry, volatility, interestRate, 0., nSteps, isCall);
  }

  public double[] getEuropeanGreeks(final LatticeSpecification lattice, final double spot, final double strike, final double timeToExpiry, final double volatility, final double interestRate,
      final double dividend, final int nSteps, final boolean isCall) {
    final LatticeSpecification modLattice = (lattice instanceof TimeVaryingLatticeSpecification) ? new TrigeorgisLatticeSpecification() : lattice;

    final double dt = timeToExpiry / nSteps;
    final double discount = Math.exp(-interestRate * dt);
    final double[] params = modLattice.getParameters(spot, strike, timeToExpiry, volatility, interestRate - dividend, nSteps, dt);
    final double upFactor = params[0];
    final double downFactor = params[1];
    final double upProbability = params[2];
    final double downProbability = params[3];
    final double upOverDown = upFactor / downFactor;
    final double sig = isCall ? 1. : -1.;

    final double assetPrice = spot * Math.pow(downFactor, nSteps);
    double[] values = _function.getPayoffAtExpiry(assetPrice, strike, nSteps, sig, upOverDown);
    final double[] res = new double[4];

    final double[] pForDelta = new double[] {spot * downFactor, spot * upFactor };
    final double[] pForGamma = new double[] {pForDelta[0] * downFactor, pForDelta[0] * upFactor, pForDelta[1] * upFactor };

    for (int i = nSteps - 1; i > -1; --i) {
      //      for (int j = 0; j < i + 1; ++j) {
      //        values[j] = discount * (upProbability * values[j + 1] + downProbability * values[j]);
      //      }
      values = _function.getNextOptionValues(discount, upProbability, downProbability, values, spot, downFactor, upOverDown, i);
      if (i == 2) {
        res[2] = 2. * ((values[2] - values[1]) / (pForGamma[2] - pForGamma[1]) - (values[1] - values[0]) / (pForGamma[1] - pForGamma[0])) / (pForGamma[2] - pForGamma[0]);
        res[3] = values[1];
      }
      if (i == 1) {
        res[1] = (values[1] - values[0]) / (pForDelta[1] - pForDelta[0]);
      }
    }
    res[0] = values[0];
    res[3] = modLattice.getTheta(spot, volatility, interestRate, dividend, dt, res);
    return res;
  }

  public double getAmericanPrice(final LatticeSpecification lattice, final double spot, final double strike, final double timeToExpiry, final double volatility, final double interestRate,
      final int nSteps, final boolean isCall) {
    return getAmericanPrice(lattice, spot, strike, timeToExpiry, volatility, interestRate, 0., nSteps, isCall);
  }

  public double getAmericanPrice(final LatticeSpecification lattice, final double spot, final double strike, final double timeToExpiry, final double volatility, final double interestRate,
      final double dividend, final int nSteps, final boolean isCall) {
    //    if (isCall && dividend == 0. && interestRate >= 0.) {
    //      return getEuropeanPrice(lattice, spot, strike, timeToExpiry, volatility, interestRate, dividend, nSteps, true);
    //    } else {
    final LatticeSpecification modLattice = (lattice instanceof TimeVaryingLatticeSpecification) ? new TrigeorgisLatticeSpecification() : lattice;

    final double dt = timeToExpiry / nSteps;
    final double discount = Math.exp(-interestRate * dt);
    final double[] params = modLattice.getParameters(spot, strike, timeToExpiry, volatility, interestRate - dividend, nSteps, dt);
    final double upFactor = params[0];
    final double downFactor = params[1];
    final double upProbability = params[2];
    final double downProbability = params[3];
    final double upOverDown = upFactor / downFactor;
    final double sig = isCall ? 1. : -1.;

    final double assetPrice = spot * Math.pow(downFactor, nSteps);
    double[] values = _function.getPayoffAtExpiry(assetPrice, strike, nSteps, sig, upOverDown);

    for (int i = nSteps - 1; i > -1; --i) {
      //        assetPrice = spot * Math.pow(downFactor, i);
      //        for (int j = 0; j < i + 1; ++j) {
      //          values[j] = Math.max(discount * (upProbability * values[j + 1] + downProbability * values[j]), strike - assetPrice);
      //          assetPrice *= upOverDown;
      //        }
      values = _function.getNextOptionValues(discount, upProbability, downProbability, values, spot, strike, sig, downFactor, upOverDown, i);
    }

    return values[0];
    //    }
  }

  public double getAmericanPrice(final LatticeSpecification lattice, final double spot, final double strike, final double timeToExpiry, final double[] volatility,
      final double[] interestRate, final int nSteps, final boolean isCall) {
    if (isCall) {
      return getEuropeanPrice(lattice, spot, strike, timeToExpiry, volatility, interestRate, nSteps, true);
    }
    Validate.isTrue(lattice instanceof TimeVaryingLatticeSpecification);
    final TimeVaryingLatticeSpecification vLattice = (TimeVaryingLatticeSpecification) lattice;

    ArgumentChecker.isTrue(nSteps == interestRate.length, "Wrong interestRate length");
    ArgumentChecker.isTrue(nSteps == volatility.length, "Wrong volatility length");

    final double[] nu = vLattice.getShiftedDrift(volatility, interestRate);
    final double spaceStep = vLattice.getSpaceStep(timeToExpiry, volatility, nSteps, nu);
    final double downFactor = Math.exp(-spaceStep);
    final double upOverDown = Math.exp(2. * spaceStep);

    final double[] upProbability = new double[nSteps];
    final double[] downProbability = new double[nSteps];
    final double[] df = new double[nSteps];
    for (int i = 0; i < nSteps; ++i) {
      final double[] params = vLattice.getParameters(volatility[i], nu[i], spaceStep);
      upProbability[i] = params[1];
      downProbability[i] = 1. - params[1];
      df[i] = Math.exp(-interestRate[i] * params[0]);
    }
    final double sig = isCall ? 1. : -1.;

    final double assetPrice = spot * Math.pow(downFactor, nSteps);
    double[] values = _function.getPayoffAtExpiry(assetPrice, strike, nSteps, sig, upOverDown);
    for (int i = nSteps - 1; i > -1; --i) {
      //        assetPrice = spot * Math.pow(downFactor, i);
      //        for (int j = 0; j < i + 1; ++j) {
      //          values[j] = Math.max(df[i] * (upProbability[i] * values[j + 1] + downProbability[i] * values[j]), sig * (assetPrice - strike));
      //          assetPrice *= upOverDown;
      //        }
      values = _function.getNextOptionValues(df[i], upProbability[i], downProbability[i], values, spot, strike, sig, downFactor, upOverDown, i);
    }

    return values[0];
  }

  public double getAmericanPriceProportionalDividends(final LatticeSpecification lattice, final double spot, final double strike, final double timeToExpiry, final double volatility,
      final double interestRate, final double[] dividendTimes, final double[] dividends, final int nSteps, final boolean isCall) {
    final LatticeSpecification modLattice = (lattice instanceof TimeVaryingLatticeSpecification) ? new TrigeorgisLatticeSpecification() : lattice;

    final double dt = timeToExpiry / nSteps;
    final double discount = Math.exp(-interestRate * dt);
    final double[] params = modLattice.getParameters(spot, strike, timeToExpiry, volatility, interestRate, nSteps, dt);
    final double upFactor = params[0];
    final double downFactor = params[1];
    final double upProbability = params[2];
    final double downProbability = params[3];
    final double upOverDown = upFactor / downFactor;
    final double sig = isCall ? 1. : -1.;

    final int nDivs = dividendTimes.length;
    final int[] divSteps = new int[nDivs];
    for (int i = 0; i < nDivs; ++i) {
      divSteps[i] = (int) (dividendTimes[i] / dt);
    }

    double assetPriceBase = spot;
    for (int i = 0; i < nDivs; ++i) {
      assetPriceBase *= (1. - dividends[i]);
    }
    assetPriceBase = assetPriceBase * Math.pow(downFactor, nSteps);
    final double[] values = _function.getPayoffAtExpiry(assetPriceBase, strike, nSteps, sig, upOverDown);

    int counter = 0;
    for (int i = nSteps - 1; i > -1; --i) {
      for (int k = nDivs - 1 - counter; k > -1; --k) {
        if (i == divSteps[k]) {
          assetPriceBase /= (1. - dividends[k]);
          ++counter;
        }
      }
      assetPriceBase /= downFactor;
      double assetPrice = assetPriceBase;

      for (int j = 0; j < i + 1; ++j) {
        values[j] = Math.max(discount * (upProbability * values[j + 1] + downProbability * values[j]), sig * (assetPrice - strike));
        assetPrice *= upOverDown;
      }
    }

    return values[0];
  }

  public double getAmericanPriceCashDividends(final LatticeSpecification lattice, final double spot, final double strike, final double timeToExpiry, final double volatility,
      final double interestRate, final double[] dividendTimes, final double[] dividends, final int nSteps, final boolean isCall) {
    final LatticeSpecification modLattice = (lattice instanceof TimeVaryingLatticeSpecification) ? new TrigeorgisLatticeSpecification() : lattice;

    final double dt = timeToExpiry / nSteps;
    final double discount = Math.exp(-interestRate * dt);
    final double[] params = modLattice.getParameters(spot, strike, timeToExpiry, volatility, interestRate, nSteps, dt);
    final double upFactor = params[0];
    final double downFactor = params[1];
    final double upProbability = params[2];
    final double downProbability = params[3];
    final double upOverDown = upFactor / downFactor;
    final double sig = isCall ? 1. : -1.;

    final int nDivs = dividendTimes.length;
    final int[] divSteps = new int[nDivs];
    for (int i = 0; i < nDivs; ++i) {
      divSteps[i] = (int) (dividendTimes[i] / dt);
    }

    double assetPriceBase = spot;
    for (int i = 0; i < nDivs; ++i) {
      assetPriceBase -= dividends[i] * Math.exp(-interestRate * dividendTimes[i]);
    }
    double assetPrice = assetPriceBase * Math.pow(downFactor, nSteps);
    final double[] values = _function.getPayoffAtExpiry(assetPrice, strike, nSteps, sig, upOverDown);

    int counter = 0;
    double sumDiscountDiv = 0.;
    for (int i = nSteps - 1; i > -1; --i) {
      assetPrice = assetPriceBase * Math.pow(downFactor, i);
      sumDiscountDiv *= Math.exp(-interestRate * dt);
      for (int k = nDivs - 1 - counter; k > -1; --k) {
        if (i == divSteps[k]) {
          sumDiscountDiv += dividends[k] * Math.exp(-interestRate * (dividendTimes[k] - dt * i));
          ++counter;
        }
      }
      for (int j = 0; j < i + 1; ++j) {
        values[j] = Math.max(discount * (upProbability * values[j + 1] + downProbability * values[j]), sig * (assetPrice + sumDiscountDiv - strike));
        assetPrice *= upOverDown;
      }
    }

    return values[0];
  }

  public double[] getAmericanGreeks(final LatticeSpecification lattice, final double spot, final double strike, final double timeToExpiry, final double volatility, final double interestRate,
      final int nSteps, final boolean isCall) {
    if (isCall) {
      return getEuropeanGreeks(lattice, spot, strike, timeToExpiry, volatility, interestRate, nSteps, true);
    }
    return getAmericanGreeks(lattice, spot, strike, timeToExpiry, volatility, interestRate, 0., nSteps, false);
  }

  public double[] getAmericanGreeks(final LatticeSpecification lattice, final double spot, final double strike, final double timeToExpiry, final double volatility, final double interestRate,
      final double dividend, final int nSteps, final boolean isCall) {
    final LatticeSpecification modLattice = (lattice instanceof TimeVaryingLatticeSpecification) ? new TrigeorgisLatticeSpecification() : lattice;
    final double[] res = new double[4];

    final double dt = timeToExpiry / nSteps;
    final double discount = Math.exp(-interestRate * dt);
    final double[] params = modLattice.getParameters(spot, strike, timeToExpiry, volatility, interestRate - dividend, nSteps, dt);
    final double upFactor = params[0];
    final double downFactor = params[1];
    final double upProbability = params[2];
    final double downProbability = params[3];
    final double upOverDown = upFactor / downFactor;
    final double sig = isCall ? 1. : -1.;

    final double assetPrice = spot * Math.pow(downFactor, nSteps);
    double[] values = _function.getPayoffAtExpiry(assetPrice, strike, nSteps, sig, upOverDown);

    final double[] pForDelta = new double[] {spot * downFactor, spot * upFactor };
    final double[] pForGamma = new double[] {pForDelta[0] * downFactor, pForDelta[0] * upFactor, pForDelta[1] * upFactor };

    for (int i = nSteps - 1; i > -1; --i) {
      //      assetPrice = spot * Math.pow(downFactor, i);
      //      for (int j = 0; j < i + 1; ++j) {
      //        values[j] = Math.max(discount * (upProbability * values[j + 1] + downProbability * values[j]), sig * (assetPrice - strike));
      //        assetPrice *= upOverDown;
      //      }
      values = _function.getNextOptionValues(discount, upProbability, downProbability, values, spot, strike, sig, downFactor, upOverDown, i);
      if (i == 2) {
        res[2] = 2. * ((values[2] - values[1]) / (pForGamma[2] - pForGamma[1]) - (values[1] - values[0]) / (pForGamma[1] - pForGamma[0])) / (pForGamma[2] - pForGamma[0]);
        res[3] = values[1];
      }
      if (i == 1) {
        res[1] = (values[1] - values[0]) / (pForDelta[1] - pForDelta[0]);
      }
    }
    res[0] = values[0];
    res[3] = modLattice.getTheta(spot, volatility, interestRate, dividend, dt, res);

    return res;
  }

  public double getEuropeanSpreadPrice(final double spot1, final double spot2, final double strike, final double timeToExpiry, final double volatility1, final double volatility2,
      final double correlation, final double interestRate, final double dividend1, final double dividend2, final int nSteps, final boolean isCall) {
    ArgumentChecker.isTrue(_function instanceof VanillaPayoff, "Barrier option is not supported");
    final VanillaPayoff function = (VanillaPayoff) _function;

    final double vol12 = volatility1 * volatility2;
    final double vol11 = volatility1 * volatility1;
    final double vol22 = volatility2 * volatility2;

    final double dt = timeToExpiry / nSteps;
    final double discount = Math.exp(-interestRate * dt);
    final double rootDt = Math.sqrt(dt);
    final double dx1 = volatility1 * rootDt;
    final double dx2 = volatility2 * rootDt;

    final double dx12 = dx1 * dx2;
    final double nu1Factored = (interestRate - dividend1 - 0.5 * vol11) * dx2 * dt;
    final double nu2Factored = (interestRate - dividend2 - 0.5 * vol22) * dx1 * dt;
    final double vol12Factored = vol12 * correlation * dt;

    final double uuProbability = 0.25 * (dx12 + nu1Factored + nu2Factored + vol12Factored) / dx12;
    final double udProbability = 0.25 * (dx12 + nu1Factored - nu2Factored - vol12Factored) / dx12;
    final double duProbability = 0.25 * (dx12 - nu1Factored + nu2Factored - vol12Factored) / dx12;
    final double ddProbability = 0.25 * (dx12 - nu1Factored - nu2Factored + vol12Factored) / dx12;

    final double downFactor1 = Math.exp(-dx1);
    final double downFactor2 = Math.exp(-dx2);
    final double upOverDown1 = Math.exp(2. * dx1);
    final double upOverDown2 = Math.exp(2. * dx2);

    final double sign = isCall ? 1. : -1.;

    final double assetPrice1 = spot1 * Math.pow(downFactor1, nSteps);
    final double assetPrice2 = spot2 * Math.pow(downFactor2, nSteps);
    double[][] values = function.getPayoffAtExpiry(assetPrice1, assetPrice2, strike, nSteps, sign, upOverDown1, upOverDown2);
    for (int i = nSteps - 1; i > -1; --i) {
      values = function.getNextOptionValues(discount, uuProbability, udProbability, duProbability, ddProbability, values, i);
    }

    return values[0][0];
  }

  public double getAmericanSpreadPrice(final double spot1, final double spot2, final double strike, final double timeToExpiry, final double volatility1, final double volatility2,
      final double correlation, final double interestRate, final double dividend1, final double dividend2, final int nSteps, final boolean isCall) {
    ArgumentChecker.isTrue(_function instanceof VanillaPayoff, "Barrier option is not supported");
    final VanillaPayoff function = (VanillaPayoff) _function;

    final double vol12 = volatility1 * volatility2;
    final double vol11 = volatility1 * volatility1;
    final double vol22 = volatility2 * volatility2;

    final double dt = timeToExpiry / nSteps;
    final double discount = Math.exp(-interestRate * dt);
    final double rootDt = Math.sqrt(dt);
    final double dx1 = volatility1 * rootDt;
    final double dx2 = volatility2 * rootDt;

    final double dx12 = dx1 * dx2;
    final double nu1Factored = (interestRate - dividend1 - 0.5 * vol11) * dx2 * dt;
    final double nu2Factored = (interestRate - dividend2 - 0.5 * vol22) * dx1 * dt;
    final double vol12Factored = vol12 * correlation * dt;

    final double uuProbability = 0.25 * (dx12 + nu1Factored + nu2Factored + vol12Factored) / dx12;
    final double udProbability = 0.25 * (dx12 + nu1Factored - nu2Factored - vol12Factored) / dx12;
    final double duProbability = 0.25 * (dx12 - nu1Factored + nu2Factored - vol12Factored) / dx12;
    final double ddProbability = 0.25 * (dx12 - nu1Factored - nu2Factored + vol12Factored) / dx12;

    final double downFactor1 = Math.exp(-dx1);
    final double downFactor2 = Math.exp(-dx2);
    final double upOverDown1 = Math.exp(2. * dx1);
    final double upOverDown2 = Math.exp(2. * dx2);

    final double sign = isCall ? 1. : -1.;

    final double assetPrice1 = spot1 * Math.pow(downFactor1, nSteps);
    final double assetPrice2 = spot2 * Math.pow(downFactor2, nSteps);
    double[][] values = function.getPayoffAtExpiry(assetPrice1, assetPrice2, strike, nSteps, sign, upOverDown1, upOverDown2);
    for (int i = nSteps - 1; i > -1; --i) {
      values = function.getNextOptionValues(discount, strike, uuProbability, udProbability, duProbability, ddProbability, values, spot1, spot2, sign, downFactor1, downFactor2, upOverDown1,
          upOverDown2, i);
    }

    return values[0][0];
  }

  private abstract class PayoffFunction {
    public abstract double[] getPayoffAtExpiry(final double assetPrice, final double strike, final int nSteps, final double sig, final double upOverDown);

    public abstract double[] getPayoffAtExpiry(final StandardOptionDataBundle data, final OptionPayoffFunction<StandardOptionDataBundle> payoffFunction, final double assetPrice, final double strike,
        final int nSteps, final double sig,
        final double upOverDown);

    public abstract double[] getNextOptionValues(final double discount, final double upProbability, final double downProbability, final double[] values, final double baseAssetPrice,
        final double downFactor, final double upOverDown, final int steps);

    public abstract double[] getNextOptionValues(final double discount, final double upProbability, final double downProbability, final double[] values, final double baseAssetPrice,
        final double strike, final double sign, final double downFactor, final double upOverDown, final int steps);
  }

  @SuppressWarnings("synthetic-access")
  private class VanillaPayoff extends PayoffFunction {
    @Override
    public double[] getPayoffAtExpiry(final double assetPrice, final double strike, final int nSteps, final double sig, final double upOverDown) {
      final double[] values = new double[nSteps + 1];
      double priceTmp = assetPrice;
      for (int i = 0; i < nSteps + 1; ++i) {
        values[i] = Math.max(sig * (priceTmp - strike), 0);
        priceTmp *= upOverDown;
      }
      return values;
    }

    @Override
    public double[] getPayoffAtExpiry(final StandardOptionDataBundle data, final OptionPayoffFunction<StandardOptionDataBundle> payoffFunction, final double assetPrice, final double strike,
        final int nSteps, final double sig, final double upOverDown) {
      final double[] values = new double[nSteps + 1];
      double priceTmp = assetPrice;
      for (int i = 0; i < nSteps + 1; ++i) {
        final StandardOptionDataBundle dataAtExpiry = data.withSpot(priceTmp);
        values[i] = payoffFunction.getPayoff(dataAtExpiry, 0.);
        priceTmp *= upOverDown;
      }
      return values;
    }

    @Override
    public double[] getNextOptionValues(final double discount, final double upProbability, final double downProbability, final double[] values, final double baseAssetPrice, final double downFactor,
        final double upOverDown, final int steps) {
      final double[] res = new double[steps + 1];
      for (int j = 0; j < steps + 1; ++j) {
        res[j] = discount * (upProbability * values[j + 1] + downProbability * values[j]);
      }
      return res;
    }

    @Override
    public double[] getNextOptionValues(final double discount, final double upProbability, final double downProbability, final double[] values, final double baseAssetPrice, final double strike,
        final double sign, final double downFactor, final double upOverDown, final int steps) {
      final double[] res = new double[steps + 1];
      double assetPrice = baseAssetPrice * Math.pow(downFactor, steps);
      for (int j = 0; j < steps + 1; ++j) {
        res[j] = Math.max(discount * (upProbability * values[j + 1] + downProbability * values[j]), sign * (assetPrice - strike));
        assetPrice *= upOverDown;
      }
      return res;
    }

    /*
     * Functions for two-dimensional case
     */
    public double[][] getPayoffAtExpiry(final double assetPrice1, final double assetPrice2, final double strike, final int nSteps, final double sign, final double upOverDown1,
        final double upOverDown2) {
      final int nStepsP = nSteps + 1;
      final double[][] values = new double[nStepsP][nStepsP];
      double priceTmp1 = assetPrice1;
      for (int i = 0; i < nStepsP; ++i) {
        double priceTmp2 = assetPrice2;
        for (int j = 0; j < nStepsP; ++j) {
          values[i][j] = Math.max(sign * (priceTmp1 - priceTmp2 - strike), 0);
          priceTmp2 *= upOverDown2;
        }
        priceTmp1 *= upOverDown1;
      }
      return values;
    }

    public double[][] getNextOptionValues(final double discount, final double uuProbability, final double udProbability, final double duProbability, final double ddProbability,
        final double[][] values, final int steps) {
      final int stepsP = steps + 1;
      final double[][] res = new double[stepsP][stepsP];
      for (int j = 0; j < stepsP; ++j) {
        for (int i = 0; i < stepsP; ++i) {
          res[j][i] = discount * (uuProbability * values[j + 1][i + 1] + udProbability * values[j + 1][i] + duProbability * values[j][i + 1] + ddProbability * values[j][i]);
        }
      }
      return res;
    }

    public double[][] getNextOptionValues(final double discount, final double strike, final double uuProbability, final double udProbability, final double duProbability, final double ddProbability,
        final double[][] values, final double baseAssetPrice1, final double baseAssetPrice2, final double sign, final double downFactor1, final double downFactor2,
        final double upOverDown1, final double upOverDown2, final int steps) {
      final int stepsP = steps + 1;
      final double[][] res = new double[stepsP][stepsP];
      double assetPrice1 = baseAssetPrice1 * Math.pow(downFactor1, steps);
      for (int j = 0; j < stepsP; ++j) {
        double assetPrice2 = baseAssetPrice2 * Math.pow(downFactor2, steps);
        for (int i = 0; i < stepsP; ++i) {
          res[j][i] = discount * (uuProbability * values[j + 1][i + 1] + udProbability * values[j + 1][i] + duProbability * values[j][i + 1] + ddProbability * values[j][i]);
          res[j][i] = Math.max(res[j][i], sign * (assetPrice1 - assetPrice2 - strike));
          assetPrice2 *= upOverDown2;
        }
        assetPrice1 *= upOverDown1;
      }
      return res;
    }
  }

  @SuppressWarnings("synthetic-access")
  private class DownAndOutPayoff extends PayoffFunction {
    @Override
    public double[] getPayoffAtExpiry(final double assetPrice, final double strike, final int nSteps, final double sig, final double upOverDown) {
      final double[] values = new double[nSteps + 1];
      double priceTmp = assetPrice;
      for (int i = 0; i < nSteps + 1; ++i) {
        values[i] = priceTmp <= _barrier ? 0. : Math.max(sig * (priceTmp - strike), 0);
        priceTmp *= upOverDown;
      }
      return values;
    }

    @Override
    public double[] getPayoffAtExpiry(final StandardOptionDataBundle data, final OptionPayoffFunction<StandardOptionDataBundle> payoffFunction, final double assetPrice, final double strike,
        final int nSteps, final double sig,
        final double upOverDown) {

      return null;
    }

    @Override
    public double[] getNextOptionValues(final double discount, final double upProbability, final double downProbability, final double[] values, final double baseAssetPrice, final double downFactor,
        final double upOverDown, final int steps) {
      final double[] res = new double[steps + 1];
      double assetPrice = baseAssetPrice * Math.pow(downFactor, steps);
      for (int j = 0; j < steps + 1; ++j) {
        res[j] = (assetPrice <= _barrier) ? 0. : discount * (upProbability * values[j + 1] + downProbability * values[j]);
        assetPrice *= upOverDown;
      }
      return res;
    }

    @Override
    public double[] getNextOptionValues(final double discount, final double upProbability, final double downProbability, final double[] values, final double baseAssetPrice, final double strike,
        final double sign, final double downFactor, final double upOverDown, final int steps) {
      final double[] res = new double[steps + 1];
      double assetPrice = baseAssetPrice * Math.pow(downFactor, steps);
      for (int j = 0; j < steps + 1; ++j) {
        res[j] = (assetPrice <= _barrier) ? 0. : Math.max(discount * (upProbability * values[j + 1] + downProbability * values[j]), sign * (assetPrice - strike));
        assetPrice *= upOverDown;
      }
      return res;
    }
  }

  @SuppressWarnings("synthetic-access")
  private class UpAndOutPayoff extends PayoffFunction {
    @Override
    public double[] getPayoffAtExpiry(final double assetPrice, final double strike, final int nSteps, final double sig, final double upOverDown) {
      final double[] values = new double[nSteps + 1];
      double priceTmp = assetPrice;
      for (int i = 0; i < nSteps + 1; ++i) {
        values[i] = (priceTmp >= _barrier) ? 0. : Math.max(sig * (priceTmp - strike), 0);
        priceTmp *= upOverDown;
      }
      return values;
    }

    @Override
    public double[] getPayoffAtExpiry(final StandardOptionDataBundle data, final OptionPayoffFunction<StandardOptionDataBundle> payoffFunction, final double assetPrice, final double strike,
        final int nSteps, final double sig,
        final double upOverDown) {

      return null;
    }

    @Override
    public double[] getNextOptionValues(final double discount, final double upProbability, final double downProbability, final double[] values, final double baseAssetPrice, final double downFactor,
        final double upOverDown, final int steps) {
      final double[] res = new double[steps + 1];
      double assetPrice = baseAssetPrice * Math.pow(downFactor, steps);
      for (int j = 0; j < steps + 1; ++j) {
        res[j] = assetPrice >= _barrier ? 0. : discount * (upProbability * values[j + 1] + downProbability * values[j]);
        assetPrice *= upOverDown;
      }
      return res;
    }

    @Override
    public double[] getNextOptionValues(final double discount, final double upProbability, final double downProbability, final double[] values, final double baseAssetPrice, final double strike,
        final double sign, final double downFactor, final double upOverDown, final int steps) {
      final double[] res = new double[steps + 1];
      double assetPrice = baseAssetPrice * Math.pow(downFactor, steps);
      for (int j = 0; j < steps + 1; ++j) {
        res[j] = assetPrice >= _barrier ? 0. : Math.max(discount * (upProbability * values[j + 1] + downProbability * values[j]), sign * (assetPrice - strike));
        assetPrice *= upOverDown;
      }
      return res;
    }
  }

}
