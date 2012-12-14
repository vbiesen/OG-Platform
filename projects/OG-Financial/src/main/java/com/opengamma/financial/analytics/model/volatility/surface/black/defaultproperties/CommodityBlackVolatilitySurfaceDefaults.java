/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.financial.analytics.model.volatility.surface.black.defaultproperties;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.opengamma.engine.ComputationTarget;
import com.opengamma.engine.ComputationTargetType;
import com.opengamma.engine.function.FunctionCompilationContext;
import com.opengamma.engine.value.ValuePropertyNames;
import com.opengamma.engine.value.ValueRequirement;
import com.opengamma.financial.analytics.OpenGammaFunctionExclusions;
import com.opengamma.financial.property.DefaultPropertyFunction;
import com.opengamma.util.ArgumentChecker;

/**
 *
 */
public abstract class CommodityBlackVolatilitySurfaceDefaults extends DefaultPropertyFunction {
  private static final Logger s_logger = LoggerFactory.getLogger(CommodityBlackVolatilitySurfaceDefaults.class);
  private final String[] _valueRequirementNames;
  private final Map<String, String> _currencyToCurveName;
  private final Map<String, String> _currencyToCurveCalculationMethodName;
  private final Map<String, String> _currencyToSurfaceName;

  public CommodityBlackVolatilitySurfaceDefaults(final ComputationTargetType target, final String[] valueRequirementNames, final String... defaultsPerCurrency) {
    super(target, true);
    ArgumentChecker.notNull(defaultsPerCurrency, "defaults per currency");
    ArgumentChecker.notNull(valueRequirementNames, "value requiremement names");
    final int n = defaultsPerCurrency.length;
    ArgumentChecker.isTrue(n % 4 == 0, "Need one forward curve name, forward curve calculation method and surface name per currency");
    _valueRequirementNames = valueRequirementNames;
    _currencyToCurveName = Maps.newLinkedHashMap();
    _currencyToCurveCalculationMethodName = Maps.newLinkedHashMap();
    _currencyToSurfaceName = Maps.newLinkedHashMap();
    for (int i = 0; i < n; i += 4) {
      final String currencyPair = defaultsPerCurrency[i];
      _currencyToCurveName.put(currencyPair, defaultsPerCurrency[i + 1]);
      _currencyToCurveCalculationMethodName.put(currencyPair, defaultsPerCurrency[i + 2]);
      _currencyToSurfaceName.put(currencyPair, defaultsPerCurrency[i + 3]);
    }
  }

  @Override
  public abstract boolean canApplyTo(FunctionCompilationContext context, ComputationTarget target);

  @Override
  protected void getDefaults(final PropertyDefaults defaults) {
    for (final String valueRequirement : _valueRequirementNames) {
      defaults.addValuePropertyName(valueRequirement, ValuePropertyNames.CURVE);
      defaults.addValuePropertyName(valueRequirement, ValuePropertyNames.CURVE_CALCULATION_METHOD);
      defaults.addValuePropertyName(valueRequirement, ValuePropertyNames.SURFACE);
    }
  }

  @Override
  protected Set<String> getDefaultValue(final FunctionCompilationContext context, final ComputationTarget target, final ValueRequirement desiredValue, final String propertyName) {
    final String currencyPair = getCurrency(target);
    final String curveName = _currencyToCurveName.get(currencyPair);
    if (curveName == null) {
      s_logger.error("Could not get curve name for {}; should never happen", target.getValue());
      return null;
    }
    if (ValuePropertyNames.CURVE.equals(propertyName)) {
      return Collections.singleton(curveName);
    }
    if (ValuePropertyNames.CURVE_CALCULATION_METHOD.equals(propertyName)) {
      return Collections.singleton(_currencyToCurveCalculationMethodName.get(currencyPair));
    }
    if (ValuePropertyNames.SURFACE.equals(propertyName)) {
      return Collections.singleton(_currencyToSurfaceName.get(currencyPair));
    }
    s_logger.error("Could not find default value for {} in this function", propertyName);
    return null;
  }

  protected Collection<String> getAllCurrencies() {
    return _currencyToCurveName.keySet();
  }

  protected abstract String getCurrency(ComputationTarget target);

  @Override
  public String getMutualExclusionGroup() {
    return OpenGammaFunctionExclusions.COMMODITY_BLACK_VOLATILITY_SURFACE_DEFAULTS;
  }

}
