/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.examples.functionrepo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.engine.function.config.FunctionConfiguration;
import com.opengamma.engine.function.config.ParameterizedFunctionConfiguration;
import com.opengamma.engine.function.config.RepositoryConfiguration;
import com.opengamma.engine.function.config.RepositoryConfigurationSource;
import com.opengamma.engine.function.config.SimpleRepositoryConfigurationSource;
import com.opengamma.engine.function.config.StaticFunctionConfiguration;
import com.opengamma.financial.analytics.ircurve.YieldCurveDefinition;
import com.opengamma.financial.analytics.ircurve.YieldCurveInterpolatingFunction;
import com.opengamma.financial.analytics.ircurve.YieldCurveMarketDataFunction;
import com.opengamma.financial.analytics.ircurve.YieldCurveSpecificationFunction;
import com.opengamma.financial.analytics.model.volatility.local.DummyFXForwardCurveFunction;
import com.opengamma.financial.analytics.model.volatility.local.FXForwardCurveFromYieldCurveDefaultPropertiesFunction;
import com.opengamma.financial.analytics.model.volatility.local.FXForwardCurveFromYieldCurveFunction;
import com.opengamma.financial.analytics.volatility.cube.BloombergVolatilityCubeDefinitionSource;
import com.opengamma.financial.analytics.volatility.cube.VolatilityCubeFunction;
import com.opengamma.financial.analytics.volatility.cube.VolatilityCubeInstrumentProvider;
import com.opengamma.financial.analytics.volatility.cube.VolatilityCubeMarketDataFunction;
import com.opengamma.financial.convention.ConventionBundleSource;
import com.opengamma.master.config.ConfigDocument;
import com.opengamma.master.config.ConfigMaster;
import com.opengamma.master.config.ConfigSearchRequest;
import com.opengamma.master.config.ConfigSearchResult;
import com.opengamma.util.SingletonFactoryBean;
import com.opengamma.util.money.Currency;

/**
 * Creates function repository configuration for curve supplying functions.
 * 
 * Note [PLAT-1094] - the functions should really be built by scanning the curves and currencies available. 
 */
public class ExampleCurveFunctionConfiguration extends SingletonFactoryBean<RepositoryConfigurationSource> {

  private static final Logger s_logger = LoggerFactory.getLogger(ExampleCurveFunctionConfiguration.class);

  private ConfigMaster _configMaster;
  @SuppressWarnings("unused")
  private ConventionBundleSource _conventionBundleSource; //TODO not sure if we'll need this in the future

  public void setConfigMaster(final ConfigMaster configMaster) {
    _configMaster = configMaster;
  }

  public void setConventionBundleSource(final ConventionBundleSource conventionBundleSource) {
    _conventionBundleSource = conventionBundleSource;
  }

  public RepositoryConfiguration constructRepositoryConfiguration() {
    final List<FunctionConfiguration> configs = new ArrayList<FunctionConfiguration>();

    if (_configMaster != null) {
      // [PLAT-1094] Scan the config master for documents. This is probably in the wrong place; the code should live in OG-Financial as it is
      // tightly coupled to the ConfigDbInterpolatedYieldCurveSource and MarketInstrumentImpliedYieldCurveFunction classes
      final ConfigSearchRequest<YieldCurveDefinition> searchRequest = new ConfigSearchRequest<YieldCurveDefinition>();
      searchRequest.setType(YieldCurveDefinition.class);

      final ConfigSearchResult<YieldCurveDefinition> searchResult = _configMaster.search(searchRequest);
      for (ConfigDocument<YieldCurveDefinition> configDocument : searchResult.getDocuments()) {
        final String documentName = configDocument.getName();
        final int underscore = documentName.lastIndexOf('_');
        if (underscore <= 0) {
          continue;
        }
        final String curveName = documentName.substring(0, underscore);
        final String currencyISO = documentName.substring(underscore + 1);
        s_logger.debug("Found {} curve for {}", curveName, currencyISO);
        addYieldCurveFunction(configs, currencyISO, curveName);
      }
    } 
    s_logger.info("Created repository configuration with {} curve provider functions", configs.size());

    addSwaptionVolCubeFunction(configs);
    s_logger.info("Added swaption vol cube to repository configuration");
    addFXForwardCurveFunction(configs);
    s_logger.info("Added FX forward curve to repository configuration");
    return new RepositoryConfiguration(configs);
  }

  private void addSwaptionVolCubeFunction(final List<FunctionConfiguration> configs) {
    addVolatilityCubeFunction(configs, "USD", "SECONDARY");

    Set<Currency> volCubeCurrencies = VolatilityCubeInstrumentProvider.BLOOMBERG.getAllCurrencies();
    for (Currency currency : volCubeCurrencies) {
      addVolatilityCubeFunction(configs, currency.getCode(), BloombergVolatilityCubeDefinitionSource.DEFINITION_NAME);
    }
  }

  private void addYieldCurveFunction(final List<FunctionConfiguration> configs, final String currency, final String curveName) {
    configs.add(new ParameterizedFunctionConfiguration(YieldCurveMarketDataFunction.class.getName(), Arrays.asList(currency, curveName)));
    configs.add(new ParameterizedFunctionConfiguration(YieldCurveInterpolatingFunction.class.getName(), Arrays.asList(currency, curveName)));
    configs.add(new ParameterizedFunctionConfiguration(YieldCurveSpecificationFunction.class.getName(), Arrays.asList(currency, curveName)));
  }

  private void addFXForwardCurveFunction(List<FunctionConfiguration> configs) {
    configs.add(new ParameterizedFunctionConfiguration(DummyFXForwardCurveFunction.class.getName(), Arrays.asList("DEFAULT", "EUR", "USD")));    
    configs.add(new StaticFunctionConfiguration(FXForwardCurveFromYieldCurveFunction.class.getName()));
    configs.add(new ParameterizedFunctionConfiguration(FXForwardCurveFromYieldCurveDefaultPropertiesFunction.class.getName(), Arrays.asList("DEFAULT", "SECONDARY", "SECONDARY")));
  }
  
  private void addVolatilityCubeFunction(List<FunctionConfiguration> configs, String... parameters) {
    addVolatilityCubeFunction(configs, Arrays.asList(parameters));
  }

  private void addVolatilityCubeFunction(final List<FunctionConfiguration> configs, List<String> parameters) {
    if (parameters.size() != 2) {
      throw new IllegalArgumentException();
    }

    configs.add(new ParameterizedFunctionConfiguration(VolatilityCubeFunction.class.getName(), parameters));
    configs.add(new ParameterizedFunctionConfiguration(VolatilityCubeMarketDataFunction.class.getName(), parameters));
  }

  //-------------------------------------------------------------------------
  public RepositoryConfigurationSource constructRepositoryConfigurationSource() {
    return new SimpleRepositoryConfigurationSource(constructRepositoryConfiguration());
  }

  @Override
  protected RepositoryConfigurationSource createObject() {
    return constructRepositoryConfigurationSource();
  }

}
