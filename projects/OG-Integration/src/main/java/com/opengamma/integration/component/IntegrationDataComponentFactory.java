/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.integration.component;

import java.util.LinkedHashMap;
import java.util.Map;

import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.bbg.BloombergIdentifierProvider;
import com.opengamma.bbg.loader.BloombergHistoricalTimeSeriesLoader;
import com.opengamma.bbg.referencedata.ReferenceDataProvider;
import com.opengamma.component.ComponentInfo;
import com.opengamma.component.ComponentRepository;
import com.opengamma.component.factory.AbstractComponentFactory;
import com.opengamma.financial.security.DefaultSecurityLoader;
import com.opengamma.master.historicaltimeseries.ExternalIdResolver;
import com.opengamma.master.historicaltimeseries.HistoricalTimeSeriesLoader;
import com.opengamma.master.historicaltimeseries.HistoricalTimeSeriesMaster;
import com.opengamma.master.security.SecurityLoader;
import com.opengamma.master.security.SecurityMaster;
import com.opengamma.provider.historicaltimeseries.HistoricalTimeSeriesProvider;
import com.opengamma.provider.security.SecurityProvider;

/**
 * Component factory for the Integration project.
 */
@BeanDefinition
public class IntegrationDataComponentFactory extends AbstractComponentFactory {

  /**
   * The classifier that the factory should publish under.
   */
  @PropertyDefinition(validate = "notNull")
  private String _classifier;
  /**
   * The Bloomberg reference data provider.
   */
  @PropertyDefinition(validate = "notNull")
  private ReferenceDataProvider _referenceDataProvider;
  /**
   * The security provider.
   */
  @PropertyDefinition(validate = "notNull")
  private SecurityProvider _securityProvider;
  /**
   * The time-series provider.
   */
  @PropertyDefinition(validate = "notNull")
  private HistoricalTimeSeriesProvider _historicalTimeSeriesProvider;
  /**
   * The security master.
   */
  @PropertyDefinition(validate = "notNull")
  private SecurityMaster _securityMaster;
  /**
   * The time-series master.
   */
  @PropertyDefinition(validate = "notNull")
  private HistoricalTimeSeriesMaster _historicalTimeSeriesMaster;

  //-------------------------------------------------------------------------
  @Override
  public void init(ComponentRepository repo, LinkedHashMap<String, String> configuration) throws Exception {
    initSecurityLoader(repo);
    initHistoricalTimeSeriesLoader(repo);
  }

  protected SecurityLoader initSecurityLoader(ComponentRepository repo) {
    SecurityLoader secLoader = createSecurityLoader(repo);
    ComponentInfo info = new ComponentInfo(SecurityLoader.class, getClassifier());
    repo.registerComponent(info, secLoader);
    return secLoader;
  }

  /**
   * Creates the loader.
   * 
   * @param repo  the repository, not null
   * @return the loader, not null
   */
  protected SecurityLoader createSecurityLoader(ComponentRepository repo) {
    return new DefaultSecurityLoader(getSecurityMaster(), getSecurityProvider());
  }

  protected HistoricalTimeSeriesLoader initHistoricalTimeSeriesLoader(ComponentRepository repo) {
    HistoricalTimeSeriesLoader htsLoader = createHistoricalTimeSeriesLoader(repo);
    ComponentInfo info = new ComponentInfo(HistoricalTimeSeriesLoader.class, getClassifier());
    repo.registerComponent(info, htsLoader);
    return htsLoader;
  }

  /**
   * Creates the loader.
   * 
   * @param repo  the repository, not null
   * @return the loader, not null
   */
  protected HistoricalTimeSeriesLoader createHistoricalTimeSeriesLoader(ComponentRepository repo) {
    ExternalIdResolver idProvider = new BloombergIdentifierProvider(getReferenceDataProvider());
    return new BloombergHistoricalTimeSeriesLoader(getHistoricalTimeSeriesMaster(), getHistoricalTimeSeriesProvider(), idProvider);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code IntegrationDataComponentFactory}.
   * @return the meta-bean, not null
   */
  public static IntegrationDataComponentFactory.Meta meta() {
    return IntegrationDataComponentFactory.Meta.INSTANCE;
  }
  static {
    JodaBeanUtils.registerMetaBean(IntegrationDataComponentFactory.Meta.INSTANCE);
  }

