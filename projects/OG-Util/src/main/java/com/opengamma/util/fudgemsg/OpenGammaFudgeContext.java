/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.util.fudgemsg;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.fudgemsg.AnnotationReflector;
import org.fudgemsg.FudgeContext;
import org.fudgemsg.FudgeTypeDictionary;
import org.reflections.Configuration;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.reflections.vfs.SystemDir;
import org.reflections.vfs.Vfs;
import org.reflections.vfs.Vfs.Dir;
import org.reflections.vfs.Vfs.UrlType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.opengamma.timeseries.date.localdate.ImmutableLocalDateDoubleTimeSeries;
import com.opengamma.timeseries.precise.zdt.ImmutableZonedDateTimeDoubleTimeSeries;

/**
 * Provides a shared singleton {@code FudgeContext} for use throughout OpenGamma.
 * <p>
 * The {@code FudgeContext} is a low-level object necessary to use the Fudge messaging system.
 * Providing the context to Fudge on demand would clutter code and configuration.
 * This class instead provides a singleton that can be used whenever necessary.
 */
public final class OpenGammaFudgeContext {

  /** Logger. */
  private static final Logger s_logger = LoggerFactory.getLogger(OpenGammaFudgeContext.class);

  /**
   * Restricted constructor.
   */
  private OpenGammaFudgeContext() {
  }

  /**
   * Gets the singleton instance of the context, creating it if necessary.
   * @return the singleton instance, not null
   */
  public static FudgeContext getInstance() {
    return ContextHolder.INSTANCE;
  }

  private static final String DEFAULT_ANNOTATION_REFLECTOR_FILTER =
      "-java., " +
      "-javax., " +
      "-sun., " +
      "-sunw., " +
      "-com.sun., " +
      "-org.springframework., " +
      "-org.eclipse., " +
      "-org.apache., " +
      "-org.antlr., " +
      "-org.hibernate., " +
      "-org.fudgemsg., " +
      "-org.threeten., " +
      "-org.reflections., " +
      "-org.joda., " +
      "-cern.clhep., " +
      "-cern.colt., " +
      "-cern.jet.math., " +
      "-ch.qos.logback., " +
      "-com.codahale.metrics., " +
      "-com.mongodb., " +
      "-com.sleepycat., " +
      "-com.yahoo.platform.yui., " +
      "-de.odysseus.el., " +
      "-freemarker., " +
      "-groovy., " +
      "-groovyjar, " +
      "-it.unimi.dsi.fastutil., " +
      "-jargs.gnu., " +
      "-javassist., " +
      "-jsr166y., " +
      "-net.sf.ehcache., " +
      "-org.bson., " +
      "-org.codehaus.groovy., " +
      "-org.cometd., " +
      "-com.google.common., " +
      "-org.hsqldb., " +
      "-com.jolbox., " +
      "-edu.emory.mathcs., " +
      "-info.ganglia., " +
      "-org.aopalliance., " +
      "-org.dom4j., " +
      "-org.mozilla.javascript., " +
      "-org.mozilla.classfile., " +
      "-org.objectweb.asm., " +
      "-org.osgi., " +
      "-org.postgresql., " +
      "-org.quartz., " +
      "-org.slf4j., " +
      "-org.w3c.dom, " +
      "-org.xml.sax., " +
      "-org.jcsp., " +
      "-org.json., " +
      "-redis.";

