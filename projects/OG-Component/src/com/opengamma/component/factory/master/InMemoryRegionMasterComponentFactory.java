/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.component.factory.master;

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
import org.springframework.context.Lifecycle;

import com.opengamma.component.ComponentInfo;
import com.opengamma.component.ComponentRepository;
import com.opengamma.component.factory.AbstractComponentFactory;
import com.opengamma.component.factory.ComponentInfoAttributes;
import com.opengamma.core.change.BasicChangeManager;
import com.opengamma.core.change.ChangeManager;
import com.opengamma.core.change.JmsChangeManager;
import com.opengamma.id.ObjectIdSupplier;
import com.opengamma.master.region.RegionMaster;
import com.opengamma.master.region.impl.DataRegionsResource;
import com.opengamma.master.region.impl.InMemoryRegionMaster;
import com.opengamma.master.region.impl.RegionFileReader;
import com.opengamma.util.jms.JmsConnector;

/**
 * Component factory for the database region master.
 */
@BeanDefinition
public class InMemoryRegionMasterComponentFactory extends AbstractComponentFactory {

  /**
   * The classifier that the factory should publish under.
   */
  @PropertyDefinition
  private String _classifier;
  /**
   * The flag determining whether the component should be published by REST.
   */
  @PropertyDefinition
  private boolean _publishRest;
  /**
   * The JMS connector.
   */
  @PropertyDefinition
  private JmsConnector _jmsConnector;
  /**
   * The JMS change manager topic.
   */
  @PropertyDefinition
  private String _jmsChangeManagerTopic;
  /**
   * The scheme used by the {@code UniqueId}.
   */
  @PropertyDefinition
  private String _idScheme;

  //-------------------------------------------------------------------------
  @Override
  public void init(ComponentRepository repo, LinkedHashMap<String, String> configuration) {
    ComponentInfo info = new ComponentInfo(RegionMaster.class, getClassifier());
    
    // create
    String scheme = (getIdScheme() != null ? getIdScheme() : InMemoryRegionMaster.DEFAULT_OID_SCHEME);
    ChangeManager cm = new BasicChangeManager();
    if (getJmsChangeManagerTopic() != null) {
      cm = new JmsChangeManager(getJmsConnector(), getJmsChangeManagerTopic());
      repo.registerLifecycle((Lifecycle) cm);
      info.addAttribute(ComponentInfoAttributes.JMS_CHANGE_MANAGER_TOPIC, getJmsChangeManagerTopic());
    }
    InMemoryRegionMaster master = new InMemoryRegionMaster(new ObjectIdSupplier(scheme), cm);
    RegionFileReader.createPopulated(master);
    
    // register
    info.addAttribute(ComponentInfoAttributes.UNIQUE_ID_SCHEME, scheme);
    repo.registerComponent(info, master);
    
    // publish
    if (isPublishRest()) {
      repo.publishRest(info, new DataRegionsResource(master));
    }
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code InMemoryRegionMasterComponentFactory}.
   * @return the meta-bean, not null
   */
  public static InMemoryRegionMasterComponentFactory.Meta meta() {
    return InMemoryRegionMasterComponentFactory.Meta.INSTANCE;
  }
  static {
    JodaBeanUtils.registerMetaBean(InMemoryRegionMasterComponentFactory.Meta.INSTANCE);
  }

  @Override
  public InMemoryRegionMasterComponentFactory.Meta metaBean() {
    return InMemoryRegionMasterComponentFactory.Meta.INSTANCE;
  }

  @Override
  protected Object propertyGet(String propertyName, boolean quiet) {
    switch (propertyName.hashCode()) {
      case -281470431:  // classifier
        return getClassifier();
      case -614707837:  // publishRest
        return isPublishRest();
      case -1495762275:  // jmsConnector
        return getJmsConnector();
      case -758086398:  // jmsChangeManagerTopic
        return getJmsChangeManagerTopic();
      case -661606752:  // idScheme
        return getIdScheme();
    }
    return super.propertyGet(propertyName, quiet);
  }