  @Override
  public IntegrationDataComponentFactory.Meta metaBean() {
    return IntegrationDataComponentFactory.Meta.INSTANCE;
  }

  @Override
  protected Object propertyGet(String propertyName, boolean quiet) {
    switch (propertyName.hashCode()) {
      case -281470431:  // classifier
        return getClassifier();
      case -1788671322:  // referenceDataProvider
        return getReferenceDataProvider();
      case 809869649:  // securityProvider
        return getSecurityProvider();
      case -1592479713:  // historicalTimeSeriesProvider
        return getHistoricalTimeSeriesProvider();
      case -887218750:  // securityMaster
        return getSecurityMaster();
      case 173967376:  // historicalTimeSeriesMaster
        return getHistoricalTimeSeriesMaster();
    }
    return super.propertyGet(propertyName, quiet);
  }

  @Override
  protected void propertySet(String propertyName, Object newValue, boolean quiet) {
    switch (propertyName.hashCode()) {
      case -281470431:  // classifier
        setClassifier((String) newValue);
        return;
      case -1788671322:  // referenceDataProvider
        setReferenceDataProvider((ReferenceDataProvider) newValue);
        return;
      case 809869649:  // securityProvider
        setSecurityProvider((SecurityProvider) newValue);
        return;
      case -1592479713:  // historicalTimeSeriesProvider
        setHistoricalTimeSeriesProvider((HistoricalTimeSeriesProvider) newValue);
        return;
      case -887218750:  // securityMaster
        setSecurityMaster((SecurityMaster) newValue);
        return;
      case 173967376:  // historicalTimeSeriesMaster
        setHistoricalTimeSeriesMaster((HistoricalTimeSeriesMaster) newValue);
        return;
    }
    super.propertySet(propertyName, newValue, quiet);
  }