  /**
   * Avoid double-checked-locking using the Initialization-on-demand holder idiom.
   */
  static final class ContextHolder {
    static final FudgeContext INSTANCE = constructContext();
    private static FudgeContext constructContext() {
      FudgeContext fudgeContext = new FudgeContext();
      ExtendedFudgeBuilderFactory.init(fudgeContext.getObjectDictionary());
      InnerClassFudgeBuilderFactory.init(fudgeContext.getObjectDictionary());
      
      // hack to handle non-existent classpath directory entries
      List<UrlType> urlTypes = Lists.newArrayList(Vfs.getDefaultUrlTypes());
      urlTypes.add(0, new OGFileUrlType());
      Vfs.setDefaultURLTypes(urlTypes);
      
      // hack to try to get a better classpath
      List<ClassLoader> loaders = new ArrayList<>();
      loaders.add(OpenGammaFudgeContext.class.getClassLoader());
      try {
        loaders.add(Thread.currentThread().getContextClassLoader());
      } catch (Exception ex) {
        // ignore
      }
      Configuration config = new ConfigurationBuilder()
        .setUrls(ClasspathHelper.forManifest(ClasspathHelper.forJavaClassPath()))
        .setScanners(new TypeAnnotationsScanner(), new FieldAnnotationsScanner())
        .filterInputsBy(FilterBuilder.parse(DEFAULT_ANNOTATION_REFLECTOR_FILTER))
        .addClassLoaders(loaders)
        .useParallelExecutor();
      AnnotationReflector reflector = new AnnotationReflector(config);
      
      fudgeContext.getObjectDictionary().addAllAnnotatedBuilders(reflector);
      fudgeContext.getTypeDictionary().addAllAnnotatedSecondaryTypes(reflector);
      
      FudgeTypeDictionary td = fudgeContext.getTypeDictionary();
      td.registerClassRename("com.opengamma.util.timeseries.zoneddatetime.ArrayZonedDateTimeDoubleTimeSeries", ImmutableZonedDateTimeDoubleTimeSeries.class);
      td.registerClassRename("com.opengamma.timeseries.zoneddatetime.ArrayZonedDateTimeDoubleTimeSeries", ImmutableZonedDateTimeDoubleTimeSeries.class);
      td.registerClassRename("com.opengamma.util.timeseries.zoneddatetime.ListZonedDateTimeDoubleTimeSeries", ImmutableZonedDateTimeDoubleTimeSeries.class);
      td.registerClassRename("com.opengamma.timeseries.zoneddatetime.ListZonedDateTimeDoubleTimeSeries", ImmutableZonedDateTimeDoubleTimeSeries.class);
      
      td.registerClassRename("com.opengamma.util.timeseries.localdate.ArrayLocalDateDoubleTimeSeries", ImmutableLocalDateDoubleTimeSeries.class);
      td.registerClassRename("com.opengamma.timeseries.localdate.ArrayLocalDateDoubleTimeSeries", ImmutableLocalDateDoubleTimeSeries.class);
      td.registerClassRename("com.opengamma.util.timeseries.localdate.ListLocalDateDoubleTimeSeries", ImmutableLocalDateDoubleTimeSeries.class);
      td.registerClassRename("com.opengamma.timeseries.localdate.ListLocalDateDoubleTimeSeries", ImmutableLocalDateDoubleTimeSeries.class);
      td.registerClassRename("com.opengamma.util.timeseries.localdate.MapLocalDateDoubleTimeSeries", ImmutableLocalDateDoubleTimeSeries.class);
      td.registerClassRename("com.opengamma.timeseries.localdate.MapLocalDateDoubleTimeSeries", ImmutableLocalDateDoubleTimeSeries.class);

      return fudgeContext;
    }
  }

  //-------------------------------------------------------------------------
  // handle non-existent classpath directory entries
  private static final class OGFileUrlType implements UrlType {

    @Override
    public boolean matches(URL url) throws Exception {
      return url.getProtocol().equals("file") && !url.toExternalForm().contains(".jar");
    }

    @Override
    public Dir createDir(URL url) throws Exception {
      File file = Vfs.getFile(url);
      if (file == null || file.exists() == false) {
        s_logger.warn("URL could not be resolved to a file: " + url);
        return new EmptyDir(file);
      } else {
        return new SystemDir(file);
      }
    }

    @Override
    public String toString() {
      return "directories (OGFileUrlType fix)";
    }
  }

  //-------------------------------------------------------------------------
  // handle non-existent classpath directory entries
  private static final class EmptyDir implements Vfs.Dir {
    private final File _file;

    private EmptyDir(File file) {
      this._file = file;
    }

    public String getPath() {
      return _file.getPath().replace("\\", "/");
    }


    public Iterable<Vfs.File> getFiles() {
      return Collections.emptyList();  // just return no files
    }

    public void close() {
    }

    @Override
    public String toString() {
      return _file.toString();
    }
  }
}