  @Override
  protected void propertySet(String propertyName, Object newValue, boolean quiet) {
    switch (propertyName.hashCode()) {
      case -281470431:  // classifier
        setClassifier((String) newValue);
        return;
      case -614707837:  // publishRest
        setPublishRest((Boolean) newValue);
        return;
      case -1495762275:  // jmsConnector
        setJmsConnector((JmsConnector) newValue);
        return;
      case -758086398:  // jmsChangeManagerTopic
        setJmsChangeManagerTopic((String) newValue);
        return;
      case -661606752:  // idScheme
        setIdScheme((String) newValue);
        return;
    }
    super.propertySet(propertyName, newValue, quiet);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      InMemoryRegionMasterComponentFactory other = (InMemoryRegionMasterComponentFactory) obj;
      return JodaBeanUtils.equal(getClassifier(), other.getClassifier()) &&
          JodaBeanUtils.equal(isPublishRest(), other.isPublishRest()) &&
          JodaBeanUtils.equal(getJmsConnector(), other.getJmsConnector()) &&
          JodaBeanUtils.equal(getJmsChangeManagerTopic(), other.getJmsChangeManagerTopic()) &&
          JodaBeanUtils.equal(getIdScheme(), other.getIdScheme()) &&
          super.equals(obj);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash += hash * 31 + JodaBeanUtils.hashCode(getClassifier());
    hash += hash * 31 + JodaBeanUtils.hashCode(isPublishRest());
    hash += hash * 31 + JodaBeanUtils.hashCode(getJmsConnector());
    hash += hash * 31 + JodaBeanUtils.hashCode(getJmsChangeManagerTopic());
    hash += hash * 31 + JodaBeanUtils.hashCode(getIdScheme());
    return hash ^ super.hashCode();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the classifier that the factory should publish under.
   * @return the value of the property
   */
  public String getClassifier() {
    return _classifier;
  }

  /**
   * Sets the classifier that the factory should publish under.
   * @param classifier  the new value of the property
   */
  public void setClassifier(String classifier) {
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
   * Gets the flag determining whether the component should be published by REST.
   * @return the value of the property
   */
  public boolean isPublishRest() {
    return _publishRest;
  }

  /**
   * Sets the flag determining whether the component should be published by REST.
   * @param publishRest  the new value of the property
   */
  public void setPublishRest(boolean publishRest) {
    this._publishRest = publishRest;
  }

  /**
   * Gets the the {@code publishRest} property.
   * @return the property, not null
   */
  public final Property<Boolean> publishRest() {
    return metaBean().publishRest().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the JMS connector.
   * @return the value of the property
   */
  public JmsConnector getJmsConnector() {
    return _jmsConnector;
  }

  /**
   * Sets the JMS connector.
   * @param jmsConnector  the new value of the property
   */
  public void setJmsConnector(JmsConnector jmsConnector) {
    this._jmsConnector = jmsConnector;
  }

  /**
   * Gets the the {@code jmsConnector} property.
   * @return the property, not null
   */
  public final Property<JmsConnector> jmsConnector() {
    return metaBean().jmsConnector().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the JMS change manager topic.
   * @return the value of the property
   */
  public String getJmsChangeManagerTopic() {
    return _jmsChangeManagerTopic;
  }

  /**
   * Sets the JMS change manager topic.
   * @param jmsChangeManagerTopic  the new value of the property
   */
  public void setJmsChangeManagerTopic(String jmsChangeManagerTopic) {
    this._jmsChangeManagerTopic = jmsChangeManagerTopic;
  }

  /**
   * Gets the the {@code jmsChangeManagerTopic} property.
   * @return the property, not null
   */
  public final Property<String> jmsChangeManagerTopic() {
    return metaBean().jmsChangeManagerTopic().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the scheme used by the {@code UniqueId}.
   * @return the value of the property
   */
  public String getIdScheme() {
    return _idScheme;
  }

  /**
   * Sets the scheme used by the {@code UniqueId}.
   * @param idScheme  the new value of the property
   */
  public void setIdScheme(String idScheme) {
    this._idScheme = idScheme;
  }

  /**
   * Gets the the {@code idScheme} property.
   * @return the property, not null
   */
  public final Property<String> idScheme() {
    return metaBean().idScheme().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code InMemoryRegionMasterComponentFactory}.
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
        this, "classifier", InMemoryRegionMasterComponentFactory.class, String.class);
    /**
     * The meta-property for the {@code publishRest} property.
     */
    private final MetaProperty<Boolean> _publishRest = DirectMetaProperty.ofReadWrite(
        this, "publishRest", InMemoryRegionMasterComponentFactory.class, Boolean.TYPE);
    /**
     * The meta-property for the {@code jmsConnector} property.
     */
    private final MetaProperty<JmsConnector> _jmsConnector = DirectMetaProperty.ofReadWrite(
        this, "jmsConnector", InMemoryRegionMasterComponentFactory.class, JmsConnector.class);
    /**
     * The meta-property for the {@code jmsChangeManagerTopic} property.
     */
    private final MetaProperty<String> _jmsChangeManagerTopic = DirectMetaProperty.ofReadWrite(
        this, "jmsChangeManagerTopic", InMemoryRegionMasterComponentFactory.class, String.class);
    /**
     * The meta-property for the {@code idScheme} property.
     */
    private final MetaProperty<String> _idScheme = DirectMetaProperty.ofReadWrite(
        this, "idScheme", InMemoryRegionMasterComponentFactory.class, String.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<Object>> _map = new DirectMetaPropertyMap(
      this, (DirectMetaPropertyMap) super.metaPropertyMap(),
        "classifier",
        "publishRest",
        "jmsConnector",
        "jmsChangeManagerTopic",
        "idScheme");

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
        case -614707837:  // publishRest
          return _publishRest;
        case -1495762275:  // jmsConnector
          return _jmsConnector;
        case -758086398:  // jmsChangeManagerTopic
          return _jmsChangeManagerTopic;
        case -661606752:  // idScheme
          return _idScheme;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends InMemoryRegionMasterComponentFactory> builder() {
      return new DirectBeanBuilder<InMemoryRegionMasterComponentFactory>(new InMemoryRegionMasterComponentFactory());
    }

    @Override
    public Class<? extends InMemoryRegionMasterComponentFactory> beanType() {
      return InMemoryRegionMasterComponentFactory.class;
    }

    @Override
    public Map<String, MetaProperty<Object>> metaPropertyMap() {
      return _map;
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
     * The meta-property for the {@code publishRest} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Boolean> publishRest() {
      return _publishRest;
    }

    /**
     * The meta-property for the {@code jmsConnector} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<JmsConnector> jmsConnector() {
      return _jmsConnector;
    }

    /**
     * The meta-property for the {@code jmsChangeManagerTopic} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<String> jmsChangeManagerTopic() {
      return _jmsChangeManagerTopic;
    }

    /**
     * The meta-property for the {@code idScheme} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<String> idScheme() {
      return _idScheme;
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