  @Override
  protected void validate() {
    JodaBeanUtils.notNull(_classifier, "classifier");
    JodaBeanUtils.notNull(_referenceDataProvider, "referenceDataProvider");
    JodaBeanUtils.notNull(_securityProvider, "securityProvider");
    JodaBeanUtils.notNull(_historicalTimeSeriesProvider, "historicalTimeSeriesProvider");
    JodaBeanUtils.notNull(_securityMaster, "securityMaster");
    JodaBeanUtils.notNull(_historicalTimeSeriesMaster, "historicalTimeSeriesMaster");
    super.validate();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      IntegrationDataComponentFactory other = (IntegrationDataComponentFactory) obj;
      return JodaBeanUtils.equal(getClassifier(), other.getClassifier()) &&
          JodaBeanUtils.equal(getReferenceDataProvider(), other.getReferenceDataProvider()) &&
          JodaBeanUtils.equal(getSecurityProvider(), other.getSecurityProvider()) &&
          JodaBeanUtils.equal(getHistoricalTimeSeriesProvider(), other.getHistoricalTimeSeriesProvider()) &&
          JodaBeanUtils.equal(getSecurityMaster(), other.getSecurityMaster()) &&
          JodaBeanUtils.equal(getHistoricalTimeSeriesMaster(), other.getHistoricalTimeSeriesMaster()) &&
          super.equals(obj);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash += hash * 31 + JodaBeanUtils.hashCode(getClassifier());
    hash += hash * 31 + JodaBeanUtils.hashCode(getReferenceDataProvider());
    hash += hash * 31 + JodaBeanUtils.hashCode(getSecurityProvider());
    hash += hash * 31 + JodaBeanUtils.hashCode(getHistoricalTimeSeriesProvider());
    hash += hash * 31 + JodaBeanUtils.hashCode(getSecurityMaster());
    hash += hash * 31 + JodaBeanUtils.hashCode(getHistoricalTimeSeriesMaster());
    return hash ^ super.hashCode();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the classifier that the factory should publish under.
   * @return the value of the property, not null
   */
  public String getClassifier() {
    return _classifier;
  }

  /**
   * Sets the classifier that the factory should publish under.
   * @param classifier  the new value of the property, not null
   */
  public void setClassifier(String classifier) {
    JodaBeanUtils.notNull(classifier, "classifier");
    this._classifier = classifier;
  }

  /**
   * Gets the the {@code classifier} property.
   * @return the property, not null
   */
  public final Property<String> classifier() {
    return metaBean().classifier().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the Bloomberg reference data provider.
   * @return the value of the property, not null
   */
  public ReferenceDataProvider getReferenceDataProvider() {
    return _referenceDataProvider;
  }

  /**
   * Sets the Bloomberg reference data provider.
   * @param referenceDataProvider  the new value of the property, not null
   */
  public void setReferenceDataProvider(ReferenceDataProvider referenceDataProvider) {
    JodaBeanUtils.notNull(referenceDataProvider, "referenceDataProvider");
    this._referenceDataProvider = referenceDataProvider;
  }

  /**
   * Gets the the {@code referenceDataProvider} property.
   * @return the property, not null
   */
  public final Property<ReferenceDataProvider> referenceDataProvider() {
    return metaBean().referenceDataProvider().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the security provider.
   * @return the value of the property, not null
   */
  public SecurityProvider getSecurityProvider() {
    return _securityProvider;
  }

  /**
   * Sets the security provider.
   * @param securityProvider  the new value of the property, not null
   */
  public void setSecurityProvider(SecurityProvider securityProvider) {
    JodaBeanUtils.notNull(securityProvider, "securityProvider");
    this._securityProvider = securityProvider;
  }

  /**
   * Gets the the {@code securityProvider} property.
   * @return the property, not null
   */
  public final Property<SecurityProvider> securityProvider() {
    return metaBean().securityProvider().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the time-series provider.
   * @return the value of the property, not null
   */
  public HistoricalTimeSeriesProvider getHistoricalTimeSeriesProvider() {
    return _historicalTimeSeriesProvider;
  }

  /**
   * Sets the time-series provider.
   * @param historicalTimeSeriesProvider  the new value of the property, not null
   */
  public void setHistoricalTimeSeriesProvider(HistoricalTimeSeriesProvider historicalTimeSeriesProvider) {
    JodaBeanUtils.notNull(historicalTimeSeriesProvider, "historicalTimeSeriesProvider");
    this._historicalTimeSeriesProvider = historicalTimeSeriesProvider;
  }

  /**
   * Gets the the {@code historicalTimeSeriesProvider} property.
   * @return the property, not null
   */
  public final Property<HistoricalTimeSeriesProvider> historicalTimeSeriesProvider() {
    return metaBean().historicalTimeSeriesProvider().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the security master.
   * @return the value of the property, not null
   */
  public SecurityMaster getSecurityMaster() {
    return _securityMaster;
  }

  /**
   * Sets the security master.
   * @param securityMaster  the new value of the property, not null
   */
  public void setSecurityMaster(SecurityMaster securityMaster) {
    JodaBeanUtils.notNull(securityMaster, "securityMaster");
    this._securityMaster = securityMaster;
  }

  /**
   * Gets the the {@code securityMaster} property.
   * @return the property, not null
   */
  public final Property<SecurityMaster> securityMaster() {
    return metaBean().securityMaster().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the time-series master.
   * @return the value of the property, not null
   */
  public HistoricalTimeSeriesMaster getHistoricalTimeSeriesMaster() {
    return _historicalTimeSeriesMaster;
  }

  /**
   * Sets the time-series master.
   * @param historicalTimeSeriesMaster  the new value of the property, not null
   */
  public void setHistoricalTimeSeriesMaster(HistoricalTimeSeriesMaster historicalTimeSeriesMaster) {
    JodaBeanUtils.notNull(historicalTimeSeriesMaster, "historicalTimeSeriesMaster");
    this._historicalTimeSeriesMaster = historicalTimeSeriesMaster;
  }

  /**
   * Gets the the {@code historicalTimeSeriesMaster} property.
   * @return the property, not null
   */
  public final Property<HistoricalTimeSeriesMaster> historicalTimeSeriesMaster() {
    return metaBean().historicalTimeSeriesMaster().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code IntegrationDataComponentFactory}.
   */
  public static class Meta extends AbstractComponentFactory.Meta {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code classifier} property.
     */
    private final MetaProperty<String> _classifier = DirectMetaProperty.ofReadWrite(
        this, "classifier", IntegrationDataComponentFactory.class, String.class);
    /**
     * The meta-property for the {@code referenceDataProvider} property.
     */
    private final MetaProperty<ReferenceDataProvider> _referenceDataProvider = DirectMetaProperty.ofReadWrite(
        this, "referenceDataProvider", IntegrationDataComponentFactory.class, ReferenceDataProvider.class);
    /**
     * The meta-property for the {@code securityProvider} property.
     */
    private final MetaProperty<SecurityProvider> _securityProvider = DirectMetaProperty.ofReadWrite(
        this, "securityProvider", IntegrationDataComponentFactory.class, SecurityProvider.class);
    /**
     * The meta-property for the {@code historicalTimeSeriesProvider} property.
     */
    private final MetaProperty<HistoricalTimeSeriesProvider> _historicalTimeSeriesProvider = DirectMetaProperty.ofReadWrite(
        this, "historicalTimeSeriesProvider", IntegrationDataComponentFactory.class, HistoricalTimeSeriesProvider.class);
    /**
     * The meta-property for the {@code securityMaster} property.
     */
    private final MetaProperty<SecurityMaster> _securityMaster = DirectMetaProperty.ofReadWrite(
        this, "securityMaster", IntegrationDataComponentFactory.class, SecurityMaster.class);
    /**
     * The meta-property for the {@code historicalTimeSeriesMaster} property.
     */
    private final MetaProperty<HistoricalTimeSeriesMaster> _historicalTimeSeriesMaster = DirectMetaProperty.ofReadWrite(
        this, "historicalTimeSeriesMaster", IntegrationDataComponentFactory.class, HistoricalTimeSeriesMaster.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> _metaPropertyMap$ = new DirectMetaPropertyMap(
        this, (DirectMetaPropertyMap) super.metaPropertyMap(),
        "classifier",
        "referenceDataProvider",
        "securityProvider",
        "historicalTimeSeriesProvider",
        "securityMaster",
        "historicalTimeSeriesMaster");

    /**
     * Restricted constructor.
     */
    protected Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -281470431:  // classifier
          return _classifier;
        case -1788671322:  // referenceDataProvider
          return _referenceDataProvider;
        case 809869649:  // securityProvider
          return _securityProvider;
        case -1592479713:  // historicalTimeSeriesProvider
          return _historicalTimeSeriesProvider;
        case -887218750:  // securityMaster
          return _securityMaster;
        case 173967376:  // historicalTimeSeriesMaster
          return _historicalTimeSeriesMaster;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends IntegrationDataComponentFactory> builder() {
      return new DirectBeanBuilder<IntegrationDataComponentFactory>(new IntegrationDataComponentFactory());
    }

    @Override
    public Class<? extends IntegrationDataComponentFactory> beanType() {
      return IntegrationDataComponentFactory.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return _metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code classifier} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<String> classifier() {
      return _classifier;
    }

    /**
     * The meta-property for the {@code referenceDataProvider} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<ReferenceDataProvider> referenceDataProvider() {
      return _referenceDataProvider;
    }

    /**
     * The meta-property for the {@code securityProvider} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<SecurityProvider> securityProvider() {
      return _securityProvider;
    }

    /**
     * The meta-property for the {@code historicalTimeSeriesProvider} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<HistoricalTimeSeriesProvider> historicalTimeSeriesProvider() {
      return _historicalTimeSeriesProvider;
    }

    /**
     * The meta-property for the {@code securityMaster} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<SecurityMaster> securityMaster() {
      return _securityMaster;
    }

    /**
     * The meta-property for the {@code historicalTimeSeriesMaster} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<HistoricalTimeSeriesMaster> historicalTimeSeriesMaster() {
      return _historicalTimeSeriesMaster;
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
